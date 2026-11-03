import React from 'react';
import { Sparkles } from 'lucide-react';

interface GlassLoadingOverlayProps {
  message: string;
}

export const GlassLoadingOverlay: React.FC<GlassLoadingOverlayProps> = ({ message }) => {
  return (
    <div className="absolute inset-0 z-[100] flex items-center justify-center bg-black/30 backdrop-blur-md animate-fade-in">
      <div className="p-8 rounded-3xl bg-white/10 border border-white/20 backdrop-blur-2xl shadow-2xl text-center flex flex-col items-center gap-4">
        <div className="w-12 h-12 rounded-full border-4 border-cyan-400 border-t-transparent animate-spin" />
        <p className="text-white font-bold tracking-wider">{message}</p>
        <Sparkles className="w-6 h-6 text-cyan-300 animate-pulse" />
      </div>
    </div>
  );
};
