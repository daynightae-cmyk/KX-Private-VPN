import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { AndroidBottomNav } from '../../common/AndroidBottomNav';
import { RepairServiceId, RepairOperationLog } from '../../../types/repair';
import { useRepairStats } from '../../../hooks/useRepairStats';
import { 
  getInitialGhostCandidates, 
  getInitialDuplicateGroups, 
  getInitialHeavyweightItems, 
  getInitialCacheEntries, 
  getInitialMediaSlimItems, 
  getInitialDownloadIssues, 
  getInitialSessionRecords, 
  getInitialNetworkDoctorProbes, 
  generateAutopilotPlan, 
  getInitialOperationLogs,
  formatBytes,
  formatBytesAr
} from '../../../utils/repair-engine';
import { RepairServiceModal } from './RepairServiceModal';
import { 
  Sparkles, 
  ChevronLeft, 
  Activity, 
  ShieldCheck, 
  HardDrive, 
  Ghost, 
  Fingerprint, 
  Scale, 
  Cpu, 
  Image as ImageIcon, 
  DownloadCloud, 
  ShieldAlert, 
  Stethoscope, 
  Wand2, 
  Play, 
  RotateCcw,
  History,
  CheckCircle2,
  ChevronRight,
  Zap,
  Home,
  Server,
  Shield,
  BarChart2,
  Settings,
  Wrench
} from 'lucide-react';

