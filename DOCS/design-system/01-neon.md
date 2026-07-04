# 01 — Neon Design Language

> Cyberpunk / synthwave aesthetic. Pure black backgrounds, neon glow accents, glass-morphism. Adapted from `DESIGN.md` (the coordinator dashboard's design system) for native Jetpack Compose.

---

## 1. Philosophy

| Principle | Description |
|---|---|
| **Dark-first** | Every surface starts dark. Light is added sparingly through accents. |
| **Neon accents on muted canvas** | Base palette is neutral dark. Accents (cyan, magenta, lime) provide all visual energy. |
| **Glass, not flat** | Surfaces use `backdrop-blur` equivalent + semi-transparent backgrounds for depth. |
| **Glow, not shadow** | Elevation is communicated via glow (neon light), not drop shadows. |
| **Monospace for data** | All numbers, timestamps, technical values use monospace font with tabular-nums. |
| **Animated but not distracting** | Motion communicates state changes — never slows the user down. |

---

## 2. Color Palette

### 2.1 Dark Mode (Primary — Neon's Natural State)

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#0F0F14` | Outermost background, body, root container |
| `bgSurface` | `#1A1A22` | Cards, content panels, stat blocks |
| `bgSidebar` | `#15151C` | Sidebar, navigation panel |
| `bgElevated` | `#25252F` | Hover states, active items, emphasis backgrounds |
| `bgGlass` | `rgba(26, 26, 34, 0.85)` | Glass-morphism panels (with backdrop-blur) |

| Accent Token | Hex | Role | Semantic |
|---|---|---|---|
| `accentPrimary` | `#5FC9FF` (cyan) | Primary actions, links, active states | "Go" / info |
| `accentSecondary` | `#BCFF5F` (lime) | Success, positive, completion | Success |
| `accentTertiary` | `#FF5F7E` (coral) | Destructive, errors, delete | Danger |
| `accentQuaternary` | `#FFB45F` (amber) | Warnings, pending states | Warning |
| `accentPurple` | `#A78BFA` | Highlights, special tags | Neutral accent |

> The user's selected accent (from Settings) becomes `accentPrimary`. The other accents stay fixed.

### 2.2 Light Mode (Unusual But Workable)

Neon's identity is dark-first. Light mode inverts the canvas while keeping neon accents — striking but odd.

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#F5F5F8` | Off-white background (not pure white — easier on eyes) |
| `bgSurface` | `#FFFFFF` | Cards, content panels |
| `bgSidebar` | `#EFEFF3` | Sidebar |
| `bgElevated` | `#E5E5EC` | Hover, active |
| `bgGlass` | `rgba(255, 255, 255, 0.85)` | Glass panels |

Accents stay the SAME as dark mode (neon colors on light bg = high contrast, slightly unusual but readable). Glow effects are replaced with stronger borders in light mode (glow on white = invisible).

### 2.3 Text Colors

| Token | Dark | Light |
|---|---|---|
| `fgPrimary` | `#FFFFFF` | `#0F0F14` |
| `fgSecondary` | `#C8C8D4` | `#3A3A45` |
| `fgMuted` | `#8888A0` | `#6A6A7A` |
| `fgDim` | `#55556A` | `#9A9AAB` |

### 2.4 Border Colors

| Pattern | Dark | Light |
|---|---|---|
| Default | `rgba(255,255,255,0.08)` | `rgba(15,15,20,0.10)` |
| Subtle | `rgba(255,255,255,0.04)` | `rgba(15,15,20,0.05)` |
| Strong | `rgba(255,255,255,0.12)` | `rgba(15,15,20,0.18)` |
| Accent | `accentPrimary` at 20% opacity | same |

### 2.5 Glow Tokens

| Token | Value (Dark) | Light Equivalent |
|---|---|---|
| `glowPrimary` | `0 0 20px rgba(accentPrimary, 0.25)` | Border `2px solid accentPrimary` |
| `glowSecondary` | `0 0 20px rgba(accentSecondary, 0.25)` | Border `2px solid accentSecondary` |
| `glowTertiary` | `0 0 20px rgba(accentTertiary, 0.25)` | Border `2px solid accentTertiary` |

> In light mode, glow is replaced with a solid accent border (glow is invisible on light backgrounds).

---

## 3. Typography

### 3.1 Font Stack

| Usage | Font | How To Load |
|---|---|---|
| Body / UI | Inter | Google Fonts via `FontFamily` — bundle TTF in `res/font/` for offline |
| Numbers / Code | JetBrains Mono | Google Fonts — bundle TTF |

### 3.2 Type Scale

| Element | Size | Weight | Font |
|---|---|---|---|
| Display (hero) | 48sp | Bold | Inter |
| Headline (screen title) | 28sp | Bold | Inter |
| Title (section) | 22sp | Semibold | Inter |
| Subtitle | 16sp | Semibold | Inter |
| Body | 14sp | Regular | Inter |
| Body Small | 12sp | Regular | Inter |
| Label | 11sp | Medium | Inter, uppercase, +0.08em tracking |
| Mono Value | 14sp | Bold | JetBrains Mono, tabular-nums |
| Mono Large | 26sp | Bold | JetBrains Mono, tabular-nums |

### 3.3 Compose Implementation

```kotlin
val NeonTypography = KutaTypography(
    display = TextStyle(fontFamily = InterFont, fontSize = 48.sp, fontWeight = Bold),
    headline = TextStyle(fontFamily = InterFont, fontSize = 28.sp, fontWeight = Bold),
    title = TextStyle(fontFamily = InterFont, fontSize = 22.sp, fontWeight = SemiBold),
    subtitle = TextStyle(fontFamily = InterFont, fontSize = 16.sp, fontWeight = SemiBold),
    body = TextStyle(fontFamily = InterFont, fontSize = 14.sp, fontWeight = Normal),
    bodySmall = TextStyle(fontFamily = InterFont, fontSize = 12.sp, fontWeight = Normal),
    label = TextStyle(fontFamily = InterFont, fontSize = 11.sp, fontWeight = Medium,
                      textTransform = Uppercase, letterSpacing = 0.08.sp),
    monoValue = TextStyle(fontFamily = JetBrainsMonoFont, fontSize = 14.sp, fontWeight = Bold,
                          fontFeatureSettings = "tnum"),
    monoLarge = TextStyle(fontFamily = JetBrainsMonoFont, fontSize = 26.sp, fontWeight = Bold,
                          fontFeatureSettings = "tnum"),
)
```

---

## 4. Spacing

Use shared `KutaSpacing` (see `00-shared-architecture.md` §5.1). Neon doesn't override spacing values.

---

## 5. Component Specs

### 5.1 NeonButton

- **Primary:** `bgBase` background, `accentPrimary` border (1px), `accentPrimary` text, glow on hover
- **Secondary:** transparent bg, `accentPrimary` text, no border
- **Destructive:** `accentTertiary` border + text
- **Ghost:** transparent, `fgMuted` text, `fgSecondary` on hover
- **Height:** 48dp (primary), 40dp (compact)
- **Corner radius:** 10dp
- **Hover:** glow appears (`glowPrimary`), bg lightens slightly
- **Press:** glow intensifies, scale 0.98
- **Disabled:** 50% opacity, no glow

```kotlin
@Composable
fun NeonButton(text: String, onClick: () -> Unit, ...) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isHovered) bgElevated else bgBase)
            .border(1.dp, accentPrimary, RoundedCornerShape(10.dp))
            .then(if (isHovered) Modifier.neonGlow(accentPrimary) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Text(text, color = accentPrimary, style = typography.body)
    }
}
```

### 5.2 NeonCard

- Background: `bgSurface` (with optional glass-morphism: `bgGlass` + blur)
- Border: 1dp `borderDefault`
- Corner radius: 14dp
- Hover: border becomes `accentPrimary` at 20%, subtle glow
- Padding: 20dp (default), 16dp (compact)

### 5.3 NeonInput

- Background: `bgBase`
- Border: 1dp `borderDefault`, becomes `accentPrimary` on focus
- Height: 44dp
- Corner radius: 10dp
- Focus: glow `glowPrimary`
- Placeholder: `fgDim`

### 5.4 NeonDialog

- Background: `bgGlass` + backdrop-blur (16dp blur)
- Border: 1dp `borderStrong`
- Corner radius: 18dp
- Shadow: deep drop shadow (`rgba(0,0,0,0.5)`)
- Max width: 560dp

### 5.5 NeonNavigationBar (Bottom)

- Background: `bgGlass` + backdrop-blur
- Border top: 1dp `borderDefault`
- Active item: `accentPrimary` icon + glow dot indicator
- Inactive item: `fgMuted` icon
- Height: 64dp (with safe-area inset)

### 5.6 NeonTopAppBar

- Background: `bgGlass` + backdrop-blur
- Height: 56dp
- Title: `fgPrimary`, headline typography
- Back button: `fgSecondary`, `accentPrimary` on hover

### 5.7 NeonListItem

- Background: transparent
- Hover: `bgElevated` at 50%
- Padding: 16dp vertical, 24dp horizontal
- Divider: 1dp `borderSubtle`

### 5.8 NeonChip

- Background: `accentPrimary` at 10% opacity
- Border: `accentPrimary` at 20%
- Text: `accentPrimary`
- Corner radius: 6dp
- Height: 24dp

### 5.9 NeonBadge

- Background: `accentPrimary`
- Text: `bgBase`
- Corner radius: 4dp
- Padding: 2dp x 8dp
- Font: label typography

### 5.10 NeonSkeleton

- Background: `bgElevated`
- Shimmer: `accentPrimary` at 5% opacity, sweep animation
- Corner radius: matches target component

---

## 6. Effects — Compose Implementation

### 6.1 Glass-Morphism (Backdrop Blur)

**Problem:** Compose's `Modifier.blur()` requires API 31+. minSdk is 26.

**Solution:** Use the [Haze library](https://github.com/chrisbanes/haze) by Chris Banes. It provides glass-morphism with scrim fallbacks below API 31.

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.chrisbanes.haze:haze:0.7.0")
}

// Usage
val hazeState = remember { HazeState() }
Box(
    modifier = Modifier
        .fillMaxSize()
        .hazeEffect(hazeState, style = HazeStyle(blurRadius = 16.dp))
) {
    // content behind
    Box(modifier = Modifier.fillMaxSize()) { /* background content */ }
    // glass panel
    Box(modifier = Modifier.hazeSource(hazeState)) { /* glass panel content */ }
}
```

**Fallback (API < 31):** Haze uses a scrim (semi-transparent overlay) instead of real blur. Looks slightly less premium but works.

### 6.2 Neon Glow

**Problem:** Compose doesn't have a built-in glow effect. `Modifier.shadow()` uses blur (wrong for neon).

**Solution:** Custom `Modifier` using `drawBehind` + `Paint` with `setShadowLayer` (software-rendered, but acceptable for small elements).

```kotlin
fun Modifier.neonGlow(color: Color, radius: Dp = 20.dp): Modifier = this.then(
    Modifier.drawBehind {
        val paint = Paint().apply {
            this.color = Color.Transparent
            style = PaintingStyle.Fill
            isAntiAlias = true
        }
        paint.asFrameworkPaint().apply {
            setShadowLayer(
                radius.toPx(),
                0f,
                0f,
                color.copy(alpha = 0.5f).toArgb()
            )
        }
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                0f, 0f, size.width, size.height,
                10.dp.toPx(), 10.dp.toPx(),
                paint
            )
        }
    }
)
```

**Perf warning:** `setShadowLayer` is software-rendered. Avoid applying to every item in a large grid (1000+ items). For grids, use a static border instead of glow.

### 6.3 Grid Background (Optional Subtle Texture)

A very subtle dot-grid or line-grid background for the "cyberpunk canvas" feel:

```kotlin
fun Modifier.neonGridPattern(): Modifier = this.then(
    Modifier.drawBehind {
        val spacing = 20.dp.toPx()
        var x = 0f
        var y = 0f
        while (x < size.width) {
            while (y < size.height) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.03f),
                    radius = 1.dp.toPx(),
                    center = Offset(x, y)
                )
                y += spacing
            }
            y = 0f
            x += spacing
        }
    }
)
```

---

## 7. Animations

| Interaction | Animation |
|---|---|
| Button hover | Glow fade-in 100ms, bg shift 100ms |
| Button press | Scale 0.98 (100ms), glow intensify |
| Card hover | Border color shift 150ms, subtle glow |
| Dialog open | Scale 0.95→1 + fade 200ms |
| Bottom sheet | Slide up 300ms with spring |
| Tab switch | Cross-fade 200ms |
| Loading | Shimmer sweep (accent at 5%) |
| Page transition | Slide + fade 300ms |

All animations respect `AccessibilityManager.isHighTextContrastEnabled` — disable or simplify when reduced motion is on.

---

## 8. Screen-Specific Guidance

### 8.1 Library Grid (Anime Cards)

- Card: `NeonCard`, 14dp radius, no glow (perf — grid has many cards)
- Cover image: fills card, no border
- Title: `fgPrimary`, body typography, 2-line max
- Metadata (episodes, score): `fgMuted`, mono
- Hover: border becomes `accentPrimary` at 20%, card lifts 2dp
- Selected: `accentPrimary` border at 100%, subtle glow

### 8.2 Anime Detail Screen

- Hero banner: full-width, 16:9, with gradient overlay (top transparent → bottom `bgBase`)
- Title: display typography, `fgPrimary`
- Metadata: mono, `fgSecondary`
- Action buttons: `NeonButton` (primary: "Watch Now", secondary: "Add to List")
- Episode list: `NeonListItem` rows, episode number in mono

### 8.3 Player UI

- Background: pure black (`#000000`) for true OLED
- Controls overlay: `bgGlass` + blur
- Scrubber: `accentPrimary` track, glow on the handle
- Buttons: `fgSecondary`, `accentPrimary` on hover
- Timestamps: mono, `fgSecondary`
- **Tone down:** reduce glow intensity on the scrubber (perf during playback)

