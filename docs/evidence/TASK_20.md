# TASK_20 — Network Doctor (real diagnostics audit)

**source_sha:** `95fa3d6` (`feat(android): add live network diagnostics and consented HTTPS timing`) — already on `main`
**test_honesty_sha:** `2e23f26` (`feat(android): tap-to-enter cinematic KX, honest network CI and bilingual service copy`)
**status:** GREEN — implemented and merged on `main`; catalog `NETWORK.available = true` is justified by real Android work, not a marketing flag.

This task was an audit, not a re-implementation. The Network Doctor already existed on `main`; the
audit confirmed every reported value comes from a real Android API or a real, consented network
operation, and that nothing is simulated.

## Real implementation

`android/app/src/main/java/com/knoux/kxprivatevpn/platform/network/NetworkDoctor.kt`

- `read(context)` reads `ConnectivityManager.activeNetwork`, `getNetworkCapabilities` and
  `getLinkProperties`. It reports:
  - `present` = an active network exists,
  - `validated` = `NET_CAPABILITY_VALIDATED`,
  - `captive` = `NET_CAPABILITY_CAPTIVE_PORTAL`,
  - `vpn` = `TRANSPORT_VPN` present,
  - `transports` = the real Wi-Fi / Cellular / Ethernet / VPN transports,
  - `dns` = `LinkProperties.dnsServers` host addresses.
  No value is invented; an absent network yields `present = false` and empty lists.
- `validate(value)` accepts only an explicit `https` URI with a non-blank host, no user-info, no
  query, no fragment, a valid port (`-1` or `1..65535`), length `1..2048` and no ISO control
  characters. Embedded credentials and non-HTTPS schemes are rejected.
- `probe(value, approved)` requires explicit approval before any external request, runs inside
  `withTimeout(7000)`, and uses a bounded `ThreadPoolExecutor(1, 1, ArrayBlockingQueue(1), AbortPolicy)`
  with a daemon worker so repeated requests cannot spawn unlimited blocked threads. It issues a real
  `HttpsURLConnection` `HEAD` (connect/read timeout 2500 ms, redirects disabled) and measures latency
  with the monotonic `SystemClock.elapsedRealtimeNanos()` — never wall-clock subtraction, never a
  random or hard-coded number. `invokeOnCancellation` cancels the future and purges the queue.

`android/app/src/main/java/com/knoux/kxprivatevpn/features/NetworkScreen.kt`

- Registers a real `ConnectivityManager.registerDefaultNetworkCallback` and refreshes on
  `onAvailable` / `onLost` / `onCapabilitiesChanged` / `onLinkPropertiesChanged`; unregisters on dispose.
- Renders the measured present/validated/captive/VPN/transports/DNS facts.
- The probe button is enabled only when the consent checkbox is ticked and the URL is non-blank.
  A timeout or failure is shown as an error, never a fake success. The result shows the real HTTP
  code and measured elapsed milliseconds.

## Tests

- JVM `android/app/src/test/java/com/knoux/kxprivatevpn/NetworkDoctorTest.kt` (2 tests):
  `externalProbeRequiresApprovalBeforeAnyNetworkOperation`, `restrictsRequestsToExplicitHttpsWithoutEmbeddedSecrets`.
- Instrumentation `android/app/src/androidTest/java/com/knoux/kxprivatevpn/NetworkDoctorTest.kt`
  (`NetworkDoctorPlatformTest`, 2 tests):
  - `reportsAndroidCapabilitiesWithoutInventingProtection` asserts `NetworkDoctor.read` matches the
    real `ConnectivityManager` capabilities and DNS exactly.
  - `reportsRealProbeOutcomeWithoutAssumingExternalInternet` probes the real repository URL with
    approval and records `LIVE_HTTP=<code>` + measured elapsed ms on success. On an offline CI
    emulator it records `LIVE_HTTP=UNVERIFIED` + the precise error class and marks the external
    check **SKIPPED via `Assume.assumeTrue`, never PASSED**. The evidence file explicitly states
    `No VPN claim`.

## CI evidence

Preserved under `docs/evidence/ci-95fa3d6-api26/` (device screenshots, `logcat.txt`, launch frames,
360dp/411dp display captures). The network route is wired in `NativeClient` (`"network" -> NetworkScreen()`)
and `RepairCatalog` (`NETWORK("network", "network", true)`).

## Honest limits / deferred scope

These are deliberate scope deferrals, not simulated behavior:

- DNS is read from `LinkProperties`; there is no active standalone DNS resolver probe.
- The only active reachability check is a consented HTTPS `HEAD`; there is no raw TCP connect or
  ICMP ping (Android restricts ICMP without root).
- There is no battery / data-saver gate before a probe; the user consents per request.
- `NetworkReading` carries no timestamp; the screen reflects the live callback state.
- Reporting `TRANSPORT_VPN` is a capability fact, not a guarantee of protection. The doctor never
  claims the tunnel is secure or that traffic is encrypted beyond what the OS reports.

## Conclusion

The Network Doctor performs real Android diagnostics and a real, consented, monotonic-timed HTTPS
measurement. Offline external checks are reported SKIPPED/UNVERIFIED, never PASS. `available = true`
in the repair catalog is therefore honest. No fake latency, hard-coded IP, `Math.random`, or
simulated protection claim exists in the implementation.