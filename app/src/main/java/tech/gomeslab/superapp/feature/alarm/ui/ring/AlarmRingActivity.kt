package tech.gomeslab.superapp.feature.alarm.ui.ring

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.ui.theme.SuperAppTheme
import tech.gomeslab.superapp.feature.alarm.AlarmIntents
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.service.AlarmService
import javax.inject.Inject

// Tela exibida quando o alarme toca (via full-screen intent). Aparece sobre a tela de bloqueio
// e liga o display. O som/vibração são do AlarmService; aqui só encaminhamos Desligar/Soneca.
@AndroidEntryPoint
class AlarmRingActivity : ComponentActivity() {

    @Inject lateinit var repository: AlarmRepository

    private var alarm by mutableStateOf<Alarm?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()

        val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, AlarmIntents.NO_ID)

        setContent {
            SuperAppTheme(darkTheme = true) {
                AlarmRingScreen(
                    alarm = alarm,
                    onDismiss = { sendToService(AlarmIntents.ACTION_STOP) },
                    onSnooze = { sendToService(AlarmIntents.ACTION_SNOOZE) },
                )
            }
        }

        lifecycleScope.launch { alarm = repository.getById(id) }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
    }

    private fun sendToService(action: String) {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmIntents.EXTRA_ALARM_ID, alarm?.id ?: AlarmIntents.NO_ID)
        }
        startService(intent)
        finish()
    }
}
