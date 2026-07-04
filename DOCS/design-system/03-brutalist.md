# 03 — Brutalist Design Language

> Raw, bold, unapologetic. Thick black borders, hard zero-blur shadows, bright saturated accents. Adapted from `NOTEBOOK_TEMPLATE.zip` for native Jetpack Compose.

---

## 1. Philosophy

| Principle | Description |
|---|---|
| **Raw and bold** | Thick 3px borders everywhere. No soft shadows, no gradients — just hard edges and solid color. |
| **Physical and 3D** | Hard-offset shadows make elements feel like physical objects stacked on a canvas. Press = object pushes into the canvas. |
| **Bright and saturated** | Accent colors are bold (blue, pink, green, yellow, orange, purple, red). No muted pastels here. |
| **Uppercase and heavy** | Typography is bold (weight 800), uppercase, tightly tracked. Loud and confident. |
| **Grid-aligned** | A visible grid background gives the "canvas" feel — elements sit on the grid. |
| **Color-burst interactions** | Hover = color tint shifts + shadow grows. Press = color deepens + shadow shrinks. Every interaction is a color event. |

---

## 2. Color Palette

### 2.1 Light Mode ("Acid Cream")

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#D9D5CC` | Warm off-white (darker than pure white — the "canvas") |
| `bgSurface` | `#EDEAE3` | Cards, content panels (lighter than bg) |
| `bgSidebar` | `#EDEAE3` | Sidebar |
| `bgElevated` | `#CDC9C0` | Hover (darker — inverted from other designs) |

| Accent Token | Hex | Role | Shadow Variant |
|---|---|---|---|
| `accentPrimary` | `#2563EB` (blue) | Primary actions | `shadowBlue` |
| `accentPink` | `#EC4899` | Tags, highlights | `shadowPink` |
| `accentGreen` | `#22C55E` | Success | `shadowGreen` |
| `accentYellow` | `#F59E0B` | Warnings | `shadowYellow` |
| `accentOrange` | `#F97316` | Highlights | `shadowOrange` |
| `accentPurple` | `#8B5CF6` | Special | `shadowPurple` |
| `accentRed` | `#EF4444` | Destructive | `shadowRed` |

> The user's selected accent (from Settings) becomes `accentPrimary`. The other accents are used for semantic states (success=green, warning=yellow, danger=red) and for the colored-shadow card variants.

### 2.2 Dark Mode ("Midnight Raw")

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#2A2A32` | Deep charcoal (the dark canvas) |
| `bgSurface` | `#363640` | Cards |
| `bgSidebar` | `#32323C` | Sidebar |
| `bgElevated` | `#3E3E48` | Hover |

Accent colors shift slightly brighter in dark mode (so they pop):

| Accent Token | Dark Hex |
|---|---|
| `accentPrimary` | `#3B82F6` |
| `accentPink` | `#F472B6` |
| `accentGreen` | `#4ADE80` |
| `accentYellow` | `#FBBF24` |
| `accentOrange` | `#FB923C` |
| `accentPurple` | `#A78BFA` |
| `accentRed` | `#FF3333` |

### 2.3 Brutalist-Specific Tokens

| Token | Light | Dark | Usage |
|---|---|---|---|
| `borderColor` | `#1A1A1A` (near-black) | `#555555` (gray) | The universal border color |
| `shadowColor` | `#1A1A1A` | `#1A1A1E` | Hard shadow color |
| `gridLineColor` | `rgba(26,26,26,0.14)` | `rgba(255,255,255,0.08)` | Background grid lines |
| `gridSize` | 28dp | 28dp | Background grid spacing |
| `hoverBgTint` | `#DBEAFE` (light blue wash) | `#3E4258` | Hover background tint |
| `activeBgTint` | `#BFDBFE` (deeper blue wash) | `#4A5068` | Press background tint |

### 2.4 Text Colors

| Token | Light | Dark |
|---|---|---|
| `fgPrimary` | `#1A1A1A` | `#E8E8E8` |
| `fgSecondary` | `#3A3A3A` | `#B0B0B8` |
| `fgMuted` | `#5A5A5A` | `#909098` |
| `fgDim` | `#7A7A7A` | `#707078` |

