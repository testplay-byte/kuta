# 02 — Notebook Design Language

> Cozy journal / coffee-stained notebook aesthetic. Warm earth tones, paper textures, washi tape, sticky notes. Adapted from `COFFEE_TEMPLATE.zip` for native Jetpack Compose.

---

## 1. Philosophy

| Principle | Description |
|---|---|
| **Warm and tactile** | Every surface feels like paper — warm, slightly textured, inviting touch. |
| **Handcrafted imperfection** | Subtle rotations, hand-drawn accents, washi tape — feels made by a person, not a machine. |
| **Coffee-stained comfort** | Earth tones (browns, creams, sages) — never harsh, always cozy. |
| **Margin-aware** | Real notebooks have margins; our UI respects them. Left red margin line, top/bottom breathing room. |
| **Ink, not pixels** | Text feels written, not rendered. Headers have ink underlines; emphasis uses ink weight. |
| **Gentle motion** | Animations are slow and organic — paper doesn't snap, it settles. |

---

## 2. Color Palette

### 2.1 Light Mode ("Coffee White")

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#F0E6D8` | Warm off-white background (the "page") |
| `bgSurface` | `#FDF8F2` | Cards, content panels (slightly lighter than bg) |
| `bgSidebar` | `#FDF8F2` | Sidebar |
| `bgElevated` | `#E8DDD0` | Hover, active |
| `bgPaper` | `#FDF8F2` | Paper-textured surfaces (cards with texture) |

| Accent Token | Hex | Role | Semantic |
|---|---|---|---|
| `accentPrimary` | `#B8653F` (coffee brown) | Primary actions, active states | Default |
| `accentSecondary` | `#6B8E5B` (sage green) | Success, completion | Success |
| `accentTertiary` | `#C44040` (warm red) | Destructive, errors | Danger |
| `accentQuaternary` | `#C99545` (caramel) | Warnings, highlights | Warning |
| `accentPlum` | `#966B94` | Special tags, rare accents | Neutral accent |

### 2.2 Dark Mode ("Dark Coffee")

| Token | Hex | Usage |
|---|---|---|
| `bgBase` | `#1A1412` | Deep brown-black (the "dark page") |
| `bgSurface` | `#2A2220` | Cards, panels |
| `bgSidebar` | `#2A2220` | Sidebar |
| `bgElevated` | `#3A3230` | Hover, active |
| `bgPaper` | `#2A2220` | Paper-textured surfaces |

| Accent Token | Hex | Role |
|---|---|---|
| `accentPrimary` | `#D4956A` (latte) | Primary actions |
| `accentSecondary` | `#7B9E6B` (sage) | Success |
| `accentTertiary` | `#E06060` (warm red) | Destructive |
| `accentQuaternary` | `#D4A55A` (caramel) | Warnings |

### 2.3 Notebook-Specific Tokens

| Token | Light | Dark | Usage |
|---|---|---|---|
| `ruledLine` | `#DDD2C2` | `#3A3230` | Horizontal ruled lines on paper |
| `marginLine` | `#D4A0A0` | `#5A3A3A` | Vertical left margin line (like real notebooks) |
| `paperShadow` | `rgba(120,95,65,0.15)` | `rgba(0,0,0,0.25)` | Soft warm shadow on cards |
| `paperShadowHover` | `rgba(120,95,65,0.25)` | `rgba(0,0,0,0.35)` | Stronger shadow on hover |
| `stickyNote` | `#FFF8CC` | `#3A3220` | Sticky note background (yellowish) |
| `washiTape` | `rgba(210,190,160,0.8)` | `rgba(80,70,60,0.5)` | Washi tape decoration |

### 2.4 Text Colors

| Token | Light | Dark |
|---|---|---|
| `fgPrimary` | `#2E1A0E` (dark coffee) | `#F0E0D0` (cream) |
| `fgSecondary` | `#4A3425` | `#D4B8A0` |
| `fgMuted` | `#7A6450` | `#A89080` |
| `fgDim` | `#9A8470` | `#7A6855` |

### 2.5 Border Colors

