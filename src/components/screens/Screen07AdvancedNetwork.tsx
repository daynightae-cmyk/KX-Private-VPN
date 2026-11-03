import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { 
  ChevronLeft, 
  MoreVertical, 
  Layers, 
  Link as LinkIcon, 
  Infinity as InfinityIcon, 
  Sliders, 
  Database, 
  ShieldAlert, 
  RefreshCw, 
  Activity,
  ChevronDown
} from 'lucide-react';

export const Screen07AdvancedNetwork: React.FC = () => {
  const { 
    advancedSettings, 
    updateAdvancedSettings, 
    setCurrentScreen, 
    connectionStatus,
    t, 
    locale 
  } = useVPN();

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dce2f9] text-slate-800">
      {/* Top Header: Left Back, Center KX Logo, Right 3-dots */}
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
          className="w-10 h-10 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Options"
        >
          <MoreVertical className="w-5 h-5" />
        </button>
      </div>

      {/* Screen Title */}
      <div className="text-center z-10 my-1">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.advancedNetwork}
        </h2>
      </div>

      {/* Top Info Banner Card */}
      <div className="z-10 my-1">
        <div className="w-full rounded-2xl p-[1px] bg-gradient-to-r from-white via-cyan-200/40 to-purple-200/40 shadow-xs">
          <div className="w-full rounded-2xl bg-white/60 backdrop-blur-xl p-3 flex items-center gap-3 border border-white/80">
            {/* 3D Layered Glass Plates Icon */}
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-cyan-400 to-indigo-600 flex items-center justify-center text-white shadow-[0_2px_12px_rgba(6,182,212,0.4)] border border-white/50 shrink-0">
              <Layers className="w-5 h-5" />
            </div>
            <p className="text-[11px] font-medium text-slate-600 leading-snug">
              {t.advancedNetworkDesc}
            </p>
          </div>
        </div>
      </div>

      {/* Protocol Section */}
      <div className="z-10 my-1 space-y-1.5">
        <h3 className="text-xs font-black tracking-wide text-indigo-950 uppercase">
          {t.protocol}
        </h3>

        {/* 3 Protocol Cards in 1 Row */}
        <div className="grid grid-cols-3 gap-2">
          {/* WireGuard */}
          <button
            onClick={() => updateAdvancedSettings({ protocol: 'wireguard' })}
            className={`py-2.5 px-2 rounded-2xl border transition-all flex flex-col items-center justify-center gap-1 ${
              advancedSettings.protocol === 'wireguard'
                ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-sm border-transparent'
                : 'bg-white/60 text-indigo-950 border-white hover:bg-white'
            }`}
          >
            <LinkIcon className="w-4 h-4" />
            <span className="text-[11px] font-bold tracking-tight">
              {t.protocolWireGuard}
            </span>
          </button>

          {/* OpenVPN */}
          <button
            onClick={() => updateAdvancedSettings({ protocol: 'openvpn' })}
            className={`py-2.5 px-2 rounded-2xl border transition-all flex flex-col items-center justify-center gap-1 ${
              advancedSettings.protocol === 'openvpn'
                ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-sm border-transparent'
                : 'bg-white/60 text-indigo-950 border-white hover:bg-white'
            }`}
          >
            <InfinityIcon className="w-4 h-4" />
            <span className="text-[11px] font-bold tracking-tight">
              {t.protocolOpenVPN}
            </span>
          </button>

          {/* Automatic (Default Active) */}
          <button
            onClick={() => updateAdvancedSettings({ protocol: 'automatic' })}
            className={`py-2.5 px-2 rounded-2xl border transition-all flex flex-col items-center justify-center gap-1 ${
              advancedSettings.protocol === 'automatic'
                ? 'bg-gradient-to-r from-indigo-500 via-purple-600 to-cyan-500 text-white shadow-sm border-transparent'
                : 'bg-white/60 text-indigo-950 border-white hover:bg-white'
            }`}
          >
            <InfinityIcon className="w-4 h-4" />
            <span className="text-[11px] font-bold tracking-tight">
              {t.protocolAutomatic}
            </span>
          </button>
        </div>
      </div>

      {/* Advanced Controls Section */}
      <div className="flex-1 overflow-y-auto space-y-2 z-10 my-1 pr-0.5">
        <h3 className="text-xs font-black tracking-wide text-indigo-950 uppercase">
          {t.advancedControls}
        </h3>

        {/* MTU Slider Card */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-7 h-7 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Sliders className="w-4 h-4" />
              </div>
              <div>
                <span className="block font-bold text-xs text-indigo-950">{t.mtu}</span>
                <span className="block text-[10px] text-slate-500">{t.mtuDesc}</span>
              </div>
            </div>

            <span className="px-2.5 py-0.5 rounded-full bg-white shadow-xs border border-slate-200 text-xs font-mono font-bold text-indigo-950">
              {advancedSettings.mtu}
            </span>
          </div>

          {/* Slider */}
          <input 
            type="range"
            min="1280"
            max="1500"
            step="10"
            value={advancedSettings.mtu}
            onChange={(e) => updateAdvancedSettings({ mtu: Number(e.target.value) })}
            className="w-full h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
          />
        </div>

        {/* DNS Mode Dropdown Card */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center">
              <Database className="w-4 h-4" />
            </div>
            <div>
              <span className="block font-bold text-xs text-indigo-950">{t.dnsMode}</span>
              <span className="block text-[10px] text-slate-500">{t.dnsModeDesc}</span>
            </div>
          </div>

          <div className="flex items-center gap-1 px-3 py-1 rounded-full bg-white shadow-xs border border-slate-200 text-xs font-semibold text-indigo-950">
            <span>{t.encryptedDns}</span>
            <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
          </div>
        </div>

        {/* Obfuscation Toggle Card */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
              <ShieldAlert className="w-4 h-4" />
            </div>
            <div>
              <span className="block font-bold text-xs text-indigo-950">{t.obfuscation}</span>
              <span className="block text-[10px] text-slate-500">{t.obfuscationDesc}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => updateAdvancedSettings({ obfuscation: !advancedSettings.obfuscation })}
            className={`relative w-11 h-6 rounded-full p-0.5 transition-colors duration-300 shrink-0 ${
              advancedSettings.obfuscation ? 'bg-gradient-to-r from-indigo-500 to-purple-600' : 'bg-slate-300'
            }`}
            aria-label="Toggle Obfuscation"
          >
            <div 
              className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${
                advancedSettings.obfuscation ? (locale === 'ar' ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Port Hopping Toggle Card */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-xl bg-cyan-50 text-cyan-600 flex items-center justify-center">
              <RefreshCw className="w-4 h-4" />
            </div>
            <div>
              <span className="block font-bold text-xs text-indigo-950">{t.portHopping}</span>
              <span className="block text-[10px] text-slate-500">{t.portHoppingDesc}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => updateAdvancedSettings({ portHopping: !advancedSettings.portHopping })}
            className={`relative w-11 h-6 rounded-full p-0.5 transition-colors duration-300 shrink-0 ${
              advancedSettings.portHopping ? 'bg-gradient-to-r from-indigo-500 to-purple-600' : 'bg-slate-300'
            }`}
            aria-label="Toggle Port Hopping"
          >
            <div 
              className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${
                advancedSettings.portHopping ? (locale === 'ar' ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Handshake Keepalive Slider Card */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl p-3 border border-white/80 shadow-xs space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-7 h-7 rounded-xl bg-pink-50 text-pink-600 flex items-center justify-center">
                <Activity className="w-4 h-4" />
              </div>
              <div>
                <span className="block font-bold text-xs text-indigo-950">{t.handshakeKeepalive}</span>
                <span className="block text-[10px] text-slate-500">{t.handshakeKeepaliveDesc}</span>
              </div>
            </div>

            <span className="px-2.5 py-0.5 rounded-full bg-white shadow-xs border border-slate-200 text-xs font-mono font-bold text-indigo-950">
              {advancedSettings.handshakeKeepalive} s
            </span>
          </div>

          {/* Slider */}
          <input 
            type="range"
            min="0"
            max="60"
            step="5"
            value={advancedSettings.handshakeKeepalive}
            onChange={(e) => updateAdvancedSettings({ handshakeKeepalive: Number(e.target.value) })}
            className="w-full h-1.5 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-indigo-600"
          />
        </div>
      </div>
    </div>
  );
};
