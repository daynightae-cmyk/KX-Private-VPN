import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  Menu, 
  Bell, 
  Check, 
  Clock, 
  ArrowRight, 
  ArrowDown, 
  ArrowUp, 
  Activity, 
  MapPin, 
  Power, 
  Server, 
  Home, 
  Settings,
  ShieldCheck
} from 'lucide-react';

export const Screen04ConnectedSession: React.FC = () => {
  const { 
    sessionStats, 
    disconnectNow, 
    setCurrentScreen, 
    selectedServer, 
    t, 
    locale 
  } = useVPN();

  // Format session duration into HH:MM:SS
  const formatTime = (secs: number) => {
    const hours = Math.floor(secs / 3600);
    const minutes = Math.floor((secs % 3600) / 60);
    const seconds = secs % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-5 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dbe2fb] text-slate-800">
      {/* Background World Map Matrix & Neon Route Arcs matching reference 04-connected-session.png */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* Soft pastel light glows */}
        <div className="absolute top-[5%] left-[10%] w-[80%] h-[40%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-blue-300/40 via-purple-300/30 to-transparent blur-3xl" />
        <div className="absolute bottom-[20%] right-[-10%] w-[70%] h-[50%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-300/40 via-indigo-200/30 to-transparent blur-3xl" />

        {/* World Map Dotted Matrix Silhouette Representation */}
        <svg className="absolute top-8 left-0 w-full h-44 opacity-25" viewBox="0 0 400 200" fill="none">
          <pattern id="dot-matrix" x="0" y="0" width="8" height="8" patternUnits="userSpaceOnUse">
            <circle cx="2" cy="2" r="1" fill="#6366f1" />
          </pattern>
          <rect width="400" height="200" fill="url(#dot-matrix)" />
          
          {/* Neon Route Arcs connecting pins */}
          <path d="M 90 90 Q 200 20 310 70" stroke="url(#arc-route)" strokeWidth="2" strokeDasharray="4 3" />
          {/* Pin 1: UAE / Origin */}
          <circle cx="90" cy="90" r="4" fill="#38bdf8" />
          <circle cx="90" cy="90" r="8" stroke="#38bdf8" strokeWidth="1" opacity="0.6" />
          {/* Pin 2: Germany / Destination */}
          <circle cx="310" cy="70" r="4" fill="#a855f7" />
          <circle cx="310" cy="70" r="8" stroke="#a855f7" strokeWidth="1" opacity="0.6" />

          <defs>
            <linearGradient id="arc-route" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" />
              <stop offset="100%" stopColor="#c084fc" />
            </linearGradient>
          </defs>
        </svg>

        {/* Ambient Sparkles */}
        <div className="absolute top-[28%] left-[15%] text-indigo-400 text-xs">✦</div>
        <div className="absolute top-[25%] right-[20%] text-cyan-500 text-sm">✦</div>
      </div>

      {/* Top Header: Hamburger, Center KX Shield, Bell */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen('06-split-tunneling')}
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-900"
          aria-label="Menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Center Shield KX Emblem */}
        <div className="flex items-center gap-1.5">
          <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 p-[1px] shadow-[0_2px_12px_rgba(99,102,241,0.3)]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Shield"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <span className="font-extrabold text-lg tracking-wider text-indigo-950">
            KX
          </span>
        </div>

        {/* Notification Bell */}
        <button 
          onClick={() => setCurrentScreen('05-security-center')}
          className="relative w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-900"
          aria-label="Notifications"
        >
          <Bell className="w-5 h-5" />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-pink-500 shadow-[0_0_6px_#ec4899]" />
        </button>
      </div>

      {/* Center Hero: Glowing Circular Glass Orb with Shield Checkmark */}
      <div className="flex flex-col items-center justify-center my-auto z-10 w-full py-1">
        <div className="relative w-52 h-52 md:w-56 md:h-56 flex items-center justify-center">
          {/* Luminous Multi-ring Glow */}
          <div className="absolute inset-[-14%] rounded-full bg-gradient-to-tr from-cyan-400/35 via-indigo-400/35 to-pink-400/35 blur-2xl animate-pulse-glow" />
          
          {/* Glowing Planetary Rings */}
          <div className="absolute inset-[-10%] rounded-full border border-cyan-400/50 rotate-[-25deg] scale-y-[0.45] pointer-events-none" />
          <div className="absolute inset-[-10%] rounded-full border border-pink-400/50 rotate-[45deg] scale-y-[0.4] pointer-events-none" />
          
          {/* Orbit Satellites */}
          <div className="absolute top-[20%] left-[8%] w-3 h-3 rounded-full bg-cyan-400 shadow-[0_0_10px_#38bdf8] border border-white" />
          <div className="absolute bottom-[20%] right-[10%] w-3.5 h-3.5 rounded-full bg-pink-400 shadow-[0_0_12px_#ec4899] border border-white" />

          {/* Glass Orb Shell */}
          <div className="relative w-full h-full rounded-full p-[2px] bg-gradient-to-b from-white via-white/40 to-cyan-400/50 shadow-[0_12px_36px_rgba(100,116,170,0.25)] overflow-hidden backdrop-blur-2xl">
            {/* Top Reflection */}
            <div className="absolute -top-[15%] left-[10%] right-[10%] h-[50%] rounded-full bg-gradient-to-b from-white/90 via-white/20 to-transparent blur-[1px] pointer-events-none" />

            <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-white/70 via-indigo-50/60 to-blue-100/70 p-3">
              {/* Shield Checkmark Icon */}
              <div className="w-16 h-16 md:w-20 md:h-20 rounded-full bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center shadow-[0_6px_25px_rgba(59,130,246,0.45)] border-2 border-white">
                <Check className="w-9 h-9 md:w-11 md:h-11 text-white stroke-[3]" />
              </div>

              {/* "Connected" Text */}
              <span className="mt-3 text-lg font-black tracking-tight text-indigo-950">
                {t.connected}
              </span>

              {/* Running Session Duration Badge */}
              <div className="mt-1 flex items-center gap-1.5 px-3 py-0.5 rounded-full bg-indigo-500/15 border border-indigo-300/40 text-xs font-semibold text-indigo-900 font-mono shadow-xs">
                <Clock className="w-3.5 h-3.5 text-indigo-600" />
                <span>{formatTime(sessionStats.sessionDurationSeconds)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Route Card: UAE ➔ Germany matching reference */}
      <div className="w-full z-10 my-1">
        <div className="w-full rounded-2xl p-[1px] bg-gradient-to-r from-white via-cyan-300/40 to-purple-300/40 shadow-sm">
          <div className="w-full rounded-2xl bg-white/60 backdrop-blur-xl px-5 py-2.5 flex items-center justify-center gap-4 border border-white/80">
            {/* Origin */}
            <div className="flex items-center gap-2">
              <span className="text-xl">{sessionStats.originFlag}</span>
              <span className="font-extrabold text-sm text-indigo-950 uppercase tracking-wide">
                {sessionStats.originCountry}
              </span>
            </div>

            {/* Arrow */}
            <ArrowRight className={`w-4 h-4 text-indigo-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />

            {/* Destination */}
            <div className="flex items-center gap-2">
              <span className="text-xl">
                {selectedServer ? selectedServer.flagEmoji : '🇩🇪'}
              </span>
              <span className="font-extrabold text-sm text-indigo-950 tracking-wide">
                {selectedServer ? (locale === 'ar' ? selectedServer.nameAr : selectedServer.name) : 'Germany'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 3 Speed Cards with Mini Wave Graphs */}
      <div className="grid grid-cols-3 gap-2.5 z-10 my-1">
        {/* Download Speed */}
        <div className="rounded-2xl p-[1px] bg-white/70 shadow-sm border border-white">
          <div className="w-full rounded-2xl bg-white/50 backdrop-blur-md px-2.5 py-2 flex flex-col justify-between h-20">
            <div className="flex items-center gap-1.5">
              <div className="w-6 h-6 rounded-full bg-cyan-500/20 text-cyan-600 flex items-center justify-center shrink-0">
                <ArrowDown className="w-3.5 h-3.5" />
              </div>
              <span className="text-[10px] font-semibold text-slate-500 truncate">
                {t.downloadSpeed}
              </span>
            </div>
            <div>
              <span className="text-sm font-black text-indigo-950 font-mono">
                {sessionStats.currentDownloadSpeedMbps}
              </span>
              <span className="text-[10px] font-medium text-slate-500 ml-1">Mbps</span>
            </div>
            {/* Cyan Wave Curve */}
            <svg className="w-full h-3 overflow-visible" viewBox="0 0 100 20" fill="none">
              <path d="M 0 10 Q 25 0 50 10 T 100 10" stroke="#06b6d4" strokeWidth="2" fill="none" />
            </svg>
          </div>
        </div>

        {/* Upload Speed */}
        <div className="rounded-2xl p-[1px] bg-white/70 shadow-sm border border-white">
          <div className="w-full rounded-2xl bg-white/50 backdrop-blur-md px-2.5 py-2 flex flex-col justify-between h-20">
            <div className="flex items-center gap-1.5">
              <div className="w-6 h-6 rounded-full bg-purple-500/20 text-purple-600 flex items-center justify-center shrink-0">
                <ArrowUp className="w-3.5 h-3.5" />
              </div>
              <span className="text-[10px] font-semibold text-slate-500 truncate">
                {t.uploadSpeed}
              </span>
            </div>
            <div>
              <span className="text-sm font-black text-indigo-950 font-mono">
                {sessionStats.currentUploadSpeedMbps}
              </span>
              <span className="text-[10px] font-medium text-slate-500 ml-1">Mbps</span>
            </div>
            {/* Purple Wave Curve */}
            <svg className="w-full h-3 overflow-visible" viewBox="0 0 100 20" fill="none">
              <path d="M 0 12 Q 25 18 50 10 T 100 12" stroke="#a855f7" strokeWidth="2" fill="none" />
            </svg>
          </div>
        </div>

        {/* Ping */}
        <div className="rounded-2xl p-[1px] bg-white/70 shadow-sm border border-white">
          <div className="w-full rounded-2xl bg-white/50 backdrop-blur-md px-2.5 py-2 flex flex-col justify-between h-20">
            <div className="flex items-center gap-1.5">
              <div className="w-6 h-6 rounded-full bg-blue-500/20 text-blue-600 flex items-center justify-center shrink-0">
                <Activity className="w-3.5 h-3.5" />
              </div>
              <span className="text-[10px] font-semibold text-slate-500 truncate">
                {t.ping}
              </span>
            </div>
            <div>
              <span className="text-sm font-black text-indigo-950 font-mono">
                {sessionStats.currentPingMs}
              </span>
              <span className="text-[10px] font-medium text-slate-500 ml-1">ms</span>
            </div>
            {/* Blue Wave Curve */}
            <svg className="w-full h-3 overflow-visible" viewBox="0 0 100 20" fill="none">
              <path d="M 0 8 Q 25 16 50 6 T 100 8" stroke="#3b82f6" strokeWidth="2" fill="none" />
            </svg>
          </div>
        </div>
      </div>

      {/* 2 Info Cards: Session Duration & Assigned IP */}
      <div className="grid grid-cols-2 gap-3 z-10 my-1">
        {/* Session Duration */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-md border border-white/80 px-3 py-2 flex items-center gap-2.5 shadow-xs">
          <div className="w-8 h-8 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 shrink-0">
            <Clock className="w-4 h-4" />
          </div>
          <div>
            <span className="block text-[10px] font-semibold text-slate-500">
              {t.sessionDuration}
            </span>
            <span className="block text-xs font-bold text-indigo-950 font-mono">
              {formatTime(sessionStats.sessionDurationSeconds)}
            </span>
          </div>
        </div>

        {/* Assigned IP */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-md border border-white/80 px-3 py-2 flex items-center gap-2.5 shadow-xs">
          <div className="w-8 h-8 rounded-full bg-cyan-50 flex items-center justify-center text-cyan-600 shrink-0">
            <MapPin className="w-4 h-4" />
          </div>
          <div>
            <span className="block text-[10px] font-semibold text-slate-500">
              {t.assignedIp}
            </span>
            <span className="block text-xs font-bold text-indigo-950 font-mono">
              {sessionStats.assignedIp}
            </span>
          </div>
        </div>
      </div>

      {/* Action Buttons: Disconnect & Change Server */}
      <div className="space-y-2 z-10 my-1">
        {/* Disconnect Button (Luminous gradient pill) */}
        <button
          onClick={disconnectNow}
          className="w-full h-12 rounded-full bg-gradient-to-r from-pink-500 via-purple-600 to-indigo-600 hover:from-pink-600 hover:to-indigo-700 text-white font-bold text-sm shadow-[0_4px_20px_rgba(236,72,153,0.35)] active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <Power className="w-4 h-4 text-white" />
          <span>{t.disconnect}</span>
        </button>

        {/* Change Server Button (Frosted glass pill) */}
        <button
          onClick={() => setCurrentScreen('03-server-selection')}
          className="w-full h-11 rounded-full bg-white/70 hover:bg-white text-indigo-950 font-semibold text-xs border border-white shadow-xs active:scale-[0.98] transition-all flex items-center justify-center gap-2"
        >
          <Server className="w-4 h-4 text-indigo-600" />
          <span>{t.changeServer}</span>
          <ArrowRight className={`w-3.5 h-3.5 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
        </button>
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />
    </div>
  );
};
