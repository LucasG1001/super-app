package tech.gomeslab.superapp.feature.reminder

object ReminderIntents {
    const val ACTION_FIRE = "tech.gomeslab.superapp.reminder.FIRE"
    const val ACTION_NAG = "tech.gomeslab.superapp.reminder.NAG"
    const val ACTION_TAKEN = "tech.gomeslab.superapp.reminder.TAKEN"
    const val ACTION_SKIP = "tech.gomeslab.superapp.reminder.SKIP"
    const val ACTION_DONE = "tech.gomeslab.superapp.reminder.DONE"
    const val ACTION_SNOOZE = "tech.gomeslab.superapp.reminder.SNOOZE"

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_OCCURRENCE = "occurrence"
    const val EXTRA_ATTEMPT = "attempt"
}

object ReminderNotifications {
    const val CHANNEL_ID = "reminder_channel"
    const val REFILL_CHANNEL_ID = "medicine_stock_channel"

    const val NAG_INTERVAL_MINUTES = 10L
    const val MAX_NAGS = 3
    const val SNOOZE_MINUTES = 10L

    private const val OCCURRENCE_BASE_ID = 20_000
    private const val REFILL_BASE_ID = 40_000

    fun occurrenceNotificationId(reminderId: Long): Int = OCCURRENCE_BASE_ID + reminderId.toInt()

    fun refillNotificationId(reminderId: Long): Int = REFILL_BASE_ID + reminderId.toInt()
}
