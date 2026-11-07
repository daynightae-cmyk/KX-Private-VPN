# KX Private VPN — safe main convergence, 2026-11-06

**Product repository:** `https://github.com/daynightae-cmyk/KX-Private-VPN`
**Canonical checkout:** `K:\KX-Private-VPN`
**Integrated baseline:** `6b3f2a8620a32449cf22d6836d9e17f37d7c39a3` (`main` / `origin/main` on inspection).
**Safe integration worktree:** `D:\KX-CONSOLIDATE-20261106` / `integration/kx-consolidation-20261106`.

## Verified reality
- The user's physical **itel S686LN**, Android 15 / API 35, 1080×2436, serial `1334825549012211`, is ADB-authorized. Its app is `com.knoux.kxprivatevpn`, versionName 0.1.0/versionCode 1, installed 2026-11-05.
- The **different** repository `daynightae-cmyk/KX` uses package `com.kx.vpn` and cannot safely be merged wholesale into this app: that would create duplicate modules, navigation, storage, and VPN implementations. Its task/06 local dirty work remains preserved, untouched.
- KX-Private-VPN's merged PRs #4–#14 already include TASK_05 Security Center through TASK_14 Duplicate DNA plus native consumer UI. Existing functionality was **not** reimplemented. The source tree includes native WireGuard session management, profile import, analytics, tools, and Repair Lab.
- Last pre-convergence main CI (run `42041166664`) had **build SUCCESS**, but device API26/API35 failed one external-network test `NetworkDoctorPlatformTest.measuresOneAuthorizedRequestToOwnersRepository` due `SocketTimeoutException`. This was **not** a production VPN handshake failure; it was an inappropriate always-online requirement on CI emulators.
- No owned WireGuard exit/gateway or verified VPN handshake, DNS/IPv6 leak status, real server fleet or actual Pro entitlement is established. Never call these complete.

## Unique work audited and selected
| Source | Unique value | Safe action |
| --- | --- | --- |
| `D:\KX-INTEGRATION-20261105\wt`, `integration/qoder-freebuff-ui` | New `ServiceGlyph.kt` native icon family and refined EN/AR tool copy, preservation commit `d4e4220` | Cherry-picked cleanly on latest main as `267565d`, without reintroducing older, superseded Qoder UI tree |
| `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106`, uncommitted | Owner-requested glass orb tap-to-enter, 3D intro, bilingual hint and entry test | Surgically ported without changing original working set |
| Same entry worktree | Measured Network Doctor probe outcome; no fabricated network success | Ported; when outbound HTTPS times out, write honest diagnostic and mark external evidence SKIPPED, not PASS |
| Same entry worktree | More exact privacy scope for Storage X-Ray and honest missing-service descriptions | Ported relevant AR/EN descriptions, kept explicit ready count |
| `K:\KX-Private-VPN-task07`, dirty `docs/evidence/TASK_07.md` | Older implementation SHA/status wording | Protected in patch; **do not** overwrite already merged main ledger with obsolete 'merge pending' text |
| `codex/task-00-native-cinematic` | Older dialog screenshot test changes | Compared against current main; latest version contains the material behavior, no mass replay |
| `traycer/*` removed worktree paths | Worktrees are prunable but refs/history still exist | Neither pruned nor deleted; preservation rule |
| `daynightae-cmyk/KX` task/06 dirty | Independent implementation under different package | Preserve separately, no destructive repo-to-repo merge |

## Preservation
Read-only snapshots: `D:\KX-PRESERVE-20261106\entry.patch`, `entry-latest.patch`, `alt-kx.patch`, `task07.patch`, `QODER-ServiceGlyph.kt`.
The phone's installed, debug-signed APK was pulled safely to `D:\KX-PRESERVE-20261106\phone-kxprivatevpn-before.apk`, SHA-256 `D2FBFF212851D4F343556DB1AD8B9B2059A2F6E2B9A2DBA3F80D9E36E65CDF75`. No app data, branches, stash, worktrees, or files were wiped.

Installed APK **signing certificate SHA-256** `c07168d455a86f8c792ef44ebf213ab63ec327226589692bcad69231e67c085e`.
Convergence build APK same signing certificate SHA-256 verified. Package ID identical `com.knoux.kxprivatevpn`, enabling an in-place update **without uninstall or data wipe** when device smoke is authorized.

