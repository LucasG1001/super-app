package tech.gomeslab.superapp.feature.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.alarm.AlarmIntents
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import tech.gomeslab.superapp.feature.alarm.service.AlarmService
import tech.gomeslab.superapp.feature.alarm.ui.ring.AlarmRingActivity
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: AlarmRepository
    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, AlarmIntents.NO_ID)
        if (id == AlarmIntents.NO_ID) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val alarm = repository.getById(id) ?: return@launch
                if (!alarm.enabled) return@launch
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmIntents.ACTION_START
                    putExtra(AlarmIntents.EXTRA_ALARM_ID, id)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // Ao disparar por exact alarm, o app tem uma janela para iniciar Activity em
                // background — tenta abrir a tela cheia mesmo com o aparelho desbloqueado.
                // Se o SO bloquear, o full-screen intent da notificação assume (tela travada).
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    action = AlarmIntents.ACTION_START
                    putExtra(AlarmIntents.EXTRA_ALARM_ID, id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(ringIntent) }

                if (alarm.repeats) {
                    scheduler.schedule(alarm)
                } else {
                    repository.setEnabled(id, false)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
