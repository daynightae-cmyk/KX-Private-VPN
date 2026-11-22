# CORRECTION — 2026-11-22 (independent visual audit of the 2026-11-19 captures)

**Finding.** The six `*-ar.png` files committed on 2026-11-19 (PR #53) were **labelled by intended
route, not by actual content**. Each file was opened and read individually on 2026-11-22; the real
screens are:

| Committed filename (2026-11-19) | Claimed screen | **Actual screen (verified 2026-11-22)** | New filename |
|---|---|---|---|
| `01-home-ar.png` | Home | ✅ **Home** (`الرئيسية`, disconnected, orbital halo) | unchanged |
| `02-tools-ar.png` | Tools Catalog | ❌ **Security Center** (`مركز الأمان`) | `02-security-center-ar.png` |
| `03-repair-ar.png` | Repair Lab | ❌ **Statistics** (`الإحصاءات`, "الاستخدام اليومي") | `03-statistics-ar.png` |
| `04-settings-ar.png` | Settings | ❌ **Session Privacy** (`خصوصية الجلسات`) | `04-session-privacy-ar.png` |
| `05-security-ar.png` | Security | ❌ **Session Privacy** (`خصوصية الجلسات`) | `05-session-privacy-b-ar.png` |
| `06-statistics-ar.png` | Statistics | ❌ **Security Center** (`مركز الأمان`) | `06-security-center-b-ar.png` |

**Consequences (must be stated plainly):**

1. The 2026-11-19 set contains **no Tools Catalog capture and no Repair Lab capture**, even though
   PR #53's commit message and the ledger claim "6 truthful captures (Home, Tools, Repair, Settings,
   Security, Statistics) screen-verified via UIAutomator". That claim is **not supported** by the files.
2. The files are still **byte-distinct** (6 unique SHA-256 values), so the earlier "no duplicates"
   check was performed on bytes only — it does **not** validate the screen labels.
3. This is **not** an app navigation bug. The Compose route table is correct
   (`features/NativeClient.kt`: `route == "tools" → ToolsCatalogScreen`, `route == "security" →
   SecurityRoute`, `route == "stats" → StatisticsRoute`). The mismatch is a capture/labelling error
   in the evidence tooling, not a product defect.

**Action taken.** The five mislabelled files were **renamed to their true content** (bytes preserved,
SHA-256 unchanged) so the filenames no longer misrepresent the screens. The inaccurate labels in
PR #53's prose were **retracted** and superseded by this note and by
`docs/evidence/device-20261122/`.

**Still required for a real Ref 14/18/03 visual acceptance.** Correctly-labelled device captures of
the **Home, Tools Catalog and Repair Lab** screens on SM-A025F. Home is re-captured this session in
`../device-20261122/01-home-ar-disconnected.png`. Tools Catalog and Repair Lab remain
**VISUAL_UNVERIFIED** on device in this session.

> Rule reaffirmed: a filename, a distinct hash, a successful build or a green CI run are **not**
> visual evidence. Only an opened image whose content matches its label is.
