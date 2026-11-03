import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialGhostCandidates, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { GhostCandidate } from '../../../types/repair';
import { 
  ChevronLeft, 
  Ghost, 
  ShieldCheck, 
  Trash2, 
  CheckCircle2, 
  AlertTriangle, 
  Sparkles, 
  Info,
  Check,
  ChevronRight,
  FolderX,
  FolderCheck
} from 'lucide-react';

export const Screen13GhostHunter: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [ghosts, setGhosts] = useState<GhostCandidate[]>(getInitialGhostCandidates);
  const [isCleaning, setIsCleaning] = useState(false);
  const [cleanedSummary, setCleanedSummary] = useState<{ count: number; bytes: number } | null>(null);

  const selectedCount = ghosts.filter(g => g.selected).length;
  const selectedBytes = ghosts.filter(g => g.selected).reduce((sum, g) => sum + g.sizeBytes, 0);

  const handleToggleAll = () => {
    const allSelected = ghosts.every(g => g.selected);
    setGhosts(prev => prev.map(g => ({ ...g, selected: !allSelected })));
  };

  const handleToggleItem = (id: string) => {
    setGhosts(prev => prev.map(g => g.id === id ? { ...g, selected: !g.selected } : g));
  };

  const handleExecuteCleanup = () => {
    if (selectedCount === 0) return;
    setIsCleaning(true);
    setTimeout(() => {
      setCleanedSummary({ count: selectedCount, bytes: selectedBytes });
      setGhosts(prev => prev.filter(g => !g.selected));
      setIsCleaning(false);
    }, 700);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#0a0720] text-white"
    >
      {/* Background Spectral Nebula */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-15%] w-[130%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-purple-800/25 via-indigo-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[5%] right-[-10%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-pink-600/20 via-transparent to-transparent blur-3xl" />

        {/* Ambient Wave Rings */}
        <div className="absolute top-[25%] left-[20%] w-64 h-64 rounded-full border border-purple-500/10 pointer-events-none animate-ping" style={{ animationDuration: '4s' }} />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-purple-500 to-indigo-600 flex items-center justify-center shadow-[0_0_12px_rgba(168,85,247,0.4)]">
            <Ghost className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              GHOST HUNTER™
            </span>
            <span className="text-[9px] font-mono text-purple-300 -mt-0.5">
              TASK_13 · {isAr ? 'صائد المجلدات والملفات الفارغة' : 'Empty Tree & Zero-Byte Scanner'}
            </span>
          </div>
        </div>

        <button 
          onClick={handleToggleAll}
          className="px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-[10px] text-purple-300 font-mono hover:bg-white/10 cursor-pointer"
        >
          {isAr ? 'تحديد' : 'Toggle'}
        </button>
      </div>

      {/* Spectral Scanner HUD */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-purple-400/40 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#140e34]/80 p-4 space-y-2.5 border border-white/15">
            <div className="flex items-center justify-between">
              <div>
                <span className="block font-bold text-sm text-white">
                  {isAr ? 'نتائج الفحص الطيفي' : 'Spectral Scan Findings'}
                </span>
                <span className="block text-[10px] text-purple-300 font-mono">
                  {ghosts.length} {isAr ? 'عناصر مؤكدة جاهزة للمراجعة' : 'verified candidates identified'}
                </span>
              </div>

              {/* Space badge */}
              <div className="px-3 py-1 rounded-xl bg-purple-950/60 border border-purple-400/30 text-xs font-mono font-bold text-purple-200">
                {format(selectedBytes)}
              </div>
            </div>

            {/* Zero-damage guarantee */}
            <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-[10px] text-slate-300 flex items-center gap-2">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span>
                {isAr ? 'ضمان الأمان: لا يتم حذف أي مجلد يحتوي على ملفات أو وثائق شخصية.' : 'Safety Guarantee: Directories with personal documents are strictly protected.'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Cleaned Banner */}
      {cleanedSummary && (
        <div className="z-10 mb-2 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>
              {isAr ? `تم تنظيف ${cleanedSummary.count} عناصر واسترجاع ${format(cleanedSummary.bytes)} بنجاح!` : `Cleaned ${cleanedSummary.count} ghost items, reclaimed ${format(cleanedSummary.bytes)}!`}
            </span>
          </div>
          <button onClick={() => setCleanedSummary(null)} className="text-white/60 hover:text-white">✕</button>
        </div>
      )}

      {/* Candidates Checklist */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold tracking-wider text-purple-300 uppercase">
            {isAr ? 'قائمة المراجعة قبل التنظيف' : 'Review Before Cleanup'}
          </span>
          <span className="text-[10px] font-mono text-slate-400">
            {selectedCount} / {ghosts.length} {isAr ? 'محدد' : 'selected'}
          </span>
        </div>

        {ghosts.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
            <FolderCheck className="w-8 h-8 text-emerald-400 mx-auto" />
            <span className="block font-bold text-white">{isAr ? 'النظام نظيف تماماً' : 'System Completely Clean'}</span>
            <span className="block text-[10px] text-slate-400">{isAr ? 'لم يتم العثور على أي مجلدات فارغة أو ملفات تالفة' : 'No empty folders or zero-byte placeholders remain.'}</span>
          </div>
        ) : (
          <div className="space-y-2">
            {ghosts.map(g => (
              <div 
                key={g.id}
                onClick={() => handleToggleItem(g.id)}
                className={`p-3.5 rounded-2xl border transition-all cursor-pointer space-y-1.5 ${
                  g.selected ? 'bg-purple-950/40 border-purple-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="truncate max-w-[260px]">
                    <span className="block font-bold text-xs text-white truncate">
                      {g.fileName}
                    </span>
                    <span className="block text-[9px] text-slate-400 truncate font-mono">
                      {g.path}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    <span className="px-1.5 py-0.2 rounded-full bg-purple-400/20 text-[9px] font-mono text-purple-200 border border-purple-400/30">
                      {g.confidence}% {isAr ? 'ثقة' : 'match'}
                    </span>
                    <input 
                      type="checkbox" 
                      checked={g.selected} 
                      readOnly 
                      className="w-4 h-4 rounded text-purple-500 accent-purple-400 cursor-pointer"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between text-[10px] pt-1 border-t border-white/5">
                  <span className="text-purple-300 font-medium">
                    {isAr ? g.typeLabelAr : g.typeLabel}
                  </span>
                  <span className="text-slate-300 font-mono">
                    {format(g.sizeBytes)}
                  </span>
                </div>

                <p className="text-[10px] text-slate-400">
                  {isAr ? g.reasonAr : g.reason}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1 space-y-2">
        <button
          onClick={handleExecuteCleanup}
          disabled={selectedCount === 0 || isCleaning}
          className="w-full h-12 rounded-full bg-gradient-to-r from-purple-600 to-pink-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(168,85,247,0.5)] cursor-pointer"
        >
          <Trash2 className="w-4 h-4" />
          <span>
            {isCleaning 
              ? (isAr ? 'جارٍ التنظيف الآمن...' : 'Cleaning safely...') 
              : (isAr ? `تنظيف العناصر المحددة (${format(selectedBytes)})` : `Clean Selected Ghost Files (${format(selectedBytes)})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
