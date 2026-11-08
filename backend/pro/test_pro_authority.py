"""Run: python -B -m unittest discover -s /absolute/backend/pro -p 'test_*.py' -v

All listeners are ephemeral IPv4 loopback. All databases are temporary within
this directory. Clock injection avoids sleeps except for bounded real-socket
progress checks. Tests capture CLI output and never print generated codes,
installation secrets or peppers.
"""

from concurrent.futures import ThreadPoolExecutor
from contextlib import closing, redirect_stderr, redirect_stdout
import hashlib
import hmac
import http.client
import io
import json
import os
from pathlib import Path
import secrets
import select
import socket
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import patch

import pro_authority as pro


HERE = Path(__file__).resolve().parent
SCRIPT = HERE / "pro_authority.py"
SUCCESS_KEYS = {"nonce", "subject", "audience", "issuedAtMillis", "expiresAtMillis",
                "authorityNowMillis", "revoked", "features"}


class Clock:
    def __init__(self, value):
        self.value = value
        self.lock = threading.Lock()

    def __call__(self):
        with self.lock:
            return self.value

    def advance(self, amount):
        with self.lock:
            self.value += amount


class AuthorityTransactionTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix="pro-transaction-test-", dir=HERE)
        self.addCleanup(temporary.cleanup)
        self.database = Path(temporary.name) / "authority.sqlite3"
        self.authority = pro.Authority(self.database, secrets.token_bytes(32), initialize=True)
        self.code_id = self.authority.issue(60)["codeId"]
        self.events = []
        events = self.events

        class TrackedConnection(sqlite3.Connection):
            def commit(self):
                events.append(("commit", self.in_transaction))
                super().commit()

            def rollback(self):
                events.append(("rollback", self.in_transaction))
                super().rollback()

            def close(self):
                # Observe state BEFORE SQLite's implicit close-time rollback.
                events.append(("close", self.in_transaction))
                super().close()

        self.connect = sqlite3.connect

        def tracked_connect(*args, **kwargs):
            return self.connect(*args, **kwargs, factory=TrackedConnection)

        patcher = patch.object(pro.sqlite3, "connect", side_effect=tracked_connect)
        self.addCleanup(patcher.stop)
        patcher.start()

    def assert_transaction_outcome(self, state, events):
        with closing(self.connect(self.database)) as connection:
            row = connection.execute("SELECT state FROM codes WHERE id = ?", (self.code_id,)).fetchone()
            self.assertEqual(row[0], state)
        self.assertEqual(self.events, events)

    def test_normal_early_return_commits(self):
        self.assertTrue(self.authority.revoke(code_id=self.code_id))
        self.assert_transaction_outcome("revoked", [("commit", True), ("close", False)])

    def test_body_errors_roll_back_before_close(self):
        for error_type in (RuntimeError, BaseException, GeneratorExit):
            with self.subTest(error_type=error_type.__name__):
                self.events.clear()
                error = error_type("test transaction abort")
                with self.assertRaises(error_type) as caught:
                    with self.authority._transaction() as connection:
                        connection.execute("UPDATE codes SET state = 'revoked'")
                        raise error
                self.assertIs(caught.exception, error)
                self.assert_transaction_outcome("fresh", [("rollback", True), ("close", False)])

    def test_generator_close_rolls_back_before_close(self):
        def mutate():
            with self.authority._transaction() as connection:
                connection.execute("UPDATE codes SET state = 'revoked'")
                yield

        with closing(mutate()) as operation:
            next(operation)
        self.assert_transaction_outcome("fresh", [("rollback", True), ("close", False)])

    def test_commit_failure_rolls_back_before_close(self):
        with self.assertRaises(sqlite3.IntegrityError):
            with self.authority._transaction() as connection:
                connection.execute("UPDATE codes SET state = 'revoked'")
                # A real deferred FK violation leaves the transaction open when
                # COMMIT fails; this fixture table is rolled back with the update.
                connection.execute("""CREATE TABLE deferred_failure (
                    code_id INTEGER REFERENCES codes(id) DEFERRABLE INITIALLY DEFERRED
                )""")
                connection.execute("INSERT INTO deferred_failure VALUES (-1)")
        self.assert_transaction_outcome("fresh", [("commit", True), ("rollback", True), ("close", False)])


class AuthorityHTTPTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="pro-test-", dir=HERE)
        self.addCleanup(self.temporary.cleanup)
        self.database = Path(self.temporary.name) / "authority.sqlite3"
        self.pepper = secrets.token_bytes(32)
        self.secret = secrets.token_hex(32)
        self.subject = pro.subject_for_secret(self.secret)
        self.nonce = secrets.token_hex(32)
        self.clock = Clock(1_800_000_000_000)
        self.rate_clock = Clock(100)
        # Exercise the production contention budget. A test-wide 100ms override
        # makes correct durable commits fail on hosted disks under parallel load.
        self.authority = pro.Authority(self.database, self.pepper, initialize=True,
                                       clock=self.clock)
        self.server = pro.ProHTTPServer(self.authority, limiter=pro.RateLimiter(
            peer_limit=10000, subject_limit=10000, clock=self.rate_clock))
        self.thread = threading.Thread(target=self.server.serve_forever,
                                       kwargs={"poll_interval": 0.01}, daemon=True)
        self.thread.start()
        self.addCleanup(self.stop_server)

    def stop_server(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=3)
        self.assertFalse(self.thread.is_alive())

    def request(self, route="/v1/validate", *, secret=None, payload=None,
                body=None, headers=None, method="POST"):
        if secret is None:
            secret = self.secret
        if body is None:
            if payload is None:
                payload = {"nonce": self.nonce}
            body = json.dumps(payload).encode("utf-8")
        request_headers = {"Authorization": "Bearer " + secret,
                           "Content-Type": "application/json"}
        if headers:
            request_headers.update(headers)
        connection = http.client.HTTPConnection(*self.server.server_address, timeout=5)
        try:
            connection.request(method, route, body=body, headers=request_headers)
            response = connection.getresponse()
            response_headers = dict(response.getheaders())
            data = response.read()
            self.assert_security_headers(response_headers)
            return response.status, json.loads(data) if data else None, response_headers
        finally:
            connection.close()

    def raw_request(self, data):
        with socket.create_connection(self.server.server_address, timeout=5) as connection:
            connection.sendall(data)
            connection.shutdown(socket.SHUT_WR)
            response = http.client.HTTPResponse(connection)
            response.begin()
            headers = dict(response.getheaders())
            body = response.read()
            self.assert_security_headers(headers)
            return response.status, json.loads(body), headers

    def raw_headers(self, extra, *, body=None, request_line=b"POST /v1/validate HTTP/1.1"):
        if body is None:
            body = json.dumps({"nonce": self.nonce}).encode("ascii")
        return (request_line + b"\r\nHost: localhost\r\nAuthorization: Bearer "
                + self.secret.encode("ascii") + b"\r\nContent-Type: application/json\r\n"
                + f"Content-Length: {len(body)}\r\n".encode("ascii") + extra + b"\r\n" + body)

    def assert_security_headers(self, headers):
        self.assertEqual(headers.get("Cache-Control"), "no-store")
        self.assertEqual(headers.get("X-Content-Type-Options"), "nosniff")
        self.assertEqual(headers.get("Content-Type"), "application/json")
        self.assertEqual(headers.get("Connection"), "close")
        self.assertFalse(any(key.lower().startswith("access-control-") for key in headers))
        self.assertNotIn("Server", headers)
        self.assertNotIn("Set-Cookie", headers)

    def redeem(self, issuance, **kwargs):
        return self.request("/v1/redeem", payload={"nonce": self.nonce, "code": issuance["code"]}, **kwargs)

    def assert_inactive(self, response, nonce=None):
        status, payload, _ = response
        self.assertEqual(status, 403)
        self.assertEqual(payload, {"error": "inactive", "nonce": self.nonce if nonce is None else nonce})

    def assert_bad(self, response):
        status, payload, _ = response
        self.assertEqual(status, 400)
        self.assertEqual(payload, {"error": "bad_request"})

    def test_issue_redeem_validate_exact_contract_and_no_credentials(self):
        issuance = self.authority.issue(60)
        self.clock.advance(1250)
        status, grant, headers = self.redeem(issuance)
        self.assertEqual(status, 200)
        self.assertEqual(set(grant), SUCCESS_KEYS)
        self.assertEqual(grant, {"nonce": self.nonce, "subject": self.subject,
                                "audience": "com.knoux.kxprivatevpn",
                                "issuedAtMillis": self.clock(),
                                "expiresAtMillis": issuance["expiresAtMillis"],
                                "authorityNowMillis": self.clock(), "revoked": False,
                                "features": ["PRO_AUTOMATION"]})
        self.assertIs(grant["revoked"], False)
        for field in ("issuedAtMillis", "expiresAtMillis", "authorityNowMillis"):
            self.assertIs(type(grant[field]), int)
        self.assertTrue(issuance["code"] not in json.dumps(grant))
        self.assertTrue(self.secret not in json.dumps(grant))
        self.assertEqual(int(headers["Content-Length"]), len(pro._json_bytes(grant)))
        self.assertEqual(self.request()[1], grant)

    def test_retry_after_lost_response_retains_activation_and_updates_nonce(self):
        issuance = self.authority.issue(60)
        # Send a complete redemption but discard the HTTP response. A subsequent
        # response is not necessary to commit the original SQLite transaction.
        body = pro._json_bytes({"nonce": self.nonce, "code": issuance["code"]})
        sent = threading.Event()
        original = self.authority.redeem

        def redeem_and_signal(*args):
            result = original(*args)
            sent.set()
            return result

        with patch.object(self.authority, "redeem", side_effect=redeem_and_signal):
            with socket.create_connection(self.server.server_address, timeout=5) as connection:
                connection.sendall(self.raw_headers(b"", body=body,
                                                    request_line=b"POST /v1/redeem HTTP/1.1"))
                self.assertTrue(sent.wait(timeout=3))
        activated_at = self.clock()
        self.clock.advance(500)
        fresh_nonce = secrets.token_hex(32)
        status, retried, _ = self.request("/v1/redeem", payload={"nonce": fresh_nonce, "code": issuance["code"]})
        self.assertEqual(status, 200)
        self.assertEqual(retried["nonce"], fresh_nonce)
        self.assertEqual(retried["issuedAtMillis"], activated_at)
        self.assertEqual(retried["authorityNowMillis"], self.clock())
        self.assertEqual(retried["expiresAtMillis"], issuance["expiresAtMillis"])

    def test_unknown_validate_and_wrong_binding_are_indistinguishable(self):
        self.assert_inactive(self.request())
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        other_secret = secrets.token_hex(32)
        self.assert_inactive(self.redeem(issuance, secret=other_secret))
        self.assert_inactive(self.request(secret=other_secret))
        self.assert_inactive(self.redeem({"code": secrets.token_hex(32)}))
        self.assertEqual(self.request()[0], 200)

    def test_expiry_boundary_is_enforced_on_redeem_and_validate(self):
        issuance = self.authority.issue(1)
        self.assertEqual(self.redeem(issuance)[0], 200)
        self.clock.advance(999)
        self.assertEqual(self.request()[0], 200)
        self.clock.advance(1)
        self.assert_inactive(self.request())
        self.assert_inactive(self.redeem(issuance))
        self.clock.advance(-1000)
        self.assert_inactive(self.redeem(issuance))
        self.assert_inactive(self.request())

    def test_unredeemed_expired_code_is_terminal(self):
        issuance = self.authority.issue(1)
        self.clock.advance(1000)
        self.assert_inactive(self.redeem(issuance))
        self.clock.advance(-1000)
        self.assert_inactive(self.redeem(issuance))

    def test_revoke_subject_and_replay_cannot_resurrect(self):
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        self.assertTrue(self.authority.revoke(subject=self.subject))
        self.assert_inactive(self.request())
        self.assert_inactive(self.redeem(issuance))
        self.assert_inactive(self.redeem(issuance, secret=secrets.token_hex(32)))
        fresh = self.authority.issue(120)
        self.assertEqual(self.redeem(fresh)[0], 200)
        self.assert_inactive(self.redeem(issuance))
        self.assertEqual(self.request()[1]["expiresAtMillis"], fresh["expiresAtMillis"])

    def test_revoke_code_id_including_before_activation(self):
        fresh = self.authority.issue(60)
        self.assertTrue(self.authority.revoke(code_id=fresh["codeId"]))
        self.assert_inactive(self.redeem(fresh))
        active = self.authority.issue(60)
        self.assertEqual(self.redeem(active)[0], 200)
        self.assertTrue(self.authority.revoke(code_id=active["codeId"]))
        self.assert_inactive(self.redeem(active))
        self.assert_inactive(self.request())
        self.assertFalse(self.authority.revoke(code_id=99999))

    def test_renewal_supersedes_atomically_and_old_code_never_restores(self):
        old = self.authority.issue(60)
        self.assertEqual(self.redeem(old)[0], 200)
        self.clock.advance(2000)
        new = self.authority.issue(120)
        status, grant, _ = self.redeem(new)
        self.assertEqual(status, 200)
        self.assertEqual(grant["issuedAtMillis"], self.clock())
        self.assert_inactive(self.redeem(old))
        self.assertTrue(self.authority.revoke(code_id=old["codeId"]))
        self.assertEqual(self.request()[1], grant)
        self.authority.revoke(code_id=new["codeId"])
        self.assert_inactive(self.redeem(old))
        self.assert_inactive(self.redeem(new))
        self.assert_inactive(self.request())

    def test_invalid_or_other_bound_code_cannot_displace_active_grant(self):
        old = self.authority.issue(60)
        self.assertEqual(self.redeem(old)[0], 200)
        expired = self.authority.issue(1)
        self.clock.advance(1000)
        self.assert_inactive(self.redeem(expired))
        other = self.authority.issue(120)
        self.assertEqual(self.redeem(other, secret=secrets.token_hex(32))[0], 200)
        self.assert_inactive(self.redeem(other))
        self.assertEqual(self.request()[1]["expiresAtMillis"], old["expiresAtMillis"])

    def test_parallel_redemption_has_exactly_one_winning_installation(self):
        issuance = self.authority.issue(60)
        candidates = [secrets.token_hex(32) for _ in range(12)]
        barrier = threading.Barrier(len(candidates))

        def attempt(candidate):
            barrier.wait(timeout=5)
            return self.redeem(issuance, secret=candidate)[0]

        with ThreadPoolExecutor(max_workers=len(candidates)) as executor:
            statuses = list(executor.map(attempt, candidates))
        self.assertEqual(statuses.count(200), 1)
        self.assertEqual(statuses.count(403), len(candidates) - 1)
        winner = candidates[statuses.index(200)]
        self.assertEqual(self.redeem(issuance, secret=winner)[0], 200)
        self.assertEqual(self.request(secret=winner)[0], 200)
        with closing(sqlite3.connect(self.database)) as connection, connection:
            self.assertEqual(connection.execute("SELECT count(*) FROM codes WHERE state='active'").fetchone()[0], 1)

    def test_parallel_same_installation_retries_are_idempotent(self):
        issuance = self.authority.issue(60)
        with ThreadPoolExecutor(max_workers=8) as executor:
            responses = list(executor.map(lambda _: self.redeem(issuance), range(8)))
        self.assertEqual([response[0] for response in responses], [200] * len(responses))
        self.assertTrue(all(response[1] == responses[0][1] for response in responses))

    def test_simultaneous_fresh_renewals_leave_one_active_grant(self):
        codes = [self.authority.issue(60 + index) for index in range(8)]
        with ThreadPoolExecutor(max_workers=8) as executor:
            responses = list(executor.map(self.redeem, codes))
        self.assertEqual([response[0] for response in responses], [200] * len(responses))
        replays = [self.redeem(issuance)[0] for issuance in codes]
        self.assertEqual(replays.count(200), 1)
        self.assertEqual(replays.count(403), 7)

    def test_peer_limit_counts_bad_headers_and_ignores_proxy_headers(self):
        self.server.limiter = pro.RateLimiter(peer_limit=2, subject_limit=100, window_seconds=10,
                                              clock=self.rate_clock)
        self.assert_bad(self.request(headers={"Authorization": "invalid"}))
        self.assert_bad(self.request(headers={"Authorization": "invalid", "X-Forwarded-For": "192.0.2.1"}))
        status, body, headers = self.request(secret=secrets.token_hex(32), headers={
            "X-Forwarded-For": "192.0.2.2", "Forwarded": "for=192.0.2.3", "X-Real-IP": "192.0.2.4"})
        self.assertEqual((status, body), (429, {"error": "rate_limited"}))
        self.assertEqual(headers["Retry-After"], "10")
        self.rate_clock.advance(9)
        self.assertEqual(self.request()[0], 429)
        self.rate_clock.advance(1)
        self.assert_inactive(self.request())

    def test_subject_limit_shared_routes_counts_malformed_and_invalid_codes(self):
        self.server.limiter = pro.RateLimiter(peer_limit=100, subject_limit=3, window_seconds=10,
                                              clock=self.rate_clock)
        self.assert_bad(self.redeem({"code": "bad"}))
        self.assert_inactive(self.redeem({"code": secrets.token_hex(32)}))
        self.assert_inactive(self.request())
        response = self.redeem({"code": secrets.token_hex(32)})
        self.assertEqual(response[:2], (429, {"error": "rate_limited"}))
        self.assertEqual(response[2]["Retry-After"], "10")
        self.assert_inactive(self.request(secret=secrets.token_hex(32)))
        self.rate_clock.advance(10)
        self.assert_inactive(self.request())

    def test_malformed_http_requests_count_against_peer(self):
        self.server.limiter = pro.RateLimiter(peer_limit=1, clock=self.rate_clock)
        self.assert_bad(self.raw_request(b"broken request syntax here\r\n\r\n"))
        self.assertEqual(self.request()[0], 429)

    def test_json_malformed_duplicate_unknown_and_type_confusion(self):
        nonce = self.nonce
        bodies = [b"", b"{", b"\xff", b"null", b"[]", b"true", b"42",
                  b'{"nonce":NaN}', b'{"nonce":Infinity}',
                  b'{"nonce":null}', b'{"nonce":true}', b'{"nonce":12}',
                  b'{"nonce":[]}', b'{"nonce":{}}', b'{"nonce":"abcd"}',
                  json.dumps({"nonce": nonce.upper()}).encode("ascii"),
                  json.dumps({"nonce": nonce, "extra": 1}).encode("ascii"),
                  json.dumps({"nonce": nonce, "code": secrets.token_hex(32)}).encode("ascii"),
                  ('{"nonce":"' + nonce + '","nonce":"' + nonce + '"}').encode("ascii"),
                  ('{"nonce":"' + nonce + '","no\\u006ece":"' + nonce + '"}').encode("ascii"),
                  ('{"nonce":"' + nonce + '"} {}').encode("ascii"),
                  b"[" * 1500 + b"]" * 1500,
                  b'{"nonce":"' + b"a" * 64 + b'\\n"}']
        for index, body in enumerate(bodies):
            with self.subTest(case=index):
                self.assert_bad(self.request(body=body))
        redeem_bodies = [{"nonce": nonce}, {"nonce": nonce, "code": 123},
                         {"nonce": nonce, "code": True}, {"nonce": nonce, "code": None},
                         {"nonce": nonce, "code": "A" * 64},
                         {"nonce": nonce, "code": "a" * 63},
                         {"nonce": nonce, "code": "a" * 65}]
        for index, payload in enumerate(redeem_bodies):
            with self.subTest(redeem_case=index):
                self.assert_bad(self.request("/v1/redeem", payload=payload))
        duplicate = ('{"nonce":"' + nonce + '","code":"' + 'a' * 64
                     + '","code":"' + 'b' * 64 + '"}').encode("ascii")
        self.assert_bad(self.request("/v1/redeem", body=duplicate))

    def test_body_limit_exact_boundary_and_over_limit(self):
        body = json.dumps({"nonce": self.nonce}).encode("ascii")
        self.assert_inactive(self.request(body=body + b" " * (4096 - len(body))))
        self.assert_bad(self.request(body=body + b" " * (4097 - len(body))))
        self.assert_bad(self.raw_request(self.raw_headers(b"", body=b"x" * 4097)))

    def test_methods_routes_and_content_types_rejected(self):
        for method in ("GET", "PUT", "DELETE", "PATCH", "OPTIONS", "TRACE", "CONNECT", "CUSTOM"):
            with self.subTest(method=method):
                self.assert_bad(self.request(method=method))
        self.assertEqual(self.request(method="HEAD")[:2], (400, None))
        for route in ("/", "/v1/revoke", "/v1/issue", "/v1/validate?nonce=x", "/v1/validate/",
                      "//v1/validate", "http://localhost/v1/validate", "/v1/%76alidate"):
            with self.subTest(route=route):
                self.assert_bad(self.request(route))
        for content_type in ("", "text/plain", "application/json; charset=latin1", "application/json; other=x"):
            with self.subTest(content_type=content_type):
                self.assert_bad(self.request(headers={"Content-Type": content_type}))
        self.assert_inactive(self.request(headers={"Content-Type": "application/json; charset=utf-8"}))

    def test_authorization_format_missing_and_duplicate_headers(self):
        for index, value in enumerate(("", "Basic xyz", "Bearer " + "a" * 63,
                                       "Bearer " + "A" * 64, "Bearer  " + "a" * 64)):
            with self.subTest(case=index):
                self.assert_bad(self.request(headers={"Authorization": value}))
        body = json.dumps({"nonce": self.nonce}).encode("ascii")
        missing = (b"POST /v1/validate HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n"
                   + f"Content-Length: {len(body)}\r\n\r\n".encode("ascii") + body)
        self.assert_bad(self.raw_request(missing))
        for index, extra in enumerate((b"Authorization: Bearer " + self.secret.encode("ascii") + b"\r\n",
                                       b"Content-Length: 2\r\n", b"Content-Type: application/json\r\n",
                                       b"Transfer-Encoding: chunked\r\n", b"Expect: 100-continue\r\n",
                                       b" Folded-header\r\n", b"Malformed-header\r\n")):
            with self.subTest(header_case=index):
                self.assert_bad(self.raw_request(self.raw_headers(extra)))

    def test_bad_http_framing_is_sanitized(self):
        cases = [b"nonsense\r\n\r\n", b"POST /v1/validate HTTP/9.9\r\n\r\n",
                 b"POST " + b"x" * 8193 + b" HTTP/1.1\r\n\r\n",
                 self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2", b"Content-Length: -1"),
                 self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2", b"Content-Length: +2"),
                 self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2", b"Content-Length: 9999999999"),
                 self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2", b"Content-Length: 100"),
                 self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2\r\n", b"")]
        for index, data in enumerate(cases):
            with self.subTest(case=index):
                self.assert_bad(self.raw_request(data))

    def test_unhandled_errors_are_503_and_never_logged(self):
        output, errors = io.StringIO(), io.StringIO()
        with redirect_stdout(output), redirect_stderr(errors):
            for error in (RuntimeError(self.secret), OSError(self.secret), TimeoutError(self.secret),
                          ConnectionError(self.secret), sqlite3.DatabaseError(self.secret)):
                with patch.object(self.authority, "validate", side_effect=error):
                    self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))
            with patch.object(self.authority, "validate", return_value={"invalid": object()}):
                self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))
            self.assert_bad(self.request("/" + self.secret))
            self.assert_bad(self.request(body=self.secret.encode("ascii")))
        self.assertEqual(output.getvalue(), "")
        self.assertEqual(errors.getvalue(), "")

    def test_incomplete_header_timeout_is_sanitized_and_rate_counted(self):
        self.server.socket_timeout = 0.05
        self.server.limiter = pro.RateLimiter(peer_limit=1, clock=self.rate_clock)
        with socket.create_connection(self.server.server_address, timeout=5) as connection:
            connection.sendall(b"POST /v1/validate HTTP/1.1\r\nHost: ")
            response = http.client.HTTPResponse(connection)
            response.begin()
            self.assertEqual(response.status, 400)
            self.assert_security_headers(dict(response.getheaders()))
            self.assertEqual(json.loads(response.read()), {"error": "bad_request"})
        self.assertEqual(self.request()[0], 429)

    def test_incomplete_body_timeout_is_sanitized_and_rate_counted(self):
        self.server.socket_timeout = 0.05
        self.server.limiter = pro.RateLimiter(peer_limit=100, subject_limit=1, clock=self.rate_clock)
        data = self.raw_headers(b"", body=b"{}").replace(b"Content-Length: 2", b"Content-Length: 100")
        with socket.create_connection(self.server.server_address, timeout=5) as connection:
            connection.sendall(data)
            response = http.client.HTTPResponse(connection)
            response.begin()
            self.assertEqual(response.status, 400)
            self.assert_security_headers(dict(response.getheaders()))
            self.assertEqual(json.loads(response.read()), {"error": "bad_request"})
        self.assertEqual(self.request()[0], 429)

    def assert_progress_deadline(self, initial, *, finish_headers=None):
        # One available worker makes a leaked permit observable on the next request.
        self.server._slots = threading.BoundedSemaphore(1)
        # Leave the idle deadline beyond the absolute deadline, then stop writing
        # before the latter. Sending during a server close can cause a TCP reset
        # on Windows and discard the response, obscuring the deadline assertion.
        self.server.socket_timeout = 0.75
        self.server.request_timeout = 0.6
        self.server.limiter = pro.RateLimiter(peer_limit=1, subject_limit=1,
                                              window_seconds=10, clock=self.rate_clock)
        started_threads = []
        original_start = threading.Thread.start

        def record_start(thread):
            started_threads.append(thread)
            original_start(thread)

        output, errors = io.StringIO(), io.StringIO()
        with (patch.object(threading.Thread, "start", new=record_start),
              patch.object(self.authority, "redeem", wraps=self.authority.redeem) as redeem,
              patch.object(self.authority, "validate", wraps=self.authority.validate) as validate,
              redirect_stdout(output), redirect_stderr(errors)):
            try:
                with socket.create_connection(self.server.server_address, timeout=2) as connection:
                    started = time.monotonic()
                    connection.sendall(initial)
                    progress = 0
                    transitioned = False
                    while time.monotonic() - started < 0.9:
                        # Keep supplying bytes well inside the inactivity timeout.
                        if select.select([connection], [], [], 0.03)[0]:
                            break
                        # No EOF and no writes racing the 0.6s close. Without the
                        # absolute deadline, idle expiry is after 1.15s and must
                        # still fail the unchanged 0.9s response bound below.
                        elapsed = time.monotonic() - started
                        if elapsed >= 0.5:
                            continue
                        if finish_headers is not None and elapsed >= 0.4:
                            connection.sendall(finish_headers)
                            finish_headers = None
                            transitioned = True
                        else:
                            connection.sendall(b" ")
                            progress += 1
                    else:
                        self.fail("Continuing input retained the worker beyond the absolute deadline")
                    self.assertGreaterEqual(progress, 3)
                    self.assertLess(time.monotonic() - started, 0.9)
                    self.assertIsNone(finish_headers)
                    with http.client.HTTPResponse(connection) as response:
                        response.begin()
                        self.assertEqual(response.status, 400)
                        self.assert_security_headers(dict(response.getheaders()))
                        self.assertEqual(json.loads(response.read()), {"error": "bad_request"})
                    if transitioned:
                        # Headers finished at ~0.4s: a fresh body deadline would
                        # last until ~1.0s and fail the shared 0.9s upper bound.
                        self.assertGreaterEqual(time.monotonic() - started, 0.4)
                for thread in started_threads:
                    thread.join(timeout=2)
                    self.assertFalse(thread.is_alive())
                self.assertEqual((redeem.call_count, validate.call_count), (0, 0))
                self.assertEqual(self.request()[:2], (429, {"error": "rate_limited"}))
                self.assertEqual((redeem.call_count, validate.call_count), (0, 0))
                self.rate_clock.advance(10)
                self.assert_inactive(self.request())
            finally:
                for thread in started_threads:
                    thread.join(timeout=2)
                    self.assertFalse(thread.is_alive())
        # Exactly the slow request and two probes: no watchdog/timer/per-byte
        # threads, including delayed threads left behind by fast responses.
        self.assertEqual(len(started_threads), 3)
        self.assertFalse(any(isinstance(thread, threading.Timer) for thread in started_threads))
        self.assertEqual(output.getvalue(), "")
        self.assertEqual(errors.getvalue(), "")

    def test_progressing_request_line_hits_absolute_deadline(self):
        self.assert_progress_deadline(b"POST /v1/validate")

    def test_progressing_headers_hit_absolute_deadline(self):
        self.assert_progress_deadline(b"POST /v1/validate HTTP/1.1\r\nHost: "
                                      + self.secret.encode("ascii"))

    def test_progressing_body_hits_absolute_deadline(self):
        issuance = self.authority.issue(60)
        body = pro._json_bytes({"nonce": self.nonce, "code": issuance["code"]})
        initial = self.raw_headers(b"", body=body, request_line=b"POST /v1/redeem HTTP/1.1")
        initial = initial.replace(f"Content-Length: {len(body)}".encode("ascii"),
                                  b"Content-Length: 4096")
        self.assert_progress_deadline(initial)
        # The timed-out redemption never bound this still-usable fresh code.
        self.rate_clock.advance(10)
        self.assertEqual(self.redeem(issuance, secret=secrets.token_hex(32))[0], 200)

    def test_headers_and_body_share_absolute_deadline(self):
        body = pro._json_bytes({"nonce": self.nonce})
        initial = self.raw_headers(b"", body=body).split(b"\r\n\r\n", 1)[0]
        initial = initial.replace(f"Content-Length: {len(body)}".encode("ascii"),
                                  b"Content-Length: 4096") + b"\r\nX-Padding: "
        self.assert_progress_deadline(initial, finish_headers=b"\r\n\r\n" + body)

    def test_deadline_rechecked_after_buffered_json_before_authority(self):
        issuance = self.authority.issue(60)
        for route, payload in (("/v1/redeem", {"nonce": self.nonce, "code": issuance["code"]}),
                               ("/v1/validate", {"nonce": self.nonce})):
            with self.subTest(route=route):
                deadline_clock = Clock(100)
                original_loads = json.loads

                def expire_during_json(*args, **kwargs):
                    result = original_loads(*args, **kwargs)
                    if "object_pairs_hook" in kwargs:
                        deadline_clock.advance(10)
                    return result

                # A complete real-socket request may be entirely buffered. Move
                # only the server's monotonic clock while validating its JSON.
                with (patch.object(pro, "time") as server_time,
                      patch.object(pro.json, "loads", side_effect=expire_during_json),
                      patch.object(self.authority, "redeem", wraps=self.authority.redeem) as redeem,
                      patch.object(self.authority, "validate", wraps=self.authority.validate) as validate):
                    server_time.monotonic.side_effect = deadline_clock
                    response = self.request(route, payload=payload)
                    self.assertEqual((redeem.call_count, validate.call_count), (0, 0))
                    self.assert_bad(response)
        self.assertEqual(self.redeem(issuance, secret=secrets.token_hex(32))[0], 200)

    def test_sqlite_lock_unavailable_then_recovers(self):
        # Only the deliberate lock-outage test needs a shortened wait.
        self.authority.busy_timeout = 0.1
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        connection = sqlite3.connect(self.database, isolation_level=None)
        try:
            connection.execute("BEGIN EXCLUSIVE")
            self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))
        finally:
            connection.rollback()
            connection.close()
        self.assertEqual(self.request()[0], 200)

    def test_missing_database_not_recreated_and_recovers(self):
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        moved = self.database.with_suffix(".moved")
        self.database.rename(moved)
        try:
            self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))
            self.assertFalse(self.database.exists())
        finally:
            moved.rename(self.database)
        self.assertEqual(self.request()[0], 200)

    def test_failed_renewal_rolls_back_supersession(self):
        old = self.authority.issue(60)
        self.assertEqual(self.redeem(old)[0], 200)
        fresh = self.authority.issue(120)
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.execute("""CREATE TRIGGER reject_activation BEFORE UPDATE OF state ON codes
                                  WHEN NEW.state = 'active'
                                  BEGIN SELECT RAISE(FAIL, 'test transaction failure'); END""")
        self.assertEqual(self.redeem(fresh)[:2], (503, {"error": "unavailable"}))
        self.assertEqual(self.request()[1]["expiresAtMillis"], old["expiresAtMillis"])
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.execute("DROP TRIGGER reject_activation")
        self.assertEqual(self.redeem(fresh)[0], 200)
        self.assert_inactive(self.redeem(old))

    def test_persistence_correct_pepper_and_wrong_pepper_refused_without_writes(self):
        issuance = self.authority.issue(60)
        status, grant, _ = self.redeem(issuance)
        self.assertEqual(status, 200)
        before = self.database.read_bytes()
        with self.assertRaises(pro.ConfigurationError):
            pro.Authority(self.database, secrets.token_bytes(32), initialize=True)
        self.assertEqual(before, self.database.read_bytes())
        self.server.authority = pro.Authority(self.database, self.pepper, clock=self.clock)
        self.assertEqual(self.request()[1], grant)
        self.assertEqual(self.redeem(issuance)[1], grant)
        self.server.authority.revoke(subject=self.subject)
        self.server.authority = pro.Authority(self.database, self.pepper, clock=self.clock)
        self.assert_inactive(self.request())
        self.assert_inactive(self.redeem(issuance))

    def test_verifier_checked_on_each_http_operation(self):
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.execute("UPDATE metadata SET pepper_verifier = ?", ("0" * 64,))
        self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))
        self.assertEqual(self.redeem({"code": secrets.token_hex(32)})[:2], (503, {"error": "unavailable"}))

    def test_database_holds_only_code_hmac_and_subject_hash(self):
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        with closing(sqlite3.connect(self.database)) as connection, connection:
            row = connection.execute("SELECT code_mac, subject FROM codes").fetchone()
            expected = hmac.new(self.pepper, issuance["code"].encode("ascii"), hashlib.sha256).hexdigest()
            self.assertTrue(hmac.compare_digest(row[0], expected))
            self.assertEqual(row[1], hashlib.sha256(self.secret.encode("ascii")).hexdigest())
            self.assertEqual(connection.execute("PRAGMA integrity_check").fetchone()[0], "ok")
        stored = self.database.read_bytes()
        self.assertTrue(issuance["code"].encode("ascii") not in stored)
        self.assertTrue(self.secret.encode("ascii") not in stored)
        self.assertTrue(self.pepper.hex().encode("ascii") not in stored)
        self.assertTrue(b"127.0.0.1" not in stored)

    def test_code_generation_collision_retries_with_unique_constraint(self):
        original = self.authority.issue(60)
        next_code = secrets.token_hex(32)
        with patch.object(pro.secrets, "token_hex", side_effect=[original["code"], next_code]) as random:
            second = self.authority.issue(60)
        self.assertEqual(random.call_args_list[0].args, (32,))
        self.assertEqual(random.call_count, 2)
        self.assertNotEqual(original["codeId"], second["codeId"])
        self.assertTrue(hmac.compare_digest(second["code"], next_code))
        with patch.object(pro.secrets, "token_hex", return_value=original["code"]):
            with self.assertRaises(pro.ConfigurationError):
                self.authority.issue(60)

    def test_duration_bounds_and_admin_argument_validation(self):
        for duration in (0, -1, True, 1.5, "60", pro.MAX_DURATION_SECONDS + 1):
            with self.subTest(duration=duration):
                with self.assertRaises(pro.ConfigurationError):
                    self.authority.issue(duration)
        maximum = self.authority.issue(pro.MAX_DURATION_SECONDS)
        self.assertEqual(maximum["expiresAtMillis"] - self.clock(), 366 * 86400 * 1000)
        for arguments in ({}, {"subject": self.subject, "code_id": 1}, {"subject": "invalid"},
                          {"code_id": 0}, {"code_id": True}, {"code_id": 2**63}):
            with self.assertRaises(pro.ConfigurationError):
                self.authority.revoke(**arguments)

    def test_wallclock_before_activation_fails_closed(self):
        issuance = self.authority.issue(60)
        self.assertEqual(self.redeem(issuance)[0], 200)
        self.clock.advance(-1)
        self.assertEqual(self.request()[:2], (503, {"error": "unavailable"}))

    def test_non_loopback_bind_refused_and_default_is_ephemeral(self):
        self.assertEqual(self.server.server_address[0], "127.0.0.1")
        self.assertGreater(self.server.server_address[1], 0)
        for address in ("0.0.0.0", "192.0.2.1", "localhost", "::1"):
            with self.subTest(address=address):
                with self.assertRaises(pro.ConfigurationError):
                    pro.ProHTTPServer(self.authority, bind=address)

    def test_worker_capacity_returns_sanitized_503_then_recovers(self):
        # Exhaust the worker permits without depending on scheduler timing.
        acquired = 0
        while self.server._slots.acquire(blocking=False):
            acquired += 1
        try:
            with socket.create_connection(self.server.server_address, timeout=5) as connection:
                response = http.client.HTTPResponse(connection)
                response.begin()
                self.assertEqual(response.status, 503)
                self.assert_security_headers(dict(response.getheaders()))
                self.assertEqual(json.loads(response.read()), {"error": "unavailable"})
        finally:
            for _ in range(acquired):
                self.server._slots.release()
        self.assert_inactive(self.request())


