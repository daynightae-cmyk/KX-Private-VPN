import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { ActionCard } from './import/ActionCard';
import { QRScannerModal } from './import/QRScannerModal';
import { FileImportModal } from './import/FileImportModal';
import { ClipboardImportModal } from './import/ClipboardImportModal';
import { ManualEntryModal } from './import/ManualEntryModal';
import { ParsedWireGuardConfig } from '../../utils/config-parser';
import { 
  ChevronLeft, 
  MoreVertical, 
  QrCode, 
  FileText, 
  ClipboardList, 
  Sliders, 
  ShieldCheck, 
  Trash2,
  CheckCircle2,
  Zap,
  Info
} from 'lucide-react';

export const Screen09ImportConfig: React.FC = () => {
  const { 
    imports, 
    addImport, 
    clearImports, 
    setCurrentScreen, 
    connectionStatus,
    setSelectedServer,
    servers,
    t, 
    locale 
  } = useVPN();

  const [activeModal, setActiveModal] = useState<'qr' | 'file' | 'clipboard' | 'manual' | null>(null);
  const [selectedImportDetail, setSelectedImportDetail] = useState<string | null>(null);
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);

  const showNotification = (msg: string) => {
    setNotificationMsg(msg);
    setTimeout(() => setNotificationMsg(null), 3500);
  };

  const handleImportSuccess = (config: ParsedWireGuardConfig) => {
    addImport({
      name: config.metadata.suggestedName,
      protocol: 'WireGuard',
      endpoint: config.peer.endpoint,
      publicKey: config.peer.publicKey,
      dns: config.metadata.dnsServers,
    });

    showNotification(
      locale === 'ar' 
        ? `تم استيراد ${config.metadata.suggestedName} بنجاح!` 
        : `Successfully validated and imported ${config.metadata.suggestedName}!`
    );
  };

  const handleConnectImported = (name: string, endpoint: string) => {
    const matched = servers.find(s => s.name.toLowerCase().includes('germany') || s.name.toLowerCase().includes('united states'));
    if (matched) {
      setSelectedServer({
        ...matched,
        name: name,
        city: endpoint,
      });
    }
    showNotification(
      locale === 'ar' 
        ? `تم تنشيط وتطبيق نفق ${name}` 
        : `Applied tunnel profile: ${name}`
    );
    setCurrentScreen('02-home-disconnected');
  };

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dce2f9] text-slate-800">
      {/* Background Ambient Cosmic Ribbons matching 09-import-configuration.png */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[5%] -left-[20%] w-[120%] h-[50%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-indigo-300/30 via-purple-200/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[10%] -right-[20%] w-[100%] h-[60%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-300/25 via-blue-200/15 to-transparent blur-2xl" />

        {/* Delicate Light Wave Arcs */}
        <svg className="absolute inset-0 w-full h-full opacity-40" viewBox="0 0 390 844" fill="none" preserveAspectRatio="none">
          <path d="M -30 200 C 100 150, 260 320, 420 240" stroke="#818cf8" strokeWidth="1.5" strokeDasharray="4 4" />
          <path d="M -20 480 C 140 510, 260 380, 420 440" stroke="#38bdf8" strokeWidth="1.2" />
        </svg>
      </div>

      {/* Toast Notification Banner */}
      {notificationMsg && (
        <div className="absolute top-2 left-4 right-4 z-50 px-3.5 py-2 rounded-2xl bg-indigo-900/90 backdrop-blur-xl border border-cyan-400 text-white text-xs font-semibold shadow-xl flex items-center justify-between animate-fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span className="truncate">{notificationMsg}</span>
          </div>
          <button onClick={() => setNotificationMsg(null)} className="text-white/60 hover:text-white">
            ✕
          </button>
        </div>
      )}

      {/* Top Header: Left Back, Center KX Logo, Right 3-dots */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen(connectionStatus === 'connected' ? '04-connected-session' : '02-home-disconnected')}
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Back"
        >
          <ChevronLeft className={`w-5 h-5 ${locale === 'ar' ? 'rotate-180' : ''}`} />
        </button>

        {/* Center Shield KX Emblem with luxury designer typography */}
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 p-[1px] shadow-sm">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Shield"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <span className="kx-brand-title text-xl tracking-[0.2em] text-indigo-950">
            KX
          </span>
        </div>

        <button 
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Options"
        >
          <MoreVertical className="w-5 h-5" />
        </button>
      </div>

      {/* Screen Title */}
      <div className="text-center z-10 my-0.5">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.importConfiguration}
        </h2>
      </div>

      {/* 4 Dedicated Action Cards in 2x2 Grid (Glass Orb Design System) */}
      <div className="grid grid-cols-2 gap-2.5 z-10 my-1">
        {/* Action 1: Scan QR */}
        <ActionCard
          id="card-scan-qr"
          title={locale === 'ar' ? 'مسح QR' : 'Scan QR'}
          subtitle={locale === 'ar' ? 'كاميرا الجهاز والتحقق الفوري' : 'Live Camera / Scanner'}
          icon={QrCode}
          gradientFrom="from-blue-500"
          gradientTo="to-indigo-600"
          onClick={() => setActiveModal('qr')}
        />

        {/* Action 2: Import File */}
        <ActionCard
          id="card-import-file"
          title={locale === 'ar' ? 'استيراد ملف' : 'Import File'}
          subtitle={locale === 'ar' ? 'ملفات WireGuard .conf' : 'WireGuard .conf profiles'}
          icon={FileText}
          gradientFrom="from-cyan-400"
          gradientTo="to-blue-500"
          onClick={() => setActiveModal('file')}
        />

        {/* Action 3: Paste from Clipboard */}
        <ActionCard
          id="card-paste-clipboard"
          title={locale === 'ar' ? 'لصق من الحافظة' : 'Paste from Clipboard'}
          subtitle={locale === 'ar' ? 'فحص تلقائي للنص المنسوخ' : 'Instant Parser & Validator'}
          icon={ClipboardList}
          gradientFrom="from-purple-500"
          gradientTo="to-pink-500"
          onClick={() => setActiveModal('clipboard')}
        />

        {/* Action 4: Manual Entry */}
        <ActionCard
          id="card-manual-entry"
          title={locale === 'ar' ? 'إدخال يدوي' : 'Manual Entry'}
          subtitle={locale === 'ar' ? 'ضبط المعلمات والتشفير' : 'Public key & endpoint setup'}
          icon={Sliders}
          gradientFrom="from-indigo-400"
          gradientTo="to-cyan-500"
          onClick={() => setActiveModal('manual')}
        />
      </div>

      {/* Center Hero: Glowing Glass Orb with Animated QR Code Viewfinder */}
      <div className="flex flex-col items-center justify-center my-auto z-10 w-full py-1">
        <div 
          onClick={() => setActiveModal('qr')}
          className="relative w-48 h-48 md:w-52 md:h-52 flex items-center justify-center cursor-pointer active:scale-95 transition-transform"
          role="button"
          tabIndex={0}
          aria-label="Scan QR Code"
        >
          {/* Luminous Glow Behind Orb */}
          <div className="absolute inset-[-12%] rounded-full bg-gradient-to-tr from-cyan-400/35 via-indigo-400/35 to-pink-400/35 blur-2xl animate-pulse-glow" />
          
          {/* Orbital Satellites */}
          <div className="absolute inset-[-10%] rounded-full border border-cyan-400/40 rotate-[-25deg] scale-y-[0.45] pointer-events-none" />
          <div className="absolute inset-[-10%] rounded-full border border-pink-400/40 rotate-[45deg] scale-y-[0.4] pointer-events-none" />
          <div className="absolute top-[18%] left-[8%] w-3 h-3 rounded-full bg-cyan-400 shadow-[0_0_10px_#38bdf8] border border-white" />
          <div className="absolute bottom-[22%] right-[10%] w-3.5 h-3.5 rounded-full bg-pink-400 shadow-[0_0_12px_#ec4899] border border-white" />

          {/* Glass Orb Shell */}
          <div className="relative w-full h-full rounded-full p-[2px] bg-gradient-to-b from-white via-white/40 to-cyan-400/50 shadow-[0_12px_36px_rgba(100,116,170,0.25)] overflow-hidden backdrop-blur-2xl">
            {/* Top Reflection */}
            <div className="absolute -top-[15%] left-[10%] right-[10%] h-[50%] rounded-full bg-gradient-to-b from-white/90 via-white/20 to-transparent blur-[1px] pointer-events-none" />

            <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-white/70 via-indigo-50/60 to-blue-100/70 p-3">
              {/* QR Target Viewfinder */}
              <div className="relative w-24 h-24 md:w-28 md:h-28 rounded-2xl bg-white/50 border border-white flex items-center justify-center p-3 shadow-inner">
                {/* 4 Corner Reticles */}
                <div className="absolute top-1 left-1 w-3.5 h-3.5 border-t-2 border-l-2 border-cyan-500" />
                <div className="absolute top-1 right-1 w-3.5 h-3.5 border-t-2 border-r-2 border-cyan-500" />
                <div className="absolute bottom-1 left-1 w-3.5 h-3.5 border-b-2 border-l-2 border-cyan-500" />
                <div className="absolute bottom-1 right-1 w-3.5 h-3.5 border-b-2 border-r-2 border-cyan-500" />

                {/* Scanning Laser Line */}
                <div className="absolute left-2 right-2 h-0.5 bg-cyan-400 shadow-[0_0_8px_#06b6d4] animate-laser" />

                {/* Stylized QR Vector */}
                <svg className="w-14 h-14 text-indigo-900/80" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M2 2h8v8H2V2zm2 2v4h4V4H4zm10-2h8v8h-8V2zm2 2v4h4V4h-4zM2 14h8v8H2v-8zm2 2v4h4v-4H4zm14-2h4v4h-4v-4zm-4 4h4v4h-4v-4zm4 0h2v4h-2v-4zm-8-2h2v2h-2v-2zm4-2h2v2h-2v-2zm-2 4h2v2h-2v-2zm-6-2h2v2H6v-2zm8 4h2v2h-2v-2z" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        {/* Subtitle Prompt */}
        <p className="text-[11px] font-medium text-slate-500 mt-1.5">
          {t.scanQrPrompt}
        </p>
      </div>

      {/* Recent Imports Section matching reference */}
      <div className="z-10 my-1">
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-bold text-xs text-indigo-950">{t.recentImports}</span>
            <button
              onClick={clearImports}
              className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 transition-colors"
            >
              {t.clear}
            </button>
          </div>

          <div className="space-y-1.5 max-h-36 overflow-y-auto pr-0.5">
            {imports.map(item => (
              <div 
                key={item.id}
                className="rounded-xl bg-white/60 p-2.5 border border-white flex items-center justify-between shadow-2xs hover:bg-white/80 transition-all"
              >
                <div 
                  className="flex items-center gap-2.5 flex-1 cursor-pointer"
                  onClick={() => setSelectedImportDetail(item.id === selectedImportDetail ? null : item.id)}
                >
                  <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center shrink-0 shadow-2xs">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block font-bold text-xs text-indigo-950">{item.name}</span>
                    <span className="block text-[10px] text-slate-500 font-mono">
                      {item.protocol} · {locale === 'ar' ? item.importedAtAr : item.importedAt}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  {/* Connect / Apply Button */}
                  <button
                    onClick={() => handleConnectImported(item.name, item.endpoint)}
                    className="px-2 py-1 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-[10px] font-bold flex items-center gap-1 transition-colors"
                    title="Connect to this tunnel"
                  >
                    <Zap className="w-3 h-3 text-cyan-600" />
                    <span>{locale === 'ar' ? 'تطبيق' : 'Use'}</span>
                  </button>

                  <button 
                    onClick={() => setSelectedImportDetail(item.id === selectedImportDetail ? null : item.id)}
                    className="w-7 h-7 rounded-full flex items-center justify-center text-slate-400 hover:text-indigo-950"
                    aria-label="Item Options"
                  >
                    <MoreVertical className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Modal 1: QR Scanner */}
      <QRScannerModal 
        isOpen={activeModal === 'qr'} 
        onClose={() => setActiveModal(null)} 
        onImportSuccess={handleImportSuccess}
        locale={locale}
      />

      {/* Modal 2: File Import */}
      <FileImportModal 
        isOpen={activeModal === 'file'} 
        onClose={() => setActiveModal(null)} 
        onImportSuccess={handleImportSuccess}
        locale={locale}
      />

      {/* Modal 3: Clipboard Import */}
      <ClipboardImportModal 
        isOpen={activeModal === 'clipboard'} 
        onClose={() => setActiveModal(null)} 
        onImportSuccess={handleImportSuccess}
        locale={locale}
      />

      {/* Modal 4: Manual Entry */}
      <ManualEntryModal 
        isOpen={activeModal === 'manual'} 
        onClose={() => setActiveModal(null)} 
        onImportSuccess={handleImportSuccess}
        locale={locale}
      />
    </div>
  );
};
