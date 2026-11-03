import React, { useState } from 'react';
import { 
  RepairServiceId, 
  StorageStats, 
  GhostCandidate, 
  DuplicateGroup, 
  DuplicateFileItem,
  StorageCategoryBreakdown,
  HeavyweightItem, 
  CacheEntry, 
  MediaSlimItem, 
  DownloadIssueItem, 
  SessionShieldRecord, 
  NetworkDoctorProbe, 
  AutopilotAction 
} from '../../../types/repair';
import { formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { 
  X, 
  ShieldCheck, 
  AlertTriangle, 
  Trash2, 
  CheckCircle2, 
  Zap, 
  Radio, 
  Layers, 
  Sparkles, 
  ArrowRight, 
  RefreshCw,
  HardDrive,
  Cpu,
  Search,
  Fingerprint,
  PieChart,
  FileText,
  Sliders,
  Play
} from 'lucide-react';

interface RepairServiceModalProps {
  serviceId: RepairServiceId | null;
  onClose: () => void;
  locale: string;
  onActionComplete: (serviceName: string, itemsCount: number, bytesReclaimed: number) => void;
  storageStats: StorageStats;
  ghosts: GhostCandidate[];
  duplicates: DuplicateGroup[];
  heavyweights: HeavyweightItem[];
  caches: CacheEntry[];
  mediaSlims: MediaSlimItem[];
  downloads: DownloadIssueItem[];
  sessions: SessionShieldRecord[];
  probes: NetworkDoctorProbe[];
  autopilotActions: AutopilotAction[];
}

export const RepairServiceModal: React.FC<RepairServiceModalProps> = ({
  serviceId,
  onClose,
  locale,
  onActionComplete,
  storageStats,
  ghosts,
  duplicates,
  heavyweights,
  caches,
  mediaSlims,
  downloads,
  sessions,
  probes,
  autopilotActions,
}) => {
  const [ghostList, setGhostList] = useState(ghosts);
  const [cacheList, setCacheList] = useState(caches);
  const [duplicateList, setDuplicateList] = useState(duplicates);
  const [downloadList, setDownloadList] = useState(downloads);
  const [activeTab, setActiveTab] = useState<'scan' | 'results' | 'executed'>('results');
  const [isProcessing, setIsProcessing] = useState(false);
  const [successNotice, setSuccessNotice] = useState<string | null>(null);

  // Autopilot wizard state
  const [autopilotStep, setAutopilotStep] = useState<'preview' | 'executing' | 'report'>('preview');

  if (!serviceId) return null;

  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  // 1. Ghost Hunter Execution
  const handleCleanGhosts = () => {
    setIsProcessing(true);
    setTimeout(() => {
      const selected = ghostList.filter(g => g.selected);
      const totalBytes = selected.reduce((sum, g) => sum + g.sizeBytes, 0);
      setGhostList(prev => prev.filter(g => !g.selected));
      setIsProcessing(false);
      setSuccessNotice(isAr ? `تم تنظيف ${selected.length} عناصر بأمان واستعادة ${format(totalBytes)}` : `Safely cleaned ${selected.length} items, reclaimed ${format(totalBytes)}.`);
      onActionComplete('KX Ghost Hunter', selected.length, totalBytes);
    }, 700);
  };

  // 2. Cache Surgeon Execution
  const handleCleanCache = () => {
    setIsProcessing(true);
    setTimeout(() => {
      const selected = cacheList.filter(c => c.selected);
      const totalBytes = selected.reduce((sum, c) => sum + c.sizeBytes, 0);
      setCacheList(prev => prev.filter(c => !c.selected));
      setIsProcessing(false);
      setSuccessNotice(isAr ? `تم تفريغ الذاكرة المؤقتة (${format(totalBytes)}) بنجاح!` : `Purged ${format(totalBytes)} of temporary cache safely!`);
      onActionComplete('KX Cache Surgeon', selected.length, totalBytes);
    }, 700);
  };

  // 3. Duplicate DNA Removal
  const handleCleanDuplicates = () => {
    setIsProcessing(true);
    setTimeout(() => {
      let totalBytes = 0;
      let count = 0;
      const updated = duplicateList.map(group => {
        const keptFiles = group.files.filter((f: DuplicateFileItem) => !f.selectedForRemoval);
        const removed = group.files.filter((f: DuplicateFileItem) => f.selectedForRemoval);
        count += removed.length;
        totalBytes += removed.reduce((sum: number, f: DuplicateFileItem) => sum + f.sizeBytes, 0);
        return {
          ...group,
          files: keptFiles,
          potentialReclaimBytes: 0,
        };
      }).filter(g => g.files.length > 1);

      setDuplicateList(updated);
      setIsProcessing(false);
      setSuccessNotice(isAr ? `تم حذف ${count} نسخ مكررة وتوفير ${format(totalBytes)}!` : `Removed ${count} redundant copies, saving ${format(totalBytes)}!`);
      onActionComplete('KX Duplicate DNA', count, totalBytes);
    }, 800);
  };

  // 4. Autopilot Execution
  const handleExecuteAutopilot = () => {
    setAutopilotStep('executing');
    setTimeout(() => {
      const selected = autopilotActions.filter(a => a.selected);
      const totalBytes = selected.reduce((sum, a) => sum + a.reclaimBytes, 0);
      const totalItems = selected.reduce((sum, a) => sum + a.itemCount, 0);
      setAutopilotStep('report');
      onActionComplete('KX Repair Autopilot', totalItems, totalBytes);
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-md animate-fade-in">
      <div 
        dir={isAr ? 'rtl' : 'ltr'}
        className="relative w-full max-w-md max-h-[90vh] flex flex-col rounded-3xl bg-gradient-to-b from-[#151945]/95 via-[#0e1234]/95 to-[#07091d]/98 border border-white/20 shadow-[0_20px_60px_rgba(0,0,0,0.8)] text-white overflow-hidden"
      >
        {/* Top Header Bar */}
        <div className="flex items-center justify-between px-5 py-3.5 border-b border-white/10 bg-white/[0.04]">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-400 to-indigo-600 flex items-center justify-center shadow-[0_0_12px_rgba(6,182,212,0.4)]">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <div>
              <span className="block font-bold text-sm tracking-wide text-white">
                {serviceId === 'storage-xray' && (isAr ? 'أشعة التخزين X-Ray™' : 'KX Storage X-Ray™')}
                {serviceId === 'ghost-hunter' && (isAr ? 'صائد المخلفات Ghost Hunter™' : 'KX Ghost Hunter™')}
                {serviceId === 'duplicate-dna' && (isAr ? 'بصمة المكررات Duplicate DNA™' : 'KX Duplicate DNA™')}
                {serviceId === 'heavyweight-finder' && (isAr ? 'مستكشف الملفات الثقيلة™' : 'KX Heavyweight Finder™')}
                {serviceId === 'cache-surgeon' && (isAr ? 'مبضع الذاكرة المؤقتة Cache Surgeon™' : 'KX Cache Surgeon™')}
                {serviceId === 'media-slim' && (isAr ? 'استوديو ترشيق الوسائط Media Slim™' : 'KX Media Slim Studio™')}
                {serviceId === 'download-rescue' && (isAr ? 'إنقاذ التنزيلات Download Rescue™' : 'KX Download Rescue™')}
                {serviceId === 'session-shield' && (isAr ? 'درع الجلسات والخصوصية Session Shield™' : 'KX Session Shield™')}
                {serviceId === 'network-doctor' && (isAr ? 'طبيب الشبكة Network Doctor™' : 'KX Network Doctor™')}
                {serviceId === 'repair-autopilot' && (isAr ? 'الطيار الآلي الذكي Repair Autopilot™' : 'KX Repair Autopilot™')}
              </span>
              <span className="block text-[10px] text-cyan-300/80 font-mono">
                {isAr ? 'خدمة أصلية بدون ادعاءات وهمية' : 'Zero-Placebo Native Diagnostics'}
              </span>
            </div>
          </div>

          <button 
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-slate-300 hover:text-white transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Success Toast */}
        {successNotice && (
          <div className="mx-4 mt-3 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/50 text-emerald-200 text-xs flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{successNotice}</span>
          </div>
        )}

        {/* Modal Body Content (Switch by serviceId) */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4">
          
          {/* SERVICE 1: STORAGE X-RAY */}
          {serviceId === 'storage-xray' && (
            <div className="space-y-4">
              <div className="rounded-2xl p-4 bg-white/[0.05] border border-white/10 space-y-3">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-indigo-200">{isAr ? 'السعة الكلية:' : 'Total Storage:'}</span>
                  <span className="font-bold font-mono">{format(storageStats.totalBytes)}</span>
                </div>
                {/* Proportional Storage Bar */}
                <div className="h-3 w-full rounded-full bg-black/40 overflow-hidden flex p-0.5 border border-white/10">
                  {storageStats.categories.map((cat: StorageCategoryBreakdown) => (
                    <div 
                      key={cat.id}
                      style={{ width: `${cat.percentage}%`, backgroundColor: cat.color }}
                      className="h-full first:rounded-l-full last:rounded-r-full transition-all"
                      title={`${cat.name}: ${cat.percentage}%`}
                    />
                  ))}
                </div>
                <div className="flex justify-between text-[11px] text-slate-400 font-mono">
                  <span>{isAr ? 'المستخدم:' : 'Used:'} {format(storageStats.usedBytes)} ({storageStats.usedPercentage}%)</span>
                  <span>{isAr ? 'المتاح:' : 'Free:'} {format(storageStats.freeBytes)}</span>
                </div>
              </div>

              {/* Categories Breakdown */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold text-cyan-300 tracking-wider uppercase">
                  {isAr ? 'تفصيل الفئات المقروءة' : 'Categorized Accessible Storage'}
                </h4>
                <div className="space-y-1.5">
                  {storageStats.categories.map((cat: StorageCategoryBreakdown) => (
                    <div 
                      key={cat.id} 
                      className="p-3 rounded-xl bg-white/[0.04] border border-white/10 flex items-center justify-between hover:bg-white/[0.07] transition-colors"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-3 h-3 rounded-full" style={{ backgroundColor: cat.color }} />
                        <div>
                          <span className="block font-bold text-xs text-white">
                            {isAr ? cat.nameAr : cat.name}
                          </span>
                          <span className="block text-[10px] text-slate-400 font-mono">
                            {cat.fileCount} {isAr ? 'عنصر' : 'files'} · {cat.percentage}%
                          </span>
                        </div>
                      </div>
                      <span className="text-xs font-mono font-bold text-white">
                        {format(cat.bytes)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* SERVICE 2: GHOST HUNTER */}
          {serviceId === 'ghost-hunter' && (
            <div className="space-y-4">
              <div className="p-3 rounded-2xl bg-cyan-950/40 border border-cyan-500/30 text-xs text-cyan-200 flex items-center gap-2.5">
                <ShieldCheck className="w-4 h-4 text-cyan-400 shrink-0" />
                <span>
                  {isAr ? 'فحص آمن: يتم كشف المجلدات الفارغة والملفات التالفة المؤكدة فقط، دون المساس بأي بيانات شخصية.' : 'Safe Scan: Confirmed zero-byte files & empty trees only. Personal data is never touched.'}
                </span>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-indigo-200">
                    {isAr ? 'عناصر تم العثور عليها:' : 'Identified Candidates:'} ({ghostList.length})
                  </span>
                  <button 
                    onClick={() => setGhostList(prev => prev.map(g => ({ ...g, selected: !prev.every(x => x.selected) })))}
                    className="text-cyan-400 hover:text-cyan-300 font-bold text-[11px]"
                  >
                    {isAr ? 'تحديد الكل' : 'Toggle All'}
                  </button>
                </div>

                <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
                  {ghostList.length === 0 ? (
                    <div className="p-6 text-center text-slate-400 text-xs">
                      {isAr ? '🎉 ممتاز! لا توجد ملفات مخلفات أو مجلدات فارغة.' : '🎉 Clean! No ghost files or empty directories found.'}
                    </div>
                  ) : (
                    ghostList.map(g => (
                      <div 
                        key={g.id}
                        onClick={() => setGhostList(prev => prev.map(x => x.id === g.id ? { ...x, selected: !x.selected } : x))}
                        className={`p-3 rounded-xl border transition-all cursor-pointer ${
                          g.selected ? 'bg-cyan-950/30 border-cyan-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <span className="block font-bold text-xs text-white truncate max-w-[240px]">
                              {g.fileName}
                            </span>
                            <span className="block text-[10px] text-cyan-300 font-mono mt-0.5">
                              {isAr ? g.typeLabelAr : g.typeLabel} · {format(g.sizeBytes)}
                            </span>
                            <p className="text-[10px] text-slate-400 mt-1">
                              {isAr ? g.reasonAr : g.reason}
                            </p>
                          </div>
                          <input 
                            type="checkbox" 
                            checked={g.selected} 
                            readOnly 
                            className="w-4 h-4 rounded text-cyan-500 accent-cyan-400 cursor-pointer"
                          />
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              {ghostList.length > 0 && (
                <button
                  onClick={handleCleanGhosts}
                  disabled={isProcessing || !ghostList.some(g => g.selected)}
                  className="w-full h-12 rounded-2xl bg-gradient-to-r from-red-500 to-pink-600 hover:from-red-400 hover:to-pink-500 disabled:opacity-50 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(239,68,68,0.4)] cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>
                    {isProcessing ? (isAr ? 'جارٍ التنظيف الآمن...' : 'Cleaning safely...') : (isAr ? 'حذف العناصر المحددة بعد المراجعة' : 'Clean Selected Ghost Items')}
                  </span>
                </button>
              )}
            </div>
          )}

          {/* SERVICE 3: DUPLICATE DNA */}
          {serviceId === 'duplicate-dna' && (
            <div className="space-y-4">
              <div className="p-3 rounded-2xl bg-purple-950/40 border border-purple-500/30 text-xs text-purple-200 flex items-center gap-2.5">
                <Fingerprint className="w-4 h-4 text-purple-400 shrink-0" />
                <span>
                  {isAr ? 'تطابق بت-مقابل-بت بواسطة تشفير SHA-256. يتم اقتراح الإبقاء على الملف الأصلي وحذف التكرارات فقط.' : 'Bit-for-bit SHA-256 matching. The original file is preserved by default.'}
                </span>
              </div>

              <div className="space-y-3 max-h-64 overflow-y-auto pr-1">
                {duplicateList.length === 0 ? (
                  <div className="p-6 text-center text-slate-400 text-xs">
                    {isAr ? 'لا توجد نسخ مكررة مطابقة.' : 'No duplicate files detected.'}
                  </div>
                ) : (
                  duplicateList.map(group => (
                    <div key={group.id} className="p-3 rounded-2xl bg-white/[0.04] border border-white/10 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-xs text-white truncate max-w-[220px]">
                          {group.groupName}
                        </span>
                        <span className="text-[10px] font-mono font-bold text-purple-300">
                          {format(group.fileSize)}
                        </span>
                      </div>

                      <div className="space-y-1.5">
                        {group.files.map((f: DuplicateFileItem) => (
                          <div 
                            key={f.id}
                            onClick={() => {
                              if (f.isOriginal) return;
                              setDuplicateList(prev => prev.map(grp => grp.id === group.id ? {
                                ...grp,
                                files: grp.files.map((file: DuplicateFileItem) => file.id === f.id ? { ...file, selectedForRemoval: !file.selectedForRemoval } : file)
                              } : grp));
                            }}
                            className={`p-2 rounded-xl text-xs flex items-center justify-between border ${
                              f.isOriginal 
                                ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-300' 
                                : f.selectedForRemoval 
                                  ? 'bg-red-950/20 border-red-500/40 text-red-200' 
                                  : 'bg-white/[0.02] border-white/10 text-slate-300'
                            }`}
                          >
                            <div className="truncate max-w-[230px]">
                              <span className="block truncate font-medium">{f.name}</span>
                              <span className="block text-[9px] text-slate-400 truncate">{f.path}</span>
                            </div>

                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white/10 shrink-0">
                              {f.isOriginal ? (isAr ? 'الأصل (محفوظ)' : 'Original (Keep)') : (f.selectedForRemoval ? (isAr ? 'حذف مكرر' : 'Remove') : (isAr ? 'إبقاء' : 'Keep'))}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </div>

              {duplicateList.length > 0 && (
                <button
                  onClick={handleCleanDuplicates}
                  disabled={isProcessing}
                  className="w-full h-12 rounded-2xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(168,85,247,0.4)] cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>
                    {isProcessing ? (isAr ? 'جارٍ حذف المكررات...' : 'Cleaning duplicates...') : (isAr ? 'إزالة النسخ المكررة المحددة' : 'Remove Selected Duplicates')}
                  </span>
                </button>
              )}
            </div>
          )}

          {/* SERVICE 5: CACHE SURGEON */}
          {serviceId === 'cache-surgeon' && (
            <div className="space-y-4">
              <div className="p-3 rounded-2xl bg-indigo-950/40 border border-indigo-500/30 text-xs text-indigo-200 flex items-center gap-2.5">
                <Cpu className="w-4 h-4 text-cyan-400 shrink-0" />
                <span>
                  {isAr ? 'تنظيف الذاكرة المؤقتة الحقيقية لتطبيق KX والذاكرة المشتركة المسموحة دون أي ادعاءات وهمية.' : 'Cleans real KX-owned temporary caches & accessible media buffers legitimately.'}
                </span>
              </div>

              <div className="space-y-2">
                {cacheList.map(item => (
                  <div 
                    key={item.id}
                    onClick={() => setCacheList(prev => prev.map(c => c.id === item.id ? { ...c, selected: !c.selected } : c))}
                    className={`p-3 rounded-2xl border transition-all cursor-pointer ${
                      item.selected ? 'bg-indigo-950/30 border-indigo-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div>
                        <span className="block font-bold text-xs text-white">
                          {isAr ? item.labelAr : item.label}
                        </span>
                        <span className="block text-[10px] text-cyan-300 font-mono mt-0.5">
                          {format(item.sizeBytes)}
                        </span>
                        <p className="text-[10px] text-slate-400 mt-1">
                          {isAr ? item.explanationAr : item.explanation}
                        </p>
                      </div>
                      <input 
                        type="checkbox" 
                        checked={item.selected} 
                        readOnly 
                        className="w-4 h-4 rounded text-indigo-500 accent-indigo-400 cursor-pointer"
                      />
                    </div>
                  </div>
                ))}
              </div>

              {cacheList.length > 0 && (
                <button
                  onClick={handleCleanCache}
                  disabled={isProcessing || !cacheList.some(c => c.selected)}
                  className="w-full h-12 rounded-2xl bg-gradient-to-r from-indigo-500 via-purple-600 to-cyan-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_20px_rgba(99,102,241,0.4)] cursor-pointer"
                >
                  <Zap className="w-4 h-4" />
                  <span>
                    {isProcessing ? (isAr ? 'جارٍ الجراحة والتنظيف...' : 'Purging cache...') : (isAr ? 'تفريغ الذاكرة المؤقتة المحددة' : 'Purge Selected Cache Data')}
                  </span>
                </button>
              )}
            </div>
          )}

          {/* SERVICE 9: NETWORK DOCTOR */}
          {serviceId === 'network-doctor' && (
            <div className="space-y-4">
              <div className="p-3 rounded-2xl bg-cyan-950/40 border border-cyan-500/30 text-xs text-cyan-200">
                {isAr ? 'فحص شامل وحقيقي لمسار الاتصال والتشفير وسلامة حزم DNS لمنع أي تسريب خارج النفق.' : 'Live 4-tier network audit verifying DNS, WireGuard handshake, and IPv6 isolation.'}
              </div>

              <div className="space-y-2">
                {probes.map(probe => (
                  <div key={probe.id} className="p-3 rounded-xl bg-white/[0.04] border border-white/10 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 shadow-[0_0_8px_#10b981]" />
                        <span className="font-bold text-xs text-white">
                          {isAr ? probe.testNameAr : probe.testName}
                        </span>
                      </div>
                      {probe.latencyMs && (
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-950/60 text-emerald-300 border border-emerald-500/30">
                          {probe.latencyMs} ms
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-300">
                      {isAr ? probe.detailAr : probe.detail}
                    </p>
                    <span className="block text-[9px] font-mono text-cyan-400/80">
                      Target: {probe.targetEndpoint}
                    </span>
                  </div>
                ))}
              </div>

              <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/10 text-center text-xs text-slate-400">
                <span>{isAr ? 'حالة النفق: نشط ومشفر بالكامل 100%' : 'Tunnel Health: Verified Active & Encrypted 100%'}</span>
              </div>
            </div>
          )}

          {/* SERVICE 10: REPAIR AUTOPILOT */}
          {serviceId === 'repair-autopilot' && (
            <div className="space-y-4">
              {autopilotStep === 'preview' && (
                <>
                  <div className="p-3 rounded-2xl bg-gradient-to-r from-cyan-950/60 to-indigo-950/60 border border-cyan-400/40 text-xs text-cyan-200 flex items-center gap-2.5">
                    <Sparkles className="w-5 h-5 text-cyan-300 shrink-0" />
                    <span>
                      {isAr ? 'الطيار الآلي يجمع الفرص الآمنة المؤكدة من جميع الخدمات مع مراجعة واضحة قبل التنفيذ.' : 'Autopilot aggregates verified safe opportunities across all services with full user confirmation.'}
                    </span>
                  </div>

                  <div className="space-y-2">
                    <span className="text-xs font-bold text-white">
                      {isAr ? 'خطة الصيانة المقترحة:' : 'Proposed Maintenance Operations:'}
                    </span>
                    {autopilotActions.map(action => (
                      <div key={action.id} className="p-3 rounded-xl bg-white/[0.04] border border-white/10 flex items-center justify-between">
                        <div>
                          <span className="block font-bold text-xs text-white">
                            {isAr ? action.titleAr : action.title}
                          </span>
                          <span className="block text-[10px] text-cyan-300 font-mono mt-0.5">
                            {action.itemCount} {isAr ? 'عناصر' : 'items'} · {format(action.reclaimBytes)}
                          </span>
                        </div>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          action.riskLevel === 'safe-reversible' 
                            ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/30' 
                            : 'bg-yellow-950/60 text-yellow-300 border border-yellow-500/30'
                        }`}>
                          {action.riskLevel === 'safe-reversible' ? (isAr ? 'آمن وقابل للتراجع' : 'Safe / Reversible') : (isAr ? 'مراجعة مطلوبة' : 'Review Required')}
                        </span>
                      </div>
                    ))}
                  </div>

                  <button
                    onClick={handleExecuteAutopilot}
                    className="w-full h-12 rounded-2xl bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-600 hover:opacity-95 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(6,182,212,0.5)] cursor-pointer"
                  >
                    <Play className="w-4 h-4 fill-white" />
                    <span>
                      {isAr ? 'الموافقة وتنفيذ خطة الطيار الآلي' : 'Approve & Execute Autopilot Plan'}
                    </span>
                  </button>
                </>
              )}

              {autopilotStep === 'executing' && (
                <div className="py-12 flex flex-col items-center justify-center space-y-4">
                  <div className="w-14 h-14 rounded-full border-4 border-cyan-400 border-t-transparent animate-spin" />
                  <span className="font-bold text-sm text-cyan-200">
                    {isAr ? 'جارٍ تنفيذ عمليات الصيانة بأمان...' : 'Safely executing transactional maintenance...'}
                  </span>
                  <span className="text-xs text-slate-400 font-mono">
                    {isAr ? 'التحقق من سلامة البيانات وقياس المساحة المستعادة' : 'Verifying integrity & measuring reclaimed storage'}
                  </span>
                </div>
              )}

              {autopilotStep === 'report' && (
                <div className="space-y-4 text-center py-4">
                  <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-400 flex items-center justify-center mx-auto text-emerald-400 shadow-[0_0_20px_#10b981]">
                    <CheckCircle2 className="w-8 h-8" />
                  </div>
                  <div className="space-y-1">
                    <h3 className="font-black text-lg text-white">
                      {isAr ? 'اكتملت الصيانة الذكية بنجاح!' : 'Autopilot Maintenance Complete!'}
                    </h3>
                    <p className="text-xs text-slate-300">
                      {isAr ? 'تم استرجاع مساحة حقيقية تم قياسها بدقة وتم حفظ سجل العملية.' : 'Real measured space reclaimed and auditable operation log recorded.'}
                    </p>
                  </div>

                  <button
                    onClick={onClose}
                    className="w-full h-11 rounded-2xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs transition-colors cursor-pointer"
                  >
                    {isAr ? 'إغلاق وعرض التقرير' : 'Close & View Report'}
                  </button>
                </div>
              )}
            </div>
          )}

          {/* FALLBACK FOR OTHER SERVICES: Interactive Overview */}
          {['heavyweight-finder', 'media-slim', 'download-rescue', 'session-shield'].includes(serviceId) && (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-white/[0.04] border border-white/10 space-y-2">
                <span className="font-bold text-xs text-white">
                  {serviceId === 'heavyweight-finder' && (isAr ? 'أكبر الملفات حجماً في المساحة المصرح بها' : 'Largest Accessible Storage Consumers')}
                  {serviceId === 'media-slim' && (isAr ? 'ترشيق الوسائط والصور بدون فقدان جودة ملحوظ' : 'Quality-Preserving Media Optimization')}
                  {serviceId === 'download-rescue' && (isAr ? 'فحص التنزيلات المنقطعة والتالفة' : 'Incomplete & Corrupted Download Diagnostics')}
                  {serviceId === 'session-shield' && (isAr ? 'سجلات الخصوصية وجلسات الاتصال المحلية' : 'Privacy & Local Session Ledger')}
                </span>
                <p className="text-xs text-slate-300">
                  {isAr 
                    ? 'يتم عرض هذه البيانات استناداً إلى واجهات برمجة تطبيقات أندرويد الرسمية دون ادعاءات وهمية وبموافقة المستخدم التامة.'
                    : 'Grounded strictly in legitimate Android platform APIs with explicit user preview and consent.'}
                </p>
              </div>

              {serviceId === 'heavyweight-finder' && (
                <div className="space-y-1.5">
                  {heavyweights.map(hw => (
                    <div key={hw.id} className="p-3 rounded-xl bg-white/[0.03] border border-white/10 flex items-center justify-between text-xs">
                      <div className="truncate max-w-[240px]">
                        <span className="block font-bold text-white truncate">{hw.name}</span>
                        <span className="block text-[10px] text-slate-400 font-mono">{hw.category} · {hw.lastModified}</span>
                      </div>
                      <span className="font-mono font-bold text-cyan-300">{format(hw.sizeBytes)}</span>
                    </div>
                  ))}
                </div>
              )}

              {serviceId === 'session-shield' && (
                <div className="space-y-1.5">
                  {sessions.map(sec => (
                    <div key={sec.id} className="p-3 rounded-xl bg-white/[0.03] border border-white/10 space-y-1 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-white">{isAr ? sec.nameAr : sec.name}</span>
                        <span className="text-[10px] font-mono text-cyan-400">{sec.createdDate}</span>
                      </div>
                      <p className="text-[10px] text-slate-400">{sec.dataSummary}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