| Pattern | Light | Dark |
|---|---|---|
| Default | `#D5C8B8` | `#3A3230` |
| Subtle | `#E5DAD0` | `#2E2624` |
| Strong | `#B5A590` | `#4A4240` |
| Accent | `accentPrimary` at 30% | same |

---

## 3. Typography

### 3.1 Font Stack

| Usage | Font | How To Load |
|---|---|---|
| Body / UI | Inter | Bundle TTF in `res/font/` |
| Hand-written accents | Caveat (or similar handwritten font) | Bundle TTF — used for hero titles, sticky notes, casual labels |

### 3.2 Type Scale

| Element | Size | Weight | Font |
|---|---|---|---|
| Display (hero, hand-written) | 48sp | Bold | Caveat |
| Headline (screen title) | 28sp | Bold | Inter |
| Title (section) | 22sp | Semibold | Inter |
| Subtitle | 16sp | Semibold | Inter |
| Body | 14sp | Regular | Inter |
| Body Small | 12sp | Regular | Inter |
| Label | 11sp | Medium | Inter, uppercase, +0.08em |
| Hand-written | 20sp | Regular | Caveat (for sticky notes, casual labels) |

### 3.3 Ink Underline

Headers and emphasized text get an "ink underline" — a slightly skewed, semi-transparent accent-colored bar beneath the text:

```kotlin
@Composable
fun InkUnderlineText(text: String, ...) {
    Box {
        Text(text, ...)
        // Draw a slightly rotated, semi-transparent accent bar beneath
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotate(-0.5f)
                .offset(y = 2.dp)
                .background(accentPrimary.copy(alpha = 0.3f))
                .height(3.dp)
        )
    }
}
```

---

## 4. Spacing

