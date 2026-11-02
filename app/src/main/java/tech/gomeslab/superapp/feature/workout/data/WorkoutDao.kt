package tech.gomeslab.superapp.feature.workout.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    fun observeAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): ExerciseEntity?

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Query("SELECT (SELECT COUNT(*) FROM workout_sets WHERE exerciseId = :id) + (SELECT COUNT(*) FROM routine_exercises WHERE exerciseId = :id)")
    suspend fun exerciseUsage(id: Long): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long): Int

    @Query("UPDATE exercises SET archived = 1 WHERE id = :id")
    suspend fun archiveExercise(id: Long): Int

    @Query("SELECT * FROM routines ORDER BY position, id")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_exercises ORDER BY routineId, position")
    fun observeRoutineExercises(): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: Long): RoutineEntity?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    suspend fun getRoutineExercises(routineId: Long): List<RoutineExerciseEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM routines")
    suspend fun maxRoutinePosition(): Int

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity): Int

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long): Int

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long): Int

    @Insert
    suspend fun insertRoutineExercises(items: List<RoutineExerciseEntity>): List<Long>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinishedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NULL LIMIT 1")
    suspend fun getActiveWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastFinishedWorkout(): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity): Int

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long): Int

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseOrder, setIndex")
    fun observeSets(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseOrder, setIndex")
    suspend fun getSets(workoutId: Long): List<WorkoutSetEntity>

    @Query(
        "SELECT s.* FROM workout_sets s INNER JOIN workouts w ON w.id = s.workoutId " +
            "WHERE w.finishedAt IS NOT NULL AND s.completed = 1 ORDER BY w.startedAt, s.exerciseOrder, s.setIndex",
    )
    fun observeFinishedSets(): Flow<List<WorkoutSetEntity>>

    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>): List<Long>

    @Update
    suspend fun updateSet(set: WorkoutSetEntity): Int

    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE id = :id")
    suspend fun updateWeight(id: Long, weightKg: Double?): Int

    @Query("UPDATE workout_sets SET reps = :reps WHERE id = :id")
    suspend fun updateReps(id: Long, reps: Int?): Int

    @Query("UPDATE workout_sets SET durationSec = :durationSec WHERE id = :id")
    suspend fun updateDuration(id: Long, durationSec: Int?): Int

    @Query("UPDATE workout_sets SET type = :type WHERE id = :id")
    suspend fun updateType(id: Long, type: String): Int

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long): Int

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseOrder = :exerciseOrder")
    suspend fun deleteExerciseSets(workoutId: Long, exerciseOrder: Int): Int

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND completed = 0")
    suspend fun deleteIncompleteSets(workoutId: Long): Int

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun deleteSets(workoutId: Long): Int

    @Query("SELECT * FROM body_measurements ORDER BY epochDay DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE epochDay = :epochDay")
    suspend fun getMeasurement(epochDay: Long): BodyMeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity): Long

    @Query("DELETE FROM body_measurements WHERE epochDay = :epochDay")
    suspend fun deleteMeasurement(epochDay: Long): Int
}
