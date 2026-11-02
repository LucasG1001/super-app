package tech.gomeslab.superapp.core.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing

private const val EXPANDED_HEADER_FRACTION = 0.35f

/**
 * Scaffold com o "extended app bar" do One UI: título grande centralizado no terço superior
 * (área de leitura) que condensa numa barra de 56dp ao rolar o conteúdo e reexpande ao voltar ao topo.
 */
@Composable
fun ExtendedScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapsedTitle: String = title,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val collapsedPx = with(density) { Sizes.topBarHeight.toPx() }
        val expandedPx = with(density) { maxHeight.toPx() * EXPANDED_HEADER_FRACTION }
            .coerceAtLeast(collapsedPx)
        val rangePx = expandedPx - collapsedPx

        var offsetPx by rememberSaveable { mutableFloatStateOf(0f) }
        val clampedOffset = offsetPx.coerceIn(-rangePx, 0f)
        val fraction = if (rangePx > 0f) -clampedOffset / rangePx else 1f

        val connection = remember(rangePx) {
            object : NestedScrollConnection {
                private fun consume(delta: Float): Float {
                    val old = offsetPx.coerceIn(-rangePx, 0f)
                    val new = (old + delta).coerceIn(-rangePx, 0f)
                    offsetPx = new
                    return new - old
                }

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                    if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset =
                    if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val current = offsetPx.coerceIn(-rangePx, 0f)
                    if (current != 0f && current != -rangePx) {
                        val target = if (-current / rangePx > 0.5f) -rangePx else 0f
                        animate(current, target) { value, _ -> offsetPx = value }
                    }
                    return Velocity.Zero
                }
            }
        }

        val headerHeight = with(density) { (expandedPx + clampedOffset).toDp() }

        Scaffold(
            modifier = Modifier.nestedScroll(connection),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(headerHeight),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clipToBounds()
                            .graphicsLayer { alpha = (1f - fraction * 1.6f).coerceIn(0f, 1f) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.topBarHeight)
                            .padding(horizontal = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navigationIcon?.invoke()
                        Text(
                            text = collapsedTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.md)
                                .graphicsLayer { alpha = ((fraction - 0.6f) / 0.4f).coerceIn(0f, 1f) },
                        )
                        actions()
                    }
                }
            },
            bottomBar = bottomBar,
            content = content,
        )
    }
}
