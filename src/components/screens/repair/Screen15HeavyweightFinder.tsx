import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialHeavyweightItems, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { HeavyweightItem } from '../../../types/repair';
import { 
  ChevronLeft, 
  Scale, 
  Film, 
  Archive, 
  Music, 
  FileText, 
  Package, 
  Trash2, 
  Filter, 
  CheckCircle2, 
  Sparkles,
  ChevronRight,
  Flame
} from 'lucide-react';

export const Screen15HeavyweightFinder: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [items, setItems] = useState<HeavyweightItem[]>(getInitialHeavyweightItems);
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [isDeleting, setIsDeleting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const filteredItems = items.filter(it => {
    if (filterCategory === 'all') return true;
    return it.category === filterCategory;
  });

  const selectedCount = items.filter(it => it.selected).length;
  const selectedBytes = items.filter(it => it.selected).reduce((sum, it) => sum + it.sizeBytes, 0);

  const toggleSelect = (id: string) => {
    setItems(prev => prev.map(it => it.id === id ? { ...it, selected: !it.selected } : it));
  };

  const handleDeleteSelected = () => {
    if (selectedCount === 0) return;
    setIsDeleting(true);
    setTimeout(() => {
      setNotice(isAr ? `تم حذف ${selectedCount} ملفات كبيرة بنجاح واستعادة ${format(selectedBytes)}!` : `Safely deleted ${selectedCount} large files, reclaiming ${format(selectedBytes)}!`);
      setItems(prev => prev.filter(it => !it.selected));
      setIsDeleting(false);
    }, 700);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070b22] text-white"
    >
      {/* Background Glow */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-15%] w-[130%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-900/30 via-indigo-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-15%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-600/20 via-transparent to-transparent blur-3xl" />
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
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-800 flex items-center justify-center shadow-[0_0_12px_rgba(59,130,246,0.4)]">
            <Scale className="w-4 h-4 text-white" />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base font-bold tracking-wider">
              HEAVYWEIGHT FINDER™
            </span>
            <span className="text-[9px] font-mono text-cyan-300 -mt-0.5">
              TASK_15 · {isAr ? 'مستكشف الملفات الضخمة والخريطة الحرارية' : 'Large Storage Consumers & Heatmap'}
            </span>
          </div>
        </div>

        <div className="w-9" />
      </div>

      {/* Storage Heatmap Bar */}
      <div className="z-10 my-2">
        <div className="relative rounded-3xl p-[1px] bg-gradient-to-b from-blue-400/40 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_12px_36px_rgba(0,0,0,0.5)] overflow-hidden">
          <div className="w-full rounded-3xl bg-[#101742]/80 p-4 space-y-2.5 border border-white/15">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Flame className="w-4 h-4 text-orange-400" />
                <span className="font-bold text-xs text-white">
                  {isAr ? 'الخريطة الحرارية للملفات الكبيرة' : 'Storage Heatmap Distribution'}
                </span>
              </div>
              <span className="text-[10px] font-mono text-cyan-300 font-bold">
                {items.length} {isAr ? 'ملفات ضخمة' : 'heavy files'}
              </span>
            </div>

            {/* Heatmap blocks */}
            <div className="grid grid-cols-4 gap-1.5 h-6 w-full">
              <div className="rounded-lg bg-blue-500/80 flex items-center justify-center text-[9px] font-mono font-bold text-white shadow-xs" title="Video 4.8GB">
                4.8 GB
              </div>
              <div className="rounded-lg bg-indigo-500/70 flex items-center justify-center text-[9px] font-mono font-bold text-white shadow-xs" title="Archive 2.1GB">
                2.1 GB
              </div>
              <div className="rounded-lg bg-purple-500/60 flex items-center justify-center text-[9px] font-mono font-bold text-white shadow-xs" title="Audio 1.4GB">
                1.4 GB
              </div>
              <div className="rounded-lg bg-cyan-500/60 flex items-center justify-center text-[9px] font-mono font-bold text-white shadow-xs" title="Installer 850MB">
                850 MB
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter Chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto py-1 z-10 scrollbar-none">
        {['all', 'video', 'archive', 'audio'].map(cat => (
          <button
            key={cat}
            onClick={() => setFilterCategory(cat)}
            className={`px-3 py-1 rounded-full text-[11px] font-bold font-mono transition-all cursor-pointer ${
              filterCategory === cat
                ? 'bg-gradient-to-r from-blue-500 to-indigo-600 text-white shadow-xs border border-white/30'
                : 'bg-white/5 hover:bg-white/10 text-slate-300 border border-white/10'
            }`}
          >
            {cat === 'all' && (isAr ? 'الكل' : 'All')}
            {cat === 'video' && (isAr ? 'فيديو' : 'Video')}
            {cat === 'archive' && (isAr ? 'أرشيف' : 'Archive')}
            {cat === 'audio' && (isAr ? 'صوت' : 'Audio')}
          </button>
        ))}
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

      {/* Large Files List */}
      <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 my-1 z-10">
        {filteredItems.map(item => (
          <div
            key={item.id}
            onClick={() => toggleSelect(item.id)}
            className={`p-3.5 rounded-2xl border transition-all cursor-pointer space-y-1.5 ${
              item.selected ? 'bg-blue-950/40 border-blue-400/50' : 'bg-white/[0.03] border-white/10'
            }`}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2.5 truncate max-w-[240px]">
                <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center shrink-0">
                  {item.category === 'video' && <Film className="w-4 h-4 text-blue-400" />}
                  {item.category === 'archive' && <Archive className="w-4 h-4 text-purple-400" />}
                  {item.category === 'audio' && <Music className="w-4 h-4 text-pink-400" />}
                  {item.category === 'installer' && <Package className="w-4 h-4 text-cyan-400" />}
                </div>

                <div className="truncate">
                  <span className="block font-bold text-xs text-white truncate">
                    {item.name}
                  </span>
                  <span className="block text-[9px] text-slate-400 font-mono truncate">
                    {item.lastModified} · {item.path}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <span className="font-mono font-bold text-xs text-cyan-300">
                  {format(item.sizeBytes)}
                </span>
                <input 
                  type="checkbox" 
                  checked={item.selected} 
                  readOnly 
                  className="w-4 h-4 rounded text-blue-500 accent-blue-400 cursor-pointer"
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Action Footer */}
      <div className="w-full z-10 pt-1">
        <button
          onClick={handleDeleteSelected}
          disabled={selectedCount === 0 || isDeleting}
          className="w-full h-12 rounded-full bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 hover:opacity-95 disabled:opacity-40 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-[0_4px_25px_rgba(59,130,246,0.5)] cursor-pointer"
        >
          <Trash2 className="w-4 h-4" />
          <span>
            {isDeleting 
              ? (isAr ? 'جارٍ الحذف...' : 'Deleting...') 
              : (isAr ? `حذف الملفات الكبيرة المحددة (${format(selectedBytes)})` : `Delete Selected (${format(selectedBytes)})`)}
          </span>
        </button>
      </div>
    </div>
  );
};
