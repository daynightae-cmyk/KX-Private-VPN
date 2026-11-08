# PR #40 — CI forensic blockers, 2026-11-08

**Baseline under review:** `a9204ef33422dae448650fd287188ccf45a0fd04`, `visual-reference-batch-1`, GitHub Actions run `42487446065`.

## Exact findings

| Gate | Baseline status | Evidence |
| --- | --- | --- |
| Native Android / build | PASS | GitHub job `128752314473` |
| Native Android / device (26) | PASS | GitHub job `128752314671` |
| Native Android / device (35) | FAIL / INCOMPLETE | GitHub job `128752314628`: announced **318** tests; **37** attempted before termination; `ClientFlowTest.appearanceTapIsNotLostWhileTheLocaleWriteSettles` reported FAILED but the XML failure node was **empty**; emulator `emulator-5554` disappeared from ADB. Cleanup: `Expected 318 tests, received 36`. No Java assertion stack trace or established application regression. |
| SonarCloud Quality Gate | FAIL | GitHub check `128752581126`: **C Security Rating on New Code**, required **A**. Four issues identified via `/api/issues/search?componentKeys=daynightae-cmyk_KX-Private-VPN&pullRequest=40`. |

## Sonar issues and surgical repairs in this continuation

1. `pythonsecurity:S8707` at `android/scripts/prepare_brand_asset.py` lines 33 and 71 — CLI supplied arbitrary master/output paths to file readers and directory creation. Fixed by resolving *only* repository-owned constant master/output paths from the script location, treating CLI path flags as assertions against the permitted canonical paths. Symlink escape refused. CLI remains compatible with the approved relative and absolute paths.
2. `kotlin:S3776` on `KxOrb` in `CrystalIdentity.kt` (cognitive complexity 58 vs 45 allowed). Extracted native shield drawing into a `DrawScope` helper, leaving draw instructions, transforms and layer ordering unchanged.
3. `kotlin:S1479` in `KxReferenceUi.kt` (31 `when` branches, limit 30). Grouped mirrored Back/Forward glyphs into one branch while preserving the exact previous coordinates.

## Verification policy

- `python -m unittest discover -s android/scripts -p test_prepare_brand_asset_security.py -v` — 4/4 passed locally.
- Negative attempts `--output K:\_KX_CI_PR40_20261108\should-not-exist.png` and `--master C:\Windows\win.ini` — both rejected with exit code 2; no unexpected file created.
- **Owner logo unchanged:** master SHA-256 `f336992dedba8f80580fc550735b0629c595f85d8c2a0587f08bf6f6fb2775e8`; runtime derived PNG SHA-256 `21d95dab9ecb6362319d4cc1ed49582c5562ae4dd28bad1a711b3172b4f07c70`.
- CI artifacts `native-device-api-35` inspected separately at `K:\_KX_CI_PR40_20261108` (outside the repository).
- No changes to owner's original image, `DownloadRescue` agents' uncommitted files, other agents' worktrees, native VPN feature logic or release configuration.
- **Not a CI PASS claim:** new SHA must be analyzed by SonarCloud and run through API 26 + API 35. Do not merge until both gates pass. In particular, do not suppress/skip `ClientFlowTest` or mask lost emulator tests as passing.
