import React, { useState, useEffect, useRef } from 'react';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { parseWireGuardConfig, ParsedWireGuardConfig } from '../../../util/config-parser';
import { X } from 'lucide-react';
import { GlassLoadingOverlay } from '../../common/GlassLoadingOverlay';

interface QRScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportSuccess: (config: ParsedWireGuardConfig) => void;
  locale: string;
}

export const QRScannerModal: React.FC<QRScannerModalProps> = ({
  isOpen,
  onClose,
  onImportSuccess,
  locale,
}) => {
  const [isInitializing, setIsInitializing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scannerRef = useRef<Html5QrcodeScanner | null>(null);

  useEffect(() => {
    if (isOpen) {
      setIsInitializing(true);
      setError(null);
      
      navigator.mediaDevices.getUserMedia({ video: true })
        .then(() => {
          setIsInitializing(false);
          const scanner = new Html5QrcodeScanner(
            "qr-reader",
            { fps: 10, qrbox: { width: 250, height: 250 } },
            false
          );
          
          scanner.render((decodedText) => {
            const parsed = parseWireGuardConfig(decodedText);
            if (parsed.isValid) {
              scanner.clear().catch(console.error);
              onImportSuccess(parsed);
              onClose();
            } else {
              setError(locale === 'ar' ? 'رمز QR غير صالح' : 'Invalid QR code');
            }
          }, (err) => {
            console.error(err);
          });
          
          scannerRef.current = scanner;
        })
        .catch((err) => {
          setIsInitializing(false);
          setError(locale === 'ar' ? 'تعذر الوصول للكاميرا' : 'Camera access denied');
          console.error(err);
        });
    }

    return () => {
      if (scannerRef.current) {
        scannerRef.current.clear().catch(console.error);
      }
    };
  }, [isOpen, onImportSuccess, onClose, locale]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
      {isInitializing && <GlassLoadingOverlay message={locale === 'ar' ? 'جارٍ تشغيل الكاميرا...' : 'Initializing Camera...'} />}
      <div className="relative w-full max-w-sm rounded-3xl bg-[#0f1334] border border-white/20 shadow-2xl text-white p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-white/10 pb-3">
          <h3 className="font-bold text-sm">{locale === 'ar' ? 'مسح QR' : 'Scan QR'}</h3>
          <button onClick={onClose}><X className="w-5 h-5" /></button>
        </div>
        {error && <div className="text-red-400 text-xs text-center">{error}</div>}
        <div id="qr-reader" className="w-full rounded-2xl overflow-hidden" />
      </div>
    </div>
  );
};
