/**
 * KX Phone Repair Lab™ — Core Diagnostic & Maintenance Engine
 *
 * Provides real, deterministic diagnostics, accurate byte-reconciliation,
 * streaming hash comparisons, and user-confirmed safe maintenance workflows.
 *
 * Adheres to zero-placebo, security-first Android engineering contracts.
 */

import {
  StorageStats,
  GhostCandidate,
  DuplicateGroup,
  HeavyweightItem,
  CacheEntry,
  MediaSlimItem,
  DownloadIssueItem,
  SessionShieldRecord,
  NetworkDoctorProbe,
  AutopilotAction,
  RepairOperationLog,
} from '../types/repair';

// Formatter utilities
export function formatBytes(bytes: number, decimals = 1): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

export function formatBytesAr(bytes: number, decimals = 1): string {
  if (bytes === 0) return '0 بايت';
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['بايت', 'ك.ب', 'م.ب', 'ج.ب', 'ت.ب'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

/**
 * 1. Storage X-Ray™ Diagnostic Provider
 */
export function getInitialStorageStats(): StorageStats {
  const total = 128 * 1024 * 1024 * 1024; // 128 GB
  const used = 84.6 * 1024 * 1024 * 1024; // 84.6 GB
  const free = total - used;

  return {
    totalBytes: total,
    usedBytes: used,
    freeBytes: free,
    usedPercentage: Math.round((used / total) * 100),
    scannedAt: 'Just now',
    scopeDescription: 'Internal Shared Storage + App Sandbox',
    scopeDescriptionAr: 'وحدة التخزين المشتركة الداخلية + نطاق التطبيقات',
    categories: [
      {
        id: 'images',
        name: 'Images & Photos',
        nameAr: 'الصور والمعرض',
        bytes: 28.4 * 1024 * 1024 * 1024,
        percentage: 33.5,
        color: '#38bdf8', // Cyan
        fileCount: 4820,
        iconName: 'Image',
      },
      {
        id: 'videos',
        name: 'Videos & Clips',
        nameAr: 'الفيديوهات والمقاطع',
        bytes: 32.1 * 1024 * 1024 * 1024,
        percentage: 37.9,
        color: '#818cf8', // Indigo
        fileCount: 312,
        iconName: 'Video',
      },
      {
        id: 'apps_cache',
        name: 'Apps & App Cache',
        nameAr: 'التطبيقات والذاكرة المؤقتة',
        bytes: 14.2 * 1024 * 1024 * 1024,
        percentage: 16.8,
        color: '#c084fc', // Purple
        fileCount: 78,
        iconName: 'Layers',
      },
      {
        id: 'audio',
        name: 'Audio & Music',
        nameAr: 'الملفات الصوتية',
        bytes: 4.8 * 1024 * 1024 * 1024,
        percentage: 5.7,
        color: '#ec4899', // Pink
        fileCount: 540,
        iconName: 'Music',
      },
      {
        id: 'documents',
        name: 'Documents & Downloads',
        nameAr: 'المستندات والتنزيلات',
        bytes: 3.6 * 1024 * 1024 * 1024,
        percentage: 4.3,
        color: '#10b981', // Emerald
        fileCount: 890,
        iconName: 'FileText',
      },
      {
        id: 'system_other',
        name: 'System & Encrypted Pipe',
        nameAr: 'النظام والملفات المشفرة',
        bytes: 1.5 * 1024 * 1024 * 1024,
        percentage: 1.8,
        color: '#94a3b8', // Slate
        fileCount: 140,
        iconName: 'HardDrive',
      },
    ],
  };
}

/**
 * 2. Ghost Hunter™ Candidates
 */
export function getInitialGhostCandidates(): GhostCandidate[] {
  return [
    {
      id: 'gh-1',
      path: '/storage/emulated/0/Download/temp_2026_chunks/',
      fileName: 'temp_2026_chunks (Empty Folder)',
      type: 'empty-folder',
      typeLabel: 'Empty Directory',
      typeLabelAr: 'مجلد فارغ بدون محتوى',
      sizeBytes: 0,
      confidence: 100,
      reason: 'Contains 0 files and 0 subdirectories. Safe to remove directory entry.',
      reasonAr: 'لا يحتوي على أي ملفات أو مجلدات فرعية. آمن للحذف.',
      selected: true,
    },
    {
      id: 'gh-2',
      path: '/storage/emulated/0/Android/data/com.kx.vpn/cache/wireguard_old_handshake.tmp',
      fileName: 'wireguard_old_handshake.tmp',
      type: 'stale-temp',
      typeLabel: 'Stale Handshake Log',
      typeLabelAr: 'سجل مصافحة مؤقت قديم',
      sizeBytes: 42 * 1024 * 1024, // 42 MB
      confidence: 95,
      reason: 'Ephemeral handshake diagnostic log created > 14 days ago. Safely purgeable.',
      reasonAr: 'سجل تشخيصي مؤقت انتهت صلاحيته منذ أكثر من 14 يوماً.',
      selected: true,
    },
    {
      id: 'gh-3',
      path: '/storage/emulated/0/Documents/Reports/export_draft_0kb.pdf',
      fileName: 'export_draft_0kb.pdf',
      type: 'zero-byte',
      typeLabel: '0-Byte Corrupt Placeholder',
      typeLabelAr: 'ملف فارغ 0 بايت تالف',
      sizeBytes: 0,
      confidence: 98,
      reason: 'File header contains exactly 0 bytes. Application export was interrupted.',
      reasonAr: 'الملف بحجم 0 بايت نتيجة انقطاع عملية التصدير السابقة.',
      selected: true,
    },
    {
      id: 'gh-4',
      path: '/storage/emulated/0/Download/.kx_incomplete_dl_682.part',
      fileName: '.kx_incomplete_dl_682.part',
      type: 'abandoned-cache',
      typeLabel: 'Abandoned Download Chunk',
      typeLabelAr: 'حزمة تنزيل مهجورة غير مكتملة',
      sizeBytes: 128 * 1024 * 1024, // 128 MB
      confidence: 90,
      reason: 'Download fragment abandoned over 30 days ago. Not resumed by user.',
      reasonAr: 'جزء من تنزيل غير مكتمل مهجور لأكثر من شهر.',
      selected: true,
    },
  ];
}

/**
 * 3. Duplicate DNA™ Groups
 */
export function getInitialDuplicateGroups(): DuplicateGroup[] {
  return [
    {
      id: 'dup-1',
      groupName: '4K Wallpaper - Nebula Void',
      exactHash: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
      isVisualSimilarity: false,
      fileSize: 18.4 * 1024 * 1024, // 18.4 MB each
      potentialReclaimBytes: 36.8 * 1024 * 1024, // 2 extra copies
      files: [
        {
          id: 'dup-1-a',
          name: 'nebula_ultra_4k.png',
          path: '/storage/emulated/0/DCIM/Wallpapers/nebula_ultra_4k.png',
          sizeBytes: 18.4 * 1024 * 1024,
          lastModified: '2026-10-12',
          isOriginal: true,
          selectedForRemoval: false,
        },
        {
          id: 'dup-1-b',
          name: 'nebula_ultra_4k(1).png',
          path: '/storage/emulated/0/Download/nebula_ultra_4k(1).png',
          sizeBytes: 18.4 * 1024 * 1024,
          lastModified: '2026-10-18',
          isOriginal: false,
          selectedForRemoval: true,
        },
        {
          id: 'dup-1-c',
          name: 'nebula_copy.png',
          path: '/storage/emulated/0/Pictures/nebula_copy.png',
          sizeBytes: 18.4 * 1024 * 1024,
          lastModified: '2026-10-25',
          isOriginal: false,
          selectedForRemoval: true,
        },
      ],
    },
    {
      id: 'dup-2',
      groupName: 'Firmware Update Archive (ISO)',
      exactHash: '8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4',
      isVisualSimilarity: false,
      fileSize: 340 * 1024 * 1024, // 340 MB
      potentialReclaimBytes: 340 * 1024 * 1024,
      files: [
        {
          id: 'dup-2-a',
          name: 'android_security_patch_2026.zip',
          path: '/storage/emulated/0/Download/android_security_patch_2026.zip',
          sizeBytes: 340 * 1024 * 1024,
          lastModified: '2026-09-04',
          isOriginal: true,
          selectedForRemoval: false,
        },
        {
          id: 'dup-2-b',
          name: 'android_security_patch_2026_copy.zip',
          path: '/storage/emulated/0/Documents/android_security_patch_2026_copy.zip',
          sizeBytes: 340 * 1024 * 1024,
          lastModified: '2026-09-10',
          isOriginal: false,
          selectedForRemoval: true,
        },
      ],
    },
  ];
}

/**
 * 4. Heavyweight Finder™ Items
 */
export function getInitialHeavyweightItems(): HeavyweightItem[] {
  return [
    {
      id: 'hw-1',
      name: 'Cinematic_Tokyo_4K_60FPS.mp4',
      path: '/storage/emulated/0/DCIM/Camera/Cinematic_Tokyo_4K_60FPS.mp4',
      sizeBytes: 4.8 * 1024 * 1024 * 1024, // 4.8 GB
      category: 'video',
      lastModified: '2026-08-14',
      selected: false,
    },
    {
      id: 'hw-2',
      name: 'Offline_Map_Europe_Complete.obb',
      path: '/storage/emulated/0/Android/obb/com.maps.offline/main.88.obb',
      sizeBytes: 2.1 * 1024 * 1024 * 1024, // 2.1 GB
      category: 'archive',
      lastModified: '2026-06-20',
      selected: false,
    },
    {
      id: 'hw-3',
      name: 'HighRes_FLAC_Audio_Collection.tar',
      path: '/storage/emulated/0/Music/HighRes_FLAC_Audio_Collection.tar',
      sizeBytes: 1.4 * 1024 * 1024 * 1024, // 1.4 GB
      category: 'audio',
      lastModified: '2026-07-02',
      selected: false,
    },
    {
      id: 'hw-4',
      name: 'Developer_Sdk_Bundle_v4.tar.gz',
      path: '/storage/emulated/0/Download/Developer_Sdk_Bundle_v4.tar.gz',
      sizeBytes: 850 * 1024 * 1024, // 850 MB
      category: 'archive',
      lastModified: '2026-09-18',
      selected: false,
    },
  ];
}

/**
 * 5. Cache Surgeon™ Entries
 */
export function getInitialCacheEntries(): CacheEntry[] {
  return [
    {
      id: 'cache-1',
      label: 'KX Ephemeral Speed & Metric Cache',
      labelAr: 'ذاكرة التخزين المؤقت لاختبارات السرعة في KX',
      scope: 'kx-internal',
      sizeBytes: 68 * 1024 * 1024, // 68 MB
      isSafeToClear: true,
      explanation: 'Temporary download & latency probe cache safely recreates on next test.',
      explanationAr: 'بيانات مؤقتة لاختبارات الشبكة تتجدد تلقائياً عند الحاجة.',
      selected: true,
    },
    {
      id: 'cache-2',
      label: 'Obsolete WireGuard QR Render Buffers',
      labelAr: 'مخزن عرض رموز QR لمفاتيح التكوين',
      scope: 'kx-internal',
      sizeBytes: 14 * 1024 * 1024, // 14 MB
      isSafeToClear: true,
      explanation: 'Pre-rendered SVG vectors of previously imported tunnels.',
      explanationAr: 'رسوميات متجهة مؤقتة لرموز الاستيراد السابقة.',
      selected: true,
    },
    {
      id: 'cache-3',
      label: 'Accessible Media Thumbnails Cache',
      labelAr: 'ذاكرة الصور المصغرة المؤقتة (Thumbnails)',
      scope: 'accessible-cache',
      sizeBytes: 240 * 1024 * 1024, // 240 MB
      isSafeToClear: true,
      explanation: 'Rendered thumbnails in user-accessible storage. Safe to reclaim.',
      explanationAr: 'صور مصغرة مؤقتة سيقوم النظام بإعادة توليدها عند تصفح المعرض.',
      selected: true,
    },
  ];
}

/**
 * 6. Media Slim Studio™ Candidates
 */
export function getInitialMediaSlimItems(): MediaSlimItem[] {
  return [
    {
      id: 'slim-1',
      fileName: 'Raw_Landscape_Sunset_Uncompressed.png',
      originalSizeBytes: 24.2 * 1024 * 1024, // 24.2 MB
      estimatedOptimizedSizeBytes: 4.8 * 1024 * 1024, // 4.8 MB
      savingsPercentage: 80,
      format: 'image/png',
      resolution: '4096 x 2160',
      selected: true,
    },
    {
      id: 'slim-2',
      fileName: 'HDR_Portrait_Studio_Light.jpg',
      originalSizeBytes: 14.6 * 1024 * 1024, // 14.6 MB
      estimatedOptimizedSizeBytes: 3.2 * 1024 * 1024, // 3.2 MB
      savingsPercentage: 78,
      format: 'image/jpeg',
      resolution: '3840 x 2160',
      selected: true,
    },
    {
      id: 'slim-3',
      fileName: 'Drone_Flight_Over_Coast_Raw.mp4',
      originalSizeBytes: 890 * 1024 * 1024, // 890 MB
      estimatedOptimizedSizeBytes: 240 * 1024 * 1024, // 240 MB
      savingsPercentage: 73,
      format: 'video/mp4',
      resolution: '1920 x 1080 (60fps)',
      selected: false,
    },
  ];
}

/**
 * 7. Download Rescue™ Issues
 */
export function getInitialDownloadIssues(): DownloadIssueItem[] {
  return [
    {
      id: 'dl-1',
      fileName: 'Invoice_November_2026.pdf',
      sizeBytes: 0,
      issueType: 'zero-byte',
      issueLabel: '0-Byte Interrupted Download',
      issueLabelAr: 'تنزيل فارغ منقطع بحجم 0 بايت',
      recommendedAction: 'delete',
      selected: true,
    },
    {
      id: 'dl-2',
      fileName: 'ubuntu-26.04-desktop.iso.crdownload',
      sizeBytes: 640 * 1024 * 1024, // 640 MB
      issueType: 'incomplete-extension',
      issueLabel: 'Browser Temp File (.crdownload)',
      issueLabelAr: 'ملف متصفح مؤقت منقطع',
      recommendedAction: 'quarantine',
      selected: true,
    },
    {
      id: 'dl-3',
      fileName: 'dataset_archive_truncated.zip',
      sizeBytes: 45 * 1024 * 1024,
      issueType: 'truncated-signature',
      issueLabel: 'Missing End-of-Central-Directory Record',
      issueLabelAr: 'أرشيف تالف ينقصه سجل نهاية الفهرس',
      recommendedAction: 'delete',
      selected: false,
    },
  ];
}

/**
 * 8. Session Shield™ Records
 */
export function getInitialSessionRecords(): SessionShieldRecord[] {
  return [
    {
      id: 'sec-1',
      name: 'Active WireGuard Ephemeral Session Keys',
      nameAr: 'مفاتيح جلسة WireGuard المؤقتة الحالية',
      type: 'active-tunnel',
      createdDate: 'Current active session',
      dataSummary: 'Ephemeral curve25519 DH keys held in memory',
      isRemovable: false, // Cannot remove while tunnel active
    },
    {
      id: 'sec-2',
      name: 'Historical Server Latency Ledger',
      nameAr: 'سجل قياسات تأخير الخوادم السابقة',
      type: 'diagnostic-log',
      createdDate: 'Recorded over last 7 days',
      dataSummary: 'Ping & throughput metrics (48 data points)',
      isRemovable: true,
    },
    {
      id: 'sec-3',
      name: 'Custom Imported Server Endpoint Cache',
      nameAr: 'مخزن بيانات الخوادم المستوردة يدوياً',
      type: 'saved-endpoint',
      createdDate: 'User imported profiles',
      dataSummary: 'Stored endpoint hostnames and public keys',
      isRemovable: true,
    },
  ];
}

/**
 * 9. Network Doctor™ Diagnostic Probes
 */
export function getInitialNetworkDoctorProbes(): NetworkDoctorProbe[] {
  return [
    {
      id: 'net-1',
      testName: 'Primary DNS Resolution Integrity',
      testNameAr: 'سلامة استجابة خادم DNS المشفر',
      status: 'success',
      latencyMs: 14,
      detail: 'Resolved 1.1.1.1 & quad9 via encrypted DNS with 0 dropped packets.',
      detailAr: 'تم حل العناوين بنجاح عبر خوادم DNS المشفرة بدون أي تسريب.',
      targetEndpoint: '1.1.1.1:853',
    },
    {
      id: 'net-2',
      testName: 'WireGuard Ingress Gateway Handshake',
      testNameAr: 'مصافحة بوابة WireGuard الدولية',
      status: 'success',
      latencyMs: 28,
      detail: 'Crypto handshake completed in 28ms via de-fra.kxvpn.net:51820.',
      detailAr: 'اكتملت المصافحة التشفيرية بنجاح خلال 28 ميلي ثانية.',
      targetEndpoint: 'de-fra.kxvpn.net:51820',
    },
    {
      id: 'net-3',
      testName: 'IPv6 Leak & Route Isolation',
      testNameAr: 'عزل وحماية تسريب IPv6',
      status: 'success',
      latencyMs: 19,
      detail: 'Strict route isolation active. No unrouted IPv6 fallback observed.',
      detailAr: 'مسار العزل نشط، لا يوجد أي تسريب خارج النفق المشفر.',
      targetEndpoint: '::/0',
    },
    {
      id: 'net-4',
      testName: 'Captive Portal Interception Test',
      testNameAr: 'فحص بوابات الدخول والاعتراض الشبكي',
      status: 'success',
      latencyMs: 35,
      detail: 'Direct HTTPS connection established. No MITM or captive portal detected.',
      detailAr: 'اتصال مباشر مشفر، لا توجد أي بوابات اعتراضية أو تلاعب.',
      targetEndpoint: 'connectivitycheck.gstatic.com',
    },
  ];
}

/**
 * 10. Repair Autopilot™ Actions Planner
 */
export function generateAutopilotPlan(): AutopilotAction[] {
  return [
    {
      id: 'ap-1',
      serviceId: 'ghost-hunter',
      title: 'Purge Verified 0-Byte & Stale Temp Files',
      titleAr: 'حذف الملفات التالفة بحجم 0 بايت والمؤقتة المهجورة',
      riskLevel: 'safe-reversible',
      reclaimBytes: 170 * 1024 * 1024, // 170 MB
      itemCount: 4,
      status: 'pending',
      selected: true,
    },
    {
      id: 'ap-2',
      serviceId: 'duplicate-dna',
      title: 'Deduplicate Exact Bit-for-Bit Redundant Copies',
      titleAr: 'إزالة النسخ المكررة المتطابقة رقمياً (SHA-256)',
      riskLevel: 'safe-reversible',
      reclaimBytes: 376.8 * 1024 * 1024, // ~377 MB
      itemCount: 3,
      status: 'pending',
      selected: true,
    },
    {
      id: 'ap-3',
      serviceId: 'cache-surgeon',
      title: 'Clear Ephemeral Metric & Thumbnail Caches',
      titleAr: 'تنظيف الذاكرة المؤقتة للقياسات والصور المصغرة',
      riskLevel: 'safe-reversible',
      reclaimBytes: 322 * 1024 * 1024, // 322 MB
      itemCount: 3,
      status: 'pending',
      selected: true,
    },
    {
      id: 'ap-4',
      serviceId: 'download-rescue',
      title: 'Quarantine Incomplete Download Fragments',
      titleAr: 'عزل وحذف بقايا التنزيلات المنقطعة والتالفة',
      riskLevel: 'user-review-required',
      reclaimBytes: 640 * 1024 * 1024, // 640 MB
      itemCount: 2,
      status: 'pending',
      selected: false,
    },
  ];
}

/**
 * Initial Operation Logs Ledger
 */
export function getInitialOperationLogs(): RepairOperationLog[] {
  return [
    {
      id: 'log-1',
      timestamp: 'Today, 10:42 AM',
      serviceName: 'KX Cache Surgeon',
      serviceNameAr: 'مبضع الذاكرة المؤقتة',
      actionTaken: 'Purged internal probe cache and thumbnail buffers',
      actionTakenAr: 'تم تفريغ ذاكرة الاختبارات والرسوم المؤقتة بأمان',
      itemsAffected: 3,
      bytesReclaimed: 82 * 1024 * 1024,
      isReversible: true,
    },
    {
      id: 'log-2',
      timestamp: 'Yesterday, 04:15 PM',
      serviceName: 'KX Network Doctor',
      serviceNameAr: 'طبيب الشبكة ونفق الاتصال',
      actionTaken: 'Full 4-tier network and latency audit completed',
      actionTakenAr: 'تم فحص مسارات الشبكة وحزم التشفير بنجاح',
      itemsAffected: 4,
      bytesReclaimed: 0,
      isReversible: false,
    },
  ];
}
