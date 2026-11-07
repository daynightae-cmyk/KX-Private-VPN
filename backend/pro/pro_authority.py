"""Local, standard-library Pro authority; not a VPN gateway or deployed service.

Run `python pro_authority.py --help` for the local administration commands.
Public access MUST use separately managed TLS termination. The backend only binds
IPv4 loopback, ignores proxy headers, emits no request logs, and closes every HTTP
connection. Protect the database and KX_PRO_PEPPER with operating-system access
controls. Keep the pepper stable across restarts; no secret is generated for it.

Code duration starts at issuance (absolute expiry), not activation. Activation is
issuedAtMillis. Expired, revoked and superseded codes are terminal; a new fresh
code is the only way to renew. Each installation has at most one active grant.
The process-local rate limiter is shared across both routes and resets on restart;
run one authority process per database, behind a TLS terminator for public use.
"""

import argparse
from contextlib import contextmanager
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import sqlite3
import sys
import threading
import time


AUDIENCE = "com.knoux.kxprivatevpn"
FEATURES = ["PRO_AUTOMATION"]
MAX_BODY_BYTES = 4096
MAX_DURATION_SECONDS = 366 * 24 * 60 * 60
HEX64 = re.compile(r"[0-9a-f]{64}\Z", re.ASCII)
AUTHORIZATION = re.compile(r"(?i:Bearer) ([0-9a-f]{64})\Z", re.ASCII)
CONTENT_TYPE = re.compile(
    r'application/json(?:\s*;\s*charset\s*=\s*(?:utf-8|"utf-8"))?\Z', re.I
)
VERIFIER_CONTEXT = b"KX Pro authority pepper verifier v1"


class ConfigurationError(Exception):
    """Invalid local configuration; never contains secret material."""


class Inactive(Exception):
    """Indistinguishable invalid, unbound, expired, replaced or revoked grant."""


class BadRequest(Exception):
    """Malformed client input; never contains client material."""


def wallclock_millis():
    return time.time_ns() // 1_000_000


def is_hex64(value):
    return type(value) is str and HEX64.fullmatch(value) is not None


def subject_for_secret(secret):
    if not is_hex64(secret):
        raise BadRequest()
    return hashlib.sha256(secret.encode("ascii")).hexdigest()


def pepper_from_env():
    value = os.environ.get("KX_PRO_PEPPER", "")
    if not re.fullmatch(r"[0-9a-fA-F]{64}", value, flags=re.ASCII):
        raise ConfigurationError()
    return bytes.fromhex(value)


