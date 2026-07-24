package tech.gomeslab.superapp.feature.alarm.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.gomeslab.superapp.feature.alarm.AlarmIntents
import tech.gomeslab.superapp.feature.alarm.receiver.AlarmReceiver
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Encapsula o AlarmManager. Usa setAlarmClock: é exato e não exige a permissão
// SCHEDULE_EXACT_ALARM, além de exibir o ícone de alarme do sistema.
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val triggerAt = NextTrigger.compute(Calendar.getInstance(), alarm.hour, alarm.minute, alarm.daysOfWeek)
        setClock(alarm.id, triggerAt)
    }

    fun scheduleSnooze(alarm: Alarm) {
        val triggerAt = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
        setClock(alarm.id, triggerAt)
    }

    fun triggerNow(alarmId: Long) {
        setClock(alarmId, System.currentTimeMillis())
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId))
    }

    private fun setClock(alarmId: Long, triggerAt: Long) {
        val info = AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent())
        alarmManager.setAlarmClock(info, firePendingIntent(alarmId))
    }

    private fun firePendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmIntents.ACTION_FIRE
            putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showPendingIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
