package tech.gomeslab.superapp.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.gomeslab.superapp.feature.alarm.data.AlarmDao
import tech.gomeslab.superapp.feature.alarm.data.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val NAME = "superapp.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN gradualVolumeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
