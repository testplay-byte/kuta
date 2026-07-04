# Kuta — Design Language Alternatives Research

This document researches six design-language alternatives to Material 3 for **Kuta**, an anime
streaming viewer built on Jetpack Compose (currently a fork of Aniyomi). The app is **dark-first**,
**content-first** (anime cover art is the hero), and integrates four trackers (AniList as the
"front door" in Phase 2, plus MAL, Shiki, Bangumi). Build context that influences feasibility:

- `compileSdk=35`, `minSdk=26`, `targetSdk=34` (see `DOCS/architecture/11-build-variants.md`).
- `minSdk=26` matters: **AGSL `RuntimeShader` requires API 33+**; real-time **backdrop blur
  requires API 31+**. Below those API levels, any glow/blur must fall back to a scrim, gradient,
  or pre-rendered drawable.
- The existing theme is a thin layer on top of Material 3 (`presentation-core/.../theme/Color.kt`
  and `Typography.kt` — currently only adds two helpers: `ColorScheme.active` and
  `Typography.header`). Ripping out Material 3 entirely is therefore possible but means
  rebuilding every component in `presentation-core/components/material/` (Button, Scaffold,
  NavigationBar, Tabs, Slider, AlertDialog, FloatingActionButton, IconToggleButton, Surface,
  PullRefresh…).

For each option we document philosophy, key characteristics (colors, typography, components,
motion), example apps to reference, Compose feasibility, pros, cons, and a one-line verdict. A
comparison table sits at the end. **No option is picked** — the user decides.

---

## 1. iOS Human Interface Guidelines (adapted for Android)

### Philosophy
Apple's HIG is built on three principles — **Clarity** (legible text, precise controls, sharp
graphics), **Deference** (the UI serves content and never competes with it), and **Depth**
(realistic layering, motion, and translucency convey hierarchy). The 2025 "Liquid Glass"
refresh (WWDC25 session 356) explicitly reshapes the relationship between interface and content
so controls become translucent layers floating over content.

### Key characteristics
- **Colors**: System-defined semantic palette (label, secondaryLabel, systemBackground,
  secondarySystemBackground…). Dark mode is a first-class peer to light mode, not an
  afterthought. Accent colors are tinted sparingly (often a single brand blue/purple). Surfaces
  use true blacks on OLED (`#000000`) in dark mode.
- **Typography**: SF Pro / SF Pro Display / SF Pro Text. Dynamic Type scales with user
  preference. **Large titles** (bold, ~34pt) lead screens; body is regular weight 17pt; captions
  are 13pt. Tight letter-spacing on display weights.
- **Components**: Rounded corners (continuous squircles, ~10–16dp). Filled buttons are tinted
  with the accent color; secondary controls are bordered or rendered as "plain" text. **Frosted
  glass / vibrancy** on toolbars, sheets, and navigation bars — content shows through with a
  saturation boost. Subtle drop shadows for elevation. Sheet-style modals with a grabber handle.
- **Motion**: Spring-based, eased curves (`easeIn`, `easeOut`). Subtle parallax. Sheet
  presentations slide up with a slight overshoot. Tab transitions are crossfades.

### Example apps
- **Apple Music, Apple TV+, App Store, Photos** (iOS) — canonical HIG.
- **Things 3**, **Bear**, **Overcast** — third-party iOS apps that follow HIG closely.
- **Phone Link / "Windows 11 Phone Link for iOS"** — Apple-style chrome on Windows.
- The **WWDC25 "Liquid Glass"** redesign is the reference for the newest translucent aesthetic.

### Feasibility for Jetpack Compose
- Yes, implementable. The main work is **replacing `MaterialTheme` with a custom theme** that
  exposes semantic colors (background, surface, surface-elevated, label, secondaryLabel, accent,
  separator) and a Typography scale modelled on SF Pro.