---

## 3. Typography

### 3.1 Font Stack

| Usage | Font | How To Load |
|---|---|---|
| Everything | Inter | Bundle TTF — use Black (900) weight for headings, Bold (700) for body emphasis |
| Optional accent | Space Grotesk | For display/hero headings if Inter feels too neutral |

### 3.2 Type Scale

| Element | Size | Weight | Transform |
|---|---|---|---|
| Display (hero) | 48sp | Black (900) | Uppercase, -0.02em tracking |
| Headline (screen title) | 28sp | Black (900) | Uppercase, -0.02em |
| Title (section) | 22sp | ExtraBold (800) | Uppercase, -0.02em |
| Subtitle | 16sp | Bold (700) | None |
| Body | 14sp | SemiBold (600) | None |
| Body Small | 12sp | Medium (500) | None |
| Label | 11sp | ExtraBold (800) | Uppercase, +0.04em |
| Button | 14sp | ExtraBold (800) | Uppercase, -0.02em |

> Brutalist uses heavier weights than the other designs. Uppercase is the default for headings and labels.

### 3.3 Section Header with Accent Bar

Section headers have a thick accent bar on the left:

```kotlin
@Composable
fun BrutalistSectionHeader(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(accentPrimary)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text.uppercase(),
            style = typography.title,
            color = fgPrimary
        )
    }
}
```

---

## 4. Spacing

Use shared `KutaSpacing`. Brutalist tends toward tighter spacing (denser, more "stacked" feel) but uses the same scale values.

---

## 5. Component Specs

### 5.1 BrutalistButton

- **Primary:** `accentPrimary` bg, white text, 3px black border, hard shadow
- **Secondary:** `bgSurface` bg, `fgPrimary` text, 3px black border
- **Destructive:** `accentRed` bg, white text
- **Ghost:** transparent, `fgMuted` text, no border
- **Height:** 48dp
- **Corner radius:** 8dp
- **Border:** 3dp solid `borderColor`
- **Shadow:** 3dp hard offset (`shadowColor`), zero blur
- **Hover:** bg shifts to `hoverBgTint`, shadow grows to 5dp, lift -1dp/-1dp
- **Press:** bg shifts to `activeBgTint`, shadow shrinks to 1dp, press +2dp/+2dp
- **Disabled:** 50% opacity

```kotlin
@Composable
fun BrutalistButton(text: String, onClick: () -> Unit, ...) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val offset = if (isPressed) Modifier.offset(2.dp, 2.dp) else if (isHovered) Modifier.offset((-1).dp, (-1).dp) else Modifier
    val shadowSize = if (isPressed) 1.dp else if (isHovered) 5.dp else 3.dp
    val bg = if (isPressed) activeBgTint else if (isHovered) hoverBgTint else accentPrimary

    Box(
        modifier = Modifier
            .height(48.dp)
            .then(offset)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(8.dp))
            .hardShadow(shadowColor, offset = shadowSize)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White,
            style = typography.button
        )
    }
}
```

### 5.2 BrutalistCard

- Background: `bgSurface`
- Border: 3dp solid `borderColor`
- Corner radius: 10dp
- Shadow: 4dp hard offset (`shadowColor`)
- Padding: 20dp
- Hover: bg shifts to `hoverBgTint`, shadow grows to 8dp, lift -2dp/-2dp
- Press: bg shifts to `activeBgTint`, shadow shrinks to 1dp, press +3dp/+3dp

### 5.3 BrutalistCard Variants (Colored Shadows)

Cards can have colored shadows for visual variety:

- `BrutalistCardBlue` — shadow uses `accentBlue`
- `BrutalistCardPink` — shadow uses `accentPink`
- `BrutalistCardGreen` — shadow uses `accentGreen`
- `BrutalistCardYellow` — shadow uses `accentYellow`
- `BrutalistCardOrange` — shadow uses `accentOrange`
- `BrutalistCardPurple` — shadow uses `accentPurple`

