package com.yigitozgumus.perseus.impl

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry as Nav3Entry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlin.math.roundToInt

private const val ANIM_DURATION = 300
private const val SCRIM_FRACTION = 0.5f
private const val DISMISS_FRACTION = 0.3f

data class BottomSheetProperties(
    val dismissOnBackPress: Boolean = true,
    val dismissOnSwipeDown: Boolean = true,
    val dismissOnClickOutside: Boolean = true
)

internal data class BottomSheetScene<T : Any>(
    override val key: T,
    override val previousEntries: List<Nav3Entry<T>>,
    override val overlaidEntries: List<Nav3Entry<T>>,
    private val entry: Nav3Entry<T>,
    private val properties: BottomSheetProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<Nav3Entry<T>> = listOf(entry)
    override val content: @Composable (() -> Unit) = {
        val lifecycleOwner = rememberLifecycleOwner()
        BackHandler(enabled = properties.dismissOnBackPress) { onBack() }
        BottomSheetContainer(
            onDismiss = onBack,
            dismissOnSwipeDown = properties.dismissOnSwipeDown,
            dismissOnClickOutside = properties.dismissOnClickOutside
        ) {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                entry.Content()
            }
        }
    }
}

@Composable
private fun BottomSheetContainer(
    onDismiss: () -> Unit,
    dismissOnSwipeDown: Boolean,
    dismissOnClickOutside: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) dragOffset else sheetHeight,
        animationSpec = tween(ANIM_DURATION)
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible && dragOffset == 0f) SCRIM_FRACTION else 0f,
        animationSpec = tween(ANIM_DURATION)
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .then(if (dismissOnClickOutside) Modifier.clickable(
                    indication = null, interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() } else Modifier)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .onGloballyPositioned { sheetHeight = it.size.height.toFloat() }
                .then(if (dismissOnSwipeDown) Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > sheetHeight * DISMISS_FRACTION) onDismiss()
                            else dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onVerticalDrag = { _, amount ->
                            dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                        }
                    )
                } else Modifier)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding(),
            content = content
        )
    }
}

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<Nav3Entry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        val props = last.metadata[BOTTOM_SHEET_KEY] as? BottomSheetProperties ?: return null
        @Suppress("UNCHECKED_CAST")
        return BottomSheetScene(
            key = last.contentKey as T,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = last,
            properties = props,
            onBack = onBack
        )
    }

    companion object {
        fun bottomSheet(properties: BottomSheetProperties = BottomSheetProperties()): Map<String, Any> =
            mapOf(BOTTOM_SHEET_KEY to properties)

        internal const val BOTTOM_SHEET_KEY = "bottomSheet"
    }
}
