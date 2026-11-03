import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { 
  ScreenId, 
  ConnectionStatus, 
  ServerItem, 
  SplitAppItem, 
  ImportItem, 
  SecuritySettings, 
  AdvancedSettings, 
  SessionStats,
  HistoricalServerUsage
} from '../types/vpn';
import { Locale, translations } from '../i18n/translations';

interface VPNContextType {
  currentScreen: ScreenId;
  setCurrentScreen: (screen: ScreenId) => void;
  connectionStatus: ConnectionStatus;
  toggleConnect: () => void;
  disconnectNow: () => void;
  selectedServer: ServerItem;
  setSelectedServer: (server: ServerItem) => void;
  servers: ServerItem[];
  toggleFavoriteServer: (id: string) => void;
  sessionStats: SessionStats;
  securitySettings: SecuritySettings;
  toggleSecuritySetting: (key: keyof SecuritySettings) => void;
  splitApps: SplitAppItem[];
  splitMode: 'bypass' | 'only_vpn';
  setSplitMode: (mode: 'bypass' | 'only_vpn') => void;
  toggleSplitApp: (id: string) => void;
  advancedSettings: AdvancedSettings;
  updateAdvancedSettings: (partial: Partial<AdvancedSettings>) => void;
  imports: ImportItem[];
  addImport: (item: Omit<ImportItem, 'id' | 'importedAt' | 'importedAtAr'>) => void;
  clearImports: () => void;
  history: HistoricalServerUsage[];
  locale: Locale;
  setLocale: (locale: Locale) => void;
  toggleLocale: () => void;
  t: (typeof translations)['en'];
  theme: 'night' | 'day';
  setTheme: (theme: 'night' | 'day') => void;
  toggleTheme: () => void;
  viewMode: 'device' | 'desktop' | 'compare';
  setViewMode: (mode: 'device' | 'desktop' | 'compare') => void;
  compareOpacity: number;
  setCompareOpacity: (val: number) => void;
  deviceViewport: '360' | '411' | 'responsive';
  setDeviceViewport: (vp: '360' | '411' | 'responsive') => void;
  isAuditOpen: boolean;
  setIsAuditOpen: (open: boolean) => void;
}

const initialServers: ServerItem[] = [
  {
    id: 'us',
    name: 'United States',
    nameAr: 'الولايات المتحدة',
    countryCode: 'US',
    flagEmoji: '🇺🇸',
    ping: 23,
    load: 32,
    loadSegments: 3,
    isRecommended: true,
    isFavorite: true,
    categories: ['all', 'fastest', 'streaming', 'gaming'],
    ip: '142.250.190.46',
    city: 'New York',
  },
  {
    id: 'uk',
    name: 'United Kingdom',
    nameAr: 'المملكة المتحدة',
    countryCode: 'GB',
    flagEmoji: '🇬🇧',
    ping: 28,
    load: 37,
    loadSegments: 3,
    isFavorite: false,
    categories: ['all', 'fastest', 'streaming'],
    ip: '51.140.112.89',
    city: 'London',
  },
  {
    id: 'de',
    name: 'Germany',
    nameAr: 'ألمانيا',
    countryCode: 'DE',
    flagEmoji: '🇩🇪',
    ping: 31,
    load: 44,
    loadSegments: 3,
    isFavorite: false,
    categories: ['all', 'fastest'],
    ip: '91.107.182.204',
    city: 'Frankfurt',
  },
  {
    id: 'sg',
    name: 'Singapore',
    nameAr: 'سنغافورة',
    countryCode: 'SG',
    flagEmoji: '🇸🇬',
    ping: 36,
    load: 48,
    loadSegments: 3,
    isFavorite: false,
    categories: ['all', 'fastest', 'gaming'],
    ip: '13.251.104.22',
    city: 'Singapore',
  },
  {
    id: 'jp',
    name: 'Japan',
    nameAr: 'اليابان',
    countryCode: 'JP',
    flagEmoji: '🇯🇵',
    ping: 42,
    load: 62,
    loadSegments: 4,
    isFavorite: false,
    categories: ['all', 'streaming', 'gaming'],
    ip: '54.238.12.80',
    city: 'Tokyo',
  },
  {
    id: 'ca',
    name: 'Canada',
    nameAr: 'كندا',
    countryCode: 'CA',
    flagEmoji: '🇨🇦',
    ping: 49,
    load: 69,
    loadSegments: 4,
    isFavorite: false,
    categories: ['all', 'streaming'],
    ip: '35.182.78.11',
    city: 'Montreal',
  },
  {
    id: 'au',
    name: 'Australia',
    nameAr: 'أستراليا',
    countryCode: 'AU',
    flagEmoji: '🇦🇺',
    ping: 56,
    load: 76,
    loadSegments: 4,
    isFavorite: false,
    categories: ['all', 'gaming'],
    ip: '13.239.52.190',
    city: 'Sydney',
  },
];

