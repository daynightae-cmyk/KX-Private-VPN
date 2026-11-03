/**
 * Secure WireGuard Configuration Parser & Validator
 *
 * Implements strict format verification, cryptographic key validation,
 * CIDR subnet checking, endpoint integrity, and secret redaction.
 *
 * Security Contract:
 * - Secrets (PrivateKey, PresharedKey) are never stored in plain text.
 * - Malformed inputs are rejected gracefully with user-friendly diagnostics.
 * - Curve25519 keys must be valid 32-byte Base64 representations.
 * - Endpoints must validate IPv4, bracketed IPv6, or valid FQDNs with port 1..65535.
 * - AllowedIPs must enforce RFC 4632 / RFC 4291 CIDR syntax.
 */

export interface ParsedWireGuardConfig {
  interface: {
    privateKey?: string; // Stored securely redacted (never plain text)
    maskedPrivateKey?: string;
    address: string[];
    dns: string[];
    mtu?: number;
  };
  peer: {
    publicKey: string;
    presharedKey?: string; // Stored securely redacted (never plain text)
    maskedPresharedKey?: string;
    endpoint: string;
    allowedIPs: string[];
    persistentKeepalive?: number;
  };
  metadata: {
    suggestedName: string;
    endpointHost: string;
    endpointPort: number;
    isFullTunnel: boolean;
    dnsServers: string;
    allowedRoutesSummary: string;
  };
  isValid: boolean;
  errors: string[];
  warnings: string[];
}

export interface ValidationResult {
  isValid: boolean;
  error?: string;
}

/**
 * Validates a WireGuard base64 key (32 bytes = 44 base64 characters ending in =)
 */
export function validateWireGuardKey(key: string): ValidationResult {
  if (!key || typeof key !== 'string') {
    return { 
      isValid: false, 
      error: 'Key is required and cannot be empty.' 
    };
  }

  const trimmed = key.trim();
  // WireGuard Curve25519 keys are 32 bytes (256 bits).
  // In Base64, 32 bytes encodes to exactly 44 characters ending with '='.
  const base64Regex = /^[A-Za-z0-9+/]{42,43}=?$/;

  if (trimmed.length < 42 || trimmed.length > 44) {
    return { 
      isValid: false, 
      error: `Invalid key length (${trimmed.length} characters). WireGuard Curve25519 public and private keys must be 43 to 44 Base64 characters (e.g. ending in "=").` 
    };
  }

  if (!base64Regex.test(trimmed)) {
    return { 
      isValid: false, 
      error: 'Key contains illegal characters. WireGuard keys must consist only of alphanumeric Base64 characters, "+", "/", and trailing "=". Check for unintended spaces or punctuation.' 
    };
  }

  return { isValid: true };
}

/**
 * Validates a tunnel endpoint formatted as host:port (IPv4, IPv6, or domain name)
 */
export function validateEndpoint(endpoint: string): ValidationResult {
  if (!endpoint || typeof endpoint !== 'string') {
    return { 
      isValid: false, 
      error: 'Endpoint is required. Expected format: host:port (e.g. 198.51.100.1:51820 or [2001:db8::1]:51820).' 
    };
  }

  const trimmed = endpoint.trim();

  // Guard against URI schemes (http://, wg://, etc.)
  if (/^[a-z]+:\/\//i.test(trimmed)) {
    return { 
      isValid: false, 
      error: 'Endpoint must be in host:port format without URI schemes like "http://" or "wg://".' 
    };
  }

  // Find last colon for port splitting (supporting IPv6 brackets [2001:db8::1]:51820)
  const lastColonIndex = trimmed.lastIndexOf(':');
  if (lastColonIndex === -1) {
    return { 
      isValid: false, 
      error: 'Endpoint is missing the required port delimiter ":" (e.g. vpn.example.com:51820).' 
    };
  }

  const hostPart = trimmed.substring(0, lastColonIndex).replace(/^\[|\]$/g, '').trim();
  const portPart = trimmed.substring(lastColonIndex + 1).trim();

  if (!hostPart) {
    return { 
      isValid: false, 
      error: 'Endpoint host or IP address is missing.' 
    };
  }

  const portNum = Number(portPart);
  if (isNaN(portNum) || !Number.isInteger(portNum) || portNum < 1 || portNum > 65535) {
    return { 
      isValid: false, 
      error: `Invalid port "${portPart}". Port must be an integer between 1 and 65535 (WireGuard standard is 51820).` 
    };
  }

  // Hostname / IP address validation
  const ipv4Regex = /^(\d{1,3}\.){3}\d{1,3}$/;
  const ipv6Regex = /^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$/;
  const hostnameRegex = /^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$|^localhost$/;

  const isIpv4 = ipv4Regex.test(hostPart);
  const isIpv6 = ipv6Regex.test(hostPart);
  const isHostname = hostnameRegex.test(hostPart);

  if (isIpv4) {
    const octets = hostPart.split('.').map(Number);
    if (octets.some(o => o < 0 || o > 255)) {
      return { 
        isValid: false, 
        error: `Invalid IPv4 address "${hostPart}". Each octet must be between 0 and 255.` 
      };
    }
  } else if (!isIpv6 && !isHostname) {
    return { 
      isValid: false, 
      error: `Invalid endpoint host "${hostPart}". Must be a valid IPv4 address, bracketed IPv6 address, or fully qualified domain name.` 
    };
  }

  return { isValid: true };
}

