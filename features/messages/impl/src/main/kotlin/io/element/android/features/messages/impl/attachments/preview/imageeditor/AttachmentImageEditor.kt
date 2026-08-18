/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.exifinterface.media.ExifInterface
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.features.messages.impl.attachments.preview.resolvedImageMimeType
import io.element.android.libraries.androidutils.bitmap.rotateToExifMetadataOrientation
import io.element.android.libraries.androidutils.bitmap.writeBitmap
import io.element.android.libraries.androidutils.file.createTmpFile
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.core.mimetype.MimeTypes
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeAnimatedImage
import io.element.android.libraries.core.mimetype.MimeTypes.isMimeTypeImage
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private const val EDITED_MEDIA_DIR_NAME = "edited-media"

interface AttachmentImageEditor {
    suspend fun canEdit(localMedia: LocalMedia): Boolean

    suspend fun exportEdits(
        localMedia: LocalMedia,
        edits: AttachmentImageEdits,
    ): Result<EditedLocalMedia>
}

data class EditedLocalMedia(
    val localMedia: LocalMedia,
    val file: File,
)

@ContributesBinding(AppScope::class)
class DefaultAttachmentImageEditor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
) : AttachmentImageEditor {
    override suspend fun canEdit(localMedia: LocalMedia): Boolean = withContext(dispatchers.io) {
        localMedia.info.resolvedImageMimeType()
            ?.takeIf { it.isEditableStillImageMimeType() }
            ?.let { return@withContext true }

        val decodedMimeType = context.contentResolver.openInputStream(localMedia.uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, options)
            options.outMimeType
        }

        decodedMimeType.isEditableStillImageMimeType()
    }

    override suspend fun exportEdits(
        localMedia: LocalMedia,
        edits: AttachmentImageEdits,
    ): Result<EditedLocalMedia> = withContext(dispatchers.io) {
        runCatchingExceptions {
            val sourceMimeType = localMedia.info.resolvedImageMimeType() ?: localMedia.info.mimeType
            val exportedMimeType = exportedMimeTypeFor(sourceMimeType)
            val exifOrientation = context.contentResolver.openInputStream(localMedia.uri)?.let { input ->
                input.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                }
            } ?: ExifInterface.ORIENTATION_UNDEFINED

            val decodedBitmap = context.contentResolver.openInputStream(localMedia.uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: error("Unable to decode image from ${localMedia.uri}")

            val normalizedBitmap = decodedBitmap.rotateToExifMetadataOrientation(exifOrientation)
            if (normalizedBitmap !== decodedBitmap) {
                decodedBitmap.recycle()
            }

            val transformedBitmap = normalizedBitmap.applyEdits(edits)
            if (transformedBitmap !== normalizedBitmap) {
                normalizedBitmap.recycle()
            }

            // Draw the markup before cropping, so that strokes outside of the crop area are trimmed
            // along with the image, exactly as they appear behind the crop overlay.
            val markedUpBitmap = transformedBitmap.drawMarkup(edits.strokes)
            if (markedUpBitmap !== transformedBitmap) {
                transformedBitmap.recycle()
            }

            val cropRect = edits.cropRect.toPixelRect(
                imageWidth = markedUpBitmap.width,
                imageHeight = markedUpBitmap.height,
            )
            val isCropUnchanged = cropRect.left == 0 && cropRect.top == 0 &&
                cropRect.width() == markedUpBitmap.width && cropRect.height() == markedUpBitmap.height
            val croppedBitmap = if (isCropUnchanged) {
                markedUpBitmap
            } else {
                Bitmap.createBitmap(
                    markedUpBitmap,
                    cropRect.left,
                    cropRect.top,
                    cropRect.width(),
                    cropRect.height(),
                )
            }
            if (croppedBitmap !== markedUpBitmap) {
                markedUpBitmap.recycle()
            }

            val editedMediaDir = File(context.cacheDir, EDITED_MEDIA_DIR_NAME).apply { mkdirs() }
            val outputFile = context.createTmpFile(baseDir = editedMediaDir, extension = compressFileExtension(exportedMimeType))
            outputFile.writeBitmap(
                bitmap = croppedBitmap,
                format = compressFormat(exportedMimeType),
                quality = 90,
            )
            croppedBitmap.recycle()

            EditedLocalMedia(
                localMedia = localMedia.copy(
                    uri = Uri.fromFile(outputFile),
                    info = localMedia.info.copy(mimeType = exportedMimeType),
                ),
                file = outputFile,
            )
        }
    }
}

