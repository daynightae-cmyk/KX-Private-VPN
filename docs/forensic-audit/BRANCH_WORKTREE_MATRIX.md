# BRANCH & WORKTREE MATRIX
**Generated:** 2026-11-15 11:00:00 UAE  
**Source:** `git branch -avv`, `git worktree list --porcelain`, `git log --graph --all`  

---

## WORKTREE TOPOLOGY

```
K:\KX-Private-VPN (MAIN WORKTREE)
├── .git (common dir)
├── worktrees/
│   ├── KX-Private-VPN-task07 → K:\KX-Private-VPN-task07
│   ├── KX-GUARDIAN-20261108 → K:\KX-GUARDIAN-20261108
│   ├── KX-NETWORK-REF02-20261108 → K:\KX-NETWORK-REF02-20261108
│   ├── KX-PR42-20261109 → K:\KX-PR42-20261109
│   ├── KX-REF16-NETWORK-20261111 → K:\KX-REF16-NETWORK-20261111
│   └── KX-REF17-FIX-20261110 → K:\KX-REF17-FIX-20261110
└── (17 total worktrees registered)
```

---

## BRANCH HIERARCHY (from `git log --graph --all --oneline -50`)

```
* d4fda7e (origin/main, origin/HEAD) Merge PR #49
|\
| * ccda186 (HEAD, feature/kx-reference-09-15-smart-repair-20261110) fix(autopilot)...
| * 3ed4fa6 feat(autopilot): Smart Repair (refs 09, 15)...
| | * 3aeda22 (origin/feature/kx-reference-16..., feature/kx-reference-16...) Merge PR #47
| | |\
| | | * be5c89a (origin/feature/kx-reference-17..., feature/kx-reference-17...) fix(media)...
| | | * bc23dd1 test(android): anchor Media Slim...
| | | * ce4a01b test(android): expect 350 combined tests...
| | | * 16dc38f merge(android): bring Storage Health into Media Slim...
| | | * 83e0549 Merge PR #46
| | | |\
| | | | * 01b4c8c (origin/feature/kx-reference-07..., feature/kx-reference-07...) merge(android)...
| | | | * 6669bb8 (origin/feature/kx-reference-08...) test(backend)...
| | | | * ...
| | | * 8af3428 Merge PR #45
| | | |\
| | | | * e2f234e feat(media): rebuild Media Slim...
| | | | * 84d7731 fix(storage): keep Arabic health labels...
| | | | * e73c2ef feat(storage): rebuild Storage Health...
| | | | * ...
| | | * bcae689 Merge PR #44
| | | |\
| | | | * 4139952 (origin/feature/kx-reference-06..., feature/kx-reference-06...) feat(android)...
| | | | * ...
| | * 8af3428 Merge PR #45 (already shown)
| * 9ebc779 (origin/docs/merge-record-20261108) docs(evidence)...
* b9fd077 Merge PR #38
* ...
```

---

## DETAILED BRANCH MATRIX