Hover/active bg tints match the shadow color (e.g., `BrutalistCardBlue` hovers to light blue `#DBEAFE`).

### 5.4 BrutalistInput

- Background: `bgSurface`
- Border: 3dp solid `borderColor`
- Height: 44dp
- Corner radius: 8dp
- Focus: bg shifts to `hoverBgTint`, shadow appears (3dp `accentPrimary`)
- Placeholder: `fgMuted`

### 5.5 BrutalistDialog

- Background: `bgSurface`
- Border: 3dp solid `borderColor`
- Corner radius: 10dp
- Shadow: 5dp hard offset
- Max width: 560dp

### 5.6 BrutalistNavigationBar (Bottom)

- Background: `bgSidebar`
- Border top: 3dp solid `borderColor`
- Shadow: 4dp hard offset (top, into the nav bar)
- Active item: `accentPrimary` bg, white icon, 2dp border, hard shadow
- Inactive item: `fgMuted` icon, no border
- Height: 64dp

### 5.7 BrutalistTopAppBar

- Background: `bgSidebar`
- Border bottom: 3dp solid `borderColor`
- Height: 56dp
- Title: `fgPrimary`, headline typography, uppercase
- Back button: `fgPrimary`, 2dp border, `accentPrimary` bg on hover

### 5.8 BrutalistListItem

- Background: transparent
- Hover: `hoverBgTint` bg, 2dp border appears, hard shadow 2dp
- Active: `accentPrimary` bg, white text
- Padding: 14dp vertical, 16dp horizontal
- Divider: none (borders separate items in Brutalist)

### 5.9 BrutalistChip

- Background: `bgSurface`
- Border: 2dp solid `borderColor`
- Text: `fgPrimary`, label typography (uppercase)
- Corner radius: 6dp
- Slight rotation (-1deg) for rawness
- Variants: colored bg (yellow, pink, green) for tags

### 5.10 BrutalistBadge

- Background: `accentYellow` (or other bright color)
- Border: 2dp solid `borderColor`
- Text: `fgPrimary`
- Corner radius: 4dp
- Slight rotation (-2deg) — looks hand-pinned

### 5.11 BrutalistSkeleton

- Background: `bgElevated`
- Border: 2dp DASHED `borderColor` (raw, unfinished look)
- Shimmer: diagonal sweep
- Corner radius: 10dp

---

## 6. Effects — Compose Implementation

### 6.1 Hard-Edge Shadow (Zero Blur)

**Problem:** Compose's `Modifier.shadow()` always blurs. Brutalist needs zero-blur hard offsets.

**Solution:** Custom `Modifier` using `drawBehind`:

```kotlin
fun Modifier.hardShadow(
    color: Color = shadowColor,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
): Modifier = this.then(
    Modifier.drawBehind {
        // Draw the shadow as a solid colored rect, offset
        val offsetXPx = offsetX.toPx()
        val offsetYPx = offsetY.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(offsetXPx, offsetYPx),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
    }
)
```

**Note:** The shadow is drawn BEHIND the content. The element itself needs a background (opaque) so the shadow doesn't show through. The border goes ON TOP of the background.

### 6.2 Grid Background

The signature Brutalist grid:

```kotlin
fun Modifier.brutalistGrid(): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val gridSize = 28.dp.toPx()
        val gridColor = gridLineColor
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += gridSize
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += gridSize
        }
    }
)
```

Apply to the screen background (not cards — cards sit ON the grid).

### 6.3 Press Animation (Shadow Shrink + Translate)

The signature Brutalist interaction — element "presses into" the canvas:

```kotlin
@Composable
fun Modifier.brutalistPress(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    return this.then(
        if (isPressed) {
            Modifier
                .offset(2.dp, 2.dp)
                .hardShadow(color = shadowColor, offsetX = 1.dp, offsetY = 1.dp)
        } else {
            Modifier
                .hardShadow(color = shadowColor, offsetX = 3.dp, offsetY = 3.dp)
        }
    )
}
```

### 6.4 Pop-In Animation

Elements pop in with a slight rotate (signature Brutalist entrance):