## Verification ledger (do not overclaim)
- Initial convergence before test-suite gate updates: `gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin` => `BUILD SUCCESSFUL` (7m06s), JVM **148/148, 0 failures, 0 skipped**; run log `D:\KX-PRESERVE-20261106\consolidation-build.log`.
- Full-activity instrumentation tests initially assumed Home opened automatically. Added a human-like tap to all **11** MainActivity launch sites, plus `KxEntryTestGate.kt` asserting Home does not appear before a tap. **These edits require their own exact-SHA CI compile/execution.**
- A subsequent isolated local incremental compilation encountered a **Gradle UDP FileLockContentionHandler `BindException: Address already in use`** before compilation; not a code compile result. Do not count it as PASS or code failure.
- PR [#16](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/16) first CI on `9bbaf53`: build/lint/units PASS (run `42116165059`); API26 started **136 tests**, finished with **one actual failure** in `RepairLabScreenTest.journalShowsOnlyRecordedOperationsAndAnHonestEmptyState`, because the new honest label "Cache cleanup" appears in three semantics nodes and an unscoped text selector demanded exactly one. Corrected by tagging and asserting the journal action itself. API35 failed **before tests** with `No connected devices` (emulator offline). Neither result is misreported as PASS; CI on the corrected exact SHA is still required.
- CI run `42118058608` on `1eefa30`: **build/lint/units PASS**, API26 **PASS** (complete instrumentation), API35 ran 52 tests without failure before headless SwiftShader/ColorBuffer died during `IdentityTest.arabicRtlLargeText360dp` when Arabic screen readback and secondary dialog coexist; `emulator-5554` disappeared and remaining tests did **not** run. Workaround preserves API35 RTL/1.5x/360dp semantic visibility assertion and tests the dialog separately; API26 retains the combined screenshot/dialog path. The API35 combined visual/dialog interaction remains **UNVERIFIED**; require exact-SHA CI rerun after scoped test update.
- CI run `42119555673` on `9b8adb3`: **build PASS**, API26 **PASS**, API35 emulator went offline after `:app:installDebug` and before instrumentation while the verification script performed pretest recording/screenshot operations; the app's Android tests did not run on API35 in that attempt. The verification script now gates risky headless API35 screenrecord/framebuffer operations while retaining the full instrumentation suite, and records `capture-scope.txt` explicitly as **NOT visual PASS**. Recheck CI on the new SHA.
- Physical in-place install: `adb install -r` returned `Success` on itel S686LN / Android 15 API35. APK package unchanged, signing certificate SHA-256 matched exactly, no uninstall or clear-data command used. The built APK hash was `E48241730266579892326884336C0BEF9D1B7E84F9898689E53DBF84F3A65895`. ADB `am start -W` reported `LaunchState: COLD`.
- Real-device human gate: screenshot after **6 seconds without touching the screen** still showed the KX glass orb and "اضغط على شعار KX للدخول", not Home. ADB `input tap 540 650` then displayed the original Arabic disconnected Home. Checked sampled crash buffer with no KX crash lines. Images: [before Home](evidence/convergence-20261106-public/01-before-home.png), [cold splash gate](evidence/convergence-20261106-public/02-after-splash-gate.png), [Home after tap](evidence/convergence-20261106-public/03-after-orb-entry-home.png).
- Physical device **not** used for the full instrumentation suite (could modify user-owned profiles and settings). VPN live handshake, DNS/leaks, automated reconnect and Pro activation remain unverified and blocked.

## Remaining release gates
1. Green CI on integrated exact SHA: Android build/lint/units, emulator API26 and API35. Any external-network checks must show authentic pass or explicit SKIPPED evidence.
2. In-place update and cold splash/manual entry/Home smoke were observed on the actual phone. Remaining physical checks: full service navigation, AR/EN/RTL, day/night and detailed crash/performance telemetry. Never run the destructive whole test suite on the personal phone.
3. Final integrate only after checks to main through a reviewed PR, and verify the new `origin/main` SHA and post-merge CI.
4. Core VPN release remains network-unverified until an owner-controlled WireGuard endpoint supplies real handshake, routing, DNS, leak and reconnect evidence.
5. Pro activation by WhatsApp still requires the **owner's WhatsApp number** and a separate authenticated licensing/entitlement backend. No global shared PIN in APK and no claims of active Pro services.

**Evidence semantics:** COMMITTED ≠ CI PASS ≠ DEVICE VERIFIED ≠ VPN NETWORK VERIFIED ≠ RELEASED.

## ملحق تسوية المصدر — 2026-11-07

هذا ملحق لتدقيق المحتوى، وليس اعتماد تشغيل أو إذن حذف. المقارنة عند
`origin/main = 29bb4922b54d623e8c230c79de138711a8189423`؛ النتائج أعلاه تخص تواريخها
ولا تصف تلقائيًا حالة PR #28 أو الهاتف الحالي.

| المصدر | دليل الاحتفاظ أو الاستبدال |
| --- | --- |
| `d4e422010aaff0fc7dc117482aeb49e1c23e9fd9` | يطابق التزام الدمج `267565df11eae8c3600085413fd7b9c7e8cb7102` في stable patch-ID: `3e36dc18cb953bfb55bab0991040458233e2cbf8`، والتزام الدمج سلف لـ main المدقق. |
| `ServiceGlyph.kt` | blob `52e529762521702d258f57f33c75b0a815d97102` مطابق في d4e4220 وae5861e وmain المدقق. خرائط الرموز الأصلية بقيت؛ توجد إضافات وتغييرات حجم لاحقة، فلا تعني مطابقة الملف مطابقة Home كاملًا. |
| النصوص العربية والإنجليزية | بقيت تسميات الأدوات والعدّ الصريح للخدمات الجاهزة. لم يبق `repair_measure_scope` و`repair_unavailable_body` حرفيًا: استبدلهما `2e23f26178536a954475c70de0e310b8ea8567f0` بوصف أدق لنطاق StatFs والتخزين الخاص وعدم المحاكاة. |
| `ae5861efc55b3ff49ab0b0f8a1beb1ade3a3f541` | ملفا KxLaunch وIdentityTest يطابقان نسخهما المنقولة في 2e23f26. حلّ الدوران المرتبط بدورة الحياة والتسليم مرة واحدة محل رد النداء الفوري؛ هذا استبدال مقصود لا فقد غير مفسّر. استعادة composition ليست بذاتها اختبار إعادة إنشاء Activity. |
| `e20500f41f1dfede632174a3bb172fc5f16973d8` | من الملفات الستة عشر المعدّلة، تطابق محتويات 12 ملفًا كاملة عند السلف المتكامل `263548b0283cad92cfb65a6a823a33a119970201`. الاستثناءات: Home وRepairLab واختبارا Home/ServerSelection، مع تكييفات لاحقة. ستة ملفات موارد advanced/network/security بالعربية والإنجليزية ما زالت مطابقة بايتًا عند main المدقق. وصف اللقطة بأنها مستبدلة بالكامل غير دقيق. |
| `e2542b2` و`d696fda` | patch-ID المجمع `48060eed4c7a4b357fcfa15b72891e26145d2b73` يطابق squash `a70d89d` عبر PR #23، وهو سلف main المدقق. EvidenceCapture blob `9581167f51895bbbaf36e14c60d3aafb9a87097a` وGhostHunterFlowTest blob `0bc0feeb83d48f9b57563d111fa880e57f3bce7f` مطابقان عند d696fda وmain. لا حاجة لإعادة cherry-pick لمجرد أن الالتزامين الأصليين ليسا سلفين. |

### السجل المنفرد وحدود الأرشفة

السجل التاريخي ذو 38 سطرًا
[`KX_UNIQUE_WORK_INTEGRATION_20261106.md`](https://github.com/daynightae-cmyk/KX-Private-VPN/blob/ae5861efc55b3ff49ab0b0f8a1beb1ade3a3f541/docs/evidence/KX_UNIQUE_WORK_INTEGRATION_20261106.md)
باقٍ في المصدر ae5861e بالـ blob `12e4dc212cdd2c78c35f7104d28971fd5d8b1424`، وليس ملفًا
مدمجًا حرفيًا في main. يمكن استعادته بـ
`git show ae5861efc55b3ff49ab0b0f8a1beb1ade3a3f541:docs/evidence/KX_UNIQUE_WORK_INTEGRATION_20261106.md`.
ادعاءاته عن المهام والبوابات تخص لقطة 2026-11-06، فلا تُنقل بوصفها الحالة الراهنة.
يبقى ref المصدر محفوظًا؛ التوثيق المتداخل لا يعادل دمج بايتات السجل.

فرع Qoder المصدر محلي فقط، ولم تُعَد مطابقة ملفات patch الخارجية بالهاش في هذا التدقيق.
PR #28 وتعديلاته غير المثبتة منفصلان عن مقارنة main أعلاه. لا يُصرّح هذا الملحق بإزالة
أي ref أو worktree أو ملف غير متتبع، ولا يثبت عدم فقد كل سلوك وقت التشغيل.

### حصر نسخ العمل للحماية، لا للحذف

لقطة metadata بتاريخ 2026-11-07 نحو 18:07 UTC؛ كل المسارات السبعة المسجلة موجودة،
ولا مدخل مفقود أو prunable أو locked. الأرقام مقابل main المدقق أعلاه:

| المسار | HEAD وقت الحصر | أمام/خلف main | الحالة | حجم الملفات المنطقي بالبايت |
| --- | --- | --- | --- | ---: |
| `K:/KX-Private-VPN` | `d696fda1d0f44ef0c8c30aa17ec5a4151a2123eb` | 2 / 10 | الأصل المحمي؛ 11 مدخلًا غير متتبع | 915245004 |
| `D:/KNOUX-Workspace/KX-PRIVATE-ENTRY-20261106` | `ae5861efc55b3ff49ab0b0f8a1beb1ade3a3f541` | 1 / 43 | Git-clean لكن سجل مصدر منفرد | 263874949 |
| `D:/KX-CONSOLIDATE-20261106` | `d2aa4e0e936993e0cda057964637554874cad0f4` | 0 / 34 | صور convergence غير متتبعة | 270170993 |
| `D:/KX-HARDEN-20261107` | `a5d82d7b42082ab31fc6fba9d660aff8ef6be766` | 5 / 0 | نسخة PR #28 النشطة؛ تعديلات وبناء جارٍ أثناء الحصر | 1097678111 |
| `D:/KX-INTEGRATION-20261105/wt` | `d4e422010aaff0fc7dc117482aeb49e1c23e9fd9` | 2 / 48 | Git-clean؛ ref الحفظ محلي فقط | 269894866 |
| `D:/KX-TASK21-20261107` | `29bb4922b54d623e8c230c79de138711a8189423` | 0 / 0 | `docs/evidence/evidence.zip` غير متتبع | 478016964 |
| `K:/KX-Private-VPN-task07` | `b1c882852738f319c2e345dd055fa75c69bd32ca` | 0 / 90 | `docs/evidence/TASK_07.md` معدل | 247386748 |

المجموع المرصود 3542267635 بايت، يشمل 265413240 بايت في `.git` المشتركة المحمية؛
باستثنائها 3276854395 بايت. هذه ليست لقطة ذرية ولا مساحة قابلة للاسترداد، خصوصًا مع بناء
PR #28 المتزامن؛ تشمل ملفات متجاهلة لم يصنف محتواها. لا توجد نسخة تجمع النظافة من Git
والاندماج النسبي في main. حتى ثبوت ذلك لاحقًا لا يمنح إذن الحذف.

تفاصيل تحتاج حفظًا: مجلد صور convergence غير المتتبع 4325905 بايت؛ evidence.zip
حجمه 100199 بايت؛ السجل المعدل في task07 حجمه 10800 بايت. يحتوي zip على 28 ملفًا
تطابق main بعد تسوية CRLF/LF، لا تطابق بايتًا لكل العناصر؛ بقي محفوظًا.
المرشحون المعتمدون للحذف في هذا المرور: **لا شيء**. المحذوف: **0 بايت**.
المجلدات غير المسجلة وغير المؤكدة خارج نطاق الحصر وتبقى محفوظة. لم يُفحص المستودع المختلف.

### خصوصية الأدلة غير المتتبعة

فُتحت الصور السبع غير الموجودة بمساراتها نفسها على main للمراجعة. ثلاث صور تحت task16
(`phone-download-picker.png` و`phone-picker-view.png` و`phone-picker-roots.png`) تكشف أسماء
ملفات شخصية أو حسابات؛ تبقى أصولها محليًا ولا تُنشر خامًا. صورة المجلد ذي صفر ملفات
ليست دليل تصنيف ناجح لملفات غير فارغة. وجود نسخة مطابقة أو محتوى متكافئ بعد تسوية
CRLF/LF يبرر وصف التكرار فقط، ولا يمنح إذن الحذف. لم تُحذف أي صورة أو نسخة عمل.