internal fun exportedMimeTypeFor(sourceMimeType: String?): String {
    return if (sourceMimeType == MimeTypes.Png) {
        MimeTypes.Png
    } else {
        MimeTypes.Jpeg
    }
}

private fun Bitmap.applyEdits(edits: AttachmentImageEdits): Bitmap {
    val normalizedTurns = (edits.rotationQuarterTurns % 4 + 4) % 4
    if (normalizedTurns == 0 && !edits.isFlippedHorizontally && !edits.isFlippedVertically) {
        return this
    }
    val centerX = width / 2f
    val centerY = height / 2f
    val matrix = Matrix().apply {
        val scaleX = if (edits.isFlippedHorizontally) -1f else 1f
        val scaleY = if (edits.isFlippedVertically) -1f else 1f
        if (scaleX < 0f || scaleY < 0f) {
            postScale(scaleX, scaleY, centerX, centerY)
        }
        if (normalizedTurns != 0) {
            postRotate(normalizedTurns * 90f, centerX, centerY)
        }
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

internal fun Bitmap.drawMarkup(strokes: List<MarkupStroke>): Bitmap {
    if (strokes.isEmpty()) {
        return this
    }
    val target = copy(Bitmap.Config.ARGB_8888, true) ?: return this
    val canvas = Canvas(target)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = minOf(target.width, target.height) * MarkupStroke.RELATIVE_WIDTH
    }
    for (stroke in strokes) {
        val points = stroke.points
        if (points.isEmpty()) continue
        paint.color = stroke.color.value.toArgb()
        if (points.size == 1) {
            // A tap leaves a single point behind, which a path would not render.
            canvas.drawCircle(
                points[0].x * target.width,
                points[0].y * target.height,
                paint.strokeWidth / 2f,
                Paint(paint).apply { style = Paint.Style.FILL },
            )
            continue
        }
        val path = Path().apply {
            moveTo(points[0].x * target.width, points[0].y * target.height)
            for (index in 1 until points.size) {
                lineTo(points[index].x * target.width, points[index].y * target.height)
            }
        }
        canvas.drawPath(path, paint)
    }
    return target
}

private data class PixelCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun width() = right - left
    fun height() = bottom - top
}

private fun NormalizedCropRect.toPixelRect(imageWidth: Int, imageHeight: Int): PixelCropRect {
    val leftPx = (left * imageWidth).roundToInt().coerceIn(0, imageWidth - 1)
    val topPx = (top * imageHeight).roundToInt().coerceIn(0, imageHeight - 1)
    val rightPx = (right * imageWidth).roundToInt().coerceIn(leftPx + 1, imageWidth)
    val bottomPx = (bottom * imageHeight).roundToInt().coerceIn(topPx + 1, imageHeight)
    return PixelCropRect(
        left = leftPx,
        top = topPx,
        right = rightPx,
        bottom = bottomPx,
    )
}

private fun compressFormat(mimeType: String) = when (mimeType) {
    MimeTypes.Png -> Bitmap.CompressFormat.PNG
    else -> Bitmap.CompressFormat.JPEG
}

private fun compressFileExtension(mimeType: String) = when (mimeType) {
    MimeTypes.Png -> "png"
    else -> "jpeg"
}

private fun String?.isEditableStillImageMimeType(): Boolean {
    return this != null &&
        this.isMimeTypeImage() &&
        !this.isMimeTypeAnimatedImage() &&
        this != MimeTypes.Svg
}
