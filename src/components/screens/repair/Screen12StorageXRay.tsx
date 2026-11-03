import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialStorageStats, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { StorageCategoryBreakdown } from '../../../types/repair';
import { 
  ChevronLeft, 
  HardDrive, 
  Layers, 
  Filter, 
  ArrowUpDown, 
  Info, 
  Sparkles, 
  PieChart, 
  Calendar, 
  FolderCheck,
  CheckCircle2,
  ChevronRight
} from 'lucide-react';

export const Screen12StorageXRay: React.FC = () => {
  const { setCurrentScreen, locale, t } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [stats, setStats] = useState(getInitialStorageStats);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'size' | 'count'>('size');

  const filteredCategories = [...stats.categories].sort((a, b) => {
    return sortBy === 'size' ? b.bytes - a.bytes : b.fileCount - a.fileCount;
  });

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070a20] text-white"
    >
      {/* Background Nebulae */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-[20%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-cyan-600/25 via-indigo-900/15 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-15%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-blue-600/20 via-transparent to-transparent blur-3xl" />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-cyan-400 to-blue-600 flex items-center justify-center shadow-[0_0_12px_rgba(6,182,212,0.4)]">
            <HardDrive className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              STORAGE X-RAY™
            </span>
            <span className="text-[9px] font-mono text-cyan-300 -mt-0.5">
              TASK_12 · {isAr ? 'ذكاء وتحليل التخزين' : 'Deep Storage Intelligence'}
            </span>
          </div>
        </div>

        <button 
          onClick={() => setSortBy(prev => prev === 'size' ? 'count' : 'size')}
          className="px-2.5 py-1 rounded-full bg-white/5 border border-white/10 flex items-center gap-1 text-[10px] text-cyan-300 font-mono hover:bg-white/10 cursor-pointer"
          title="Toggle Sort"
        >
          <ArrowUpDown className="w-3 h-3" />
          <span>{sortBy === 'size' ? (isAr ? 'الحجم' : 'Size') : (isAr ? 'العدد' : 'Count')}</span>
        </button>
      </div>

      {/* Central 3D Concentric Storage Core */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-cyan-400/35 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#11163e]/80 p-4 space-y-3 border border-white/15">
            {/* 3D Circular Core Graphic */}
            <div className="flex items-center justify-center py-2">
              <div className="relative w-36 h-36 flex items-center justify-center">
                {/* Luminous Glow */}
                <div className="absolute inset-[-10%] rounded-full bg-cyan-400/20 blur-xl animate-pulse-glow" />
                {/* Outer Ring */}
                <svg className="w-full h-full rotate-[-90deg]" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="42" fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="9" />
                  <circle 
                    cx="50" 
                    cy="50" 
                    r="42" 
                    fill="none" 
                    stroke="url(#storage-xray-grad)" 
                    strokeWidth="9" 
                    strokeDasharray="264" 
                    strokeDashoffset={264 - (264 * stats.usedPercentage) / 100}
                    strokeLinecap="round"
                    className="transition-all duration-1000"
                  />
                  <defs>
                    <linearGradient id="storage-xray-grad" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stopColor="#38bdf8" />
                      <stop offset="50%" stopColor="#818cf8" />
                      <stop offset="100%" stopColor="#c084fc" />
                    </linearGradient>
                  </defs>
                </svg>

                {/* Core Text */}
                <div className="absolute flex flex-col items-center justify-center text-center">
                  <span className="text-2xl font-black font-mono tracking-tight text-white">
                    {stats.usedPercentage}%
                  </span>
                  <span className="text-[10px] text-cyan-300 font-bold tracking-wider uppercase -mt-0.5">
                    {isAr ? 'مستخدم' : 'USED'}
                  </span>
                </div>
              </div>
            </div>

            {/* Total Capacity Bar */}
            <div className="flex items-center justify-between text-xs font-mono px-1">
              <span className="text-slate-300">{isAr ? 'المستخدم:' : 'Used:'} {format(stats.usedBytes)}</span>
              <span className="text-cyan-300 font-bold">{isAr ? 'المتاح:' : 'Free:'} {format(stats.freeBytes)}</span>
            </div>

            {/* Scope Disclosure */}
            <div className="p-2.5 rounded-xl bg-cyan-950/40 border border-cyan-400/20 text-[10px] text-cyan-200/90 flex items-center gap-2">
              <Info className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
              <span>
                {isAr ? stats.scopeDescriptionAr : stats.scopeDescription}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Categories Drilldown Ledger */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold tracking-wider text-cyan-300 uppercase">
            {isAr ? 'الفئات المصنفة' : 'Categorized Storage Breakdown'}
          </span>
          <span className="text-[10px] font-mono text-indigo-300">
            {filteredCategories.length} {isAr ? 'فئات' : 'categories'}
          </span>
        </div>

        <div className="space-y-2">
          {filteredCategories.map((cat: StorageCategoryBreakdown) => (
            <div 
              key={cat.id}
              onClick={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
              className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10 hover:border-cyan-400/40 transition-all cursor-pointer space-y-2"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="w-3.5 h-3.5 rounded-full shadow-xs" style={{ backgroundColor: cat.color }} />
                  <div>
                    <span className="block font-bold text-xs text-white">
                      {isAr ? cat.nameAr : cat.name}
                    </span>
                    <span className="block text-[10px] text-slate-400 font-mono">
                      {cat.fileCount} {isAr ? 'ملف مسجل' : 'indexed files'} · {cat.percentage}%
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <span className="font-mono font-bold text-xs text-white">
                    {format(cat.bytes)}
                  </span>
                  <ChevronRight className={`w-4 h-4 text-slate-400 transition-transform ${selectedCategory === cat.id ? 'rotate-90' : ''}`} />
                </div>
              </div>

              {/* Proportional Mini Bar */}
              <div className="h-1.5 w-full rounded-full bg-black/40 overflow-hidden">
                <div 
                  className="h-full rounded-full" 
                  style={{ width: `${cat.percentage}%`, backgroundColor: cat.color }} 
                />
              </div>

              {/* Expanded Drilldown Info */}
              {selectedCategory === cat.id && (
                <div className="pt-2 border-t border-white/10 text-[10px] text-slate-300 space-y-1 animate-fade-in font-mono">
                  <div className="flex justify-between">
                    <span>{isAr ? 'متوسط حجم الملف:' : 'Average file size:'}</span>
                    <span className="text-cyan-300">{format(Math.round(cat.bytes / Math.max(1, cat.fileCount)))}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>{isAr ? 'الموقع المصرح:' : 'Authorized Scope:'}</span>
                    <span className="text-slate-400">/storage/emulated/0/{cat.id}</span>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Bottom CTA */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={() => setCurrentScreen('13-ghost-hunter')}
          className="w-full h-12 rounded-full bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-600 hover:opacity-95 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(6,182,212,0.5)] cursor-pointer"
        >
          <span>{isAr ? 'المتابعة إلى فحص المخلفات (Ghost Hunter™)' : 'Proceed to Ghost Hunter™ Scan'}</span>
          <ChevronRight className={`w-4 h-4 ${isAr ? 'rotate-180' : ''}`} />
        </button>
      </div>
    </div>
  );
};
