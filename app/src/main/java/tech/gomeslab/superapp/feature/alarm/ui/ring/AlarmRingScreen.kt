package tech.gomeslab.superapp.feature.alarm.ui.ring

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.ui.theme.RingContent
import tech.gomeslab.superapp.core.ui.theme.RingContentSecondary
import tech.gomeslab.superapp.core.ui.theme.RingGradientBottom
import tech.gomeslab.superapp.core.ui.theme.RingGradientTop
import tech.gomeslab.superapp.core.ui.theme.RingSurface
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.ui.formatTime
import tech.gomeslab.superapp.feature.alarm.ui.fullDateLabel
import java.util.Calendar
import kotlin.math.hypot

@Composable
fun AlarmRingScreen(
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000)
        }
    }
    val calendar = Calendar.getInstance().apply { timeInMillis = now }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RingGradientTop, RingGradientBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(Spacing.lg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val label = alarm?.label.orEmpty()
            Text(
                text = label.ifBlank { "Alarme" },
                style = MaterialTheme.typography.titleLarge,
                color = RingContent,
                textAlign = TextAlign.Center,
            )
            Text(
                text = formatTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)),
                style = MaterialTheme.typography.displaySmall,
                color = RingContent,
            )
            Text(
                text = fullDateLabel(now),
                style = MaterialTheme.typography.bodyLarge,
                color = RingContentSecondary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DragToDismiss(onDismiss = onDismiss)
            Spacer(Modifier.height(Spacing.lg))
            SnoozeButton(minutes = alarm?.snoozeMinutes, onClick = onSnooze)
        }
    }
}

@Composable
private fun SnoozeButton(minutes: Int?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = RingSurface,
        contentColor = RingContent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(Icons.Filled.Snooze, contentDescription = null)
            Text(
                text = if (minutes != null) "Soneca · $minutes min" else "Soneca",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DragToDismiss(onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val thresholdPx = with(density) { Sizes.dismissThreshold.toPx() }
    val handlePx = with(density) { Sizes.dismissHandle.toPx() }

    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var distance by remember { mutableFloatStateOf(0f) }
    var crossed by remember { mutableStateOf(false) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1100), RepeatMode.Reverse),
        label = "pulseScale",
    )

    val areaSize = Sizes.dismissHandle * 2
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(areaSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragEnd = {
                            if (distance >= thresholdPx) {
                                onDismiss()
                            } else {
                                scope.launch {
                                    animate(
                                        initialValue = distance,
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    ) { value, _ -> distance = value }
                                }
                            }
                            crossed = false
                        },
                        onDragCancel = {
                            distance = 0f
                            crossed = false
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                            dragY += amount.y
                            distance = hypot(dragX, dragY).coerceAtMost(thresholdPx * 1.1f)
                            if (distance >= thresholdPx && !crossed) {
                                crossed = true
                                view.performHapticFeedback(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        HapticFeedbackConstants.CONFIRM
                                    } else {
                                        HapticFeedbackConstants.LONG_PRESS
                                    },
                                )
                            } else if (distance < thresholdPx) {
                                crossed = false
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val progress = (distance / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .size(Sizes.dismissHandle)
                    .graphicsLayer {
                        val grow = (handlePx + distance * 2f) / handlePx
                        val scale = if (distance > 0f) grow else pulse
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.35f + progress * 0.45f
                    }
                    .clip(CircleShape)
                    .background(RingSurface),
            )
            Box(
                modifier = Modifier
                    .size(Sizes.dismissHandle)
                    .clip(CircleShape)
                    .background(RingSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Desligar",
                    tint = RingContent,
                )
            }
        }
        Text(
            text = "Arraste para desligar",
            style = MaterialTheme.typography.bodyMedium,
            color = RingContentSecondary,
        )
    }
}
