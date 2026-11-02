package tech.gomeslab.superapp.feature.reminder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val title: String,
    val note: String,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    // Minutos do dia separados por vírgula ("480,840" = 08:00 e 14:00).
    val timesCsv: String,
    val repeat: String,
    val daysOfWeek: Int,
    val leadMinutes: Int,
    val dose: String,
    val stock: Int?,
    val refillThreshold: Int?,
    val enabled: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "reminder_logs",
    indices = [Index(value = ["reminderId", "occurrenceAt"], unique = true)],
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val occurrenceAt: Long,
    val status: String,
    val actedAt: Long,
)
