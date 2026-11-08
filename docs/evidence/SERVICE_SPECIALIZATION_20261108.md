# تخصص خدمات KX — سجل التنفيذ 2026-11-08

## الواقع وحدود الأدلة

- المستودع الوحيد: https://github.com/daynightae-cmyk/KX-Private-VPN
- `origin/main`: `29bb4922b54d623e8c230c79de138711a8189423` بعد fetch؛ النسخة الأساسية لم تُعدّل أو تُستبدل.
- [PR28](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/28) مفتوح عند `a0278b03e2ecd130d44837460798947094629a93`، وليس مدمجًا. [CI42400592246](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42400592246): build/API26/API35/Sonar ناجحة؛ CodeRabbit متخطى. لا يعني ذلك قبولًا بصريًا أو انعدام ملاحظات Sonar.
- [CI main](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42344190594) ناجح عند SHA أعلاه.
- فرع هذه الدفعة: `feature/kx-service-specialization-20261108` في `D:/KX-SERVICES-20261108` من main، وليس من فرع PR28 غير المقبول نهائيًا.
- `K:/KX-Private-VPN` باقٍ عند `d696fda1d0f44ef0c8c30aa17ec5a4151a2123eb`. الملفات غير المتتبعة `.idea/` ولقطات task16 وtask18 محفوظة. لا reset أو clean أو إزالة فروع/مساحات عمل.
- حُفظت جميع مساحات العمل السبع السابقة، ومجلدات references وKX-PRESERVE وKX-WireGuard-LAN-QA دون تعديل. ظهور `evidence.zip` غير المتتبع في مساحة main لا يُجيز حذفه.
- جلسات Qoder المسجلة الأخرى للمشروع كانت `cold`، لكن توجد عملية OpenCode؛ هذا ليس دليلًا على انتهاء عملها. لذلك استُخدمت مساحات معزولة. وُجدت عملية `com.knoux.kxprivatevpn.test` على emulator-5554/API26؛ لم نثبت أو نشغّل عليه شيئًا ولم نتدخل في الهاتف.
- Mobile Use inventory/doctor: صفر أسطح Canvas منشورة، وليس غيابًا لـSDK أو الأجهزة. التحقق المباشر عبر Canvas غير متاح في هذه الجلسة.

## الحصر من المصدر — لا ادعاء تحقق تشغيلي

المصدر: `RepairCatalog.kt:13–67` يسجل **10 خدمات إصلاح**. `NativeClient.kt:84–214` يربط **22 وجهة ملموسة**؛ هذه ليست 22 خدمة Android خلفية. مسار `repair/*` احتياطي لخدمة غير متاحة وليس خدمة إضافية. لكل صف أدناه الحكم الحالي **UNVERIFIED** تشغيليًا لهذه المهمة؛ «موجود» يعني وجود المصدر والربط فقط.

المسارات أدناه تحت `android/app/src/main/java/com/knoux/kxprivatevpn/`. أسماء الاختبارات مرجعية من مجلدي `src/test` و`src/androidTest` ولا تعني أنها نُفذت هنا.

