package tech.gomeslab.superapp.feature.alarm.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.theme.Spacing

private val DAY_LABELS = listOf("D", "S", "T", "Q", "Q", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Novo alarme" else "Editar alarme") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        key(state.isNew, state.id) {
            val timeState = rememberTimePickerState(
                initialHour = state.hour,
                initialMinute = state.minute,
                is24Hour = true,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timeState)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text("Repetir", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        (0..6).forEach { day ->
                            FilterChip(
                                selected = state.repeatsOn(day),
                                onClick = { viewModel.toggleDay(day) },
                                label = { Text(DAY_LABELS[day]) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.label,
                    onValueChange = viewModel::setLabel,
                    label = { Text("Nome do alarme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Vibrar", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.vibrate, onCheckedChange = viewModel::setVibrate)
                }

                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.setTime(timeState.hour, timeState.minute)
                        viewModel.save(onDone)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Salvar")
                }
            }
        }
    }
}

private fun AlarmEditState.repeatsOn(dayBitIndex: Int): Boolean =
    (daysOfWeek and (1 shl dayBitIndex)) != 0