### 8.4 Settings

- Section headers: title typography, `fgPrimary`, with accent left-border (2dp)
- List items: `NeonListItem`
- Toggles: `accentPrimary` when on, `borderStrong` when off
- Section dividers: 1dp `borderSubtle`

### 8.5 Browse Screen

- Source list: `NeonListItem`
- Source icon: 32dp, rounded
- Source name: `fgPrimary`, subtitle typography
- Selected source: `accentPrimary` bg at 10%

---

## 9. Accessibility

- **Contrast:** Neon accents on pure black = 7:1+ (AAA). On light mode, verify accents on `#F5F5F8` (should be 4.5:1+).
- **Color-blind:** Don't rely on color alone — use icons + text labels. E.g., error states use coral + an "!" icon + "Error" text.
- **Reduced motion:** Disable glow animations, simplify transitions to instant or fade-only.
- **High contrast mode:** Borders become 2dp solid `accentPrimary`, glow removed.

---

## 10. Reference

- Original DESIGN.md (coordinator dashboard) — adapted from this
- Apps with similar aesthetic: Cyberpunk 2077 UI, Deus Ex: Human Revolution, synthwave music apps
- Compose libraries: Haze (glass-morphism), custom Modifier for glow

---

*Neon is the most distinctive of the 4 designs. It gives Kuta a unified identity with the coordinator dashboard and OLED-perfect dark mode.*
