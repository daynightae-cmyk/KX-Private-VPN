import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialMediaSlimItems, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { MediaSlimItem } from '../../../types/repair';
import { 
  ChevronLeft, 
  Image as ImageIcon, 
  Sliders, 
  CheckCircle2, 
  Sparkles, 
  ShieldCheck, 
  ArrowRight,
  Eye,
  ChevronRight
} from 'lucide-react';

export const Screen17MediaSlim: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [items, setItems] = useState<MediaSlimItem[]>(getInitialMediaSlimItems);
  const [qualitySlider, setQualitySlider] = useState<number>(85); // 85% high fidelity
  const [isCompressing, setIsCompressing] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  const selectedCount = items.filter(i => i.selected).length;
  const potentialSavedBytes = items.filter(i => i.selected).reduce((sum, i) => sum + (i.originalSizeBytes - i.estimatedOptimizedSizeBytes), 0);

  const toggleSelect = (id: string) => {
    setItems(prev => prev.map(i => i.id === id ? { ...i, selected: !i.selected } : i));
  };

  const handleOptimize = () => {
    if (selectedCount === 0) return;
    setIsCompressing(true);
    setTimeout(() => {
      setSuccess(isAr ? `تم ترشيق ${selectedCount} وسائط بنجاح وتوفير ${format(potentialSavedBytes)} مع الحفاظ على النسخ الأصلية!` : `Optimized ${selectedCount} media files, saving ${format(potentialSavedBytes)} while preserving originals!`);
      setItems(prev => prev.map(i => i.selected ? { ...i, selected: false, originalSizeBytes: i.estimatedOptimizedSizeBytes } : i));
      setIsCompressing(false);
    }, 850);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#0c0920] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-amber-700/20 via-purple-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-orange-600/20 via-transparent to-transparent blur-3xl" />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-amber-400 to-orange-500 flex items-center justify-center shadow-[0_0_12px_rgba(245,158,11,0.4)]">
            <ImageIcon className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              MEDIA SLIM™
            </span>
            <span className="text-[9px] font-mono text-amber-300 -mt-0.5">
              TASK_17 · {isAr ? 'استوديو ترشيق وسائط المعرض' : 'Quality-Preserving Media Optimization'}
            </span>
          </div>
        </div>

        <div className="w-9" />
      </div>

      {/* Compression Slider HUD */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-amber-400/40 via-white/10 to-orange-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#1b1236]/80 p-4 space-y-3 border border-white/15">
            <div className="flex items-center justify-between">
              <div>
                <span className="block font-bold text-sm text-white">
                  {isAr ? 'جودة الترشيق البصري' : 'Target Visual Quality'}
                </span>
                <span className="block text-[10px] text-amber-300 font-mono">
                  {isAr ? 'بدون فقدان ملحوظ بالعين المجردة' : 'Perceptually Lossless Tuning'}
                </span>
              </div>

              <div className="px-3 py-1 rounded-xl bg-amber-950/60 border border-amber-400/30 text-xs font-mono font-bold text-amber-200">
                {qualitySlider}%
              </div>
            </div>

            {/* Slider */}
            <div className="space-y-1">
              <input
                type="range"
                min="60"
                max="95"
                value={qualitySlider}
                onChange={(e) => setQualitySlider(Number(e.target.value))}
                className="w-full h-2 rounded-lg bg-black/40 accent-amber-400 cursor-pointer"
              />
              <div className="flex justify-between text-[9px] text-slate-400 font-mono">
                <span>{isAr ? 'أعلى توفير (60%)' : 'Maximum Space (60%)'}</span>
                <span>{isAr ? 'أعلى جودة (95%)' : 'Visually Lossless (95%)'}</span>
              </div>
            </div>

            <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-[10px] text-slate-300 flex items-center gap-2">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span>
                {isAr ? 'حفظ الأصل: يتم إنشاء نسخة محسنة مع الاحتفاظ بالملف الأصلي في مجلد النسخ الاحتياطي.' : 'Preserves Originals: Copies are written safely to a backup vault prior to replacement.'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Success Notice */}
      {success && (
        <div className="z-10 mb-2 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{success}</span>
          </div>
          <button onClick={() => setSuccess(null)} className="text-white/60 hover:text-white">✕</button>
        </div>
      )}

      {/* Media Candidates List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold tracking-wider text-amber-300 uppercase">
            {isAr ? 'مرشحات الترشيق' : 'Optimization Candidates'}
          </span>
          <span className="text-[10px] font-mono text-slate-400">
            {selectedCount} {isAr ? 'محدد' : 'selected'} · {format(potentialSavedBytes)} {isAr ? 'توفير تقديري' : 'savings'}
          </span>
        </div>

        <div className="space-y-2">
          {items.map(item => (
            <div
              key={item.id}
              onClick={() => toggleSelect(item.id)}
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer space-y-2 ${
                item.selected ? 'bg-amber-950/30 border-amber-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="truncate max-w-[240px]">
                  <span className="block font-bold text-xs text-white truncate">
                    {item.fileName}
                  </span>
                  <span className="block text-[10px] text-slate-400 font-mono mt-0.5">
                    {item.resolution} · {item.format}
                  </span>
                </div>

                <input 
                  type="checkbox" 
                  checked={item.selected} 
                  readOnly 
                  className="w-4 h-4 rounded text-amber-500 accent-amber-400 cursor-pointer shrink-0"
                />
              </div>

              {/* Original vs Estimated */}
              <div className="flex items-center justify-between text-[11px] font-mono pt-1 border-t border-white/5">
                <div className="flex items-center gap-1.5 text-slate-300">
                  <span>{format(item.originalSizeBytes)}</span>
                  <ArrowRight className={`w-3 h-3 text-amber-400 ${isAr ? 'rotate-180' : ''}`} />
                  <span className="text-emerald-400 font-bold">{format(item.estimatedOptimizedSizeBytes)}</span>
                </div>

                <span className="px-2 py-0.5 rounded-full bg-amber-400/20 text-amber-300 text-[10px] font-bold">
                  -{item.savingsPercentage}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handleOptimize}
          disabled={selectedCount === 0 || isCompressing}
          className="w-full h-12 rounded-full bg-gradient-to-r from-amber-500 via-orange-500 to-rose-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(245,158,11,0.5)] cursor-pointer"
        >
          <Sparkles className="w-4 h-4" />
          <span>
            {isCompressing 
              ? (isAr ? 'جارٍ ترشيق الوسائط...' : 'Optimizing media...') 
              : (isAr ? `ترشيق الوسائط المحددة (توفير ${format(potentialSavedBytes)})` : `Optimize Selected Media (Save ${format(potentialSavedBytes)})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