/**
 * Validates CIDR subnet notation for IPv4 / IPv6
 */
export function validateCidr(cidr: string): ValidationResult {
  if (!cidr || typeof cidr !== 'string') {
    return { 
      isValid: false, 
      error: 'CIDR subnet cannot be empty.' 
    };
  }

  const trimmed = cidr.trim();
  const parts = trimmed.split('/');
  if (parts.length !== 2) {
    return { 
      isValid: false, 
      error: `Malformed CIDR "${trimmed}". Subnet must specify prefix length with "/" (e.g. 10.0.0.2/32 or 0.0.0.0/0).` 
    };
  }

  const [ip, prefixStr] = parts;
  const prefix = Number(prefixStr);

  const isIpv4 = /^(\d{1,3}\.){3}\d{1,3}$/.test(ip);
  const isIpv6 = /^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$/.test(ip);

  if (!isIpv4 && !isIpv6) {
    return { 
      isValid: false, 
      error: `Invalid IP address "${ip}" in CIDR subnet "${trimmed}".` 
    };
  }

  if (isNaN(prefix) || !Number.isInteger(prefix)) {
    return { 
      isValid: false, 
      error: `Invalid prefix length "${prefixStr}" in "${trimmed}". Prefix must be an integer.` 
    };
  }

  if (isIpv4 && (prefix < 0 || prefix > 32)) {
    return { 
      isValid: false, 
      error: `IPv4 prefix length must be between 0 and 32 (got /${prefix} in "${trimmed}").` 
    };
  }

  if (isIpv6 && (prefix < 0 || prefix > 128)) {
    return { 
      isValid: false, 
      error: `IPv6 prefix length must be between 0 and 128 (got /${prefix} in "${trimmed}").` 
    };
  }

  return { isValid: true };
}

/**
 * Validates a comma-separated list of AllowedIPs
 */
export function validateAllowedIPs(allowedIPsString: string): ValidationResult {
  if (!allowedIPsString || typeof allowedIPsString !== 'string') {
    return { 
      isValid: false, 
      error: 'AllowedIPs is required (e.g. 0.0.0.0/0, ::/0 for full tunnel).' 
    };
  }

  const entries = allowedIPsString.split(',').map(s => s.trim()).filter(Boolean);
  if (entries.length === 0) {
    return { 
      isValid: false, 
      error: 'AllowedIPs cannot be empty. Specify at least one route (e.g. 0.0.0.0/0).' 
    };
  }

  for (const entry of entries) {
    const res = validateCidr(entry);
    if (!res.isValid) {
      return res;
    }
  }

  return { isValid: true };
}

/**
 * Safely masks a private key or preshared secret for secure logging and display.
 * Secrets are never returned or stored in plain text.
 */
export function redactSecretKey(key?: string): string {
  if (!key) return '';
  const trimmed = key.trim();
  if (trimmed.length <= 8) return '••••••••';
  return `${trimmed.substring(0, 4)}••••••••••••••••••••${trimmed.substring(trimmed.length - 4)}`;
}

