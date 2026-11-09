---
name: IncidentIQ
description: Light editorial AI incident management workspace anchored by Serko's sky-to-horizon aura and crisp pill geometry.
colors:
  canvas-pure: "#FFFFFF"
  frost-surface: "#F6F6F6"
  sky-mist: "#E8F3FE"
  sky-zenith: "#E5F2FF"
  sky-mid: "#96CBFA"
  aura-violet: "#E388F2"
  aura-rose: "#F489AD"
  aura-coral: "#FF897F"
  aura-ember: "#FF8C51"
  ink-primary: "#000000"
  ink-secondary: "#4D4D4D"
  ink-muted: "#71717A"
  ink-dim: "#9CA3AF"
  horizon-blue: "#1B98F2"
  horizon-purple: "#CF69E0"
  horizon-pink: "#F56293"
  positive-mint: "#C0F2B5"
  positive-forest: "#17510B"
  positive-signal: "#158142"
  alert-red: "#F13931"
  alert-warning: "#EA580C"
  border-hairline: "#E5E7EB"
typography:
  display:
    fontFamily: "Plus Jakarta Sans, Segoe UI, Helvetica Neue, sans-serif"
    fontSize: "54px"
    fontWeight: 700
    lineHeight: 1.08
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Plus Jakarta Sans, Segoe UI, Helvetica Neue, sans-serif"
    fontSize: "36px"
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: "-0.015em"
  title:
    fontFamily: "Plus Jakarta Sans, Segoe UI, Helvetica Neue, sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.25
    letterSpacing: "-0.01em"
  body:
    fontFamily: "Plus Jakarta Sans, Segoe UI, Helvetica Neue, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "normal"
  label:
    fontFamily: "Plus Jakarta Sans, Segoe UI, Helvetica Neue, sans-serif"
    fontSize: "11.5px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.9px"
rounded:
  sm: "10px"
  md: "16px"
  card: "22px"
  hero-panel: "26px"
  pill: "999px"
spacing:
  xs: "8px"
  sm: "12px"
  md: "18px"
  lg: "28px"
  section: "84px"
components:
  button-primary:
    backgroundColor: "{colors.ink-primary}"
    textColor: "{colors.canvas-pure}"
    rounded: "{rounded.pill}"
    padding: "10px 22px"
  button-secondary:
    backgroundColor: "{colors.canvas-pure}"
    textColor: "{colors.ink-primary}"
    rounded: "{rounded.pill}"
    padding: "10px 22px"
  button-rainbow:
    backgroundColor: "{colors.canvas-pure}"
    textColor: "{colors.ink-primary}"
    rounded: "{rounded.pill}"
    padding: "10px 22px"
  input-field:
    backgroundColor: "{colors.frost-surface}"
    textColor: "{colors.ink-primary}"
    rounded: "{rounded.sm}"
    padding: "10px 14px"
  badge-positive:
    backgroundColor: "{colors.positive-mint}"
    textColor: "{colors.positive-forest}"
    rounded: "{rounded.pill}"
    padding: "4px 11px"
---

# Design System: IncidentIQ

## Overview

**Creative North Star: "The Horizon Command Press"**

IncidentIQ translates the high-contrast light editorial calm of `https://www.serko.ai/` into a native JavaFX 21 desktop workbench backed by Spring Boot telemetry. Rather than immersing engineers in noisy dark-mode neon panels, the interface treats production incident triage like an architectural press sheet: pure white surfaces framed by soft frost cards, disciplined pure-black typography, and a focused horizon spectrum reserved exclusively for AI intelligence and primary hero breakthroughs.

Every flow—from the animated 3D wireframe telemetry globe on the public Home showcase through JWT authentication into the split-pane Incident Queue, Report ingestion, and Operational Analytics—inherits the exact same spatial cadence, pill geometry, and restrained signal palette.

**Key Characteristics:**
- Pure white canvases paired with soft frost surfaces and ice-sky feature bands rather than saturated backgrounds or dark mode.
- Multi-stop sky-to-sunset spectrum (`horizon-blue` through `horizon-purple`, `horizon-pink`, and `aura-ember`) reserved for AI agent triggers, hero horizon atmosphere, and headline emphasis.
- Universal `999px` capsule pill affordances for actions, navigation switchers, and semantic status badges.
- Selective HTML5 Canvas (`WebView`) integration inside native JavaFX scenes strictly where real-time 3D geodesic projection outperforms static vectors.

## Colors

The palette balances stark neutral editorial contrast against atmospheric sky pastels and a single disciplined horizon spectrum for AI actions.

