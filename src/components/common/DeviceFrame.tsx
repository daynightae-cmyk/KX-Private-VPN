import React from 'react';
import { useVPN } from '../../context/VPNContext';
import { ScreenId } from '../../types/vpn';
import { Wifi, Signal, Battery, Layers } from 'lucide-react';

interface DeviceFrameProps {
  children: React.ReactNode;
}

export const referenceImages: Record<ScreenId, string> = {
  '00-splash': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '01-welcome': '/assets/aistudio/01-welcome_464a7c4f.png',
  '02-home-disconnected': '/assets/aistudio/02-home-disconnected_825c75b0.png',
  '03-server-selection': '/assets/aistudio/03-server-selection_8d2c742a.png',
  '04-connected-session': '/assets/aistudio/04-connected-session_f0cfe658.png',
  '05-security-center': '/assets/aistudio/05-security-center_d0a1f09a.png',
  '06-split-tunneling': '/assets/aistudio/06-split-tunneling_c9265fd7.png',
  '07-advanced-network': '/assets/aistudio/07-advanced-network_5b3214f4.png',
  '08-statistics': '/assets/aistudio/08-statistics_a6c474b9.png',
  '09-import-configuration': '/assets/aistudio/09-import-configuration_670a2aea.png',
  '10-profile-settings': '/assets/aistudio/10-profile-settings_75031956.png',
  '11-repair-lab': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '12-storage-xray': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '13-ghost-hunter': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '14-duplicate-dna': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '15-heavyweight-finder': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '16-cache-surgeon': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '17-media-slim': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '18-download-rescue': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '19-session-shield': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '20-network-doctor': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  '21-repair-autopilot': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
  'founder-identity': '/assets/aistudio/00-brand-logo_c0b92dd8.png',
};

export const DeviceFrame: React.FC<DeviceFrameProps> = ({ children }) => {
  const { 
    currentScreen, 
    viewMode, 
    compareOpacity, 
    deviceViewport, 
    theme 
  } = useVPN();

  const isNightTheme = theme === 'night';
  const refImage = referenceImages[currentScreen];

  // Specific time shown in references:
  // 01, 02, 08, 10 show 12:30
  // 03, 04, 05, 06, 07, 09 show 10:24
  const clockText = (currentScreen === '03-server-selection' || 
                     currentScreen === '04-connected-session' || 
                     currentScreen === '05-security-center' || 
                     currentScreen === '06-split-tunneling' || 
                     currentScreen === '07-advanced-network' || 
                     currentScreen === '09-import-configuration') 
                     ? '10:24' : '12:30';

  const widthClass = deviceViewport === '360' 
    ? 'w-[360px]' 
    : deviceViewport === '411' 
      ? 'w-[412px]' 
      : 'w-full max-w-[420px]';

  return (
    <div className={`relative ${widthClass} mx-auto transition-all duration-300`}>
      {/* Phone Outer Chassis Shadow */}
      <div className="relative rounded-[48px] p-3 bg-gradient-to-b from-slate-700 via-slate-900 to-black shadow-[0_25px_70px_rgba(0,0,0,0.6),0_0_0_1px_rgba(255,255,255,0.15)] overflow-hidden">
        {/* Antennas & Volume/Power Buttons Mock */}
        <div className="absolute top-28 -left-[4px] w-[3px] h-10 bg-slate-600 rounded-l-sm" />
        <div className="absolute top-44 -left-[4px] w-[3px] h-10 bg-slate-600 rounded-l-sm" />
        <div className="absolute top-36 -right-[4px] w-[3px] h-14 bg-slate-600 rounded-r-sm" />

        {/* Screen Bezel */}
        <div className="relative rounded-[38px] overflow-hidden bg-black aspect-[9/19.5] flex flex-col justify-between">
          
          {/* Android Status Bar matching reference */}
          <div className={`z-40 px-6 pt-3 pb-1 flex items-center justify-between text-xs font-semibold tracking-tight ${isNightTheme ? 'text-white' : 'text-slate-800'}`}>
            {/* Clock */}
            <span className="font-mono text-xs">{clockText}</span>

            {/* Camera Punch Hole */}
            <div className="w-4 h-4 rounded-full bg-black border border-white/20 shadow-inner flex items-center justify-center">
              <div className="w-1.5 h-1.5 rounded-full bg-slate-900" />
            </div>

            {/* System Icons: WiFi, Signal, Battery */}
            <div className="flex items-center gap-1.5">
              <Wifi className="w-3.5 h-3.5" />
              <Signal className="w-3.5 h-3.5" />
              <div className="flex items-center gap-0.5">
                <span className="text-[10px] font-mono">100%</span>
                <Battery className="w-3.5 h-3.5" />
              </div>
            </div>
          </div>

          {/* Interactive Screen Content */}
          <div className="relative flex-1 w-full overflow-hidden flex flex-col">
            {children}

            {/* Overlay Comparison Mode if Active */}
            {viewMode === 'compare' && refImage && (
              <div 
                className="absolute inset-0 pointer-events-none z-50 overflow-hidden mix-blend-normal transition-opacity duration-200"
                style={{ opacity: compareOpacity }}
              >
                <img 
                  src={refImage} 
                  alt="Reference Overlay"
                  className="w-full h-full object-cover"
                  referrerPolicy="no-referrer"
                />
              </div>
            )}
          </div>

          {/* Android Bottom Navigation Pill Gesture Bar */}
          <div className="z-40 w-full py-1.5 flex items-center justify-center pointer-events-none">
            <div className={`w-32 h-1 rounded-full ${isNightTheme ? 'bg-white/40' : 'bg-slate-400/50'}`} />
          </div>
        </div>
      </div>
    </div>
  );
};
