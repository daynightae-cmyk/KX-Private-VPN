import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  Pencil, 
  Crown, 
  User, 
  Bell, 
  Palette, 
  Globe, 
  Power, 
  Link as LinkIcon, 
  Headphones, 
  ShieldCheck, 
  Info, 
  ChevronRight, 
  Home, 
  Rocket, 
  Sliders, 
  Settings,
  X
} from 'lucide-react';

export const Screen10ProfileSettings: React.FC = () => {
  const { 
    locale, 
    setLocale, 
    theme, 
    toggleTheme, 
    setCurrentScreen, 
    t 
  } = useVPN();

  const [backgroundConnection, setBackgroundConnection] = useState(true);
  const [activeModal, setActiveModal] = useState<'language' | 'appearance' | 'about' | null>(null);

  return (
    <div className="relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden bg-gradient-to-b from-[#e3e8fc] via-[#eceffa] to-[#dce2f9] text-slate-800">
      {/* Top Header: Right Edit Pencil Icon */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <div className="w-9" />

        {/* Center Glowing Glass Orb */}
        <div className="relative w-20 h-20 flex items-center justify-center">
          <div className="absolute inset-[-10%] rounded-full bg-gradient-to-tr from-cyan-400/35 via-indigo-400/35 to-pink-400/35 blur-xl animate-pulse-glow" />
          <div className="relative w-full h-full rounded-full p-[1px] bg-gradient-to-b from-white via-white/40 to-cyan-400/50 shadow-md overflow-hidden backdrop-blur-2xl">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Logo"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
        </div>

        {/* Edit Button */}
        <button 
          className="w-9 h-9 rounded-full bg-white/70 shadow-sm border border-white flex items-center justify-center hover:bg-white active:scale-95 transition-all text-indigo-950"
          aria-label="Edit Profile"
        >
          <Pencil className="w-4 h-4" />
        </button>
      </div>

      {/* Profile Header: Name, Email & Premium Badge */}
      <div className="text-center z-10 my-1 space-y-1">
        <h2 className="text-xl font-black tracking-tight text-indigo-950">
          {t.myProfile}
        </h2>
        <span className="block text-xs font-medium text-slate-500 font-mono">
          user@kxvpn.com
        </span>

        {/* KX Premium Badge */}
        <div className="inline-flex items-center gap-1.5 px-3.5 py-1 rounded-full bg-gradient-to-r from-purple-600 via-indigo-600 to-cyan-500 text-white text-[11px] font-bold shadow-xs mt-1">
          <Crown className="w-3.5 h-3.5 text-yellow-300" />
          <span>{t.premiumBadge}</span>
        </div>
      </div>

      {/* 3 Grouped Settings Cards matching reference */}
      <div className="flex-1 overflow-y-auto space-y-2 z-10 pr-0.5 my-1.5">
        {/* Group 1: Account, Notifications, Appearance */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl border border-white/80 shadow-xs divide-y divide-slate-100 overflow-hidden">
          {/* Account */}
          <button 
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <User className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.account}</span>
            </div>
            <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </button>

          {/* Notifications */}
          <button 
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
                <Bell className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.notifications}</span>
            </div>
            <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </button>

          {/* Appearance (Theme switcher) */}
          <button 
            onClick={() => setActiveModal('appearance')}
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <Palette className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.appearance}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] font-semibold text-slate-400 capitalize">
                {theme === 'day' ? t.dayTheme : t.nightTheme}
              </span>
              <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
            </div>
          </button>
        </div>

        {/* Group 2: Language, Startup, Background Connection */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl border border-white/80 shadow-xs divide-y divide-slate-100 overflow-hidden">
          {/* Language */}
          <button 
            onClick={() => setActiveModal('language')}
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-cyan-50 text-cyan-600 flex items-center justify-center">
                <Globe className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.language}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-[10px] font-semibold text-indigo-700">
                {locale === 'ar' ? 'العربية' : 'English'}
              </span>
              <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
            </div>
          </button>

          {/* Startup */}
          <button 
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <Power className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.startup}</span>
            </div>
            <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </button>

          {/* Background Connection (Toggle Switch) */}
          <div className="w-full px-3.5 py-2.5 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-pink-50 text-pink-600 flex items-center justify-center">
                <LinkIcon className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.backgroundConnection}</span>
            </div>

            <button
              type="button"
              onClick={() => setBackgroundConnection(!backgroundConnection)}
              className={`relative w-11 h-6 rounded-full p-0.5 transition-colors duration-300 shrink-0 ${
                backgroundConnection ? 'bg-gradient-to-r from-indigo-500 to-purple-600' : 'bg-slate-300'
              }`}
              aria-label="Toggle Background Connection"
            >
              <div 
                className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${
                  backgroundConnection ? (locale === 'ar' ? '-translate-x-5' : 'translate-x-5') : 'translate-x-0'
                }`}
              />
            </button>
          </div>
        </div>

        {/* Group 3: Support, Privacy Policy, About */}
        <div className="rounded-2xl bg-white/60 backdrop-blur-xl border border-white/80 shadow-xs divide-y divide-slate-100 overflow-hidden">
          {/* Support */}
          <button 
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
                <Headphones className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.support}</span>
            </div>
            <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </button>

          {/* Privacy Policy */}
          <button 
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <span className="font-bold text-xs text-indigo-950">{t.privacyPolicy}</span>
            </div>
            <ChevronRight className={`w-4 h-4 text-slate-400 ${locale === 'ar' ? 'rotate-180' : ''}`} />
          </button>

          {/* About & Founder Identity */}
          <button 
            onClick={() => setCurrentScreen('founder-identity')}
            className="w-full px-3.5 py-2.5 flex items-center justify-between hover:bg-white/40 text-left transition-colors cursor-pointer group"
          >
            <div className="flex items-center gap-3">
              <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-cyan-400 to-indigo-600 text-white flex items-center justify-center shadow-xs font-mono font-bold text-[10px]">
                SE
              </div>
              <div>
                <span className="font-bold text-xs text-indigo-950 block">{t.about}</span>
                <span className="text-[10px] text-indigo-600 font-medium block">
                  {locale === 'ar' ? 'م. صادق الجزار · العقل الهندسي' : 'Eng. Sadek Elgazar · Founder'}
                </span>
              </div>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="text-[9px] font-mono font-bold px-1.5 py-0.5 rounded-full bg-indigo-100 text-indigo-800">
                KNOUX
              </span>
              <ChevronRight className={`w-4 h-4 text-slate-400 group-hover:translate-x-0.5 transition-transform ${locale === 'ar' ? 'rotate-180' : ''}`} />
            </div>
          </button>
        </div>
      </div>

      {/* Version Footer matching reference 10-profile-settings.png */}
      <div className="flex items-center justify-center gap-3 z-10 py-1">
        <div className="h-[1px] w-12 bg-indigo-300/40" />
        <span className="text-[11px] font-bold tracking-[0.2em] text-indigo-900/60 font-mono uppercase">
          KX VPN v1.0
        </span>
        <div className="h-[1px] w-12 bg-indigo-300/40" />
      </div>

      {/* Android Bottom Navigation Bar */}
      <AndroidBottomNav />


      {/* Language Modal */}
      {activeModal === 'language' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-xs rounded-3xl bg-white p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b pb-2">
              <h3 className="font-bold text-sm text-indigo-950">{t.language}</h3>
              <button onClick={() => setActiveModal(null)} className="text-slate-400 hover:text-slate-700">
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="space-y-2">
              <button
                onClick={() => { setLocale('en'); setActiveModal(null); }}
                className={`w-full p-3 rounded-2xl border text-left font-semibold text-xs flex items-center justify-between ${
                  locale === 'en' ? 'border-indigo-600 bg-indigo-50 text-indigo-950' : 'border-slate-200 text-slate-600'
                }`}
              >
                <span>English (LTR)</span>
                {locale === 'en' && <span className="text-indigo-600 font-bold">✓</span>}
              </button>

              <button
                onClick={() => { setLocale('ar'); setActiveModal(null); }}
                className={`w-full p-3 rounded-2xl border text-right font-semibold text-xs flex items-center justify-between ${
                  locale === 'ar' ? 'border-indigo-600 bg-indigo-50 text-indigo-950' : 'border-slate-200 text-slate-600'
                }`}
              >
                {locale === 'ar' && <span className="text-indigo-600 font-bold">✓</span>}
                <span>العربية (RTL)</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Appearance Modal */}
      {activeModal === 'appearance' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-xs rounded-3xl bg-white p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b pb-2">
              <h3 className="font-bold text-sm text-indigo-950">{t.appearance}</h3>
              <button onClick={() => setActiveModal(null)} className="text-slate-400 hover:text-slate-700">
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="space-y-2">
              <button
                onClick={() => { toggleTheme(); setActiveModal(null); }}
                className="w-full p-3 rounded-2xl border border-slate-200 hover:border-indigo-600 font-semibold text-xs flex items-center justify-between text-indigo-950"
              >
                <span>Toggle Day / Night Mode</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 uppercase font-mono">{theme}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* About Modal */}
      {activeModal === 'about' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="w-full max-w-xs rounded-3xl bg-white p-5 shadow-2xl space-y-3 text-center">
            <div className="w-12 h-12 rounded-full mx-auto p-1 bg-gradient-to-tr from-cyan-400 to-indigo-600">
              <img src="/assets/aistudio/00-brand-logo_c0b92dd8.png" alt="KX" className="w-full h-full object-contain" />
            </div>
            <h3 className="font-extrabold text-sm text-indigo-950">{t.fullAppName}</h3>
            <p className="text-xs text-slate-500">{t.tagline}</p>
            <div className="p-3 bg-slate-50 rounded-2xl text-[10px] text-slate-600 space-y-1 text-left">
              <p>• Architecture: Kotlin Jetpack Compose / React Desktop & Mobile</p>
              <p>• Visual Engine: KX Glass Orbit System</p>
              <p>• Protocol: WireGuard Certified State Engine</p>
            </div>
            <button
              onClick={() => setActiveModal(null)}
              className="w-full py-2 rounded-xl bg-indigo-600 text-white font-bold text-xs"
            >
              {t.done}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
