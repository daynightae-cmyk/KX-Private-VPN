import React, { useEffect, useState, useRef } from 'react';
import { useVPN } from '../../context/VPNContext';
import { ArrowRight, Sparkles, Shield, Zap, RefreshCw, Radio } from 'lucide-react';

export const Screen00Splash: React.FC = () => {
  const { setCurrentScreen, t, locale, theme, toggleTheme, toggleLocale } = useVPN();
  const [phaseIndex, setPhaseIndex] = useState(0);
  const [pulseActive, setPulseActive] = useState(false);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const [shockwaves, setShockwaves] = useState<number[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);

  // Futuristic initialization sequence stages
  const phases = [
    { title: 'QUANTUM PROTOCOL', subtitle: locale === 'ar' ? 'تهيئة بروتوكولات التشفير الفائقة' : 'Initializing Post-Quantum Encryption' },
    { title: 'SHIELD SYNCHRONIZATION', subtitle: locale === 'ar' ? 'معايرة الدرع الكريستالي الهولوجرامي' : 'Calibrating Holographic Shield Resonance' },
    { title: 'ZERO-KNOWLEDGE TUNNEL', subtitle: locale === 'ar' ? 'التحقق من نفق الخصوصية الصارم' : 'Establishing Zero-Telemetry Encrypted Pipe' },
    { title: 'GATEWAY READY', subtitle: locale === 'ar' ? 'البوابة جاهزة للاتصال الفوري' : 'High-Speed Global Ingress Ready' },
  ];

  useEffect(() => {
    const timer = setInterval(() => {
      setPhaseIndex(prev => {
        if (prev < phases.length - 1) {
          return prev + 1;
        }
        return prev;
      });
    }, 900);

    return () => clearInterval(timer);
  }, [phases.length]);

  // Interactive mouse/touch tilt
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width - 0.5) * 20; // -10 to +10 deg
    const y = ((e.clientY - rect.top) / rect.height - 0.5) * -20;
    setTilt({ x, y });
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0 });
  };

  // Trigger non-traditional interactive holographic shockwave
  const triggerHologramPulse = () => {
    setPulseActive(true);
    setShockwaves(prev => [...prev.slice(-3), Date.now()]);
    setTimeout(() => setPulseActive(false), 700);
  };

  const progressPercent = Math.min(100, Math.round(((phaseIndex + 1) / phases.length) * 100));

  return (
    <div 
      ref={containerRef}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      className="relative w-full h-full min-h-[740px] flex flex-col items-center justify-between p-6 overflow-hidden bg-[#070a1e] text-white select-none"
    >
      {/* 1. Deep Space & Iridescent Nebula Light Matrix */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {/* Core radial blooms */}
        <div className="absolute -top-[25%] -left-[20%] w-[150%] h-[150%] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/45 via-[#131138]/40 to-transparent blur-3xl" />
        <div className="absolute top-[35%] -right-[30%] w-[100%] h-[100%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-cyan-500/25 via-purple-900/20 to-transparent blur-3xl" />
        <div className="absolute bottom-[-20%] left-[10%] w-[90%] h-[60%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-pink-600/20 via-transparent to-transparent blur-3xl" />

        {/* Dynamic Curved Vector Flow Ribbons matching canonical art */}
        <svg className="absolute inset-0 w-full h-full opacity-60" viewBox="0 0 400 850" fill="none" preserveAspectRatio="none">
          <path d="M-60 180 C 120 120, 260 360, 460 260" stroke="url(#splash-cyan-stream)" strokeWidth="2" />
          <path d="M-40 420 C 130 500, 280 260, 460 480" stroke="url(#splash-violet-stream)" strokeWidth="2.2" />
          <path d="M-50 680 C 110 590, 290 740, 450 640" stroke="url(#splash-pink-stream)" strokeWidth="1.6" />
          <defs>
            <linearGradient id="splash-cyan-stream" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.1" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="1" />
              <stop offset="100%" stopColor="#818cf8" stopOpacity="0.1" />
            </linearGradient>
            <linearGradient id="splash-violet-stream" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#c084fc" stopOpacity="0.1" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#ec4899" stopOpacity="0.1" />
            </linearGradient>
            <linearGradient id="splash-pink-stream" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#f43f5e" stopOpacity="0.1" />
              <stop offset="50%" stopColor="#a855f7" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#38bdf8" stopOpacity="0.1" />
            </linearGradient>
          </defs>
        </svg>

        {/* Floating Quantum Micro-Particles */}
        <div className="absolute top-[18%] left-[15%] w-1.5 h-1.5 rounded-full bg-cyan-300 shadow-[0_0_10px_#38bdf8] animate-particle-1" />
        <div className="absolute top-[28%] right-[18%] w-2 h-2 rounded-full bg-pink-400 shadow-[0_0_12px_#ec4899] animate-particle-2" />
        <div className="absolute bottom-[36%] left-[22%] w-1.5 h-1.5 rounded-full bg-indigo-300 shadow-[0_0_8px_#818cf8] animate-particle-3" />
        <div className="absolute top-[52%] right-[12%] w-1 h-1 rounded-full bg-white shadow-[0_0_6px_#fff] animate-particle-1" />
      </div>

      {/* 2. Top Status HUD Bar */}
      <div className="w-full flex items-center justify-between z-20 pt-1">
        {/* Live Quantum Pulse Indicator */}
        <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-white/[0.08] backdrop-blur-xl border border-white/15 text-xs text-cyan-300 shadow-sm">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-cyan-400"></span>
          </span>
          <span className="font-mono tracking-wider font-semibold text-[11px]">
            {phases[phaseIndex].title}
          </span>
        </div>

        {/* Quick controls: Theme & Language preview */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={toggleLocale}
            className="px-2.5 py-1 rounded-full bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-semibold text-indigo-200 transition-colors"
            title="Toggle Language"
          >
            {locale === 'en' ? 'عربي' : 'EN'}
          </button>
          <button
            onClick={toggleTheme}
            className="px-2.5 py-1 rounded-full bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-semibold text-indigo-200 transition-colors"
            title="Toggle Theme"
          >
            {theme === 'night' ? '🌙 Night' : '☀️ Day'}
          </button>
        </div>
      </div>

      {/* 3. CENTER HERO: Non-traditional Interactive Glass Orb Core */}
      <div className="flex flex-col items-center justify-center my-auto z-20 w-full">
        {/* Interactive 3D Holographic Container */}
        <div 
          onClick={triggerHologramPulse}
          style={{
            transform: `perspective(1000px) rotateY(${tilt.x}deg) rotateX(${tilt.y}deg) scale(${pulseActive ? 1.04 : 1})`,
            transition: 'transform 0.25s cubic-bezier(0.1, 0.9, 0.2, 1)',
          }}
          className="relative w-64 h-64 md:w-72 md:h-72 flex items-center justify-center cursor-pointer group"
          role="button"
          tabIndex={0}
          title="Click to trigger quantum pulse"
        >
          {/* Luminous Neon Aura */}
          <div className="absolute inset-[-16%] rounded-full bg-gradient-to-tr from-cyan-500/30 via-indigo-600/35 to-purple-600/35 blur-3xl animate-pulse-glow" />

          {/* Interactive Shockwave rings emitted on click */}
          {shockwaves.map(id => (
            <div
              key={id}
              className="absolute inset-0 rounded-full border-2 border-cyan-300 shadow-[0_0_25px_#38bdf8] pointer-events-none animate-shockwave"
            />
          ))}

          {/* Planetary Orbital Rings with 3 Satellites */}
          <div className="absolute inset-[-14%] rounded-full border border-cyan-400/40 rotate-[-26deg] scale-y-[0.45] animate-orbit pointer-events-none" />
          <div className="absolute inset-[-14%] rounded-full border border-pink-400/35 rotate-[45deg] scale-y-[0.4] animate-orbit-reverse pointer-events-none" />

          {/* Satellite Spheres with glowing trails */}
          <div className="absolute top-[20%] left-[4%] w-3.5 h-3.5 rounded-full bg-cyan-300 shadow-[0_0_14px_#38bdf8] border border-white" />
          <div className="absolute bottom-[18%] right-[6%] w-4 h-4 rounded-full bg-pink-400 shadow-[0_0_16px_#ec4899] border border-white" />
          <div className="absolute top-[26%] right-[10%] w-2.5 h-2.5 rounded-full bg-indigo-300 shadow-[0_0_10px_#818cf8] border border-white" />

          {/* The Exact Canonical KX Glass Orb */}
          <div className="relative w-full h-full rounded-full p-1 bg-gradient-to-b from-white/95 via-white/20 to-cyan-400/50 shadow-[0_16px_50px_rgba(0,0,0,0.7)] overflow-hidden backdrop-blur-2xl">
            {/* Top Specular Curved Highlight */}
            <div className="absolute -top-[10%] left-[10%] right-[10%] h-[40%] rounded-full bg-gradient-to-b from-white/80 via-white/10 to-transparent blur-[1px] pointer-events-none z-10" />

            {/* Laser Scan line sweeping across orb */}
            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-cyan-400/25 to-transparent h-10 w-full animate-laser pointer-events-none z-10" />

            {/* Core Image Asset */}
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Canonical Brand Logo"
              className="w-full h-full object-contain filter drop-shadow-[0_10px_35px_rgba(99,102,241,0.65)] select-none"
              referrerPolicy="no-referrer"
            />
          </div>

          {/* Tap-to-pulse badge */}
          <div className="absolute -bottom-3 px-3 py-0.5 rounded-full bg-[#13153d]/90 border border-cyan-400/30 text-[10px] font-mono tracking-widest text-cyan-200/90 shadow-lg flex items-center gap-1.5 backdrop-blur-md opacity-85 group-hover:opacity-100 transition-opacity">
            <Zap className="w-2.5 h-2.5 text-yellow-300 animate-pulse" />
            <span>PULSE SPHERE</span>
          </div>
        </div>

        {/* 4. Luxury Designer Typography ("KX VPN" / "KX Private VPN") */}
        <div className="mt-8 text-center space-y-2">
          {/* Main Brand Title with luxury designer font */}
          <div className="flex flex-col items-center justify-center">
            <div className="relative inline-block">
              <h1 className="kx-brand-title-large text-4xl sm:text-5xl md:text-6xl tracking-widest select-none">
                KX
              </h1>
              {/* Laser reflection sweep */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent opacity-0 hover:opacity-100 transition-opacity pointer-events-none" />
            </div>

            <div className="flex items-center gap-3 mt-1">
              <div className="h-[1px] w-8 bg-gradient-to-r from-transparent to-cyan-400/60" />
              <span className="kx-brand-sub text-sm sm:text-base font-bold tracking-[0.3em]">
                PRIVATE VPN
              </span>
              <div className="h-[1px] w-8 bg-gradient-to-l from-transparent to-cyan-400/60" />
            </div>
          </div>

          {/* Tagline / Subtitle */}
          <p className="text-xs sm:text-sm font-medium tracking-widest text-indigo-200/80 italic pt-1 max-w-xs mx-auto">
            {phases[phaseIndex].subtitle}
          </p>
        </div>
      </div>

      {/* 5. Bottom Modern Control Deck & Launch Gate */}
      <div className="w-full max-w-sm space-y-4 z-20 pb-2">
        {/* Futuristic Multi-Segment Quantum Progress Meter */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs font-mono">
            <span className="text-cyan-300/80 flex items-center gap-1.5">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
              {phases[phaseIndex].title}
            </span>
            <span className="text-white font-bold">{progressPercent}%</span>
          </div>

          {/* Segmented Glowing Bar */}
          <div className="grid grid-cols-4 gap-1.5 h-2 w-full">
            {phases.map((_, idx) => (
              <div 
                key={idx}
                className={`h-full rounded-full transition-all duration-500 ${
                  idx <= phaseIndex 
                    ? 'bg-gradient-to-r from-cyan-400 to-indigo-500 shadow-[0_0_10px_#38bdf8]' 
                    : 'bg-white/10'
                }`}
              />
            ))}
          </div>
        </div>

        {/* Primary Unconventional Launch Button with Shimmer Sweep */}
        <button
          onClick={() => setCurrentScreen('01-welcome')}
          className="shimmer-sweep w-full h-14 rounded-full p-[1px] bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_4px_32px_rgba(99,102,241,0.55)] hover:shadow-[0_6px_40px_rgba(6,182,212,0.7)] active:scale-[0.98] transition-all cursor-pointer group"
        >
          <div className="w-full h-full rounded-full bg-gradient-to-r from-[#171a4f] via-[#211a54] to-[#14234c] flex items-center justify-center gap-3 px-6 border border-white/25">
            <Sparkles className="w-4 h-4 text-cyan-300 animate-spin" style={{ animationDuration: '6s' }} />
            <span className="kx-brand-title text-sm sm:text-base tracking-wider text-white font-bold">
              {locale === 'ar' ? 'بدء تجربة KX الآمنة' : 'LAUNCH KX EXPERIENCE'}
            </span>
            <ArrowRight className={`w-4 h-4 text-cyan-300 transition-transform group-hover:translate-x-1 ${locale === 'ar' ? 'rotate-180 group-hover:-translate-x-1' : ''}`} />
          </div>
        </button>

        {/* Secondary Sub-action */}
        <div className="flex items-center justify-center gap-4 text-xs text-indigo-300/60 pt-1 font-mono">
          <span>CURVE25519 READY</span>
          <span>•</span>
          <span>ZERO-LOG SHIELD</span>
          <span>•</span>
          <span>v1.0.4</span>
        </div>
      </div>
    </div>
  );
};
