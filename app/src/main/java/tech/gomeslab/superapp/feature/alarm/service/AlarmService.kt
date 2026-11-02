package tech.gomeslab.superapp.feature.alarm.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.gomeslab.superapp.feature.alarm.AlarmIntents
import tech.gomeslab.superapp.feature.alarm.AlarmNotifier
import tech.gomeslab.superapp.feature.alarm.AlarmNotifications
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import tech.gomeslab.superapp.feature.alarm.ui.ring.AlarmPlayer
import javax.inject.Inject

// Toca o som e vibra em primeiro plano (foreground service), independente de a tela de alarme
// abrir ou não. É o que garante que o alarme "toca" mesmo com o aparelho desbloqueado.
@AndroidEntryPoint
class AlarmService : Service() {

    @Inject lateinit var repository: AlarmRepository
    @Inject lateinit var scheduler: AlarmScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val player by lazy { AlarmPlayer(applicationContext) }
    private var currentAlarm: Alarm? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmIntents.ACTION_START -> start(intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, AlarmIntents.NO_ID))
            AlarmIntents.ACTION_STOP -> stopRinging()
            AlarmIntents.ACTION_SNOOZE -> snooze()
            else -> stopRinging()
        }
        return START_NOT_STICKY
    }

    private fun start(alarmId: Long) {
        if (alarmId == AlarmIntents.NO_ID) {
            stopSelf()
            return
        }
        // Leitura rápida do Room para montar a notificação antes do startForeground.
        val alarm = runBlocking { repository.getById(alarmId) }
        if (alarm == null) {
            stopSelf()
            return
        }
        currentAlarm = alarm

        val notification = AlarmNotifier.buildRingingNotification(this, alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AlarmNotifications.RINGING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(AlarmNotifications.RINGING_NOTIFICATION_ID, notification)
        }

        player.start(alarm.soundUri, alarm.vibrate, alarm.gradualVolumeSeconds)
    }

    private fun snooze() {
        val alarm = currentAlarm
        if (alarm != null) {
            scope.launch { scheduler.scheduleSnooze(alarm) }
        }
        stopRinging()
    }

    private fun stopRinging() {
        player.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.stop()
        scope.cancel()
    }
}
