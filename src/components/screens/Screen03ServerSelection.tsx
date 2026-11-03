import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  ChevronLeft, 
  Settings, 
  Search, 
  Mic, 
  Zap, 
  Play, 
  Gamepad2, 
  Star, 
  Crown
} from 'lucide-react';

export const Screen03ServerSelection: React.FC = () => {
  const { 
    servers, 
    selectedServer, 
    setSelectedServer, 
    toggleFavoriteServer, 
    setCurrentScreen, 
    t, 
    locale 
  } = useVPN();

  const [activeCategory, setActiveCategory] = useState<'all' | 'fastest' | 'streaming' | 'gaming'>('all');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredServers = servers.filter(server => {
    const matchesCategory = activeCategory === 'all' || server.categories.includes(activeCategory);
    const matchesSearch = 
      server.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
      server.nameAr.includes(searchQuery) ||
      server.city.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-[#0c102c] text-white">
      {/* Background Cosmic Atmosphere */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-0 left-[-20%] w-[140%] h-[50%] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-blue-700/20 via-purple-900/15 to-transparent blur-3xl" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[80%] h-[40%] bg-[radial-gradient(circle,_var(--tw-gradient-stops))] from-indigo-500/20 via-transparent to-transparent blur-2xl" />

        {/* Diagonal Wave Ribbons */}
        <svg className="absolute inset-0 w-full h-full" viewBox="0 0 390 844" fill="none" preserveAspectRatio="none">
          <path d="M -20 180 C 100 120, 290 320, 420 240" stroke="url(#servers-wave-1)" strokeWidth="1.6" opacity="0.6" />
          <path d="M -30 680 C 140 720, 270 560, 430 620" stroke="url(#servers-wave-2)" strokeWidth="1.8" opacity="0.7" />
          <defs>
            <linearGradient id="servers-wave-1" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#38bdf8" stopOpacity="0" />
              <stop offset="50%" stopColor="#818cf8" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#c084fc" stopOpacity="0" />
            </linearGradient>
            <linearGradient id="servers-wave-2" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#c084fc" stopOpacity="0" />
              <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#ec4899" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>

        <div className="absolute top-[16%] right-[22%] text-white/70 text-xs">✦</div>
      </div>

      {/* Top Header: Left Back button, Center KX Orb, Right Settings */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen('02-home-disconnected')}
          className="w-10 h-10 rounded-full bg-white/10 border border-white/15 flex items-center justify-center hover:bg-white/20 active:scale-95 transition-all text-white"
          aria-label="Back"
        >
          <ChevronLeft className={`w-5 h-5 ${locale === 'ar' ? 'rotate-180' : ''}`} />
        </button>

        {/* Small Glowing KX Orb */}
        <div className="relative w-12 h-12 rounded-full p-[1px] bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_0_20px_rgba(99,102,241,0.6)]">
          <img 
            src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
            alt="KX"
            className="w-full h-full object-contain rounded-full"
            referrerPolicy="no-referrer"
          />
        </div>

        <button 
          onClick={() => setCurrentScreen('10-profile-settings')}
          className="w-10 h-10 rounded-full bg-white/10 border border-white/15 flex items-center justify-center hover:bg-white/20 active:scale-95 transition-all text-white"
          aria-label="Settings"
        >
          <Settings className="w-5 h-5" />
        </button>
      </div>

      {/* Title: "Select Server" with horizontal accent bars matching reference */}
      <div className="flex items-center justify-center gap-3 z-10 my-2">
        <div className="w-8 h-[2px] bg-gradient-to-r from-transparent to-cyan-400" />
        <h2 className="text-xl font-bold tracking-wide text-white">
          {t.selectServer}
        </h2>
        <div className="w-8 h-[2px] bg-gradient-to-l from-transparent to-cyan-400" />
      </div>

      {/* Rounded Glass Search Field */}
      <div className="relative z-10 w-full my-1.5">
        <div className="w-full rounded-full p-[1px] bg-gradient-to-r from-white/20 via-cyan-400/30 to-purple-500/20 shadow-md">
          <div className="w-full h-12 rounded-full bg-[#131742]/70 backdrop-blur-xl px-4 flex items-center gap-3 border border-white/10">
            <Search className="w-4 h-4 text-indigo-300 shrink-0" />
            <input 
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={t.searchPlaceholder}
              className="w-full bg-transparent border-none outline-none text-sm text-white placeholder-indigo-200/50"
            />
            <button 
              type="button" 
              className="text-indigo-300 hover:text-white"
              aria-label="Voice Search"
            >
              <Mic className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Category Pills: All / Fastest / Streaming / Gaming */}
      <div className="flex items-center gap-2 z-10 my-2 overflow-x-auto pb-1 no-scrollbar">
        {/* All */}
        <button
          onClick={() => setActiveCategory('all')}
          className={`px-4 py-1.5 rounded-full text-xs font-semibold tracking-wide transition-all ${
            activeCategory === 'all'
              ? 'bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-[0_2px_12px_rgba(99,102,241,0.5)] border border-white/30'
              : 'bg-white/5 text-indigo-200 border border-white/10 hover:bg-white/10'
          }`}
        >
          {t.tabAll}
        </button>

        {/* Fastest */}
        <button
          onClick={() => setActiveCategory('fastest')}
          className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wide flex items-center gap-1.5 transition-all ${
            activeCategory === 'fastest'
              ? 'bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-[0_2px_12px_rgba(99,102,241,0.5)] border border-white/30'
              : 'bg-white/5 text-indigo-200 border border-white/10 hover:bg-white/10'
          }`}
        >
          <Zap className="w-3.5 h-3.5 text-cyan-300" />
          <span>{t.tabFastest}</span>
        </button>

        {/* Streaming */}
        <button
          onClick={() => setActiveCategory('streaming')}
          className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wide flex items-center gap-1.5 transition-all ${
            activeCategory === 'streaming'
              ? 'bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-[0_2px_12px_rgba(99,102,241,0.5)] border border-white/30'
              : 'bg-white/5 text-indigo-200 border border-white/10 hover:bg-white/10'
          }`}
        >
          <Play className="w-3.5 h-3.5 text-pink-300" />
          <span>{t.tabStreaming}</span>
        </button>

        {/* Gaming */}
        <button
          onClick={() => setActiveCategory('gaming')}
          className={`px-3.5 py-1.5 rounded-full text-xs font-semibold tracking-wide flex items-center gap-1.5 transition-all ${
            activeCategory === 'gaming'
              ? 'bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-[0_2px_12px_rgba(99,102,241,0.5)] border border-white/30'
              : 'bg-white/5 text-indigo-200 border border-white/10 hover:bg-white/10'
          }`}
        >
          <Gamepad2 className="w-3.5 h-3.5 text-emerald-300" />
          <span>{t.tabGaming}</span>
        </button>
      </div>

      {/* Dense Server List (7 Servers matching reference) */}
      <div className="flex-1 overflow-y-auto space-y-2.5 z-10 pr-1 my-1">
        {filteredServers.map((server) => {
          const isSelected = selectedServer?.id === server.id;

          return (
            <div
              key={server.id}
              onClick={() => setSelectedServer(server)}
              className={`group relative rounded-2xl p-[1px] cursor-pointer transition-all ${
                isSelected
                  ? 'bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-500 shadow-[0_0_20px_rgba(99,102,241,0.4)]'
                  : 'bg-white/10 hover:bg-white/20'
              }`}
            >
              <div className="w-full rounded-2xl bg-[#131742]/80 backdrop-blur-xl px-3.5 py-2.5 flex items-center justify-between border border-white/10">
                {/* Left: Flag & Country Info */}
                <div className="flex items-center gap-3">
                  {/* Circular Flag Icon */}
                  <div className="w-9 h-9 rounded-full bg-white/10 flex items-center justify-center text-xl shadow-inner border border-white/20 shrink-0">
                    {server.flagEmoji}
                  </div>

                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-sm text-white">
                        {locale === 'ar' ? server.nameAr : server.name}
                      </span>
                      {server.isRecommended && (
                        <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-purple-500/20 border border-purple-400/40 text-[10px] font-semibold text-purple-200">
                          <Crown className="w-2.5 h-2.5 text-yellow-300" />
                          <span>{t.recommended}</span>
                        </span>
                      )}
                    </div>

                    {/* Server Latency & Load Bars */}
                    <div className="flex items-center gap-3 mt-1">
                      <div className="flex items-center gap-1 text-xs text-cyan-300 font-semibold font-mono">
                        <Zap className="w-3 h-3 text-cyan-300" />
                        <span>{server.ping} ms</span>
                      </div>

                      <div className="flex items-center gap-1.5">
                        {/* 5-segment load bar indicator */}
                        <div className="flex items-center gap-0.5">
                          {[1, 2, 3, 4, 5].map((seg) => (
                            <div 
                              key={seg}
                              className={`w-2 h-1.5 rounded-xs ${
                                seg <= server.loadSegments 
                                  ? (server.loadSegments >= 4 ? 'bg-amber-400' : 'bg-emerald-400') 
                                  : 'bg-white/15'
                              }`}
                            />
                          ))}
                        </div>
                        <span className="text-[10px] text-indigo-200/70 font-mono">
                          {t.load} {server.load}%
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Right: Star Favorite Button */}
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleFavoriteServer(server.id);
                  }}
                  className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-white/10 active:scale-90 transition-all text-indigo-200"
                  aria-label="Toggle Favorite"
                >
                  <Star 
                    className={`w-4 h-4 ${server.isFavorite ? 'text-amber-400 fill-amber-400' : 'text-indigo-300/60'}`} 
                  />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Done Selection Action Footer */}
      <div className="w-full z-10 pt-1 pb-1">
        <button
          onClick={() => setCurrentScreen('02-home-disconnected')}
          className="w-full h-10 rounded-full bg-gradient-to-r from-indigo-500 via-purple-500 to-cyan-400 text-white font-semibold text-xs tracking-wider uppercase shadow-[0_4px_20px_rgba(99,102,241,0.4)] active:scale-[0.98] transition-all flex items-center justify-center gap-1.5"
        >
          <span>{locale === 'ar' ? 'تأكيد اختيار الخادم' : 'Select Server & Return'}</span>
        </button>
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />
    </div>
  );
};
