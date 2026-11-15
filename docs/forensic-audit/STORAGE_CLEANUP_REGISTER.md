# STORAGE CLEANUP REGISTER (REVIEW ONLY — NO DELETES)
**Generated:** 2026-11-15 12:15:00 UAE  
**Policy:** **NO DELETIONS AUTHORIZED** — This is a proposal register only. All cleanup requires explicit owner approval.  

---

## DISK SPACE SNAPSHOT (2026-11-15 10:47)

| Volume | Free | Total | % Free | Critical? |
|--------|------|-------|--------|-----------|
| **C:** | 6.50 GB | 130.3 GB | 5.0% | **YES** — System drive |
| **D:** | 1.26 GB | 62.9 GB | 2.0% | **YES** — Worktrees, Gradle, SDK |
| **K:** | 9.74 GB | 51.5 GB | 18.9% | OK — Canonical repo |

---

## STORAGE CONSUMERS (K:\KX-Private-VPN)

| Path | Size | Category | Regenerable? | Unique? | Action |
|------|------|----------|--------------|---------|--------|
| `android/build/` | ~6 MB | Build output | ✅ Yes | ❌ No | `gradle clean` (if no active daemon) |
| `android/.gradle/` | ~500 MB | Gradle cache | ✅ Yes | ❌ No | **ACTIVE DAEMON** — DO NOT CLEAN |
| `android/.kotlin/` | ~200 MB | Kotlin cache | ✅ Yes | ❌ No | **ACTIVE DAEMON** — DO NOT CLEAN |
| `android/app/build/` | ~100 MB | App build output | ✅ Yes | ❌ No | `gradle clean` |
| `docs/evidence/` | ~2.3 GB | CI/Phone captures | ❌ No | ✅ **YES** | **PRESERVE** |
| `docs/evidence/task16/phone-rollback/installed-base.apk` | 29 MB | APK backup | ❌ No | ✅ **YES** | **PRESERVE** |
| `docs/evidence/task21/phone-rollback/installed-base.apk` | 29 MB | APK backup | ❌ No | ✅ **YES** | **PRESERVE** |
| `.idea/` | ~50 MB | IDE config | ✅ Yes | ❌ No | Safe to delete (regenerates) |
| `node_modules/` | ~200 MB | Web prototype | ✅ Yes | ❌ No | **NOT NEEDED FOR ANDROID** |
| `dist/` | ~1 MB | Web build output | ✅ Yes | ❌ No | **NOT NEEDED FOR ANDROID** |
| `public/assets/aistudio/` | ~15 MB | Owner reference PNGs | ❌ No | ✅ **YES** | **PRESERVE** |
| `src/` (web) | ~5 MB | Web prototype source | ❌ No | ✅ **YES** | **PRESERVE** (audit trail) |
| `backend/` | ~10 MB | Pro backend (Python) | ❌ No | ✅ **YES** | **PRESERVE** |

---

## WORKTREE STORAGE (External to Canonical Repo)

