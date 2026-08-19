/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A point on the image, normalized so that markup keeps its place on the image content
 * when the image is rotated, flipped or cropped.
 */
@Immutable
data class NormalizedPoint(
    val x: Float,
    val y: Float,
) {
    fun rotateAntiClockwise() = NormalizedPoint(x = y, y = 1f - x)

    fun flipHorizontally() = copy(x = 1f - x)

    fun flipVertically() = copy(y = 1f - y)
}

/**
 * The kind of freehand stroke a drag leaves behind: an opaque pen, or a broader translucent
 * highlighter which leaves the image readable underneath it.
 */
enum class MarkupStrokeKind(
    /** The width of the stroke, as a fraction of the smallest side of the image. */
    val relativeWidth: Float,
    @FloatRange(from = 0.0, to = 1.0) val alpha: Float,
) {
    Pen(relativeWidth = 0.012f, alpha = 1f),
    Highlighter(relativeWidth = 0.036f, alpha = 0.4f),
}

/**
 * How far the eraser reaches from the point it is dragged over, as a fraction of the height of
 * the image.
 */
const val ERASER_RELATIVE_RADIUS = 0.04f

/**
 * A single freehand stroke drawn on top of the image.
 */
@Immutable
data class MarkupStroke(
    val points: ImmutableList<NormalizedPoint>,
    val color: MarkupColor,
    val kind: MarkupStrokeKind = MarkupStrokeKind.Pen,
) {
    fun rotateAntiClockwise() = copy(points = points.map { it.rotateAntiClockwise() }.toImmutableList())

    fun flipHorizontally() = copy(points = points.map { it.flipHorizontally() }.toImmutableList())

    fun flipVertically() = copy(points = points.map { it.flipVertically() }.toImmutableList())

    /** Whether the stroke passes within [radius] of [point], both measured in image heights. */
    fun isNear(point: NormalizedPoint, radius: Float, aspectRatio: Float): Boolean {
        val polyline = points.map { it.toOffset(width = aspectRatio, height = 1f) }
        return distanceToPolyline(point.toOffset(width = aspectRatio, height = 1f), polyline) <= radius
    }
}

/**
 * The shapes that can be stamped onto the image by dragging from one corner to the other.
 */
enum class MarkupShapeKind {
    Arrow,
    Line,
    Rectangle,
    Ellipse,
}

/**
 * A shape drawn on the image, defined by the two ends of the drag that created it.
 *
 * Like a stroke, the ends are normalized against the image so that a shape stays on the part of
 * the image it was drawn on, and can be rendered into the image at its original resolution.
 */
@Immutable
data class MarkupShape(
    val kind: MarkupShapeKind,
    val start: NormalizedPoint,
    val end: NormalizedPoint,
    val color: MarkupColor,
) {
    fun rotateAntiClockwise() = copy(
        start = start.rotateAntiClockwise(),
        end = end.rotateAntiClockwise(),
    )

    fun flipHorizontally() = copy(
        start = start.flipHorizontally(),
        end = end.flipHorizontally(),
    )

    fun flipVertically() = copy(
        start = start.flipVertically(),
        end = end.flipVertically(),
    )

    fun strokeWidth(width: Float, height: Float) = min(width, height) * RELATIVE_WIDTH

    /**
     * The straight runs making up the shape, for an image laid out at [width] by [height].
     *
     * Both the editor and the exported image draw from this, so that a shape can't come out
     * differently in the file than it looked on screen. An ellipse has no straight runs and is
     * drawn from [ovalBounds] instead.
     */
    fun polylines(width: Float, height: Float): List<List<Offset>> {
        val startOffset = start.toOffset(width, height)
        val endOffset = end.toOffset(width, height)
        return when (kind) {
            MarkupShapeKind.Line -> listOf(listOf(startOffset, endOffset))
            MarkupShapeKind.Arrow -> {
                val headLength = strokeWidth(width, height) * ARROW_HEAD_WIDTH_RATIO
                val angle = atan2(endOffset.y - startOffset.y, endOffset.x - startOffset.x)
                val barbs = listOf(angle + PI_F - ARROW_HEAD_ANGLE, angle + PI_F + ARROW_HEAD_ANGLE).map { barbAngle ->
                    Offset(
                        x = endOffset.x + headLength * cos(barbAngle),
                        y = endOffset.y + headLength * sin(barbAngle),
                    )
                }
                listOf(
                    listOf(startOffset, endOffset),
                    listOf(barbs[0], endOffset, barbs[1]),
                )
            }
            MarkupShapeKind.Rectangle -> bounds(width, height).let { bounds ->
                listOf(
                    listOf(
                        Offset(bounds.left, bounds.top),
                        Offset(bounds.right, bounds.top),
                        Offset(bounds.right, bounds.bottom),
                        Offset(bounds.left, bounds.bottom),
                        Offset(bounds.left, bounds.top),
                    )
                )
            }
            MarkupShapeKind.Ellipse -> emptyList()
        }
    }

    /** The bounds an ellipse is drawn inside, or `null` for the shapes made of straight runs. */
    fun ovalBounds(width: Float, height: Float): Rect? = bounds(width, height).takeIf { kind == MarkupShapeKind.Ellipse }

    /** Whether the shape passes within [radius] of [point], both measured in image heights. */
    fun isNear(point: NormalizedPoint, radius: Float, aspectRatio: Float): Boolean {
        val target = point.toOffset(width = aspectRatio, height = 1f)
        val runs = when (kind) {
            // An ellipse is sampled so that it can be hit tested with the same code as the rest.
            MarkupShapeKind.Ellipse -> listOf(sampleOval(bounds(width = aspectRatio, height = 1f)))
            else -> polylines(width = aspectRatio, height = 1f)
        }
        return runs.any { distanceToPolyline(target, it) <= radius }
    }

    private fun bounds(width: Float, height: Float) = Rect(
        left = min(start.x, end.x) * width,
        top = min(start.y, end.y) * height,
        right = max(start.x, end.x) * width,
        bottom = max(start.y, end.y) * height,
    )

    companion object {
        /** The width of a shape, as a fraction of the smallest side of the image. */
        const val RELATIVE_WIDTH = 0.012f

        /** The length of an arrow's head, as a multiple of the stroke width. */
        const val ARROW_HEAD_WIDTH_RATIO = 5f

        /** How far each barb of an arrow's head opens away from its shaft. */
        private const val ARROW_HEAD_ANGLE = 0.5f

        private const val PI_F = Math.PI.toFloat()

        /** How many segments an ellipse is sampled into when it is hit tested. */
        private const val OVAL_SAMPLE_COUNT = 64

        private fun sampleOval(bounds: Rect): List<Offset> {
            val radiusX = bounds.width / 2f
            val radiusY = bounds.height / 2f
            return (0..OVAL_SAMPLE_COUNT).map { step ->
                val angle = 2f * PI_F * step / OVAL_SAMPLE_COUNT
                Offset(
                    x = bounds.center.x + radiusX * cos(angle),
                    y = bounds.center.y + radiusY * sin(angle),
                )
            }
        }
    }
}