| المعرّف / القسم | البطاقة / الشاشة / المسار | المحرك والصلاحيات | الفحص والنتائج | الإجراء والإيصال | الاختبارات / الفجوة |
|---|---|---|---|---|---|
| home / الرئيسية | KxHomeScreen / home | ClientViewModel وVpnState | عدادات النفق الحقيقية عند توفرها | اتصال/فصل وتنقل؛ لا إيصال مستقل | HomeScreenTest؛ لا إثبات اتصال من UI |
| repair / مختبر الإصلاح | KX Care / RepairLabRoute / repair | RepairMeasurement، StatFs وذاكرة KX المؤقتة | قياس محلي وسجل؛ ليس فحص الخدمات العشر | فتح الخدمات فقط | RepairCatalogTest وRepairFlowTest |
| storage / فحص | StorageXrayRoute / storage | StatFs، MediaStore وSAF؛ إذن وسائط اختياري | تصنيفات وأحجام ونطاق مسموح، إلغاء | قراءة فقط؛ لا إيصال تغيير | StorageScanTest وStorageXrayDeviceTest |
| ghost / إصلاح | GhostHunterRoute / ghost | SAF قراءة/كتابة؛ GhostHunter | ملفات صفرية ومجلدات فارغة مع أسباب | اختيار/موافقة/إعادة تحقق/حذف؛ إيصال وسجل | GhostHunterTest وGhostHunterFlowTest؛ ليس كشف بقايا كل التطبيقات |
| duplicate / إصلاح | DuplicateDnaRoute / duplicate | SAF وSHA-256 | تجميع التكرار المطابق وحماية نسخة | موافقة وإعادة تجزئة قبل الحذف وفحص الغياب؛ إيصال وسجل | DuplicateDnaTest وDuplicateDnaScreenTest |
| heavyweight / فحص | HeavyweightRoute / heavyweight | SAF قراءة؛ Heavyweight | ترتيب ملفات ≥1MiB، مرشحات وتفاصيل | فتح عارض خارجي؛ لا حذف أو إيصال | HeavyweightTest وHeavyweightScreenTest؛ يحتفظ بأكبر200 ويعرض حتى100 |
| cache / إصلاح | CacheScreen / cache | CacheMaintenance؛ cacheDir الخاص فقط | قياسات ومرشحات واختيار | موافقة وإعادة تحقق الحجم/التاريخ/التجزئة؛ إيصال وسجل | CacheMaintenanceTest وCacheFlowTest؛ لا تنظيف شامل لتطبيقات أخرى |
| media / إصلاح | MediaSlimRoute / media | SAF وImageCompressor | صور قابلة للمعالجة وجودة وأحجام | نسخ WebP والتحقق مع حفظ الأصل؛ إيصال غير دائم | MediaSlimTest وMediaSlimCompressorTest؛ الفيديو مؤجل، فرق حجم النسخة ليس مساحة مستردة |
| download / إصلاح | DownloadRescueRoute / download | Downloads، SAF وMediaStore الظاهر | تصنيف الرأس/الذيل، لا تحقق شامل للتكامل | تصدير/إعادة تسمية/حذف بموافقة؛ إيصالات مؤقتة | DownloadRescueTest وDownloadRescueScreenTest؛ خلل تصدير متعدد مؤكد أدناه |
| session / إصلاح | SessionShieldRoute / shield | مخازن إعدادات وسجل KX | جرد محلي واحتفاظ | إزالة إعداد محلي/محو سجل بموافقة؛ إيصال مؤقت | SessionShieldTest وSessionShieldScreenTest؛ لا إنهاء جلسات تطبيقات أخرى |
| network / تشخيص | NetworkScreen / network | ConnectivityManager وHTTPS؛ INTERNET/ACCESS_NETWORK_STATE | اتصال وDNS وطلب HEAD اختياري محدود | نتائج مؤقتة، قراءة فقط | NetworkDoctorTest؛ لا إصلاح أو قياس سرعة أو إثبات منع تسرب |
| autopilot / Pilot | AutopilotRoute / autopilot | AutopilotViewModel، cache وسجل KX | خطة حتمية مقاسة واختيار | موافقة مجمدة، تنفيذ، إلغاء، إيصال جزئي وسجل وإعادة قياس | AutopilotTest وAutopilotScreenTest؛ ليس AI أو تشغيلًا ذاتيًا لكل الخدمات |
| apps / جهاز | InstalledAppsRoute / apps | PackageManager؛ التطبيقات القابلة للتشغيل الظاهرة | قائمة وتصنيف/مرشحات | معلومات التطبيق وإزالة عبر Android فقط؛ لا إيصال إزالة متحقق | InstalledAppClassificationTest؛ بحث/فرز وإثبات الإلغاء يحتاج مراجعة |
| import / VPN | ImportScreen / import | ProfileStore مشفر Keystore وConfigValidator | ملف/صورةQR/نص، تحقق WireGuard | حفظ إعداد المستخدم؛ لا خوادم مصطنعة | ConfigValidatorTest وProfileStoreTest |
| servers / VPN | ServerSelectionRoute / servers | ProfileStore | بحث واختيار إعداد حقيقي | اختيار/إزالة؛ حالة محفوظة | ServerSelectionScreenTest |
| session / VPN | VpnSessionScreen / session | WireGuard GoBackend وVpnService consent وخدمة foreground | handshake وعدادات إن توفرت | اتصال/فصل وحالات خطأ | VpnControllerTest وVpnFlowTest وVpnSessionServiceTest؛ UP لا يثبت مرور الشبكة |
| security / أمان | SecurityRoute / security | SecurityScreens وقراءات إعدادات Android | تقييم مفسر للحالة | إعدادات النظام، لا كتابة مميزة | SecurityPolicyTest وSecurityFlowTest؛ ليس اختبار تسرب فعليًا |
| split / VPN | SplitTunnelRoute / split | SplitTunnelConfigurator وPackageManager وDataStore | اختيار تطبيقات ظاهرة | حفظ قواعد وإعادة اتصال | SplitTunnelPolicyTest وSplitTunnelFlowTest |
| advanced / VPN | AdvancedNetworkRoute / advanced | AdvancedNetworkConfigurator | DNS/MTU/keepalive | حفظ/إعادة ضبط/إعادة اتصال بإعداد حقيقي | AdvancedNetworkConfigTest وAdvancedNetworkFlowTest |
| stats / إحصاء | StatisticsRoute / stats | UsageRecorder/Store/Aggregator | جلسات وعدادات مسجلة فعليًا | نطاق زمني ومحو بموافقة | UsageRecorderTest وStatisticsFlowTest؛ ليس تتبعًا لكل تطبيقات الجهاز |
| education / بدء | إعداد أولي / education | DataStore | شرح النطاق | حفظ اكتمال التعليم | ClientFlowTest |
| settings / تخصيص | PreferenceScreen / settings | DataStore | لغة ومظهر محفوظان | تغيير/إعادة ضبط المظهر | ClientFlowTest؛ الإشعارات غير متاحة كمفتاح فعلي |
| Pro / غير مسجل في main | لا شاشة في main؛ موجود في PR28 | ProEntitlements في main، HTTPS/Keystore في PR28 | غير مهيأ عند غياب endpoint | fail-closed؛ لا تفعيل إنتاجي مثبت | ProEntitlementsTest؛ PR28 محفوظ ولا نكرر تنفيذه |

