import { useState, useCallback, Dispatch, SetStateAction } from 'react';
import { StorageStats } from '../types/repair';
import { getInitialStorageStats } from '../utils/repair-engine';

/**
 * Emulated Android StatFs and StorageStatsManager hook.
 * Provides accurate block-level calculations, system reserved space,
 * available bytes for user, and async scan refresh capability.
 */
export interface StatFsMetrics {
  blockSize: number;
  totalBlocks: number;
  freeBlocks: number;
  availableBlocks: number;
  systemReservedBytes: number;
}

export const useRepairStats = (): [
  StorageStats,
  Dispatch<SetStateAction<StorageStats>>,
  {
    statFs: StatFsMetrics;
    refreshStats: () => void;
    isScanning: boolean;
  }
] => {
  const [stats, setStats] = useState<StorageStats>(() => {
    const initial = getInitialStorageStats();
    return initial;
  });

  const [isScanning, setIsScanning] = useState(false);

  // StatFs emulated block metrics (4KB standard Android block size)
  const blockSize = 4096;
  const systemReservedBytes = 9.8 * 1024 * 1024 * 1024; // ~9.8 GB system reserved
  const totalBlocks = Math.floor((stats.totalBytes + systemReservedBytes) / blockSize);
  const freeBlocks = Math.floor(stats.freeBytes / blockSize);
  const availableBlocks = Math.floor((stats.freeBytes * 0.92) / blockSize); // StatFs available for non-root

  const statFs: StatFsMetrics = {
    blockSize,
    totalBlocks,
    freeBlocks,
    availableBlocks,
    systemReservedBytes,
  };

  const refreshStats = useCallback(() => {
    setIsScanning(true);
    setTimeout(() => {
      setStats(getInitialStorageStats());
      setIsScanning(false);
    }, 600);
  }, []);

  return [stats, setStats, { statFs, refreshStats, isScanning }];
};
