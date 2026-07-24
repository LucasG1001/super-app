package tech.gomeslab.superapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import tech.gomeslab.superapp.feature.alarm.AlarmNotifications

@HiltAndroidApp
class SuperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
        manager.createNotificationChannel(alarmChannel)
    }
}