- **Frosted glass / vibrancy**: use [Haze](https://github.com/chrisbanes/haze) (chrisbanes). Real
  backdrop blur on API 31+; on API 26–30 (which covers our `minSdk=26`) it falls back to a tinted
  scrim, so the look degrades gracefully rather than breaking.
- **Squircle corners**: use `Modifier.clip(RoundedCornerShape(16.dp))` or the
  `squircle-shape` library for true continuous corners.
- **Spring motion**: built into Compose (`spring()`, `AspectRatioTweenSpec`); no extra deps.
- **SF Pro licensing**: SF Pro is **not licensed for Android apps**. Use Inter, Geist Sans, or
  IBM Plex Sans as substitutes — all very close in feel and freely licensed.
- No blockers, but expect ~1–2 weeks to rebuild the component set in `presentation-core/components/material/`.

### Pros for our use case (anime streaming viewer, dark-first)
- **Deference** is exactly the right philosophy for content-forward apps — anime cover art stays
  the hero, UI chrome recedes.
- Frosted-glass toolbars over cover-art scrollers is a proven, premium pattern (Apple TV+,
  Plex iOS, Infuse).
- Large-title navigation makes library / browse / history screens feel calm and legible.
- True-black backgrounds on OLED save battery on long browsing sessions.

### Cons for our use case
- "Looks like an iOS app on Android" — some Android users find frosted glass + large titles
  jarring on a phone where it doesn't match the system.
- Haze fallback on API 26–30 means ~40% of installs (depending on device mix) won't see real
  blur — design must look right in scrim mode too.
- Material 3 components we currently use (NavigationBar, PullRefresh, Slider) need replacements
  or heavy restyling.
- Doesn't read as "distinctive" the way a custom language would — HIG is the second-most-used
  design language after Material.

### Verdict
Premium, content-first, and the most defensible "obvious" choice — but it leans familiar rather
than distinctive, and depends on Haze for the glass effect on older devices.

---

## 2. Fluent Design (Microsoft)

### Philosophy
Fluent is Microsoft's design language built on five pillars: **Light, Depth, Motion, Material,
and Scale**. The Win11 era focuses on **Mica** (an opaque material that samples the desktop
wallpaper once for a subtle, persistent tint) for primary surfaces, and **Acrylic** (a
translucent blur) for transient surfaces like flyouts and command bars. The guiding idea is that
the OS chrome should feel like a quiet, ambient extension of the user's wallpaper and lighting.

### Key characteristics
- **Colors**: System accent color (user-pickable, defaults to a blue). Background is Mica-tinted
  dark gray on Win11 dark mode (~`#1F1F1F` / `#202020`). Text is a near-white with secondary
  grays. Light/dark mode both fully supported.
- **Typography**: Segoe UI Variable (Win11). Type ramp: Caption 12 / Body 14 / Body Large 18 /
  Title 20 / Title Large 28 / Display 40+. Light weight on large sizes, semibold on small.
- **Components**: Subtle 4–8dp corner radius (Fluent is less rounded than M3). Filled buttons
  with subtle hover "reveal highlight" (a radial light that follows the cursor). **Reveal
  highlight** on hover for borders. Cards have a 1px hairline border + subtle elevation.
  NavigationView pattern (hamburger or left rail). Toggle switches are pill-shaped.
- **Motion**: **Connected animation** (an element morphs from one screen to the next).
  Eased `0.1, 0.9, 0.2, 1` standard curve. Subtle — never bouncy.

### Example apps
- **Windows 11 Settings, File Explorer, Photos** — canonical Fluent.
- **Microsoft Store, Edge, Teams** — Fluent on Win11.
- **Microsoft 365 (web)** — Fluent 2 web implementation.
- Note: Fluent on Android is virtually nonexistent; Microsoft's own Android apps (Edge, Office,
  SwiftKey) tend to use Material 3.

### Feasibility for Jetpack Compose
- Implementable, but **no first-class Fluent Compose library exists** (Microsoft maintains
  WinUI 3 / React Native for Windows, not Compose).
- **Mica**: hard to do honestly on Android — there's no "desktop wallpaper" concept. Closest
  analog is sampling the user's home-screen wallpaper via `WallpaperManager`, but Android
  restricts that. More realistically: fake it with a subtle vertical gradient tied to the
  accent color.
- **Acrylic**: same Haze library as option 1, same API 31+ caveat.
- **Reveal highlight**: doable with `Modifier.pointerInput` tracking pointer position + a
  `Brush.radialGradient` overlay. No library; ~1 day of work.
- **Connected animation**: Voyager (the nav library we use) has `DefaultNavigatorScreenTransition`
  for shared-element transitions; you'd extend this.
- **Segoe UI Variable**: not licensed for non-Windows apps. Substitute Inter or Segoe-flavored
  alternatives (e.g., Segoe UI is bundled only on Windows).
