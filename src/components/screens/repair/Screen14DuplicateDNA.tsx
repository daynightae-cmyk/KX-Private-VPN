import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialDuplicateGroups, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { DuplicateGroup, DuplicateFileItem } from '../../../types/repair';
import { 
  ChevronLeft, 
  Fingerprint, 
  ShieldCheck, 
  Trash2, 
  CheckCircle2, 
  Copy, 
  Check, 
  Sparkles,
  ChevronRight,
  Eye,
  Hash
} from 'lucide-react';

export const Screen14DuplicateDNA: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [duplicateGroups, setDuplicateGroups] = useState<DuplicateGroup[]>(getInitialDuplicateGroups);
  const [isCleaning, setIsCleaning] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const totalPotentialReclaim = duplicateGroups.reduce((acc, grp) => {
    const removalFiles = grp.files.filter(f => f.selectedForRemoval);
    return acc + removalFiles.reduce((s, f) => s + f.sizeBytes, 0);
  }, 0);

  const totalRemovalCount = duplicateGroups.reduce((acc, grp) => {
    return acc + grp.files.filter(f => f.selectedForRemoval).length;
  }, 0);

  const toggleRemoval = (groupId: string, fileId: string) => {
    setDuplicateGroups(prev => prev.map(grp => {
      if (grp.id !== groupId) return grp;
      return {
        ...grp,
        files: grp.files.map(f => {
          if (f.id !== fileId) return f;
          if (f.isOriginal) return f; // Protected original
          return { ...f, selectedForRemoval: !f.selectedForRemoval };
        }),
      };
    }));
  };

  const handleExecuteRemoval = () => {
    if (totalRemovalCount === 0) return;
    setIsCleaning(true);
    setTimeout(() => {
      const reclaimed = totalPotentialReclaim;
      const count = totalRemovalCount;
      setDuplicateGroups(prev => prev.map(grp => ({
        ...grp,
        files: grp.files.filter(f => !f.selectedForRemoval),
      })).filter(grp => grp.files.length > 1));
      setIsCleaning(false);
      setSuccessMsg(isAr ? `تم حذف ${count} نسخ مكررة وتوفير ${format(reclaimed)} بنجاح!` : `Safely purged ${count} duplicates, reclaiming ${format(reclaimed)}!`);
    }, 750);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#090822] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-pink-800/25 via-indigo-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-rose-600/20 via-transparent to-transparent blur-3xl" />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-pink-500 to-rose-600 flex items-center justify-center shadow-[0_0_12px_rgba(244,63,94,0.4)]">
            <Fingerprint className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              DUPLICATE DNA™
            </span>
            <span className="text-[9px] font-mono text-pink-300 -mt-0.5">
              TASK_14 · {isAr ? 'بصمة التكرارات عبر SHA-256' : 'Cryptographic Hash Deduplication'}
            </span>
          </div>
        </div>

        <div className="w-9" />
      </div>

      {/* Summary Card */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-pink-400/40 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#170e30]/80 p-4 space-y-2 border border-white/15">
            <div className="flex items-center justify-between">
              <div>
                <span className="block font-bold text-sm text-white">
                  {isAr ? 'التطابق الرقمي المتطابق' : 'Bit-for-Bit Hash Matches'}
                </span>
                <span className="block text-[10px] text-pink-300 font-mono">
                  {duplicateGroups.length} {isAr ? 'مجموعات مكررة متطابقة' : 'duplicate clusters identified'}
                </span>
              </div>

              <div className="px-3 py-1 rounded-xl bg-pink-950/60 border border-pink-400/30 text-xs font-mono font-bold text-pink-200">
                {format(totalPotentialReclaim)}
              </div>
            </div>

            <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/10 text-[10px] text-slate-300 flex items-center gap-2">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
              <span>
                {isAr ? 'حماية الملف الأصلي: يتم حفظ الملف الأقدم تلقائياً واقتراح إزالة النسخ المكررة فقط.' : 'Original Protection: Oldest original copy is locked; only extra duplicates are marked for removal.'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Success banner */}
      {successMsg && (
        <div className="z-10 mb-2 p-3 rounded-2xl bg-emerald-950/80 border border-emerald-500/40 text-emerald-200 text-xs flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-white/60 hover:text-white">✕</button>
        </div>
      )}

      {/* Duplicate Groups List */}
      <div className="flex-1 overflow-y-auto space-y-3 pr-0.5 my-1 z-10">
        {duplicateGroups.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-xs rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
            <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
            <span className="block font-bold text-white">{isAr ? 'لا توجد مكررات' : 'No Duplicates Found'}</span>
            <span className="block text-[10px] text-slate-400">{isAr ? 'جميع الملفات في المساحة المصرح بها فريدة من نوعها.' : 'All accessible files are unique.'}</span>
          </div>
        ) : (
          duplicateGroups.map(grp => (
            <div key={grp.id} className="p-3.5 rounded-2xl bg-white/[0.04] border border-white/10 space-y-2.5">
              <div className="flex items-center justify-between">
                <div>
                  <span className="block font-bold text-xs text-white truncate max-w-[220px]">
                    {grp.groupName}
                  </span>
                  <div className="flex items-center gap-1.5 text-[9px] text-slate-400 font-mono mt-0.5">
                    <Hash className="w-2.5 h-2.5 text-pink-400" />
                    <span className="truncate max-w-[170px]">{grp.exactHash.substring(0, 16)}...</span>
                  </div>
                </div>

                <span className="text-[10px] font-mono font-bold text-pink-300">
                  {format(grp.fileSize)}
                </span>
              </div>

              {/* Files in this cluster */}
              <div className="space-y-1.5">
                {grp.files.map((file: DuplicateFileItem) => (
                  <div 
                    key={file.id}
                    onClick={() => toggleRemoval(grp.id, file.id)}
                    className={`p-2.5 rounded-xl text-xs flex items-center justify-between border transition-all cursor-pointer ${
                      file.isOriginal 
                        ? 'bg-emerald-950/25 border-emerald-500/40 text-emerald-200 cursor-default' 
                        : file.selectedForRemoval 
                          ? 'bg-pink-950/30 border-pink-500/50 text-pink-200' 
                          : 'bg-white/[0.02] border-white/10 text-slate-300'
                    }`}
                  >
                    <div className="truncate max-w-[220px]">
                      <span className="block truncate font-medium">{file.name}</span>
                      <span className="block text-[9px] text-slate-400 truncate font-mono">{file.path}</span>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <span className={`px-2 py-0.5 rounded-full text-[9px] font-bold ${
                        file.isOriginal 
                          ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' 
                          : file.selectedForRemoval 
                            ? 'bg-red-500/20 text-red-300 border border-red-500/40' 
                            : 'bg-white/10 text-slate-400'
                      }`}>
                        {file.isOriginal ? (isAr ? 'الأصل (محفوظ)' : 'Keep Original') : (file.selectedForRemoval ? (isAr ? 'حذف' : 'Remove') : (isAr ? 'إبقاء' : 'Keep'))}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handleExecuteRemoval}
          disabled={totalRemovalCount === 0 || isCleaning}
          className="w-full h-12 rounded-full bg-gradient-to-r from-pink-500 via-rose-600 to-purple-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(244,63,94,0.5)] cursor-pointer"
        >
          <Trash2 className="w-4 h-4" />
          <span>
            {isCleaning 
              ? (isAr ? 'جارٍ إزالة النسخ المكررة...' : 'Purging duplicates...') 
              : (isAr ? `إزالة النسخ المكررة (${format(totalPotentialReclaim)})` : `Remove Selected Copies (${format(totalPotentialReclaim)})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
