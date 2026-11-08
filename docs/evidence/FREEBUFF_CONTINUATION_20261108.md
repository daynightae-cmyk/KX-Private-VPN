# استكمال Freebuff — التطبيقات والخوادم، 2026-11-08

## حدود النسخة والعمل المحفوظ

- المستودع الوحيد: https://github.com/daynightae-cmyk/KX-Private-VPN . ربط المحادثة وأدواتها متحقق عند `K:/KX-Private-VPN`؛ لا تعديل للمستودع المختلف `daynightae-cmyk/KX`.
- أساس هذه الدفعة بعد fetch: `9ebc7797e9fd4f3be5a119624a67784da7f684c6`، وليس نسخة Home التاريخية.
- شجرة التنفيذ المستقلة: `D:/KX-FREEBUFF-SERVICES-20261108`، الفرع `feature/kx-freebuff-service-completion-20261108`. تحقق عدم وجود المسار قبل إنشائه؛ لم تُستبدل شجرة لوكيل آخر. لا سجل Freebuff مكشوف يجيز الادعاء بمعرفة كل جلساته.
- شجرة Freebuff الأصلية نظيفة عند `988b6ddc2f484ac9b7eb97e0ef4f7909c88525f3`، ومحفوظة دون تعديل. كذلك حُفظت المراجع والأرشيفات والفروع وكل مساحات العمل القائمة.
- ملفات Network Doctor في `D:/KX-REPAIR-20261108` وDownload Safety في `D:/KX-DOWNLOAD-SAFETY-20261108` خارج هذه الدفعة. تغييراتها غير الملتزمة ليست عملًا مفقودًا ولا إذنًا باستبداله.
- [PR28](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/28) مفتوح عند `a0278b03e2ecd130d44837460798947094629a93` وقت المطابقة، وحالة الدمج `DIRTY`. لا إعادة تنفيذ أو دمج جانبي لعمل VPN/Pro فيه.

## مطابقة النسخة التاريخية

قاعدة Freebuff: `a1d7c0872a9cebd3218cf0969463fa4a64fac160`. تسلسل العمل المحفوظ: `6a669f9`، `8cd3175`، `82e43e7`، `988b6dd`. فُحصت التعديلات نفسها، لا أسماء ملفات التسليم وحدها.

| العنصر التاريخي | التصنيف | القرار |
|---|---|---|
| أيقونات Canvas الإحدى عشرة | ALREADY ON MAIN | إبقاء النسخة الحديثة؛ أضيفت فيها أيقونة Apps مستقلة |
| شبكة Home القديمة 3/2 أعمدة وكرة148dp | SUPERSEDED BY BETTER MAIN IMPLEMENTATION | الكتالوج الحديث ذو وصف ونطاق محفوظ؛ لا استرجاع للشبكة |
| بطاقات Repair المختصرة | ALREADY ON MAIN | لا تنفيذ مكرر |
| أسماء الخدمات العشر ونصوص الحالة الأساسية EN/AR | ALREADY ON MAIN | الحفاظ عليها؛ ملف network العربي مطابق تاريخيًا |
| تسميات التخزين والذاكرة المؤقتة القديمة | SUPERSEDED BY BETTER MAIN IMPLEMENTATION | الحفاظ على تحديد نطاق KX الحديث |
| نسخ مبسطة لبعض القياسات والصلاحيات وعناوين Ghost/Duplicate | UNIQUE AND WORTH INTEGRATING | مرشحات نصية لدفعة لاحقة، دون إسقاط التحذيرات؛ ليست منفذة هنا |
| وعود الفيديو والسرعة وإصلاح التنزيلات الواسعة | SUPERSEDED BY BETTER MAIN IMPLEMENTATION | لا إعادة وعود تتجاوز المحركات الفعلية |
| وصف Sentinel بأنه ملفات نظام عامة | REQUIRES FURTHER REVIEW | لا تعميم لعداد excludedSentinels |
| بحث واختيار الإعدادات المحفوظة وحاجز busy وتأكيد الإزالة | ALREADY ON MAIN | الحفاظ على ProfileStore والمسارات الحالية |
| إظهار البحث فقط عند ثلاثة إعدادات وتنسيق البطاقات القديم | PRESERVED HISTORICAL ALTERNATIVE | ليس التصميم المعتمد الجديد |
| trim للبحث ودلالة الاختيار وإزالة بحد48dp | UNIQUE AND WORTH INTEGRATING | منفذة في هذه الدفعة مع اختبارات مخصصة |
| مستندات التسليم والرقع | PRESERVED HISTORICAL ALTERNATIVE | لا cherry-pick شامل ولا حذف للأرشيف |

