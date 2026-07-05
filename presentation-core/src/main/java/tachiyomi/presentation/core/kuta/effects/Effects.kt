package tachiyomi.presentation.core.kuta.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent

/**
 * FORK: Phase 2 — Effect Modifiers for the 4 design languages.
 * Per DOCS/design-system/01-neon.md §6, 02-notebook.md §6, 03-brutalist.md §6.
 *
 * These are custom Modifiers that implement design-specific visual effects.
 * Phase 2B subagents will use these in their component implementations.
 */

// ===== Neon Effects =====

/**
 * Neon glow effect — uses [Paint.setShadowLayer] (software-rendered).
 * Perf warning: avoid applying to every item in a large grid.
 * Per 01-neon.md §6.2.
 */
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
                color.copy(alpha = 0.5f).toArgb(),
            )
        }
        drawContext.canvas.nativeCanvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            10.dp.toPx(), 10.dp.toPx(),
            paint.asFrameworkPaint(),
        )
    },
)

/**
 * Subtle dot-grid background for the cyberpunk canvas feel.
 * Per 01-neon.md §6.3.
 */
fun Modifier.neonGridPattern(dotColor: Color = Color.White.copy(alpha = 0.03f)): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = 20.dp.toPx()
        var x = 0f
        var y = 0f
        while (x < size.width) {
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = 1.dp.toPx(),
                    center = Offset(x, y),
                )
                y += spacing
            }
            y = 0f
            x += spacing
        }
    },
)

// ===== Notebook Effects =====

/**
 * Warm soft paper shadow — the Notebook equivalent of elevation.
 * Uses Modifier.shadow with warm ambient/spot colors (brownish, not gray).
 * Per 02-notebook.md §6.5.
 */
fun Modifier.paperShadow(
    elevation: Dp = 4.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp),
    ambientColor: Color = Color(0xFF785F41).copy(alpha = 0.15f),
    spotColor: Color = Color(0xFF785F41).copy(alpha = 0.2f),
): Modifier = this.then(
    Modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
    ),
)

/**
 * Subtle dot-grid texture on paper surfaces.
 * Per 02-notebook.md §6.1.
 */
fun Modifier.paperTexture(dotColor: Color = Color(0xFF8B7355).copy(alpha = 0.06f)): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = 20.dp.toPx()
        var x = 0f
        var y = 0f
        while (x < size.width) {
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = 0.8.dp.toPx(),
                    center = Offset(x, y),
                )
                y += spacing
            }
            y = 0f
            x += spacing
        }
    },
)

/**
 * Horizontal ruled lines like a notebook page.
 * Per 02-notebook.md §6.2.
 */
fun Modifier.ruledLines(lineColor: Color, lineSpacing: Dp = 32.dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = lineSpacing.toPx()
        var y = spacing
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += spacing
        }
    },
)

/**
 * Vertical red margin line (like real notebooks).
 * Per 02-notebook.md §6.3.
 */
fun Modifier.notebookMarginLine(marginColor: Color, position: Dp = 48.dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        drawLine(
            color = marginColor,
            start = Offset(position.toPx(), 0f),
            end = Offset(position.toPx(), size.height),
            strokeWidth = 2.dp.toPx(),
        )
    },
)

// ===== Brutalist Effects =====

/**
 * Hard-edge shadow (zero blur) — the signature Brutalist effect.
 * Per 03-brutalist.md §6.1.
 */
fun Modifier.hardShadow(
    color: Color,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    cornerRadius: Dp = 10.dp,
): Modifier = this.then(
    Modifier.drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        )
    },
)

/**
 * Grid background — the signature Brutalist canvas.
 * Per 03-brutalist.md §6.2.
 */
fun Modifier.brutalistGrid(gridColor: Color, gridSize: Dp = 28.dp): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
        val spacing = gridSize.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            x += spacing
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += spacing
        }
    },
)

// ===== Glass Effect (Neon) =====
// Haze wrapper — Phase 2B subagent will implement using dev.chrisbanes.haze.
// For now, a simple semi-transparent overlay fallback.

/**
 * Glass-morphism effect using the Haze library.
 * Per 01-neon.md §6.1.
 *
 * Phase 2B: replace with real Haze implementation.
 */
// TODO: Phase 2B — implement glass effect using Haze library
