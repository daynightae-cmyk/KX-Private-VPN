import React from 'react';
import { LucideIcon } from 'lucide-react';

interface ActionCardProps {
  id: string;
  title: string;
  subtitle?: string;
  icon: LucideIcon;
  gradientFrom: string;
  gradientTo: string;
  iconColor?: string;
  onClick: () => void;
  badge?: string;
}

export const ActionCard: React.FC<ActionCardProps> = ({
  id,
  title,
  subtitle,
  icon: Icon,
  gradientFrom,
  gradientTo,
  iconColor = 'text-white',
  onClick,
  badge,
}) => {
  return (
    <button
      id={id}
      type="button"
      onClick={onClick}
      className="group relative w-full rounded-2xl p-[1px] bg-gradient-to-r from-white via-white/70 to-white/90 shadow-xs hover:shadow-md active:scale-[0.98] transition-all text-left overflow-hidden cursor-pointer"
    >
      {/* Frosted Glass Body */}
      <div className="w-full h-full rounded-2xl bg-white/60 backdrop-blur-xl p-3 flex items-center gap-3 border border-white/80 group-hover:bg-white/75 transition-colors">
        {/* Gradient Icon Emblem with Glass Rim */}
        <div 
          className={`relative w-10 h-10 rounded-2xl bg-gradient-to-tr ${gradientFrom} ${gradientTo} flex items-center justify-center shadow-xs shrink-0 border border-white/50 group-hover:scale-105 transition-transform duration-300`}
        >
          <Icon className={`w-5 h-5 ${iconColor}`} />
          {/* Subtle Glint */}
          <div className="absolute top-0 left-0 right-0 h-1/2 rounded-t-2xl bg-white/30 pointer-events-none" />
        </div>

        {/* Text Container */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5">
            <span className="block font-bold text-xs text-indigo-950 truncate tracking-tight">
              {title}
            </span>
            {badge && (
              <span className="px-1.5 py-0.2 rounded-full bg-indigo-100 text-[9px] font-semibold text-indigo-700 font-mono shrink-0">
                {badge}
              </span>
            )}
          </div>
          {subtitle && (
            <span className="block text-[10px] text-slate-500 truncate mt-0.5 font-medium">
              {subtitle}
            </span>
          )}
        </div>
      </div>
    </button>
  );
};
