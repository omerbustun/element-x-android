/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.attachments.preview.imageeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.R
import io.element.android.libraries.designsystem.components.button.BackButton
import io.element.android.libraries.designsystem.components.dialogs.TextFieldDialog
import io.element.android.libraries.designsystem.preview.ElementPreviewDark
import io.element.android.libraries.designsystem.text.toPx
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Scaffold
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.components.TextButton
import io.element.android.libraries.designsystem.theme.components.TopAppBar
import io.element.android.libraries.designsystem.theme.components.hide
import io.element.android.libraries.designsystem.utils.CommonDrawables
import io.element.android.libraries.emoji.api.picker.EmojiPickerRenderer
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import io.element.android.libraries.emoji.api.picker.NoOpEmojiPickerRenderer
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.min
import kotlin.math.roundToInt

private val minHandleTouchRadius = 16.dp
private val maxHandleTouchRadius = 56.dp

/**
 * Ref: https://www.figma.com/design/zftpgS6LjiczobJZ1GUNpt/Updates-to-Media---File-Upload?node-id=51-3539
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentImageEditorView(
    state: AttachmentImageEditorState,
    onCropRectChange: (NormalizedCropRect) -> Unit,
    onDrawToolSelect: (DrawTool) -> Unit,
    onMarkupColorSelect: (MarkupColor) -> Unit,
    onStrokeAdd: (MarkupStroke) -> Unit,
    onShapeKindSelect: (MarkupShapeKind) -> Unit,
    onShapeAdd: (MarkupShape) -> Unit,
    onErase: (NormalizedPoint, Float) -> Unit,
    onUndoClick: () -> Unit,
    onStickerPickerRequest: (StickerPicker) -> Unit,
    onEmojiStickerAdd: (String) -> Unit,
    onTextStickerAdd: (String) -> Unit,
    onStickerChange: (MarkupSticker) -> Unit,
    onStickerSelect: (Long?) -> Unit,
    onStickerRemove: (Long) -> Unit,
    emojiPickerState: EmojiPickerState,
    emojiPickerRenderer: EmojiPickerRenderer,
    onRotateClick: () -> Unit,
    onFlipHorizontallyClick: () -> Unit,
    onFlipVerticallyClick: () -> Unit,
    onResetClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotateContentDescription = stringResource(R.string.screen_image_edition_a11y_rotate_to_the_left)
    val rotationStateDescription = pluralStringResource(
        R.plurals.screen_image_edition_a11y_rotation_state,
        state.edits.rotationDegrees,
        state.edits.rotationDegrees,
    )
    val flipHorizontalLabel = stringResource(R.string.screen_image_edition_a11y_flip_image_horizontally)
    val flipHorizontalState = if (state.edits.isFlippedHorizontally) {
        stringResource(R.string.screen_image_edition_a11y_flip_image_horizontally_state_flipped)
    } else {
        stringResource(R.string.screen_image_edition_a11y_flip_image_horizontally_state_original)
    }
    val flipVerticalLabel = stringResource(R.string.screen_image_edition_a11y_flip_image_vertically)
    val flipVerticalState = if (state.edits.isFlippedVertically) {
        stringResource(R.string.screen_image_edition_a11y_flip_image_vertically_state_flipped)
    } else {
        stringResource(R.string.screen_image_edition_a11y_flip_image_vertically_state_original)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(
                        imageVector = CompoundIcons.Close(),
                        onClick = onCancelClick,
                    )
                },
                title = {
                    Text(
                        modifier = Modifier.semantics {
                            heading()
                        },
                        text = stringResource(R.string.screen_image_edition_title),
                    )
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ElementTheme.colors.bgCanvasDefault)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ImageEditorCanvas(
                    state = state,
                    onCropRectChange = onCropRectChange,
                    onStrokeAdd = onStrokeAdd,
                    onShapeAdd = onShapeAdd,
                    onErase = onErase,
                    onStickerChange = onStickerChange,
                    onStickerSelect = onStickerSelect,
                    onStickerRemove = onStickerRemove,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 360.dp)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
            ) {
                if (state.activeTool == ImageEditorTool.Draw && state.drawTool == DrawTool.Shape) {
                    ShapeKindPicker(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        selectedKind = state.shapeKind,
                        onShapeKindSelect = onShapeKindSelect,
                    )
                }
                if (state.showsMarkupColorPicker) {
                    MarkupColorPicker(
                        selectedColor = state.markupColor,
                        onMarkupColorSelect = onMarkupColorSelect,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        TextButton(
                            text = stringResource(CommonStrings.action_reset),
                            destructive = true,
                            onClick = onResetClick,
                        )
                    }
                    Row(
                        modifier = Modifier.weight(2f),
                        // Center the content horizontally
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        when (state.activeTool) {
                            ImageEditorTool.Crop -> {
                                IconButton(
                                    onClick = onFlipHorizontallyClick,
                                    modifier = Modifier
                                        .clearAndSetSemantics {
                                            contentDescription = flipHorizontalLabel
                                            stateDescription = flipHorizontalState
                                        }
                                ) {
                                    Icon(
                                        imageVector = CompoundIcons.FlipHorizontal(),
                                        contentDescription = null,
                                    )
                                }
                                IconButton(
                                    onClick = onRotateClick,
                                    modifier = Modifier
                                        .clearAndSetSemantics {
                                            contentDescription = rotateContentDescription
                                            stateDescription = rotationStateDescription
                                        }
                                ) {
                                    Icon(
                                        imageVector = CompoundIcons.RotateLeft(),
                                        contentDescription = null,
                                    )
                                }
                                IconButton(
                                    onClick = onFlipVerticallyClick,
                                    modifier = Modifier
                                        .clearAndSetSemantics {
                                            contentDescription = flipVerticalLabel
                                            stateDescription = flipVerticalState
                                        }
                                ) {
                                    Icon(
                                        imageVector = CompoundIcons.FlipVertical(),
                                        contentDescription = null,
                                    )
                                }
                            }
                            ImageEditorTool.Draw -> {
                                // Erasing is itself how markup is taken back, so it has no undo.
                                if (state.drawTool != DrawTool.Eraser) {
                                    IconButton(
                                        onClick = onUndoClick,
                                        enabled = if (state.drawTool == DrawTool.Shape) {
                                            state.edits.shapes.isNotEmpty()
                                        } else {
                                            state.edits.strokes.isNotEmpty()
                                        },
                                    ) {
                                        Icon(
                                            imageVector = CompoundIcons.Restart(),
                                            contentDescription = stringResource(R.string.screen_image_edition_a11y_undo),
                                        )
                                    }
                                }
                            }
                            // Stickers are added from the attachment preview, and removed by
                            // selecting them, so there is nothing to put here.
                            ImageEditorTool.Sticker -> Unit
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        TextButton(
                            text = stringResource(CommonStrings.action_save),
                            onClick = onDoneClick,
                        )
                    }
                }
                if (state.activeTool == ImageEditorTool.Draw) {
                    DrawToolPicker(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        activeTool = state.drawTool,
                        onDrawToolSelect = onDrawToolSelect,
                    )
                }
            }
        }
    }

    when (state.stickerPicker) {
        StickerPicker.None -> Unit
        StickerPicker.Emoji -> EmojiStickerBottomSheet(
            emojiPickerState = emojiPickerState,
            emojiPickerRenderer = emojiPickerRenderer,
            onSelectEmoji = onEmojiStickerAdd,
            onDismiss = { onStickerPickerRequest(StickerPicker.None) },
        )
        StickerPicker.Text -> TextFieldDialog(
            title = stringResource(R.string.screen_image_edition_a11y_add_text),
            value = "",
            placeholder = null,
            onSubmit = onTextStickerAdd,
            onDismissRequest = { onStickerPickerRequest(StickerPicker.None) },
            maxLines = 3,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiStickerBottomSheet(
    emojiPickerState: EmojiPickerState,
    emojiPickerRenderer: EmojiPickerRenderer,
    onSelectEmoji: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrollable = false,
    ) {
        emojiPickerRenderer.Render(
            state = emojiPickerState,
            onSelectEmoji = { emoji ->
                sheetState.hide(coroutineScope) { onSelectEmoji(emoji.unicode) }
            },
            selectedEmojis = persistentSetOf(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DrawToolPicker(
    activeTool: DrawTool,
    onDrawToolSelect: (DrawTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(R.string.screen_image_edition_a11y_selected)
    Row(modifier = modifier) {
        for (tool in DrawTool.entries) {
            val isSelected = tool == activeTool
            val label = when (tool) {
                DrawTool.Pen -> stringResource(R.string.screen_image_edition_a11y_pen_tool)
                DrawTool.Highlighter -> stringResource(R.string.screen_image_edition_a11y_highlighter_tool)
                DrawTool.Shape -> stringResource(R.string.screen_image_edition_a11y_shape_tool)
                DrawTool.Eraser -> stringResource(R.string.screen_image_edition_a11y_eraser_tool)
            }
            IconButton(
                onClick = { onDrawToolSelect(tool) },
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = label
                    if (isSelected) {
                        stateDescription = selectedStateDescription
                    }
                },
            ) {
                Icon(
                    imageVector = when (tool) {
                        DrawTool.Pen -> CompoundIcons.Edit()
                        DrawTool.Highlighter -> CompoundIcons.EditSolid()
                        DrawTool.Shape -> CompoundIcons.ArrowUpRight()
                        DrawTool.Eraser -> CompoundIcons.Delete()
                    },
                    contentDescription = null,
                    tint = if (isSelected) ElementTheme.colors.iconAccentPrimary else ElementTheme.colors.iconSecondary,
                )
            }
        }
    }
}

@Composable
private fun ShapeKindPicker(
    selectedKind: MarkupShapeKind,
    onShapeKindSelect: (MarkupShapeKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(R.string.screen_image_edition_a11y_selected)
    Row(modifier = modifier.padding(bottom = 12.dp)) {
        for (kind in MarkupShapeKind.entries) {
            val isSelected = kind == selectedKind
            val label = stringResource(kind.a11yLabelResourceId())
            IconButton(
                onClick = { onShapeKindSelect(kind) },
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = label
                    if (isSelected) {
                        stateDescription = selectedStateDescription
                    }
                },
            ) {
                Icon(
                    imageVector = when (kind) {
                        MarkupShapeKind.Arrow -> CompoundIcons.ArrowUpRight()
                        MarkupShapeKind.Line -> CompoundIcons.Minus()
                        MarkupShapeKind.Rectangle -> CompoundIcons.Stop()
                        MarkupShapeKind.Ellipse -> CompoundIcons.Circle()
                    },
                    contentDescription = null,
                    tint = if (isSelected) ElementTheme.colors.iconAccentPrimary else ElementTheme.colors.iconSecondary,
                )
            }
        }
    }
}

private fun MarkupShapeKind.a11yLabelResourceId() = when (this) {
    MarkupShapeKind.Arrow -> R.string.screen_image_edition_a11y_shape_arrow
    MarkupShapeKind.Line -> R.string.screen_image_edition_a11y_shape_line
    MarkupShapeKind.Rectangle -> R.string.screen_image_edition_a11y_shape_rectangle
    MarkupShapeKind.Ellipse -> R.string.screen_image_edition_a11y_shape_ellipse
}

@Composable
private fun MarkupColorPicker(
    selectedColor: MarkupColor,
    onMarkupColorSelect: (MarkupColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStateDescription = stringResource(R.string.screen_image_edition_a11y_selected)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for (color in MarkupColor.entries) {
            val isSelected = color == selectedColor
            val label = stringResource(color.a11yLabelResourceId())
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onMarkupColorSelect(color) }
                    .clearAndSetSemantics {
                        contentDescription = label
                        if (isSelected) {
                            stateDescription = selectedStateDescription
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 28.dp else 22.dp)
                        .clip(CircleShape)
                        .background(color.value)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = ElementTheme.colors.borderInteractiveSecondary,
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}

private fun MarkupColor.a11yLabelResourceId() = when (this) {
    MarkupColor.White -> R.string.screen_image_edition_a11y_pen_colour_white
    MarkupColor.Black -> R.string.screen_image_edition_a11y_pen_colour_black
    MarkupColor.Red -> R.string.screen_image_edition_a11y_pen_colour_red
    MarkupColor.Orange -> R.string.screen_image_edition_a11y_pen_colour_orange
    MarkupColor.Yellow -> R.string.screen_image_edition_a11y_pen_colour_yellow
    MarkupColor.Green -> R.string.screen_image_edition_a11y_pen_colour_green
    MarkupColor.Blue -> R.string.screen_image_edition_a11y_pen_colour_blue
    MarkupColor.Purple -> R.string.screen_image_edition_a11y_pen_colour_purple
}

@Composable
private fun BoxScope.ImageEditorCanvas(
    state: AttachmentImageEditorState,
    onCropRectChange: (NormalizedCropRect) -> Unit,
    onStrokeAdd: (MarkupStroke) -> Unit,
    onShapeAdd: (MarkupShape) -> Unit,
    onErase: (NormalizedPoint, Float) -> Unit,
    onStickerChange: (MarkupSticker) -> Unit,
    onStickerSelect: (Long?) -> Unit,
    onStickerRemove: (Long) -> Unit,
) {
    var imageSize by remember(state.localMedia.uri) { mutableStateOf(IntSize.Zero) }
    val rotationQuarterTurns = state.edits.normalizedRotationQuarterTurns
    val flipScaleX = if (state.edits.isFlippedHorizontally) -1f else 1f
    val flipScaleY = if (state.edits.isFlippedVertically) -1f else 1f

    var imageRect by remember { mutableStateOf(Rect.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        val displayedSize = remember(maxWidth, maxHeight, imageSize, rotationQuarterTurns) {
            val sourceWidth = imageSize.width.takeIf { it > 0 } ?: 1
            val sourceHeight = imageSize.height.takeIf { it > 0 } ?: 1
            val aspectRatio = if (rotationQuarterTurns % 2 == 0) {
                sourceWidth.toFloat() / sourceHeight.toFloat()
            } else {
                sourceHeight.toFloat() / sourceWidth.toFloat()
            }
            fitSize(
                containerWidth = constraints.maxWidth.toFloat(),
                containerHeight = constraints.maxHeight.toFloat(),
                aspectRatio = aspectRatio,
            )
        }
        val density = LocalDensity.current
        val displayedWidthDp = with(density) { displayedSize.width.toDp() }
        val displayedHeightDp = with(density) { displayedSize.height.toDp() }
        val imageLayoutSize = remember(displayedSize, rotationQuarterTurns) {
            if (rotationQuarterTurns % 2 == 0) {
                displayedSize
            } else {
                Size(
                    width = displayedSize.height,
                    height = displayedSize.width,
                )
            }
        }
        val imageLayoutWidthDp = with(density) { imageLayoutSize.width.toDp() }
        val imageLayoutHeightDp = with(density) { imageLayoutSize.height.toDp() }

        Box(
            modifier = Modifier
                .size(displayedWidthDp, displayedHeightDp)
                .align(Alignment.Center)
                .onPlaced {
                    imageRect = it.boundsInParent()
                },
            contentAlignment = Alignment.Center,
        ) {
            if (LocalInspectionMode.current) {
                Image(
                    painter = painterResource(id = CommonDrawables.sample_background),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(imageLayoutWidthDp, imageLayoutHeightDp)
                        .graphicsLayer {
                            scaleX = flipScaleX
                            scaleY = flipScaleY
                        }
                        .graphicsLayer { rotationZ = rotationQuarterTurns * 90f },
                    contentScale = ContentScale.Fit,
                )
            } else {
                AsyncImage(
                    model = state.localMedia.uri,
                    contentDescription = stringResource(CommonStrings.common_image),
                    modifier = Modifier
                        .requiredSize(imageLayoutWidthDp, imageLayoutHeightDp)
                        .graphicsLayer {
                            scaleX = flipScaleX
                            scaleY = flipScaleY
                        }
                        .graphicsLayer { rotationZ = rotationQuarterTurns * 90f },
                    contentScale = ContentScale.Fit,
                    onState = { painterState ->
                        if (painterState is AsyncImagePainter.State.Success) {
                            imageSize = IntSize(
                                width = painterState.result.image.width,
                                height = painterState.result.image.height,
                            )
                        }
                    }
                )
            }
        }
        val minHandleTouchRadiusPx = minHandleTouchRadius.toPx()
        val maxHandleTouchRadiusPx = maxHandleTouchRadius.toPx()
        val touchRadiusPx by rememberUpdatedState(
            (min(
                state.edits.cropRect.width * imageRect.width,
                state.edits.cropRect.height * imageRect.height,
            ) / 4f).coerceIn(
                minHandleTouchRadiusPx,
                maxHandleTouchRadiusPx,
            )
        )
        var dragTarget by remember { mutableStateOf<CropDragTarget?>(null) }
        val latestCropRect by rememberUpdatedState(state.edits.cropRect)
        val latestPenColor by rememberUpdatedState(state.markupColor)
        val latestImageRect by rememberUpdatedState(imageRect)
        var strokeInProgress by remember { mutableStateOf(persistentListOf<NormalizedPoint>().toImmutableList()) }
        var shapeInProgress by remember { mutableStateOf<MarkupShape?>(null) }
        val latestShapeKind by rememberUpdatedState(state.shapeKind)
        val drawGuidelines = dragTarget == CropDragTarget.Move || state.previewDebug
        val gestureModifier = when (state.activeTool) {
            ImageEditorTool.Crop -> Modifier.pointerInput(state.activeTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragTarget = detectDragTarget(
                            touchPoint = offset,
                            imageOffset = latestImageRect.topLeft,
                            cropRect = latestCropRect,
                            canvasSize = Size(latestImageRect.width, latestImageRect.height),
                            handleTouchRadius = touchRadiusPx,
                        )
                    },
                    onDragCancel = {
                        dragTarget = null
                    },
                    onDragEnd = {
                        dragTarget = null
                    },
                ) { change, dragAmount ->
                    val activeTarget = dragTarget ?: return@detectDragGestures
                    change.consume()
                    val gestureAreaWidth = latestImageRect.width.takeIf { it > 0f } ?: size.width.toFloat()
                    val gestureAreaHeight = latestImageRect.height.takeIf { it > 0f } ?: size.height.toFloat()
                    onCropRectChange(
                        latestCropRect.applyChange(
                            dragTarget = activeTarget,
                            deltaX = dragAmount.x / gestureAreaWidth,
                            deltaY = dragAmount.y / gestureAreaHeight,
                        )
                    )
                }
            }
            ImageEditorTool.Sticker -> Modifier.pointerInput(state.activeTool) {
                // A tap on the image itself, rather than on a sticker, clears the selection.
                detectTapGestures { onStickerSelect(null) }
            }
            ImageEditorTool.Draw -> when (state.drawTool) {
                DrawTool.Pen, DrawTool.Highlighter -> Modifier.pointerInput(state.drawTool) {
                    val strokeKind = state.drawTool.strokeKind ?: return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            strokeInProgress = persistentListOf(offset.toNormalizedPoint(latestImageRect)).toImmutableList()
                        },
                        onDragCancel = {
                            strokeInProgress = persistentListOf<NormalizedPoint>().toImmutableList()
                        },
                        onDragEnd = {
                            val points = strokeInProgress
                            strokeInProgress = persistentListOf<NormalizedPoint>().toImmutableList()
                            if (points.isNotEmpty()) {
                                onStrokeAdd(MarkupStroke(points = points, color = latestPenColor, kind = strokeKind))
                            }
                        },
                    ) { change, _ ->
                        change.consume()
                        strokeInProgress = (strokeInProgress + change.position.toNormalizedPoint(latestImageRect)).toImmutableList()
                    }
                }
                DrawTool.Shape -> Modifier.pointerInput(state.drawTool) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val point = offset.toNormalizedPoint(latestImageRect)
                            shapeInProgress = MarkupShape(
                                kind = latestShapeKind,
                                start = point,
                                end = point,
                                color = latestPenColor,
                            )
                        },
                        onDragCancel = {
                            shapeInProgress = null
                        },
                        onDragEnd = {
                            val shape = shapeInProgress
                            shapeInProgress = null
                            // A tap leaves a shape with no size behind, which would draw nothing.
                            if (shape != null && shape.start != shape.end) {
                                onShapeAdd(shape)
                            }
                        },
                    ) { change, _ ->
                        change.consume()
                        shapeInProgress = shapeInProgress?.copy(end = change.position.toNormalizedPoint(latestImageRect))
                    }
                }
                DrawTool.Eraser -> Modifier.pointerInput(state.drawTool) {
                    // Read from the latest rect on each touch, so that erasing still reaches the
                    // markup after the image has been rotated underneath it.
                    fun erase(offset: Offset) = onErase(
                        offset.toNormalizedPoint(latestImageRect),
                        latestImageRect.aspectRatio(),
                    )
                    detectDragGestures(onDragStart = ::erase) { change, _ ->
                        change.consume()
                        erase(change.position)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
            contentAlignment = Alignment.Center,
        ) {
            MarkupOverlay(
                imageSize = DpSize(displayedWidthDp, displayedHeightDp),
                strokes = state.edits.strokes,
                shapes = state.edits.shapes,
                strokeInProgress = strokeInProgress,
                strokeKindInProgress = state.drawTool.strokeKind.takeIf { state.activeTool == ImageEditorTool.Draw },
                shapeInProgress = shapeInProgress,
                markupColor = state.markupColor,
            )
            StickerLayer(
                imageSize = DpSize(displayedWidthDp, displayedHeightDp),
                stickers = state.edits.stickers,
                selectedStickerId = state.selectedStickerId,
                isInteractive = state.activeTool == ImageEditorTool.Sticker,
                onStickerChange = onStickerChange,
                onStickerSelect = onStickerSelect,
                onStickerRemove = onStickerRemove,
            )
            CropOverlay(
                imageSize = DpSize(displayedWidthDp, displayedHeightDp),
                cropRect = state.edits.cropRect,
                drawGuidelines = drawGuidelines,
                previewDebug = state.previewDebug,
                touchRadiusPx = touchRadiusPx,
                dragTarget = dragTarget,
            )
        }
    }
}

/**
 * Lays the stickers out on top of the image. They can be dragged, pinched and rotated while the
 * sticker tool is selected.
 */
