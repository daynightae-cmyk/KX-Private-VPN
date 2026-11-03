import React, { useState } from 'react';
import { parseWireGuardConfig, SAMPLE_WIREGUARD_CONFIG, ParsedWireGuardConfig } from '../../../util/config-parser';
import { ClipboardList, X, CheckCircle2, AlertTriangle, Clipboard, Check } from 'lucide-react';

interface ClipboardImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportSuccess: (config: ParsedWireGuardConfig) => void;
  locale: string;
}

export const ClipboardImportModal: React.FC<ClipboardImportModalProps> = ({
  isOpen,
  onClose,
  onImportSuccess,
  locale,
}) => {
  const [text, setText] = useState<string>(SAMPLE_WIREGUARD_CONFIG);
  const [parsed, setParsed] = useState<ParsedWireGuardConfig>(() => parseWireGuardConfig(SAMPLE_WIREGUARD_CONFIG));
  const [pasteSuccess, setPasteSuccess] = useState(false);

  if (!isOpen) return null;

  const handleTextChange = (val: string) => {
    setText(val);
    setParsed(parseWireGuardConfig(val));
  };

  const handlePasteClipboard = async () => {
    try {
      const clipText = await navigator.clipboard.readText();
      if (clipText) {
        setText(clipText);
        setParsed(parseWireGuardConfig(clipText));
        setPasteSuccess(true);
        setTimeout(() => setPasteSuccess(false), 2000);
      }
    } catch {
      // Fallback if browser clipboard permission prompt blocked
      setText(SAMPLE_WIREGUARD_CONFIG);
      setParsed(parseWireGuardConfig(SAMPLE_WIREGUARD_CONFIG));
    }
  };

  const handleApply = () => {
    if (parsed.isValid) {
      onImportSuccess(parsed);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-sm rounded-3xl bg-[#0f1334] border border-white/20 shadow-2xl text-white overflow-hidden p-5 space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-purple-500 to-pink-500 flex items-center justify-center text-white shadow-xs">
              <ClipboardList className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-white">
                {locale === 'ar' ? 'لصق من الحافظة' : 'Paste from Clipboard'}
              </h3>
              <p className="text-[10px] text-indigo-200/70">
                {locale === 'ar' ? 'فحص فوري لصحة تكوين WireGuard' : 'Instant WireGuard validation of pasted text'}
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white active:scale-95 transition-all"
            aria-label="Close"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Action button to read from clipboard */}
        <button
          type="button"
          onClick={handlePasteClipboard}
          className="w-full py-2 px-3 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 flex items-center justify-center gap-2 text-xs font-semibold text-cyan-300 active:scale-98 transition-all"
        >
          {pasteSuccess ? (
            <>
              <Check className="w-4 h-4 text-emerald-400" />
              <span>{locale === 'ar' ? 'تم اللصق بنجاح!' : 'Pasted from Clipboard!'}</span>
            </>
          ) : (
            <>
              <Clipboard className="w-4 h-4" />
              <span>{locale === 'ar' ? 'قراءة الحافظة وتعبئة البيانات' : 'Read System Clipboard'}</span>
            </>
          )}
        </button>

        {/* Textarea for WireGuard .conf content */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-[11px] font-bold text-slate-300">
            <span>{locale === 'ar' ? 'نص التكوين:' : 'Configuration Content:'}</span>
            {parsed.isValid ? (
              <span className="text-[10px] text-emerald-400 font-mono flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" />
                <span>VALID</span>
              </span>
            ) : (
              <span className="text-[10px] text-amber-400 font-mono flex items-center gap-1">
                <AlertTriangle className="w-3 h-3" />
                <span>INVALID</span>
              </span>
            )}
          </div>

          <textarea
            value={text}
            onChange={(e) => handleTextChange(e.target.value)}
            rows={7}
            className="w-full rounded-2xl bg-black/60 border border-white/15 p-3 text-xs font-mono text-cyan-200 focus:outline-none focus:border-cyan-400 resize-none"
            placeholder="[Interface]&#10;PrivateKey = ...&#10;Address = 10.0.0.2/32&#10;&#10;[Peer]&#10;PublicKey = ...&#10;Endpoint = vpn.example.com:51820&#10;AllowedIPs = 0.0.0.0/0"
          />
        </div>

        {/* Validation Errors/Status */}
        {parsed.errors.length > 0 ? (
          <div className="p-2.5 rounded-xl bg-red-900/40 border border-red-500/40 text-[10px] text-red-200 space-y-0.5">
            {parsed.errors.map((err, idx) => (
              <div key={idx} className="flex items-start gap-1">
                <span className="text-red-400 font-bold">•</span>
                <span>{err}</span>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-500/40 text-[10px] text-emerald-300 flex items-center justify-between">
            <span>Endpoint: {parsed.peer.endpoint}</span>
            <span className="font-mono">Allowed: {parsed.metadata.allowedRoutesSummary}</span>
          </div>
        )}

        {/* Import Action Button */}
        <button
          type="button"
          onClick={handleApply}
          disabled={!parsed.isValid}
          className="w-full h-11 rounded-full bg-gradient-to-r from-purple-500 via-indigo-600 to-cyan-500 hover:from-purple-400 hover:to-cyan-400 disabled:opacity-50 text-white font-bold text-xs tracking-wider uppercase shadow-md active:scale-98 transition-all flex items-center justify-center gap-2"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>{locale === 'ar' ? 'استيراد التكوين المنسوخ' : 'Import Configuration'}</span>
        </button>
      </div>
    </div>
  );
};
