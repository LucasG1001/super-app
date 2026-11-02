package tech.gomeslab.superapp.feature.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.reminder.ReminderActions
import tech.gomeslab.superapp.feature.reminder.ReminderIntents
import tech.gomeslab.superapp.feature.reminder.ReminderScheduler
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var actions: ReminderActions

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return
        val occurrence = intent.getStringExtra(ReminderIntents.EXTRA_OCCURRENCE)
            ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        val attempt = intent.getIntExtra(ReminderIntents.EXTRA_ATTEMPT, 0)

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ReminderIntents.ACTION_FIRE, ReminderIntents.ACTION_NAG ->
                        actions.onFire(reminderId, occurrence, attempt)
                    ReminderIntents.ACTION_TAKEN -> occurrence?.let { actions.record(reminderId, it, LogStatus.TAKEN) }
                    ReminderIntents.ACTION_SKIP -> occurrence?.let { actions.record(reminderId, it, LogStatus.SKIPPED) }
                    ReminderIntents.ACTION_DONE -> occurrence?.let { actions.record(reminderId, it, LogStatus.DONE) }
                    ReminderIntents.ACTION_SNOOZE -> occurrence?.let { actions.snooze(reminderId, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}

@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                scheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