## الدفعة الأولى — التغييرات الفعلية

- إعادة استخدام GlassSurface/KxOrb/KxServiceGlyph ضمن KxServiceCard؛ عنوان ووصف ونطاق وإجراء واحد قابل للوصول. لا محرك أو حالة نجاح مصطنعة.
- HomeServiceCatalog مركزي بتسع وجهات موجودة، موزعة إلى التخزين والتطبيقات، الخصوصية والتشخيص، وإعداد VPN. مختبر الإصلاح يبقى المدخل المستقل للخدمات العشر؛ الروابط الفرعية اختصارات مقصودة.
- بطاقات متكيفة بارتفاع حسب النص، وعمود هاتف واحد؛ عمودان فقط عند عرض600dp فأكثر وحجم خط لا يتجاوز1.3. نصوص عربية وإنجليزية مستقلة، وسهم يعكس اتجاهه في RTL.
- أيقونة تطبيقات مميزة بدل إعادة استخدام درع الأمان. لا تعديل على مشهد الدخول أو شكل الكرة أو بيانات VPN؛ تغييرات PR28 تبقى منفصلة.
- توسعة HomeScreenTest لكل الوجهات، وفحص نصوص/دلالات/حدود لمس ومصفوفة360/411dp × خمس حالات لغة/مظهر/خط، مع20 لقطة fixture. ليست هذه بيانات هاتف المستخدم أو إثبات محركات الخدمات.
- HomeServiceCatalogTest يفحص الوجهات والفرادية والمجموعات والبيانات الوصفية.

