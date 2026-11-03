import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { ScreenId } from '../../types/vpn';
import { 
  Home, 
  Server, 
  Wrench, 
  Shield, 
  BarChart2, 
  Settings 
} from 'lucide-react';

interface AndroidBottomNavProps {
  activeScreen?: ScreenId;
}

export const AndroidBottomNav: React.FC<AndroidBottomNavProps> = () => {
  const { currentScreen, setCurrentScreen, connectionStatus, locale, t } = useVPN();

  // Helper to determine active tab
  const isHomeActive = currentScreen === '02-home-disconnected' || currentScreen === '04-connected-session' || currentScreen === '00-splash' || currentScreen === '01-welcome';
  const isServersActive = currentScreen === '03-server-selection' || currentScreen === '09-import-configuration';
  const isRepairActive = currentScreen.startsWith('11-') || 
                         currentScreen.startsWith('12-') || 
                         currentScreen.startsWith('13-') || 
                         currentScreen.startsWith('14-') || 
                         currentScreen.startsWith('15-') || 
                         currentScreen.startsWith('16-') || 
                         currentScreen.startsWith('17-') || 
                         currentScreen.startsWith('18-') || 
                         currentScreen.startsWith('19-') || 
                         currentScreen.startsWith('20-') || 
                         currentScreen.startsWith('21-');
  const isSecurityActive = currentScreen === '05-security-center' || currentScreen === '06-split-tunneling' || currentScreen === '07-advanced-network';
  const isStatsActive = currentScreen === '08-statistics';
  const isSettingsActive = currentScreen === '10-profile-settings' || currentScreen === 'founder-identity';

  const handleHomeClick = () => {
    if (connectionStatus === 'connected') {
      setCurrentScreen('04-connected-session');
    } else {
      setCurrentScreen('02-home-disconnected');
    }
  };

  const tabs = [
    {
      id: 'home',
      label: t.navHome,
      icon: Home,
      isActive: isHomeActive,
      onClick: handleHomeClick,
    },
    {
      id: 'servers',
      label: t.navServers,
      icon: Server,
      isActive: isServersActive,
      onClick: () => setCurrentScreen('03-server-selection'),
    },
    {
      id: 'repair',
      label: (t as any).navRepair || (locale === 'ar' ? 'الصيانة' : 'Repair'),
      icon: Wrench,
      isActive: isRepairActive,
      onClick: () => setCurrentScreen('11-repair-lab'),
      isSpecial: true,
    },
    {
      id: 'security',
      label: t.navSecurity,
      icon: Shield,
      isActive: isSecurityActive,
      onClick: () => setCurrentScreen('05-security-center'),
    },
    {
      id: 'stats',
      label: t.navStats,
      icon: BarChart2,
      isActive: isStatsActive,
      onClick: () => setCurrentScreen('08-statistics'),
    },
    {
      id: 'settings',
      label: t.navSettings,
      icon: Settings,
      isActive: isSettingsActive,
      onClick: () => setCurrentScreen('10-profile-settings'),
    },
  ];

  return (
    <nav className="w-full z-20 pt-1.5 pb-0.5 px-1 shrink-0">
      <div className="w-full rounded-full bg-[#11143d]/85 backdrop-blur-2xl p-1 flex items-center justify-between border border-white/20 shadow-[0_8px_32px_rgba(0,0,0,0.6)]">
        {tabs.map((tab) => {
          const Icon = tab.icon;

          if (tab.isActive) {
            return (
              <button
                key={tab.id}
                onClick={tab.onClick}
                className="flex-1 py-1.5 px-1 rounded-full bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-600 text-white flex flex-col items-center justify-center shadow-[0_0_15px_rgba(99,102,241,0.6)] border border-white/30 transition-all cursor-pointer"
              >
                <Icon className="w-4 h-4 text-white" />
                <span className="text-[9.5px] font-bold tracking-tight mt-0.5 whitespace-nowrap">
                  {tab.label}
                </span>
              </button>
            );
          }

          return (
            <button
              key={tab.id}
              onClick={tab.onClick}
              className={`flex-1 py-1.5 px-1 rounded-full flex flex-col items-center justify-center transition-all cursor-pointer ${
                tab.isSpecial
                  ? 'text-cyan-300/90 hover:text-cyan-200 hover:bg-cyan-500/10'
                  : 'text-indigo-200/70 hover:text-white hover:bg-white/5'
              }`}
            >
              <Icon className={`w-4 h-4 ${tab.isSpecial ? 'text-cyan-400 drop-shadow-[0_0_6px_rgba(6,182,212,0.6)]' : ''}`} />
              <span className="text-[9.5px] font-medium tracking-tight mt-0.5 whitespace-nowrap">
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