- Significant effort: ~2–3 weeks to design and build a Fluent component kit from scratch.

### Pros for our use case
- Subtle, calm, professional — good for "I want to relax and watch anime" mood.
- Dark mode is excellent and the accent-color-is-user-pickable model maps well to letting users
  theme Kuta (current Aniyomi lets users pick theme + accent).
- 1px hairline borders + low elevation make cover-art grids look clean and gallery-like.
- Reveal highlight on hover/long-press would feel fresh on mobile.

### Cons for our use case
- **Mica can't be authentically reproduced on Android** — the defining material becomes a fake.
- Fluent has near-zero Android footprint; users won't recognize the language and may perceive
  it as "weird generic dark UI" rather than "premium".
- Sharper corners (4–8dp) read as more utilitarian / business-y than content-forward.
- Effort is high for a language that won't carry brand recognition.

### Verdict
Calm and professional, but the defining Mica material can't be authentically done on Android —
this is the weakest fit of the six for an anime viewer.

---

## 3. Custom Minimalist Dark (Spotify / Letterboxd style)

### Philosophy
Let content be the loudest thing on screen; the chrome is barely there. Surfaces are
near-black, typography is high-contrast sans-serif, accent color is used sparingly for status
and primary actions, and ornamentation (shadows, gradients, borders) is minimized in favor of
clean grid layout and whitespace. This is the dominant aesthetic of modern dark-first content
apps.

### Key characteristics
- **Colors**: True black `#000000` or near-black `#0A0A0A` background. Cards are a slightly
  elevated dark gray `#121212`–`#1A1A1A`. Text is paper-white `#FFFFFF` for primary, ~`#B3B3B3`
  for secondary (Spotify's exact secondary). Single accent color: **Spotify green `#1DB954`**;
  **Letterboxd** uses three accent dots (blue/green/orange) for ratings. Letterboxd's base is a
  dark blue-gray (`#2C3440` / `#14181C`).
- **Typography**: Spotify uses **Circular** (geometric humanist sans, similar to Inter).
  Letterboxd uses **Graphik** (similar to Inter). Both rely on **bold display weights for
  section headers** and a tight type scale. Type is the primary visual hierarchy — far more
  than borders or shadows.
- **Components**: Rounded cards (Spotify ~8dp, Letterboxd uses sharp posters). Buttons are pill
  (`RoundedCornerShape(50%)`) outlined or filled. No drop shadows on most surfaces — separation
  is achieved by background-color step. Bottom nav is icon-only with tiny labels, no card
  background.
- **Motion**: Minimal. Subtle scale-on-press (1.0 → 0.97). Crossfades between tabs. Spotify
  uses a marquee scroll for long titles. No springs, no parallax.

### Example apps
- **Spotify** (mobile + desktop) — the canonical reference. Black background, dark-gray cards,
  green accent, Circular type.
- **Letterboxd** (web + mobile) — movie posters as a clean grid, dark blue-gray base, ratings as
  three colored dots.
- **Tidal, YouTube Music** (dark mode), **Plex** (default dark theme), **Goodreads** (dark mode).
- **Aniyomi/Animiru themselves** already lean this way — many anime viewers (Mihon, Aniyomi)
  already use dark backgrounds + cover-art grids. The "distinctive" lift is small.

### Feasibility for Jetpack Compose
- **Trivial.** This is essentially what we already have, minus M3 chrome. We'd strip the M3
  tokens and substitute a custom dark palette + Inter or Geist font.
- No special libraries required. Optionally use Coil3's existing image pipeline (already
  integrated — see worklog Task 3-b) for cover-art loading.
- The hardest part is **restraint** — designers must resist adding borders/shadows/gradients.

### Pros for our use case
- Lowest effort of the six options.
- **Cover art looks best on pure black** — anime key visuals pop, OLED saves battery.
- Familiar to anyone who's used Spotify/Letterboxd/Plex — almost zero learning curve.
- Highly legible; excellent for long browsing sessions.
- Accent color slot maps cleanly to AniList blue (`#3DB4F2`), MAL blue, Shiki purple, Bangumi
  pink — we can tint per-tracker context.

### Cons for our use case
- **Not distinctive.** It reads as "another dark media app" — closer to Aniyomi/Mihon than to
  something identifiably Kuta.
- No signature visual moment — risk of feeling like a reskin.
- Requires strong typographic discipline to avoid looking flat and unfinished.

