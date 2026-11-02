package tech.gomeslab.superapp.core.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.distinctUntilChanged
import tech.gomeslab.superapp.core.ui.theme.Sizes
import kotlin.math.abs

private const val LOOP_MULTIPLIER = 1000

/**
 * Seletor em "rolo" circular (estilo relógio One UI): valores 0 until [count], com snap no item central,
 * vizinhos menores/esmaecidos e um tique háptico a cada troca.
 */
@Composable
fun WheelPicker(
    count: Int,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    visibleItems: Int = 3,
) {
    val half = visibleItems / 2
    val totalItems = count * LOOP_MULTIPLIER
    val middleBase = (totalItems / 2) - (totalItems / 2) % count

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleBase + selected - half,
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current
    val currentOnChange by rememberUpdatedState(onSelectedChange)

    LaunchedEffect(listState, count) {
        var first = true
        snapshotFlow { listState.centeredIndex() }
            .distinctUntilChanged()
            .collect { index ->
                if (index < 0) return@collect
                if (first) {
                    first = false
                    return@collect
                }
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                currentOnChange(index % count)
            }
    }

    LaunchedEffect(selected) {
        val centered = listState.centeredIndex()
        if (!listState.isScrollInProgress && centered >= 0 && centered % count != selected) {
            val delta = selected - centered % count
            listState.scrollToItem(centered + delta - half)
        }
    }

    Box(
        modifier = modifier
            .width(Sizes.wheelWidth)
            .height(Sizes.wheelItemHeight * visibleItems),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(count = totalItems) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.wheelItemHeight)
                        .graphicsLayer {
                            val distance = listState.distanceFromCenter(index) / size.height
                            val clamped = distance.coerceIn(0f, 2f)
                            alpha = 1f - clamped * 0.45f
                            val scale = 1f - clamped * 0.22f
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(index % count),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun LazyListState.viewportCenter(): Float {
    val info = layoutInfo
    return (info.viewportStartOffset + info.viewportEndOffset) / 2f
}

private fun LazyListState.centeredIndex(): Int {
    val center = viewportCenter()
    return layoutInfo.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2f - center) }
        ?.index ?: -1
}

private fun LazyListState.distanceFromCenter(index: Int): Float {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return Float.MAX_VALUE
    return abs(item.offset + item.size / 2f - viewportCenter())
}
