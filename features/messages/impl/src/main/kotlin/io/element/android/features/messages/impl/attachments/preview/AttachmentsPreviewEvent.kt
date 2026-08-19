/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview

import io.element.android.features.messages.impl.attachments.preview.imageeditor.DrawTool
import io.element.android.features.messages.impl.attachments.preview.imageeditor.ImageEditorEntryPoint
import io.element.android.features.messages.impl.attachments.preview.imageeditor.MarkupColor
import io.element.android.features.messages.impl.attachments.preview.imageeditor.MarkupShape
import io.element.android.features.messages.impl.attachments.preview.imageeditor.MarkupShapeKind
import io.element.android.features.messages.impl.attachments.preview.imageeditor.MarkupSticker
import io.element.android.features.messages.impl.attachments.preview.imageeditor.MarkupStroke
import io.element.android.features.messages.impl.attachments.preview.imageeditor.NormalizedCropRect
import io.element.android.features.messages.impl.attachments.preview.imageeditor.NormalizedPoint
import io.element.android.features.messages.impl.attachments.preview.imageeditor.StickerPicker

sealed interface AttachmentsPreviewEvent {
    data object SendAttachment : AttachmentsPreviewEvent
    data object CancelAndDismiss : AttachmentsPreviewEvent
    data object CancelAndClearSendState : AttachmentsPreviewEvent
    data class OpenImageEditor(val entryPoint: ImageEditorEntryPoint) : AttachmentsPreviewEvent
    data object CloseImageEditor : AttachmentsPreviewEvent
    data object RotateImageToTheLeft : AttachmentsPreviewEvent
    data object FlipImageHorizontally : AttachmentsPreviewEvent
    data object FlipImageVertically : AttachmentsPreviewEvent
    data object ApplyImageEdits : AttachmentsPreviewEvent
    data object ResetImageEdits : AttachmentsPreviewEvent
    data class UpdateImageCropRect(val cropRect: NormalizedCropRect) : AttachmentsPreviewEvent
    data class SelectDrawTool(val tool: DrawTool) : AttachmentsPreviewEvent
    data class SelectMarkupColor(val color: MarkupColor) : AttachmentsPreviewEvent
    data class AddMarkupStroke(val stroke: MarkupStroke) : AttachmentsPreviewEvent
    data class SelectMarkupShapeKind(val kind: MarkupShapeKind) : AttachmentsPreviewEvent
    data class AddMarkupShape(val shape: MarkupShape) : AttachmentsPreviewEvent
    data class EraseMarkup(val point: NormalizedPoint, val aspectRatio: Float) : AttachmentsPreviewEvent
    data object UndoMarkup : AttachmentsPreviewEvent
    data class ShowStickerPicker(val picker: StickerPicker) : AttachmentsPreviewEvent
    data class AddEmojiSticker(val unicode: String) : AttachmentsPreviewEvent
    data class AddTextSticker(val text: String) : AttachmentsPreviewEvent
    data class UpdateSticker(val sticker: MarkupSticker) : AttachmentsPreviewEvent
    data class SelectSticker(val id: Long?) : AttachmentsPreviewEvent
    data class RemoveSticker(val id: Long) : AttachmentsPreviewEvent
    data object ClearImageEditError : AttachmentsPreviewEvent
    data class SetCurrentCarouselIndex(val index: Int) : AttachmentsPreviewEvent
}
