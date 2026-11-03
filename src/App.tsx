import React from 'react';
import { VPNProvider, useVPN } from './context/VPNContext';
import { DeviceFrame, referenceImages } from './components/common/DeviceFrame';
import { Screen00Splash } from './components/screens/Screen00Splash';
import { Screen01Welcome } from './components/screens/Screen01Welcome';
import { Screen02HomeDisconnected } from './components/screens/Screen02HomeDisconnected';
import { Screen03ServerSelection } from './components/screens/Screen03ServerSelection';
import { Screen04ConnectedSession } from './components/screens/Screen04ConnectedSession';
import { Screen05SecurityCenter } from './components/screens/Screen05SecurityCenter';
import { Screen06SplitTunneling } from './components/screens/Screen06SplitTunneling';
import { Screen07AdvancedNetwork } from './components/screens/Screen07AdvancedNetwork';
import { Screen08Statistics } from './components/screens/Screen08Statistics';
import { Screen09ImportConfig } from './components/screens/Screen09ImportConfig';
import { Screen10ProfileSettings } from './components/screens/Screen10ProfileSettings';
import { Screen11RepairLab } from './components/screens/repair/Screen11RepairLab';
import { Screen12StorageXRay } from './components/screens/repair/Screen12StorageXRay';
import { Screen13GhostHunter } from './components/screens/repair/Screen13GhostHunter';
import { Screen14DuplicateDNA } from './components/screens/repair/Screen14DuplicateDNA';
import { Screen15HeavyweightFinder } from './components/screens/repair/Screen15HeavyweightFinder';
import { Screen16CacheSurgeon } from './components/screens/repair/Screen16CacheSurgeon';
import { Screen17MediaSlim } from './components/screens/repair/Screen17MediaSlim';
import { Screen18DownloadRescue } from './components/screens/repair/Screen18DownloadRescue';
import { Screen19SessionShield } from './components/screens/repair/Screen19SessionShield';
import { Screen20NetworkDoctor } from './components/screens/repair/Screen20NetworkDoctor';
import { Screen21RepairAutopilot } from './components/screens/repair/Screen21RepairAutopilot';
import { ScreenFounderIdentity } from './components/screens/ScreenFounderIdentity';
import { AuditLedgerModal } from './components/audit/AuditLedgerModal';
import { ScreenId } from './types/vpn';
import { 
  Smartphone, 
  Columns, 
  Sun, 
  Moon, 
  Languages, 
  ShieldCheck, 
  Eye, 
  Sparkles,
  SlidersHorizontal,
  ChevronDown
} from 'lucide-react';

const screenList: { id: ScreenId; label: string; labelAr: string; task: string }[] = [
  { id: 'founder-identity', label: 'Founder: Eng. Sadek Elgazar', labelAr: 'المؤسس: م. صادق الجزار', task: 'FOUNDER' },
  { id: '00-splash', label: '00. Splash & Brand', labelAr: '00. الشعار وبوابة الإطلاق', task: 'TASK_00' },
  { id: '01-welcome', label: '01. Welcome Onboarding', labelAr: '01. شاشة الترحيب', task: 'TASK_01' },
  { id: '02-home-disconnected', label: '02. Home Disconnected', labelAr: '02. الرئيسية (غير متصل)', task: 'TASK_02' },
  { id: '03-server-selection', label: '03. Server Selection', labelAr: '03. اختيار الخوادم', task: 'TASK_03' },
  { id: '04-connected-session', label: '04. Connected Session', labelAr: '04. جلسة الاتصال الحية', task: 'TASK_04' },
  { id: '05-security-center', label: '05. Security Center', labelAr: '05. مركز الأمان', task: 'TASK_05' },
  { id: '06-split-tunneling', label: '06. Split Tunneling', labelAr: '06. النفق المجزأ', task: 'TASK_06' },
  { id: '07-advanced-network', label: '07. Advanced Network', labelAr: '07. الشبكة المتقدمة', task: 'TASK_07' },
  { id: '08-statistics', label: '08. Statistics & Charts', labelAr: '08. الإحصائيات والمخططات', task: 'TASK_08' },
  { id: '09-import-configuration', label: '09. Import Config', labelAr: '09. استيراد التكوين', task: 'TASK_09' },
  { id: '10-profile-settings', label: '10. Profile & Settings', labelAr: '10. الملف الشخصي والإعدادات', task: 'TASK_10' },
  { id: '11-repair-lab', label: '11. Phone Repair Lab™', labelAr: '11. مختبر الصيانة والخصوصية', task: 'TASK_11' },
  { id: '12-storage-xray', label: '12. Storage X-Ray™', labelAr: '12. مسح الذاكرة الشامل', task: 'TASK_12' },
  { id: '13-ghost-hunter', label: '13. Ghost Hunter™', labelAr: '13. صائد الملفات الشبحية', task: 'TASK_13' },
  { id: '14-duplicate-dna', label: '14. Duplicate DNA™', labelAr: '14. كاشف التكرار الدقيق', task: 'TASK_14' },
  { id: '15-heavyweight-finder', label: '15. Heavyweight Finder™', labelAr: '15. صائد الملفات الضخمة', task: 'TASK_15' },
  { id: '16-cache-surgeon', label: '16. Cache Surgeon™', labelAr: '16. جراح الذاكرة المخبأة', task: 'TASK_16' },
  { id: '17-media-slim', label: '17. Media Slim™', labelAr: '17. ضاغط الوسائط الذكي', task: 'TASK_17' },
  { id: '18-download-rescue', label: '18. Download Rescue™', labelAr: '18. منقذ التنزيلات التالفة', task: 'TASK_18' },
  { id: '19-session-shield', label: '19. Session Shield™', labelAr: '19. درع الجلسات والخصوصية', task: 'TASK_19' },
  { id: '20-network-doctor', label: '20. Network Doctor™', labelAr: '20. طبيب الشبكة والمسارات', task: 'TASK_20' },
  { id: '21-repair-autopilot', label: '21. Repair Autopilot™', labelAr: '21. الطيار الآلي للصيانة', task: 'TASK_21' },
];

