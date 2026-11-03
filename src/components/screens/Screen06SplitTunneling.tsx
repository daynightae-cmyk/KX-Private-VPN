import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { 
  ChevronLeft, 
  Search, 
  EyeOff, 
  ShieldCheck, 
  LayoutGrid, 
  Eye
} from 'lucide-react';

export const Screen06SplitTunneling: React.FC = () => {
  const { 
    splitApps, 
    splitMode, 
    setSplitMode, 
    toggleSplitApp, 
    setCurrentScreen, 
    connectionStatus,
    t, 
    locale 
  } = useVPN();

  const [searchQuery, setSearchQuery] = useState('');

  const filteredApps = splitApps.filter(app => 
    app.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const throughVpnCount = splitApps.filter(a => a.throughVpn).length;
  const bypassingCount = splitApps.length - throughVpnCount;

  // Custom SVG App Logos for maximum fidelity
  const renderAppIcon = (type: string) => {
    switch (type) {
      case 'chrome':
        return (
          <div className="w-10 h-10 rounded-2xl bg-white shadow-xs border border-slate-100 flex items-center justify-center p-1.5 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full">
              <circle cx="12" cy="12" r="10" fill="#EA4335" />
              <circle cx="12" cy="12" r="5" fill="#4285F4" />
              <path d="M12 2A10 10 0 0 1 20.66 7L12 7Z" fill="#FBBC05" />
              <path d="M12 22A10 10 0 0 1 3.34 17L7.67 9.5L12 17Z" fill="#34A853" />
              <circle cx="12" cy="12" r="4.5" fill="#FFFFFF" />
              <circle cx="12" cy="12" r="3.5" fill="#4285F4" />
            </svg>
          </div>
        );
      case 'youtube':
        return (
          <div className="w-10 h-10 rounded-2xl bg-[#FF0000] shadow-xs flex items-center justify-center p-2 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full fill-white">
              <path d="M21.58 7.19a2.5 2.5 0 0 0-1.76-1.77C18.25 5 12 5 12 5s-6.25 0-7.82.42A2.5 2.5 0 0 0 2.42 7.19C2 8.76 2 12 2 12s0 3.24.42 4.81a2.5 2.5 0 0 0 1.76 1.77C5.75 19 12 19 12 19s6.25 0 7.82-.42a2.5 2.5 0 0 0 1.76-1.77C22 15.24 22 12 22 12s0-3.24-.42-4.81z" />
              <polygon points="10 15 15 12 10 9" fill="#FF0000" />
            </svg>
          </div>
        );
      case 'spotify':
        return (
          <div className="w-10 h-10 rounded-2xl bg-[#1DB954] shadow-xs flex items-center justify-center p-2 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full fill-white">
              <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm4.58 14.43a.62.62 0 0 1-.86.2 12.35 12.35 0 0 0-5.72-1.5 12.63 12.63 0 0 0-4.05.65.62.62 0 1 1-.38-1.18 13.88 13.88 0 0 1 4.49-.72 13.56 13.56 0 0 1 6.32 1.68.62.62 0 0 1 .2.87zm1.22-2.73a.78.78 0 0 1-1.07.26 14.2 14.2 0 0 0-6.62-1.74 14.54 14.54 0 0 0-4.7.77.78.78 0 1 1-.49-1.48 16.08 16.08 0 0 1 5.26-.86 15.74 15.74 0 0 1 7.36 1.95.78.78 0 0 1 .26 1.1zm.13-2.85a.93.93 0 0 1-1.28.31 16.52 16.52 0 0 0-7.85-2.07 16.9 16.9 0 0 0-5.49.9.93.93 0 0 1-.58-1.77 18.77 18.77 0 0 1 6.14-1 18.36 18.36 0 0 1 8.75 2.32.93.93 0 0 1 .31 1.31z" />
            </svg>
          </div>
        );
      case 'instagram':
        return (
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-[#f09433] via-[#dc2743] to-[#bc1888] shadow-xs flex items-center justify-center p-2 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full fill-white" stroke="white" strokeWidth="0.5">
              <rect x="2" y="2" width="20" height="20" rx="5" ry="5" fill="none" stroke="white" strokeWidth="2" />
              <circle cx="12" cy="12" r="4" fill="none" stroke="white" strokeWidth="2" />
              <circle cx="18" cy="6" r="1.2" fill="white" />
            </svg>
          </div>
        );
      case 'whatsapp':
        return (
          <div className="w-10 h-10 rounded-2xl bg-[#25D366] shadow-xs flex items-center justify-center p-2 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full fill-white">
              <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19.01L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 14.99 3.81 13.47 3.81 11.91C3.81 7.37 7.5 3.67 12.05 3.67Z" />
            </svg>
          </div>
        );
      case 'discord':
        return (
          <div className="w-10 h-10 rounded-2xl bg-[#5865F2] shadow-xs flex items-center justify-center p-2 shrink-0">
            <svg viewBox="0 0 24 24" className="w-full h-full fill-white">
              <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515a.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0a12.64 12.64 0 0 0-.617-1.25a.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057a19.9 19.9 0 0 0 5.993 3.03a.078.078 0 0 0 .084-.028c.462-.63.874-1.295 1.226-1.994.021-.041.001-.09-.041-.106a13.107 13.107 0 0 1-1.872-.892a.077.077 0 0 1-.008-.128c.126-.093.252-.19.372-.287a.075.075 0 0 1 .077-.01c3.931 1.797 8.18 1.797 12.061 0a.075.075 0 0 1 .079.009c.12.098.245.195.372.288a.077.077 0 0 1-.006.127c-.598.35-1.22.645-1.873.891a.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028a19.839 19.839 0 0 0 6.002-3.03a.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.028zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419c0-1.333.956-2.419 2.157-2.419c1.21 0 2.176 1.096 2.157 2.42c0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419c0-1.333.955-2.419 2.157-2.419c1.21 0 2.176 1.096 2.157 2.42c0 1.333-.946 2.418-2.157 2.418z" />
            </svg>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dce2f9] text-slate-800">
      {/* Top Header: Left Back, Center KX Logo, Title */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen(connectionStatus === 'connected' ? '04-connected-session' : '02-home-disconnected')}
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Back"
        >
          <ChevronLeft className={`w-5 h-5 ${locale === 'ar' ? 'rotate-180' : ''}`} />
        </button>

        {/* Center Shield KX Emblem */}
        <div className="flex items-center gap-1.5">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 p-[1px]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Shield"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <span className="font-black text-lg tracking-wider text-indigo-950">
            KX
          </span>
        </div>

        <div className="w-10" />
      </div>

      {/* Screen Title */}
      <div className="text-center z-10 my-1">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.splitTunneling}
        </h2>
      </div>

      {/* Hero Explanation Card with Split-Orb Graphic */}
      <div className="z-10 my-1">
        <div className="w-full rounded-3xl p-[1px] bg-gradient-to-r from-white via-cyan-200/40 to-purple-200/40 shadow-xs">
          <div className="w-full rounded-3xl bg-white/60 backdrop-blur-xl p-3.5 flex items-center gap-3.5 border border-white/80">
            {/* Split Orb Visual Graphic matching reference */}
            <div className="relative w-16 h-16 rounded-2xl bg-gradient-to-tr from-cyan-400/20 to-purple-500/20 flex items-center justify-center p-2 shrink-0 border border-white shadow-inner">
              <div className="relative w-full h-full flex flex-col justify-between items-center">
                {/* Upper path: Shield check (blue) */}
                <div className="w-6 h-6 rounded-full bg-blue-500 text-white flex items-center justify-center shadow-xs">
                  <ShieldCheck className="w-3.5 h-3.5" />
                </div>
                {/* Connecting branch curve */}
                <div className="w-7 h-[2px] bg-gradient-to-r from-blue-400 to-purple-400 my-0.5" />
                {/* Lower path: Eye off (purple) */}
                <div className="w-6 h-6 rounded-full bg-purple-500 text-white flex items-center justify-center shadow-xs">
                  <EyeOff className="w-3.5 h-3.5" />
                </div>
              </div>
            </div>

            {/* Explanation Prose */}
            <div>
              <h3 className="font-extrabold text-xs text-indigo-950 leading-snug">
                {locale === 'ar' 
                  ? 'اختر التطبيقات التي تستخدم VPN والتي تتجاوزه.' 
                  : 'Choose which apps use the VPN and which bypass it.'}
              </h3>
              <p className="text-[10px] text-slate-500 mt-1 leading-relaxed">
                {t.splitTunnelingDesc}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Segmented Control: Bypass VPN vs Only through VPN */}
      <div className="z-10 my-1.5">
        <div className="w-full rounded-full bg-white/70 backdrop-blur-md p-1 flex items-center gap-1 border border-white shadow-xs">
          {/* Bypass VPN */}
          <button
            onClick={() => setSplitMode('bypass')}
            className={`flex-1 py-2 px-3 rounded-full text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              splitMode === 'bypass'
                ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-indigo-950'
            }`}
          >
            <EyeOff className="w-3.5 h-3.5" />
            <span>{t.bypassVpn}</span>
          </button>

          {/* Only through VPN */}
          <button
            onClick={() => setSplitMode('only_vpn')}
            className={`flex-1 py-2 px-3 rounded-full text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              splitMode === 'only_vpn'
                ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-indigo-950'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>{t.onlyThroughVpn}</span>
          </button>
        </div>
      </div>

      {/* Search Input Bar */}
      <div className="z-10 my-1">
        <div className="w-full h-10 rounded-full bg-white/60 backdrop-blur-md px-3.5 flex items-center gap-2.5 border border-white/80 shadow-xs">
          <Search className="w-4 h-4 text-slate-400 shrink-0" />
          <input 
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={t.searchApps}
            className="w-full bg-transparent border-none outline-none text-xs font-medium text-slate-800 placeholder-slate-400"
          />
        </div>
      </div>

      {/* App List (6 apps matching reference) */}
      <div className="flex-1 overflow-y-auto space-y-2 z-10 pr-0.5 my-1">
        {filteredApps.map((app) => {
          const isThrough = app.throughVpn;

          return (
            <div
              key={app.id}
              onClick={() => toggleSplitApp(app.id)}
              className="w-full rounded-2xl bg-white/60 backdrop-blur-xl px-3.5 py-2 flex items-center justify-between border border-white/80 shadow-xs cursor-pointer active:scale-[0.99] transition-all"
            >
              {/* App Info */}
              <div className="flex items-center gap-3">
                {renderAppIcon(app.iconType)}

                <div>
                  <span className="block font-bold text-xs text-indigo-950">
                    {app.name}
                  </span>
                  <div className="flex items-center gap-1 mt-0.5">
                    {isThrough ? (
                      <>
                        <ShieldCheck className="w-3 h-3 text-indigo-600" />
                        <span className="text-[10px] font-semibold text-indigo-700">
                          {t.onlyThroughVpn}
                        </span>
                      </>
                    ) : (
                      <>
                        <Eye className="w-3 h-3 text-slate-400" />
                        <span className="text-[10px] font-medium text-slate-500">
                          {t.bypassVpn}
                        </span>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {/* Toggle Switch */}
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleSplitApp(app.id);
                }}
                className={`relative w-11 h-6 rounded-full p-0.5 transition-colors duration-300 shrink-0 ${
                  isThrough
                    ? 'bg-gradient-to-r from-indigo-500 to-purple-600 shadow-sm'
                    : 'bg-slate-300'
                }`}
                aria-label={`Toggle ${app.name}`}
              >
                <div 
                  className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${
                    isThrough ? (locale === 'ar' ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          );
        })}
      </div>

      {/* Bottom Summary Bar: "6 apps | 3 through VPN | 3 bypassing" */}
      <div className="w-full z-10 pt-1">
        <div className="w-full rounded-full bg-white/70 backdrop-blur-xl px-4 py-2 flex items-center justify-between border border-white text-[11px] font-semibold text-indigo-950 shadow-xs">
          <div className="flex items-center gap-1.5">
            <LayoutGrid className="w-3.5 h-3.5 text-indigo-600" />
            <span>{t.appsSummary.replace('{total}', String(splitApps.length))}</span>
          </div>

          <span className="text-slate-300">|</span>

          <div className="flex items-center gap-1.5 text-indigo-700">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>{t.throughVpnSummary.replace('{count}', String(throughVpnCount))}</span>
          </div>

          <span className="text-slate-300">|</span>

          <div className="flex items-center gap-1.5 text-slate-500">
            <EyeOff className="w-3.5 h-3.5" />
            <span>{t.bypassingSummary.replace('{count}', String(bypassingCount))}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