class RateLimiterTests(unittest.TestCase):
    def test_bounded_storage_no_eviction_bypass_and_eventual_recovery(self):
        clock = Clock(0)
        limiter = pro.RateLimiter(peer_limit=100, subject_limit=100, max_buckets=3,
                                  window_seconds=10, clock=clock)
        self.assertTrue(limiter.allow("peer", "subject1"))
        self.assertTrue(limiter.allow("peer", "subject2"))
        for number in range(100):
            self.assertFalse(limiter.allow("peer", "different" + str(number)))
        self.assertEqual(len(limiter._buckets), 3)
        self.assertFalse(limiter.allow("peer", "subject1"))
        clock.advance(10)
        self.assertTrue(limiter.allow("peer", "new"))
        self.assertLessEqual(len(limiter._buckets), 3)

    def test_subject_budget_applies_across_different_direct_peers(self):
        limiter = pro.RateLimiter(peer_limit=100, subject_limit=1)
        self.assertTrue(limiter.allow("127.0.0.1", "subject"))
        self.assertFalse(limiter.allow("127.0.0.2", "subject"))
        self.assertTrue(limiter.allow("127.0.0.2", "another-subject"))

    def test_thread_safety_exact_budget(self):
        limiter = pro.RateLimiter(peer_limit=100, subject_limit=7)
        with ThreadPoolExecutor(max_workers=16) as executor:
            allowed = list(executor.map(lambda _: limiter.allow("peer", "subject"), range(100)))
        self.assertEqual(sum(allowed), 7)
        self.assertEqual(len(limiter._buckets), 2)


