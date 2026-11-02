package tech.gomeslab.superapp.feature.workout.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val kind: String,
    val restSeconds: Int,
    val archived: Boolean,
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
    val note: String,
)

@Entity(tableName = "routine_exercises", indices = [Index("routineId")])
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val setCount: Int,
    val targetReps: String,
    val note: String,
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long?,
    val name: String,
    val startedAt: Long,
    // null = treino em andamento (só existe um por vez).
    val finishedAt: Long?,
    val note: String,
)

@Entity(tableName = "workout_sets", indices = [Index("workoutId"), Index("exerciseId")])
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseOrder: Int,
    val setIndex: Int,
    val type: String,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val completed: Boolean,
    val completedAt: Long?,
)

@Entity(tableName = "body_measurements", indices = [Index(value = ["epochDay"], unique = true)])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val neck: Double?,
    val shoulders: Double?,
    val chest: Double?,
    val waist: Double?,
    val abdomen: Double?,
    val hips: Double?,
    val leftArm: Double?,
    val rightArm: Double?,
    val leftForearm: Double?,
    val rightForearm: Double?,
    val leftThigh: Double?,
    val rightThigh: Double?,
    val leftCalf: Double?,
    val rightCalf: Double?,
)