@Composable
private fun StickerLayer(
    imageSize: DpSize,
    stickers: ImmutableList<MarkupSticker>,
    selectedStickerId: Long?,
    isInteractive: Boolean,
    onStickerChange: (MarkupSticker) -> Unit,
    onStickerSelect: (Long?) -> Unit,
    onStickerRemove: (Long) -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { imageSize.width.toPx() }
    val heightPx = with(density) { imageSize.height.toPx() }
    Box(
        modifier = Modifier.size(imageSize.width, imageSize.height),
        contentAlignment = Alignment.Center,
    ) {
        for (sticker in stickers) {
            StickerItem(
                sticker = sticker,
                containerWidthPx = widthPx,
                containerHeightPx = heightPx,
                smallestSideDp = minOf(imageSize.width, imageSize.height),
                isSelected = selectedStickerId == sticker.id,
                isInteractive = isInteractive,
                onStickerChange = onStickerChange,
                onStickerSelect = onStickerSelect,
                onStickerRemove = onStickerRemove,
            )
        }
    }
}

@Composable
private fun BoxScope.StickerItem(
    sticker: MarkupSticker,
    containerWidthPx: Float,
    containerHeightPx: Float,
    smallestSideDp: Dp,
    isSelected: Boolean,
    isInteractive: Boolean,
    onStickerChange: (MarkupSticker) -> Unit,
    onStickerSelect: (Long?) -> Unit,
    onStickerRemove: (Long) -> Unit,
) {
    val latestSticker by rememberUpdatedState(sticker)
    val isSelectedNow by rememberUpdatedState(isSelected)
    val fontSize = with(LocalDensity.current) {
        (smallestSideDp * sticker.relativeFontSize * sticker.scale).toSp()
    }
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset {
                IntOffset(
                    x = ((sticker.center.x - 0.5f) * containerWidthPx).roundToInt(),
                    y = ((sticker.center.y - 0.5f) * containerHeightPx).roundToInt(),
                )
            }
            // The gestures sit outside the rotation, so that a drag moves the sticker across the
            // screen rather than along its own, rotated, axes.
            .then(
                if (isInteractive) {
                    Modifier
                        .pointerInput(sticker.id) {
                            detectTapGestures { onStickerSelect(latestSticker.id) }
                        }
                        .pointerInput(sticker.id) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val current = latestSticker
                                if (!isSelectedNow) {
                                    onStickerSelect(current.id)
                                }
                                onStickerChange(
                                    current.copy(
                                        center = NormalizedPoint(
                                            x = (current.center.x + pan.x / containerWidthPx).coerceIn(0f, 1f),
                                            y = (current.center.y + pan.y / containerHeightPx).coerceIn(0f, 1f),
                                        ),
                                        scale = (current.scale * zoom).coerceIn(MarkupSticker.MIN_SCALE, MarkupSticker.MAX_SCALE),
                                        rotationDegrees = current.rotationDegrees + rotation,
                                    )
                                )
                            }
                        }
                } else {
                    Modifier
                }
            )
            .graphicsLayer { rotationZ = sticker.rotationDegrees },
    ) {
        Text(
            text = sticker.text,
            color = sticker.color?.value ?: Color.White,
            softWrap = false,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(
                fontSize = fontSize,
                // Keep light stickers legible on light images, matching the shadow drawn on export.
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(0f, fontSize.value / 32f),
                    blurRadius = fontSize.value / 12f,
                ),
            ),
            modifier = Modifier
                .padding(4.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, ElementTheme.colors.borderInteractivePrimary)
                    } else {
                        Modifier
                    }
                ),
        )
        if (isSelected && isInteractive) {
            IconButton(
                onClick = { onStickerRemove(sticker.id) },
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector = CompoundIcons.Close(),
                    contentDescription = stringResource(CommonStrings.action_remove),
                    tint = ElementTheme.colors.iconPrimary,
                )
            }
        }
    }
}

