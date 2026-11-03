import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialDownloadIssues, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { DownloadIssueItem } from '../../../types/repair';
import { 
  ChevronLeft, 
  DownloadCloud, 
  AlertTriangle, 
  CheckCircle2, 
  Trash2, 
  ShieldCheck, 
  Archive, 
  FileWarning,
  ChevronRight
} from 'lucide-react';

export const Screen18DownloadRescue: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [downloads, setDownloads] = useState<DownloadIssueItem[]>(getInitialDownloadIssues);
  const [isQuarantining, setIsQuarantining] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const selectedCount = downloads.filter(d => d.selected).length;

  const toggleSelect = (id: string) => {
    setDownloads(prev => prev.map(d => d.id === id ? { ...d, selected: !d.selected } : d));
  };

  const handleRescueOrQuarantine = () => {
    if (selectedCount === 0) return;
    setIsQuarantining(true);
    setTimeout(() => {
      setNotice(isAr ? `تم عزل ومعالجة ${selectedCount} تنزيلات منقطعة بنجاح!` : `Quarantined and handled ${selectedCount} corrupted download items!`);
      setDownloads(prev => prev.filter(d => !d.selected));
      setIsQuarantining(false);
    }, 700);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070f20] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-emerald-800/20 via-indigo-950/20 to-transparent blur-3xl" />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-emerald-400 to-cyan-600 flex items-center justify-center shadow-[0_0_12px_rgba(16,185,129,0.4)]">
            <DownloadCloud className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              DOWNLOAD RESCUE™
            </span>
            <span className="text-[9px] font-mono text-emerald-300 -mt-0.5">
              TASK_18 · {isAr ? 'تشخيص التنزيلات المنقطعة والتالفة' : 'Incomplete Download Diagnostic'}
            </span>
          </div>
        </div>

        <div className="w-9" />
      </div>

      {/* Overview Card */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-emerald-400/40 via-white/10 to-cyan-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#0e1d35]/80 p-4 space-y-2.5 border border-white/15">
            <div className="flex items-center justify-between">
              <div>
                <span className="block font-bold text-sm text-white">
                  {isAr ? 'تنبيهات سلامة التنزيل' : 'Download Integrity Scanner'}
                </span>
                <span className="block text-[10px] text-emerald-300 font-mono">
                  {downloads.length} {isAr ? 'ملفات تحتاج للمراجعة' : 'problematic files flagged'}
                </span>
              </div>

              <div className="px-3 py-1 rounded-xl bg-emerald-950/60 border border-emerald-400/30 text-xs font-mono font-bold text-emerald-200">
                {selectedCount} {isAr ? 'محدد' : 'selected'}
              </div>
            </div>

            <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-[10px] text-slate-300 flex items-center gap-2">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span>
                {isAr ? 'فحص دقيق: يتم كشف بقايا التنزيل المنقطعة مثل .crdownload دون المساس بالملفات السليمة.' : 'Accurate Inspection: Inspects signatures and incomplete extensions without guessing.'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Notice */}
      {notice && (
        <div className="z-10 mb-2 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{notice}</span>
          </div>
          <button onClick={() => setNotice(null)} className="text-white/60 hover:text-white">✕</button>
        </div>
      )}

      {/* Issues List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        {downloads.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
            <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
            <span className="block font-bold text-white">{isAr ? 'جميع التنزيلات سليمة' : 'All Downloads Intact'}</span>
            <span className="block text-[10px] text-slate-400">{isAr ? 'لا توجد أي ملفات مؤقتة منقطعة أو تالفة.' : 'No truncated or orphaned downloads found.'}</span>
          </div>
        ) : (
          downloads.map(item => (
            <div
              key={item.id}
              onClick={() => toggleSelect(item.id)}
              className={`p-3.5 rounded-2xl border transition-all cursor-pointer space-y-1.5 ${
                item.selected ? 'bg-emerald-950/30 border-emerald-400/50' : 'bg-white/[0.03] border-white/10 opacity-70'
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="truncate max-w-[240px]">
                  <span className="block font-bold text-xs text-white truncate">
                    {item.fileName}
                  </span>
                  <span className="block text-[10px] text-amber-300 font-mono mt-0.5">
                    {isAr ? item.issueLabelAr : item.issueLabel}
                  </span>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <span className="font-mono text-xs font-bold text-slate-300">
                    {format(item.sizeBytes)}
                  </span>
                  <input 
                    type="checkbox" 
                    checked={item.selected} 
                    readOnly 
                    className="w-4 h-4 rounded text-emerald-500 accent-emerald-400 cursor-pointer"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between text-[10px] font-mono text-slate-400 pt-1 border-t border-white/5">
                <span>Action: {item.recommendedAction.toUpperCase()}</span>
                <span className="text-emerald-400">{isAr ? 'تأكيد المستخدم مطلوب' : 'User Approval Required'}</span>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handleRescueOrQuarantine}
          disabled={selectedCount === 0 || isQuarantining}
          className="w-full h-12 rounded-full bg-gradient-to-r from-emerald-500 via-teal-500 to-cyan-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(16,185,129,0.5)] cursor-pointer"
        >
          <Trash2 className="w-4 h-4" />
          <span>
            {isQuarantining 
              ? (isAr ? 'جارٍ المعالجة...' : 'Processing...') 
              : (isAr ? `معالجة وعزل العناصر المحددة (${selectedCount})` : `Quarantine/Clean Selected (${selectedCount})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