class Authority:
    """Short-lived, per-operation SQLite connections with durable transactions.

    initialize=True is reserved for local issuance. Existing files must already
    have this schema and the correct pepper verifier; even an empty existing
    file is rejected. Missing runtime databases are never silently recreated.
    """

    def __init__(self, database, pepper, *, initialize=False,
                 clock=wallclock_millis, busy_timeout=2.0):
        if type(pepper) is not bytes or len(pepper) != 32:
            raise ConfigurationError()
        self.database = Path(database).resolve()
        self._pepper = pepper
        self._verifier = hmac.new(pepper, VERIFIER_CONTEXT, hashlib.sha256).hexdigest()
        self.clock = clock
        self.busy_timeout = busy_timeout
        created = False
        if initialize:
            try:
                descriptor = os.open(self.database, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
            except FileExistsError:
                pass
            else:
                os.close(descriptor)
                created = True
        with self._connection(verify=not created) as connection:
            if created:
                connection.execute("BEGIN IMMEDIATE")
                try:
                    self._create_schema(connection)
                    connection.commit()
                except Exception:
                    connection.rollback()
                    raise

    @contextmanager
    def _connection(self, *, verify=True):
        # mode=rw prevents accidental creation on deletion, bad paths or outages.
        connection = sqlite3.connect(
            self.database.as_uri() + "?mode=rw", uri=True,
            timeout=self.busy_timeout, isolation_level=None,
        )
        try:
            connection.row_factory = sqlite3.Row
            connection.execute("PRAGMA foreign_keys = ON")
            connection.execute("PRAGMA synchronous = FULL")
            if verify:
                row = connection.execute(
                    "SELECT schema_version, pepper_verifier FROM metadata WHERE id = 1"
                ).fetchone()
                if (row is None or row["schema_version"] != 1
                        or type(row["pepper_verifier"]) is not str
                        or not hmac.compare_digest(row["pepper_verifier"], self._verifier)):
                    raise ConfigurationError()
            yield connection
        finally:
            connection.close()

    def _create_schema(self, connection):
        connection.execute("""
            CREATE TABLE metadata (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                schema_version INTEGER NOT NULL CHECK (schema_version = 1),
                pepper_verifier TEXT NOT NULL
            )
        """)
        connection.execute("INSERT INTO metadata VALUES (1, 1, ?)", (self._verifier,))
        connection.execute("""
            CREATE TABLE codes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code_mac TEXT NOT NULL UNIQUE
                    CHECK (length(code_mac) = 64 AND code_mac NOT GLOB '*[^0-9a-f]*'),
                expires_at_ms INTEGER NOT NULL CHECK (expires_at_ms > 0),
                subject TEXT CHECK (subject IS NULL OR
                    (length(subject) = 64 AND subject NOT GLOB '*[^0-9a-f]*')),
                activated_at_ms INTEGER,
                state TEXT NOT NULL DEFAULT 'fresh'
                    CHECK (state IN ('fresh', 'active', 'revoked', 'superseded', 'expired')),
                CHECK ((subject IS NULL AND activated_at_ms IS NULL
                        AND state IN ('fresh', 'revoked', 'expired'))
                    OR (subject IS NOT NULL AND activated_at_ms IS NOT NULL
                        AND state IN ('active', 'revoked', 'superseded', 'expired'))),
                CHECK (activated_at_ms IS NULL OR
                    (activated_at_ms >= 0 AND activated_at_ms < expires_at_ms))
            )
        """)
        connection.execute("""
            CREATE UNIQUE INDEX one_active_grant_per_subject
                ON codes(subject) WHERE state = 'active'
        """)
        connection.execute("CREATE INDEX codes_by_subject ON codes(subject)")

    @contextmanager
    def _transaction(self):
        with self._connection() as connection:
            connection.execute("BEGIN IMMEDIATE")
            try:
                yield connection
                connection.commit()
            except Exception:
                connection.rollback()
                raise

    def _now(self):
        now = self.clock()
        if type(now) is not int or now < 0:
            raise ConfigurationError()
        return now

    def _code_mac(self, code):
        return hmac.new(self._pepper, code.encode("ascii"), hashlib.sha256).hexdigest()

    def issue(self, duration_seconds):
        """Return a code exactly at issuance; the plaintext is never persisted."""
        if (type(duration_seconds) is not int
                or not 1 <= duration_seconds <= MAX_DURATION_SECONDS):
            raise ConfigurationError()
        with self._transaction() as connection:
            expires_at = self._now() + duration_seconds * 1000
            # Uniqueness is enforced by SQLite, including across concurrent admins.
            for _ in range(8):
                code = secrets.token_hex(32)
                try:
                    cursor = connection.execute(
                        "INSERT INTO codes(code_mac, expires_at_ms) VALUES (?, ?)",
                        (self._code_mac(code), expires_at),
                    )
                except sqlite3.IntegrityError:
                    continue
                return {"codeId": cursor.lastrowid, "code": code,
                        "expiresAtMillis": expires_at}
            raise ConfigurationError()

    def redeem(self, code, subject):
        if not is_hex64(code) or not is_hex64(subject):
            raise BadRequest()
        return self._grant(subject, code)

    def validate(self, subject):
        if not is_hex64(subject):
            raise BadRequest()
        return self._grant(subject)

    def _grant(self, subject, code=None):
        result = None
        with self._transaction() as connection:
            now = self._now()
            if code is None:
                row = connection.execute(
                    "SELECT * FROM codes WHERE subject = ? AND state = 'active'", (subject,)
                ).fetchone()
            else:
                row = connection.execute(
                    "SELECT * FROM codes WHERE code_mac = ?", (self._code_mac(code),)
                ).fetchone()
            if row is not None and row["state"] in ("fresh", "active"):
                if now >= row["expires_at_ms"]:
                    # Commit the terminal state, even if the wall clock later regresses.
                    connection.execute("UPDATE codes SET state = 'expired' WHERE id = ?", (row["id"],))
                elif row["state"] == "fresh" and code is not None:
                    # These two updates commit together; a replay of any superseded
                    # code can never replace the new grant or move its activation.
                    connection.execute(
                        "UPDATE codes SET state = 'superseded' WHERE subject = ? AND state = 'active'",
                        (subject,),
                    )
                    connection.execute(
                        "UPDATE codes SET state = 'active', subject = ?, activated_at_ms = ? WHERE id = ?",
                        (subject, now, row["id"]),
                    )
                    result = self._entitlement(subject, now, row["expires_at_ms"], now)
                elif row["state"] == "active" and hmac.compare_digest(row["subject"], subject):
                    if now < row["activated_at_ms"]:
                        # Fail closed rather than returning a grant issued in the future.
                        raise ConfigurationError()
                    result = self._entitlement(subject, row["activated_at_ms"], row["expires_at_ms"], now)
        if result is None:
            raise Inactive()
        return result

    @staticmethod
    def _entitlement(subject, issued_at, expires_at, now):
        return {"subject": subject, "audience": AUDIENCE,
                "issuedAtMillis": issued_at, "expiresAtMillis": expires_at,
                "authorityNowMillis": now, "revoked": False, "features": list(FEATURES)}

    def revoke(self, *, subject=None, code_id=None):
        if (subject is None) == (code_id is None):
            raise ConfigurationError()
        if subject is not None and not is_hex64(subject):
            raise ConfigurationError()
        if code_id is not None and (type(code_id) is not int or not 1 <= code_id <= 2**63 - 1):
            raise ConfigurationError()
        column, value = ("subject", subject) if subject is not None else ("id", code_id)
        with self._transaction() as connection:
            cursor = connection.execute(
                "UPDATE codes SET state = 'revoked' WHERE " + column + " = ?", (value,)
            )
            return cursor.rowcount > 0


class RateLimiter:
    """Shared route budget; bounded, non-evicting live buckets under one lock.

    Invalid authorization is still counted against the direct peer. With valid
    authorization, both budgets are consumed even on malformed/failed requests.
    Rejected attempts never extend a window. At capacity, new identities fail
    closed until a bucket expires rather than evicting existing rate budgets.
    """

    def __init__(self, *, peer_limit=120, subject_limit=30, window_seconds=60,
                 max_buckets=8192, clock=time.monotonic):
        if any(type(value) is not int or value < 1 for value in
               (peer_limit, subject_limit, window_seconds, max_buckets)):
            raise ConfigurationError()
        self.peer_limit = peer_limit
        self.subject_limit = subject_limit
        self.retry_after = window_seconds
        self.max_buckets = max_buckets
        self.clock = clock
        self._buckets = {}
        self._lock = threading.Lock()

    def allow(self, peer, subject=None):
        keys = [(('peer', peer), self.peer_limit)]
        if subject is not None:
            keys.append((('subject', subject), self.subject_limit))
        with self._lock:
            now = self.clock()
            expired = [key for key, (deadline, _) in self._buckets.items() if now >= deadline]
            for key in expired:
                del self._buckets[key]
            allowed = True
            for key, limit in keys:
                if key not in self._buckets:
                    if len(self._buckets) >= self.max_buckets:
                        allowed = False
                        continue
                    self._buckets[key] = (now + self.retry_after, 0)
                deadline, count = self._buckets[key]
                count = min(count + 1, limit + 1)
                self._buckets[key] = (deadline, count)
                if count > limit:
                    allowed = False
            return allowed


def _unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise BadRequest()
        result[key] = value
    return result


def _reject_constant(_value):
    raise BadRequest()


def _json_bytes(payload):
    return json.dumps(payload, separators=(",", ":"), ensure_ascii=True).encode("ascii")


class ProHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, _format, *args):
        # BaseHTTPRequestHandler otherwise logs URL, peer and parser errors.
        pass

    def handle_one_request(self):
        self.close_connection = True
        self._rate_counted = False
        self._response_started = False
        self.command = ""
        self.request_version = "HTTP/1.1"
        self.requestline = ""
        try:
            try:
                self.raw_requestline = self.rfile.readline(8193)
                if not self.raw_requestline:
                    return
                if len(self.raw_requestline) > 8192:
                    self.send_error(400)
                    return
                if not self.parse_request():
                    return
            except (ConnectionError, TimeoutError):
                # Incomplete headers consume the peer budget too. Exceptions in
                # authority operations below are instead always sanitized as 503.
                self.send_error(400)
                return
            self.close_connection = True
            # Dispatch every method ourselves so no default HTML 501 is possible.
            self._dispatch()
        except Exception:
            if not self._response_started:
                self._safe_respond(503, {"error": "unavailable"})
        finally:
            self.close_connection = True

    def _subject(self):
        headers = getattr(self, "headers", None)
        values = headers.get_all("Authorization", []) if headers is not None else []
        if len(values) != 1:
            return None
        match = AUTHORIZATION.fullmatch(values[0])
        return subject_for_secret(match[1]) if match else None

    def _count_rate(self):
        if self._rate_counted:
            return True
        self._rate_counted = True
        return self.server.limiter.allow(self.client_address[0], self._subject())

    def send_error(self, code, message=None, explain=None):
        # Sanitizes all HTTP parser errors, without reflecting the request line.
        if not self._count_rate():
            self._respond(429, {"error": "rate_limited"})
        else:
            self._respond(400, {"error": "bad_request"})

    def handle_expect_100(self):
        self.send_error(400)
        return False

    def _dispatch(self):
        if not self._count_rate():
            self._respond(429, {"error": "rate_limited"})
            return
        try:
            subject = self._subject()
            target = self.raw_requestline.decode("iso-8859-1").split()
            if (self.command != "POST" or self.request_version not in ("HTTP/1.0", "HTTP/1.1")
                    or len(target) != 3 or target[1] != self.path
                    or self.path not in ("/v1/redeem", "/v1/validate") or subject is None):
                raise BadRequest()
            seen_headers = set()
            for key, value in self.headers.items():
                normalized = key.lower()
                if (normalized in seen_headers or "\r" in value or "\n" in value
                        or not re.fullmatch(r"[!#$%&'*+.^_`|~0-9A-Za-z-]+", key)):
                    raise BadRequest()
                seen_headers.add(normalized)
            if (self.headers.defects or "Transfer-Encoding" in self.headers or "Expect" in self.headers
                    or not CONTENT_TYPE.fullmatch(self.headers.get("Content-Type", ""))):
                raise BadRequest()
            content_length = self.headers.get("Content-Length", "")
            if not re.fullmatch(r"[0-9]{1,10}", content_length, flags=re.ASCII):
                raise BadRequest()
            length = int(content_length)
            if not 2 <= length <= MAX_BODY_BYTES:
                raise BadRequest()
            try:
                body = self.rfile.read(length)
            except (ConnectionError, TimeoutError):
                raise BadRequest() from None
            if len(body) != length:
                raise BadRequest()
            try:
                payload = json.loads(body.decode("utf-8"), object_pairs_hook=_unique_object,
                                     parse_constant=_reject_constant)
            except (ValueError, UnicodeError, RecursionError):
                raise BadRequest() from None
            required = {"nonce", "code"} if self.path == "/v1/redeem" else {"nonce"}
            if type(payload) is not dict or set(payload) != required:
                raise BadRequest()
            if not all(is_hex64(payload[key]) for key in required):
                raise BadRequest()
        except BadRequest:
            self._respond(400, {"error": "bad_request"})
            return
        try:
            if self.path == "/v1/redeem":
                grant = self.server.authority.redeem(payload["code"], subject)
            else:
                grant = self.server.authority.validate(subject)
        except Inactive:
            self._respond(403, {"error": "inactive", "nonce": payload["nonce"]})
            return
        # Only explicitly constructed success fields ever cross the HTTP boundary.
        self._respond(200, {"nonce": payload["nonce"], **grant})

    def _safe_respond(self, status, payload):
        try:
            self._respond(status, payload)
        except (OSError, ValueError):
            pass

    def _respond(self, status, payload):
        self.close_connection = True
        # The base parser uses HTTP/0.9 for malformed lines; still return a framed,
        # sanitized HTTP response rather than an unframed or default HTML error.
        self.request_version = "HTTP/1.1"
        body = _json_bytes(payload)
        self._response_started = True
        self.send_response_only(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Connection", "close")
        if status == 429:
            self.send_header("Retry-After", str(self.server.limiter.retry_after))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)
        self.wfile.flush()


