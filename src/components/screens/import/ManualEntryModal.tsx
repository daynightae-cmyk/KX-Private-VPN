import React, { useState } from 'react';
import { 
  validateEndpoint, 
  validateWireGuardKey, 
  validateAllowedIPs, 
  validateCidr,
  ParsedWireGuardConfig 
} from '../../../util/config-parser';
import { Sliders, X, CheckCircle2, AlertTriangle, ShieldCheck } from 'lucide-react';

interface ManualEntryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImportSuccess: (config: ParsedWireGuardConfig) => void;
  locale: string;
}

export const ManualEntryModal: React.FC<ManualEntryModalProps> = ({
  isOpen,
  onClose,
  onImportSuccess,
  locale,
}) => {
  const [tunnelName, setTunnelName] = useState('KX-Manual-Tunnel');
  const [endpoint, setEndpoint] = useState('vpn.kx-network.net:51820');
  const [publicKey, setPublicKey] = useState('a1g0WG1sYXRpb25LZXlGb3JLU1ZQTlZlcmlmaWVkMDA=');
  const [address, setAddress] = useState('10.14.0.2/32');
  const [allowedIPs, setAllowedIPs] = useState('0.0.0.0/0, ::/0');
  const [dns, setDns] = useState('1.1.1.1, 1.0.0.1');
  const [mtu, setMtu] = useState(1420);
  const [keepalive, setKeepalive] = useState(25);

  const [formErrors, setFormErrors] = useState<string[]>([]);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const errors: string[] = [];

    // 1. Validate Tunnel Name
    if (!tunnelName.trim()) {
      errors.push('Tunnel Name is required.');
    }

    // 2. Validate Endpoint
    const endRes = validateEndpoint(endpoint);
    if (!endRes.isValid) {
      errors.push(endRes.error || 'Invalid Endpoint.');
    }

    // 3. Validate Public Key
    const keyRes = validateWireGuardKey(publicKey);
    if (!keyRes.isValid) {
      errors.push(keyRes.error || 'Invalid Public Key.');
    }

    // 4. Validate Address CIDR
    const addrRes = validateCidr(address);
    if (!addrRes.isValid) {
      errors.push(`Interface Address: ${addrRes.error}`);
    }

    // 5. Validate Allowed IPs
    const ipsRes = validateAllowedIPs(allowedIPs);
    if (!ipsRes.isValid) {
      errors.push(ipsRes.error || 'Invalid AllowedIPs.');
    }

    if (errors.length > 0) {
      setFormErrors(errors);
      return;
    }

    setFormErrors([]);

    const colonIdx = endpoint.lastIndexOf(':');
    const endpointHost = endpoint.substring(0, colonIdx).replace(/^\[|\]$/g, '');
    const endpointPort = Number(endpoint.substring(colonIdx + 1));

    const parsedConfig: ParsedWireGuardConfig = {
      interface: {
        address: [address.trim()],
        dns: dns.split(',').map(s => s.trim()).filter(Boolean),
        mtu,
      },
      peer: {
        publicKey: publicKey.trim(),
        endpoint: endpoint.trim(),
        allowedIPs: allowedIPs.split(',').map(s => s.trim()).filter(Boolean),
        persistentKeepalive: keepalive,
      },
      metadata: {
        suggestedName: tunnelName.trim(),
        endpointHost,
        endpointPort,
        isFullTunnel: allowedIPs.includes('0.0.0.0/0'),
        dnsServers: dns,
        allowedRoutesSummary: allowedIPs,
      },
      isValid: true,
      errors: [],
      warnings: [],
    };

    onImportSuccess(parsedConfig);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-sm rounded-3xl bg-[#0f1334] border border-white/20 shadow-2xl text-white overflow-hidden p-5 space-y-3.5 max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-2.5 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-indigo-500 to-cyan-500 flex items-center justify-center text-white shadow-xs">
              <Sliders className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-white">
                {locale === 'ar' ? 'الإدخال اليدوي لتكوين WireGuard' : 'Manual WireGuard Entry'}
              </h3>
              <p className="text-[10px] text-indigo-200/70">
                {locale === 'ar' ? 'معلمات التشفير وتعيين المسارات' : 'Validated Cryptographic Parameters'}
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto pr-1 space-y-2.5 text-xs">
          {/* Tunnel Name */}
          <div>
            <label className="block text-[11px] font-bold text-indigo-200 mb-1">
              {locale === 'ar' ? 'اسم التكوين (Nickname):' : 'Configuration Nickname:'}
            </label>
            <input 
              type="text"
              value={tunnelName}
              onChange={(e) => setTunnelName(e.target.value)}
              className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-3 text-xs text-white focus:outline-none focus:border-cyan-400"
              required
            />
          </div>

          {/* Endpoint */}
          <div>
            <label className="block text-[11px] font-bold text-indigo-200 mb-1">
              {locale === 'ar' ? 'نقطة النهاية (Endpoint host:port):' : 'Peer Endpoint (host:port):'}
            </label>
            <input 
              type="text"
              value={endpoint}
              onChange={(e) => setEndpoint(e.target.value)}
              placeholder="e.g. 198.51.100.1:51820 or de.vpn.com:51820"
              className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-3 text-xs text-cyan-300 font-mono focus:outline-none focus:border-cyan-400"
              required
            />
          </div>

          {/* Peer Public Key */}
          <div>
            <label className="block text-[11px] font-bold text-indigo-200 mb-1">
              {locale === 'ar' ? 'المفتاح العام (44 حرف Base64):' : 'Peer Public Key (Base64):'}
            </label>
            <input 
              type="text"
              value={publicKey}
              onChange={(e) => setPublicKey(e.target.value)}
              placeholder="44-character Base64 string ending in ="
              className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-3 text-xs text-cyan-300 font-mono focus:outline-none focus:border-cyan-400"
              required
            />
          </div>

          {/* Interface Address */}
          <div>
            <label className="block text-[11px] font-bold text-indigo-200 mb-1">
              {locale === 'ar' ? 'عنوان واجهة العميل (Address CIDR):' : 'Client Interface Address (CIDR):'}
            </label>
            <input 
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="10.0.0.2/32"
              className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-3 text-xs text-white font-mono focus:outline-none focus:border-cyan-400"
              required
            />
          </div>

          {/* Allowed IPs */}
          <div>
            <label className="block text-[11px] font-bold text-indigo-200 mb-1">
              {locale === 'ar' ? 'المسارات المسموحة (AllowedIPs):' : 'Allowed IPs (0.0.0.0/0 for full tunnel):'}
            </label>
            <input 
              type="text"
              value={allowedIPs}
              onChange={(e) => setAllowedIPs(e.target.value)}
              className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-3 text-xs text-white font-mono focus:outline-none focus:border-cyan-400"
              required
            />
          </div>

          {/* DNS & MTU */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[10px] font-bold text-indigo-200 mb-1">DNS:</label>
              <input 
                type="text"
                value={dns}
                onChange={(e) => setDns(e.target.value)}
                className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-2.5 text-xs text-white font-mono focus:outline-none focus:border-cyan-400"
              />
            </div>
            <div>
              <label className="block text-[10px] font-bold text-indigo-200 mb-1">MTU:</label>
              <input 
                type="number"
                value={mtu}
                onChange={(e) => setMtu(Number(e.target.value))}
                min={1280}
                max={1500}
                className="w-full h-8 rounded-xl bg-white/5 border border-white/15 px-2.5 text-xs text-white font-mono focus:outline-none focus:border-cyan-400"
              />
            </div>
          </div>

          {/* Validation Errors Disclosures */}
          {formErrors.length > 0 && (
            <div className="p-2.5 rounded-xl bg-red-900/40 border border-red-500/40 text-[10px] text-red-200 space-y-0.5">
              {formErrors.map((err, idx) => (
                <div key={idx} className="flex items-start gap-1">
                  <span className="text-red-400 font-bold">•</span>
                  <span>{err}</span>
                </div>
              ))}
            </div>
          )}

          {/* Submit */}
          <button
            type="submit"
            className="w-full h-10 rounded-full bg-gradient-to-r from-cyan-400 via-indigo-500 to-purple-600 hover:from-cyan-300 hover:to-purple-500 text-white font-bold text-xs tracking-wider uppercase shadow-md active:scale-98 transition-all flex items-center justify-center gap-1.5 mt-2"
          >
            <ShieldCheck className="w-4 h-4" />
            <span>{locale === 'ar' ? 'حفظ وتثبيت التكوين' : 'Save Validated Tunnel'}</span>
          </button>
        </form>
      </div>
    </div>
  );
};
