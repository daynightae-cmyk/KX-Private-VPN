import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  ChevronLeft, 
  Settings, 
  Database, 
  Gauge, 
  Server, 
  ShieldCheck, 
  BarChart2, 
  Clock, 
  ArrowDown, 
  ArrowUp, 
  Layers, 
  Home, 
  User, 
  Shield
} from 'lucide-react';

export const Screen08Statistics: React.FC = () => {
  const { 
    history, 
    setCurrentScreen, 
    connectionStatus,
    t, 
    locale 
  } = useVPN();

  const [timeRange, setTimeRange] = useState<'7D' | '30D' | '3M' | '1Y'>('7D');

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dce2f9] text-slate-800">
      {/* Top Header: Left Back, Center KX Logo, Right Settings */}
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

        <button 
          onClick={() => setCurrentScreen('10-profile-settings')}
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Settings"
        >
          <Settings className="w-5 h-5" />
        </button>
      </div>

      {/* Screen Title */}
      <div className="text-center z-10 my-1">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.statistics}
        </h2>
      </div>

      {/* 4 Metric Cards (2x2 Grid) matching reference */}
      <div className="grid grid-cols-2 gap-2.5 z-10 my-1">
        {/* Total Data */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-400 to-indigo-500 text-white flex items-center justify-center shadow-xs shrink-0">
            <Database className="w-5 h-5" />
          </div>
          <div>
            <span className="block text-[10px] font-semibold text-slate-500">{t.totalData}</span>
            <span className="block text-base font-black text-indigo-950 font-mono">48.7 GB</span>
            <span className="block text-[10px] font-bold text-emerald-600">▲ +12% {t.vsYesterday}</span>
          </div>
        </div>

        {/* Average Ping */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-400 to-cyan-500 text-white flex items-center justify-center shadow-xs shrink-0">
            <Gauge className="w-5 h-5" />
          </div>
          <div>
            <span className="block text-[10px] font-semibold text-slate-500">{t.averagePing}</span>
            <span className="block text-base font-black text-indigo-950 font-mono">36 ms</span>
            <span className="block text-[10px] font-bold text-emerald-600">▼ -18% {t.vsLast7Days}</span>
          </div>
        </div>

        {/* Most Used Server */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-indigo-500 to-purple-600 text-white flex items-center justify-center shadow-xs shrink-0">
            <Server className="w-5 h-5" />
          </div>
          <div>
            <span className="block text-[10px] font-semibold text-slate-500">{t.mostUsedServer}</span>
            <div className="flex items-center gap-1">
              <span className="text-xs">🇸🇬</span>
              <span className="font-bold text-xs text-indigo-950">
                {locale === 'ar' ? 'سنغافورة' : 'Singapore'}
              </span>
            </div>
            <span className="block text-[10px] font-medium text-slate-500">62% {t.ofTime}</span>
          </div>
        </div>

        {/* Privacy Score */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <span className="text-[10px] font-semibold text-slate-500">{t.privacyScore}</span>
          </div>
          <div>
            <span className="text-lg font-black text-indigo-950 font-mono">98/100</span>
            <div className="w-full h-1.5 rounded-full bg-slate-200 overflow-hidden mt-1">
              <div className="h-full rounded-full bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-500 w-[98%]" />
            </div>
          </div>
        </div>
      </div>

      {/* Daily Data Usage Card with Glowing Bezier Chart */}
      <div className="z-10 my-1">
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3.5 border border-white/80 shadow-xs space-y-2.5">
          {/* Header & Tabs */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BarChart2 className="w-4 h-4 text-indigo-600" />
              <span className="font-bold text-xs text-indigo-950">{t.dailyDataUsage}</span>
            </div>

            <div className="flex items-center gap-1 bg-slate-200/60 p-0.5 rounded-full text-[10px] font-semibold">
              {(['7D', '30D', '3M', '1Y'] as const).map(tab => (
                <button
                  key={tab}
                  onClick={() => setTimeRange(tab)}
                  className={`px-2.5 py-0.5 rounded-full transition-all ${
                    timeRange === tab ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-xs' : 'text-slate-600'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>

          {/* SVG Area Chart matching reference */}
          <div className="relative w-full h-28 pt-2">
            <svg className="w-full h-full overflow-visible" viewBox="0 0 320 80">
              <defs>
                <linearGradient id="chart-area-grad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.4" />
                  <stop offset="100%" stopColor="#a855f7" stopOpacity="0.0" />
                </linearGradient>
              </defs>

              {/* Grid Lines */}
              <line x1="0" y1="10" x2="320" y2="10" stroke="#cbd5e1" strokeWidth="0.5" strokeDasharray="3 3" />
              <line x1="0" y1="35" x2="320" y2="35" stroke="#cbd5e1" strokeWidth="0.5" strokeDasharray="3 3" />
              <line x1="0" y1="60" x2="320" y2="60" stroke="#cbd5e1" strokeWidth="0.5" strokeDasharray="3 3" />

              {/* Area Fill */}
              <path 
                d="M 10 65 Q 40 60 70 55 T 130 50 T 180 55 T 230 45 T 280 40 L 305 20 L 305 75 L 10 75 Z"
                fill="url(#chart-area-grad)"
              />

              {/* Glowing Line */}
              <path 
                d="M 10 65 Q 40 60 70 55 T 130 50 T 180 55 T 230 45 T 280 40 L 305 20"
                fill="none"
                stroke="#06b6d4"
                strokeWidth="2.5"
              />

              {/* Data Point Circles */}
              {[
                { x: 10, y: 65 },
                { x: 60, y: 56 },
                { x: 110, y: 52 },
                { x: 160, y: 54 },
                { x: 210, y: 46 },
                { x: 260, y: 42 },
                { x: 305, y: 20 },
              ].map((pt, idx) => (
                <circle 
                  key={idx}
                  cx={pt.x}
                  cy={pt.y}
                  r="3.5"
                  fill="#ffffff"
                  stroke={idx === 6 ? "#ec4899" : "#06b6d4"}
                  strokeWidth="2"
                />
              ))}
            </svg>

            {/* Peak Tooltip Pill at Sunday (10.8 GB) */}
            <div className="absolute top-0 right-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-purple-600 to-pink-500 text-white font-mono text-[9px] font-bold shadow-xs">
              10.8 GB
            </div>

            {/* X-Axis Day Labels */}
            <div className="flex justify-between text-[9px] font-semibold text-slate-400 mt-1 px-1">
              <span>Mon</span>
              <span>Tue</span>
              <span>Wed</span>
              <span>Thu</span>
              <span>Fri</span>
              <span>Sat</span>
              <span className="text-purple-600 font-bold">Sun</span>
            </div>
          </div>
        </div>
      </div>

      {/* 2 Middle Cards: Average Speed & Connection Time */}
      <div className="grid grid-cols-2 gap-2.5 z-10 my-1">
        {/* Average Speed */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center gap-1.5">
            <Gauge className="w-4 h-4 text-cyan-600" />
            <span className="text-[10px] font-semibold text-slate-500">{t.averageSpeed}</span>
          </div>
          <div>
            <span className="text-base font-black text-indigo-950 font-mono">112.4 Mbps</span>
            <div className="flex items-center gap-1.5 mt-1">
              <span className="px-1.5 py-0.2 rounded-full bg-cyan-100 text-[9px] font-bold text-cyan-700">↓ 138.7</span>
              <span className="px-1.5 py-0.2 rounded-full bg-purple-100 text-[9px] font-bold text-purple-700">↑ 86.1</span>
            </div>
          </div>
        </div>

        {/* Connection Time with 60% Donut Ring */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center justify-between">
          <div>
            <div className="flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-purple-600" />
              <span className="text-[10px] font-semibold text-slate-500">{t.connectionTime}</span>
            </div>
            <span className="block text-sm font-black text-indigo-950 font-mono mt-1">14 h 26 m</span>
            <span className="block text-[9px] text-slate-400">60% {t.ofDay}</span>
          </div>

          {/* SVG Donut Ring */}
          <div className="relative w-12 h-12 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 36 36">
              <path
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                fill="none"
                stroke="#e2e8f0"
                strokeWidth="3.5"
              />
              <path
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                fill="none"
                stroke="url(#donut-grad)"
                strokeWidth="3.5"
                strokeDasharray="60, 100"
              />
              <defs>
                <linearGradient id="donut-grad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stopColor="#06b6d4" />
                  <stop offset="100%" stopColor="#a855f7" />
                </linearGradient>
              </defs>
            </svg>
            <span className="absolute text-[9px] font-bold font-mono text-indigo-950">60%</span>
          </div>
        </div>
      </div>

      {/* Server History List */}
      <div className="z-10 my-1">
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <Layers className="w-4 h-4 text-indigo-600" />
              <span className="font-bold text-xs text-indigo-950">{t.serverHistory}</span>
            </div>
            <span className="text-[10px] font-semibold text-slate-400">{t.last7Days} ⌵</span>
          </div>

          <div className="space-y-1.5">
            {history.map(item => (
              <div key={item.id} className="flex items-center justify-between py-1 border-b border-slate-100 last:border-none">
                <div className="flex items-center gap-2">
                  <span className="text-base">{item.flagEmoji}</span>
                  <div>
                    <span className="block font-bold text-xs text-indigo-950">
                      {locale === 'ar' ? item.serverNameAr : item.serverName}
                    </span>
                    <span className="block text-[9px] text-slate-400">
                      {locale === 'ar' ? item.timeRangeAr : item.timeRange}
                    </span>
                  </div>
                </div>

                <div className="text-right">
                  <span className="block font-mono font-bold text-xs text-indigo-950">{item.dataTransferred}</span>
                  <span className="block text-[9px] font-mono text-slate-500">
                    {locale === 'ar' ? item.durationAr : item.duration}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />
    </div>
  );
};
