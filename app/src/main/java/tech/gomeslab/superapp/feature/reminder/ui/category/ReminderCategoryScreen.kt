package tech.gomeslab.superapp.feature.reminder.ui.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.ui.AgendaRow
import tech.gomeslab.superapp.feature.reminder.ui.KindBadge
import tech.gomeslab.superapp.feature.reminder.ui.home.EmptyHint
import tech.gomeslab.superapp.feature.reminder.ui.home.ReminderCategory

@Composable
fun ReminderCategoryScreen(
    onBack: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: ReminderCategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subtitle = when (state.rows.size) {
        0 -> null
        1 -> "1 item"
        else -> "${state.rows.size} itens"
    }

    ExtendedScaffold(
        title = state.category.title,
        subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = innerPadding.calculateTopPadding() + Spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (state.rows.isEmpty()) {
                item { EmptyHint(emptyText(state.category)) }
            }
            items(state.rows, key = { it.key }) { row ->
                when (row) {
                    is CategoryRow.AgendaEntry -> AgendaRow(
                        item = row.item,
                        subtitle = row.subtitle,
                        onConfirm = { viewModel.confirm(row.item) },
                        onUndo = { viewModel.undo(row.item.reminder.id, row.item.occurrence) },
                        onOpen = { onOpenReminder(row.item.reminder.id) },
                        onDelete = { viewModel.delete(row.item.reminder.id) },
                        modifier = Modifier.animateItem(),
                    )
                    is CategoryRow.Summary -> SummaryRow(
                        reminder = row.reminder,
                        subtitle = row.subtitle,
                        warning = row.warning,
                        onClick = { onOpenReminder(row.reminder.id) },
                        modifier = Modifier.animateItem(),
                    )
                    is CategoryRow.History -> SummaryRow(
                        reminder = row.reminder,
                        subtitle = row.subtitle,
                        warning = null,
                        onClick = { onOpenReminder(row.reminder.id) },
                        trailing = {
                            IconButton(onClick = { viewModel.undo(row.reminder.id, row.occurrence) }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Desfazer")
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

private fun emptyText(category: ReminderCategory): String = when (category) {
    ReminderCategory.TODAY -> "Nada para hoje."
    ReminderCategory.SCHEDULED -> "Nenhum lembrete agendado."
    ReminderCategory.MEDICINE -> "Nenhum remédio cadastrado."
    ReminderCategory.DONE -> "Nada concluído nos últimos 30 dias."
}

@Composable
private fun SummaryRow(
    reminder: Reminder,
    subtitle: String,
    warning: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            KindBadge(reminder.kind)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (warning != null) {
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (reminder.needsRefill) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            trailing?.invoke()
        }
    }
}