### Verdict
Safe, cheap, and content-forward — but it won't give Kuta a recognisable identity beyond "dark
anime viewer".

---

## 4. Editorial / Magazine Style (Apple Music / content-first)

### Philosophy
Treat the app like a printed magazine: **content is the hero**, typography is bold and
editorial, layouts are asymmetric grids with intentional whitespace, and imagery is large,
full-bleed, and immersive. The UI chrome almost disappears — there are no buttons or toolbars
where content can speak instead.

### Key characteristics
- **Colors**: Background can flip between true black and a tinted dark surface pulled from the
  currently-displayed cover art (Apple Music does this — the now-playing screen tints itself to
  the album art). Text is paper-white; secondary text is a desaturated mid-gray. Accent colors
  are derived from content, not from a fixed brand palette.
- **Typography**: **Heavy display weights** — Apple Music uses SF Pro Display Heavy / Black for
  section titles. Large sizes (40pt+). Tight leading. Body uses regular weight at comfortable
  sizes. Often a serif is mixed in for editorial flourish (e.g., Apple News uses NY Serif).
- **Components**: **Mixed grid layouts** — not all cards are equal size. Hero card spans 2
  columns; secondary cards are smaller. Cards are sharp-cornered or very lightly rounded (2dp).
  Full-bleed imagery. Minimal buttons — often just text links with chevrons. Section headers
  are large display text, not cards.
- **Motion**: Parallax on scroll (image moves slower than text). Hero images expand on tap into
  immersive detail views. Subtle crossfades. Apple Music's now-playing has a fluid, spring-based
  expand/collapse.

### Example apps
- **Apple Music**, **Apple News**, **Apple TV+** — canonical editorial.
- **Letterboxd** (in magazine mode) — movie stills as full-bleed sections.
- **MUBI** (cinema-streaming app) — strong editorial layout, large film stills, serif accents.
- **NYT Cooking**, **Substack** — content-first editorial.

### Feasibility for Jetpack Compose
- Implementable. The challenge is **layout, not components**.
- Use `LazyVerticalStaggeredGrid` (Compose Foundation) for asymmetric grids.
- Use Coil3 (already integrated) with `Palette` API to extract dominant color from cover art and
  tint the surrounding surface — gives the "Apple Music now-playing" effect.
- No drop-shadow libraries needed; separation is by imagery, not elevation.
- Custom display typography: bundle a free editorial display face such as **Fraunces** (open
  source) or **PP Editorial New** (paid); pair with Inter for body.
- Effort is medium — ~1.5–2 weeks, mostly layout and palette-extraction work.

### Pros for our use case
- **Anime key visuals are basically movie posters** — this aesthetic is built for cover art.
  Seasonal posters, character art, and episode thumbnails all become hero content.
- Dynamic palette extraction means the app feels alive and **recolors itself per anime** —
  a deeply on-brand effect for a viewer.
- Magazine-style sections map cleanly to AniList's "Seasonal", "Trending", "Popular this
  season" curations — AniList as "front door" becomes a magazine cover.
- Distinctive — no other anime viewer uses this language (Aniyomi, Mihon, Animiru all use
  Material-style grids).

### Cons for our use case
- Editorial layouts are **hard to make work in dense library screens** (1000+ anime in user
  library). The hero/magazine treatment works for browse but fights against uniform grid
  browsing.
- Heavy display type can feel pretentious for utility screens (downloads, settings, history).
- Dynamic palette extraction is GPU work per cover — need caching strategy to avoid jank in
  fast-scrolling grids.
- More design skill required: editorial requires taste, not just a token system.

### Verdict
The most distinctive and most on-brand for anime-as-content — but requires designing two
distinct modes (editorial browse vs. uniform library grid) and the most taste to execute well.

---

## 5. Retro / Neo-brutalist (Gumroad / Linear)

### Philosophy
Reject polished neutrality in favor of **graphic bluntness**: thick black borders, hard offset
shadows (no blur), high-contrast electric accent colors, monospace or grotesque display
typefaces, and an intentional "raw" or "ugly" aesthetic. The look is loud, opinionated, and
unmistakably hand-made. Linear is a refined variant — near-black surfaces, paper-white type,
one electric accent (lavender-blue or acid-lime `#e4f222`).

