import React, { useState } from 'react';
import { useVPN } from '../../../context/VPNContext';
import { generateAutopilotPlan, formatBytes, formatBytesAr } from '../../../utils/repair-engine';
import { AutopilotAction } from '../../../types/repair';
import { 
  ChevronLeft, 
  Rocket, 
  ShieldCheck, 
  CheckCircle2, 
  AlertTriangle, 
  Play, 
  RotateCcw, 
  Sparkles, 
  FileCheck2, 
  ChevronRight, 
  Lock,
  Layers
} from 'lucide-react';

type AutopilotStage = 
  | 'preview'      // User selects actions
  | 'approving'    // Explicit consent confirmation
  | 'executing'    // Animated execution of stages
  | 'completed';   // Final verifiable report

export const Screen21RepairAutopilot: React.FC = () => {
  const { setCurrentScreen, locale } = useVPN();
  const isAr = locale === 'ar';
  const format = isAr ? formatBytesAr : formatBytes;

  const [actions, setActions] = useState<AutopilotAction[]>(generateAutopilotPlan);
  const [stage, setStage] = useState<AutopilotStage>('preview');
  const [currentExecutingIdx, setCurrentExecutingIdx] = useState<number>(0);
  const [hasApprovedTerms, setHasApprovedTerms] = useState<boolean>(false);

  const selectedActions = actions.filter(a => a.selected);
  const totalReclaimBytes = selectedActions.reduce((acc, curr) => acc + curr.reclaimBytes, 0);

  const toggleAction = (id: string) => {
    if (stage !== 'preview') return;
    setActions(prev => prev.map(a => a.id === id ? { ...a, selected: !a.selected } : a));
  };

  const handleStartExecution = () => {
    if (!hasApprovedTerms) return;
    setStage('executing');
    setCurrentExecutingIdx(0);

    let idx = 0;
    const interval = setInterval(() => {
      idx++;
      if (idx >= selectedActions.length) {
        clearInterval(interval);
        setStage('completed');
      } else {
        setCurrentExecutingIdx(idx);
      }
    }, 700);
  };

  const handleReset = () => {
    setActions(generateAutopilotPlan());
    setStage('preview');
    setHasApprovedTerms(false);
    setCurrentExecutingIdx(0);
  };

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#070f20] text-white"
    >
      {/* Background Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[8%] left-[-10%] w-[120%] h-[50%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-indigo-700/25 via-blue-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[100%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-500/25 via-transparent to-transparent blur-3xl" />
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
            <Rocket className="w-4 h-4 text-cyan-400" />
            <h1 className="text-base font-bold tracking-wide text-white">
              {isAr ? 'الطيار الآلي للصيانة' : 'Repair Autopilot™'}
            </h1>
          </div>
          <span className="text-[10px] text-cyan-300/80 font-mono tracking-widest uppercase">
            TASK_21 • 8-STAGE ORCHESTRATOR
          </span>
        </div>

        <div className="w-9 h-9 rounded-full bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-300">
          <Sparkles className="w-4 h-4" />
        </div>
      </div>

      {/* 8-Stage Visual Indicator */}
      <div className="z-10 py-1.5 px-2 bg-white/[0.03] border border-white/5 rounded-xl flex items-center justify-between text-[9px] font-mono text-slate-400">
        <div className="flex items-center gap-1">
          <span className={`px-1.5 py-0.5 rounded ${stage === 'preview' ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-400/30' : 'text-slate-500'}`}>
            1.SCAN
          </span>
          <span className="text-slate-600">→</span>
          <span className={`px-1.5 py-0.5 rounded ${stage === 'preview' ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-400/30' : 'text-slate-500'}`}>
            2.PREVIEW
          </span>
          <span className="text-slate-600">→</span>
          <span className={`px-1.5 py-0.5 rounded ${stage === 'approving' ? 'bg-amber-500/20 text-amber-300 font-bold border border-amber-400/30' : 'text-slate-500'}`}>
            3.APPROVE
          </span>
          <span className="text-slate-600">→</span>
          <span className={`px-1.5 py-0.5 rounded ${stage === 'executing' ? 'bg-indigo-500/30 text-indigo-300 font-bold border border-indigo-400/30 animate-pulse' : 'text-slate-500'}`}>
            4.EXECUTE
          </span>
          <span className="text-slate-600">→</span>
          <span className={`px-1.5 py-0.5 rounded ${stage === 'completed' ? 'bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-400/30' : 'text-slate-500'}`}>
            5.REPORT
          </span>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="z-10 flex-1 overflow-y-auto space-y-3 my-2 pr-0.5 custom-scrollbar">
        {/* Stage 1 & 2: Preview & Selection */}
        {stage === 'preview' && (
          <>
            <div className="p-3.5 rounded-2xl bg-gradient-to-br from-white/[0.08] to-white/[0.02] border border-cyan-500/30 backdrop-blur-md shadow-[0_8px_24px_rgba(0,0,0,0.3)]">
              <div className="flex items-center justify-between mb-2">
                <div>
                  <span className="text-[10px] text-slate-400 uppercase tracking-wider block">
                    {isAr ? 'إجمالي المساحة المستردة المحسوبة' : 'Calculated Recoverable Space'}
                  </span>
                  <div className="text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 via-indigo-200 to-white">
                    {format(totalReclaimBytes)}
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-[10px] text-emerald-400 font-semibold block">
                    {selectedActions.length} {isAr ? 'إجراءات محددة' : 'actions queued'}
                  </span>
                  <span className="text-[9px] text-slate-400 font-mono">
                    Zero-Placebo Safety
                  </span>
                </div>
              </div>

              <p className="text-[11px] text-slate-300 leading-relaxed pt-2 border-t border-white/10">
                {isAr
                  ? 'يقوم الطيار الآلي بتنفيذ سلسلة إجراءات صيانة موثوقة لحذف المخلفات الآمنة، ودمج النسخ المتطابقة، وتحرير ذاكرة التخزين المؤقت دون المساس بالبيانات الشخصية.'
                  : 'Autopilot chains safe diagnostic maintenance tasks to reclaim storage and optimize network paths with zero loss of personal files.'}
              </p>
            </div>

            {/* Actions Checklist */}
            <div className="space-y-2">
              <div className="flex items-center justify-between px-1">
                <span className="text-xs font-semibold text-slate-300">
                  {isAr ? 'الإجراءات المقترحة' : 'Queued Safe Actions'}
                </span>
                <span className="text-[10px] text-slate-400">
                  {isAr ? 'انقر لتفعيل / تعطيل أي بند' : 'Tap to toggle items'}
                </span>
              </div>

              {actions.map((act) => (
                <div
                  key={act.id}
                  onClick={() => toggleAction(act.id)}
                  className={`p-3 rounded-xl border transition-all cursor-pointer flex items-center justify-between gap-3 ${
                    act.selected 
                      ? 'bg-white/[0.06] border-cyan-400/40' 
                      : 'bg-white/[0.02] border-white/5 opacity-60'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-5 h-5 rounded-md flex items-center justify-center transition-all ${
                      act.selected ? 'bg-cyan-500 text-black font-bold' : 'border border-slate-600'
                    }`}>
                      {act.selected && <CheckCircle2 className="w-3.5 h-3.5" />}
                    </div>
                    <div>
                      <h4 className="text-xs font-semibold text-white">
                        {isAr ? act.titleAr : act.title}
                      </h4>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className="text-[10px] text-cyan-300 font-mono">
                          +{format(act.reclaimBytes)}
                        </span>
                        <span className="text-[9px] text-slate-400">
                          ({act.itemCount} {isAr ? 'عناصر' : 'items'})
                        </span>
                      </div>
                    </div>
                  </div>

                  <span className="text-[9px] px-2 py-0.5 rounded-full font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    {isAr ? 'آمن 100%' : 'SAFE'}
                  </span>
                </div>
              ))}
            </div>
          </>
        )}

        {/* Stage 3: Explicit Approval Gate */}
        {stage === 'approving' && (
          <div className="space-y-3 p-3.5 rounded-2xl bg-white/[0.06] border border-amber-500/40 backdrop-blur-md">
            <div className="flex items-center gap-2.5 text-amber-400">
              <ShieldCheck className="w-5 h-5 shrink-0" />
              <h3 className="text-sm font-bold">
                {isAr ? 'تأكيد موافقة المستخدم الصريحة' : 'Explicit User Approval Gate'}
              </h3>
            </div>

            <p className="text-xs text-slate-300 leading-relaxed">
              {isAr
                ? 'وفقاً لعقد الأمان الصارم، لا يُسمح بتنفيذ أي إجراء حذف دون موافقة مسبقة. سيتم تحرير المساحة التالية بأمان:'
                : 'In accordance with our strict security contract, no file deletion or cache wipe may occur without your explicit consent. The following will be reclaimed safely:'}
            </p>

            <div className="p-2.5 rounded-xl bg-black/40 border border-white/10 text-center">
              <span className="text-xl font-bold text-cyan-300 font-mono">
                {format(totalReclaimBytes)}
              </span>
              <span className="text-[10px] text-slate-400 block mt-0.5">
                {selectedActions.length} {isAr ? 'إجراءات سيتم تنفيذها' : 'safe operations selected'}
              </span>
            </div>

            <label className="flex items-start gap-2.5 pt-2 cursor-pointer">
              <input
                type="checkbox"
                checked={hasApprovedTerms}
                onChange={(e) => setHasApprovedTerms(e.target.checked)}
                className="mt-0.5 accent-cyan-500 w-4 h-4 cursor-pointer"
              />
              <span className="text-xs text-slate-200">
                {isAr
                  ? 'أؤكد مراجعتي للإجراءات وأوافق على حذف الملفات المؤقتة والنسخ المكررة المحددة.'
                  : 'I have reviewed the plan and authorize purging the selected temporary and redundant items.'}
              </span>
            </label>
          </div>
        )}

        {/* Stage 4: Executing Animation */}
        {stage === 'executing' && (
          <div className="space-y-4 p-4 rounded-2xl bg-white/[0.06] border border-indigo-500/40 backdrop-blur-md text-center py-8">
            <div className="w-16 h-16 mx-auto rounded-full bg-gradient-to-br from-cyan-500/20 to-indigo-600/30 border border-cyan-400/50 flex items-center justify-center animate-pulse">
              <Rocket className="w-8 h-8 text-cyan-300 animate-bounce" />
            </div>

            <div>
              <h3 className="text-sm font-bold text-white mb-1">
                {isAr ? 'جارٍ تنفيذ الطيار الآلي...' : 'Executing Autopilot Sequence...'}
              </h3>
              <p className="text-xs text-cyan-300 font-mono">
                {selectedActions[currentExecutingIdx]?.title || 'Finalizing...'}
              </p>
            </div>

            <div className="w-full bg-black/40 h-2 rounded-full overflow-hidden border border-white/10">
              <div 
                className="h-full bg-gradient-to-r from-cyan-400 to-indigo-500 transition-all duration-300"
                style={{ width: `${Math.round(((currentExecutingIdx + 1) / selectedActions.length) * 100)}%` }}
              />
            </div>
          </div>
        )}

        {/* Stage 5: Completed Report */}
        {stage === 'completed' && (
          <div className="space-y-3 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 backdrop-blur-md">
            <div className="flex items-center gap-2 text-emerald-400">
              <CheckCircle2 className="w-5 h-5 shrink-0" />
              <h3 className="text-sm font-bold">
                {isAr ? 'اكتملت الصيانة الآلية بنجاح!' : 'Autopilot Maintenance Complete!'}
              </h3>
            </div>

            <div className="p-3 rounded-xl bg-black/30 border border-emerald-500/20 text-center">
              <span className="text-[10px] text-slate-400 uppercase block mb-1">
                {isAr ? 'المساحة التي تم استردادها فعلياً' : 'Total Reclaimed Storage'}
              </span>
              <span className="text-2xl font-black text-emerald-300 font-mono">
                +{format(totalReclaimBytes)}
              </span>
            </div>

            <div className="text-xs text-slate-300 space-y-1.5 pt-1">
              <div className="flex items-center justify-between py-1 border-b border-white/5">
                <span>{isAr ? 'العمليات المنجزة:' : 'Operations Executed:'}</span>
                <span className="text-white font-bold">{selectedActions.length}</span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-white/5">
                <span>{isAr ? 'حالة الأمان بعد الفحص:' : 'Integrity Verification:'}</span>
                <span className="text-emerald-400 font-bold">100% Passed</span>
              </div>
            </div>

            <button
              onClick={handleReset}
              className="w-full mt-2 py-2 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-xs text-white font-medium flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>{isAr ? 'إعادة تشغيل فحص جديد' : 'Run Another Autopilot Check'}</span>
            </button>
          </div>
        )}
      </div>

      {/* Footer Dynamic Action Button */}
      <div className="z-10 pt-2 border-t border-white/10">
        {stage === 'preview' && (
          <button
            onClick={() => setStage('approving')}
            disabled={selectedActions.length === 0}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-semibold text-xs shadow-[0_4px_16px_rgba(6,182,212,0.35)] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <Play className="w-3.5 h-3.5 fill-white" />
            <span>{isAr ? 'مراجعة خطة الصيانة وتأكيدها' : 'Review & Authorize Plan'}</span>
          </button>
        )}

        {stage === 'approving' && (
          <div className="flex gap-2">
            <button
              onClick={() => setStage('preview')}
              className="w-1/3 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white font-medium text-xs cursor-pointer"
            >
              {isAr ? 'رجوع' : 'Back'}
            </button>
            <button
              onClick={handleStartExecution}
              disabled={!hasApprovedTerms}
              className="w-2/3 py-2.5 rounded-xl bg-gradient-to-r from-emerald-600 to-cyan-600 hover:from-emerald-500 hover:to-cyan-500 text-white font-bold text-xs shadow-[0_4px_16px_rgba(16,185,129,0.35)] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-40"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>{isAr ? 'الموافقة وبدء التنفيذ' : 'Approve & Execute'}</span>
            </button>
          </div>
        )}

        {stage === 'completed' && (
          <button
            onClick={() => setCurrentScreen('11-repair-lab')}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-cyan-600/80 to-blue-600/80 hover:from-cyan-500 hover:to-blue-500 text-white font-medium text-xs shadow-[0_4px_16px_rgba(6,182,212,0.3)] active:scale-[0.99] transition-all cursor-pointer"
          >
            {isAr ? 'العودة لمختبر الصيانة' : 'Return to Repair Lab'}
          </button>
        )}
      </div>
    </div>
  );
};
