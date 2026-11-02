package tech.gomeslab.superapp.feature.alarm.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    // Bitmask de dias: bit (Calendar.DAY_OF_WEEK - 1). 0 = alarme único (não repete).
    val daysOfWeek: Int,
    val soundUri: String?,
    val vibrate: Boolean,
    val snoozeMinutes: Int,
    @ColumnInfo(defaultValue = "0") val gradualVolumeSeconds: Int = 0,
    val createdAt: Long,
)
