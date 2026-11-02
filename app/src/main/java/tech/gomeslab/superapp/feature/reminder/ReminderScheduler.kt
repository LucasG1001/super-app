package tech.gomeslab.superapp.feature.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import tech.gomeslab.superapp.feature.reminder.receiver.ReminderReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// Não usa setAlarmClock (que é do despertador): lembretes não devem exibir o ícone de alarme do
// sistema. Exato quando permitido (USE_EXACT_ALARM no manifest); senão, aproximado.
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReminderRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun scheduleNext(reminder: Reminder) {
        val next = ReminderSchedule.nextOccurrence(
            now = LocalDateTime.now(),
            reminder = reminder,
            handled = repository.handledOccurrences(reminder.id),
        )
        if (next == null) {
            alarmManager.cancel(firePendingIntent(reminder.id, null))
            return
        }
        val triggerAt = ReminderSchedule.notifyAt(next, reminder).toMillis()
        setExact(triggerAt, firePendingIntent(reminder.id, next))
    }

    suspend fun rescheduleAll() {
        repository.getEnabled().forEach { scheduleNext(it) }
    }

    fun scheduleNag(reminderId: Long, occurrence: LocalDateTime, attempt: Int, delayMinutes: Long) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        setExact(triggerAt, nagPendingIntent(reminderId, occurrence, attempt))
    }

    fun cancelNag(reminderId: Long) {
        alarmManager.cancel(nagPendingIntent(reminderId, null, 0))
    }

    fun cancel(reminderId: Long) {
        alarmManager.cancel(firePendingIntent(reminderId, null))
        cancelNag(reminderId)
    }

    private fun setExact(triggerAt: Long, operation: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun firePendingIntent(reminderId: Long, occurrence: LocalDateTime?): PendingIntent =
        broadcast(ReminderIntents.ACTION_FIRE, reminderId, occurrence, 0)

    private fun nagPendingIntent(reminderId: Long, occurrence: LocalDateTime?, attempt: Int): PendingIntent =
        broadcast(ReminderIntents.ACTION_NAG, reminderId, occurrence, attempt)

    private fun broadcast(action: String, reminderId: Long, occurrence: LocalDateTime?, attempt: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderIntents.EXTRA_REMINDER_ID, reminderId)
            occurrence?.let { putExtra(ReminderIntents.EXTRA_OCCURRENCE, it.toString()) }
            putExtra(ReminderIntents.EXTRA_ATTEMPT, attempt)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
