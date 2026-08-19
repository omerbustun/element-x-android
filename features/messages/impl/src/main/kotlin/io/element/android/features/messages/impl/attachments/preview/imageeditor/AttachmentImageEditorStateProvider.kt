/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.net.toUri
import io.element.android.libraries.mediaviewer.api.anImageMediaInfo
import io.element.android.libraries.mediaviewer.api.local.LocalMedia
import kotlinx.collections.immutable.persistentListOf

open class AttachmentImageEditorStateProvider : PreviewParameterProvider<AttachmentImageEditorState> {
    private val caterpillarCrop = NormalizedCropRect(
        left = 0.3f,
        top = 0.3f,
        right = 0.8f,
        bottom = 0.75f,
    )

    override val values: Sequence<AttachmentImageEditorState>
        get() = sequenceOf(
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    // Cheat a bit so that the crop match the sample image size (1024 * 682)
                    cropRect = 0.17f.let { correction ->
                        NormalizedCropRect(
                            left = 0f,
                            top = correction,
                            right = 1f,
                            bottom = 1 - correction,
                        )
                    },
                ),
            ),
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = caterpillarCrop,
                ),
            ),
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = caterpillarCrop,
                ),
                previewDebug = true,
            ),
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = caterpillarCrop,
                ).rotateAntiClockwise(),
            ),
            // Small crop
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = NormalizedCropRect(
                        left = 0.3f,
                        top = 0.6f,
                        right = 0.4f,
                        bottom = 0.7f,
                    ),
                ),
                previewDebug = true,
            ),
            // Big crop
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = NormalizedCropRect(
                        left = 0.05f,
                        top = 0.05f,
                        right = 0.95f,
                        bottom = 0.95f,
                    ),
                ),
                previewDebug = true,
            ),
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = caterpillarCrop,
                ).flipHorizontally(),
            ),
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    cropRect = caterpillarCrop,
                ).flipVertically(),
            ),
            // Pen tool, with a stroke already drawn
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    strokes = persistentListOf(aMarkupStroke()),
                ),
                activeTool = ImageEditorTool.Draw,
                markupColor = MarkupColor.Red,
            ),
            // Highlighter, over a stroke already drawn with the pen
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    strokes = persistentListOf(
                        aMarkupStroke(),
                        aMarkupStroke(color = MarkupColor.Yellow, kind = MarkupStrokeKind.Highlighter),
                    ),
                ),
                activeTool = ImageEditorTool.Draw,
                drawTool = DrawTool.Highlighter,
                markupColor = MarkupColor.Yellow,
            ),
            // Shape tool, with one shape of each kind on the image
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    shapes = persistentListOf(
                        aMarkupShape(MarkupShapeKind.Arrow),
                        aMarkupShape(MarkupShapeKind.Line, color = MarkupColor.Blue),
                        aMarkupShape(MarkupShapeKind.Rectangle, color = MarkupColor.Green),
                        aMarkupShape(MarkupShapeKind.Ellipse, color = MarkupColor.Purple),
                    ),
                ),
                activeTool = ImageEditorTool.Draw,
                drawTool = DrawTool.Shape,
                markupColor = MarkupColor.Red,
            ),
            // Eraser, with markup to rub out
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    strokes = persistentListOf(aMarkupStroke()),
                    shapes = persistentListOf(aMarkupShape(MarkupShapeKind.Ellipse)),
                ),
                activeTool = ImageEditorTool.Draw,
                drawTool = DrawTool.Eraser,
            ),
            // Sticker tool, with an emoji and a piece of text, the emoji selected
            anAttachmentImageEditorState(
                edits = AttachmentImageEdits(
                    stickers = persistentListOf(anEmojiSticker(), aTextSticker()),
                ),
                activeTool = ImageEditorTool.Sticker,
                selectedStickerId = 1L,
            ),
        )
}

internal fun anAttachmentImageEditorState(
    localMedia: LocalMedia = LocalMedia(
        uri = "file://preview-image".toUri(),
        info = anImageMediaInfo(),
    ),
    edits: AttachmentImageEdits = AttachmentImageEdits(),
    activeTool: ImageEditorTool = ImageEditorTool.Crop,
    drawTool: DrawTool = DrawTool.Pen,
    markupColor: MarkupColor = MarkupColor.White,
    shapeKind: MarkupShapeKind = MarkupShapeKind.Arrow,
    selectedStickerId: Long? = null,
    stickerPicker: StickerPicker = StickerPicker.None,
    previewDebug: Boolean = false,
) = AttachmentImageEditorState(
    localMedia = localMedia,
    edits = edits,
    activeTool = activeTool,
    drawTool = drawTool,
    markupColor = markupColor,
    shapeKind = shapeKind,
    selectedStickerId = selectedStickerId,
    stickerPicker = stickerPicker,
    previewDebug = previewDebug,
)

internal fun anEmojiSticker() = MarkupSticker(
    id = 1L,
    content = MarkupStickerContent.Emoji("🚀"),
    center = NormalizedPoint(x = 0.35f, y = 0.4f),
    relativeFontSize = MarkupSticker.EMOJI_RELATIVE_FONT_SIZE,
)

internal fun aTextSticker() = MarkupSticker(
    id = 2L,
    content = MarkupStickerContent.Text("Look here", MarkupColor.Yellow),
    center = NormalizedPoint(x = 0.6f, y = 0.65f),
    relativeFontSize = MarkupSticker.TEXT_RELATIVE_FONT_SIZE,
    rotationDegrees = -12f,
)

internal fun aMarkupStroke(
    color: MarkupColor = MarkupColor.Red,
    kind: MarkupStrokeKind = MarkupStrokeKind.Pen,
) = MarkupStroke(
    points = persistentListOf(
        NormalizedPoint(x = 0.2f, y = 0.3f),
        NormalizedPoint(x = 0.4f, y = 0.5f),
        NormalizedPoint(x = 0.7f, y = 0.35f),
    ),
    color = color,
    kind = kind,
)

internal fun aMarkupShape(
    kind: MarkupShapeKind,
    color: MarkupColor = MarkupColor.Red,
) = MarkupShape(
    kind = kind,
    start = NormalizedPoint(x = 0.25f, y = 0.25f),
    end = NormalizedPoint(x = 0.7f, y = 0.6f),
    color = color,
)
