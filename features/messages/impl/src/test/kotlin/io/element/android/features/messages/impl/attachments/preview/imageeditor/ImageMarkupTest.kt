/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class ImageMarkupTest {
    private val point = NormalizedPoint(x = 0.2f, y = 0.7f)

    @Test
    fun `rotating a point four times returns it to its original place`() {
        var result = point
        repeat(4) {
            result = result.rotateAntiClockwise()
        }
        assertThat(result.x).isWithin(TOLERANCE).of(point.x)
        assertThat(result.y).isWithin(TOLERANCE).of(point.y)
    }

    @Test
    fun `rotating a point moves it a quarter turn anti clockwise`() {
        val result = point.rotateAntiClockwise()
        assertThat(result.x).isWithin(TOLERANCE).of(0.7f)
        assertThat(result.y).isWithin(TOLERANCE).of(0.8f)
    }

    @Test
    fun `flipping a point mirrors the matching axis only`() {
        val flippedHorizontally = point.flipHorizontally()
        assertThat(flippedHorizontally.x).isWithin(TOLERANCE).of(0.8f)
        assertThat(flippedHorizontally.y).isWithin(TOLERANCE).of(point.y)

        val flippedVertically = point.flipVertically()
        assertThat(flippedVertically.x).isWithin(TOLERANCE).of(point.x)
        assertThat(flippedVertically.y).isWithin(TOLERANCE).of(0.3f)
    }

    @Test
    fun `rotating a stroke rotates every point and keeps the colour`() {
        val stroke = MarkupStroke(
            points = persistentListOf(
                NormalizedPoint(x = 0f, y = 0f),
                NormalizedPoint(x = 1f, y = 1f),
            ),
            color = MarkupColor.Blue,
        )
        val result = stroke.rotateAntiClockwise()
        assertThat(result.color).isEqualTo(MarkupColor.Blue)
        assertThat(result.points).hasSize(2)
        assertThat(result.points[0]).isEqualTo(NormalizedPoint(x = 0f, y = 1f))
        assertThat(result.points[1]).isEqualTo(NormalizedPoint(x = 1f, y = 0f))
    }

    @Test
    fun `adding a stroke marks the edits as changed`() {
        val sut = AttachmentImageEdits()
        assertThat(sut.hasChanges).isFalse()

        val result = sut.addStroke(aMarkupStroke())
        assertThat(result.strokes).hasSize(1)
        assertThat(result.hasChanges).isTrue()
    }

    @Test
    fun `removing the last stroke undoes a single stroke at a time`() {
        val sut = AttachmentImageEdits()
            .addStroke(aMarkupStroke(color = MarkupColor.Red))
            .addStroke(aMarkupStroke(color = MarkupColor.Green))

        val result = sut.removeLastStroke()
        assertThat(result.strokes).hasSize(1)
        assertThat(result.strokes[0].color).isEqualTo(MarkupColor.Red)

        val emptyResult = result.removeLastStroke()
        assertThat(emptyResult.strokes).isEmpty()
        assertThat(emptyResult.hasChanges).isFalse()
    }

    @Test
    fun `removing the last stroke of empty edits is a no-op`() {
        val result = AttachmentImageEdits().removeLastStroke()
        assertThat(result.strokes).isEmpty()
    }

    @Test
    fun `transforming the edits also transforms the strokes`() {
        val stroke = MarkupStroke(
            points = persistentListOf(NormalizedPoint(x = 0.25f, y = 0.5f)),
            color = MarkupColor.White,
        )
        val sut = AttachmentImageEdits(strokes = persistentListOf(stroke))

        val rotated = sut.rotateAntiClockwise().strokes[0].points[0]
        assertThat(rotated.x).isWithin(TOLERANCE).of(0.5f)
        assertThat(rotated.y).isWithin(TOLERANCE).of(0.75f)

        val flipped = sut.flipHorizontally().strokes[0].points[0]
        assertThat(flipped.x).isWithin(TOLERANCE).of(0.75f)
        assertThat(flipped.y).isWithin(TOLERANCE).of(0.5f)
    }

    @Test
    fun `rotating a sticker moves its centre and turns it with the image`() {
        val sticker = aSticker(center = NormalizedPoint(x = 0.25f, y = 0.5f), rotationDegrees = 10f)

        val result = sticker.rotateAntiClockwise()

        assertThat(result.center.x).isWithin(TOLERANCE).of(0.5f)
        assertThat(result.center.y).isWithin(TOLERANCE).of(0.75f)
        assertThat(result.rotationDegrees).isWithin(TOLERANCE).of(-80f)
    }

    @Test
    fun `flipping a sticker mirrors its centre and its angle`() {
        val sticker = aSticker(center = NormalizedPoint(x = 0.2f, y = 0.4f), rotationDegrees = 30f)

        val flippedHorizontally = sticker.flipHorizontally()
        assertThat(flippedHorizontally.center.x).isWithin(TOLERANCE).of(0.8f)
        assertThat(flippedHorizontally.center.y).isWithin(TOLERANCE).of(0.4f)
        assertThat(flippedHorizontally.rotationDegrees).isWithin(TOLERANCE).of(-30f)

        val flippedVertically = sticker.flipVertically()
        assertThat(flippedVertically.center.x).isWithin(TOLERANCE).of(0.2f)
        assertThat(flippedVertically.center.y).isWithin(TOLERANCE).of(0.6f)
    }

    @Test
    fun `a sticker exposes its text and colour by content`() {
        val emoji = aSticker(content = MarkupStickerContent.Emoji("🚀"))
        assertThat(emoji.text).isEqualTo("🚀")
        assertThat(emoji.color).isNull()

        val text = aSticker(content = MarkupStickerContent.Text("Hello", MarkupColor.Blue))
        assertThat(text.text).isEqualTo("Hello")
        assertThat(text.color).isEqualTo(MarkupColor.Blue)
    }

    @Test
    fun `adding a sticker marks the edits as changed`() {
        val sut = AttachmentImageEdits()
        assertThat(sut.hasChanges).isFalse()

        val result = sut.addSticker(aSticker())
        assertThat(result.stickers).hasSize(1)
        assertThat(result.hasChanges).isTrue()
    }

    @Test
    fun `updating a sticker only replaces the matching one`() {
        val first = aSticker(id = 1L)
        val second = aSticker(id = 2L)
        val sut = AttachmentImageEdits().addSticker(first).addSticker(second)

        val result = sut.updateSticker(second.copy(scale = 3f))

        assertThat(result.stickers).hasSize(2)
        assertThat(result.stickers[0].scale).isWithin(TOLERANCE).of(first.scale)
        assertThat(result.stickers[1].scale).isWithin(TOLERANCE).of(3f)
    }

    @Test
    fun `removing a sticker leaves the others alone`() {
        val sut = AttachmentImageEdits()
            .addSticker(aSticker(id = 1L))
            .addSticker(aSticker(id = 2L))

        val result = sut.removeSticker(1L)

        assertThat(result.stickers.map { it.id }).containsExactly(2L)
        assertThat(result.removeSticker(2L).hasChanges).isFalse()
    }

    @Test
    fun `transforming the edits also transforms the stickers`() {
        val sut = AttachmentImageEdits()
            .addSticker(aSticker(center = NormalizedPoint(x = 0.25f, y = 0.5f)))

        val rotated = sut.rotateAntiClockwise().stickers[0]
        assertThat(rotated.center.x).isWithin(TOLERANCE).of(0.5f)
        assertThat(rotated.center.y).isWithin(TOLERANCE).of(0.75f)

        val flipped = sut.flipVertically().stickers[0]
        assertThat(flipped.center.y).isWithin(TOLERANCE).of(0.5f)
    }

    @Test
    fun `a highlighter draws wider and more faintly than the pen`() {
        assertThat(MarkupStrokeKind.Highlighter.relativeWidth).isGreaterThan(MarkupStrokeKind.Pen.relativeWidth)
        assertThat(MarkupStrokeKind.Highlighter.alpha).isLessThan(MarkupStrokeKind.Pen.alpha)
        assertThat(MarkupStrokeKind.Pen.alpha).isEqualTo(1f)
    }

    @Test
    fun `a stroke keeps its kind through a rotation and a flip`() {
        val stroke = aStroke(kind = MarkupStrokeKind.Highlighter)
        assertThat(stroke.rotateAntiClockwise().kind).isEqualTo(MarkupStrokeKind.Highlighter)
        assertThat(stroke.flipHorizontally().kind).isEqualTo(MarkupStrokeKind.Highlighter)
        assertThat(stroke.flipVertically().kind).isEqualTo(MarkupStrokeKind.Highlighter)
    }

    @Test
    fun `rotating a shape rotates both of its ends and keeps the colour`() {
        val shape = MarkupShape(
            kind = MarkupShapeKind.Arrow,
            start = NormalizedPoint(x = 0f, y = 0f),
            end = NormalizedPoint(x = 1f, y = 1f),
            color = MarkupColor.Blue,
        )
        val result = shape.rotateAntiClockwise()
        assertThat(result.kind).isEqualTo(MarkupShapeKind.Arrow)
        assertThat(result.color).isEqualTo(MarkupColor.Blue)
        assertThat(result.start).isEqualTo(NormalizedPoint(x = 0f, y = 1f))
        assertThat(result.end).isEqualTo(NormalizedPoint(x = 1f, y = 0f))
    }

    @Test
    fun `a line is drawn as the single run the drag traced`() {
        val shape = aShape(MarkupShapeKind.Line)
        val polylines = shape.polylines(width = 100f, height = 100f)
        assertThat(polylines).hasSize(1)
        assertThat(polylines[0]).hasSize(2)
        assertThat(polylines[0][0].x).isWithin(TOLERANCE).of(20f)
        assertThat(polylines[0][1].x).isWithin(TOLERANCE).of(80f)
        assertThat(shape.ovalBounds(width = 100f, height = 100f)).isNull()
    }

    @Test
    fun `an arrow adds a head at the end the drag finished on`() {
        val polylines = aShape(MarkupShapeKind.Arrow).polylines(width = 100f, height = 100f)
        assertThat(polylines).hasSize(2)
        // Both barbs meet the shaft at the point the drag ended.
        assertThat(polylines[1]).hasSize(3)
        assertThat(polylines[1][1].x).isWithin(TOLERANCE).of(polylines[0][1].x)
        assertThat(polylines[1][1].y).isWithin(TOLERANCE).of(polylines[0][1].y)
    }

    @Test
    fun `a rectangle is drawn as a closed run around its bounds`() {
        val polylines = aShape(MarkupShapeKind.Rectangle).polylines(width = 100f, height = 100f)
        assertThat(polylines).hasSize(1)
        assertThat(polylines[0]).hasSize(5)
        assertThat(polylines[0].first()).isEqualTo(polylines[0].last())
    }

    @Test
    fun `an ellipse is drawn from its bounds rather than from straight runs`() {
        val shape = aShape(MarkupShapeKind.Ellipse)
        assertThat(shape.polylines(width = 100f, height = 100f)).isEmpty()
        val bounds = shape.ovalBounds(width = 100f, height = 100f)
        assertThat(bounds).isNotNull()
        assertThat(bounds!!.left).isWithin(TOLERANCE).of(20f)
        assertThat(bounds.right).isWithin(TOLERANCE).of(80f)
    }

    @Test
    fun `the bounds of a shape do not depend on the direction it was dragged in`() {
        val forwards = aShape(MarkupShapeKind.Ellipse)
        val backwards = forwards.copy(start = forwards.end, end = forwards.start)
        assertThat(backwards.ovalBounds(width = 100f, height = 100f))
            .isEqualTo(forwards.ovalBounds(width = 100f, height = 100f))
    }

    @Test
    fun `the eraser rubs out only the markup it is dragged over`() {
        val target = aStroke(points = listOf(NormalizedPoint(x = 0.1f, y = 0.1f)))
        val other = aStroke(points = listOf(NormalizedPoint(x = 0.9f, y = 0.9f)))
        val edits = AttachmentImageEdits(strokes = persistentListOf(target, other))

        val result = edits.eraseAt(
            point = NormalizedPoint(x = 0.11f, y = 0.11f),
            radius = ERASER_RELATIVE_RADIUS,
            aspectRatio = 1f,
        )

        assertThat(result.strokes).containsExactly(other)
    }

    @Test
    fun `the eraser rubs out whole strokes rather than the part under it`() {
        val stroke = aStroke(
            points = listOf(
                NormalizedPoint(x = 0.1f, y = 0.5f),
                NormalizedPoint(x = 0.9f, y = 0.5f),
            ),
        )
        val edits = AttachmentImageEdits(strokes = persistentListOf(stroke))

        val result = edits.eraseAt(
            point = NormalizedPoint(x = 0.5f, y = 0.5f),
            radius = ERASER_RELATIVE_RADIUS,
            aspectRatio = 1f,
        )

        assertThat(result.strokes).isEmpty()
    }

    @Test
    fun `the eraser reaches the outline of a shape but not the space inside it`() {
        val shape = MarkupShape(
            kind = MarkupShapeKind.Rectangle,
            start = NormalizedPoint(x = 0.2f, y = 0.2f),
            end = NormalizedPoint(x = 0.8f, y = 0.8f),
            color = MarkupColor.Red,
        )
        val edits = AttachmentImageEdits(shapes = persistentListOf(shape))

        val insideTheShape = edits.eraseAt(
            point = NormalizedPoint(x = 0.5f, y = 0.5f),
            radius = ERASER_RELATIVE_RADIUS,
            aspectRatio = 1f,
        )
        assertThat(insideTheShape.shapes).containsExactly(shape)

        val onTheOutline = edits.eraseAt(
            point = NormalizedPoint(x = 0.5f, y = 0.2f),
            radius = ERASER_RELATIVE_RADIUS,
            aspectRatio = 1f,
        )
        assertThat(onTheOutline.shapes).isEmpty()
    }

    @Test
    fun `adding a shape marks the edits as changed`() {
        val edits = AttachmentImageEdits()
        assertThat(edits.hasChanges).isFalse()
        val result = edits.addShape(aShape(MarkupShapeKind.Arrow))
        assertThat(result.hasChanges).isTrue()
        assertThat(result.shapes).hasSize(1)
    }

    @Test
    fun `removing the last shape undoes a single shape at a time`() {
        val first = aShape(MarkupShapeKind.Arrow)
        val second = aShape(MarkupShapeKind.Line)
        val edits = AttachmentImageEdits().addShape(first).addShape(second)

        assertThat(edits.removeLastShape().shapes).containsExactly(first)
        assertThat(edits.removeLastShape().removeLastShape().shapes).isEmpty()
        assertThat(AttachmentImageEdits().removeLastShape().shapes).isEmpty()
    }

    @Test
    fun `transforming the edits also transforms the shapes`() {
        val shape = aShape(MarkupShapeKind.Arrow)
        val edits = AttachmentImageEdits(shapes = persistentListOf(shape))

        assertThat(edits.rotateAntiClockwise().shapes.single()).isEqualTo(shape.rotateAntiClockwise())
        assertThat(edits.flipHorizontally().shapes.single()).isEqualTo(shape.flipHorizontally())
        assertThat(edits.flipVertically().shapes.single()).isEqualTo(shape.flipVertically())
    }

    private fun aStroke(
        points: List<NormalizedPoint> = listOf(
            NormalizedPoint(x = 0.2f, y = 0.3f),
            NormalizedPoint(x = 0.4f, y = 0.5f),
        ),
        color: MarkupColor = MarkupColor.Red,
        kind: MarkupStrokeKind = MarkupStrokeKind.Pen,
    ) = MarkupStroke(points = points.toImmutableList(), color = color, kind = kind)

    private fun aShape(kind: MarkupShapeKind) = MarkupShape(
        kind = kind,
        start = NormalizedPoint(x = 0.2f, y = 0.2f),
        end = NormalizedPoint(x = 0.8f, y = 0.6f),
        color = MarkupColor.Red,
    )

    private fun aSticker(
        id: Long = 1L,
        content: MarkupStickerContent = MarkupStickerContent.Emoji("🚀"),
        center: NormalizedPoint = NormalizedPoint(x = 0.5f, y = 0.5f),
        rotationDegrees: Float = 0f,
    ) = MarkupSticker(
        id = id,
        content = content,
        center = center,
        relativeFontSize = MarkupSticker.EMOJI_RELATIVE_FONT_SIZE,
        rotationDegrees = rotationDegrees,
    )

    companion object {
        private const val TOLERANCE = 0.0001f
    }
}
