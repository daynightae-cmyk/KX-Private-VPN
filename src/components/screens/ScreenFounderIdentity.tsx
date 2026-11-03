import React, { useState } from 'react';
import { useVPN } from '../../context/VPNContext';
import { AndroidBottomNav } from '../common/AndroidBottomNav';
import { 
  ChevronLeft, 
  ExternalLink, 
  Copy, 
  Check, 
  Mail, 
  Phone, 
  MessageSquare, 
  Globe, 
  Github, 
  Facebook, 
  Video, 
  ShieldCheck, 
  Code2, 
  Cpu, 
  Sparkles, 
  Terminal, 
  Layers, 
  Smartphone, 
  Compass, 
  Award,
  CheckCircle2,
  Share2
} from 'lucide-react';

export const ScreenFounderIdentity: React.FC = () => {
  const { setCurrentScreen, locale, setLocale, theme, toggleTheme } = useVPN();
  const isAr = locale === 'ar';
  const isNight = theme === 'night';

  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'all' | 'direct' | 'founder' | 'company'>('all');

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2500);
    }).catch(() => {
      // Fallback
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2500);
    });
  };

  const openSafeUrl = (url: string) => {
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const expertiseGroups = [
    {
      id: 'software',
      title: 'Software Engineering',
      titleAr: 'هندسة البرمجيات والتطوير',
      icon: Code2,
      accent: 'from-cyan-400 to-blue-500',
      skills: ['React', 'TypeScript', 'JavaScript', 'Electron', 'Windows Desktop', 'Android Native']
    },
    {
      id: 'cloud',
      title: 'Cloud & Delivery',
      titleAr: 'السحابة والتكامل والنشر',
      icon: Layers,
      accent: 'from-blue-500 to-indigo-500',
      skills: ['Supabase', 'GitHub', 'Vercel', 'Git', 'API Integrations', 'CI/CD Pipelines']
    },
    {
      id: 'ai',
      title: 'AI & Automation',
      titleAr: 'الذكاء الاصطناعي والأتمتة',
      icon: Cpu,
      accent: 'from-indigo-500 to-purple-500',
      skills: ['Artificial Intelligence', 'AI Integrations', 'AI Automation', 'PowerShell', 'Intelligent Workflows']
    },
    {
      id: 'product',
      title: 'Product & Design',
      titleAr: 'تصميم وبناء المنتجات الرقمية',
      icon: Compass,
      accent: 'from-purple-500 to-pink-500',
      skills: ['UI/UX Craftsmanship', 'Product Architecture', 'Application Integration', 'Digital Product Development']
    }
  ];

  return (
    <div 
      dir={isAr ? 'rtl' : 'ltr'}
      className={`relative w-full h-full min-h-[720px] flex flex-col justify-between px-4 pt-3 pb-3 overflow-hidden transition-colors duration-300 ${
        isNight ? 'bg-[#060919] text-white' : 'bg-gradient-to-b from-[#e8edfc] via-[#f1f4fe] to-[#e4e9fa] text-slate-900'
      }`}
    >
      {/* Ambient Atmospheric Lighting */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className={`absolute top-[4%] left-[-15%] w-[130%] h-[50%] blur-3xl rounded-full ${
          isNight ? 'bg-gradient-to-br from-cyan-600/20 via-indigo-900/25 to-transparent' : 'bg-gradient-to-br from-indigo-300/30 via-cyan-200/20 to-transparent'
        }`} />
        <div className={`absolute bottom-[-10%] right-[-10%] w-[100%] h-[45%] blur-3xl rounded-full ${
          isNight ? 'bg-gradient-to-tl from-purple-700/20 via-blue-950/20 to-transparent' : 'bg-gradient-to-tl from-pink-200/30 via-purple-100/20 to-transparent'
        }`} />
      </div>

      {/* Header Bar */}
      <div className="flex items-center justify-between z-10 w-full pt-1 pb-1">
        <button 
          onClick={() => setCurrentScreen('10-profile-settings')}
          className={`w-9 h-9 rounded-full flex items-center justify-center active:scale-95 transition-all cursor-pointer ${
            isNight 
              ? 'bg-white/5 border border-white/10 hover:bg-white/10 text-indigo-200' 
              : 'bg-white/80 border border-slate-200/80 shadow-xs hover:bg-white text-slate-700'
          }`}
          aria-label={isAr ? 'رجوع للملف الشخصي' : 'Back to Settings'}
        >
          <ChevronLeft className={`w-5 h-5 ${isAr ? 'rotate-180' : ''}`} />
        </button>

        <div className="flex flex-col items-center">
          <div className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 animate-ping" />
            <h1 className="text-xs font-bold tracking-wider uppercase font-mono text-cyan-400">
              KNOUX IDENTITY CORE
            </h1>
          </div>
          <span className={`text-[10px] tracking-widest font-mono uppercase ${isNight ? 'text-slate-400' : 'text-slate-500'}`}>
            FOUNDER & CHIEF ARCHITECT
          </span>
        </div>

        {/* Quick Language Toggle inside screen */}
        <button
          onClick={() => setLocale(isAr ? 'en' : 'ar')}
          className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider font-mono uppercase transition-all cursor-pointer ${
            isNight 
              ? 'bg-white/10 hover:bg-white/15 text-cyan-300 border border-white/10' 
              : 'bg-white/90 hover:bg-white text-indigo-700 border border-indigo-100 shadow-xs'
          }`}
          aria-label="Toggle language"
        >
          {isAr ? 'EN' : 'عربي'}
        </button>
      </div>

      {/* Scrollable Content Engine */}
      <div className="z-10 flex-1 overflow-y-auto space-y-3.5 my-2 pr-0.5 custom-scrollbar">

        {/* 03 — LUXURY HERO SECTION */}
        <div className={`relative p-4 rounded-3xl backdrop-blur-2xl border transition-all overflow-hidden ${
          isNight 
            ? 'bg-gradient-to-br from-white/[0.08] via-white/[0.03] to-indigo-950/40 border-cyan-500/30 shadow-[0_12px_36px_rgba(0,0,0,0.45)]' 
            : 'bg-gradient-to-br from-white/90 via-white/70 to-indigo-50/80 border-white shadow-[0_10px_30px_rgba(99,102,241,0.12)]'
        }`}>
          {/* Subtle Corner Hologram */}
          <div className="absolute -top-12 -right-12 w-32 h-32 rounded-full bg-cyan-500/15 blur-2xl pointer-events-none" />

          <div className="flex items-start gap-3.5">
            {/* Architectural Monogram Orb */}
            <div className="relative shrink-0">
              <div className="w-16 h-16 rounded-2xl p-[1.5px] bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 shadow-[0_0_20px_rgba(6,182,212,0.35)]">
                <div className={`w-full h-full rounded-[14px] flex flex-col items-center justify-center relative overflow-hidden ${
                  isNight ? 'bg-[#0d1233]' : 'bg-[#12163a]'
                }`}>
                  <span className="font-extrabold text-lg text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 via-white to-purple-300 tracking-tighter font-mono">
                    SE
                  </span>
                  <div className="w-4 h-[1.5px] bg-gradient-to-r from-cyan-400 to-purple-400 mt-0.5 rounded-full" />
                </div>
              </div>
              <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-cyan-500 border-2 border-[#060919] flex items-center justify-center">
                <ShieldCheck className="w-3 h-3 text-white" />
              </div>
            </div>

            {/* Names & Titles */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-mono font-bold tracking-widest text-cyan-400 uppercase">
                  KNOUX FOUNDER
                </span>
                <span className="text-slate-400 text-xs">•</span>
                <span className={`text-[10px] font-mono ${isNight ? 'text-slate-400' : 'text-slate-500'}`}>
                  EST. 2026
                </span>
              </div>

              <h2 className="text-lg font-black tracking-tight leading-tight mt-0.5">
                {isAr ? 'م. صادق الجزار' : 'Eng. Sadek Elgazar'}
              </h2>
              
              <p className={`text-xs font-semibold mt-0.5 ${isNight ? 'text-indigo-200' : 'text-indigo-900'}`}>
                {isAr ? 'Eng. Sadek Elgazar' : 'م. صادق الجزار'}
              </p>

              {/* Roles Flow */}
              <div className={`text-[11px] leading-relaxed mt-2 pt-2 border-t font-medium ${
                isNight ? 'border-white/10 text-slate-300' : 'border-slate-200/80 text-slate-700'
              }`}>
                <span>{isAr ? 'مؤسس' : 'Founder'}</span>
                <span className="mx-1.5 opacity-40">·</span>
                <span>{isAr ? 'مهندس برمجيات' : 'Software Engineer'}</span>
                <span className="mx-1.5 opacity-40">·</span>
                <span>{isAr ? 'مطور منتجات رقمية' : 'Digital Product Builder'}</span>
                <span className="mx-1.5 opacity-40">·</span>
                <span>{isAr ? 'مهندس أتمتة وذكاء اصطناعي' : 'AI Automation Engineer'}</span>
              </div>
            </div>
          </div>

          {/* Ethos Quote */}
          <div className={`mt-3.5 p-3 rounded-2xl border text-xs leading-relaxed italic ${
            isNight 
              ? 'bg-black/30 border-white/5 text-slate-300' 
              : 'bg-white/80 border-indigo-100 text-slate-700 shadow-xs'
          }`}>
            {isAr
              ? '«هندسة برمجيات استثنائية، تجمع بين الدقة المعمارية الصارمة، الشفافية المطلقة، والجمال البصري الزجاجي المبتكر دون أي تزييف أو مبالغات.»'
              : '"Architecting uncompromising software experiences with mathematical precision, zero placebo, and radiant modern craft."'}
          </div>

          {/* Quick Primary Actions */}
          <div className="grid grid-cols-2 gap-2 mt-3 pt-3 border-t border-white/10">
            <button
              onClick={() => openSafeUrl('https://wa.me/971503281920')}
              className="py-2 px-3 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs flex items-center justify-center gap-1.5 shadow-[0_4px_12px_rgba(16,185,129,0.3)] active:scale-98 transition-all cursor-pointer"
            >
              <MessageSquare className="w-3.5 h-3.5 fill-current" />
              <span>{isAr ? 'واتساب مباشر' : 'WhatsApp Direct'}</span>
            </button>

            <button
              onClick={() => copyToClipboard('admin@knoux.store', 'email-quick')}
              className={`py-2 px-3 rounded-xl border font-bold text-xs flex items-center justify-center gap-1.5 active:scale-98 transition-all cursor-pointer ${
                copiedKey === 'email-quick' 
                  ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300' 
                  : isNight 
                    ? 'bg-white/10 hover:bg-white/15 border-white/15 text-white' 
                    : 'bg-white hover:bg-slate-50 border-slate-200 text-slate-800 shadow-xs'
              }`}
            >
              {copiedKey === 'email-quick' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Mail className="w-3.5 h-3.5 text-cyan-400" />}
              <span>{copiedKey === 'email-quick' ? (isAr ? 'تم نسخ البريد!' : 'Email Copied!') : (isAr ? 'نسخ البريد الرسمي' : 'Copy Email')}</span>
            </button>
          </div>
        </div>

        {/* 04 — ARCHITECTURAL PHILOSOPHY & PILLARS */}
        <div className={`p-3.5 rounded-2xl border ${
          isNight 
            ? 'bg-white/[0.04] border-white/10 backdrop-blur-md' 
            : 'bg-white/80 border-slate-200 shadow-xs'
        }`}>
          <div className="flex items-center gap-2 mb-2">
            <Award className="w-4 h-4 text-cyan-400" />
            <h3 className="text-xs font-bold uppercase tracking-wider">
              {isAr ? 'المبادئ الهندسية الأساسية' : 'Core Engineering Pillars'}
            </h3>
          </div>

          <div className="grid grid-cols-2 gap-2 text-left">
            <div className={`p-2.5 rounded-xl border ${isNight ? 'bg-black/20 border-white/5' : 'bg-slate-50/80 border-slate-100'}`}>
              <div className="flex items-center gap-1.5 text-cyan-400 mb-1">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span className="text-[11px] font-bold text-white">
                  {isAr ? 'صفر تزييف' : 'Zero Placebo'}
                </span>
              </div>
              <p className={`text-[10px] leading-relaxed ${isNight ? 'text-slate-400' : 'text-slate-600'}`}>
                {isAr ? 'قياسات حقيقية بدون أرقام وهمية أو شاشات تجميلية خادعة.' : 'Strictly authentic OS telemetry and measurable byte operations.'}
              </p>
            </div>

            <div className={`p-2.5 rounded-xl border ${isNight ? 'bg-black/20 border-white/5' : 'bg-slate-50/80 border-slate-100'}`}>
              <div className="flex items-center gap-1.5 text-purple-400 mb-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span className="text-[11px] font-bold text-white">
                  {isAr ? 'الخصوصية المعمارية' : 'Privacy-First'}
                </span>
              </div>
              <p className={`text-[10px] leading-relaxed ${isNight ? 'text-slate-400' : 'text-slate-600'}`}>
                {isAr ? 'عزل كامل للمفاتيح في الذاكرة بدون كتابة على القرص.' : 'In-RAM isolated execution with strict zero-logging policies.'}
              </p>
            </div>
          </div>
        </div>

        {/* 05 — EDITORIAL CAPABILITY MATRIX */}
        <div className="space-y-2">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-1.5">
              <Terminal className="w-3.5 h-3.5 text-cyan-400" />
              <span className="text-xs font-bold tracking-wider uppercase">
                {isAr ? 'مصفوفة الخبرات الهندسية' : 'Engineering Expertise Experience'}
              </span>
            </div>
            <span className={`text-[10px] font-mono ${isNight ? 'text-slate-400' : 'text-slate-500'}`}>
              4 CORE DOMAINS
            </span>
          </div>

          <div className="space-y-2">
            {expertiseGroups.map(group => {
              const Icon = group.icon;
              return (
                <div 
                  key={group.id}
                  className={`p-3 rounded-2xl border transition-all ${
                    isNight 
                      ? 'bg-white/[0.04] border-white/10 hover:border-cyan-500/40' 
                      : 'bg-white/80 border-slate-200/90 shadow-xs hover:border-indigo-400'
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <div className={`w-7 h-7 rounded-xl bg-gradient-to-tr ${group.accent} flex items-center justify-center text-white shadow-xs`}>
                        <Icon className="w-3.5 h-3.5" />
                      </div>
                      <div>
                        <h4 className="text-xs font-bold leading-none">
                          {isAr ? group.titleAr : group.title}
                        </h4>
                        <span className={`text-[9px] font-mono mt-0.5 block ${isNight ? 'text-slate-400' : 'text-slate-500'}`}>
                          {group.skills.length} Capabilities
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-1.5 pt-1 border-t border-white/5">
                    {group.skills.map(skill => (
                      <span 
                        key={skill}
                        className={`text-[10px] font-mono px-2 py-0.5 rounded-lg border transition-colors ${
                          isNight 
                            ? 'bg-black/30 border-white/10 text-slate-200 hover:border-cyan-400/50 hover:text-cyan-300' 
                            : 'bg-slate-100/80 border-slate-200 text-slate-700 hover:border-indigo-400 hover:text-indigo-900'
                        }`}
                      >
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 06 — CONTACT & DIGITAL PRESENCE (Fully Functional & Separated) */}
        <div className="space-y-2.5">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-1.5">
              <Globe className="w-3.5 h-3.5 text-cyan-400" />
              <span className="text-xs font-bold tracking-wider uppercase">
                {isAr ? 'قنوات التواصل والتواجد الرقمي' : 'Contact & Digital Presence'}
              </span>
            </div>

            {/* Filter Tabs */}
            <div className={`flex items-center p-0.5 rounded-lg border text-[9px] font-mono font-bold ${
              isNight ? 'bg-white/5 border-white/10' : 'bg-slate-200/70 border-slate-300/60'
            }`}>
              <button
                onClick={() => setActiveTab('all')}
                className={`px-2 py-0.5 rounded-md transition-colors cursor-pointer ${
                  activeTab === 'all' ? 'bg-cyan-500 text-black' : isNight ? 'text-slate-400' : 'text-slate-700'
                }`}
              >
                {isAr ? 'الكل' : 'ALL'}
              </button>
              <button
                onClick={() => setActiveTab('founder')}
                className={`px-2 py-0.5 rounded-md transition-colors cursor-pointer ${
                  activeTab === 'founder' ? 'bg-cyan-500 text-black' : isNight ? 'text-slate-400' : 'text-slate-700'
                }`}
              >
                {isAr ? 'المؤسس' : 'FOUNDER'}
              </button>
              <button
                onClick={() => setActiveTab('company')}
                className={`px-2 py-0.5 rounded-md transition-colors cursor-pointer ${
                  activeTab === 'company' ? 'bg-cyan-500 text-black' : isNight ? 'text-slate-400' : 'text-slate-700'
                }`}
              >
                KNOUX
              </button>
            </div>
          </div>

          {/* Cards Register */}
          <div className="space-y-2">
            {/* 1. Official Website */}
            {(activeTab === 'all' || activeTab === 'company') && (
              <div className={`p-3 rounded-2xl border flex items-center justify-between gap-3 ${
                isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
              }`}>
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 flex items-center justify-center shrink-0">
                    <Globe className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-xs font-bold">
                        {isAr ? 'الموقع الرسمي للعلامة' : 'Official KNOUX Store'}
                      </h4>
                      <span className="text-[9px] px-1.5 rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                        STORE
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono block">
                      https://knoux.store/
                    </span>
                  </div>
                </div>

                <button
                  onClick={() => openSafeUrl('https://knoux.store/')}
                  className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold text-xs flex items-center gap-1 shadow-xs active:scale-95 transition-all cursor-pointer"
                  aria-label="Visit knoux.store"
                >
                  <span>{isAr ? 'زيارة' : 'Visit'}</span>
                  <ExternalLink className="w-3 h-3" />
                </button>
              </div>
            )}

            {/* 2. Primary Email */}
            {(activeTab === 'all' || activeTab === 'direct') && (
              <div className={`p-3 rounded-2xl border flex items-center justify-between gap-3 ${
                isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
              }`}>
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center shrink-0">
                    <Mail className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-xs font-bold">
                        {isAr ? 'البريد الإلكتروني المعتمد' : 'Verified Email Address'}
                      </h4>
                      <span className="text-[9px] px-1.5 rounded-full bg-indigo-500/20 text-indigo-300 font-mono">
                        OFFICIAL
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono block">
                      admin@knoux.store
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => copyToClipboard('admin@knoux.store', 'email-main')}
                    className={`p-2 rounded-xl border text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer ${
                      copiedKey === 'email-main' 
                        ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300' 
                        : isNight 
                          ? 'bg-white/5 hover:bg-white/10 border-white/10 text-slate-200' 
                          : 'bg-slate-100 hover:bg-slate-200 border-slate-200 text-slate-700'
                    }`}
                    title={isAr ? 'نسخ البريد' : 'Copy Email'}
                  >
                    {copiedKey === 'email-main' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                  <button
                    onClick={() => window.open('mailto:admin@knoux.store')}
                    className="p-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-all cursor-pointer"
                    title={isAr ? 'إرسال بريد' : 'Send Mail'}
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}

            {/* 3. International & Local Phone */}
            {(activeTab === 'all' || activeTab === 'direct') && (
              <div className={`p-3 rounded-2xl border flex items-center justify-between gap-3 ${
                isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
              }`}>
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center shrink-0">
                    <Phone className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="flex items-center gap-1.5">
                      <h4 className="text-xs font-bold">
                        {isAr ? 'الهاتف المباشر والدولي' : 'Direct & International Phone'}
                      </h4>
                      <span className="text-[9px] px-1.5 rounded-full bg-emerald-500/20 text-emerald-300 font-mono">
                        UAE +971
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono block">
                      +971 50 328 1920 · 050 328 1920
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => copyToClipboard('+971503281920', 'phone-main')}
                    className={`p-2 rounded-xl border text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer ${
                      copiedKey === 'phone-main' 
                        ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300' 
                        : isNight 
                          ? 'bg-white/5 hover:bg-white/10 border-white/10 text-slate-200' 
                          : 'bg-slate-100 hover:bg-slate-200 border-slate-200 text-slate-700'
                    }`}
                    title={isAr ? 'نسخ الهاتف' : 'Copy Phone'}
                  >
                    {copiedKey === 'phone-main' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  </button>
                  <button
                    onClick={() => openSafeUrl('https://wa.me/971503281920')}
                    className="p-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white transition-all cursor-pointer"
                    title={isAr ? 'محادثة واتساب' : 'WhatsApp Chat'}
                  >
                    <MessageSquare className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}

            {/* 4. Founder Personal Channels */}
            {(activeTab === 'all' || activeTab === 'founder') && (
              <div className="grid grid-cols-2 gap-2">
                {/* Founder GitHub */}
                <button
                  onClick={() => openSafeUrl('https://github.com/daynightae-cmyk')}
                  className={`p-3 rounded-2xl border text-left flex items-center gap-2.5 hover:border-cyan-400/60 transition-all cursor-pointer ${
                    isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
                  }`}
                >
                  <div className="w-7 h-7 rounded-lg bg-black/40 text-white flex items-center justify-center shrink-0 border border-white/10">
                    <Github className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <span className="text-[10px] text-cyan-400 font-mono block">FOUNDER GITHUB</span>
                    <h5 className="text-xs font-bold truncate">@daynightae-cmyk</h5>
                  </div>
                </button>

                {/* Founder Facebook */}
                <button
                  onClick={() => openSafeUrl('https://www.facebook.com/CopSadek')}
                  className={`p-3 rounded-2xl border text-left flex items-center gap-2.5 hover:border-blue-400/60 transition-all cursor-pointer ${
                    isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
                  }`}
                >
                  <div className="w-7 h-7 rounded-lg bg-blue-600/30 text-blue-400 flex items-center justify-center shrink-0 border border-blue-500/30">
                    <Facebook className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <span className="text-[10px] text-blue-400 font-mono block">FOUNDER FB</span>
                    <h5 className="text-xs font-bold truncate">CopSadek</h5>
                  </div>
                </button>
              </div>
            )}

            {/* 5. KNOUX Organization Channels */}
            {(activeTab === 'all' || activeTab === 'company') && (
              <div className="grid grid-cols-3 gap-2">
                {/* Org GitHub */}
                <button
                  onClick={() => openSafeUrl('https://github.com/knoux-io')}
                  className={`p-2.5 rounded-2xl border text-center flex flex-col items-center gap-1 hover:border-cyan-400/60 transition-all cursor-pointer ${
                    isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
                  }`}
                >
                  <Github className="w-4 h-4 text-cyan-400" />
                  <span className="text-[10px] font-bold font-mono">knoux-io</span>
                  <span className="text-[9px] text-slate-400">GitHub Org</span>
                </button>

                {/* Org Facebook */}
                <button
                  onClick={() => openSafeUrl('https://www.facebook.com/knouxStore/')}
                  className={`p-2.5 rounded-2xl border text-center flex flex-col items-center gap-1 hover:border-blue-400/60 transition-all cursor-pointer ${
                    isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
                  }`}
                >
                  <Facebook className="w-4 h-4 text-blue-400" />
                  <span className="text-[10px] font-bold font-mono">knouxStore</span>
                  <span className="text-[9px] text-slate-400">Facebook Page</span>
                </button>

                {/* Org TikTok */}
                <button
                  onClick={() => openSafeUrl('https://www.tiktok.com/@knoux_7')}
                  className={`p-2.5 rounded-2xl border text-center flex flex-col items-center gap-1 hover:border-pink-400/60 transition-all cursor-pointer ${
                    isNight ? 'bg-white/[0.04] border-white/10' : 'bg-white/80 border-slate-200 shadow-xs'
                  }`}
                >
                  <Video className="w-4 h-4 text-pink-400" />
                  <span className="text-[10px] font-bold font-mono">@knoux_7</span>
                  <span className="text-[9px] text-slate-400">TikTok Lab</span>
                </button>
              </div>
            )}
          </div>
        </div>

        {/* 07 — SIGNATURE EXPERIENCE SECTION */}
        <div className={`p-4 rounded-3xl border text-center space-y-3 relative overflow-hidden ${
          isNight 
            ? 'bg-gradient-to-b from-[#0f1438]/80 to-[#070b22] border-cyan-500/30' 
            : 'bg-gradient-to-b from-white/90 to-indigo-50 border-indigo-100 shadow-xs'
        }`}>
          {/* Subtle concentric seal backplate */}
          <div className="absolute inset-0 flex items-center justify-center opacity-10 pointer-events-none">
            <div className="w-48 h-48 rounded-full border border-cyan-400" />
            <div className="w-36 h-36 rounded-full border border-indigo-400" />
          </div>

          <div className="relative z-10 space-y-2">
            <div className="w-10 h-10 mx-auto rounded-full p-[1px] bg-gradient-to-tr from-cyan-400 to-indigo-600 shadow-md">
              <img 
                src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
                alt="KNOUX Mark" 
                className="w-full h-full object-contain rounded-full"
              />
            </div>

            <div>
              <span className="text-[10px] font-mono tracking-widest text-cyan-400 uppercase font-bold block">
                ENGINEERED WITH PASSION BY
              </span>
              <h3 className="text-base font-black tracking-tight mt-0.5">
                {isAr ? 'م. صادق الجزار' : 'Eng. Sadek Elgazar'}
              </h3>
              <p className={`text-xs ${isNight ? 'text-slate-400' : 'text-slate-600'} font-medium`}>
                Founder & Chief Architect of KNOUX
              </p>
            </div>

            {/* Stylized SVG Calligraphic Signature Seal */}
            <div className="py-1 flex justify-center items-center">
              <svg className="w-44 h-10 text-cyan-400/90" viewBox="0 0 200 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M10 28C35 12 50 8 75 18C95 26 115 32 135 15C155 0 170 12 190 20" stroke="url(#paint0_linear)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M45 32C65 22 105 18 160 25" stroke="url(#paint0_linear)" strokeWidth="1.5" strokeLinecap="round" strokeDasharray="3 3" />
                <defs>
                  <linearGradient id="paint0_linear" x1="10" y1="20" x2="190" y2="20" gradientUnits="userSpaceOnUse">
                    <stop stopColor="#22d3ee" />
                    <stop offset="0.5" stopColor="#818cf8" />
                    <stop offset="1" stopColor="#c084fc" />
                  </linearGradient>
                </defs>
              </svg>
            </div>

            <p className={`text-[10px] leading-relaxed max-w-xs mx-auto ${isNight ? 'text-slate-400' : 'text-slate-500'}`}>
              {isAr
                ? '«كافة الأنظمة والمكونات مبنية بفلسفة احترام المستخدم، الأمان الحقيقي غير القابل للمساومة، والهندسة النظيفة.»'
                : 'Crafted on the conviction that software should be transparent, uncompromising, and exceptionally beautiful.'}
            </p>
          </div>
        </div>

      </div>

      {/* Footer Return Bar & Android Bottom Nav */}
      <div className="z-10 pt-1 space-y-1.5">
        <div className={`pt-1 border-t flex gap-2 ${isNight ? 'border-white/10' : 'border-slate-200'}`}>
          <button
            onClick={() => setCurrentScreen('10-profile-settings')}
            className="w-1/2 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-white font-medium text-xs active:scale-[0.99] transition-all cursor-pointer"
          >
            {isAr ? 'الإعدادات' : 'Profile Settings'}
          </button>

          <button
            onClick={() => openSafeUrl('https://knoux.store/')}
            className="w-1/2 py-2 rounded-xl bg-gradient-to-r from-cyan-500 via-indigo-600 to-purple-600 hover:from-cyan-400 hover:to-purple-500 text-white font-bold text-xs shadow-[0_4px_16px_rgba(6,182,212,0.3)] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <span>knoux.store</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>

        <AndroidBottomNav />
      </div>
    </div>
  );
};
