import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  Bell, 
  MoreVertical, 
  Layers, 
  ChevronRight, 
  Activity, 
  ArrowDown, 
  ArrowUp, 
  ShieldAlert, 
  Home, 
  Server, 
  Shield, 
  BarChart2, 
  Settings,
  Power
} from 'lucide-react';

export const Screen02HomeDisconnected: React.FC = () => {
  const { 
    connectionStatus, 
    toggleConnect, 
    setCurrentScreen, 
    selectedServer, 
    t, 
    locale 
  } = useVPN();

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-5 pt-3 pb-4 overflow-hidden bg-[#0a0d24] text-white">
      {/* Background Cosmic Wave Lights matching reference 02-home-disconnected.png */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-[10%] left-[-20%] w-[140%] h-[60%] bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-blue-600/20 via-purple-900/15 to-transparent blur-3xl" />
        <div className="absolute top-[40%] right-[-15%] w-[80%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-indigo-500/20 via-transparent to-transparent blur-2xl" />

        {/* Ambient Wave Lines */}
        <svg className="absolute inset-0 w-full h-full" viewBox="0 0 390 844" fill="none" preserveAspectRatio="none">
          <path d="M -40 260 C 80 180, 280 400, 430 320" stroke="url(#home-wave-1)" strokeWidth="1.6" opacity="0.7" />
          <path d="M -20 440 C 140 480, 260 320, 420 380" stroke="url(#home-wave-2)" strokeWidth="1.8" opacity="0.8" />
          <defs>
            <linearGradient id="home-wave-1" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
              <stop offset="50%" stopColor="#818cf8" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#c084fc" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="home-wave-2" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#c084fc" stopOpacity="0" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#ec4899" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>

        {/* Sparkles */}
        <div className="absolute top-[26%] left-[22%] text-white/80 text-xs">✦</div>
        <div className="absolute top-[22%] right-[26%] text-cyan-200/90 text-sm">✦</div>
      </div>

      {/* Header Bar matching reference: Left small KX orb + "KX VPN", Right Bell + 3-dots */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-2">
        {/* Brand identity */}
        <div className="flex items-center gap-2.5">
          <div className="relative w-9 h-9 rounded-full p-[1px] bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_0_15px_rgba(99,102,241,0.5)]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <span className="kx-brand-title text-xl tracking-[0.2em]">
            KX VPN
          </span>
        </div>

        {/* Action icons */}
        <div className="flex items-center gap-2">
          {/* Notification Bell with unread dot */}
          <button 
            onClick={() => setCurrentScreen('10-profile-settings')}
            className="relative w-9 h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 active:scale-95 transition-all text-indigo-200"
            aria-label="Notifications"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-pink-500 shadow-[0_0_6px_#ec4899]" />
          </button>

          {/* More 3-dots menu */}
          <button 
            onClick={() => setCurrentScreen('07-advanced-network')}
            className="w-9 h-9 rounded-full bg-white/5 border border-white/10 flex items-center justify-center hover:bg-white/10 active:scale-95 transition-all text-indigo-200"
            aria-label="More Options"
          >
            <MoreVertical className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Center Hero: Massive Glowing Circular Glass Orb with Power Button */}
      <div className="flex flex-col items-center justify-center my-auto z-10 w-full py-2">
        <div 
          onClick={toggleConnect}
          className="group relative w-60 h-60 md:w-64 md:h-64 flex items-center justify-center cursor-pointer active:scale-95 transition-transform duration-300"
          role="button"
          tabIndex={0}
          aria-label="Connect to VPN"
        >
          {/* Outer Multi-layered Aura Glow */}
          <div className="absolute inset-[-15%] rounded-full bg-gradient-to-tr from-cyan-500/25 via-blue-600/30 to-purple-600/30 blur-2xl group-hover:scale-105 transition-transform duration-500" />
          
          {/* Planetary Orbital Rings with 3 Satellites */}
          <div className="absolute inset-[-14%] rounded-full border border-cyan-400/35 rotate-[-26deg] scale-y-[0.45] pointer-events-none" />
          <div className="absolute inset-[-14%] rounded-full border border-pink-400/35 rotate-[45deg] scale-y-[0.4] pointer-events-none" />
          
          {/* Satellite Spheres */}
          <div className="absolute top-[22%] left-[6%] w-3 h-3 rounded-full bg-cyan-300 shadow-[0_0_12px_#38bdf8] border border-white/80" />
          <div className="absolute bottom-[20%] right-[8%] w-3.5 h-3.5 rounded-full bg-pink-400 shadow-[0_0_14px_#ec4899] border border-white/80" />
          <div className="absolute top-[26%] right-[14%] w-2.5 h-2.5 rounded-full bg-blue-300 shadow-[0_0_10px_#60a5fa] border border-white/80" />

          {/* Main Glass Sphere Body */}
          <div className="relative w-full h-full rounded-full p-[2px] bg-gradient-to-b from-white/80 via-white/20 to-cyan-400/40 shadow-[0_12px_45px_rgba(0,0,0,0.6)] overflow-hidden backdrop-blur-2xl">
            {/* Top Specular Sheen */}
            <div className="absolute -top-[15%] left-[10%] right-[10%] h-[50%] rounded-full bg-gradient-to-b from-white/70 via-white/10 to-transparent blur-[1px] pointer-events-none" />

            {/* Deep Glass Interior */}
            <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-[#1b1c4b]/80 via-[#101438]/90 to-[#0b0c26]/95 p-3">
              {/* Inner Glowing Center Ring */}
              <div className="w-20 h-20 md:w-24 md:h-24 rounded-full bg-gradient-to-tr from-cyan-500/20 via-indigo-600/20 to-purple-600/30 border border-white/30 flex items-center justify-center shadow-[0_0_30px_rgba(6,182,212,0.4)] backdrop-blur-md group-hover:border-cyan-300 transition-colors">
                <Power className="w-10 h-10 md:w-12 md:h-12 text-white drop-shadow-[0_0_14px_#38bdf8] transition-transform duration-300 group-hover:scale-105" />
              </div>

              {/* Status Badge inside Orb */}
              <div className="mt-4 flex items-center gap-2 px-3.5 py-1 rounded-full bg-black/45 backdrop-blur-md border border-white/15 text-xs font-semibold tracking-wide text-white">
                <span className="w-2 h-2 rounded-full border border-pink-400 bg-transparent animate-ping" />
                <span className="w-2 h-2 rounded-full bg-pink-500 shadow-[0_0_8px_#ec4899] -ml-4" />
                <span>
                  {connectionStatus === 'connecting' ? t.connecting : t.disconnected}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Middle: SMART CHOICE / Best Location Card */}
      <div className="w-full z-10 my-2">
        <button
          onClick={() => setCurrentScreen('03-server-selection')}
          className="w-full rounded-2xl p-[1px] bg-gradient-to-r from-cyan-400/40 via-purple-500/30 to-indigo-500/40 shadow-[0_8px_25px_rgba(0,0,0,0.3)] hover:border-cyan-400/60 active:scale-[0.99] transition-all text-left"
        >
          <div className="w-full rounded-2xl bg-[#141843]/70 backdrop-blur-xl px-4 py-3 flex items-center justify-between border border-white/15">
            <div className="flex items-center gap-3">
              {/* 3D Stacked Layers Icon */}
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center shadow-[0_0_15px_rgba(6,182,212,0.5)] border border-white/30">
                <Layers className="w-5 h-5 text-white" />
              </div>
              <div>
                <span className="block text-[11px] font-bold tracking-wider text-cyan-300 uppercase">
                  {t.smartChoice}
                </span>
                <span className="block text-base font-bold text-white tracking-tight">
                  {selectedServer ? (locale === 'ar' ? selectedServer.nameAr : selectedServer.name) : t.bestLocation}
                </span>
              </div>
            </div>

            <ChevronRight className={`w-5 h-5 text-indigo-300 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </div>
        </button>
      </div>

      {/* 2x2 Metric Cards Grid matching reference 02-home-disconnected.png 100% */}
      <div className="grid grid-cols-2 gap-3 z-10 my-1">
        {/* Ping Card */}
        <div className="relative rounded-2xl p-[1px] bg-gradient-to-b from-white/25 via-white/10 to-indigo-500/20 backdrop-blur-2xl shadow-[0_8px_24px_rgba(0,0,0,0.4)] overflow-hidden">
          <div className="w-full h-full rounded-2xl bg-[#141843]/70 px-3.5 py-3 flex items-center gap-3 relative overflow-hidden">
            {/* Luminous bottom wave arc */}
            <div className="absolute -bottom-6 -right-6 w-20 h-20 rounded-full bg-indigo-500/15 blur-xl pointer-events-none" />
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-indigo-600/40 to-cyan-500/30 border border-white/20 flex items-center justify-center text-cyan-300 shrink-0 shadow-[0_0_12px_rgba(99,102,241,0.3)]">
              <Activity className="w-4 h-4" />
            </div>
            <div>
              <span className="block text-xs font-medium text-indigo-200/80">
                {t.ping}
              </span>
              <span className="block text-sm font-bold text-white tracking-wide font-mono">
                -- ms
              </span>
            </div>
          </div>
        </div>

        {/* Download Card */}
        <div className="relative rounded-2xl p-[1px] bg-gradient-to-b from-white/25 via-white/10 to-cyan-500/20 backdrop-blur-2xl shadow-[0_8px_24px_rgba(0,0,0,0.4)] overflow-hidden">
          <div className="w-full h-full rounded-2xl bg-[#141843]/70 px-3.5 py-3 flex items-center gap-3 relative overflow-hidden">
            {/* Luminous bottom wave arc */}
            <div className="absolute -bottom-6 -right-6 w-20 h-20 rounded-full bg-cyan-500/15 blur-xl pointer-events-none" />
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-cyan-600/40 to-blue-500/30 border border-white/20 flex items-center justify-center text-cyan-300 shrink-0 shadow-[0_0_12px_rgba(6,182,212,0.3)]">
              <ArrowDown className="w-4 h-4" />
            </div>
            <div>
              <span className="block text-xs font-medium text-indigo-200/80">
                {t.download}
              </span>
              <span className="block text-sm font-bold text-white tracking-wide font-mono">
                -- Mbps
              </span>
            </div>
          </div>
        </div>

        {/* Upload Card */}
        <div className="relative rounded-2xl p-[1px] bg-gradient-to-b from-white/25 via-white/10 to-purple-500/20 backdrop-blur-2xl shadow-[0_8px_24px_rgba(0,0,0,0.4)] overflow-hidden">
          <div className="w-full h-full rounded-2xl bg-[#141843]/70 px-3.5 py-3 flex items-center gap-3 relative overflow-hidden">
            {/* Luminous bottom wave arc */}
            <div className="absolute -bottom-6 -right-6 w-20 h-20 rounded-full bg-purple-500/15 blur-xl pointer-events-none" />
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-600/40 to-pink-500/30 border border-white/20 flex items-center justify-center text-purple-300 shrink-0 shadow-[0_0_12px_rgba(168,85,247,0.3)]">
              <ArrowUp className="w-4 h-4" />
            </div>
            <div>
              <span className="block text-xs font-medium text-indigo-200/80">
                {t.upload}
              </span>
              <span className="block text-sm font-bold text-white tracking-wide font-mono">
                -- Mbps
              </span>
            </div>
          </div>
        </div>

        {/* IP Masking Card */}
        <div className="relative rounded-2xl p-[1px] bg-gradient-to-b from-white/25 via-white/10 to-blue-500/20 backdrop-blur-2xl shadow-[0_8px_24px_rgba(0,0,0,0.4)] overflow-hidden">
          <div className="w-full h-full rounded-2xl bg-[#141843]/70 px-3.5 py-3 flex items-center gap-3 relative overflow-hidden">
            {/* Luminous bottom wave arc */}
            <div className="absolute -bottom-6 -right-6 w-20 h-20 rounded-full bg-blue-500/15 blur-xl pointer-events-none" />
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-blue-600/40 to-indigo-500/30 border border-white/20 flex items-center justify-center text-blue-300 shrink-0 shadow-[0_0_12px_rgba(59,130,246,0.3)]">
              <ShieldAlert className="w-4 h-4" />
            </div>
            <div>
              <span className="block text-xs font-medium text-indigo-200/80">
                {t.ipMasking}
              </span>
              <span className="block text-sm font-bold text-white tracking-wide font-mono">
                --
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />
    </div>
  );
};