class ProHTTPServer(ThreadingHTTPServer):
    """Bounded workers and socket deadlines; no HTTP admin or proxy trust."""

    daemon_threads = True
    block_on_close = True
    allow_reuse_address = False
    request_queue_size = 32

    def __init__(self, authority, *, bind="127.0.0.1", port=0, limiter=None,
                 max_workers=32, socket_timeout=5.0):
        try:
            loopback = ipaddress.IPv4Address(bind).is_loopback
        except ipaddress.AddressValueError:
            loopback = False
        if (not loopback or type(port) is not int or not 0 <= port <= 65535
                or type(max_workers) is not int or max_workers < 1 or socket_timeout <= 0):
            raise ConfigurationError()
        self.authority = authority
        self.limiter = limiter if limiter is not None else RateLimiter()
        self.socket_timeout = socket_timeout
        self._slots = threading.BoundedSemaphore(max_workers)
        super().__init__((bind, port), ProHandler)

    def get_request(self):
        request, address = super().get_request()
        request.settimeout(self.socket_timeout)
        return request, address

    def process_request(self, request, client_address):
        if not self._slots.acquire(blocking=False):
            try:
                allowed = self.limiter.allow(client_address[0])
                status, payload = (503, {"error": "unavailable"}) if allowed else (429, {"error": "rate_limited"})
                body = _json_bytes(payload)
                reason = "Service Unavailable" if status == 503 else "Too Many Requests"
                headers = (f"HTTP/1.1 {status} {reason}\r\n"
                           "Content-Type: application/json\r\n"
                           "Cache-Control: no-store\r\n"
                           "X-Content-Type-Options: nosniff\r\n"
                           "Connection: close\r\n"
                           f"Content-Length: {len(body)}\r\n")
                if status == 429:
                    headers += f"Retry-After: {self.limiter.retry_after}\r\n"
                request.sendall(headers.encode("ascii") + b"\r\n" + body)
            except OSError:
                pass
            finally:
                self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except Exception:
            self._slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self._slots.release()

    def handle_error(self, request, client_address):
        # Do not let socketserver print exception traces containing request state.
        pass