### Key characteristics
- **Colors**: Gumroad — pure white or electric pink/yellow/lime backgrounds with thick **black
  `#000000`** borders and text. Linear — near-black `#08090A` background, paper-white type,
  one electric accent (`#5E6AD2` lavender, or `#E4F222` acid-lime on marketing pages). Both
  use **very few colors at very high saturation/contrast**.
- **Typography**: Gumroad — **Söhne** or **Inter Display** at heavy weights. Linear — Inter
  Display + Inter. Both favor **geometric grotesques**; monospace (e.g., JetBrains Mono,
  Berkeley Mono) is common for code/labels. Big, blocky headlines.
- **Components**: **Thick black borders (2–3px) and hard offset shadows** (e.g., `box-shadow:
  4px 4px 0 #000` — no blur). Buttons are square or lightly rounded (2–4dp), filled with
  electric color. Cards have visible borders, not shadows. Hover states invert colors. Inputs
  have visible 2px black borders.
- **Motion**: Snappy, mechanical. Linear uses 150–200ms ease-out. Gumroad uses 100ms hard cuts.
  No springs, no overshoot. Hover states snap.

### Example apps
- **Gumroad** (web) — the canonical neo-brutalist reference after their 2022 redesign.
  Pink/yellow sections, thick black borders, massive headlines.
- **Linear** (web + macOS app) — refined dark variant: midnight surfaces, one accent, sharp
  typography.
- **Vercel**, **Resend**, **Raycast** (web) — same refined dark-brutalist family.
- **Figma config sites**, **Bluesky** (some surfaces) —neo-brutalist touches.

### Feasibility for Jetpack Compose
- **Trivially implementable.** Hard offset shadows are just `Modifier.shadow()` with `blurRadius
  = 0` and an offset, or `drawBehind` with a translated black rectangle behind the box.
- Thick borders are `Modifier.border(width = 2.dp, color = Color.Black)`.
- Type: Inter Display is free; pair with JetBrains Mono (free) for monospace labels.
- No special libraries. Effort is low — ~1 week, mostly palette and component rebuild.
- The only subtlety: hard shadows on Android render differently from CSS — test on emulator.

### Pros for our use case
- **Maximally distinctive.** No anime viewer looks like this. Kuta would be instantly
  recognisable in screenshots.
- Linear's refined dark variant (near-black + one accent) keeps the aesthetic while still being
  legible and OLED-friendly.
- Strong visual hierarchy with very few tokens — easy to theme.
- Hard shadows and thick borders make tap targets obvious — good for a media app where users
  are half-watching.

### Cons for our use case
- **Brutalist aesthetic clashes with anime cover art** — anime key visuals are soft, painterly,
  and detailed; thick black borders and electric pinks can fight them visually. Risk of looking
  like a 2010s Tumblr theme.
- Gumroad-style bright backgrounds are off-brand for a dark-first viewer.
- Hard offset shadows look dated to some users; can read as "amateur" if not done carefully.
- Linear's refined variant is closer to option 3 (Minimalist Dark) with extra accent —
  diminishing distinctiveness.

### Verdict
Maximally distinctive but visually fights anime cover art; the Linear sub-variant is a safer
compromise but loses some of the personality.

---

## 6. Dark Neon (cyberpunk / synthwave)

### Philosophy
Pure-black or deep-dark backgrounds lit by **glowing neon accents** — cyan, magenta, electric
purple, lime. Surfaces have luminous edges, scanlines, or chromatic-aberration hints. The look
is futuristic, nocturnal, and high-contrast; the user's anime library becomes a "command
console" of glow-bordered cards. This is the aesthetic the user previously specified for the
**coordinator dashboard** — adopting it for the app would give Kuta a unified visual identity
across both surfaces.

### Key characteristics
- **Colors**: Near-black base (`#050507`, `#0A0A12`, or pure `#000000`). One or two **neon sign
  hues** — canonically **cyan `#00FFFF` / `#00E5FF`**, **magenta `#FF00FF` / `#FF2D95`**,
  **electric purple `#9D00FF`**, **lime `#A6FF00`**. Surfaces are dark gray `#12121A` with
  luminous 1–2px borders in a neon hue. Glow is achieved by blurring the neon color outward
  from edges and text.
- **Typography**: **Geometric monospace or futuristic sans** — e.g., **JetBrains Mono**,
  **Space Grotesk**, **Orbitron** (for display), **Geist Mono**. Body often at slightly larger
  sizes with letter-spacing for "terminal" feel. Text in cyan/magenta, never pure white.