Patch-id المستقر لرقعة Home الأصلية: `0302fe551806921ae41cd14868ea34b5d8ac28af`. توجد ملاحظة حفظ تصف عملًا مصدريًا غير ملتزم، لكن الرقعة المحفوظة المقابلة تعرض فروق تسليم؛ هذا اختلاف بيانات وصفية، وليس دليلًا على فقد تطبيق. لم تُضف النسخة التاريخية اختبارات.

## تصحيح أدلة PR29 وقبول Home المحدود

- [PR29](https://github.com/daynightae-cmyk/KX-Private-VPN/pull/29) مدمج بـsquash `2d47acce8c31859700aac6e5454e349e9ee1438a`؛ رأسه `faa308d29dceb007c3c9cc0ea5a1b5be925d2034`.
- [تشغيل42415323728](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42415323728): build وAPI26 وAPI35 ناجحة، لكن **SonarCloud فشل: تكرار3.1% في الكود الجديد مقابل حد3%**. CodeRabbit متخطى. عبارة «ناجح بالكامل» في السجل السابق ليست وصفًا صحيحًا لجميع البوابات.
- XML الفعلي: API26 عدد210، فشل0، أخطاء0، متخطى1؛ API35 عدد210، فشل0، أخطاء0، متخطى3. اختبارات Home الخمسة غير متخطاة على الجهازين.
- المتخطى API26: `PhysicalEntryTest.actualLauncherWaitsForOrbAndRestoresHome`. يضاف على API35: `IdentityTest.arabicRtlLargeText360dp` و`StorageXrayDeviceTest.mediaInventoryRespectsGrantedPermissionAndVersionMatrix`.
- [main42416518529](https://github.com/daynightae-cmyk/KX-Private-VPN/actions/runs/42416518529) عند `9ebc779` ناجح. تشغيل merge `42416383757` ملغى، وليس ناجحًا.
- نُزّلت صور CI العامة:20 صورة Home لكل API، أي40 صورة، بعرض360/411dp وخمس حالات لغة/مظهر/خط، أعلى الصفحة والخدمات. فُتحت أربع contact sheets حقيقية تجمعها، وصورة المالك `02-home-disconnected.png` للمقارنة.
- الفحص البصري: التفاف النصوص وتبديل الاتجاه والتسلسل الهرمي والزجاج البنفسجي/السماوي ظاهرة. عند1.6 يكبر ارتفاع البطاقات ويستدعي التمرير؛ وفي API35 توجد مساحة سفلية أكبر. لا ادعاء بفحص كل PNG بالحجم الكامل.
- كرة KX الحالية ليست زر الطاقة المطابق تمامًا للمرجع؛ هذا قبول لبنية الكتالوج واستمرارية الهوية، **وليس pixel parity** أو قبولًا على هاتف المالك.

## التطبيق الفعلي في هذه الدفعة

### Installed Apps

- محرك PackageManager واحد: `list()` يبقي عقد split-tunnel السابق ويستبعد KX، و`inventory()` يضيف بيانات الإصدار وtargetSdk وتصنيف النظام ويعرض KX بوصفه محميًا. لا QUERY_ALL_PACKAGES ولا ادعاء جرد ملفات العمل الأخرى.
- حالات READY/SCANNING/RESULTS/CANCELLED/FAILED؛ لا فحص تلقائي عند الدخول ولا عدادات وهمية. النتائج لا تُنشر إلا بعد انتهاء القراءة، وتُمسح عند بدء قراءة جديدة. الإلغاء وإعادة المحاولة محميان من وصول نتيجة قديمة.
- واجهة Compose مستقلة: نطاق واضح وزر فحص، تقدم غير محدد، إلغاء، نتائج فعلية، بحث بالاسم/الحزمة، مرشحات، ترتيب، أيقونات Android، تفاصيل قابلة للفتح، وأحجام تخزين غير مقاسة بدل أرقام مخترعة.
- موافقة ثابتة بالاسم **والحزمة**؛ يتعذر استبدال هدفها بتغيير البحث، وتستهلك مرة واحدة. KX وتطبيقات النظام والمحدّثة من النظام لا تعرض إلغاء تثبيت.
- حد التنفيذ يعيد قراءة أعلام التطبيق قبل إنشاء الطلب. `ACTION_DELETE` مع `EXTRA_RETURN_RESULT` يطلب نافذة Android؛ أضيفت فقط `REQUEST_DELETE_PACKAGES` اللازمة لهذا الطلب، وليست صلاحية إزالة صامتة.
- إيصال إحالة OPENED/RETURNED/UNAVAILABLE/BLOCKED؛ الرجوع ونتيجة Activity واختفاء launcher entry لا تثبت إزالة أو إلغاء أو مساحة مستردة. بعد الرجوع تُحدّث القائمة مع إبقاء إيصال الإحالة.
- إذا أعاد Android نتيجة بعد إعادة إنشاء العملية وفقد الذاكرة، تبدأ قراءة جديدة دون اختراع الهدف/الإيصال المفقود. لا تُحفظ قائمة التطبيقات الخاصة على القرص.
- اللغة العربية والإنجليزية والمظهران يستخدمان KxGlassTheme/GlassSurface/KxPageHeader/KxOrb؛ لا منظومة تصميم ثانية ولا تعديل لـNativeClient.

عقد Android المرجعي: [REQUEST_DELETE_PACKAGES](https://developer.android.com/reference/android/Manifest.permission#REQUEST_DELETE_PACKAGES)، و[UninstallerActivity في AOSP Android14](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android14-release/packages/PackageInstaller/src/com/android/packageinstaller/UninstallerActivity.java) الذي يفحص صلاحية الطلب وEXTRA_RETURN_RESULT ويعرض التأكيد.

### Servers

- مطابقة الاسم/endpoint بعد trim مع إبقاء نص حقل البحث كما كتبه المستخدم.
- selectableGroup وبطاقات RadioButton تنشر Selected؛ busy يمنع اختيار البطاقة والزر والإزالة.
- حد زر الإزالة48dp، مع اختبار لمس يثبت عدم تشغيل اختيار البطاقة عند الضغط على الإزالة. لا بيانات latency أو fleet مصطنعة ولا تعديل لمحرك الملفات.

## مصفوفة الاستكمال وحدود النطاق

حصر المسارات والمحركات السابق في [SERVICE_SPECIALIZATION_20261108.md](SERVICE_SPECIALIZATION_20261108.md#الحصر-من-المصدر--لا-ادعاء-تحقق-تشغيلي) يبقى مرجعًا للمصدر، وليس إثبات تشغيل لكل خدمة. الحالة أدناه تخص هذه الدفعة فقط.

| السطح | ما تم هنا | الحد المتبقي |
|---|---|---|
| Home/Repair catalog | مطابقة وحماية وفحص40 صورة Home من PR29 | لا إعادة تصميم؛ لا pixel parity ولا فحص هاتف جديد |
| Apps | تنفيذ البحث والنتائج والتفاصيل والإلغاء والإحالة | تأكيد Android الفعلي لم يُختبر بإزالة تطبيق؛ لا قياس أحجام تطبيقات أخرى |
| Servers | تحسين البحث/الدلالات/اللمس واختبارات | قبول runtime للنسخة الجديدة مطلوب قبل الدمج |
| Storage X-Ray | المحرك الموجود محفوظ | لا تغييرات أو قياسات هاتف جديدة |
| Ghost/Duplicate | آليات الموافقة وإعادة التحقق الموجودة محفوظة | لا تنفيذ حذف في هذه المهمة؛ نسخ النصوص المرشحة مؤجلة |
| Heavyweight | قائمة القراءة الموجودة محفوظة | لا وعد حذف أو استرداد مساحة |
| Cache/Autopilot | KX cache والسجل ضمن النطاق الحالي | لا تنظيف تطبيقات أخرى أو تشغيل ذاتي عام |
| Media | نسخ الصور المتحققة الموجودة محفوظة | ضغط الفيديو غير منفذ؛ فرق حجم النسخة ليس مساحة محررة |
| Download Rescue | عمل السلامة الفريد محفوظ في شجرة صاحبه | يلزم تسليم وتكامل الإصلاح، دون تكرار الملفات |
| Network Doctor | الشجرة النشطة مستثناة | يلزم تكامل صاحبها؛ لا قياس سرعة/إصلاح عام مصطنع |
| Shield/Security/Statistics | مصدر main محفوظ | لا إنهاء جلسات خارج KX أو إثبات منع تسرب جديد |
| Import/VPN/Split/Advanced | العقود الحالية محفوظة ومنها list() | لا إثبات egress عام جديد؛ PR28 مستقل |
| Settings/Pro | لا تعارض مع الملفات الحالية | entitlement fail-closed؛ لا endpoint أو ترخيص أو دفع مخترع |

## التحقق والتكامل

- SHA تنفيذ هذه الدفعة: `2c300b142fa169fdf846c227387308e53636b7bf`، يشمل13 ملف مصدر/موارد/اختبارات فقط. لا ملفات الوكلاء المستثناة ولا أدلة هاتف خاصة.
- شُغّل من مجلد android المعزول عبر PowerShell/JDK21/SDK36/Gradle home `D:/.gradle`: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`، مع JAVA_OPTS محلي للعملية `-Djava.net.preferIPv4Stack=true`.
- الجولة النهائية: **exit0، BUILD SUCCESSFUL in3m43s**. XML:28 مجموعة،251 اختبارًا،0فشل،0أخطاء،0تخطٍّ. منها27 اختبار AppInventoryModel و6 InstalledAppClassification. التغييرات بعد تشغيل البناء وقبل هذا SHA كانت git staging/commit فقط.
- lint:138 تحذيرًا،0خطأ/قاتل. الملفان المعدلان يحملان تحذير PluralsCandidate لنص shown/total وتحذير UseKtx لإنشاء bitmap؛ لم تُكتم التحذيرات أو تُغيّر البوابات. لا ادعاء بخلو المشروع من التحذيرات.
- تطابق27 مفتاح ترجمة EN/AR ومعاملاتها متحقق، وكل44 مرجع نصي في الشاشة قابل للحل. manifest المدمج يحمل REQUEST_DELETE_PACKAGES فقط لهذا الإجراء، دون DELETE_PACKAGES أو QUERY_ALL_PACKAGES.
- debug APK:28,872,740bytes؛ SHA256 `34497e09d22c4098b7305b98a62a7bb09a5897a3327cdca5d64140cf7944534c`.
- androidTest APK:1,416,641bytes؛ SHA256 `b1b2653f99f80f78229246ecbd3239a143e8519ebef3b37d83ca406436bfd6a9`.
- اختبارات Android الجديدة مبنية، وليست منفذة محليًا:9 Compose Apps،3 PackageManager للقراءة فقط،7 إضافات Server. مصفوفة Apps تنتج20 لقطة fixture؛ Server يضيف لقطة day/ar/font1.6/360dp إلى اللقطات القائمة.
- مراجعة مصدر مستقلة وجدت فقد تحديث القائمة عند استعادة نتيجة Android بعد موت العملية؛ أُصلح مع اختبار انحدار. اختبار إلغاء reader وجد بقاء SCANNING؛ أُصلح مع حاجز generation. المراجعة لم تجد ملاحظات عملية أخرى ضمن نطاقها.
- CI للنسخة الجديدة والفحص البصري للصور الجديدة ما زالا مطلوبين قبل القبول/الدمج؛ بناء APK ليس تنفيذًا لاختبارات Android. Mobile Use لم يكشف سطح Canvas محليًا في هذه الجلسة، ولم نلتف عليه عبر ADB.
- لم تُشغل أو تُثبت نسخة على هاتف المالك، ولم يُنفّذ uninstall أو pm clear أو حذف بيانات شخصية. أي تشغيل CI لاحق يستخدم المحاكي العام المنفصل واختبارات fixtures، لا الهاتف.