class SafeArgumentParser(argparse.ArgumentParser):
    def error(self, message):
        self.exit(2, "error: invalid arguments (use --help)\n")


def build_parser():
    parser = SafeArgumentParser(
        description="Local KX Pro authority (Python 3 standard library). No VPN gateway or public deployment.",
        epilog=("KX_PRO_PEPPER must already contain 64 hex characters (32 secret bytes); never put it in arguments. "
                "Public production access requires separately configured TLS termination. "
                "Protect DB/pepper and use one server process; rate limits reset on restart. "
                "Examples: python pro_authority.py issue --db /absolute/pro.sqlite3 --duration-seconds 86400; "
                "python pro_authority.py serve --db /absolute/pro.sqlite3 --port 0; "
                "python pro_authority.py revoke --db /absolute/pro.sqlite3 --code-id 1"),
    )
    subcommands = parser.add_subparsers(dest="command", required=True)
    issue = subcommands.add_parser("issue", help="Create a code; stdout contains its plaintext ONCE")
    issue.add_argument("--db", required=True, help="Private SQLite file; creates a new DB only if missing")
    issue.add_argument("--duration-seconds", required=True, type=int,
                       help=f"Lifetime from issuance, 1..{MAX_DURATION_SECONDS} seconds (max 366 days)")
    revoke = subcommands.add_parser("revoke", help="Revoke locally by subject hash or internal code ID")
    revoke.add_argument("--db", required=True, help="Existing private SQLite file")
    selection = revoke.add_mutually_exclusive_group(required=True)
    selection.add_argument("--subject", help="SHA-256 of installation secret ASCII (64 lowercase hex)")
    selection.add_argument("--code-id", type=int, help="Positive internal ID printed at issuance, NOT the code")
    serve = subcommands.add_parser("serve", help="Run loopback HTTP; public clients require TLS termination")
    serve.add_argument("--db", required=True, help="Existing private SQLite file")
    serve.add_argument("--bind", default="127.0.0.1", help="IPv4 loopback only (default 127.0.0.1); no proxy-header trust")
    serve.add_argument("--port", type=int, default=0, help="Local port, 0 selects an ephemeral port (default 0)")
    return parser


def main(argv=None):
    args = build_parser().parse_args(argv)
    try:
        authority = Authority(args.db, pepper_from_env(), initialize=args.command == "issue")
        if args.command == "issue":
            print(json.dumps(authority.issue(args.duration_seconds), separators=(",", ":")), flush=True)
        elif args.command == "revoke":
            changed = authority.revoke(subject=args.subject, code_id=args.code_id)
            print(json.dumps({"revoked": changed}, separators=(",", ":")), flush=True)
        else:
            with ProHTTPServer(authority, bind=args.bind, port=args.port) as server:
                print(json.dumps({"listening": server.server_address[0], "port": server.server_address[1],
                                  "tlsRequiredForPublicAccess": True}, separators=(",", ":")), flush=True)
                server.serve_forever()
        return 0
    except KeyboardInterrupt:
        return 0
    except Exception:
        # Never print an exception, command-line secret, database path or pepper.
        print("error: authority unavailable or configuration invalid", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
