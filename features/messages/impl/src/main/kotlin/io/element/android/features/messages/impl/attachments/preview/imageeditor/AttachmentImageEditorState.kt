/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val DEFAULT_CROP_MARGIN = 0f
private const val MIN_CROP_SIZE = 0.1f

@Immutable
data class AttachmentImageEditorState(
    val localMedia: LocalMedia,
    val edits: AttachmentImageEdits,
    val activeTool: ImageEditorTool,
    /** The colour used by the pen, the highlighter, the shapes and new text stickers. */
    val markupColor: MarkupColor,
    val shapeKind: MarkupShapeKind,
    val selectedStickerId: Long?,
    val stickerPicker: StickerPicker,
    // For preview only
    val previewDebug: Boolean,
)

/**
 * The tool the editor is currently driven by. Only one can handle drags at a time.
 */
enum class ImageEditorTool {
    Crop,
    Pen,
    Highlighter,
    Shape,
    Eraser,
    Sticker;

    /** Whether what this tool draws takes the colour selected in the palette. */
    val usesMarkupColor: Boolean
        get() = this == Pen || this == Highlighter || this == Shape || this == Sticker

    /** Whether a drag with this tool draws a freehand stroke, and of which kind. */
    val strokeKind: MarkupStrokeKind?
        get() = when (this) {
            Pen -> MarkupStrokeKind.Pen
            Highlighter -> MarkupStrokeKind.Highlighter
            else -> null
        }
}

/**
 * The picker currently shown on top of the editor, used to create a new sticker.
 */
enum class StickerPicker {
    None,
    Emoji,
    Text,
}

@Immutable
data class AttachmentImageEdits(
    val cropRect: NormalizedCropRect = NormalizedCropRect.default(),
    val rotationQuarterTurns: Int = 0,
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    val strokes: ImmutableList<MarkupStroke> = persistentListOf(),
    val shapes: ImmutableList<MarkupShape> = persistentListOf(),
    val stickers: ImmutableList<MarkupSticker> = persistentListOf(),
) {
    val normalizedRotationQuarterTurns: Int
        get() = rotationQuarterTurns % 4

    val rotationDegrees: Int
        get() = normalizedRotationQuarterTurns * 90

    val hasChanges: Boolean
        get() = cropRect != NormalizedCropRect.default() ||
            normalizedRotationQuarterTurns != 0 ||
            isFlippedHorizontally ||
            isFlippedVertically ||
            strokes.isNotEmpty() ||
            shapes.isNotEmpty() ||
            stickers.isNotEmpty()

    fun rotateAntiClockwise(): AttachmentImageEdits {
        return copy(
            rotationQuarterTurns = (normalizedRotationQuarterTurns + 3) % 4,
            // Also update the crop rect and the markup to keep the same selected area
            cropRect = cropRect.rotateAntiClockwise(),
            strokes = strokes.map { it.rotateAntiClockwise() }.toImmutableList(),
            shapes = shapes.map { it.rotateAntiClockwise() }.toImmutableList(),
            stickers = stickers.map { it.rotateAntiClockwise() }.toImmutableList(),
        )
    }

    fun flipHorizontally(): AttachmentImageEdits {
        return copy(
            isFlippedHorizontally = !isFlippedHorizontally,
            // Also update the crop rect and the markup to keep the same selected area
            cropRect = cropRect.flipHorizontally(),
            strokes = strokes.map { it.flipHorizontally() }.toImmutableList(),
            shapes = shapes.map { it.flipHorizontally() }.toImmutableList(),
            stickers = stickers.map { it.flipHorizontally() }.toImmutableList(),
        )
    }

    fun flipVertically(): AttachmentImageEdits {
        return copy(
            isFlippedVertically = !isFlippedVertically,
            // Also update the crop rect and the markup to keep the same selected area
            cropRect = cropRect.flipVertically(),
            strokes = strokes.map { it.flipVertically() }.toImmutableList(),
            shapes = shapes.map { it.flipVertically() }.toImmutableList(),
            stickers = stickers.map { it.flipVertically() }.toImmutableList(),
        )
    }

    fun addStroke(stroke: MarkupStroke) = copy(strokes = (strokes + stroke).toImmutableList())

    fun removeLastStroke() = copy(strokes = strokes.dropLast(1).toImmutableList())

    fun addShape(shape: MarkupShape) = copy(shapes = (shapes + shape).toImmutableList())

    fun removeLastShape() = copy(shapes = shapes.dropLast(1).toImmutableList())

    /**
     * Rubs out the strokes and shapes passing within [radius] of [point], both measured in image
     * heights, with [aspectRatio] the width of the image over its height.
     *
     * Whole strokes and shapes are removed rather than the parts of them under the eraser, so
     * that nothing invisible is left behind in the exported image.
     */
    fun eraseAt(
        point: NormalizedPoint,
        radius: Float,
        aspectRatio: Float,
    ) = copy(
        strokes = strokes.filterNot { it.isNear(point, radius, aspectRatio) }.toImmutableList(),
        shapes = shapes.filterNot { it.isNear(point, radius, aspectRatio) }.toImmutableList(),
    )

    fun addSticker(sticker: MarkupSticker) = copy(stickers = (stickers + sticker).toImmutableList())

    fun updateSticker(sticker: MarkupSticker) = copy(
        stickers = stickers.map { if (it.id == sticker.id) sticker else it }.toImmutableList(),
    )

    fun removeSticker(id: Long) = copy(stickers = stickers.filterNot { it.id == id }.toImmutableList())
}