/**
 * Draws the strokes the user has already made, plus the one currently being drawn.
 */
@Composable
private fun MarkupOverlay(
    imageSize: DpSize,
    strokes: ImmutableList<MarkupStroke>,
    shapes: ImmutableList<MarkupShape>,
    strokeInProgress: ImmutableList<NormalizedPoint>,
    strokeKindInProgress: MarkupStrokeKind?,
    shapeInProgress: MarkupShape?,
    markupColor: MarkupColor,
) {
    Canvas(
        modifier = Modifier.size(imageSize.width, imageSize.height)
    ) {
        val smallestSide = minOf(size.width, size.height)
        for (stroke in strokes) {
            drawMarkupStroke(
                points = stroke.points,
                color = stroke.color.value.copy(alpha = stroke.kind.alpha),
                strokeWidth = smallestSide * stroke.kind.relativeWidth,
            )
        }
        if (strokeKindInProgress != null) {
            drawMarkupStroke(
                points = strokeInProgress,
                color = markupColor.value.copy(alpha = strokeKindInProgress.alpha),
                strokeWidth = smallestSide * strokeKindInProgress.relativeWidth,
            )
        }
        for (shape in shapes + listOfNotNull(shapeInProgress)) {
            drawMarkupShape(shape)
        }
    }
}

