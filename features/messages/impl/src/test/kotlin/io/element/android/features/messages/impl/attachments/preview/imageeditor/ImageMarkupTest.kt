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

    companion object {
        private const val TOLERANCE = 0.0001f
    }
}
