/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

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
 * A single freehand stroke drawn on top of the image.
 */
@Immutable
data class MarkupStroke(
    val points: ImmutableList<NormalizedPoint>,
    val color: MarkupColor,
) {
    fun rotateAntiClockwise() = copy(points = points.map { it.rotateAntiClockwise() }.toImmutableList())

    fun flipHorizontally() = copy(points = points.map { it.flipHorizontally() }.toImmutableList())

    fun flipVertically() = copy(points = points.map { it.flipVertically() }.toImmutableList())

    companion object {
        /**
         * The width of a stroke, as a fraction of the smallest side of the image, so that a
         * stroke covers the same part of the image on screen and in the exported file.
         */
        const val RELATIVE_WIDTH = 0.012f
    }
}

/**
 * A piece of content the user has placed on top of the image: an emoji, or some text.
 */
@Immutable
sealed interface MarkupStickerContent {
    data class Emoji(val unicode: String) : MarkupStickerContent
    data class Text(val text: String, val color: MarkupColor) : MarkupStickerContent
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
 * The colours the pen can draw with.
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
