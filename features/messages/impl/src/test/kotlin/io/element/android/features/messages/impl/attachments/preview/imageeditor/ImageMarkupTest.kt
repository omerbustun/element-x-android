/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
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