- **Components**: Cards with **glowing 1px neon borders** (border color + outer glow). Buttons
  are outlined, not filled — fill would dim the glow. Active state = brighter glow + subtle
  inner pulse. Bottom nav icons glow when selected. Episode-progress bars are neon-filled
  tracks. Loading spinners are glowing rings.
- **Motion**: **Pulsing glow** (subtle brightness oscillation, 2–4s cycle). Scanline sweep
  across hero cards. Glitch on transitions (rare; can be annoying). Smooth eased motion for
  navigation; spring for emphasis.

### Example apps
- **Cyberpunk 2077** (game UI) — canonical neon-drenched dashboard.
- **Ghostwriter** (film UI), **Severance** (Macrodata refinement UI) — minimalist neon.
- **Linear's marketing pages** (acid-lime accents on black) — a restrained take.
- **Vercel/Linear dashboard** dark themes — close cousins.
- **CRT/synthwave UI kits** on Dribbble and Gumroad — useful reference for component patterns.
- The user's own **coordinator dashboard** — by definition the reference for Kuta's neon
  language if we want consistency.

### Feasibility for Jetpack Compose
- Implementable. The core technique is **glow = colored shadow with no offset and large blur**.
  Three approaches, in increasing fidelity:
  1. **`Modifier.shadow` + `drawWithContent` + `Paint.setShadowLayer(blurRadius, 0f, 0f,
     neonColor)`** — works on all API levels, but `setShadowLayer` is software-rendered and can
     be slow on big surfaces. Good for borders and text.
  2. **`drawBehind` with a `Brush.radialGradient` extending past the bounds** — cheapest glow
     for cards. Works on all APIs.
  3. **AGSL `RuntimeShader`** (Android 13+ / API 33+) for true GPU bloom/glow. Below API 33,
     fall back to approach 1 or 2.
- **`Modifier.dropShadow`** (Compose Foundation, recent) and the third-party
  [ShadowGlow](https://github.com/StarkDroid/compose-ShadowGlow) library both abstract the
  colored-shadow pattern; ShadowGlow also supports breathing/pulse animation out of the box.
- For **scanlines and chromatic aberration**: AGSL shader or a static `Image` overlay. Static is
  cheaper.
- For **glowing text**: same `setShadowLayer` trick on a `Text` `drawWithContent` override.
- **minSdk=26 caveat**: AGSL requires API 33+. Roughly half of Android devices in the wild are
  still below 33; the glow needs a non-shader fallback. Approaches 1 and 2 cover this.
- Effort is medium — ~1.5–2 weeks. Most of the work is building a `NeonModifier` /
  `glowingBorder()` helper and a glow-friendly component kit.

### Pros for our use case
- **Strongest possible brand identity.** A neon-drenched anime viewer is instantly
  recognisable in screenshots and in the app drawer.
- **Consistency with the coordinator dashboard** — both surfaces speak the same visual language,
  which is rare and memorable.
- Pure-black backgrounds + neon lines are OLED-perfect (true black, few lit pixels).
- The "anime as command console" framing appeals to the otaku demographic (which overlaps with
  the cyberpunk/synthwave fandom).
- AniList's blue (`#3DB954` is Spotify's; AniList is `#3DB4F2`-ish cyan-blue) is already close
  to a neon cyan — a fortuitous color match for our "front door" tracker.

### Cons for our use case
- **Glow effects can feel gimmicky** and age quickly — what looks cool in 2026 may look dated
  in 2028.
- **Accessibility risk**: neon-on-black can fail WCAG contrast depending on the hue; magenta and
  cyan can be hard for color-blind users. Need careful palette tuning.
- **Performance**: `setShadowLayer` is software-rendered; pervasive glow on every card in a fast
  library grid (1000+ items) can cause jank on low-end devices. Need to limit glow to borders
  (cheap) rather than fills (expensive), and disable on scroll.
- **Risk of clashing with anime cover art**: cover art is often already colorful; neon chrome
  can compete. Mitigation: keep chrome minimal — thin neon hairlines, not glowing slabs.
- Tuning the "right amount" of glow is hard — too little looks unfinished, too much looks
  amateur.

