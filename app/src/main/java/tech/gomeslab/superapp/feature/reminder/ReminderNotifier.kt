package tech.gomeslab.superapp.feature.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.receiver.ReminderReceiver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun showOccurrence(reminder: Reminder, occurrence: LocalDateTime, attempt: Int) {
        val title = when (reminder.kind) {
            ReminderKind.MEDICINE -> "Hora do remédio: ${reminder.title}"
            else -> reminder.title
        }
        val text = buildList {
            when (reminder.kind) {
                ReminderKind.MEDICINE -> add(reminder.dose.ifBlank { "Dose das ${occurrence.format(timeFormat)}" })
                ReminderKind.EVENT -> add(eventText(reminder, occurrence))
                ReminderKind.TASK -> add(occurrence.format(timeFormat))
            }
            if (attempt > 0) add("Lembrete ${attempt + 1}")
        }.joinToString(" · ")

        val builder = NotificationCompat.Builder(context, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(text, reminder.note).filter { it.isNotBlank() }.joinToString("\n")))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent())
            .setAutoCancel(reminder.kind != ReminderKind.MEDICINE)
            .setOnlyAlertOnce(false)

        if (reminder.kind == ReminderKind.MEDICINE) {
            builder.addAction(0, "Tomei", action(ReminderIntents.ACTION_TAKEN, reminder.id, occurrence))
            builder.addAction(0, "Adiar 10 min", action(ReminderIntents.ACTION_SNOOZE, reminder.id, occurrence))
            builder.addAction(0, "Pular", action(ReminderIntents.ACTION_SKIP, reminder.id, occurrence))
        } else {
            builder.addAction(0, "Concluir", action(ReminderIntents.ACTION_DONE, reminder.id, occurrence))
            builder.addAction(0, "Adiar 10 min", action(ReminderIntents.ACTION_SNOOZE, reminder.id, occurrence))
        }
        notify(ReminderNotifications.occurrenceNotificationId(reminder.id), builder)
    }

    fun showRefill(reminder: Reminder) {
        val stock = reminder.stock ?: return
        val builder = NotificationCompat.Builder(context, ReminderNotifications.REFILL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Hora de comprar ${reminder.title}")
            .setContentText(if (stock == 0) "O estoque acabou." else "Restam $stock.")
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
        notify(ReminderNotifications.refillNotificationId(reminder.id), builder)
    }

    fun cancelOccurrence(reminderId: Long) {
        manager.cancel(ReminderNotifications.occurrenceNotificationId(reminderId))
    }

    fun cancelRefill(reminderId: Long) {
        manager.cancel(ReminderNotifications.refillNotificationId(reminderId))
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        manager.notify(id, builder.build())
    }

    private fun eventText(reminder: Reminder, occurrence: LocalDateTime): String {
        val time = occurrence.format(timeFormat)
        return when (val lead = reminder.leadMinutes) {
            0 -> "Agora, às $time"
            in 1..59 -> "Em $lead min, às $time"
            in 60..1439 -> "Em ${lead / 60} h, às $time"
            else -> "Amanhã às $time"
        }
    }

    private fun openAppIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(action: String, reminderId: Long, occurrence: LocalDateTime): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderIntents.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderIntents.EXTRA_OCCURRENCE, occurrence.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
