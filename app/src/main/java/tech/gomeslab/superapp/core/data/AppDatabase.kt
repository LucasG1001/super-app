package tech.gomeslab.superapp.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import tech.gomeslab.superapp.feature.alarm.data.AlarmDao
import tech.gomeslab.superapp.feature.alarm.data.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val NAME = "superapp.db"
    }
}