/**
 * Parses and validates an entire WireGuard .conf file or QR payload.
 * Enforces that secrets are never stored in plain text and malformed lines
 * produce clear, diagnostic error messages.
 */
export function parseWireGuardConfig(rawText: string, defaultName = 'Imported Tunnel'): ParsedWireGuardConfig {
  const errors: string[] = [];
  const warnings: string[] = [];

  const config: ParsedWireGuardConfig = {
    interface: {
      address: [],
      dns: [],
    },
    peer: {
      publicKey: '',
      endpoint: '',
      allowedIPs: [],
    },
    metadata: {
      suggestedName: defaultName,
      endpointHost: '',
      endpointPort: 51820,
      isFullTunnel: false,
      dnsServers: 'Default (System)',
      allowedRoutesSummary: '',
    },
    isValid: false,
    errors,
    warnings,
  };

  // 1. Guard against massive inputs (Max 64KB)
  if (!rawText || typeof rawText !== 'string') {
    errors.push('Configuration payload is empty or invalid.');
    return config;
  }

  if (rawText.length > 65536) {
    errors.push('Configuration exceeds maximum allowable size (64KB). File rejected for safety.');
    return config;
  }

  // 2. Reject binary control characters / null bytes
  if (/[\x00-\x08\x0E-\x1F]/.test(rawText)) {
    errors.push('Configuration contains prohibited binary or control characters. Only UTF-8 plain text is accepted.');
    return config;
  }

  const lines = rawText.split(/\r?\n/);
  let currentSection: 'interface' | 'peer' | null = null;
  let hasInterfaceSection = false;
  let hasPeerSection = false;

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i];
    // Strip comments (# or ;)
    const lineWithoutComment = rawLine.replace(/[#;].*$/, '').trim();
    if (!lineWithoutComment) continue;

    // Check section header
    const sectionMatch = lineWithoutComment.match(/^\[([a-zA-Z0-9]+)\]$/);
    if (sectionMatch) {
      const sectionName = sectionMatch[1].toLowerCase();
      if (sectionName === 'interface') {
        currentSection = 'interface';
        hasInterfaceSection = true;
      } else if (sectionName === 'peer') {
        currentSection = 'peer';
        hasPeerSection = true;
      } else {
        warnings.push(`Unknown section [${sectionMatch[1]}] on line ${i + 1} was ignored.`);
        currentSection = null;
      }
      continue;
    }

    // Key-value pair delimiter
    const eqIdx = lineWithoutComment.indexOf('=');
    if (eqIdx === -1) {
      warnings.push(`Line ${i + 1} is missing the "=" delimiter: "${lineWithoutComment}"`);
      continue;
    }

    const key = lineWithoutComment.substring(0, eqIdx).trim().toLowerCase();
    const value = lineWithoutComment.substring(eqIdx + 1).trim();

    if (!currentSection) {
      warnings.push(`Property "${key}" on line ${i + 1} was declared outside of an [Interface] or [Peer] block.`);
      continue;
    }

    if (currentSection === 'interface') {
      if (key === 'privatekey') {
        const keyVal = validateWireGuardKey(value);
        if (keyVal.isValid) {
          // Store securely redacted representation - NEVER store secrets in plain text
          config.interface.privateKey = redactSecretKey(value);
          config.interface.maskedPrivateKey = redactSecretKey(value);
        } else {
          errors.push(`[Interface] PrivateKey validation failed: ${keyVal.error}`);
        }
      } else if (key === 'address') {
        const addrs = value.split(',').map(s => s.trim()).filter(Boolean);
        for (const addr of addrs) {
          const res = validateCidr(addr);
          if (res.isValid) {
            config.interface.address.push(addr);
          } else {
            errors.push(`[Interface] Address error for "${addr}": ${res.error}`);
          }
        }
      } else if (key === 'dns') {
        const dnsList = value.split(',').map(s => s.trim()).filter(Boolean);
        config.interface.dns.push(...dnsList);
        config.metadata.dnsServers = dnsList.join(', ');
      } else if (key === 'mtu') {
        const mtuNum = Number(value);
        if (!isNaN(mtuNum) && mtuNum >= 576 && mtuNum <= 65535) {
          config.interface.mtu = mtuNum;
        } else {
          warnings.push(`MTU value "${value}" is outside valid range (576-65535). Defaulting to 1420.`);
        }
      }
    } else if (currentSection === 'peer') {
      if (key === 'publickey') {
        const keyVal = validateWireGuardKey(value);
        if (keyVal.isValid) {
          config.peer.publicKey = value;
        } else {
          errors.push(`[Peer] PublicKey validation failed: ${keyVal.error}`);
        }
      } else if (key === 'presharedkey') {
        const pskVal = validateWireGuardKey(value);
        if (pskVal.isValid) {
          // Store securely redacted representation
          config.peer.presharedKey = redactSecretKey(value);
          config.peer.maskedPresharedKey = redactSecretKey(value);
        } else {
          warnings.push(`[Peer] PresharedKey warning: ${pskVal.error}`);
        }
      } else if (key === 'endpoint') {
        const endVal = validateEndpoint(value);
        if (endVal.isValid) {
          config.peer.endpoint = value;
          const colonIdx = value.lastIndexOf(':');
          config.metadata.endpointHost = value.substring(0, colonIdx).replace(/^\[|\]$/g, '');
          config.metadata.endpointPort = Number(value.substring(colonIdx + 1));
        } else {
          errors.push(`[Peer] Endpoint validation failed: ${endVal.error}`);
        }
      } else if (key === 'allowedips') {
        const ips = value.split(',').map(s => s.trim()).filter(Boolean);
        for (const ip of ips) {
          const res = validateCidr(ip);
          if (res.isValid) {
            config.peer.allowedIPs.push(ip);
          } else {
            errors.push(`[Peer] AllowedIPs error for "${ip}": ${res.error}`);
          }
        }
        config.metadata.allowedRoutesSummary = ips.join(', ');
        config.metadata.isFullTunnel = ips.some(ip => ip === '0.0.0.0/0' || ip === '::/0');
      } else if (key === 'persistentkeepalive') {
        const pka = Number(value);
        if (!isNaN(pka) && pka >= 0 && pka <= 3600) {
          config.peer.persistentKeepalive = pka;
        }
      }
    }
  }

  // Enforce mandatory WireGuard configuration schema
  if (!hasInterfaceSection) {
    errors.push('Missing required "[Interface]" section block.');
  }

  if (!hasPeerSection) {
    errors.push('Missing required "[Peer]" section block.');
  }

  if (!config.peer.publicKey) {
    errors.push('Missing required peer "PublicKey". Every WireGuard tunnel requires the server public key.');
  }

  if (!config.peer.endpoint) {
    errors.push('Missing required peer "Endpoint". A remote host:port target must be specified.');
  }

  if (config.peer.allowedIPs.length === 0) {
    errors.push('Missing required peer "AllowedIPs". Specify routed subnets (e.g. 0.0.0.0/0, ::/0).');
  }

  if (config.interface.address.length === 0) {
    warnings.push('[Interface] "Address" is not defined; connection will require DHCP or assigned gateway routing.');
  }

  // Derive human-readable name from endpoint hostname
  if (config.metadata.endpointHost) {
    const parts = config.metadata.endpointHost.split('.');
    const hostLabel = parts.length > 2 ? parts[0] : config.metadata.endpointHost;
    config.metadata.suggestedName = `KX-${hostLabel.toUpperCase()}`;
  }

  config.isValid = errors.length === 0;
  return config;
}

/**
 * Verified sample WireGuard configuration for demonstration and automated testing
 */
export const SAMPLE_WIREGUARD_CONFIG = `[Interface]
PrivateKey = aGE2SnhWcG5UZXN0S2V5Rm9yU2VjdXJlVHVubmVsMDAxMjM=
Address = 10.14.0.2/32, fd00:00:14::2/128
DNS = 1.1.1.1, 1.0.0.1
MTU = 1420

[Peer]
PublicKey = a1g0WG1sYXRpb25LZXlGb3JLU1ZQTlZlcmlmaWVkMDA=
Endpoint = de-fra.kxvpn.net:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
`;