Use shared `KutaSpacing`. Notebook adds a "margin" concept: left padding of 48dp on paper surfaces (mimicking a notebook's red margin line).

---

## 5. Component Specs

### 5.1 NotebookButton

- **Primary:** `accentPrimary` bg, `#FFFAF5` text, paper shadow
- **Secondary:** `bgSurface` bg, `accentPrimary` text, `accentPrimary` border
- **Destructive:** `accentTertiary` bg, white text
- **Ghost:** transparent, `fgMuted` text
- **Height:** 48dp
- **Corner radius:** 8dp (slightly less rounded — paper-like)
- **Shadow:** `paperShadow` (warm, soft)
- **Hover:** lift 2dp + rotate -0.3deg (paper feel), shadow grows
- **Press:** press down 1dp, shadow shrinks

```kotlin
@Composable
fun NotebookButton(text: String, onClick: () -> Unit, ...) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentPrimary)
            .paperShadow(if (isHovered) PaperShadow.Hover else PaperShadow.Default)
            .then(if (isHovered) Modifier.offset(y = (-2).dp).rotate(-0.3f) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Text(text, color = Color(0xFFFFFAF5), style = typography.body)
    }
}
```

### 5.2 NotebookCard

- Background: `bgPaper` with paper texture overlay
- Border: 1dp `borderDefault`
- Corner radius: 10dp
- Shadow: `paperShadow`
- Padding: 20dp
- Hover: lift 3dp + rotate -0.3deg, shadow grows to `paperShadowHover`
- Optional washi tape decoration on top

### 5.3 NotebookInput

- Background: `bgSurface`
- Border: 1dp `borderDefault`, becomes `accentPrimary` on focus
- Height: 44dp
- Corner radius: 8dp
- Focus: subtle paper shadow
- Placeholder: `fgDim` in italic

### 5.4 NotebookDialog

- Background: `bgPaper` with paper texture
- Border: 1dp `borderStrong`
- Corner radius: 12dp
- Shadow: deep `paperShadow`
- Optional washi tape at top
- Max width: 560dp

### 5.5 NotebookNavigationBar (Bottom)

- Background: `bgSurface` with paper texture
- Border top: 1dp `borderDefault`
- Active item: `accentPrimary` icon + small ink underline
- Inactive item: `fgMuted` icon
- Height: 64dp

### 5.6 NotebookTopAppBar

- Background: `bgSurface` (paper texture optional)
- Height: 56dp
- Title: `fgPrimary`, headline typography, with optional ink underline
- Back button: `fgSecondary`, `accentPrimary` on hover

### 5.7 NotebookListItem

- Background: transparent
- Hover: `bgElevated` at 50%
- Padding: 16dp vertical, 24dp horizontal
- Divider: 1dp `borderSubtle` (dashed option for "torn paper" feel)

### 5.8 NotebookChip

- Background: `accentPrimary` at 15% opacity
- Border: 1dp `accentPrimary` at 30%
- Text: `accentPrimary`
- Corner radius: 6dp
- Optional: slight rotation (-1deg) for hand-placed feel

### 5.9 NotebookBadge

- Background: `stickyNote` (yellowish)
- Text: `fgPrimary`
- Corner radius: 4dp
- Slight rotation (-2deg) — looks hand-pinned
- Optional: small "pin" icon

### 5.10 NotebookSkeleton

- Background: `bgElevated`
- Shimmer: warm-tinted sweep (caramel at 10%)
- Dashed border (looks like a sketch/placeholder)

### 5.11 StickyNote (Special Component)

A distinctive Notebook component — a small square note with a pin:

- Background: `stickyNote`
- Shadow: `paperShadow`
- Slight rotation (-2 to +2 deg, randomized per instance)
- Pin icon at top-center
- Used for: notifications, tips, "new" indicators

---

## 6. Effects — Compose Implementation

### 6.1 Paper Texture

A subtle dot-grid or noise overlay on paper surfaces:

```kotlin
fun Modifier.paperTexture(): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        // Overlay subtle dot grid
        val spacing = 20.dp.toPx()
        var x = 0f
        var y = 0f
        while (x < size.width) {
            while (y < size.height) {
                drawCircle(
                    color = Color(0xFF8B7355).copy(alpha = 0.06f),
                    radius = 0.8.dp.toPx(),
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

For dark mode, use `Color(0xFFFFDCB4).copy(alpha = 0.04f)`.

### 6.2 Ruled Lines Background

Horizontal lines like a real notebook page:

```kotlin
fun Modifier.ruledLines(lineSpacing: Dp = 32.dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = lineSpacing.toPx()
        var y = spacing
        while (y < size.height) {
            drawLine(
                color = ruledLineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
            y += spacing
        }
    }
)
```

### 6.3 Margin Line (Vertical, Red)

The classic notebook red margin line on the left:

```kotlin
fun Modifier.notebookMarginLine(position: Dp = 48.dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = marginLineColor,
            start = Offset(position.toPx(), 0f),
            end = Offset(position.toPx(), size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
)
```

### 6.4 Washi Tape Decoration

A semi-transparent strip at the top of a card, slightly rotated:

```kotlin
@Composable
fun WashiTape(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(18.dp)
            .rotate(-2f)
            .background(washiTapeColor)
            .clip(RoundedCornerShape(2.dp))
    )
}
```

### 6.5 Paper Shadow (Warm Soft Shadow)

```kotlin
fun Modifier.paperShadow(intensity: PaperShadowIntensity = PaperShadowIntensity.Default): Modifier =
    this.then(
        Modifier.shadow(
            elevation = when (intensity) {
                PaperShadowIntensity.None -> 0.dp
                PaperShadowIntensity.Default -> 4.dp
                PaperShadowIntensity.Hover -> 8.dp
            },
            shape = RoundedCornerShape(10.dp),
            ambientColor = Color(0xFF785F41).copy(alpha = 0.15f),
            spotColor = Color(0xFF785F41).copy(alpha = 0.2f)
        )
    )
```

### 6.6 Coffee Ring Stain (Decorative)

Optional decorative element — a faint coffee ring on certain screens:

```kotlin
@Composable
fun CoffeeRingStain(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(120.dp)) {
        drawCircle(
            color = Color(0xFF6F4E37).copy(alpha = 0.08f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
```

### 6.7 Spiral Binding (For Hero Screens)

Decorative spiral binding dots on the left edge of certain screens (onboarding, hero):

```kotlin
fun Modifier.spiralBinding(): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = 40.dp.toPx()
        val dotRadius = 4.dp.toPx()
        var y = 20.dp.toPx()
        while (y < size.height) {
            drawCircle(
                color = ruledLineColor,
                radius = dotRadius,
                center = Offset(12.dp.toPx(), y)
            )
            y += spacing
        }
    }
)
```

---

## 7. Animations

| Interaction | Animation |
|---|---|
| Button hover | Lift 2dp + rotate -0.3deg, shadow grows (200ms ease-out) |
| Button press | Press down 1dp, shadow shrinks (100ms) |
| Card hover | Lift 3dp + rotate -0.3deg, shadow grows (200ms) |
| Dialog open | Scale 0.95→1 + fade + slight rotate (250ms) |
| Bottom sheet | Slide up with settle (300ms, organic ease) |
| Tab switch | Cross-fade (200ms) |
| Loading | Shimmer sweep (caramel tint, 1.5s) |
| Page transition | Slide + fade (300ms, organic) |
| Sticky note appear | Scale 0.8→1 + rotate -2→+2deg (spring, 350ms) |

**Reduced motion:** Disable rotations and lifts. Keep only opacity changes.

---

## 8. Screen-Specific Guidance

### 8.1 Library Grid (Anime Cards)

- Card: `NotebookCard` with paper texture, no rotation on hover (perf for grids)
- Cover image: fills top 2/3 of card, no border
- Title: `fgPrimary`, body typography, 2-line max, with ink underline on hover
- Metadata: `fgMuted`, italic (hand-written feel for episode count)
- Hover: lift 2dp (no rotate — too busy on grids), shadow grows
- Selected: `accentPrimary` border at 50%, sticky-note-style "selected" tag

### 8.2 Anime Detail Screen

- Hero banner: full-width, with paper-texture overlay at bottom (fades into `bgBase`)
- Title: display typography in Caveat (hand-written), `fgPrimary`
- Metadata: body, `fgSecondary`
- Action buttons: `NotebookButton` (primary: "Watch Now", secondary: "Add to List")
- Synopsis: body, `fgSecondary`, on a `bgPaper` panel with margin line
- Episode list: `NotebookListItem` rows, episode number in hand-written style

### 8.3 Player UI

- Background: `bgBase` (warm dark, not pure black)
- Controls overlay: `bgPaper` + paper texture
- Scrubber: `accentPrimary` track, paper-shadow on handle
- Buttons: `fgSecondary`, `accentPrimary` on hover
- Timestamps: body, `fgSecondary`
- **Tone down:** remove paper texture on controls (visual noise over video)

### 8.4 Settings

- Section headers: title typography, `fgPrimary`, with ink underline, in Caveat for cozy feel
- List items: `NotebookListItem`
- Toggles: `accentPrimary` when on
- Section dividers: 1dp dashed `borderSubtle` (torn-paper feel)
- Sticky notes for tips/help text

### 8.5 Browse Screen

- Source list: `NotebookListItem`
- Source icon: 32dp, with paper-shadow
- Source name: `fgPrimary`, subtitle typography
- Selected source: `accentPrimary` bg at 15%, ink underline

### 8.6 Onboarding (Special — Notebook Is Perfect Here)

- Full-screen pages with paper texture + ruled lines
- Spiral binding on the left edge (decorative)
- Hand-written titles in Caveat
- Body text in Inter
- "Next" button as `NotebookButton`
- Progress indicator as a series of dots (like bullet points in a notebook)

---

## 9. Accessibility

- **Contrast:** Coffee brown `#B8653F` on cream `#F0E6D8` = 5.2:1 (AA pass). Verify all accent combos meet 4.5:1.
- **Color-blind:** Earth tones can be hard for red-green color-blind users. Don't rely on sage green alone for success — use a checkmark icon + "Success" text.
- **Reduced motion:** Disable rotations and lifts. Keep opacity-only animations.
- **Paper texture:** Keep opacity very low (6%) so it doesn't interfere with readability.

---

## 10. Reference

- Original template: `COFFEE_TEMPLATE.zip` (AniVerse Notebook Design System)
- Apps with similar aesthetic: Bear notes app, physical journal apps, cozy reading apps
- Fonts: Caveat (Google Fonts, free), Inter (Google Fonts, free)

---

*Notebook is the "cozy" option — warm, tactile, and human. It pairs naturally with onboarding and reading-heavy screens.*
