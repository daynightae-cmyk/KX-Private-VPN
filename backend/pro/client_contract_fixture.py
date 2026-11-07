"""Test-only loopback authority controlled through a private parent-process pipe.

Used by the Kotlin ProBackendContractTest. Never deploy this fixture. All codes,
pepper and database are ephemeral; no administrative HTTP endpoint is added.
stdout is a machine protocol, not a log. The parent must not print its contents.
"""

from pathlib import Path
import json
import secrets
import sys
import tempfile
import threading

import pro_authority as pro


class Clock:
    def __init__(self):
        self.value = 1_800_000_000_000
        self.lock = threading.Lock()

    def __call__(self):
        with self.lock:
            return self.value

    def advance(self, amount):
        with self.lock:
            self.value += amount


def reply(value):
    sys.stdout.write(json.dumps(value, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def main():
    with tempfile.TemporaryDirectory(prefix="client-contract-", dir=Path(__file__).parent) as directory:
        clock = Clock()
        authority = pro.Authority(Path(directory) / "authority.sqlite3", secrets.token_bytes(32),
                                  initialize=True, clock=clock)
        server = pro.ProHTTPServer(authority, limiter=pro.RateLimiter(peer_limit=1000, subject_limit=1000))
        worker = threading.Thread(target=server.serve_forever, kwargs={"poll_interval": 0.01}, daemon=True)
        worker.start()
        try:
            reply({"port": server.server_address[1]})
            for line in sys.stdin:
                command = json.loads(line)
                operation = command["operation"]
                if operation == "issue":
                    reply(authority.issue(command["seconds"]))
                elif operation == "revoke":
                    reply({"revoked": authority.revoke(subject=command["subject"])})
                elif operation == "advance":
                    clock.advance(command["millis"])
                    reply({"advanced": True})
                elif operation == "limit":
                    server.limiter = pro.RateLimiter(peer_limit=1000, subject_limit=1)
                    reply({"limited": True})
                elif operation == "stop":
                    reply({"stopped": True})
                    break
                else:
                    raise ValueError()
        finally:
            server.shutdown()
            server.server_close()
            worker.join(timeout=3)
            if worker.is_alive():
                raise RuntimeError()


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Never include codes, installation IDs, peppers, request input or traceback.
        sys.stderr.write("Client contract fixture failed.\n")
        sys.exit(1)
