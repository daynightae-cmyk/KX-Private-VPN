# TASK_19 — Session Shield (real KX-owned privacy center)

**branch:** `feature/task19-session-shield`
**head_sha:** `1512ece264e5503dcc2a8d9c344c426261015891`
**merged_sha:** `fcbf99ee4a3a98bfea27cdececb3207e2324b209`
**PR:** https://github.com/daynightae-cmyk/KX-Private-VPN/pull/25
**CI run:** https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42329089280
**status:** GREEN — merged to `main`; build, device (26), device (35) and SonarCloud all SUCCESS.

## Scope

A native privacy center that lists only what KX itself holds and lets the person revoke or erase it.
It never reads, lists or ends another application's session, and it never renders a credential.

## Real implementation

- `platform/security/SessionShield.kt` — `ShieldKind` (PROFILE / REPAIR_HISTORY / USAGE_HISTORY /
  SESSION_HINT), `ShieldEntry` (with `holdsSecret`), `ShieldInventory`, `ShieldAction`, `ShieldReceipt`,
  and the pure `ShieldRetention` rule (`keepFrom` / `isExpired` / `partition`, profiles always kept).
- `features/SessionShieldViewModel.kt`
  - **Revoke** removes the profile from the encrypted `ProfileStore` (AndroidKeyStore AES/GCM) and
    then reads the store back: the receipt says `secret_removed` only when the profile is verified
    absent, otherwise `secret_still_present`. The private key is gone with the profile.
  - **Erase** clears KX's own `RepairJournal`, `UsageStatsStore` and the VPN session hint; the receipt
    counts what was really removed.
  - **Prune** applies `ShieldRetention` to the journal (`replace`) and usage (`saveSurvivors`);
    profiles are never pruned.
  - Every destructive action is approval-gated (`requestRevoke` / `requestErase` open a dialog;
    `confirm…` performs it). Guidance buttons launch the system VPN settings and KX app info so the
    person withdraws consent themselves.
- `features/SessionShieldScreen.kt` — stateless screen, `route-shield` tag, profile cards that state
  `holdsSecret` without showing the value, selection, approval dialogs and an honest receipt.
- `platform/repair/RepairJournal.kt` — added `replace` (atomic) and a `clear` that checks both the
  base and AtomicFile backup delete results and errors on failure (never claims a false erase).
- `data/stats/UsageStatsStore.kt` — added `saveSurvivors` for the retention prune (checkpoint left intact).
- `NativeClient.kt` / `RepairCatalog.kt` — `shield` route wired; `SESSION.available = true`.
- EN/AR `session_strings.xml` (29 strings each).

## Tests

- JVM `SessionShieldTest.kt` — pure retention matrix and inventory rules.
- Instrumentation `SessionShieldScreenTest.kt` (8 tests) — UI states, approval separation, receipt
  honesty, and the real proof: `revokingAProfileRemovesTheSecretFromTheEncryptedStore` imports a
  profile with a private key into the real encrypted store, revokes it through the ViewModel, and
  asserts `store.load().profiles` no longer contains it **and** `store.config(id)` returns null —
  the secret is really gone, not merely hidden.
- `RepairCatalogTest` / `RepairLabScreenTest` / `RepairFlowTest` updated for the now-available session tool.

## SonarCloud gate

The first PR run failed the quality gate (`new_reliability_rating = B`) on a real BUG: `RepairJournal.clear`
ignored `File.delete()`'s Boolean (S899). Fixed by checking both base and backup delete results and
erroring on failure, plus S6532/S6615/S1172/S1313 cleanups. The re-run at `1512ece` passed with
`new_reliability_rating = A`. No test was skipped, disabled or weakened.

## Honest limits

- KX cannot end another app's session or clear another app's data; the shield states this and points
  at the official system screens.
- Physical-phone re-verification at this SHA is pending (itel S686LN ADB offline); TASK_19 is verified
  on the API 26 / API 35 CI emulators. See the final audit for device status.