| Branch | Local SHA | Remote SHA | Ahead/Behind Main | Worktree | Status | Classification |
|--------|-----------|------------|-------------------|----------|--------|----------------|
| `main` | `8af3428` | `d4fda7e` | Behind 9 | `K:\KX-Private-VPN` (canonical) | **STALE** | `INTEGRATED_UNVERIFIED` |
| `feature/kx-reference-09-15-smart-repair-20261110` | `ccda186` | `ccda186` | Behind 1 | `K:\KX-Private-VPN` | **ACTIVE** | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-reference-16-network-results-20261111` | `3aeda22` | `3aeda22` | Behind 3 | `K:\KX-REF16-NETWORK-20261111` | Merged to main via PR #47 | `MERGED_UNVERIFIED` |
| `feature/kx-reference-17-media-results-20261110` | `be5c89a` | `be5c89a` | Behind 3 | `K:\KX-REF17-FIX-20261110` | Merged to main via PR #47 | `MERGED_UNVERIFIED` |
| `feature/kx-reference-07-storage-20261109` | `01b4c8c` | `01b4c8c` | Behind 5 | `D:\Temp\kx-ref07-storage` | Merged to main via PR #45 | `MERGED_UNVERIFIED` |
| `feature/kx-reference-08-19-download-rescue-20261109` | `6669bb8` | `6669bb8` | Behind 5 | (none) | Merged to main via PR #46 | `MERGED_UNVERIFIED` |
| `feature/kx-reference-06-11-duplicates-20261109` | `4139952` | `4139952` | Behind 5 | (none) | Merged to main via PR #44 | `MERGED_UNVERIFIED` |
| `feature/kx-dyad-cache-04-20-native-20261109` | `45cf798` | `45cf798` | Behind ? | `K:\KX-PR42-20261109` | CI fix for 324-test suite | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-freebuff-service-completion-20261108` | `e92b695` | `e92b695` | Behind ? | `D:\KX-FREEBUFF-SERVICES-20261108` | CI fix for 252-test suite | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-guardian-20261108` | `5becad5` | `218ef45` (origin) | Behind 12 | `K:\KX-GUARDIAN-20261108` | Guardian feature | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-reference-02-network-doctor-20261108` | `b63ba8f` | `b63ba8f` | Behind 5 | `K:\KX-NETWORK-REF02-20261108` | Network Doctor ref | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-repair-lab-20261108` | `36bb876` | `36bb876` | Behind ? | `D:\KX-REPAIR-20261108` | Repair Lab merge | `UNIQUE_NOT_INTEGRATED` |
| `feature/kx-service-specialization-20261108` | `faa308d` | `faa308d` | Behind ? | `D:\KX-SERVICES-20261108` | Service specialization | `UNIQUE_NOT_INTEGRATED` |
| `feat/live-vpn-pro-20261107` | `c0c5687` | `c0c5687` | Behind ? | `D:\KX-HARDEN-20261107` | Modern Home + Pro/VPN | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-07-advanced-network` | `b1c8828` | `b1c8828` | Ahead 2 | `K:\KX-Private-VPN-task07` | Glyph size fix | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-02-vpn-transport` | `fb8f8b3` | `f4cb157` | Ahead 2 (local) | (none) | Real WireGuard transport | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-05-security-center` | `5ce8002` | `5ce8002` | ? | (none) | Compact security rows | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-11-repair-lab` | `869744e` | `869744e` | ? | (none) | Repair Lab day accents | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-12-storage-xray` | `f5ae806` | `f5ae806` | ? | (none) | Media permission wait | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-13-ghost-hunter` | `bd0a343` | `bd0a343` | ? | (none) | Ghost dark mode tones | `UNIQUE_NOT_INTEGRATED` |
| `codex/visual-consumer-ui` | `d23e7c1` | `d23e7c1` | ? | (none) | Import action coloring | `UNIQUE_NOT_INTEGRATED` |
| `codex/native-onboarding-config` | `f68eb11` | `f68eb11` | ? | (none) | Test XML preservation | `UNIQUE_NOT_INTEGRATED` |
| `codex/task-00-native-cinematic` | `4879c81` | `4879c81` | ? | (none) | Arabic screen capture | `UNIQUE_NOT_INTEGRATED` |
| `fix/api35-emulator-stability` | `d4123d4` | `d4123d4` | ? | (none) | Disable IME occlusion | `UNIQUE_NOT_INTEGRATED` |
| `fix/production-visual-hardening-20261107` | `f62b916` | `f62b916` | ? | (none) | Visual clipping + stop block | `UNIQUE_NOT_INTEGRATED` |
| `fix/device-suite-robustness` | `d696fda` | `d696fda` | ? | (none) | Ghost Hunter authorized tree | `UNIQUE_NOT_INTEGRATED` |
| `fix/download-export-safety-20261108` | `95caded` | `95caded` | ? | `D:\KX-DOWNLOAD-SAFETY-20261108` | Download safety merge | `ACTIVE_AGENT` |
| `feature/kx-touch-to-enter-physical-20261106` | `ae5861e` | `ae5861e` | ? | `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106` | KX orb entry + glyphs | `ACTIVE_AGENT` |
| `integration/kx-consolidation-20261106` | `d2aa4e0` | `d2aa4e0` | ? | `D:\KX-CONSOLIDATE-20261106` | Consolidation + swangle GPU | `ACTIVE_AGENT` |
| `integration/qoder-freebuff-ui` | `d4e4220` | `d4e4220` | ? | `D:\KX-INTEGRATION-20261105/wt` | Service glyphs checkpoint | `ACTIVE_AGENT` |
| `server-switch-truth` | `87bdafc` | `87bdafc` | ? | (none) | Server switch follows tunnel | `UNIQUE_NOT_INTEGRATED` |
| `settings-truth-premium` | `022f64e` | `022f64e` | ? | (none) | Notification truth + premium | `UNIQUE_NOT_INTEGRATED` |
| `task14-duplicate-dna` | `fa99f88` | `fa99f88` | ? | (none) | Re-hash before deletion | `UNIQUE_NOT_INTEGRATED` |
| `task15-heavyweight-finder` | `b1dbcd1` | `b1dbcd1` | ? | (none) | TASK_15 evidence | `UNIQUE_NOT_INTEGRATED` |
| `task16-cache-audit` | `a384c05` | `a384c05` | ? | (none) | TASK_16 evidence | `UNIQUE_NOT_INTEGRATED` |
| `visual-reference-batch-1` | `8e5d92a` | `8e5d92a` | ? | (none) | Swangle renderer A/B | `UNIQUE_NOT_INTEGRATED` |
| `vpn-session-notification-control` | `2a9546f` | `2a9546f` | ? | (none) | API35 notification shade | `UNIQUE_NOT_INTEGRATED` |
| `app-uninstall-verification` | `95e6666` | `95e6666` | ? | (none) | Uninstall evidence | `UNIQUE_NOT_INTEGRATED` |
| `chore/preserve-unique-local-work-20261110` | `ee22ee4` | `ee22ee4` | ? | (none) | Preserve local work | `UNIQUE_NOT_INTEGRATED` |
| `dialog-localization-sweep` | `c2420e9` | `c2420e9` | ? | (none) | Localized copy | `UNIQUE_NOT_INTEGRATED` |
| `feat/session-shield-customer-ux` | `3a3fdd6` | `3a3fdd6` | ? | (none) | Session Shield privacy | `UNIQUE_NOT_INTEGRATED` |
| `feature/task21-repair-autopilot` | `dfe535c` | `dfe535c` | ? | (none) | Repair Autopilot real | `UNIQUE_NOT_INTEGRATED` |
| `feature/visual-overhaul-and-missing-tasks` | `2ca10bb` | `2ca10bb` | ? | (none) | API35 dialog evidence | `UNIQUE_NOT_INTEGRATED` |

---

## WORKTREE DETAILS (from `git worktree list --porcelain`)

| # | Path | HEAD SHA | Branch | Locked? | Detached? |
|---|------|----------|--------|---------|-----------|
| 1 | `K:\KX-Private-VPN` | `ccda186` | `feature/kx-reference-09-15-smart-repair-20261110` | No | No |
| 2 | `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106` | `ae5861e` | `feature/kx-touch-to-enter-physical-20261106` | No | No |
| 3 | `D:\KX-CONSOLIDATE-20261106` | `d2aa4e0` | `integration/kx-consolidation-20261106` | No | No |
| 4 | `D:\KX-DOWNLOAD-SAFETY-20261108` | `95caded` | `fix/download-export-safety-20261108` | No | No |
| 5 | `D:\KX-FREEBUFF-SERVICES-20261108` | `e92b695` | `feature/kx-freebuff-service-completion-20261108` | No | No |
| 6 | `D:\KX-HARDEN-20261107` | `c0c5687` | `feat/live-vpn-pro-20261107` | No | No |
| 7 | `D:\KX-INTEGRATION-20261105/wt` | `d4e4220` | `integration/qoder-freebuff-ui` | No | No |
| 8 | `D:\KX-REPAIR-20261108` | `36bb876` | `feature/kx-repair-lab-20261108` | No | No |
| 9 | `D:\KX-SERVICES-20261108` | `faa308d` | `feature/kx-service-specialization-20261108` | No | No |
| 10 | `D:\KX-TASK21-20261107` | `29bb492` | (detached) | No | **Yes** |
| 11 | `D:\Temp\kx-ref07-storage` | `01b4c8c` | `feature/kx-reference-07-storage-20261109` | No | No |
| 12 | `K:\KX-GUARDIAN-20261108` | `5becad5` | `feature/kx-guardian-20261108` | No | No |
| 13 | `K:\KX-NETWORK-REF02-20261108` | `b63ba8f` | `feature/kx-reference-02-network-doctor-20261108` | No | No |
| 14 | `K:\KX-PR42-20261109` | `45cf798` | `feature/kx-dyad-cache-04-20-native-20261109` | No | No |
| 15 | `K:\KX-Private-VPN-task07` | `b1c8828` | `codex/task-07-advanced-network` | No | No |
| 16 | `K:\KX-REF16-NETWORK-20261111` | `3aeda22` | `feature/kx-reference-16-network-results-20261111` | No | No |
| 17 | `K:\KX-REF17-FIX-20261110` | `be5c89a` | `feature/kx-reference-17-media-results-20261110` | No | No |

---

## MERGE BASE ANALYSIS

| Branch | Merge Base with Main | Commits Since Base | Conflict Risk |
|--------|---------------------|-------------------|---------------|
| `feature/kx-reference-09-15-smart-repair-20261110` | `3aeda22` (PR #47) | 2 (`ccda186`, `3ed4fa6`) | **LOW** — fast-forwardable |
| `feature/kx-guardian-20261108` | `8af3428` (PR #45) | ~20 | **HIGH** — 12 behind main, touches Guardian/Privacy |
| `feature/kx-dyad-cache-04-20-native-20261109` | `8af3428` | ~15 | **MEDIUM** — CI config changes |
| `feature/kx-freebuff-service-completion-20261108` | `8af3428` | ~12 | **MEDIUM** — CI config changes |
| `codex/task-02-vpn-transport` | `8af3428` | ~25 | **MEDIUM** — core VPN transport |
| `codex/task-07-advanced-network` | `8af3428` | ~8 | **LOW** — UI glyph fix only |

---

## INTEGRATION STRATEGY

### Phase 1: Immediate (No Conflicts)
1. `git checkout main && git pull --ff-only origin main` → Fast-forward to `d4fda7e`
2. Push local `codex/task-02-vpn-transport` (ahead 2) to origin
3. PR `fix/api35-emulator-stability` → main (CI fix only)
4. PR `fix/production-visual-hardening-20261107` → main (visual fix only)

### Phase 2: Visual Fidelity (Requires Coordination)
5. PR `codex/task-07-advanced-network` → main (glyph fix)
6. PR `codex/task-05-security-center` → main (compact rows)
7. PR `codex/task-11-repair-lab` → main (day accents)
8. PR `codex/task-12-storage-xray` → main (permission fix)
9. PR `codex/task-13-ghost-hunter` → main (dark tones)
10. PR `codex/visual-consumer-ui` → main (import coloring)

### Phase 3: Major Features (Rebase Required)
11. `feature/kx-dyad-cache-04-20-native-20261109` → rebase on main → PR
12. `feature/kx-freebuff-service-completion-20261108` → rebase on main → PR
13. `server-switch-truth` → rebase on main → PR
14. `settings-truth-premium` → rebase on main → PR
15. `feat/session-shield-customer-ux` → rebase on main → PR
16. `feature/task21-repair-autopilot` → rebase on main → PR

### Phase 4: Blocked (Active Agents)
17. `fix/download-export-safety-20261108` — **WAIT** for Agent_A on `D:\KX-DOWNLOAD-SAFETY-20261108`
18. `feature/kx-touch-to-enter-physical-20261106` — **WAIT** for Agent_C on `D:\KNOUX-Workspace\KX-PRIVATE-ENTRY-20261106`
19. `integration/kx-consolidation-20261106` — **WAIT** for Agent_A on `D:\KX-CONSOLIDATE-20261106`
20. `feature/kx-guardian-20261108` — **WAIT** for rebase (12 behind) + Agent_A coordination

---

**MATRIX COMPLETE — 46 BRANCHES, 17 WORKTREES, 7 ACTIVE AGENTS CATALOGUED**