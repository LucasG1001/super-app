package tech.gomeslab.superapp.feature.alarm.ui.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import tech.gomeslab.superapp.feature.alarm.domain.GradualVolume

// Toca o som do alarme em loop e (opcionalmente) vibra. Com aumento gradual, o volume sobe do quase
// silêncio até o máximo do volume de alarme do sistema, e a vibração só começa na metade da rampa.
class AlarmPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var rampStartedAt = 0L
    private var rampDurationMillis = 0L
    private var vibrationPending = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    private val rampStep = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime() - rampStartedAt
            val volume = GradualVolume.volumeAt(elapsed, rampDurationMillis)
            player?.setVolume(volume, volume)
            if (vibrationPending && elapsed >= rampDurationMillis / 2) {
                vibrationPending = false
                startVibration()
            }
            if (volume < 1f || vibrationPending) handler.postDelayed(this, RAMP_TICK_MILLIS)
        }
    }

    fun start(soundUri: String?, vibrate: Boolean, gradualVolumeSeconds: Int = 0) {
        stop()
        val gradual = gradualVolumeSeconds > 0
        rampDurationMillis = gradualVolumeSeconds * 1_000L
        vibrationPending = vibrate && gradual

        val uri: Uri? = soundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)

        if (uri != null) {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                val initial = if (gradual) GradualVolume.volumeAt(0, rampDurationMillis) else 1f
                setVolume(initial, initial)
                start()
            }
        }

        if (gradual) {
            rampStartedAt = SystemClock.elapsedRealtime()
            handler.post(rampStep)
        } else if (vibrate) {
            startVibration()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 600, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    fun stop() {
        handler.removeCallbacks(rampStep)
        vibrationPending = false
        player?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        player = null
        vibrator?.cancel()
    }

    private companion object {
        const val RAMP_TICK_MILLIS = 500L
    }
}
