package tech.gomeslab.superapp.feature.alarm.ui.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.gomeslab.superapp.core.ui.components.ChoiceDialog
import tech.gomeslab.superapp.core.ui.components.DayCircle
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.OneUiSwitch
import tech.gomeslab.superapp.core.ui.components.OptionDivider
import tech.gomeslab.superapp.core.ui.components.OptionRow
import tech.gomeslab.superapp.core.ui.components.TimeWheels
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.NextTrigger
import tech.gomeslab.superapp.feature.alarm.ui.DAY_INITIALS
import tech.gomeslab.superapp.feature.alarm.ui.dayLabel
import tech.gomeslab.superapp.feature.alarm.ui.daysLabel
import tech.gomeslab.superapp.feature.alarm.ui.gradualVolumeLabel
import tech.gomeslab.superapp.feature.alarm.ui.snoozeLabel
import tech.gomeslab.superapp.feature.alarm.ui.untilLabel
import java.util.Calendar

private val SNOOZE_OPTIONS = listOf(5, 10, 15, 30)
private val GRADUAL_OPTIONS = listOf(0, 30, 60, 5 * 60, 10 * 60, 15 * 60, 30 * 60)

private enum class EditDialog { SNOOZE, GRADUAL }

@Composable
fun AlarmEditScreen(
    onDone: () -> Unit,
    viewModel: AlarmEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var openDialog by rememberSaveable { mutableStateOf<EditDialog?>(null) }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        viewModel.setSound(picked?.takeIf { it != Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString())
    }

    val soundTitle by produceState(initialValue = "Padrão", state.soundUri) {
        value = withContext(Dispatchers.IO) { ringtoneTitle(context, state.soundUri) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val nowCal = Calendar.getInstance()
            val triggerAt = NextTrigger.compute(nowCal, state.hour, state.minute, state.daysOfWeek)

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = untilLabel(triggerAt - nowCal.timeInMillis),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TimeWheels(
                hour = state.hour,
                minute = state.minute,
                onHourChange = viewModel::setHour,
                onMinuteChange = viewModel::setMinute,
            )

            FocusCard {
                Text(
                    text = if (state.daysOfWeek == 0) dayLabel(triggerAt, nowCal) else daysLabel(state.daysOfWeek),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        start = Spacing.lg,
                        end = Spacing.lg,
                        top = Spacing.lg,
                        bottom = Spacing.md,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DAY_INITIALS.forEachIndexed { day, initial ->
                        DayCircle(
                            label = initial,
                            selected = (state.daysOfWeek and (1 shl day)) != 0,
                            isSunday = day == Alarm.SUNDAY,
                            onClick = { viewModel.toggleDay(day) },
                        )
                    }
                }
                TextField(
                    value = state.label,
                    onValueChange = viewModel::setLabel,
                    placeholder = { Text("Nome do alarme") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = ControlActive,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = ControlActive,
                    ),
                )
            }

            FocusCard {
                OptionRow(
                    title = "Som do alarme",
                    subtitle = soundTitle,
                    onClick = {
                        ringtonePicker.launch(ringtonePickerIntent(state.soundUri))
                    },
                )
                OptionDivider()
                OptionRow(
                    title = "Vibração",
                    onClick = { viewModel.setVibrate(!state.vibrate) },
                    trailing = {
                        OneUiSwitch(checked = state.vibrate, onCheckedChange = viewModel::setVibrate)
                    },
                )
                OptionDivider()
                OptionRow(
                    title = "Soneca",
                    subtitle = snoozeLabel(state.snoozeMinutes),
                    onClick = { openDialog = EditDialog.SNOOZE },
                )
                OptionDivider()
                OptionRow(
                    title = "Aumento gradual do volume",
                    subtitle = gradualVolumeLabel(state.gradualVolumeSeconds),
                    onClick = { openDialog = EditDialog.GRADUAL },
                )
            }

            Spacer(Modifier.height(Spacing.md))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.bottomBarHeight)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            TextButton(onClick = { viewModel.save(onDone) }, modifier = Modifier.weight(1f)) {
                Text("Salvar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }

    when (openDialog) {
        EditDialog.SNOOZE -> ChoiceDialog(
            title = "Soneca",
            options = SNOOZE_OPTIONS,
            current = state.snoozeMinutes,
            label = ::snoozeLabel,
            onSelect = {
                viewModel.setSnooze(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        EditDialog.GRADUAL -> ChoiceDialog(
            title = "Aumento gradual do volume",
            options = GRADUAL_OPTIONS,
            current = state.gradualVolumeSeconds,
            label = ::gradualVolumeLabel,
            onSelect = {
                viewModel.setGradualVolume(it)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        null -> Unit
    }
}

private fun ringtonePickerIntent(currentUri: String?): Intent =
    Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Som do alarme")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            currentUri?.toUri() ?: Settings.System.DEFAULT_ALARM_ALERT_URI,
        )
    }

private fun ringtoneTitle(context: Context, soundUri: String?): String {
    if (soundUri == null) return "Padrão"
    return runCatching {
        RingtoneManager.getRingtone(context, soundUri.toUri())?.getTitle(context)
    }.getOrNull() ?: "Padrão"
}