### Verdict
The most distinctive option and the only one that gives Kuta a unified identity with the
coordinator dashboard — but the highest execution and accessibility risk, and the easiest to
get wrong.

---

## Comparison Table

| Trait                          | iOS HIG (adapted)    | Fluent (Microsoft) | Minimalist Dark (Spotify) | Editorial (Apple Music) | Neo-brutalist (Linear/Gumroad) | Dark Neon (cyberpunk) |
|--------------------------------|----------------------|--------------------|---------------------------|-------------------------|--------------------------------|-----------------------|
| **Background**                 | True black or near   | Mica-tinted gray   | True black `#000000`      | Black or content-tinted | Pure black `#08090A` (Linear)  | Near-black `#050507`  |
| **Accent strategy**            | Single brand tint    | User-pickable      | Single accent (Spotify green) | Derived from cover art | One electric accent        | 1–2 neon hues         |
| **Corner radius**              | 10–16dp squircle     | 4–8dp              | 8dp cards / pill buttons  | Sharp or 2dp            | 0–4dp                          | 4–8dp + glow          |
| **Borders vs shadows**         | Subtle shadows       | 1px hairline       | Neither (color step)      | Neither                 | **Thick black border + hard offset shadow** | **Glowing 1px border** |
| **Typography**                 | SF Pro / Inter       | Segoe UI / Inter   | Circular / Inter          | Heavy display + serif   | Inter Display / Söhne          | Mono / Space Grotesk  |
| **Motion**                     | Spring, eased        | Connected animation| Minimal, scale-on-press   | Parallax, hero expand   | Snappy, mechanical             | Pulse glow, eased     |
| **Content-forwardness**        | High (Deference)     | Medium             | **Highest**               | **Highest (hero imagery)** | Medium                     | Medium (chrome competes) |
| **Distinctiveness**            | Low (familiar)       | Low on Android     | Low (every dark app)      | **High**                | **High**                       | **Highest**           |
| **OLED battery friendliness**  | High                 | Medium             | **High**                  | High                    | High                           | **High**              |
| **Compose effort**             | Medium (1–2 wks)     | High (2–3 wks)     | **Low** (~1 wk)           | Medium (1.5–2 wks)      | Low (~1 wk)                    | Medium (1.5–2 wks)    |
| **Key library needed**         | Haze (glass)         | Haze (acrylic)     | None                      | Coil Palette API        | None                           | ShadowGlow / AGSL     |
| **minSdk=26 caveat**           | Haze scrim fallback  | Mica can't authentically be done | None        | Palette caching         | None                           | AGSL needs API 33+, fallback for 26–32 |
| **Accessibility risk**         | Low                  | Low                | Low                       | Low–Medium              | Medium (contrast)              | **High** (color-blind) |
| **Risk of clashing with covers** | Low                | Low                | Low                       | Low                     | **High** (borders fight art)   | Medium (glow competes) |
| **Brand unity with coordinator dashboard** | None   | None               | None                      | None                    | Partial (Linear variant)       | **Full** (same language) |
| **Reference apps to study**    | Apple Music, TV+     | Win11 Settings     | Spotify, Letterboxd       | Apple Music, MUBI       | Gumroad, Linear                | Cyberpunk 2077, Linear marketing |

---

## Notes for the decision-maker

- **minSdk=26 is the recurring constraint.** Any option that leans on AGSL (API 33+) or real
  backdrop blur (API 31+) needs a fallback. The fallbacks (Haze scrim, `setShadowLayer`,
  `Brush.radialGradient`) are all acceptable — but the design must look right in both modes.
- **The existing codebase is Material 3 to the bone.** Every file in
  `presentation-core/components/material/` is an M3 wrapper. Picking any of these six options
  means either (a) replacing those wrappers one by one, or (b) building a parallel
  `components/kuta/` package and migrating screens gradually. Option (b) is safer.
- **AniList's brand blue is roughly `#3DB4F2`** — a cyan-leaning blue. This is conveniently
  close to a cyberpunk neon cyan, which slightly favors option 6 if AniList is the "front door"
  tracker.
- **Two design languages can coexist**: a primary language for chrome and a secondary treatment
  for hero/browse screens. Editorial (#4) + Minimalist Dark (#3) is a common pairing; Neon (#6)
  + Minimalist Dark (#3) would let the dashboard feel neon while library grids stay calm.
- This document deliberately **does not pick**. The user chooses.