const initialSplitApps: SplitAppItem[] = [
  { id: 'chrome', name: 'Chrome', packageName: 'com.android.chrome', iconType: 'chrome', throughVpn: true },
  { id: 'youtube', name: 'YouTube', packageName: 'com.google.android.youtube', iconType: 'youtube', throughVpn: true },
  { id: 'spotify', name: 'Spotify', packageName: 'com.spotify.music', iconType: 'spotify', throughVpn: false },
  { id: 'instagram', name: 'Instagram', packageName: 'com.instagram.android', iconType: 'instagram', throughVpn: true },
  { id: 'whatsapp', name: 'WhatsApp', packageName: 'com.whatsapp', iconType: 'whatsapp', throughVpn: false },
  { id: 'discord', name: 'Discord', packageName: 'com.discord', iconType: 'discord', throughVpn: false },
];

const initialImports: ImportItem[] = [
  { id: '1', name: 'KX-US-01', protocol: 'OpenVPN', importedAt: 'Imported 2 min ago', importedAtAr: 'استوردت منذ دقيقتين', endpoint: 'us1.kxvpn.net:1194' },
  { id: '2', name: 'KX-EU-Frankfurt', protocol: 'WireGuard', importedAt: 'Imported 1 h ago', importedAtAr: 'استوردت منذ ساعة', endpoint: 'de-fra.kxvpn.net:51820' },
  { id: '3', name: 'KX-Asia-SG', protocol: 'OpenVPN', importedAt: 'Imported yesterday', importedAtAr: 'استوردت بالأمس', endpoint: 'sg-core.kxvpn.net:1194' },
];

const initialHistory: HistoricalServerUsage[] = [
  {
    id: 'h1',
    serverName: 'Singapore',
    serverNameAr: 'سنغافورة',
    countryCode: 'SG',
    flagEmoji: '🇸🇬',
    timeRange: 'Today, 10:02 – 12:30',
    timeRangeAr: 'اليوم، 10:02 – 12:30',
    duration: '2 h 28 m',
    durationAr: '2 س 28 د',
    dataTransferred: '10.8 GB',
    waveColor: 'purple',
  },
  {
    id: 'h2',
    serverName: 'United States',
    serverNameAr: 'الولايات المتحدة',
    countryCode: 'US',
    flagEmoji: '🇺🇸',
    timeRange: 'Yesterday, 19:14 – 23:46',
    timeRangeAr: 'الأمس، 19:14 – 23:46',
    duration: '4 h 32 m',
    durationAr: '4 س 32 د',
    dataTransferred: '15.6 GB',
    waveColor: 'cyan',
  },
  {
    id: 'h3',
    serverName: 'Germany',
    serverNameAr: 'ألمانيا',
    countryCode: 'DE',
    flagEmoji: '🇩🇪',
    timeRange: 'Jan 20, 14:08 – 16:11',
    timeRangeAr: '20 يناير، 14:08 – 16:11',
    duration: '2 h 03 m',
    durationAr: '2 س 03 د',
    dataTransferred: '6.4 GB',
    waveColor: 'purple',
  },
];

const VPNContext = createContext<VPNContextType | undefined>(undefined);