### Primary
- **Ink Primary** (`#000000`): Used for primary display headlines, active brand logo mark, segmented pill active text, and primary action buttons (`button-primary`).
- **Horizon Blue** (`#1B98F2`): Anchors the cool leading edge of the horizon spectrum, keyboard focus highlights on form inputs, progress bars, and `INVESTIGATING` workflow accents.
- **Horizon Purple** (`#CF69E0`) & **Aura Ember** (`#FF8C51`): Complete the warm trailing edge of the radial hero aura, inline gradient headline emphasis (`agent`, `autonomous agents`), and the iridescent `2px` border of the 6-agent pipeline button (`button-rainbow`).

### Neutral
- **Canvas Pure** (`#FFFFFF`): Dominant page ground across public storytelling bands, inside workspace backgrounds, and elevated content cards.
- **Frost Surface** (`#F6F6F6`): Ground for form inputs, filter bars, segmented pill switcher tracks, table headers, and secondary bento wrappers.
- **Sky Mist** (`#E8F3FE`) & **Sky Zenith** (`#E5F2FF`): Full-bleed atmospheric bands for featured AI diagnosis callouts, selected table rows, and upper hero sky gradients.
- **Ink Secondary / Muted** (`#4D4D4D` / `#71717A`): Body copy, table metadata, and uppercase tracked section eyebrows.
- **Border Hairline** (`#E5E7EB`): 1px structural boundaries on inputs, dropdown menus, dialogs, and workspace chrome dividers.

### Semantic
- **Positive Mint / Forest / Signal** (`#C0F2B5` / `#17510B` / `#158142`): High-legibility badge fill, dark green ink, and live API status dot indicator (`Resolved`, `6 Agents Ready`).
- **Alert Red / Warning** (`#F13931` / `#EA580C`): Pastel-backed `CRITICAL`, `HIGH`, and `OPEN` severity pills plus validation banners.

**The Horizon Restraint Rule.** Full multi-stop gradients belong exclusively to hero/CTA horizon backdrops, inline headline emphasis words, progress bars, and the tertiary `6-Agent Investigation` pill border—never fill standard workspace cards or tables with saturated gradients.

## Typography

**Display & UI Font:** `Plus Jakarta Sans` (bundled static Regular, Medium, SemiBold, Bold TTF weights), falling back to `Segoe UI` and `Helvetica Neue`.
**Code / Stack Trace Font:** `Consolas` / `Courier New` monospace strictly inside read-only error log viewers, patch previews, and stack-trace submission textareas.

**Character:** Tightly tracked geometric grotesk headlines create punchy editorial hierarchy alongside crisp, high-x-height numerals inside tables, metric cards, and incident IDs.

### Hierarchy
- **Display** (`700`, `54px`, line-height `1.08`, letter-spacing `-0.02em`): Public hero headlines paired with inline horizontal-gradient accent spans.
- **Headline** (`700`, `36px–38px`, line-height `1.15`): Section leads across public feature bands and closing horizon banners.
- **Title** (`700`, `24px–28px`, line-height `1.25`): Inside workspace screen titles (`Incident Queue`, `Report New Incident`, `Operational Analytics`).
- **Body** (`400`, `14px`, line-height `1.5`): Descriptions, table cell text, and multi-agent report summaries.
- **Label / Eyebrow** (`700`, `11.5px`, letter-spacing `0.9px`, uppercase): Section eyebrows, form field labels, and uppercase table column headers.

**The Mnemonic Guard Rule.** Disable JavaFX mnemonic parsing on every dynamic or underscore-containing chip (`APPLICATION_BUG`, `MEMORY_LEAK`) so underscores render verbatim instead of being eaten as Alt-key accelerators.

## Layout

Public pages flow inside full-bleed `ScrollPane` wrappers pinned beneath a `68px` centered top navbar (`Home`, `Platform`, `For teams`, `About` left; logo centered; `Log in` / `Get started` right) with two-column editorial grids capped at `1040px` (`48% / 52%`).

Inside workspace screens sit beneath a `66px` white header (`#EBEBEB` bottom hairline) housing brand + `Website` jump pill on the left, a centered `Incidents | Report Incident | Analytics` segmented pill track, and `Live API` badge + signed-in user pill + `Sign Out` button on the right. Content uses balanced split workspaces (`Queue` table flex-growing on the left, `410px–470px` Investigation Workbench on the right) or centered two-column `1040px–1120px` containers with `24px–36px` outer breathing room.

## Elevation & Depth

Surfaces rely primarily on flat tonal contrast (`#FFFFFF` cards seated on `#F6F6F6` tracks or `#FFFFFF` grounds separated by `1px` `#E5E7EB` / `#EBEBEB` borders) reinforced by soft diffused ambient drop shadows only on elevated cards, floating hero cards, active segmented pills, and dropdown popups.