## فجوة سلامة أُعطيت الأولوية

`DownloadRescueViewModel.kt:231–255` يصدر كل العناصر المختارة إلى URI واحد، و`Downloads.kt:239` يفتحه بوضع `wt`؛ النسخ اللاحقة تستبدل السابقة. كذلك `scan():142` يمحو إيصالًا أنشأته إزالة/إعادة تسمية. تم عزل الإصلاح في `fix/download-export-safety-20261108` / `D:/KX-DOWNLOAD-SAFETY-20261108` من main؛ لا تعديل على ملفات شخصية ولا اختبار حذف على الهاتف. قبول الإصلاح يتطلب اختبارات اختيار ثابت/ملف واحد/عدم الكتابة فوق المصدر/قراءة الوجهة والإيصال.

## التحقق البصري وحدود الإنتاج

فُتحت صورة المالك `D:/KX-QODER-VISUAL-HANDOFF-20261105/references/02-home-disconnected.png`، واللقطة التاريخية `docs/evidence/task18/phone-home-tools.png`. الثانية دليل تاريخي فقط ولا يُعاد نشرها كأثر جديد. المرجع يثبت الكرة والزجاج الداكن والبنية؛ البطاقات التاريخية تفتقر إلى وصف ونطاق وإجراء ظاهر. لقطات النسخة المعدلة لم تُفحص بعد؛ لا VISUAL PASS حتى فتحها.

البنية العامة وAPI26/API35 للنسخة الأساسية لا تكفي لقبول هذه التعديلات. لا ادعاء دمج أو اكتمال الدفعات الأربع، أو قبول الهاتف، أو شبكة VPN عامة، أو تفعيل Pro، أو توقيع إصدار، أو نشر رسمي. خوادم VPN العامة وخدمة Pro الإنتاجية والدفع والنشر وضغط الفيديو مؤجلة طبقًا للتكليف.

## الاختبارات والتكامل الجاري

