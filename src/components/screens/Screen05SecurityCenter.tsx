import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  ChevronLeft, 
  Power, 
  Database, 
  ShieldCheck, 
  RefreshCw, 
  Crosshair, 
  Infinity as InfinityIcon,
  Check
} from 'lucide-react';

export const Screen05SecurityCenter: React.FC = () => {
  const { 
    securitySettings, 
    toggleSecuritySetting, 
    setCurrentScreen, 
    connectionStatus,
    t, 
    locale 
  } = useVPN();

  const toggleItems = [
    {
      key: 'killSwitch' as const,
      label: t.killSwitch,
      desc: t.killSwitchDesc,
      icon: Power,
      color: 'blue',
    },
    {
      key: 'dnsLeakProtection' as const,
      label: t.dnsLeakProtection,
      desc: t.dnsLeakDesc,
      icon: Database,
      color: 'purple',
    },
    {
      key: 'ipv6Protection' as const,
      label: t.ipv6Protection,
      desc: t.ipv6Desc,
      icon: ShieldCheck,
      color: 'indigo',
    },
    {
      key: 'autoReconnect' as const,
      label: t.autoReconnect,
      desc: t.autoReconnectDesc,
      icon: RefreshCw,
      color: 'cyan',
    },
    {
      key: 'trackerBlocking' as const,
      label: t.trackerBlocking,
      desc: t.trackerBlockingDesc,
      icon: Crosshair,
      color: 'blue',
    },
    {
      key: 'alwaysOnVpn' as const,
      label: t.alwaysOnVpn,
      desc: t.alwaysOnVpnDesc,
      icon: InfinityIcon,
      color: 'purple',
    },
  ];

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e1e7fc] via-[#eaeffa] to-[#dce2f9] text-slate-800">
      {/* Background Ambience */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[8%] left-[10%] w-[80%] h-[35%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-blue-300/40 via-purple-300/30 to-transparent blur-3xl" />
        <div className="absolute bottom-[10%] right-[-10%] w-[60%] h-[40%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-300/30 to-transparent blur-3xl" />
      </div>

      {/* Top Header: Left Back, Center KX Logo, Title */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen(connectionStatus === 'connected' ? '04-connected-session' : '02-home-disconnected')}
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Back"
        >
          <ChevronLeft className={`w-5 h-5 ${locale === 'ar' ? 'rotate-180' : ''}`} />
        </button>

        {/* Brand identity center */}
        <div className="flex items-center gap-1.5">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-500 p-[1px]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <span className="font-black text-lg tracking-wider text-indigo-950">
            KX
          </span>
        </div>

        {/* Empty space balancer */}
        <div className="w-10" />
      </div>

      {/* Title */}
      <div className="text-center z-10 my-1">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.securityCenter}
        </h2>
      </div>

      {/* Hero: Glowing Glass Shield Orb inside soft glass bubble */}
      <div className="z-10 my-1">
        <div className="w-full rounded-3xl p-[1px] bg-gradient-to-b from-white via-white/50 to-indigo-200/40 shadow-sm">
          <div className="w-full rounded-3xl bg-white/50 backdrop-blur-xl px-4 py-3 flex flex-col items-center justify-center text-center border border-white">
            {/* Glowing Shield Orb */}
            <div className="relative w-28 h-28 flex items-center justify-center my-1">
              <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-cyan-400/30 to-purple-500/30 blur-xl animate-pulse-glow" />
              <div className="w-20 h-20 rounded-full bg-gradient-to-tr from-blue-500 to-purple-600 flex items-center justify-center shadow-[0_4px_20px_rgba(99,102,241,0.4)] border-2 border-white">
                <ShieldCheck className="w-11 h-11 text-white" />
              </div>
            </div>

            {/* Fully Protected Badge */}
            <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 border border-emerald-400/40 text-emerald-800 font-bold text-xs shadow-xs mt-1">
              <Check className="w-3.5 h-3.5 text-emerald-600 stroke-[3]" />
              <span>{t.fullyProtected}</span>
            </div>

            <p className="text-[11px] font-medium text-slate-500 mt-1">
              {t.protectedSubtitle}
            </p>
          </div>
        </div>
      </div>

      {/* 6 Security Toggles List matching reference */}
      <div className="flex-1 overflow-y-auto space-y-2 z-10 pr-0.5 my-1.5">
        {toggleItems.map((item) => {
          const isEnabled = securitySettings[item.key];
          const IconComponent = item.icon;

          return (
            <div
              key={item.key}
              onClick={() => toggleSecuritySetting(item.key)}
              className="group relative rounded-2xl p-[1px] bg-gradient-to-r from-white via-white/60 to-white/90 shadow-xs cursor-pointer active:scale-[0.99] transition-all"
            >
              <div className="w-full rounded-2xl bg-white/60 backdrop-blur-xl px-3.5 py-2.5 flex items-center justify-between border border-white/80">
                {/* Left: Icon & Description */}
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-indigo-500/15 to-purple-500/15 border border-indigo-200/50 flex items-center justify-center text-indigo-700 shrink-0">
                    <IconComponent className="w-5 h-5" />
                  </div>

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-xs text-indigo-950">
                        {item.label}
                      </span>
                      {isEnabled && (
                        <span className="flex items-center gap-0.5 px-1.5 py-0.2 rounded-full bg-emerald-100 text-[10px] font-semibold text-emerald-700">
                          <Check className="w-2.5 h-2.5 stroke-[3]" />
                          <span>{t.statusOn}</span>
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-slate-500 leading-tight mt-0.5">
                      {item.desc}
                    </p>
                  </div>
                </div>

                {/* Right: Smooth Pill Toggle Switch */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleSecuritySetting(item.key);
                  }}
                  className={`relative w-12 h-6.5 rounded-full p-0.5 transition-colors duration-300 shrink-0 ${
                    isEnabled
                      ? 'bg-gradient-to-r from-indigo-500 to-purple-600 shadow-sm'
                      : 'bg-slate-300'
                  }`}
                  aria-label={item.label}
                >
                  <div 
                    className={`w-5.5 h-5.5 rounded-full bg-white shadow-md transform transition-transform duration-300 ${
                      isEnabled ? (locale === 'ar' ? '-translate-x-5.5' : 'translate-x-5.5') : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />
    </div>
  );
};