private fun DrawScope.drawMarkupShape(shape: MarkupShape) {
    val style = Stroke(
        width = shape.strokeWidth(size.width, size.height),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    for (polyline in shape.polylines(size.width, size.height)) {
        if (polyline.size < 2) continue
        val path = Path().apply {
            moveTo(polyline[0].x, polyline[0].y)
            for (index in 1 until polyline.size) {
                lineTo(polyline[index].x, polyline[index].y)
            }
        }
        drawPath(path = path, color = shape.color.value, style = style)
    }
    shape.ovalBounds(size.width, size.height)?.let { bounds ->
        drawOval(
            color = shape.color.value,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = style,
        )
    }
}

private fun DrawScope.drawMarkupStroke(
    points: ImmutableList<NormalizedPoint>,
    color: Color,
    strokeWidth: Float,
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        // A tap leaves a single point behind, which a path would not render.
        drawCircle(
            color = color,
            radius = strokeWidth / 2f,
            center = Offset(points[0].x * size.width, points[0].y * size.height),
        )
        return
    }
    val path = Path().apply {
        moveTo(points[0].x * size.width, points[0].y * size.height)
        for (index in 1 until points.size) {
            lineTo(points[index].x * size.width, points[index].y * size.height)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun Rect.aspectRatio() = if (height > 0f) width / height else 1f

private fun Offset.toNormalizedPoint(imageRect: Rect): NormalizedPoint {
    val width = imageRect.width.takeIf { it > 0f } ?: 1f
    val height = imageRect.height.takeIf { it > 0f } ?: 1f
    return NormalizedPoint(
        x = ((x - imageRect.left) / width).coerceIn(0f, 1f),
        y = ((y - imageRect.top) / height).coerceIn(0f, 1f),
    )
}

@Composable
private fun CropOverlay(
    imageSize: DpSize,
    cropRect: NormalizedCropRect,
    drawGuidelines: Boolean,
    previewDebug: Boolean,
    touchRadiusPx: Float,
    dragTarget: CropDragTarget?,
) {
    val borderColor = ElementTheme.colors.iconPrimary
    val guideColor = ElementTheme.colors.iconPrimary

    Canvas(
        modifier = Modifier.size(imageSize.width, imageSize.height)
    ) {
        val cropLeft = cropRect.left * size.width
        val cropTop = cropRect.top * size.height
        val cropRight = cropRect.right * size.width
        val cropBottom = cropRect.bottom * size.height
        // Hardcoded black: the crop overlay must always darken the image regardless of theme.
        // No semantic token exists for this use case in the Compound design system.
        val overlayColor = Color.Black.copy(alpha = 0.48f)
        // Overlay above the crop area
        drawRect(
            color = overlayColor,
            topLeft = Offset.Zero,
            size = Size(width = size.width, height = cropTop),
        )
        // Overlay on the left of the crop area
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, cropTop),
            size = Size(width = cropLeft, height = cropBottom - cropTop),
        )
        // Overlay on the right of the crop area
        drawRect(
            color = overlayColor,
            topLeft = Offset(cropRight, cropTop),
            size = Size(width = size.width - cropRight, height = cropBottom - cropTop),
        )
        // Overlay below the crop area
        drawRect(
            color = overlayColor,
            topLeft = Offset(0f, cropBottom),
            size = Size(width = size.width, height = size.height - cropBottom),
        )
        // Main frame of the crop area
        drawRect(
            color = borderColor,
            topLeft = Offset(cropLeft, cropTop),
            size = Size(width = cropRight - cropLeft, height = cropBottom - cropTop),
            style = Stroke(width = 1.dp.toPx()),
        )
        // Guidelines dividing the crop area into 9 equal parts
        if (drawGuidelines) {
            val thirdWidth = (cropRight - cropLeft) / 3f
            val thirdHeight = (cropBottom - cropTop) / 3f
            for (index in 1..2) {
                val offsetX = cropLeft + thirdWidth * index
                val offsetY = cropTop + thirdHeight * index
                // Vertical guide line
                drawLine(
                    color = guideColor,
                    start = Offset(offsetX, cropTop),
                    end = Offset(offsetX, cropBottom),
                    strokeWidth = 1.dp.toPx(),
                )
                // Horizontal guide line
                drawLine(
                    color = guideColor,
                    start = Offset(cropLeft, offsetY),
                    end = Offset(cropRight, offsetY),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        // Corner handles
        val handleLength = 18.dp.toPx()
        val handleOffset = 2.dp.toPx()
        // Top left corner
        drawCornerHandle(
            x = cropLeft - handleOffset,
            y = cropTop - handleOffset,
            handleLength = handleLength,
            color = borderColor,
            position = CropDragTarget.Corner.TopLeft,
        )
        // Top right corner
        drawCornerHandle(
            x = cropRight + handleOffset,
            y = cropTop - handleOffset,
            handleLength = handleLength,
            color = borderColor,
            position = CropDragTarget.Corner.TopRight,
        )
        // Bottom left corner
        drawCornerHandle(
            x = cropLeft - handleOffset,
            y = cropBottom + handleOffset,
            handleLength = handleLength,
            color = borderColor,
            position = CropDragTarget.Corner.BottomLeft,
        )
        // Bottom right corner
        drawCornerHandle(
            x = cropRight + handleOffset,
            y = cropBottom + handleOffset,
            handleLength = handleLength,
            color = borderColor,
            position = CropDragTarget.Corner.BottomRight,
        )
        val handleColor = borderColor
        // Top handle
        drawEdgeHandle(
            center = Offset((cropLeft + cropRight) / 2f, cropTop - handleOffset),
            horizontal = true,
            handleLength = handleLength,
            color = handleColor,
        )
        // Right handle
        drawEdgeHandle(
            center = Offset(cropRight + handleOffset, (cropTop + cropBottom) / 2f),
            horizontal = false,
            handleLength = handleLength,
            color = handleColor,
        )
        // Bottom handle
        drawEdgeHandle(
            center = Offset((cropLeft + cropRight) / 2f, cropBottom + handleOffset),
            horizontal = true,
            handleLength = handleLength,
            color = handleColor,
        )
        // Left handle
        drawEdgeHandle(
            center = Offset(cropLeft - handleOffset, (cropTop + cropBottom) / 2f),
            horizontal = false,
            handleLength = handleLength,
            color = handleColor,
        )

        if (previewDebug) {
            // Draw disk around touchable area
            listOf(
                CropDragTarget.Edge.Top,
                CropDragTarget.Edge.Right,
                CropDragTarget.Edge.Bottom,
                CropDragTarget.Edge.Left,
                CropDragTarget.Corner.TopLeft,
                CropDragTarget.Corner.TopRight,
                CropDragTarget.Corner.BottomRight,
                CropDragTarget.Corner.BottomLeft,
                CropDragTarget.Move,
            ).forEach { target ->
                val color = when (target) {
                    is CropDragTarget.Move -> Color.Red
                    is CropDragTarget.Corner -> Color.Blue
                    is CropDragTarget.Edge -> Color.Green
                }.copy(alpha = if (dragTarget == target) 9f else 0.5f)
                drawCircle(
                    color = color,
                    radius = touchRadiusPx,
                    center = computeOffset(target, cropRect, Size(size.width, size.height)),
                )
            }
        }
    }
}

private fun fitSize(
    containerWidth: Float,
    containerHeight: Float,
    aspectRatio: Float,
): Size {
    val widthBasedHeight = containerWidth / aspectRatio
    return if (widthBasedHeight <= containerHeight) {
        Size(width = containerWidth, height = widthBasedHeight)
    } else {
        Size(width = containerHeight * aspectRatio, height = containerHeight)
    }
}

private fun detectDragTarget(
    touchPoint: Offset,
    imageOffset: Offset,
    cropRect: NormalizedCropRect,
    canvasSize: Size,
    handleTouchRadius: Float,
): CropDragTarget? {
    // Give priority on Move (extra detection of the center of crop area)
    // to ensure that user can move a small crop, then to corners and at last to edges.
    val handlesArea = mapOf(
        CropDragTarget.Move to computeOffset(CropDragTarget.Move, cropRect, canvasSize),
        CropDragTarget.Corner.TopLeft to computeOffset(CropDragTarget.Corner.TopLeft, cropRect, canvasSize),
        CropDragTarget.Corner.TopRight to computeOffset(CropDragTarget.Corner.TopRight, cropRect, canvasSize),
        CropDragTarget.Corner.BottomRight to computeOffset(CropDragTarget.Corner.BottomRight, cropRect, canvasSize),
        CropDragTarget.Corner.BottomLeft to computeOffset(CropDragTarget.Corner.BottomLeft, cropRect, canvasSize),
        CropDragTarget.Edge.Top to computeOffset(CropDragTarget.Edge.Top, cropRect, canvasSize),
        CropDragTarget.Edge.Right to computeOffset(CropDragTarget.Edge.Right, cropRect, canvasSize),
        CropDragTarget.Edge.Bottom to computeOffset(CropDragTarget.Edge.Bottom, cropRect, canvasSize),
        CropDragTarget.Edge.Left to computeOffset(CropDragTarget.Edge.Left, cropRect, canvasSize),
    )
    handlesArea.forEach { (target, corner) ->
        if ((corner - touchPoint + imageOffset).getDistance() <= handleTouchRadius) {
            return target
        }
    }
    val cropLeft = imageOffset.x + cropRect.left * canvasSize.width
    val cropTop = imageOffset.y + cropRect.top * canvasSize.height
    val cropRight = imageOffset.x + cropRect.right * canvasSize.width
    val cropBottom = imageOffset.y + cropRect.bottom * canvasSize.height
    return if (touchPoint.x in cropLeft..cropRight && touchPoint.y in cropTop..cropBottom) {
        CropDragTarget.Move
    } else {
        null
    }
}

private fun computeOffset(
    target: CropDragTarget,
    cropRect: NormalizedCropRect,
    canvasSize: Size,
) = when (target) {
    CropDragTarget.Move -> Offset((cropRect.left + cropRect.right) * canvasSize.width / 2f, (cropRect.top + cropRect.bottom) * canvasSize.height / 2f)
    CropDragTarget.Corner.TopLeft -> Offset(cropRect.left * canvasSize.width, cropRect.top * canvasSize.height)
    CropDragTarget.Edge.Top -> Offset((cropRect.left + cropRect.right) * canvasSize.width / 2f, cropRect.top * canvasSize.height)
    CropDragTarget.Corner.TopRight -> Offset(cropRect.right * canvasSize.width, cropRect.top * canvasSize.height)
    CropDragTarget.Edge.Right -> Offset(cropRect.right * canvasSize.width, (cropRect.top + cropRect.bottom) * canvasSize.height / 2f)
    CropDragTarget.Corner.BottomRight -> Offset(cropRect.right * canvasSize.width, cropRect.bottom * canvasSize.height)
    CropDragTarget.Edge.Bottom -> Offset((cropRect.left + cropRect.right) * canvasSize.width / 2f, cropRect.bottom * canvasSize.height)
    CropDragTarget.Corner.BottomLeft -> Offset(cropRect.left * canvasSize.width, cropRect.bottom * canvasSize.height)
    CropDragTarget.Edge.Left -> Offset(cropRect.left * canvasSize.width, (cropRect.top + cropRect.bottom) * canvasSize.height / 2f)
}

// x and y are the coordinates of the corner
private fun DrawScope.drawCornerHandle(
    x: Float,
    y: Float,
    handleLength: Float,
    color: Color,
    position: CropDragTarget.Corner,
) {
    val strokeWidth = 4.dp.toPx()
    val correction = strokeWidth / 2
    val horizontalCorrection = if (position.isLeft()) -correction else correction
    val horizontalEndX = if (position.isLeft()) x + handleLength else x - handleLength
    val verticalEndY = if (position.isTop()) y + handleLength else y - handleLength
    val verticalCorrection = if (position.isTop()) -correction else correction
    // Horizontal line
    drawLine(
        color = color,
        start = Offset(x + horizontalCorrection, y),
        end = Offset(horizontalEndX + horizontalCorrection, y),
        strokeWidth = strokeWidth,
    )
    // Vertical line
    drawLine(
        color = color,
        start = Offset(x, y + verticalCorrection),
        end = Offset(x, verticalEndY + verticalCorrection),
        strokeWidth = strokeWidth,
    )
}

private fun CropDragTarget.Corner.isLeft() = this == CropDragTarget.Corner.TopLeft || this == CropDragTarget.Corner.BottomLeft
private fun CropDragTarget.Corner.isTop() = this == CropDragTarget.Corner.TopLeft || this == CropDragTarget.Corner.TopRight

private fun DrawScope.drawEdgeHandle(
    center: Offset,
    horizontal: Boolean,
    handleLength: Float,
    color: Color,
) {
    val start = if (horizontal) {
        Offset(center.x - handleLength / 2f, center.y)
    } else {
        Offset(center.x, center.y - handleLength / 2f)
    }
    val end = if (horizontal) {
        Offset(center.x + handleLength / 2f, center.y)
    } else {
        Offset(center.x, center.y + handleLength / 2f)
    }
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 4.dp.toPx(),
    )
}

// Only preview in dark, dark theme is forced on the Node.
@Preview
@Composable
internal fun AttachmentImageEditorViewPreview(
    @PreviewParameter(AttachmentImageEditorStateProvider::class) state: AttachmentImageEditorState,
) = ElementPreviewDark {
    AttachmentImageEditorView(
        state = state,
        onCropRectChange = {},
        onDrawToolSelect = {},
        onMarkupColorSelect = {},
        onStrokeAdd = {},
        onShapeKindSelect = {},
        onShapeAdd = {},
        onErase = { _, _ -> },
        onUndoClick = {},
        onStickerPickerRequest = {},
        onEmojiStickerAdd = {},
        onTextStickerAdd = {},
        onStickerChange = {},
        onStickerSelect = {},
        onStickerRemove = {},
        emojiPickerState = PreviewEmojiPickerState,
        emojiPickerRenderer = NoOpEmojiPickerRenderer,
        onRotateClick = {},
        onFlipHorizontallyClick = {},
        onFlipVerticallyClick = {},
        onResetClick = {},
        onCancelClick = {},
        onDoneClick = {},
    )
}

/**
 * The picker is never opened in a preview, so it only needs to exist.
 */
private object PreviewEmojiPickerState : EmojiPickerState {
    override val isReady = false
}
