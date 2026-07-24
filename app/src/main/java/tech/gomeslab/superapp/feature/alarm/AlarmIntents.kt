package tech.gomeslab.superapp.feature.alarm

// Constantes compartilhadas entre scheduler, receiver, service e a tela de toque.
object AlarmIntents {
    const val ACTION_FIRE = "tech.gomeslab.superapp.alarm.FIRE"
    const val ACTION_START = "tech.gomeslab.superapp.alarm.START"
    const val ACTION_STOP = "tech.gomeslab.superapp.alarm.STOP"
    const val ACTION_SNOOZE = "tech.gomeslab.superapp.alarm.SNOOZE"

    const val EXTRA_ALARM_ID = "alarm_id"
    const val NO_ID = -1L
}

object AlarmNotifications {
    const val CHANNEL_ID = "alarm_channel"
    const val RINGING_NOTIFICATION_ID = 1001
}
