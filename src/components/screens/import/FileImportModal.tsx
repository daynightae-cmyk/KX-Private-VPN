import React, { useState, useRef } from 'react';
import { parseWireGuardConfig, ParsedWireGuardConfig } from '../../../util/config-parser';
import { FileText, X, Upload } from 'lucide-react';
import { GlassLoadingOverlay } from '../../common/GlassLoadingOverlay';

interface FileImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportSuccess: (config: ParsedWireGuardConfig) => void;
  locale: string;
}

export const FileImportModal: React.FC<FileImportModalProps> = ({
  isOpen,
  onClose,
  onImportSuccess,
  locale,
}) => {
  const [isParsing, setIsParsing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsParsing(true);
    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target?.result as string;
      const parsed = parseWireGuardConfig(text, file.name.replace(/\.[^/.]+$/, ''));
      setIsParsing(false);
      if (parsed.isValid) {
        onImportSuccess(parsed);
        onClose();
      } else {
        alert(locale === 'ar' ? 'ملف غير صالح' : 'Invalid configuration file');
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-md p-4 overflow-y-auto">
      {isParsing && <GlassLoadingOverlay message={locale === 'ar' ? 'جارٍ تحليل الملف...' : 'Parsing file...'} />}
      <div className="relative w-full max-w-sm rounded-3xl bg-[#0f1334] border border-white/20 shadow-2xl text-white p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h3 className="font-bold text-sm">{locale === 'ar' ? 'استيراد ملف' : 'Import File'}</h3>
          <button onClick={onClose}><X className="w-5 h-5" /></button>
        </div>
        <input type="file" ref={fileInputRef} accept=".conf" className="hidden" onChange={handleFileUpload} />
        <button 
          onClick={() => fileInputRef.current?.click()}
          className="w-full p-8 rounded-2xl border-2 border-dashed border-white/20 hover:border-cyan-400 flex flex-col items-center gap-3 transition-colors"
        >
          <Upload className="w-10 h-10 text-cyan-400" />
          <span className="text-xs">{locale === 'ar' ? 'اختر ملف .conf' : 'Select .conf file'}</span>
        </button>
      </div>
    </div>
  );
};