class CLITests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="pro-cli-test-", dir=HERE)
        self.addCleanup(self.temporary.cleanup)
        self.database = Path(self.temporary.name) / "cli.sqlite3"
        self.pepper = secrets.token_hex(32)
        self.environment = dict(os.environ, KX_PRO_PEPPER=self.pepper, PYTHONDONTWRITEBYTECODE="1")

    def run_cli(self, *arguments, environment=None):
        return subprocess.run([sys.executable, "-B", str(SCRIPT), *arguments],
                              env=self.environment if environment is None else environment,
                              capture_output=True, text=True, timeout=10)

    def test_help_and_subcommand_help(self):
        for command in ((), ("issue",), ("serve",), ("revoke",)):
            result = self.run_cli(*command, "--help", environment={**self.environment, "KX_PRO_PEPPER": ""})
            self.assertEqual(result.returncode, 0)
            self.assertIn("usage:", result.stdout)
            self.assertEqual(result.stderr, "")
        self.assertIn("TLS", self.run_cli("--help").stdout)

    def test_missing_invalid_pepper_never_printed_or_defaulted(self):
        for index, value in enumerate((None, "", "short", "g" * 64, "a" * 63, "a" * 65, " " + self.pepper)):
            environment = dict(self.environment)
            if value is None:
                environment.pop("KX_PRO_PEPPER", None)
            else:
                environment["KX_PRO_PEPPER"] = value
            with self.subTest(case=index):
                result = self.run_cli("issue", "--db", str(self.database), "--duration-seconds", "60",
                                      environment=environment)
                self.assertEqual(result.returncode, 1)
                self.assertEqual(result.stdout, "")
                self.assertEqual(result.stderr, "error: authority unavailable or configuration invalid\n")
                self.assertFalse(self.database.exists())

    def test_existing_empty_or_unrelated_database_refused(self):
        self.database.touch()
        with self.assertRaises(sqlite3.DatabaseError):
            pro.Authority(self.database, bytes.fromhex(self.pepper), initialize=True)
        with closing(sqlite3.connect(self.database)) as connection, connection:
            connection.execute("CREATE TABLE unrelated (id INTEGER)")
        with self.assertRaises(sqlite3.DatabaseError):
            pro.Authority(self.database, bytes.fromhex(self.pepper), initialize=True)
        with closing(sqlite3.connect(self.database)) as connection, connection:
            tables = connection.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
        self.assertEqual(tables, [("unrelated",)])

    def test_cli_sanitizes_invalid_arguments_and_wrong_pepper(self):
        invalid = self.run_cli("issue", "--db", str(self.database), "--duration-seconds", self.pepper)
        self.assertEqual(invalid.returncode, 2)
        self.assertEqual(invalid.stderr, "error: invalid arguments (use --help)\n")
        result = self.run_cli("issue", "--db", str(self.database), "--duration-seconds", "60")
        self.assertEqual(result.returncode, 0)
        wrong = self.run_cli("serve", "--db", str(self.database),
                             environment=dict(self.environment, KX_PRO_PEPPER=secrets.token_hex(32)))
        self.assertEqual(wrong.returncode, 1)
        self.assertEqual(wrong.stdout, "")
        self.assertEqual(wrong.stderr, "error: authority unavailable or configuration invalid\n")

    def test_real_cli_issue_serve_redeem_validate_revoke_and_restart(self):
        result = self.run_cli("issue", "--db", str(self.database), "--duration-seconds", "60")
        self.assertEqual(result.returncode, 0)
        self.assertEqual(result.stderr, "")
        self.assertEqual(len(result.stdout.splitlines()), 1)
        issuance = json.loads(result.stdout)
        self.assertEqual(set(issuance), {"code", "codeId", "expiresAtMillis"})
        self.assertTrue(pro.is_hex64(issuance["code"]))
        secret, nonce = secrets.token_hex(32), secrets.token_hex(32)
        for round_number in range(2):
            process = subprocess.Popen([sys.executable, "-B", str(SCRIPT), "serve", "--db", str(self.database)],
                                       env=self.environment, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       text=True)
            try:
                # Bounded startup read; no sleeps or unbounded readline if startup fails.
                with ThreadPoolExecutor(max_workers=1) as executor:
                    future = executor.submit(process.stdout.readline)
                    try:
                        line = future.result(timeout=5)
                    except Exception:
                        process.terminate()
                        process.wait(timeout=5)
                        raise
                listening = json.loads(line)
                self.assertEqual(listening["listening"], "127.0.0.1")
                self.assertGreater(listening["port"], 0)
                self.assertIs(listening["tlsRequiredForPublicAccess"], True)

                def request(route, payload):
                    connection = http.client.HTTPConnection("127.0.0.1", listening["port"], timeout=5)
                    try:
                        connection.request("POST", route, body=json.dumps(payload), headers={
                            "Authorization": "Bearer " + secret, "Content-Type": "application/json"})
                        response = connection.getresponse()
                        self.assertEqual(response.getheader("Cache-Control"), "no-store")
                        self.assertEqual(response.getheader("X-Content-Type-Options"), "nosniff")
                        return response.status, json.loads(response.read())
                    finally:
                        connection.close()

                if round_number == 0:
                    self.assertEqual(request("/v1/redeem", {"nonce": nonce, "code": issuance["code"]})[0], 200)
                    self.assertEqual(request("/v1/validate", {"nonce": nonce})[0], 200)
                    revoked = self.run_cli("revoke", "--db", str(self.database), "--code-id", str(issuance["codeId"]))
                    self.assertEqual(revoked.returncode, 0)
                    self.assertEqual(json.loads(revoked.stdout), {"revoked": True})
                    self.assertEqual(revoked.stderr, "")
                self.assertEqual(request("/v1/validate", {"nonce": nonce}),
                                 (403, {"error": "inactive", "nonce": nonce}))
                self.assertEqual(request("/v1/redeem", {"nonce": nonce, "code": issuance["code"]}),
                                 (403, {"error": "inactive", "nonce": nonce}))
            finally:
                process.terminate()
                remaining, errors = process.communicate(timeout=5)
                self.assertEqual(remaining, "")
                self.assertEqual(errors, "")


if __name__ == "__main__":
    unittest.main()