const MainWorkspace: React.FC = () => {
  const { 
    currentScreen, 
    setCurrentScreen, 
    viewMode, 
    setViewMode, 
    compareOpacity, 
    setCompareOpacity,
    deviceViewport, 
    setDeviceViewport,
    theme, 
    toggleTheme, 
    locale, 
    setLocale, 
    setIsAuditOpen,
    t 
  } = useVPN();

  const renderActiveScreen = () => {
    switch (currentScreen) {
      case '00-splash':
        return <Screen00Splash />;
      case '01-welcome':
        return <Screen01Welcome />;
      case '02-home-disconnected':
        return <Screen02HomeDisconnected />;
      case '03-server-selection':
        return <Screen03ServerSelection />;
      case '04-connected-session':
        return <Screen04ConnectedSession />;
      case '05-security-center':
        return <Screen05SecurityCenter />;
      case '06-split-tunneling':
        return <Screen06SplitTunneling />;
      case '07-advanced-network':
        return <Screen07AdvancedNetwork />;
      case '08-statistics':
        return <Screen08Statistics />;
      case '09-import-configuration':
        return <Screen09ImportConfig />;
      case '10-profile-settings':
        return <Screen10ProfileSettings />;
      case '11-repair-lab':
        return <Screen11RepairLab />;
      case '12-storage-xray':
        return <Screen12StorageXRay />;
      case '13-ghost-hunter':
        return <Screen13GhostHunter />;
      case '14-duplicate-dna':
        return <Screen14DuplicateDNA />;
      case '15-heavyweight-finder':
        return <Screen15HeavyweightFinder />;
      case '16-cache-surgeon':
        return <Screen16CacheSurgeon />;
      case '17-media-slim':
        return <Screen17MediaSlim />;
      case '18-download-rescue':
        return <Screen18DownloadRescue />;
      case '19-session-shield':
        return <Screen19SessionShield />;
      case '20-network-doctor':
        return <Screen20NetworkDoctor />;
      case '21-repair-autopilot':
        return <Screen21RepairAutopilot />;
      case 'founder-identity':
        return <ScreenFounderIdentity />;
      default:
        return <Screen02HomeDisconnected />;
    }
  };

  const currentRefImage = referenceImages[currentScreen];

  return (
    <div className="min-h-screen w-full flex flex-col bg-[#07091c] text-white selection:bg-indigo-500 selection:text-white">
      {/* Top Bar Contract (3-Zone Clean Header) */}
      <header className="sticky top-0 z-40 w-full px-4 md:px-8 py-3 bg-[#0d102e]/90 backdrop-blur-xl border-b border-white/10 flex items-center justify-between gap-4">
        {/* Zone 1: Brand Wordmark */}
        <div className="flex items-center gap-3 shrink-0">
          <div className="relative w-8 h-8 rounded-full bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-600 p-[1px] shadow-[0_0_15px_rgba(99,102,241,0.5)]">
            <img 
              src="/assets/aistudio/00-brand-logo_c0b92dd8.png" 
              alt="KX Logo"
              className="w-full h-full object-contain rounded-full"
              referrerPolicy="no-referrer"
            />
          </div>
          <div className="flex flex-col">
            <span className="kx-brand-title text-base md:text-lg tracking-[0.2em] whitespace-nowrap">
              KX PRIVATE VPN
            </span>
            <span className="text-[10px] font-mono tracking-wider text-cyan-300/90 -mt-0.5 hidden sm:block">
              {locale === 'ar' ? 'نظام الزجاج المداري · 11 شاشة موثقة' : 'KX GLASS ORBIT · 11 APPROVED WORKSPACES'}
            </span>
          </div>
        </div>

        {/* Zone 2: Navigation & View Controls */}
        <div className="hidden lg:flex items-center gap-3">
          {/* Quick Screen Selector Dropdown */}
          <div className="relative flex items-center">
            <select
              value={currentScreen}
              onChange={(e) => setCurrentScreen(e.target.value as ScreenId)}
              className="appearance-none h-9 pl-3 pr-8 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-xs font-semibold text-white cursor-pointer outline-none transition-colors"
            >
              {screenList.map((screen) => (
                <option key={screen.id} value={screen.id} className="bg-[#12163a] text-white">
                  {locale === 'ar' ? screen.labelAr : screen.label}
                </option>
              ))}
            </select>
            <ChevronDown className="w-3.5 h-3.5 text-slate-300 absolute right-2.5 pointer-events-none" />
          </div>

          {/* View Mode Tabs: Device / Side-by-Side Compare */}
          <div className="flex items-center bg-white/10 p-0.5 rounded-xl border border-white/10 text-xs">
            <button
              onClick={() => setViewMode('device')}
              className={`px-3 py-1.5 rounded-lg font-semibold flex items-center gap-1.5 transition-all ${
                viewMode === 'device' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-xs' : 'text-slate-300 hover:text-white'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>{locale === 'ar' ? 'عرض الجهاز' : 'Mobile View'}</span>
            </button>

            <button
              onClick={() => setViewMode('compare')}
              className={`px-3 py-1.5 rounded-lg font-semibold flex items-center gap-1.5 transition-all ${
                viewMode === 'compare' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-xs' : 'text-slate-300 hover:text-white'
              }`}
            >
              <Columns className="w-3.5 h-3.5" />
              <span>{locale === 'ar' ? 'مقارنة المرجع' : 'Side-by-Side Reference'}</span>
            </button>
          </div>

          {/* Device Width Selector (360dp vs 411dp) */}
          {viewMode === 'device' && (
            <div className="flex items-center bg-white/5 p-0.5 rounded-xl border border-white/10 text-[11px] font-mono">
              <button
                onClick={() => setDeviceViewport('360')}
                className={`px-2 py-1 rounded-lg ${deviceViewport === '360' ? 'bg-white/20 text-cyan-300' : 'text-slate-400'}`}
              >
                360dp
              </button>
              <button
                onClick={() => setDeviceViewport('411')}
                className={`px-2 py-1 rounded-lg ${deviceViewport === '411' ? 'bg-white/20 text-cyan-300' : 'text-slate-400'}`}
              >
                411dp
              </button>
            </div>
          )}

          {/* Theme Toggle (Day / Night) */}
          <button
            onClick={toggleTheme}
            className="h-9 px-3 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-xs font-semibold flex items-center gap-1.5 text-indigo-200 transition-all"
            aria-label="Toggle Theme"
          >
            {theme === 'night' ? <Moon className="w-3.5 h-3.5 text-cyan-300" /> : <Sun className="w-3.5 h-3.5 text-yellow-300" />}
            <span className="capitalize">{theme === 'night' ? t.nightTheme : t.dayTheme}</span>
          </button>

          {/* Language Toggle (EN / AR RTL) */}
          <button
            onClick={() => setLocale(locale === 'en' ? 'ar' : 'en')}
            className="h-9 px-3 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-xs font-semibold flex items-center gap-1.5 text-indigo-200 transition-all"
            aria-label="Toggle Language"
          >
            <Languages className="w-3.5 h-3.5 text-purple-300" />
            <span>{locale === 'en' ? 'العربية' : 'English'}</span>
          </button>
        </div>

        {/* Zone 3: Primary Action: Audit Ledger Modal Trigger */}
        <div className="flex items-center gap-2.5 shrink-0">
          {/* Mobile Screen Selector Trigger */}
          <div className="lg:hidden">
            <select
              value={currentScreen}
              onChange={(e) => setCurrentScreen(e.target.value as ScreenId)}
              className="h-9 px-2 rounded-xl bg-white/10 border border-white/20 text-xs font-bold text-white outline-none"
            >
              {screenList.map((screen) => (
                <option key={screen.id} value={screen.id} className="bg-[#12163a] text-white">
                  {screen.id.slice(0, 2)} · {screen.label.split('.')[1]}
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={() => setCurrentScreen('founder-identity')}
            className={`h-9 px-3 rounded-xl border flex items-center gap-2 text-xs font-bold transition-all active:scale-95 whitespace-nowrap cursor-pointer ${
              currentScreen === 'founder-identity'
                ? 'bg-gradient-to-r from-cyan-500/30 via-indigo-500/30 to-purple-500/30 border-cyan-400 text-cyan-300 shadow-[0_0_15px_rgba(6,182,212,0.4)]'
                : 'bg-white/10 hover:bg-white/15 border-white/15 text-indigo-200'
            }`}
            title={locale === 'ar' ? 'هوية المؤسس والعقل الهندسي' : 'Founder & Chief Architect Identity'}
          >
            <div className="w-5 h-5 rounded-full bg-gradient-to-tr from-cyan-400 to-indigo-500 flex items-center justify-center text-[10px] text-white font-mono font-bold">
              SE
            </div>
            <span className="hidden md:inline font-medium">
              {locale === 'ar' ? 'م. صادق الجزار' : 'Eng. Sadek Elgazar'}
            </span>
            <span className="text-[9px] px-1.5 py-0.2 rounded-full bg-cyan-400/20 text-cyan-300 font-mono hidden xl:inline">
              FOUNDER
            </span>
          </button>

          <button
            onClick={() => setIsAuditOpen(true)}
            className="h-9 px-3.5 rounded-xl bg-gradient-to-r from-emerald-500/20 to-teal-500/20 hover:from-emerald-500/30 hover:to-teal-500/30 border border-emerald-400/50 text-emerald-300 text-xs font-bold flex items-center gap-2 shadow-xs transition-all active:scale-95 whitespace-nowrap"
          >
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span className="hidden sm:inline">{locale === 'ar' ? 'سجل التدقيق والمطابقة' : 'Audit & Acceptance Matrix'}</span>
            <span className="sm:hidden">Audit</span>
          </button>
        </div>
      </header>

      {/* Screen Selector Secondary Carousel Strip */}
      <div className="w-full bg-[#0a0d24]/90 border-b border-white/5 px-4 py-2 overflow-x-auto no-scrollbar flex items-center gap-2">
        {screenList.map((screen) => {
          const isActive = currentScreen === screen.id;
          return (
            <button
              key={screen.id}
              onClick={() => setCurrentScreen(screen.id)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all flex items-center gap-1.5 shrink-0 ${
                isActive
                  ? 'bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-600 text-white shadow-[0_0_12px_rgba(99,102,241,0.5)] border border-white/40'
                  : 'bg-white/5 hover:bg-white/10 text-slate-300 border border-white/10'
              }`}
            >
              <span className="font-mono text-[10px] opacity-75">{screen.task}</span>
              <span>{locale === 'ar' ? screen.labelAr : screen.label}</span>
            </button>
          );
        })}
      </div>

      {/* Main Viewport Content Area */}
      <main className="flex-1 flex flex-col items-center justify-center p-4 md:p-8">
        {viewMode === 'compare' ? (
          /* Side-by-Side Dual Viewport */
          <div className="w-full max-w-6xl flex flex-col lg:flex-row items-center justify-center gap-8 py-4">
            {/* Left Column: Live Interactive React Application */}
            <div className="flex flex-col items-center">
              <div className="flex items-center gap-2 mb-3 text-xs font-bold text-cyan-300 uppercase tracking-wider">
                <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse" />
                <span>{locale === 'ar' ? 'التطبيق التفاعلي الحي (React / Compose)' : 'Live Interactive Implementation'}</span>
              </div>
              <DeviceFrame>
                {renderActiveScreen()}
              </DeviceFrame>
            </div>

            {/* Right Column: Approved Design Reference Asset */}
            <div className="flex flex-col items-center">
              <div className="flex items-center gap-2 mb-3 text-xs font-bold text-purple-300 uppercase tracking-wider">
                <span className="w-2 h-2 rounded-full bg-purple-400" />
                <span>{locale === 'ar' ? 'المرجع البصري المعتمد (Reference PNG)' : 'Approved Reference Specification'}</span>
              </div>
              <div className="w-[412px] rounded-[48px] p-3 bg-gradient-to-b from-slate-700 via-slate-900 to-black shadow-2xl border border-white/15 overflow-hidden">
                <div className="relative rounded-[38px] overflow-hidden aspect-[9/19.5] bg-black">
                  <img 
                    src={currentRefImage} 
                    alt="Approved Reference"
                    className="w-full h-full object-cover"
                    referrerPolicy="no-referrer"
                  />
                  <div className="absolute top-4 left-4 px-2.5 py-1 rounded-full bg-black/60 backdrop-blur-md text-[10px] font-mono text-purple-300 border border-white/20">
                    SHA-256 Certified
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : (
          /* Standard Device View */
          <div className="py-2">
            <DeviceFrame>
              {renderActiveScreen()}
            </DeviceFrame>
          </div>
        )}
      </main>

      {/* Audit Modal */}
      <AuditLedgerModal />
    </div>
  );
};

export default function App() {
  return (
    <VPNProvider>
      <MainWorkspace />
    </VPNProvider>
  );
}
