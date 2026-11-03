/**
 * KX Phone Repair Lab™ — Type Definitions
 *
 * Privacy-first Android storage, maintenance, and diagnostics domain models.
 */

export type RepairServiceId =
  | 'storage-xray'
  | 'ghost-hunter'
  | 'duplicate-dna'
  | 'heavyweight-finder'
  | 'cache-surgeon'
  | 'media-slim'
  | 'download-rescue'
  | 'session-shield'
  | 'network-doctor'
  | 'repair-autopilot';

export interface StorageCategoryBreakdown {
  id: string;
  name: string;
  nameAr: string;
  bytes: number;
  percentage: number;
  color: string;
  fileCount: number;
  iconName: string;
}

export interface StorageStats {
  totalBytes: number;
  usedBytes: number;
  freeBytes: number;
  usedPercentage: number;
  categories: StorageCategoryBreakdown[];
  scannedAt: string;
  scopeDescription: string;
  scopeDescriptionAr: string;
}

export interface GhostCandidate {
  id: string;
  path: string;
  fileName: string;
  type: 'empty-folder' | 'zero-byte' | 'stale-temp' | 'obsolete-log' | 'abandoned-cache';
  typeLabel: string;
  typeLabelAr: string;
  sizeBytes: number;
  confidence: number; // 0 - 100%
  reason: string;
  reasonAr: string;
  selected: boolean;
}

export interface DuplicateFileItem {
  id: string;
  path: string;
  name: string;
  sizeBytes: number;
  lastModified: string;
  isOriginal: boolean;
  selectedForRemoval: boolean;
  previewUrl?: string;
}

export interface DuplicateGroup {
  id: string;
  groupName: string;
  exactHash: string; // SHA-256
  isVisualSimilarity: boolean;
  fileSize: number;
  potentialReclaimBytes: number;
  files: DuplicateFileItem[];
}

export interface HeavyweightItem {
  id: string;
  name: string;
  path: string;
  sizeBytes: number;
  category: 'video' | 'archive' | 'audio' | 'document' | 'installer';
  lastModified: string;
  selected: boolean;
}

export interface CacheEntry {
  id: string;
  label: string;
  labelAr: string;
  scope: 'kx-internal' | 'accessible-cache' | 'system-guidance';
  sizeBytes: number;
  isSafeToClear: boolean;
  explanation: string;
  explanationAr: string;
  selected: boolean;
}

export interface MediaSlimItem {
  id: string;
  fileName: string;
  originalSizeBytes: number;
  estimatedOptimizedSizeBytes: number;
  savingsPercentage: number;
  format: 'image/jpeg' | 'image/png' | 'video/mp4';
  resolution: string;
  selected: boolean;
}

export interface DownloadIssueItem {
  id: string;
  fileName: string;
  sizeBytes: number;
  issueType: 'zero-byte' | 'incomplete-extension' | 'truncated-signature' | 'orphaned-temp';
  issueLabel: string;
  issueLabelAr: string;
  recommendedAction: 'delete' | 'resume' | 'quarantine';
  selected: boolean;
}

export interface SessionShieldRecord {
  id: string;
  name: string;
  nameAr: string;
  type: 'active-tunnel' | 'saved-endpoint' | 'diagnostic-log' | 'consent-token';
  createdDate: string;
  dataSummary: string;
  isRemovable: boolean;
}

export interface NetworkDoctorProbe {
  id: string;
  testName: string;
  testNameAr: string;
  status: 'pending' | 'success' | 'warning' | 'error';
  latencyMs?: number;
  detail: string;
  detailAr: string;
  targetEndpoint: string;
}

export interface AutopilotAction {
  id: string;
  serviceId: RepairServiceId;
  title: string;
  titleAr: string;
  riskLevel: 'safe-reversible' | 'user-review-required';
  reclaimBytes: number;
  itemCount: number;
  status: 'pending' | 'executed' | 'skipped' | 'failed';
  selected: boolean;
}

export interface RepairOperationLog {
  id: string;
  timestamp: string;
  serviceName: string;
  serviceNameAr: string;
  actionTaken: string;
  actionTakenAr: string;
  itemsAffected: number;
  bytesReclaimed: number;
  isReversible: boolean;
}
