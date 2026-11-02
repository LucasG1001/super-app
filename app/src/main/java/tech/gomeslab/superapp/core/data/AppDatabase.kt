package tech.gomeslab.superapp.core.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import tech.gomeslab.superapp.feature.alarm.data.AlarmDao
import tech.gomeslab.superapp.feature.alarm.data.AlarmEntity
import tech.gomeslab.superapp.feature.reminder.data.ReminderDao
import tech.gomeslab.superapp.feature.reminder.data.ReminderEntity
import tech.gomeslab.superapp.feature.reminder.data.ReminderLogEntity
import tech.gomeslab.superapp.feature.workout.data.BodyMeasurementEntity
import tech.gomeslab.superapp.feature.workout.data.ExerciseEntity
import tech.gomeslab.superapp.feature.workout.data.RoutineEntity
import tech.gomeslab.superapp.feature.workout.data.RoutineExerciseEntity
import tech.gomeslab.superapp.feature.workout.data.WorkoutDao
import tech.gomeslab.superapp.feature.workout.data.WorkoutEntity
import tech.gomeslab.superapp.feature.workout.data.WorkoutSetEntity

@Database(
    entities = [
        AlarmEntity::class,
        ReminderEntity::class,
        ReminderLogEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun reminderDao(): ReminderDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "superapp.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN gradualVolumeSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
