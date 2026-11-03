import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { getInitialSessionRecords } from '../../../utils/repair-engine';
import { SessionShieldRecord } from '../../../types/repair';
import { 
  ChevronLeft, 
  ShieldCheck, 
  Key, 
  Trash2, 
  Lock, 
  Database, 
  Cpu, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle
} from 'lucide-react';

export const Screen19SessionShield: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';

  const [records, setRecords] = useState<SessionShieldRecord[]>(getInitialSessionRecords);
  const [zeroTraceActive, setZeroTraceActive] = useState<boolean>(true);
  const [isWiping, setIsWiping] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const handleWipeLogs = () => {
    setIsWiping(true);
    setTimeout(() => {
      setRecords(prev => prev.filter(r => !r.isRemovable));
      setIsWiping(false);
      setNotice(isAr ? 'تم مسح جميع السجلات التشخيصية والذاكرة المخبأة بنجاح!' : 'All diagnostic logs and ephemeral cached endpoints purged safely!');
    }, 600);
  };

  const handleRotateKeys = () => {
    setNotice(isAr ? 'تم تدوير مفاتيح التشفير اللحظية بنجاح في الذاكرة المعزولة!' : 'Ephemeral Curve25519 session keys rotated safely in isolated RAM!');
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070f20] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[8%] left-[-15%] w-[130%] h-[55%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-700/20 via-indigo-950/20 to-transparent blur-3xl" />
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

        <div className="flex flex-col items-center">
          <div className="flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-cyan-400" />
            <h1 className="text-base font-bold tracking-wide text-white">
              {isAr ? 'درع الجلسات والخصوصية' : 'Session & Privacy Shield'}
            </h1>
          </div>
          <span className="text-[10px] text-cyan-300/80 font-mono tracking-widest uppercase">
            TASK_19 • ZERO-TRACE VAULT
          </span>
        </div>

        <div className="w-9 h-9 rounded-full bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
          <Lock className="w-4 h-4" />
        </div>
      </div>

      {/* Main Content Scrollable Area */}
      <div className="z-10 flex-1 overflow-y-auto space-y-3.5 my-2 pr-0.5 custom-scrollbar">
        {/* Feedback Notice */}
        {notice && (
          <div className="p-2.5 rounded-xl bg-emerald-500/15 border border-emerald-500/30 flex items-center gap-2 text-xs text-emerald-300 animate-fadeIn">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span className="flex-1 font-medium">{notice}</span>
            <button 
              onClick={() => setNotice(null)}
              className="text-[10px] text-emerald-400/80 hover:text-emerald-200 uppercase font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {/* Security Status Card */}
        <div className="p-3.5 rounded-2xl bg-gradient-to-br from-white/[0.07] to-white/[0.02] border border-cyan-500/30 backdrop-blur-md shadow-[0_8px_24px_rgba(0,0,0,0.3)]">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-300">
                <Cpu className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-semibold text-white">
                  {isAr ? 'وضع الذاكرة المؤقتة المعزولة' : 'Isolated RAM-Only Execution'}
                </h3>
                <p className="text-[10px] text-slate-300">
                  {isAr ? 'لا يتم كتابة أي مفاتيح خاصة على القرص' : 'Zero private keys persisted to persistent flash'}
                </p>
              </div>
            </div>
            
            {/* Toggle switch */}
            <button
              onClick={() => setZeroTraceActive(!zeroTraceActive)}
              className={`w-11 h-6 rounded-full transition-colors p-0.5 flex items-center cursor-pointer ${
                zeroTraceActive ? 'bg-cyan-500 justify-end' : 'bg-slate-700 justify-start'
              }`}
            >
              <div className="w-5 h-5 rounded-full bg-white shadow-md transform transition-transform" />
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2 pt-1 border-t border-white/5">
            <div className="p-2 rounded-xl bg-black/20 border border-white/5">
              <span className="text-[10px] text-slate-400 block mb-0.5">
                {isAr ? 'حالة التشفير' : 'Encryption State'}
              </span>
              <span className="text-xs font-bold text-cyan-300 flex items-center gap-1">
                <Lock className="w-3 h-3 text-cyan-400" /> Curve25519
              </span>
            </div>
            <div className="p-2 rounded-xl bg-black/20 border border-white/5">
              <span className="text-[10px] text-slate-400 block mb-0.5">
                {isAr ? 'السجلات المتبقية' : 'Active Footprint'}
              </span>
              <span className="text-xs font-bold text-emerald-400 flex items-center gap-1">
                <Database className="w-3 h-3 text-emerald-400" /> {records.length} {isAr ? 'سجلات' : 'records'}
              </span>
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={handleRotateKeys}
            className="p-2.5 rounded-xl bg-white/5 border border-white/10 hover:border-cyan-400/40 hover:bg-white/10 transition-all flex items-center justify-center gap-1.5 text-xs font-medium text-slate-200 cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5 text-cyan-400" />
            <span>{isAr ? 'تدوير المفاتيح' : 'Rotate Ephemeral Keys'}</span>
          </button>
          <button
            onClick={handleWipeLogs}
            disabled={isWiping || records.filter(r => r.isRemovable).length === 0}
            className="p-2.5 rounded-xl bg-rose-500/10 border border-rose-500/20 hover:bg-rose-500/20 hover:border-rose-500/40 transition-all flex items-center justify-center gap-1.5 text-xs font-medium text-rose-300 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Trash2 className="w-3.5 h-3.5 text-rose-400" />
            <span>{isWiping ? (isAr ? 'جارٍ المسح...' : 'Purging...') : (isAr ? 'مسح السجلات التشخيصية' : 'Wipe Diagnostic Logs')}</span>
          </button>
        </div>

        {/* Session Records List */}
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-semibold text-slate-300">
              {isAr ? 'سجلات الجلسات المخزنة محلياً' : 'Local Session & Diagnostic Entries'}
            </span>
            <span className="text-[10px] text-slate-400">
              {records.filter(r => r.isRemovable).length} {isAr ? 'قابلة للمسح الآمن' : 'Safe to wipe'}
            </span>
          </div>

          {records.map((record) => (
            <div 
              key={record.id}
              className="p-3 rounded-xl bg-white/[0.04] border border-white/10 backdrop-blur-sm space-y-1.5"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2">
                  <div className={`w-7 h-7 rounded-lg flex items-center justify-center ${
                    record.type === 'active-tunnel' 
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' 
                      : 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
                  }`}>
                    {record.type === 'active-tunnel' ? <Key className="w-3.5 h-3.5" /> : <Database className="w-3.5 h-3.5" />}
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-white">
                      {isAr ? record.nameAr : record.name}
                    </h4>
                    <span className="text-[10px] text-slate-400 block">
                      {record.createdDate}
                    </span>
                  </div>
                </div>

                <span className={`text-[9px] px-2 py-0.5 rounded-full font-mono font-semibold ${
                  record.isRemovable 
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' 
                    : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                }`}>
                  {record.isRemovable ? (isAr ? 'سجل تشخيصي' : 'PURGEABLE') : (isAr ? 'نشط في الذاكرة' : 'ACTIVE IN RAM')}
                </span>
              </div>

              <p className="text-[11px] text-slate-300/90 pl-9">
                {record.dataSummary}
              </p>
            </div>
          ))}
        </div>

        {/* Security Disclaimers */}
        <div className="p-3 rounded-xl bg-cyan-950/30 border border-cyan-500/20 text-[11px] text-cyan-200/90 space-y-1 flex items-start gap-2">
          <AlertCircle className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
          <p>
            {isAr 
              ? 'تلتزم KX VPN بمبدأ عدم الاحتفاظ بالسجلات الصارم (No-Logs Policy). كافة السجلات المذكورة هي قياسات محلية داخل جهازك فقط وتُحذف فورياً عند إغلاق التطبيق.'
              : 'KX VPN enforces a strict Zero-Log Privacy Policy. All items listed above reside strictly within local app-sandbox storage and are purged instantly when requested.'
            }
          </p>
        </div>
      </div>

      {/* Footer Return Action */}
      <div className="z-10 pt-2 border-t border-white/10">
        <button
          onClick={() => setCurrentScreen('11-repair-lab')}
          className="w-full py-2.5 rounded-xl bg-gradient-to-r from-cyan-600/80 to-blue-600/80 hover:from-cyan-500 hover:to-blue-500 text-white font-medium text-xs shadow-[0_4px_16px_rgba(6,182,212,0.3)] active:scale-[0.99] transition-all cursor-pointer"
        >
          {isAr ? 'العودة لمختبر الصيانة' : 'Return to Repair Lab'}
        </button>
      </div>
    </div>
  );
};
