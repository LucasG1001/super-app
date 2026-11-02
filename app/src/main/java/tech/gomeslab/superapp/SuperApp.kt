package tech.gomeslab.superapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.alarm.AlarmNotifications
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import tech.gomeslab.superapp.feature.reminder.ReminderNotifications
import tech.gomeslab.superapp.feature.reminder.ReminderScheduler
import tech.gomeslab.superapp.feature.workout.rest.WorkoutNotifications
import javax.inject.Inject

@HiltAndroidApp
class SuperApp : Application() {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        rescheduleAll()
    }

    // "Forçar parada" e reinstalações apagam os agendamentos do AlarmManager; reagendar a cada
    // abertura do processo é idempotente e garante que nada fica para trás.
    private fun rescheduleAll() {
        appScope.launch {
            alarmRepository.getEnabled().forEach { alarmScheduler.schedule(it) }
            reminderScheduler.rescheduleAll()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val alarmChannel = NotificationChannel(
            AlarmNotifications.CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        val reminderChannel = NotificationChannel(
            ReminderNotifications.CHANNEL_ID,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.reminder_channel_description)
            enableVibration(true)
        }
        val refillChannel = NotificationChannel(
            ReminderNotifications.REFILL_CHANNEL_ID,
            getString(R.string.refill_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.refill_channel_description)
        }
        val restRunningChannel = NotificationChannel(
            WorkoutNotifications.RUNNING_CHANNEL_ID,
            getString(R.string.rest_running_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.rest_running_channel_description)
        }
        val restDoneChannel = NotificationChannel(
            WorkoutNotifications.DONE_CHANNEL_ID,
            getString(R.string.rest_done_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.rest_done_channel_description)
            enableVibration(true)
        }
        manager.createNotificationChannels(
            listOf(alarmChannel, reminderChannel, refillChannel, restRunningChannel, restDoneChannel),
        )
    }
}
