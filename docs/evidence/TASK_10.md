# TASK_10 — Profile and preference controls

**baseline_sha:** `d34a083` (main after TASK_09 merge and docs)
**branch:** `codex/task-10-profile-preferences`
**implementation_sha:** `2f5e8e2`
**status:** FUNCTIONALLY COMPLETE AND VERIFIED · VISUAL FIDELITY UNVERIFIED · MERGED

## Scope
Adds honest preference controls to the existing PreferenceScreen: language/theme selection
(already persisted via LaunchPreferences), an explicit "notifications unavailable" row (no
POST_NOTIFICATIONS permission is declared in this build, so KX never posts alerts), a
reset-to-system-defaults action, and a privacy note (Keystore encryption, no backup, no account,
no analytics).

## Changed paths
- `android/app/src/main/java/com/knoux/kxprivatevpn/features/NativeClient.kt` (PreferenceScreen)
- `android/app/src/main/res/values/client_strings.xml` (+4 strings)
- `android/app/src/main/res/values-ar/client_strings.xml` (+4 strings)

## Verification
- Local gates: compileDebugKotlin PASS, testDebugUnitTest 98/98, lint 0 errors, assembleDebug PASS,
  compileDebugAndroidTestKotlin PASS.
- CI run (PR #9): build PASS; API26 device PASS (79/79, 0 failures); API35 FAIL only on
  documented `IdentityTest.arabicRtlLargeText360dp` renderer loss.
- ClientFlowTest verifies language/theme persistence across activity recreation.

## Known limits (honest)
- Visual fidelity against `10-profile-settings_75031956.png` not re-verified this cycle (image
  tooling broken). No speculative visual changes were made.
- POST_NOTIFICATIONS permission and notification channels remain deferred; no FGS notification on
  API 33+.
- API35 renderer loss unchanged. No gateway for end-to-end connection testing.

## Permission tests
No new permission. Preferences live in app-private DataStore; no storage/media access.

## PR / CI / merge
- PR: #9
- merged_sha: 4b6a3e77e81b442bc68db1264470dea5831a2766
- remote_main_sha: 4b6a3e7
- next_task: TASK_11 — Repair Lab dashboard.