export const VPNProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [currentScreen, setCurrentScreen] = useState<ScreenId>('02-home-disconnected');
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const [servers, setServers] = useState<ServerItem[]>(initialServers);
  const [selectedServer, setSelectedServer] = useState<ServerItem>(initialServers[0]);
  const [locale, setLocale] = useState<Locale>('en');
  const [theme, setTheme] = useState<'night' | 'day'>('night');
  const [viewMode, setViewMode] = useState<'device' | 'desktop' | 'compare'>('device');
  const [compareOpacity, setCompareOpacity] = useState<number>(0.5);
  const [deviceViewport, setDeviceViewport] = useState<'360' | '411' | 'responsive'>('411');
  const [isAuditOpen, setIsAuditOpen] = useState<boolean>(false);

  const [sessionStats, setSessionStats] = useState<SessionStats>({
    sessionDurationSeconds: 2538, // 00:42:18 baseline as in reference
    downloadBytes: 968884920,
    uploadBytes: 490733568,
    currentDownloadSpeedMbps: 92.4,
    currentUploadSpeedMbps: 46.8,
    currentPingMs: 28,
    assignedIp: '91.107.182.204',
    originCountry: 'UAE',
    originFlag: '🇦🇪',
  });

  const [securitySettings, setSecuritySettings] = useState<SecuritySettings>({
    killSwitch: true,
    dnsLeakProtection: true,
    ipv6Protection: true,
    autoReconnect: true,
    trackerBlocking: true,
    alwaysOnVpn: true,
  });

  const [splitApps, setSplitApps] = useState<SplitAppItem[]>(initialSplitApps);
  const [splitMode, setSplitMode] = useState<'bypass' | 'only_vpn'>('only_vpn');

  const [advancedSettings, setAdvancedSettings] = useState<AdvancedSettings>({
    protocol: 'automatic',
    mtu: 1420,
    dnsMode: 'encrypted',
    obfuscation: true,
    portHopping: true,
    handshakeKeepalive: 25,
  });

  const [imports, setImports] = useState<ImportItem[]>(initialImports);
  const [history] = useState<HistoricalServerUsage[]>(initialHistory);

  // Sync document root classes & dir
  useEffect(() => {
    document.documentElement.lang = locale;
    document.documentElement.dir = locale === 'ar' ? 'rtl' : 'ltr';
    if (theme === 'night') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [locale, theme]);

  // Real Monotonic Session Timer when Connected
  useEffect(() => {
    if (connectionStatus !== 'connected') return;

    const interval = setInterval(() => {
      setSessionStats(prev => ({
        ...prev,
        sessionDurationSeconds: prev.sessionDurationSeconds + 1,
        // Small realistic speed fluctuations
        currentDownloadSpeedMbps: Number((92.4 + (Math.sin(Date.now() / 1500) * 3.2)).toFixed(1)),
        currentUploadSpeedMbps: Number((46.8 + (Math.cos(Date.now() / 1800) * 2.1)).toFixed(1)),
      }));
    }, 1000);

    return () => clearInterval(interval);
  }, [connectionStatus]);

  // Automatic reference theme suggestion per screen (can be manually overridden anytime)
  useEffect(() => {
    // Reference screens 00, 01, 02, 03 are in Night (Dark Indigo) mode.
    // Reference screens 04, 05, 06, 07, 08, 09, 10 are in Day (Lavender Pearl) mode.
    if (currentScreen === '00-splash' || currentScreen === '01-welcome' || currentScreen === '02-home-disconnected' || currentScreen === '03-server-selection') {
      setTheme('night');
    } else {
      setTheme('day');
    }
  }, [currentScreen]);

  const toggleConnect = () => {
    if (connectionStatus === 'disconnected') {
      setConnectionStatus('connecting');
      setTimeout(() => {
        setConnectionStatus('connected');
        setCurrentScreen('04-connected-session');
      }, 1200);
    } else if (connectionStatus === 'connected') {
      setConnectionStatus('disconnecting');
      setTimeout(() => {
        setConnectionStatus('disconnected');
        setCurrentScreen('02-home-disconnected');
      }, 800);
    }
  };

  const disconnectNow = () => {
    setConnectionStatus('disconnecting');
    setTimeout(() => {
      setConnectionStatus('disconnected');
      setCurrentScreen('02-home-disconnected');
    }, 600);
  };

  const toggleFavoriteServer = (id: string) => {
    setServers(prev => prev.map(s => s.id === id ? { ...s, isFavorite: !s.isFavorite } : s));
  };

  const toggleSecuritySetting = (key: keyof SecuritySettings) => {
    setSecuritySettings(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const toggleSplitApp = (id: string) => {
    setSplitApps(prev => prev.map(app => app.id === id ? { ...app, throughVpn: !app.throughVpn } : app));
  };

  const updateAdvancedSettings = (partial: Partial<AdvancedSettings>) => {
    setAdvancedSettings(prev => ({ ...prev, ...partial }));
  };

  const addImport = (item: Omit<ImportItem, 'id' | 'importedAt' | 'importedAtAr'>) => {
    const newItem: ImportItem = {
      ...item,
      id: String(Date.now()),
      importedAt: 'Imported just now',
      importedAtAr: 'استوردت للتو',
    };
    setImports(prev => [newItem, ...prev]);
  };

  const clearImports = () => {
    setImports([]);
  };

  const toggleTheme = () => {
    setTheme(prev => prev === 'night' ? 'day' : 'night');
  };

  const toggleLocale = () => {
    setLocale(prev => prev === 'en' ? 'ar' : 'en');
  };

  const t = translations[locale];

  return (
    <VPNContext.Provider value={{
      currentScreen,
      setCurrentScreen,
      connectionStatus,
      toggleConnect,
      disconnectNow,
      selectedServer,
      setSelectedServer,
      servers,
      toggleFavoriteServer,
      sessionStats,
      securitySettings,
      toggleSecuritySetting,
      splitApps,
      splitMode,
      setSplitMode,
      toggleSplitApp,
      advancedSettings,
      updateAdvancedSettings,
      imports,
      addImport,
      clearImports,
      history,
      locale,
      setLocale,
      toggleLocale,
      t,
      theme,
      setTheme,
      toggleTheme,
      viewMode,
      setViewMode,
      compareOpacity,
      setCompareOpacity,
      deviceViewport,
      setDeviceViewport,
      isAuditOpen,
      setIsAuditOpen,
    }}>
      {children}
    </VPNContext.Provider>
  );
};

export const useVPN = () => {
  const context = useContext(VPNContext);
  if (!context) {
    throw new Error('useVPN must be used within a VPNProvider');
  }
  return context;
};
