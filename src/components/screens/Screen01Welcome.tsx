import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { ArrowRight, CloudUpload } from 'lucide-react';

export const Screen01Welcome: React.FC = () => {
  const { setCurrentScreen, t, locale } = useVPN();

  return (
    <div className="relative w-full h-full min-h-[700px] flex flex-col items-center justify-between px-6 py-8 overflow-hidden bg-[#0c102a] text-white">
      {/* Background Cosmic Wave Ribbons */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* Deep ambient gradients */}
        <div className="absolute -top-[10%] left-[-20%] w-[140%] h-[120%] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/40 via-purple-950/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-20%] w-[120%] h-[80%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-600/25 via-indigo-950/30 to-transparent blur-3xl" />

        {/* Dynamic Light Ribbon Arcs matching reference 01-welcome.png */}
        <svg className="absolute inset-0 w-full h-full" viewBox="0 0 390 844" fill="none" preserveAspectRatio="none">
          <path 
            d="M -30 220 C 120 160, 280 340, 420 280" 
            stroke="url(#welcome-wave-1)" 
            strokeWidth="1.8" 
            opacity="0.85"
          />
          <path 
            d="M -50 480 C 140 520, 260 380, 430 460" 
            stroke="url(#welcome-wave-2)" 
            strokeWidth="2.2" 
            opacity="0.9"
          />
          <path 
            d="M -20 720 C 110 650, 290 760, 420 700" 
            stroke="url(#welcome-wave-3)" 
            strokeWidth="1.5" 
            opacity="0.6"
          />
          <defs>
            <linearGradient id="welcome-wave-1" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.2" />
              <stop offset="40%" stopColor="#818cf8" stopOpacity="1" />
              <stop offset="80%" stopColor="#c084fc" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="welcome-wave-2" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#ec4899" stopOpacity="0.1" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#818cf8" stopOpacity="0.2" />
            </linearGradient>
            <linearGradient id="welcome-wave-3" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#818cf8" stopOpacity="0" />
              <stop offset="50%" stopColor="#c084fc" stopOpacity="0.7" />
              <stop offset="100%" stopColor="#38bdf8" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>

        {/* Ambient Sparkles */}
        <div className="absolute top-[28%] left-[18%] text-white/80 text-sm animate-pulse">✦</div>
        <div className="absolute top-[35%] right-[16%] text-cyan-300/80 text-xs animate-pulse" style={{ animationDelay: '1.2s' }}>✦</div>
        <div className="absolute bottom-[38%] left-[14%] text-pink-300/70 text-xs animate-pulse" style={{ animationDelay: '2s' }}>✦</div>
      </div>

      {/* Top Spacer */}
      <div className="w-full h-4" />

      {/* Center Group: Giant Glowing KX Glass Orb + Typography */}
      <div className="flex flex-col items-center justify-center my-auto z-10 w-full text-center">
        {/* Massive Luminous Glass Orb */}
        <div className="relative w-64 h-64 md:w-72 md:h-72 flex items-center justify-center mb-6">
          {/* Intense Outer Neon Aura */}
          <div className="absolute inset-[-14%] rounded-full bg-gradient-to-tr from-indigo-500/35 via-purple-500/35 to-cyan-400/35 blur-2xl animate-pulse-glow" />
          
          {/* Planetary Orbital Rings with 3 Satellites */}
          <div className="absolute inset-[-15%] rounded-full border border-cyan-400/30 rotate-[-28deg] scale-y-[0.45] pointer-events-none" />
          <div className="absolute inset-[-15%] rounded-full border border-pink-400/30 rotate-[42deg] scale-y-[0.4] pointer-events-none" />
          
          {/* Satellite Spheres */}
          <div className="absolute top-[22%] left-[6%] w-3 h-3 rounded-full bg-cyan-300 shadow-[0_0_12px_#38bdf8] border border-white/80" />
          <div className="absolute bottom-[20%] right-[8%] w-3.5 h-3.5 rounded-full bg-pink-400 shadow-[0_0_14px_#ec4899] border border-white/80" />
          <div className="absolute top-[24%] right-[12%] w-2.5 h-2.5 rounded-full bg-blue-300 shadow-[0_0_10px_#60a5fa] border border-white/80" />

          {/* Glass Orb Shell with Asset */}
          <div className="relative w-full h-full rounded-full p-1 bg-gradient-to-b from-white/90 via-white/20 to-cyan-400/40 shadow-[0_10px_40px_rgba(0,0,0,0.6)] overflow-hidden backdrop-blur-2xl">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Brand Orb"
              className="w-full h-full object-contain filter drop-shadow-[0_8px_30px_rgba(99,102,241,0.5)]"
              referrerPolicy="no-referrer"
            />
          </div>
        </div>

        {/* Wordmark & Tagline matching reference 01-welcome.png with luxury designer typography */}
        <div className="space-y-2.5">
          <div className="flex flex-col items-center">
            <h1 className="kx-brand-title-large text-5xl md:text-6xl tracking-[0.25em] select-none">
              KX
            </h1>
            <div className="flex items-center gap-3 mt-1">
              <div className="h-[1.5px] w-10 bg-gradient-to-r from-transparent via-cyan-400 to-indigo-400 shadow-[0_0_8px_#38bdf8]" />
              <span className="kx-brand-sub text-base md:text-lg font-bold tracking-[0.35em] text-cyan-200">
                Private VPN
              </span>
              <div className="h-[1.5px] w-10 bg-gradient-to-l from-transparent via-cyan-400 to-indigo-400 shadow-[0_0_8px_#38bdf8]" />
            </div>
          </div>

          <p className="text-base font-medium tracking-wide text-indigo-200/90 italic pt-1.5 font-sans">
            {t.tagline}
          </p>
        </div>
      </div>

      {/* Bottom CTA Pill Buttons matching reference exactly */}
      <div className="w-full max-w-sm space-y-3.5 z-10 pb-2">
        {/* Primary CTA: "Get Started" (Luminous gradient pill) */}
        <button
          onClick={() => setCurrentScreen('02-home-disconnected')}
          className="group relative w-full h-14 rounded-full p-[1px] bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_4px_30px_rgba(99,102,241,0.45)] hover:shadow-[0_6px_36px_rgba(6,182,212,0.6)] active:scale-[0.98] transition-all overflow-hidden"
        >
          <div className="w-full h-full rounded-full bg-gradient-to-r from-indigo-600/90 via-purple-600/90 to-cyan-500/90 flex items-center justify-center gap-2.5 px-6">
            <ArrowRight className={`w-5 h-5 text-white transition-transform group-hover:translate-x-1 ${locale === 'ar' ? 'rotate-180 group-hover:-translate-x-1' : ''}`} />
            <span className="text-white font-semibold text-base tracking-wide">
              {t.getStarted}
            </span>
          </div>
        </button>

        {/* Secondary CTA: "Import Config" (Translucent frosted glass pill) */}
        <button
          onClick={() => setCurrentScreen('09-import-configuration')}
          className="relative w-full h-14 rounded-full p-[1px] bg-gradient-to-r from-white/30 via-white/10 to-white/20 shadow-lg hover:bg-white/40 active:scale-[0.98] transition-all overflow-hidden"
        >
          <div className="w-full h-full rounded-full bg-[#171b4a]/50 backdrop-blur-xl flex items-center justify-center gap-2.5 px-6 border border-white/15">
            <CloudUpload className="w-5 h-5 text-indigo-200" />
            <span className="text-white/95 font-medium text-base tracking-wide">
              {t.importConfig}
            </span>
          </div>
        </button>
      </div>
    </div>
  );
};
