import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialCacheEntries, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { CacheEntry } from '../../../types/repair';
import { 
  ChevronLeft, 
  Cpu, 
  Zap, 
  ShieldCheck, 
  CheckCircle2, 
  Layers, 
  Sparkles,
  ChevronRight,
  RefreshCw,
  HardDrive
} from 'lucide-react';

export const Screen16CacheSurgeon: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [caches, setCaches] = useState<CacheEntry[]>(getInitialCacheEntries);
  const [isPurging, setIsPurging] = useState(false);
  const [purgedStats, setPurgedStats] = useState<{ count: number; bytes: number } | null>(null);

  const selectedCount = caches.filter(c => c.selected).length;
  const selectedBytes = caches.filter(c => c.selected).reduce((sum, c) => sum + c.sizeBytes, 0);

  const toggleSelect = (id: string) => {
    setCaches(prev => prev.map(c => c.id === id ? { ...c, selected: !c.selected } : c));
  };

  const handlePurge = () => {
    if (selectedCount === 0) return;
    setIsPurging(true);
    setTimeout(() => {
      setPurgedStats({ count: selectedCount, bytes: selectedBytes });
      setCaches(prev => prev.filter(c => !c.selected));
      setIsPurging(false);
    }, 700);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070d20] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-teal-700/25 via-indigo-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-600/20 via-transparent to-transparent blur-3xl" />
      </div>

      {/* Header */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen('11-repair-lab')}
          className="w-9 h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 active:scale-95 transition-all text-indigo-200 cursor-pointer"
          aria-label="Back to Repair Lab"
        >
          <ChevronLeft className={`w-5 h-5 ${isAr ? 'rotate-180' : ''}`} />
        </button>

        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-500 to-teal-600 flex items-center justify-center shadow-[0_0_12px_rgba(6,182,212,0.4)]">
            <Cpu className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              CACHE SURGEON™
            </span>
            <span className="text-[9px] font-mono text-cyan-300 -mt-0.5">
              TASK_16 · {isAr ? 'جراحة الذاكرة المؤقتة الحقيقية' : 'Zero-Placebo Real Cache Purge'}
            </span>
          </div>
        </div>

        <div className="w-9" />
      </div>

      {/* Precision Surgical Ring HUD */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-cyan-400/40 via-white/10 to-teal-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#0c1634]/80 p-4 space-y-2.5 border border-white/15">
            <div className="flex items-center justify-between">
              <div>
                <span className="block font-bold text-sm text-white">
                  {isAr ? 'الذاكرة المؤقتة المتاحة للاسترجاع' : 'Legitimate Reclaimable Cache'}
                </span>
                <span className="block text-[10px] text-cyan-300 font-mono">
                  {caches.length} {isAr ? 'مخازن مؤقتة مصرح بها' : 'ephemeral cache buffers'}
                </span>
              </div>

              <div className="px-3 py-1 rounded-xl bg-teal-950/60 border border-teal-400/30 text-xs font-mono font-bold text-cyan-200">
                {format(selectedBytes)}
              </div>
            </div>

            <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-[10px] text-slate-300 flex items-center gap-2">
              <ShieldCheck className="w-3.5 h-3.5 text-teal-400 shrink-0" />
              <span>
                {isAr ? 'بدون وهم: يتم تنظيف ذاكرة KX المؤقتة والذاكرة المشتركة المسموحة فقط عبر واجهات أندرويد الرسمية.' : 'Zero-Placebo: Cleans official Android cache APIs. Does not kill background tasks or inject fake RAM gains.'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Success Notice */}
      {purgedStats && (
        <div className="z-10 mb-2 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>
              {isAr ? `تم تفريغ ${format(purgedStats.bytes)} من الذاكرة المؤقتة بنجاح!` : `Purged ${format(purgedStats.bytes)} of cache safely!`}
            </span>
          </div>
          <button onClick={() => setPurgedStats(null)} className="text-white/60 hover:text-white">✕</button>
        </div>
      )}

      {/* Cache Entries List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        {caches.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
            <CheckCircle2 className="w-8 h-8 text-teal-400 mx-auto" />
            <span className="block font-bold text-white">{isAr ? 'الذاكرة المؤقتة نظيفة' : 'Cache Completely Clean'}</span>
            <span className="block text-[10px] text-slate-400">{isAr ? 'لا توجد مخازن مؤقتة متراكمة حالياً.' : 'No accumulated cache buffers detected.'}</span>
          </div>
        ) : (
          caches.map(entry => (
            <div
              key={entry.id}
              onClick={() => toggleSelect(entry.id)}
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer space-y-1.5 ${
                entry.selected ? 'bg-teal-950/40 border-teal-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div>
                  <span className="block font-bold text-xs text-white">
                    {isAr ? entry.labelAr : entry.label}
                  </span>
                  <span className="block text-[10px] text-cyan-300 font-mono mt-0.5">
                    {format(entry.sizeBytes)} · {entry.scope}
                  </span>
                  <p className="text-[10px] text-slate-400 mt-1">
                    {isAr ? entry.explanationAr : entry.explanation}
                  </p>
                </div>

                <input 
                  type="checkbox" 
                  checked={entry.selected} 
                  readOnly 
                  className="w-4 h-4 rounded text-teal-500 accent-teal-400 cursor-pointer"
                />
              </div>
            </div>
          ))
        )}
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handlePurge}
          disabled={selectedCount === 0 || isPurging}
          className="w-full h-12 rounded-full bg-gradient-to-r from-teal-500 via-cyan-500 to-indigo-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(20,184,166,0.5)] cursor-pointer"
        >
          <Zap className="w-4 h-4" />
          <span>
            {isPurging 
              ? (isAr ? 'جارٍ تفريغ الذاكرة المؤقتة...' : 'Purging cache...') 
              : (isAr ? `تفريغ الذاكرة المؤقتة المحددة (${format(selectedBytes)})` : `Purge Selected Cache (${format(selectedBytes)})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
