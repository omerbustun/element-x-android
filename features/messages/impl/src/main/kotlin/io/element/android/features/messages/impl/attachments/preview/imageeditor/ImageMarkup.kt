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
