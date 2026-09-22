# OmniDial Docs — Maintenance Guide

How the user documentation site is organized, and how to keep it up to date.
The site is plain HTML + one shared stylesheet. No build step, no framework —
anyone who can edit HTML can maintain it.

## Site map

```
<repo root>/
├── index.html            # Landing page: hero, screenshots, comparison,
│                         #   capabilities (with how-to links), latest release,
│                         #   install steps, FAQ teaser
├── docs/                 # All documentation lives here — moves as one unit
│   ├── styles.css        # THE stylesheet — shared by every page
│   ├── getting-started.html  # New-user guide: install → first calls → pro tips
│   ├── guides.html       # Hub listing all how-to guides
│   ├── faq.html          # Frequently asked questions
│   ├── releases.html     # Full version history (rendered from RELEASE.md)
│   ├── guides/
│   │   ├── keypad-dock.html  # Multi-channel calling engine + keypad dock
│   │   ├── gate-buzzer.html  # Recipe: apartment intercom auto-answer + DTMF
│   │   ├── dual-sim.html     # SIM modes, per-number SIM, roaming guard
│   │   ├── spam-protection.html  # Block list, auto-block presets, trust badges
│   │   └── backup-restore.html   # One-tap backup, what's saved, restore
│   └── screenshots/      # All doc screenshots (PNG) live here
├── documents/
│   └── RELEASE.md        # Single source of truth for release notes
└── apks/                 # Downloadable APKs — build artifacts, not docs
```

**Rule of thumb:** content lives in the HTML pages, looks live in `styles.css`,
release history lives in `documents/RELEASE.md`.

## The shared stylesheet

Every page links `<link rel="stylesheet" href="styles.css">` (pages in
`guides/` use `../styles.css`). Never paste a `<style>` block into a page —
add reusable styles to `styles.css` instead so all pages stay consistent.

Key classes you'll reuse:

| Class | Use |
|---|---|
| `.doc-hero` | Page title block (`h1` + intro `p`) under the breadcrumb |
| `.crumb` | Breadcrumb: Home / Section / Page |
| `.toc` | "On this page" box; anchor links to `section.guide` ids |
| `section.guide` + `.step-num` | Numbered how-to steps |
| `.snippet-row` | A step's visual: annotated phone mockup + explanation text |
| `.snippet-frame` / `.mock-row` / `.mock-btn` / `.mock-chip` | CSS phone mockup pieces |
| `.callout` | Numbered badge pinned on a mockup element — must sit inside a `position: relative` parent (`.mock-row`, `.mock-btn`, `.mock-topbar` all qualify) |
| `.hl` / `.hl-green` | Highlight ring on a mock row (blue = tap here, green = success state) |
| `.tip` / `.warn` | Green tip box / amber warning box |
| `.kbd` | Inline key/label chip, e.g. a dialer key or a folder path |
| `details.faq` | Expandable Q&A |
| `.guide-grid` / `.guide-card` | Cards linking to guides (hub page, index cross-links) |
| `.howto-link` | "📖 How-to guide →" pill used on index capability cards |

## Adding a new how-to guide

1. Copy `guides/backup-restore.html` — it's the shortest and shows the full
   page skeleton (header → breadcrumb → doc-hero → toc → sections → footer nav).
2. Save as `guides/<topic>.html`. Keep the header/nav/footer identical; only
   change the breadcrumb, hero text, and sections.
3. Write steps as `<section class="guide" id="...">` blocks with
   `<div class="step-num">N</div>`. Add each to the `.toc` list.
4. For each key tap in the app, add a `.snippet-row`: a `.snippet-frame`
   mockup on the left (highlight the tapped element with `.hl` + `.callout`),
   explanation on the right.
5. Add a card for it in `guides.html` and, if it maps to an index capability
   card, a `.howto-link` there too.
6. Number footer-nav prev/next links to fit the sequence.

## Updating screenshots

Real screenshots live in `docs/screenshots/` and are shown on `index.html`
(showcase + capability cards) via plain `<img src="docs/screenshots/<file>.png">`.

- **Refreshing:** retake on a current build, keep the same filename, and the
  site picks it up. Recommended: portrait, ~1080px wide PNG, dark mode on
  (matches the site theme).
- **Checklist per screenshot** (what the capture should show):
  - `panel_1_favorites.png` — Favorites grid with several starred contacts
  - `panel_2_keypad.png` — T9 keypad with suggestions visible
  - `panel_3_contacts.png` — Contacts directory with filter chips
  - `feature_contact_details.png` — Contact sheet: preferred channel + SIM pills
  - `feature_incoming_audio.png` — Incoming call with audio-route selector
  - `feature_gate_rules.png` — Rules screen with a gate-buzzer rule
  - `feature_unsaved_actions.png` — Keypad 4-action hub for an unsaved number
  - `feature_post_call_note.png` — Post-call note/reminder sheet
- The annotated **snippets** inside guides are pure CSS mockups, not images —
  edit them inline in the guide HTML (no design tool needed).

## Release workflow (docs side)

- **`documents/RELEASE.md` is the only file you edit** for release notes.
  Add a new `<details open>` card at the top, demote the previous latest to
  `<details>`. `releases.html` fetches and renders this file automatically —
  no HTML editing needed.
- On `index.html`, update the **latest release card** (version, build, date,
  APK link, 3 highlight bullets) and the `version-tag` in the header.
- Keep release notes **end-user friendly**: plain English, practical benefits,
  no class names or internal jargon (per repo `AGENTS.md` docs standard).
- The `releases.html` page strips everything after
  `## 2. GitHub Pages Deployment Steps` so maintainer-only sections never
  render publicly.

## Publishing

Copy `index.html` to the repo root and the `docs/` folder next to it
(GitHub Pages serves from root; `index.html` must stay at root).
If your repo still has the old root-level `screenshots/` folder, move it
into `docs/screenshots/` (see PowerShell below) — the pages now look there.
`releases.html` fetches `../documents/RELEASE.md` at runtime, so that file
must be deployed too (it stays at repo root).

```powershell
# one-time move on your PC, from the repo root:
Move-Item screenshots docs\screenshots -Force
```

## Style rules for all docs

- Plain, non-technical English. Name UI elements exactly as they appear in the
  app ("Recipes", "Spam & Blocked Calls", "Backup Now").
- Steps are numbered and start with the tap: "Open…", "Tap…", "Type…".
- One idea per bullet. No paragraph longer than 3 lines.
- Every guide ends with what success looks like ("What happens on a real call").