### Shadow Vocabulary
- **Soft Ambient Card** (`DropShadow(rgba(0,0,0,0.06), radius: 22, offsetY: 6)`): Standard workspace cards, KPI tiles, and showcase frames.
- **Floating Hero / Auth Card** (`DropShadow(rgba(0,0,0,0.08–0.10), radius: 38–42, offsetY: 14)`): Centered hero live-triage widget and login card floating over radial horizon backdrops.
- **Active Segment Pill** (`DropShadow(rgba(0,0,0,0.08), radius: 8, offsetY: 2)`): Distinguishes the selected white pill inside `#F6F6F6` segmented switcher tracks.

**The Flat-Track Rule.** Segmented pill tracks and filter strips never cast drop shadows; only the selected active pill floats inside its frost track.

## Shapes

The entire system contrasts generously rounded structural containers (`18px` workspace cards, `22px` feature cards, `26px–28px` showcase frames) against `999px` full-capsule pills for buttons, tags, segmented controls, and status badges. Directional timelines use subtle round-capped dotted dividers (`strokeDashArray: 1, 10`) between pipeline steps rather than heavy horizontal rules.

## Components

### Buttons
- **Shape:** Full capsule (`999px` radius) with clean `chevronRightIcon` / `chevronDownIcon` vector graphics (never raw text emojis that shift across fonts).
- **Primary (`serko-btn-black`):** `#000000` fill, `#FFFFFF` `600` weight text, `10px 22px` padding, subtle `1.025x` hover scale transition.
- **Secondary (`serko-btn-white`):** `rgba(255,255,255,0.78)` translucent fill with `1px solid rgba(0,0,0,0.10)` hairline border, brightening to `#FFFFFF` on hover.
- **AI Tertiary (`serko-btn-rainbow`):** Layered `linear-gradient` inset stroke (`0, 2px` insets) pairing `#FFFFFF` center fill with the horizon spectrum border and purple ambient hover glow—used strictly for `6-Agent Investigation` actions.

### Chips & Badges
- **Style:** Pill radius (`999px`), `11px` bold label, `4px 11px` padding. Uses soft pastel surfaces with high-contrast dark ink (`CRITICAL` rose, `HIGH` peach, `MEDIUM`/`INVESTIGATING` ice mist, `LOW`/`RESOLVED` positive mint `#C0F2B5` / `#17510B`).

### Inputs & ComboBoxes
- **Style:** Soft frost fill (`#F6F6F6`), `1px` `#E5E7EB` border, `12px` radius, `10px 14px` padding.
- **Focus:** Shifts fill to pure `#FFFFFF` with `#1B98F2` border and soft blue outer glow (`dropshadow(gaussian, rgba(27,152,242,0.18), 10, 0, 0, 0)`).

### Navigation & Segmented Tracks
- **Style:** `#F6F6F6` `999px` container track with `4px` internal padding holding transparent inactive buttons (`#52525B`) and a `#FFFFFF` floating active pill with `#000000` bold label.

### Signature Component: `SerkoHeroGlobe`
- Mouse-transparent JavaFX `WebView` host loading self-contained ES6 HTML5 2D Canvas spherical projection (`hero-globe.html`) behind the public Home hero—rendering Fibonacci lattice micro-dots, latitude/longitude wireframes, great-circle gradient arcs, and traveling telemetry packets over Serko's sunset-sky radial gradient, with an automatic pure-JavaFX `Canvas` `AnimationTimer` fallback.

## Do's and Don'ts

### Do:
- **Do** preserve `100%` light editorial grounds (`#FFFFFF` / `#F6F6F6` / `#E8F3FE`) and pure black typography (`#000000`) across every route.
- **Do** use native JavaFX `SVGPath` icons (`brandLogoMark`, `chevronRightIcon`, vector bolts) so glyphs render identically across Windows DPI configurations.
- **Do** render multi-agent and quick AI outputs as scannable structured cards rather than unbroken walls of monospace log dumps.
- **Do** set `setMinWidth(Region.USE_PREF_SIZE)` (keeping default `USE_COMPUTED_SIZE` on `prefWidth` inside an unconstrained `Pane` host) on horizontal marquee labels and status pills inside flex `HBox` rows so labels never ellipsis-truncate or collapse to zero width.

### Don't:
- **Don't** introduce dark-mode surfaces (`#0d0d1a`, neon cyan cards) anywhere in the inside workspace or auth screens (the public footer `#000000` is the sole deliberate dark anchor).
- **Don't** use CSS degree angles (`linear-gradient(155deg, ...)`) inside JavaFX stylesheets or inline styles; JavaFX CSS strictly requires directional keywords (`to bottom right`) or percentage coordinates.
- **Don't** pass opaque/translucent backdrops (`rgba(255,255,255,0.92)`) onto sticky scrolled toolbars in JavaFX where `backdrop-filter: blur` is unsupported—use solid `#FFFFFF` once scrolled so scrolling headlines do not bleed faintly underneath nav links.

