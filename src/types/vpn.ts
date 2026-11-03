export type ScreenId = 
  | '00-splash'
  | '01-welcome'
  | '02-home-disconnected'
  | '03-server-selection'
  | '04-connected-session'
  | '05-security-center'
  | '06-split-tunneling'
  | '07-advanced-network'
  | '08-statistics'
  | '09-import-configuration'
  | '10-profile-settings'
  | '11-repair-lab'
  | '12-storage-xray'
  | '13-ghost-hunter'
  | '14-duplicate-dna'
  | '15-heavyweight-finder'
  | '16-cache-surgeon'
  | '17-media-slim'
  | '18-download-rescue'
  | '19-session-shield'
  | '20-network-doctor'
  | '21-repair-autopilot'
  | 'founder-identity';

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'disconnecting';

export type ProtocolType = 'wireguard' | 'openvpn' | 'automatic';

export type SplitTunnelMode = 'bypass' | 'only_vpn';

export interface ServerItem {
  id: string;
  name: string;
  nameAr: string;
  countryCode: string; // ISO 2-letter
  flagEmoji: string;
  ping: number; // ms
  load: number; // percentage
  loadSegments: number; // 1 to 5
  isRecommended?: boolean;
  isFavorite: boolean;
  categories: ('all' | 'fastest' | 'streaming' | 'gaming')[];
  ip: string;
  city: string;
}

export interface SplitAppItem {
  id: string;
  name: string;
  packageName: string;
  iconType: 'chrome' | 'youtube' | 'spotify' | 'instagram' | 'whatsapp' | 'discord';
  throughVpn: boolean;
}

export interface ImportItem {
  id: string;
  name: string;
  protocol: 'WireGuard' | 'OpenVPN';
  importedAt: string;
  importedAtAr: string;
  endpoint: string;
  publicKey?: string;
  dns?: string;
}

export interface SecuritySettings {
  killSwitch: boolean;
  dnsLeakProtection: boolean;
  ipv6Protection: boolean;
  autoReconnect: boolean;
  trackerBlocking: boolean;
  alwaysOnVpn: boolean;
}

export interface AdvancedSettings {
  protocol: ProtocolType;
  mtu: number;
  dnsMode: 'encrypted' | 'system' | 'cloudflare' | 'quad9' | 'custom';
  obfuscation: boolean;
  portHopping: boolean;
  handshakeKeepalive: number; // seconds
}

export interface SessionStats {
  sessionDurationSeconds: number;
  downloadBytes: number;
  uploadBytes: number;
  currentDownloadSpeedMbps: number;
  currentUploadSpeedMbps: number;
  currentPingMs: number;
  assignedIp: string;
  originCountry: string;
  originFlag: string;
}

export interface HistoricalServerUsage {
  id: string;
  serverName: string;
  serverNameAr: string;
  countryCode: string;
  flagEmoji: string;
  timeRange: string;
  timeRangeAr: string;
  duration: string;
  durationAr: string;
  dataTransferred: string;
  waveColor: 'purple' | 'cyan' | 'magenta';
}