export const Screen11RepairLab: React.FC = () => {
  const { setCurrentScreen, locale, theme, t } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [storageStats, setStorageStats, { statFs, refreshStats, isScanning }] = useRepairStats();
  const [ghosts, setGhosts] = useState(getInitialGhostCandidates);
  const [duplicates, setDuplicates] = useState(getInitialDuplicateGroups);
  const [heavyweights, setHeavyweights] = useState(getInitialHeavyweightItems);
  const [caches, setCaches] = useState(getInitialCacheEntries);
  const [mediaSlims, setMediaSlims] = useState(getInitialMediaSlimItems);
  const [downloads, setDownloads] = useState(getInitialDownloadIssues);
  const [sessions, setSessions] = useState(getInitialSessionRecords);
  const [probes, setProbes] = useState(getInitialNetworkDoctorProbes);
  const [autopilotActions, setAutopilotActions] = useState(generateAutopilotPlan);
  const [operationLogs, setOperationLogs] = useState<RepairOperationLog[]>(getInitialOperationLogs);

  const [activeServiceModal, setActiveServiceModal] = useState<RepairServiceId | null>(null);
  const [selectedPulseCard, setSelectedPulseCard] = useState<string | null>(null);
  const [isScanningAll, setIsScanningAll] = useState(false);
  const [globalScanProgress, setGlobalScanProgress] = useState(100);

  // PulseLink animation trigger
  const handleServiceSelect = (serviceId: RepairServiceId) => {
    setSelectedPulseCard(serviceId);
    setTimeout(() => {
      setActiveServiceModal(serviceId);
      setSelectedPulseCard(null);
    }, 280);
  };

  // Full-phone scan trigger
  const handleAnalyzeMyPhone = () => {
    refreshStats();
    setIsScanningAll(true);
    setGlobalScanProgress(10);
    const interval = setInterval(() => {
      setGlobalScanProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsScanningAll(false);
          return 100;
        }
        return prev + 30;
      });
    }, 350);
  };

  const handleActionComplete = (serviceName: string, itemsCount: number, bytesReclaimed: number) => {
    if (bytesReclaimed > 0) {
      setStorageStats(prev => {
        const newUsed = Math.max(0, prev.usedBytes - bytesReclaimed);
        return {
          ...prev,
          usedBytes: newUsed,
          freeBytes: prev.totalBytes - newUsed,
          usedPercentage: Math.round((newUsed / prev.totalBytes) * 100),
        };
      });
    }

    const newLog: RepairOperationLog = {
      id: String(Date.now()),
      timestamp: isAr ? 'الآن' : 'Just now',
      serviceName,
      serviceNameAr: serviceName,
      actionTaken: isAr ? `تمت معالجة ${itemsCount} عناصر واستعادة ${format(bytesReclaimed)}` : `Processed ${itemsCount} items, reclaimed ${format(bytesReclaimed)}`,
      actionTakenAr: isAr ? `تمت معالجة ${itemsCount} عناصر واستعادة ${format(bytesReclaimed)}` : `Processed ${itemsCount} items, reclaimed ${format(bytesReclaimed)}`,
      itemsAffected: itemsCount,
      bytesReclaimed,
      isReversible: true,
    };

    setOperationLogs(prev => [newLog, ...prev.slice(0, 9)]);
  };

  // 10 Signature Services definition
  const services = [
    {
      id: 'storage-xray' as RepairServiceId,
      name: 'Storage X-Ray™',
      nameAr: 'أشعة التخزين X-Ray',
      summary: isAr ? 'تحليل هيكلي عميق للذاكرة' : 'Deep 3D Storage Breakdown',
      icon: HardDrive,
      gradient: 'from-cyan-400 to-blue-600',
      badge: '3D Core',
      metric: `${storageStats.usedPercentage}% ${isAr ? 'ممتلئ' : 'used'}`,
    },
    {
      id: 'ghost-hunter' as RepairServiceId,
      name: 'Ghost Hunter™',
      nameAr: 'صائد المخلفات',
      summary: isAr ? 'كشف المجلدات الفارغة والملفات التالفة' : '0-Byte & Empty Directory Scanner',
      icon: Ghost,
      gradient: 'from-purple-500 to-indigo-600',
      badge: `${ghosts.length} ${isAr ? 'عناصر' : 'items'}`,
      metric: '100% Safe',
    },
    {
      id: 'duplicate-dna' as RepairServiceId,
      name: 'Duplicate DNA™',
      nameAr: 'بصمة المكررات',
      summary: isAr ? 'تطابق بت-مقابل-بت عبر SHA-256' : 'Cryptographic SHA-256 Deduplication',
      icon: Fingerprint,
      gradient: 'from-pink-500 to-rose-600',
      badge: 'SHA-256',
      metric: format(duplicates.reduce((sum, d) => sum + d.potentialReclaimBytes, 0)),
    },
    {
      id: 'heavyweight-finder' as RepairServiceId,
      name: 'Heavyweight Finder™',
      nameAr: 'الملفات الثقيلة',
      summary: isAr ? 'كشف أكبر مستهلكي الذاكرة' : 'Largest Storage Consumers',
      icon: Scale,
      gradient: 'from-blue-600 to-indigo-800',
      badge: 'Heatmap',
      metric: `${heavyweights.length} ${isAr ? 'ملفات' : 'files'}`,
    },
    {
      id: 'cache-surgeon' as RepairServiceId,
      name: 'Cache Surgeon™',
      nameAr: 'مبضع الذاكرة المؤقتة',
      summary: isAr ? 'تفريغ الذاكرة المؤقتة الحقيقية بأمان' : 'Legitimate Ephemeral Cache Cleaner',
      icon: Cpu,
      gradient: 'from-cyan-500 to-teal-600',
      badge: 'Safe Purge',
      metric: format(caches.reduce((sum, c) => sum + c.sizeBytes, 0)),
    },
    {
      id: 'media-slim' as RepairServiceId,
      name: 'Media Slim Studio™',
      nameAr: 'ترشيق الوسائط',
      summary: isAr ? 'تحسين حجم الصور بدون فقدان الجودة' : 'Quality-Preserving Media Compression',
      icon: ImageIcon,
      gradient: 'from-amber-400 to-orange-500',
      badge: '~78% Save',
      metric: `${mediaSlims.length} ${isAr ? 'مرشحة' : 'ready'}`,
    },
    {
      id: 'download-rescue' as RepairServiceId,
      name: 'Download Rescue™',
      nameAr: 'إنقاذ التنزيلات',
      summary: isAr ? 'فحص ومعالجة التنزيلات المنقطعة' : 'Incomplete Download Diagnostic',
      icon: DownloadCloud,
      gradient: 'from-emerald-400 to-cyan-600',
      badge: 'Checksum',
      metric: `${downloads.length} ${isAr ? 'تنبيهات' : 'alerts'}`,
    },
    {
      id: 'session-shield' as RepairServiceId,
      name: 'Session Shield™',
      nameAr: 'درع الجلسات والخصوصية',
      summary: isAr ? 'إدارة مفاتيح الجلسات وسجلات النفق' : 'Private Key Vault & Session Ledger',
      icon: ShieldAlert,
      gradient: 'from-indigo-500 to-violet-700',
      badge: 'Curve25519',
      metric: 'Active',
    },
    {
      id: 'network-doctor' as RepairServiceId,
      name: 'Network Doctor™',
      nameAr: 'طبيب الشبكة ونفق VPN',
      summary: isAr ? 'فحص حقيقي للـ DNS والتسريب' : 'DNS & Tunnel Leak Inspection',
      icon: Stethoscope,
      gradient: 'from-teal-400 to-cyan-500',
      badge: '4 Probes',
      metric: '0 Leak',
    },
    {
      id: 'repair-autopilot' as RepairServiceId,
      name: 'Repair Autopilot™',
      nameAr: 'الطيار الآلي الذكي',
      summary: isAr ? 'تجميع شامل وخطة صيانة متكاملة' : 'Flagship Multi-Stage Smart Maintenance',
      icon: Wand2,
      gradient: 'from-fuchsia-500 via-purple-600 to-cyan-400',
      badge: 'Flagship',
      metric: 'Ready',
    },
  ];

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#080b22] text-white"
    >
      {/* Background Cosmic Atmosphere matching KX Design System */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-[15%] left-[-15%] w-[130%] h-[70%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-indigo-900/40 via-purple-950/20 to-transparent blur-3xl" />
        <div className="absolute top-[40%] right-[-20%] w-[100%] h-[60%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-600/20 via-transparent to-transparent blur-3xl" />

        {/* Ambient Connecting Energy Streams */}
        <svg className="absolute inset-0 w-full h-full opacity-35" viewBox="0 0 390 844" fill="none" preserveAspectRatio="none">
          <path d="M -40 220 C 120 180, 260 360, 430 300" stroke="url(#repair-cyan-stream)" strokeWidth="1.5" />
          <path d="M -20 460 C 140 500, 280 340, 420 420" stroke="url(#repair-purple-stream)" strokeWidth="1.6" />
          <defs>
            <linearGradient id="repair-cyan-stream" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#818cf8" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="repair-purple-stream" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#c084fc" stopOpacity="0" />
              <stop offset="50%" stopColor="#ec4899" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      {/* Top Header Bar */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen('02-home-disconnected')}
          className="w-9 h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 active:scale-95 transition-all text-indigo-200 cursor-pointer"
          aria-label="Back to Home"
        >
          <ChevronLeft className={`w-5 h-5 ${isAr ? 'rotate-180' : ''}`} />
        </button>

        {/* Title Lockup */}
        <div className="flex items-center gap-2">
          <div className="relative w-8 h-8 rounded-full p-[1px] bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 shadow-[0_0_12px_rgba(99,102,241,0.5)]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX" 
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-[0.18em]">
              PHONE REPAIR LAB™
            </span>
            <span className="text-[9px] font-mono tracking-wider text-cyan-300/80 -mt-0.5">
              {isAr ? 'مختبر الصيانة والخصوصية الذاتي' : 'PRIVACY-FIRST ANDROID LAB'}
            </span>
          </div>
        </div>

        <button 
          onClick={handleAnalyzeMyPhone}
          disabled={isScanningAll}
          className="w-9 h-9 rounded-full bg-cyan-500/10 border border-cyan-400/30 flex items-center justify-center hover:bg-cyan-500/20 active:scale-95 transition-all text-cyan-300 cursor-pointer"
          title="Rescan"
        >
          <RotateCcw className={`w-4 h-4 ${isScanningAll ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Hero: Storage Diagnostic Health HUD */}
      <div className="z-10 my-1">
        <div className="relative rounded-2xl p-[1px] bg-gradient-to-r from-cyan-400/40 via-purple-500/30 to-indigo-500/40 backdrop-blur-2xl shadow-[0_8px_30px_rgba(0,0,0,0.4)] overflow-hidden">
          <div className="w-full rounded-2xl bg-[#121642]/80 p-3.5 space-y-2.5 border border-white/15">
            {/* Upper row: Diagnostic ring + storage summary */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                {/* Luminous Diagnostic Ring Indicator */}
                <div className="relative w-11 h-11 flex items-center justify-center">
                  <div className="absolute inset-0 rounded-full border-2 border-cyan-400/25 animate-ping opacity-40" />
                  <div className="w-full h-full rounded-full bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-white/40">
                    <HardDrive className="w-5 h-5 text-white" />
                  </div>
                </div>
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-xs text-white">
                      {isAr ? 'سعة التخزين المقروءة' : 'Internal Storage Health'}
                    </span>
                    <span className="px-1.5 py-0.2 rounded-full bg-cyan-400/20 text-[9px] font-mono text-cyan-300 border border-cyan-400/30">
                      {storageStats.usedPercentage}%
                    </span>
                  </div>
                  <span className="text-[10px] text-indigo-200/80 font-mono">
                    {format(storageStats.usedBytes)} / {format(storageStats.totalBytes)} · {format(storageStats.freeBytes)} {isAr ? 'متاح' : 'free'}
                  </span>
                </div>
              </div>

              {/* Quick Autopilot Button */}
              <button
                onClick={() => handleServiceSelect('repair-autopilot')}
                className="px-2.5 py-1 rounded-xl bg-gradient-to-r from-indigo-500 to-cyan-500 hover:opacity-90 active:scale-95 text-white font-bold text-[10px] flex items-center gap-1 shadow-sm cursor-pointer"
              >
                <Zap className="w-3 h-3" />
                <span>{isAr ? 'طيار آلي' : 'Autopilot'}</span>
              </button>
            </div>

            {/* Segmented Category Usage Bar */}
            <div className="h-2 w-full rounded-full bg-black/40 overflow-hidden flex p-0.5 border border-white/10">
              {storageStats.categories.map(cat => (
                <div 
                  key={cat.id} 
                  style={{ width: `${cat.percentage}%`, backgroundColor: cat.color }} 
                  className="h-full first:rounded-l-full last:rounded-r-full transition-all"
                  title={`${cat.name}: ${cat.percentage}%`}
                />
              ))}
            </div>

            {/* Micro Category Legend */}
            <div className="flex items-center justify-between text-[9px] text-slate-300 font-mono">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#38bdf8]" />
                {isAr ? 'الصور' : 'Photos'}
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#818cf8]" />
                {isAr ? 'فيديو' : 'Videos'}
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#c084fc]" />
                {isAr ? 'تطبيقات' : 'Apps'}
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#10b981]" />
                {isAr ? 'مستندات' : 'Docs'}
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#94a3b8]" />
                {isAr ? 'أخرى' : 'Other'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Central 10 Services Grid (Scrollable with KX Glass Cards) */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold tracking-wider text-cyan-300 uppercase">
            {isAr ? 'الخدمات العشر المعتمدة' : '10 Signature Maintenance Services'}
          </span>
          <span className="text-[10px] font-mono text-indigo-300/70">
            {isAr ? 'اختر خدمة للفحص' : 'Select to inspect'}
          </span>
        </div>

        <div className="grid grid-cols-2 gap-2.5">
          {services.map(srv => {
            const Icon = srv.icon;
            const isPulsing = selectedPulseCard === srv.id;

            return (
              <button
                key={srv.id}
                onClick={() => handleServiceSelect(srv.id)}
                className={`group relative rounded-2xl p-[1px] bg-gradient-to-b from-white/20 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_4px_18px_rgba(0,0,0,0.3)] hover:border-cyan-400/60 active:scale-[0.98] transition-all text-left cursor-pointer overflow-hidden ${
                  isPulsing ? 'ring-2 ring-cyan-400 scale-[1.02]' : ''
                }`}
              >
                <div className="w-full h-full rounded-2xl bg-[#141843]/75 p-3 flex flex-col justify-between space-y-2 border border-white/10 relative overflow-hidden">
                  {/* Subtle inner light arc */}
                  <div className="absolute -bottom-4 -right-4 w-14 h-14 rounded-full bg-cyan-500/10 blur-lg pointer-events-none" />

                  {/* Icon & Badge Row */}
                  <div className="flex items-center justify-between">
                    <div className={`w-8 h-8 rounded-xl bg-gradient-to-tr ${srv.gradient} flex items-center justify-center shadow-xs border border-white/30 group-hover:scale-105 transition-transform`}>
                      <Icon className="w-4 h-4 text-white" />
                    </div>
                    <span className="px-1.5 py-0.2 rounded-full bg-white/10 text-[9px] font-mono font-semibold text-cyan-200">
                      {srv.badge}
                    </span>
                  </div>

                  {/* Title & Metric */}
                  <div>
                    <span className="block font-bold text-xs text-white tracking-tight truncate">
                      {isAr ? srv.nameAr : srv.name}
                    </span>
                    <span className="block text-[10px] text-slate-300 truncate mt-0.5">
                      {srv.summary}
                    </span>
                  </div>

                  {/* Bottom Metric Bar */}
                  <div className="flex items-center justify-between text-[10px] font-mono text-cyan-300/90 pt-0.5 border-t border-white/[0.08]">
                    <span>{srv.metric}</span>
                    <ChevronRight className={`w-3.5 h-3.5 text-slate-400 group-hover:text-cyan-300 transition-colors ${isAr ? 'rotate-180' : ''}`} />
                  </div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Recent Operation Logs Footer Ledger */}
        <div className="mt-2 rounded-2xl bg-white/[0.04] p-3 border border-white/10 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-bold text-white flex items-center gap-1.5">
              <History className="w-3.5 h-3.5 text-cyan-300" />
              {isAr ? 'سجل العمليات الأخير' : 'Recent Maintenance History'}
            </span>
            <span className="text-[10px] text-slate-400 font-mono">
              {operationLogs.length} {isAr ? 'عمليات موثقة' : 'audited'}
            </span>
          </div>

          <div className="space-y-1.5 max-h-24 overflow-y-auto pr-0.5">
            {operationLogs.map(log => (
              <div key={log.id} className="p-2 rounded-xl bg-white/[0.02] border border-white/5 flex items-center justify-between text-[11px]">
                <div className="truncate max-w-[220px]">
                  <span className="block font-bold text-white truncate">
                    {isAr ? log.serviceNameAr : log.serviceName}
                  </span>
                  <span className="block text-[9px] text-slate-400 truncate">
                    {isAr ? log.actionTakenAr : log.actionTaken}
                  </span>
                </div>
                <span className="text-[10px] font-mono text-cyan-300 shrink-0">
                  {log.timestamp}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom Main Action Bar: "Analyze My Phone" */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handleAnalyzeMyPhone}
          disabled={isScanningAll}
          className="w-full h-13 rounded-full p-[1px] bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_4px_25px_rgba(6,182,212,0.5)] hover:shadow-[0_6px_30px_rgba(99,102,241,0.6)] active:scale-[0.98] transition-all cursor-pointer overflow-hidden"
        >
          <div className="w-full h-full rounded-full bg-gradient-to-r from-[#141b4e] via-[#1c1854] to-[#12234e] flex items-center justify-center gap-2.5 px-6 border border-white/20">
            <Sparkles className="w-4 h-4 text-cyan-300 animate-spin" style={{ animationDuration: '6s' }} />
            <span className="kx-brand-title text-sm tracking-wider text-white font-bold">
              {isScanningAll 
                ? (isAr ? `جارٍ فحص الهاتف (${globalScanProgress}%)...` : `ANALYZING PHONE (${globalScanProgress}%)...`)
                : (isAr ? 'فحص شامل للهاتف' : 'ANALYZE MY PHONE')}
            </span>
            <ChevronRight className={`w-4 h-4 text-cyan-300 ${isAr ? 'rotate-180' : ''}`} />
          </div>
        </button>
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />

      {/* Interactive Service Detail Modal */}
      <RepairServiceModal
        serviceId={activeServiceModal}
        onClose={() => setActiveServiceModal(null)}
        locale={locale}
        onActionComplete={handleActionComplete}
        storageStats={storageStats}
        ghosts={ghosts}
        duplicates={duplicates}
        heavyweights={heavyweights}
        caches={caches}
        mediaSlims={mediaSlims}
        downloads={downloads}
        sessions={sessions}
        probes={probes}
        autopilotActions={autopilotActions}
      />
    </div>
  );
};