| Worktree | Est. Size | Location | Status |
|----------|-----------|----------|--------|
| `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106` | ~500 MB | D: | Active (Agent_C) |
| `D:\KX-CONSOLIDATE-20261106` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-DOWNLOAD-SAFETY-20261108` | ~500 MB | D: | **Active (Agent_A)** — DO NOT TOUCH |
| `D:\KX-FREEBUFF-SERVICES-20261108` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-HARDEN-20261107` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-INTEGRATION-20261105/wt` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-REPAIR-20261108` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-SERVICES-20261108` | ~500 MB | D: | Active (Agent_A) |
| `D:\KX-TASK21-20261107` | ~500 MB | D: | Active (Agent_A) |
| `D:\Temp\kx-ref07-storage` | ~500 MB | D: | Active (Agent_A) |
| `K:\KX-GUARDIAN-20261108` | ~500 MB | K: | Active (Agent_A) |
| `K:\KX-NETWORK-REF02-20261108` | ~500 MB | K: | Active (Agent_A) |
| `K:\KX-PR42-20261109` | ~500 MB | K: | Active (Agent_A) |
| `K:\KX-Private-VPN-task07` | ~500 MB | K: | Active (Agent_A) |
| `K:\KX-REF16-NETWORK-20261111` | ~500 MB | K: | Active (Agent_A) |
| `K:\KX-REF17-FIX-20261110` | ~500 MB | K: | Active (Agent_A) |

**Total Worktree Estimate:** ~8 GB across D: and K:

---

## ANDROID SDK / GRADLE GLOBAL CACHES

| Path | Est. Size | Location | Shared? |
|------|-----------|----------|---------|
| Android SDK | ~15 GB | `%LOCALAPPDATA%\Android\Sdk` | Yes (all worktrees) |
| Gradle User Home | ~2 GB | `%USERPROFILE%\.gradle` | Yes (all daemons) |
| Kotlin Cache | ~500 MB | `%USERPROFILE%\.konan` | Yes |
| Emulator AVDs | ~5-10 GB each | `%USERPROFILE%\.android\avd` | Yes |

**AVD `kx_preview.avd`**: **UNVERIFIED** — Must be preserved if exists.

---

## CLEANUP PROPOSALS (REQUIRES AUTHORIZATION)

### Safe to Clean (Regenerable, No Active Use)
| Target | Command | Est. Recovery | Risk |
|--------|---------|---------------|------|
| `node_modules/` | `rm -rf node_modules` | ~200 MB | **LOW** — Web prototype only |
| `dist/` | `rm -rf dist` | ~1 MB | **LOW** — Web build output |
| `.idea/` (canonical only) | `rm -rf .idea` | ~50 MB | **LOW** — Regenerates on IDE open |

### Conditional (Requires Daemon Stop)
| Target | Command | Est. Recovery | Blocker |
|--------|---------|---------------|---------|
| `android/build/` | `./gradlew clean` | ~6 MB | **Gradle daemons active (PID 8852, 16188)** |
| `android/app/build/` | `./gradlew :app:clean` | ~100 MB | **Gradle daemons active** |
| `android/.gradle/` | `rm -rf android/.gradle` | ~500 MB | **Gradle daemons active** |
| `android/.kotlin/` | `rm -rf android/.kotlin` | ~200 MB | **Gradle daemons active** |

### DO NOT CLEAN (Unique Evidence / Active Work)
| Target | Reason |
|--------|--------|
| `docs/evidence/` | 2.3 GB unique CI/phone captures — irreplaceable |
| `public/assets/aistudio/` | Owner reference images — immutable |
| `src/` (web) | Audit trail for visual reference |
| `backend/` | Pro backend source |
| All worktrees | Active agents, unique contributions |
| `android/hs_err_pid*.log` | Crash evidence |
| `android/replay_pid*.log` | Crash evidence |
| AVD images | Emulator state |

---

## GRADLE DAEMON MANAGEMENT

### Current State
- **PID 8852**: 1.6 GB heap, started 2026-11-15 09:49 — **ACTIVE BUILD**
- **PID 16188**: Unknown heap, started earlier — **ACTIVE**

### Safe Daemon Stop Procedure (Requires Coordination)
```bash
# 1. Identify owner worktree
# 2. Confirm no active build in that worktree
# 3. Stop daemon from owner's worktree context
cd /path/to/owner/worktree
./gradlew --stop

# 4. Verify stopped
jps -l | grep GradleDaemon
```

**DO NOT RUN `gradle --stop` FROM THIS SESSION** — Will kill Agent_A's active build.

---

## CLEANUP EXECUTION LOG (EMPTY — NO ACTIONS TAKEN)

| Date | Action | Target | Bytes Freed | Authorized By |
|------|--------|--------|-------------|---------------|
| — | — | — | — | — |

---

## RECOMMENDATION

**DO NOT EXECUTE ANY CLEANUP** until:
1. Agent_A coordination complete (Gradle daemons, worktree ownership)
2. Owner explicit approval for each target
3. `docs/evidence/` backed up to external drive (2.3 GB)
4. AVD `kx_preview.avd` verified and backed up if exists

**Current disk pressure on D: (1.26 GB) is concerning but worktrees are active.** Prioritize Agent_A handoff over cleanup.

---

**REGISTER COMPLETE — REVIEW ONLY. NO DELETIONS PERFORMED. ALL UNIQUE EVIDENCE PROTECTED.**