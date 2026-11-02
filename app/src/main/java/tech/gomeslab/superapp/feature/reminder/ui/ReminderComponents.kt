package tech.gomeslab.superapp.feature.reminder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import tech.gomeslab.superapp.core.ui.theme.AccentGreen
import tech.gomeslab.superapp.core.ui.theme.AccentOrange
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.reminder.domain.AgendaItem
import tech.gomeslab.superapp.feature.reminder.domain.AgendaStatus
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind

private const val DONE_ALPHA = 0.45f

fun kindIcon(kind: ReminderKind): ImageVector = when (kind) {
    ReminderKind.TASK -> Icons.Filled.NotificationsActive
    ReminderKind.MEDICINE -> Icons.Filled.Medication
    ReminderKind.EVENT -> Icons.Filled.Event
}

fun kindColor(kind: ReminderKind): Color = when (kind) {
    ReminderKind.TASK -> ControlActive
    ReminderKind.MEDICINE -> AccentGreen
    ReminderKind.EVENT -> AccentOrange
}


@Composable
fun KindBadge(kind: ReminderKind, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Sizes.categoryIcon)
            .clip(CircleShape)
            .background(kindColor(kind)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = kindIcon(kind),
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier.size(Sizes.smallIcon),
        )
    }
}

/**
 * Linha da agenda: hora grande (24h) à esquerda e card à direita. Atrasado fica vermelho;
 * tomado/concluído fica esmaecido e riscado. O botão da direita confirma (ou desfaz).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgendaRow(
    item: AgendaItem,
    onConfirm: () -> Unit,
    onUndo: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val reminder = item.reminder
    val finished = !item.isOpen
    val contentAlpha by animateFloatAsState(if (finished) DONE_ALPHA else 1f, label = "agendaAlpha")
    val timeColor = if (item.status == AgendaStatus.OVERDUE) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.occurrence.toLocalTime().label(),
            style = MaterialTheme.typography.titleLarge,
            color = timeColor,
            modifier = Modifier
                .width(Sizes.timelineTime)
                .alpha(contentAlpha),
        )
        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onOpen,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuOpen = true
                            },
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    KindBadge(reminder.kind, Modifier.alpha(contentAlpha))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(contentAlpha),
                    ) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (finished) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val detail = subtitle ?: agendaSubtitle(item)
                        if (detail.isNotBlank()) {
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (item.status == AgendaStatus.OVERDUE) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    ConfirmButton(
                        kind = reminder.kind,
                        done = finished,
                        onClick = if (finished) onUndo else onConfirm,
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        menuOpen = false
                        onOpen()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Excluir lembrete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

private fun agendaSubtitle(item: AgendaItem): String {
    val reminder = item.reminder
    val status = when (item.status) {
        AgendaStatus.OVERDUE -> "Atrasado"
        AgendaStatus.TAKEN -> "Tomado"
        AgendaStatus.SKIPPED -> "Pulado"
        AgendaStatus.DONE -> "Concluído"
        AgendaStatus.PENDING -> null
    }
    return listOfNotNull(status, reminder.dose.takeIf { it.isNotBlank() }, reminder.note.takeIf { it.isNotBlank() })
        .joinToString(" · ")
}

@Composable
private fun ConfirmButton(kind: ReminderKind, done: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (done) ControlActive else Color.Transparent,
        label = "confirmBg",
    )
    if (kind == ReminderKind.MEDICINE && !done) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = ControlActive,
            contentColor = OnPrimary,
        ) {
            Text(
                text = "Tomei",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        return
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = if (done) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (done) null else androidx.compose.foundation.BorderStroke(
            width = Spacing.xxs,
            color = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.size(Sizes.categoryIcon),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Desfazer",
                    modifier = Modifier.size(Sizes.smallIcon),
                )
            }
        }
    }
}
