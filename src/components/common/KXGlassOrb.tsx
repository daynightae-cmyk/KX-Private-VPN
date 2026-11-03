import React from 'react';

interface KXGlassOrbProps {
  size?: 'sm' | 'md' | 'lg' | 'xl' | 'hero';
  className?: string;
  glowColor?: 'violet' | 'cyan' | 'magenta' | 'emerald';
  variant?: 'brand' | 'power' | 'shield-check' | 'qr-scan' | 'split';
  isActive?: boolean;
  onClick?: () => void;
  showOrbits?: boolean;
  interactive?: boolean;
  children?: React.ReactNode;
}

export const KXGlassOrb: React.FC<KXGlassOrbProps> = ({
  size = 'md',
  className = '',
  glowColor = 'violet',
  variant = 'brand',
  isActive = false,
  onClick,
  showOrbits = true,
  interactive = false,
  children,
}) => {
  const sizeMap = {
    sm: 'w-10 h-10',
    md: 'w-14 h-14',
    lg: 'w-24 h-24',
    xl: 'w-36 h-36',
    hero: 'w-64 h-64 md:w-72 md:h-72',
  };

  const glowStyles = {
    violet: 'from-indigo-500/40 via-purple-500/30 to-pink-500/20 shadow-[0_0_50px_rgba(168,85,247,0.45)]',
    cyan: 'from-cyan-400/40 via-blue-500/30 to-indigo-500/20 shadow-[0_0_50px_rgba(6,182,212,0.45)]',
    magenta: 'from-pink-500/40 via-purple-600/30 to-indigo-500/20 shadow-[0_0_50px_rgba(236,72,153,0.45)]',
    emerald: 'from-emerald-400/40 via-teal-500/30 to-cyan-500/20 shadow-[0_0_50px_rgba(16,185,129,0.45)]',
  };

  return (
    <div 
      className={`relative flex items-center justify-center shrink-0 ${sizeMap[size]} ${interactive ? 'cursor-pointer active:scale-95 transition-transform duration-200' : ''} ${className}`}
      onClick={onClick}
    >
      {/* Outer Glow Halo */}
      <div 
        className={`absolute inset-[-12%] rounded-full bg-gradient-to-br ${glowStyles[glowColor]} blur-xl pointer-events-none transition-all duration-700 ${isActive ? 'opacity-100 scale-105' : 'opacity-70'}`}
      />

      {/* Orbit Rings and Satellites */}
      {showOrbits && (size === 'xl' || size === 'hero' || size === 'lg') && (
        <div className="absolute inset-[-18%] pointer-events-none">
          {/* Planetary Elliptical Orbit 1 */}
          <div className="absolute inset-0 rounded-full border border-white/20 dark:border-cyan-400/25 rotate-[-25deg] scale-y-[0.45] animate-orbit" />
          {/* Planetary Elliptical Orbit 2 */}
          <div className="absolute inset-0 rounded-full border border-pink-400/25 rotate-[45deg] scale-y-[0.4] animate-orbit-reverse" />
          
          {/* Satellite Node 1 (Cyan) */}
          <div 
            className="absolute top-[20%] left-[8%] w-3 h-3 rounded-full bg-gradient-to-br from-cyan-300 to-blue-600 shadow-[0_0_12px_#38bdf8] border border-white/60 animate-float"
            style={{ animationDelay: '0.4s' }}
          />
          {/* Satellite Node 2 (Magenta) */}
          <div 
            className="absolute bottom-[22%] right-[10%] w-3.5 h-3.5 rounded-full bg-gradient-to-br from-pink-300 to-purple-600 shadow-[0_0_14px_#ec4899] border border-white/70 animate-float"
            style={{ animationDelay: '1.2s' }}
          />
          {/* Satellite Node 3 (Pearl Blue) */}
          <div 
            className="absolute top-[28%] right-[14%] w-2.5 h-2.5 rounded-full bg-gradient-to-br from-blue-200 to-indigo-500 shadow-[0_0_10px_#818cf8] border border-white/50 animate-float"
            style={{ animationDelay: '2.1s' }}
          />

          {/* Star Sparkles */}
          <div className="absolute top-[14%] right-[28%] text-white/90 text-xs animate-pulse">✦</div>
          <div className="absolute bottom-[28%] left-[22%] text-pink-300/80 text-xs animate-pulse" style={{ animationDelay: '1s' }}>✦</div>
        </div>
      )}

      {/* Main Glass Sphere Body */}
      <div className="relative w-full h-full rounded-full p-[2px] bg-gradient-to-b from-white/80 via-white/20 to-cyan-400/40 shadow-2xl overflow-hidden backdrop-blur-2xl">
        {/* Specular Edge Highlights */}
        <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-indigo-900/40 via-purple-600/10 to-white/40 pointer-events-none" />
        
        {/* Curved Top Reflection Sheen */}
        <div className="absolute -top-[15%] left-[10%] right-[10%] h-[50%] rounded-full bg-gradient-to-b from-white/60 via-white/10 to-transparent blur-[1px] pointer-events-none" />

        {/* Content depending on Variant */}
        {variant === 'brand' && (
          <div className="relative w-full h-full rounded-full flex items-center justify-center bg-gradient-to-b from-[#1c1d4a]/70 via-[#101438]/80 to-[#0b0c26]/90 p-1">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Logo"
              className="w-full h-full object-contain filter drop-shadow-[0_4px_16px_rgba(99,102,241,0.5)]"
              referrerPolicy="no-referrer"
            />
          </div>
        )}

        {variant === 'power' && (
          <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-indigo-950/80 via-purple-950/70 to-slate-950/90 shadow-inner">
            {/* Glowing Ring Interior */}
            <div className="absolute inset-[10%] rounded-full border border-cyan-400/40 shadow-[0_0_30px_rgba(6,182,212,0.3)] flex items-center justify-center">
              <div className="w-16 h-16 md:w-20 md:h-20 rounded-full bg-gradient-to-tr from-cyan-500/20 to-purple-500/20 border border-white/30 flex items-center justify-center shadow-lg backdrop-blur-md">
                <svg className="w-8 h-8 md:w-10 md:h-10 text-white drop-shadow-[0_0_12px_#38bdf8]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18.36 6.64a9 9 0 1 1-12.73 0" />
                  <line x1="12" y1="2" x2="12" y2="12" />
                </svg>
              </div>
            </div>

            {/* Bottom Status Inside Orb */}
            <div className="absolute bottom-[16%] flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/40 backdrop-blur-md border border-white/10 text-white text-xs font-medium tracking-wide">
              <span className="w-2 h-2 rounded-full border border-pink-400 bg-transparent animate-ping" />
              <span className="w-2 h-2 rounded-full bg-pink-500 shadow-[0_0_8px_#ec4899] -ml-3.5" />
              <span>Disconnected</span>
            </div>
          </div>
        )}

        {variant === 'shield-check' && (
          <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-indigo-100/60 via-purple-100/40 to-blue-200/50 shadow-inner">
            <div className="w-20 h-20 md:w-24 md:h-24 rounded-full bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center shadow-[0_0_30px_rgba(99,102,241,0.5)] border-2 border-white/60">
              <svg className="w-10 h-10 text-white drop-shadow-md" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </div>
          </div>
        )}

        {variant === 'qr-scan' && (
          <div className="relative w-full h-full rounded-full flex flex-col items-center justify-center bg-gradient-to-b from-white/80 via-indigo-50/70 to-blue-100/80 shadow-inner">
            {/* Viewfinder Target & QR Code */}
            <div className="relative w-28 h-28 md:w-32 md:h-32 rounded-2xl bg-white/40 border border-white/80 backdrop-blur-md flex items-center justify-center p-3 shadow-lg">
              {/* Corner Reticles */}
              <div className="absolute -top-1 -left-1 w-4 h-4 border-t-2 border-l-2 border-cyan-500 rounded-tl-sm" />
              <div className="absolute -top-1 -right-1 w-4 h-4 border-t-2 border-r-2 border-cyan-500 rounded-tr-sm" />
              <div className="absolute -bottom-1 -left-1 w-4 h-4 border-b-2 border-l-2 border-cyan-500 rounded-bl-sm" />
              <div className="absolute -bottom-1 -right-1 w-4 h-4 border-b-2 border-r-2 border-cyan-500 rounded-br-sm" />

              {/* Scanning Laser Line */}
              <div className="absolute left-2 right-2 h-0.5 bg-gradient-to-r from-transparent via-cyan-400 to-transparent shadow-[0_0_10px_#06b6d4] animate-laser" />

              {/* QR Pattern Representation */}
              <svg className="w-full h-full text-indigo-900/75" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2 2h8v8H2V2zm2 2v4h4V4H4zm10-2h8v8h-8V2zm2 2v4h4V4h-4zM2 14h8v8H2v-8zm2 2v4h4v-4H4zm14-2h4v4h-4v-4zm-4 4h4v4h-4v-4zm4 0h2v4h-2v-4zm-8-2h2v2h-2v-2zm4-2h2v2h-2v-2zm-2 4h2v2h-2v-2zm-6-2h2v2H6v-2zm8 4h2v2h-2v-2z" />
              </svg>
            </div>
          </div>
        )}

        {children}
      </div>
    </div>
  );
};