```kotlin
@Composable
fun brutalistPopIn(): EnterTransition = fadeIn(tween(200)) + scaleIn(
    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
    initialScale = 0.8f
) + rotateIn(initialAngle = -2f)
```

---

## 7. Animations

| Interaction | Animation |
|---|---|
| Button hover | Bg color shift, shadow 3→5dp, lift -1dp (100ms) |
| Button press | Bg color shift, shadow 5→1dp, press +2dp (100ms) |
| Card hover | Bg tint shift, shadow 4→8dp, lift -2dp (150ms) |
| Card press | Bg tint shift, shadow 8→2dp, press +3dp (150ms) |
| Dialog open | Pop-in: scale 0.8→1 + rotate -2→0deg + fade (250ms spring) |
| Bottom sheet | Slide up with spring (300ms) |
| Tab switch | Cross-fade (150ms) |
| Loading | Skeleton pulse + diagonal shimmer |
| Page transition | Slide + fade (200ms) |
| Element entrance | Pop-in: scale + rotate + fade (350ms spring) |

**Reduced motion:** Disable rotations and translates. Keep opacity-only.

---

## 8. Screen-Specific Guidance

### 8.1 Library Grid (Anime Cards)

- Card: `BrutalistCard` with 3.5dp border, 5dp hard shadow (slightly thicker for anime cards)
- Cover image: fills top 2/3 of card, NO border (image is the visual)
- Title: `fgPrimary`, body typography (bold), 2-line max
- Metadata: `fgMuted`, mono, in a small badge
- Hover: bg shifts to `hoverBgTint`, shadow grows to 9dp, lift -2dp
- Press: bg shifts to `activeBgTint`, shadow shrinks to 2dp, press +3dp
- Selected: `accentPrimary` border + colored shadow

### 8.2 Anime Detail Screen

- Hero banner: full-width, with `borderBottom` 3dp, hard shadow below
- Title: display typography, uppercase, `fgPrimary`
- Metadata: in `BrutalistBadge` components (genre badges, score badge)
- Action buttons: `BrutalistButton` (primary: "WATCH NOW", secondary: "ADD TO LIST")
- Episode list: `BrutalistListItem` rows, episode number in bold mono

### 8.3 Player UI

- Background: `bgBase` (charcoal, not pure black)
- Controls overlay: `bgSurface` with 3dp top border
- Scrubber: `accentPrimary` track, 3dp border around handle, hard shadow
- Buttons: `fgPrimary`, 2dp border, `accentPrimary` bg on hover
- Timestamps: bold mono, `fgSecondary`
- **Tone down:** remove grid background during playback (distracting over video)

### 8.4 Settings

- Section headers: title typography, uppercase, with accent left-bar (4dp) + hard shadow on the bar
- List items: `BrutalistListItem`
- Toggles: `accentPrimary` bg when on, hard shadow
- No section dividers — borders separate items

### 8.5 Browse Screen

- Source list: `BrutalistListItem`
- Source icon: 32dp, 2dp border, hard shadow
- Source name: `fgPrimary`, subtitle typography, uppercase
- Selected source: `accentPrimary` bg, white text, hard shadow

---

## 9. Accessibility

- **Contrast:** Black on bright colors = 7:1+ (AAA). Verify all colored-shadow variants maintain 4.5:1.
- **Color-blind:** Bright accent colors can be hard for some color-blind users. Don't rely on color alone — use icons + text labels. E.g., "NEW" badge has the word "NEW" not just a colored dot.
- **Reduced motion:** Disable rotations, translates, and shadow-size changes. Keep opacity-only.
- **Border thickness:** 3dp borders are visually heavy but help low-vision users. Good for accessibility.

---

## 10. Reference

- Original template: `NOTEBOOK_TEMPLATE.zip` (AniVerse Neobrutalist Design System)
- Apps with similar aesthetic: Gumroad, Linear, Are.na, Vercel dashboard, Figma config screens
- Fonts: Inter (Google Fonts, free), Space Grotesk (Google Fonts, free)

---

*Brutalist is the "loud" option — confident, physical, and unmistakable. It gives Kuta a distinctive identity that stands out from every other anime app.*