@Immutable
data class NormalizedCropRect(
    @FloatRange(from = 0.0, to = 1.0) val left: Float,
    @FloatRange(from = 0.0, to = 1.0) val top: Float,
    @FloatRange(from = 0.0, to = 1.0) val right: Float,
    @FloatRange(from = 0.0, to = 1.0) val bottom: Float,
) {
    init {
        require(left in 0f..1f)
        require(top in 0f..1f)
        require(right in 0f..1f)
        require(bottom in 0f..1f)
        require(left < right)
        require(top < bottom)
    }

    val width: Float
        get() = right - left

    val height: Float
        get() = bottom - top

    fun applyChange(
        dragTarget: CropDragTarget,
        deltaX: Float,
        deltaY: Float,
    ): NormalizedCropRect = when (dragTarget) {
        is CropDragTarget.Move -> translate(deltaX, deltaY)
        is CropDragTarget.Corner -> dragWithCorner(dragTarget, deltaX, deltaY)
        is CropDragTarget.Edge -> dragWithEdge(dragTarget, deltaX, deltaY)
    }

    private fun translate(deltaX: Float, deltaY: Float): NormalizedCropRect {
        val clampedLeft = (left + deltaX).coerceIn(0f, 1f - width)
        val clampedTop = (top + deltaY).coerceIn(0f, 1f - height)
        return copy(
            left = clampedLeft,
            top = clampedTop,
            right = clampedLeft + width,
            bottom = clampedTop + height,
        )
    }

    private fun dragWithCorner(
        dragTarget: CropDragTarget.Corner,
        deltaX: Float,
        deltaY: Float,
    ) = when (dragTarget) {
        CropDragTarget.Corner.TopLeft -> copy(
            left = (left + deltaX).coerceIn(0f, right - MIN_CROP_SIZE),
            top = (top + deltaY).coerceIn(0f, bottom - MIN_CROP_SIZE),
        )
        CropDragTarget.Corner.TopRight -> copy(
            right = (right + deltaX).coerceIn(left + MIN_CROP_SIZE, 1f),
            top = (top + deltaY).coerceIn(0f, bottom - MIN_CROP_SIZE),
        )
        CropDragTarget.Corner.BottomRight -> copy(
            right = (right + deltaX).coerceIn(left + MIN_CROP_SIZE, 1f),
            bottom = (bottom + deltaY).coerceIn(top + MIN_CROP_SIZE, 1f),
        )
        CropDragTarget.Corner.BottomLeft -> copy(
            left = (left + deltaX).coerceIn(0f, right - MIN_CROP_SIZE),
            bottom = (bottom + deltaY).coerceIn(top + MIN_CROP_SIZE, 1f),
        )
    }

    private fun dragWithEdge(
        dragTarget: CropDragTarget.Edge,
        deltaX: Float,
        deltaY: Float,
    ) = when (dragTarget) {
        CropDragTarget.Edge.Top -> copy(
            top = (top + deltaY).coerceIn(0f, bottom - MIN_CROP_SIZE),
        )
        CropDragTarget.Edge.Right -> copy(
            right = (right + deltaX).coerceIn(left + MIN_CROP_SIZE, 1f),
        )
        CropDragTarget.Edge.Bottom -> copy(
            bottom = (bottom + deltaY).coerceIn(top + MIN_CROP_SIZE, 1f),
        )
        CropDragTarget.Edge.Left -> copy(
            left = (left + deltaX).coerceIn(0f, right - MIN_CROP_SIZE),
        )
    }

    fun rotateAntiClockwise() = copy(
        left = top,
        top = 1f - right,
        right = bottom,
        bottom = 1f - left,
    )

    fun flipHorizontally() = copy(
        left = 1f - right,
        right = 1f - left,
    )

    fun flipVertically() = copy(
        top = 1f - bottom,
        bottom = 1f - top,
    )

    companion object {
        fun default() = NormalizedCropRect(
            left = DEFAULT_CROP_MARGIN,
            top = DEFAULT_CROP_MARGIN,
            right = 1f - DEFAULT_CROP_MARGIN,
            bottom = 1f - DEFAULT_CROP_MARGIN,
        )
    }
}

sealed interface CropDragTarget {
    data object Move : CropDragTarget

    sealed interface Corner : CropDragTarget {
        data object TopLeft : Corner
        data object TopRight : Corner
        data object BottomRight : Corner
        data object BottomLeft : Corner
    }

    sealed interface Edge : CropDragTarget {
        data object Top : Edge
        data object Right : Edge
        data object Bottom : Edge
        data object Left : Edge
    }
}
