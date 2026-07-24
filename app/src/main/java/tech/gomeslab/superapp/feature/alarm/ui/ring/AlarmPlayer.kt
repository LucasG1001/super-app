package tech.gomeslab.superapp.feature.alarm.ui.ring

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// Toca o som do alarme em loop e (opcionalmente) vibra. Vive junto da AlarmRingActivity.
class AlarmPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun start(soundUri: String?, vibrate: Boolean) {
        val uri: Uri = soundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: return

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
            start()
        }

        if (vibrate) {
            val pattern = longArrayOf(0, 600, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    fun stop() {
        player?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        player = null
        vibrator?.cancel()
    }
}
