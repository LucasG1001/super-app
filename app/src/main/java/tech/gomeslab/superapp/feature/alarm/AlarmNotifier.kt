package tech.gomeslab.superapp.feature.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import tech.gomeslab.superapp.R
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.service.AlarmService
import tech.gomeslab.superapp.feature.alarm.ui.ring.AlarmRingActivity

// Constrói a notificação do alarme tocando: full-screen intent (abre a AlarmRingActivity sobre
// o lockscreen) + botões Desligar/Soneca (funcionam mesmo com a tela desbloqueada, quando o
// sistema mostra a notificação em vez de abrir a Activity).
object AlarmNotifier {

    fun buildRingingNotification(context: Context, alarm: Alarm): Notification {
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            action = AlarmIntents.ACTION_START
            putExtra(AlarmIntents.EXTRA_ALARM_ID, alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = alarm.label.ifBlank { context.getString(R.string.alarm_default_title) }
        return NotificationCompat.Builder(context, AlarmNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.alarm_ringing))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(0, context.getString(R.string.alarm_snooze), servicePending(context, AlarmIntents.ACTION_SNOOZE, alarm.id))
            .addAction(0, context.getString(R.string.alarm_dismiss), servicePending(context, AlarmIntents.ACTION_STOP, alarm.id))
            .build()
    }

    private fun servicePending(context: Context, action: String, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
        }
        // requestCode distinto por ação para não sobrescrever os dois PendingIntents.
        val requestCode = (alarmId.toInt() shl 2) or action.hashCode().and(0x3)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(AlarmNotifications.RINGING_NOTIFICATION_ID)
    }
}
