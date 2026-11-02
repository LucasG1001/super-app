package tech.gomeslab.superapp.feature.reminder

import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Ponto único para responder a uma ocorrência — usado pela notificação, pela UI e pelos comandos. */
@Singleton
class ReminderActions @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: ReminderNotifier,
) {

    suspend fun onFire(reminderId: Long, occurrence: LocalDateTime?, attempt: Int) {
        val reminder = repository.getById(reminderId) ?: return
        if (!reminder.enabled) return
        if (occurrence != null && !repository.isHandled(reminderId, occurrence)) {
            notifier.showOccurrence(reminder, occurrence, attempt)
            if (reminder.kind == ReminderKind.MEDICINE && attempt < ReminderNotifications.MAX_NAGS) {
                scheduler.scheduleNag(reminderId, occurrence, attempt + 1, ReminderNotifications.NAG_INTERVAL_MINUTES)
            }
        }
        if (attempt == 0) scheduler.scheduleNext(reminder)
    }

    suspend fun record(reminderId: Long, occurrence: LocalDateTime, status: LogStatus) {
        val updated = repository.record(reminderId, occurrence, status) ?: return
        clear(reminderId)
        if (status == LogStatus.TAKEN && updated.needsRefill) notifier.showRefill(updated)
        finishOneShot(updated)
        scheduler.scheduleNext(updated)
    }

    suspend fun undo(reminderId: Long, occurrence: LocalDateTime) {
        repository.undo(reminderId, occurrence)
        val reminder = repository.getById(reminderId) ?: return
        val reopened = if (!reminder.enabled && reminder.isOneShotTask()) {
            repository.setEnabled(reminderId, true)
            reminder.copy(enabled = true)
        } else {
            reminder
        }
        scheduler.scheduleNext(reopened)
    }

    fun snooze(reminderId: Long, occurrence: LocalDateTime) {
        clear(reminderId)
        scheduler.scheduleNag(reminderId, occurrence, 0, ReminderNotifications.SNOOZE_MINUTES)
    }

    suspend fun delete(reminderId: Long) {
        clear(reminderId)
        notifier.cancelRefill(reminderId)
        scheduler.cancel(reminderId)
        repository.delete(reminderId)
    }

    private fun clear(reminderId: Long) {
        scheduler.cancelNag(reminderId)
        notifier.cancelOccurrence(reminderId)
    }

    private suspend fun finishOneShot(reminder: Reminder) {
        if (!reminder.isOneShotTask()) return
        val pending = repository.handledOccurrences(reminder.id).size < reminder.times.size
        if (!pending) repository.setEnabled(reminder.id, false)
    }

    private fun Reminder.isOneShotTask(): Boolean =
        kind != ReminderKind.MEDICINE && repeat == ReminderRepeat.NONE
}