/**
 * The typefaces text can be written in.
 */
enum class MarkupFont {
    SansSerif,
    Serif,
    Monospace,
    Cursive,
}

/**
 * A piece of content the user has placed on top of the image: an emoji, or some text.
 */
@Immutable
sealed interface MarkupStickerContent {
    data class Emoji(val unicode: String) : MarkupStickerContent
    data class Text(
        val text: String,
        val color: MarkupColor,
        val font: MarkupFont = MarkupFont.SansSerif,
    ) : MarkupStickerContent
}

/**
 * A sticker placed on the image. Like a stroke, its position is normalized against the image so
 * that it stays on the same part of the image when that is rotated, flipped or cropped.
 */
@Immutable
data class MarkupSticker(
    val id: Long,
    val content: MarkupStickerContent,
    val center: NormalizedPoint,
    /** The base font size, as a fraction of the smallest side of the image. */
    val relativeFontSize: Float,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
) {
    val text: String
        get() = when (content) {
            is MarkupStickerContent.Emoji -> content.unicode
            is MarkupStickerContent.Text -> content.text
        }

    val color: MarkupColor?
        get() = (content as? MarkupStickerContent.Text)?.color

    val font: MarkupFont?
        get() = (content as? MarkupStickerContent.Text)?.font

    val isText: Boolean
        get() = content is MarkupStickerContent.Text

    fun rotateAntiClockwise() = copy(
        center = center.rotateAntiClockwise(),
        rotationDegrees = rotationDegrees - 90f,
    )

    // The glyphs themselves are deliberately not mirrored, which would leave text unreadable,
    // so only the position and the angle follow the flip.
    fun flipHorizontally() = copy(
        center = center.flipHorizontally(),
        rotationDegrees = -rotationDegrees,
    )

    fun flipVertically() = copy(
        center = center.flipVertically(),
        rotationDegrees = -rotationDegrees,
    )

    companion object {
        const val EMOJI_RELATIVE_FONT_SIZE = 0.2f
        const val TEXT_RELATIVE_FONT_SIZE = 0.09f
        const val MIN_SCALE = 0.2f
        const val MAX_SCALE = 8f
    }
}

/**
 * The colours the pen, the highlighter, the shapes and text stickers can be drawn with.
 */
enum class MarkupColor(val value: Color) {
    White(Color.White),
    Black(Color.Black),
    Red(Color(0xFFF04949)),
    Orange(Color(0xFFFA9129)),
    Yellow(Color(0xFFFAD636)),
    Green(Color(0xFF40BF6B)),
    Blue(Color(0xFF368CEE)),
    Purple(Color(0xFF9C5CE8)),
}

internal fun NormalizedPoint.toOffset(width: Float, height: Float) = Offset(x = x * width, y = y * height)

/**
 * The distance from [point] to the nearest part of [polyline], or [Float.MAX_VALUE] when the
 * polyline is empty.
 */
private fun distanceToPolyline(point: Offset, polyline: List<Offset>): Float {
    if (polyline.isEmpty()) return Float.MAX_VALUE
    if (polyline.size == 1) return hypot(point.x - polyline[0].x, point.y - polyline[0].y)
    return (1 until polyline.size).minOf { index ->
        distanceToSegment(point, polyline[index - 1], polyline[index])
    }
}

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val segmentX = end.x - start.x
    val segmentY = end.y - start.y
    val lengthSquared = segmentX * segmentX + segmentY * segmentY
    if (lengthSquared == 0f) return hypot(point.x - start.x, point.y - start.y)
    val projection = ((point.x - start.x) * segmentX + (point.y - start.y) * segmentY) / lengthSquared
    val clamped = projection.coerceIn(0f, 1f)
    return hypot(
        point.x - (start.x + clamped * segmentX),
        point.y - (start.y + clamped * segmentY),
    )
}