- قبل التعديل: `:app:assembleDebug :app:assembleDebugAndroidTest --console=plain` باستخدام JDK21 وSDK36 وGradle home `D:/.gradle` عاد exit0. لم يُثبت الناتج على جهاز.
- تحقق النسخة المعدلة: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` عاد exit0. XML:27 مجموعة،220 اختبارًا،0فشل،0أخطاء،0تخطٍّ؛ تشمل5 اختبارات HomeServiceCatalog الجديدة. lint:137 تحذيرًا،0خطأ/قاتل. ليس هذا تصريحًا بخلو المشروع من التحذيرات.
- مراجعة مستقلة للمصدر وجدت وصف الكرة الزخرفية المتكرر داخل دلالات البطاقة؛ أُزيل محليًا عبر clearAndSetSemantics وأُضيف تأكيد اختبار، دون المساس بدلالات الكرة التفاعلية الرئيسية. أضيفت دلالة heading للعناوين وحد لمس48dp للتنقل.
- إعادة التحقق الأولى بعد التعديل الأخير توقفت قبل البناء exit1: `FileLockContentionHandler / java.net.BindException: Address already in use`؛ تشخيص read-only أظهر منافذ UDP الديناميكية49152–65535 و70مقبس UDP فقط، بلا دليل على امتلاء المجال، ولم تُوقف أي عملية. إعادة التشغيل نفسها مع `JAVA_OPTS=-Djava.net.preferIPv4Stack=true` (إعداد للعملية فقط، ليس تغييرًا للنظام أو المستودع) عادت **BUILD SUCCESSFUL in 2m 28s** لـ`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`.
- نتائج XML بعد هذا التشغيل على الشجرة المعدلة نفسها (بما فيها تعديل دلالات الكرة الزخرفية): 27 مجموعة، 220 اختبارًا، 0 فشل، 0 أخطاء، 0 تخطٍّ. lint: 137 تحذيرًا، 0 خطأ، 0 قاتل؛ التحذيران في Kotlin المعدّل هما `ModifierParameter` (GlassComponents.kt:96، توقيع سابق) و`UseOfNonLambdaOffsetOverload` (GlassComponents.kt:275). لا ادعاء بخلو المشروع من التحذيرات.
- اختبارات `androidTest` لم تُنفَّذ محليًا في هذه الدفعة؛ بُني `assembleDebugAndroidTest` فقط. لقطات fixture العشرون تُنتج عند تشغيل `HomeScreenTest` على جهاز أو CI، ولم تُفتح أي لقطة جديدة من النسخة المعدلة. لذلك لا VISUAL PASS ولا ادعاء تنفيذ على الهاتف.
- PR [#29](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/29) **مدمج** بـsquash عند `2026-11-07T21:32:55Z`؛ merge SHA `2d47acce8c31859700aac6e5454e349e9ee1438a`. الفرع `feature/kx-service-specialization-20261108` ومساحة `D:/KX-SERVICES-20261108` باقيان كما هما؛ لم يُحذف أي فرع ولم يُنفَّذ reset أو clean.
- [CI42414269579](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42414269579) (المحاولة الأولى): `build` ناجح؛ `device (26)` ناجح210/210 (1 متخطى، 0 فشل) في6m6s؛ `device (35)` **لم يفشل أي اختبار** بل أوقفه حد `timeout 8m` الداخلي في `verify-device.sh` بعد201/210 اختبارًا و0 فشل، أي بعد ثانية من سحب الأدلة؛ كان الحد مقاسًا لمجموعة138 اختبارًا.
- أُصلح هذا برفع الحد إلى12m و`timeout-minutes: 40` للـjob (600s إقلاع +6m تثبيت +12m اختبار = 28m داخليًا)، مع تعليق يوثّق المعدل المقاس. لا كتم لفشل حقيقي: الحد لا يزال يقطع أي hang ويعيد exit غير صفري.
- [CI42415323728](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42415323728) عند `faa308d2`: ناجح بالكامل — `build` ناجح، `device (26)` 210 اختبارًا (1 متخطى، 0 فشل) في6m6s، `device (35)` 210 اختبارًا (3 متخطى، 0 فشل) في9m27s. هذه أول مرة يُنفَّذ فيها `HomeScreenTest` الموسّع على API26 وAPI35 معًا.
- المتخطى على الجهازين هو نفس المجموعة المشروطة سابقًا من اختبارات الجهاز/الإصدار (`IdentityTest`, `PhysicalEntryTest`, `StorageXrayDeviceTest`)، وليس اختبارات Home الجديدة.
- **حدود قائمة:** لم تُفتح لقطات fixture العشرون المرفوعة في CI بعد، لذلك لا VISUAL PASS ولا ادعاء مطابقة بصرية. لم يُثبَّت شيء على هاتف المالك ولم يُنفَّذ أي اختبار على الهاتف في هذه الدفعة. الأقسام B–E من التكليف (Repair Lab وتخصص الخدمات العشر، فحص التخزين، التطبيقات، الجلسات) **لم تبدأ**؛ هذه الدفعة تغطي Home والكتالوج فقط.
- **نقطة استئناف:** `origin/main` عند `2d47acce8c31859700aac6e5454e349e9ee1438a`. الدفعة التالية تبدأ من هذا الـSHA في مساحة عمل معزولة جديدة، وتُبقي مساحات ومسارات الوكلاء الآخرين دون لمس، ودون إعادة فتح ملفات PR #28.
