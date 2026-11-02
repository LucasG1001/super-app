package tech.gomeslab.superapp.feature.workout.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tech.gomeslab.superapp.core.data.AppDatabase
import tech.gomeslab.superapp.feature.workout.domain.BodyMeasurement
import tech.gomeslab.superapp.feature.workout.domain.Circumference
import tech.gomeslab.superapp.feature.workout.domain.Equipment
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.ExerciseKind
import tech.gomeslab.superapp.feature.workout.domain.MuscleGroup
import tech.gomeslab.superapp.feature.workout.domain.Routine
import tech.gomeslab.superapp.feature.workout.domain.RoutineExercise
import tech.gomeslab.superapp.feature.workout.domain.SetType
import tech.gomeslab.superapp.feature.workout.domain.Workout
import tech.gomeslab.superapp.feature.workout.domain.WorkoutSet
import tech.gomeslab.superapp.feature.workout.domain.WorkoutWithSets
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val dao = db.workoutDao()

    val exercises: Flow<List<Exercise>> = dao.observeExercises().map { list -> list.map { it.toDomain() } }

    val exercisesById: Flow<Map<Long, Exercise>> =
        dao.observeAllExercises().map { list -> list.associate { it.id to it.toDomain() } }

    val routines: Flow<List<Routine>> = combine(
        dao.observeRoutines(),
        dao.observeRoutineExercises(),
        exercisesById,
    ) { routines, items, exercises ->
        val byRoutine = items.groupBy { it.routineId }
        routines.map { routine ->
            routine.toDomain(byRoutine[routine.id].orEmpty().mapNotNull { it.toDomain(exercises) })
        }
    }

    val activeWorkout: Flow<Workout?> = dao.observeActiveWorkout().map { it?.toDomain() }

    /** Treinos finalizados (mais recente primeiro) com as séries concluídas. */
    val history: Flow<List<WorkoutWithSets>> = combine(
        dao.observeFinishedWorkouts(),
        dao.observeFinishedSets(),
    ) { workouts, sets ->
        val byWorkout = sets.groupBy { it.workoutId }
        workouts.map { w -> WorkoutWithSets(w.toDomain(), byWorkout[w.id].orEmpty().map { it.toDomain() }) }
    }

    fun observeWorkout(id: Long): Flow<WorkoutWithSets?> = combine(
        dao.observeWorkout(id),
        dao.observeSets(id),
    ) { workout, sets -> workout?.let { WorkoutWithSets(it.toDomain(), sets.map { s -> s.toDomain() }) } }

    suspend fun getExercise(id: Long): Exercise? = dao.getExercise(id)?.toDomain()

    suspend fun saveExercise(exercise: Exercise): Long =
        if (exercise.id == 0L) dao.insertExercise(exercise.toEntity()) else {
            dao.updateExercise(exercise.toEntity()); exercise.id
        }

    suspend fun removeExercise(id: Long) = db.withTransaction {
        if (dao.exerciseUsage(id) == 0) dao.deleteExercise(id) else dao.archiveExercise(id)
    }

    suspend fun getRoutine(id: Long): Routine? {
        val routine = dao.getRoutine(id) ?: return null
        val exercises = dao.getRoutineExercises(id).mapNotNull { item ->
            dao.getExercise(item.exerciseId)?.let { item.toDomain(mapOf(it.id to it.toDomain())) }
        }
        return routine.toDomain(exercises)
    }

    suspend fun saveRoutine(routine: Routine): Long = db.withTransaction {
        val id = if (routine.id == 0L) {
            dao.insertRoutine(routine.copy(position = dao.maxRoutinePosition() + 1).toEntity())
        } else {
            dao.updateRoutine(routine.toEntity())
            routine.id
        }
        dao.deleteRoutineExercises(id)
        dao.insertRoutineExercises(
            routine.exercises.mapIndexed { index, item ->
                RoutineExerciseEntity(
                    routineId = id,
                    exerciseId = item.exercise.id,
                    position = index,
                    setCount = item.setCount,
                    targetReps = item.targetReps,
                    note = item.note,
                )
            },
        )
        id
    }

    suspend fun deleteRoutine(id: Long) = db.withTransaction {
        dao.deleteRoutineExercises(id)
        dao.deleteRoutine(id)
    }

    suspend fun getActiveWorkout(): Workout? = dao.getActiveWorkout()?.toDomain()

    suspend fun lastFinishedWorkout(): WorkoutWithSets? {
        val workout = dao.getLastFinishedWorkout() ?: return null
        return WorkoutWithSets(workout.toDomain(), dao.getSets(workout.id).map { it.toDomain() })
    }

    /** Começa um treino (da ficha ou livre). Se já houver um em andamento, devolve ele. */
    suspend fun startWorkout(routine: Routine?): Long = db.withTransaction {
        dao.getActiveWorkout()?.let { return@withTransaction it.id }
        val workoutId = dao.insertWorkout(
            WorkoutEntity(
                routineId = routine?.id,
                name = routine?.name ?: "Treino livre",
                startedAt = System.currentTimeMillis(),
                finishedAt = null,
                note = "",
            ),
        )
        val sets = routine?.exercises.orEmpty().flatMapIndexed { order, item ->
            (0 until item.setCount.coerceAtLeast(1)).map { index ->
                WorkoutSet(workoutId = workoutId, exerciseId = item.exercise.id, exerciseOrder = order, setIndex = index)
                    .toEntity()
            }
        }
        if (sets.isNotEmpty()) dao.insertSets(sets)
        workoutId
    }

    suspend fun addExercise(workoutId: Long, exerciseId: Long) {
        val nextOrder = (dao.getSets(workoutId).maxOfOrNull { it.exerciseOrder } ?: -1) + 1
        dao.insertSet(WorkoutSet(workoutId = workoutId, exerciseId = exerciseId, exerciseOrder = nextOrder, setIndex = 0).toEntity())
    }

    suspend fun addSet(workoutId: Long, exerciseOrder: Int, exerciseId: Long) {
        val sets = dao.getSets(workoutId).filter { it.exerciseOrder == exerciseOrder }
        dao.insertSet(
            WorkoutSet(
                workoutId = workoutId,
                exerciseId = exerciseId,
                exerciseOrder = exerciseOrder,
                setIndex = (sets.maxOfOrNull { it.setIndex } ?: -1) + 1,
            ).toEntity(),
        )
    }

    suspend fun updateSet(set: WorkoutSet) = dao.updateSet(set.toEntity())

    suspend fun updateWeight(setId: Long, weightKg: Double?) = dao.updateWeight(setId, weightKg)

    suspend fun updateReps(setId: Long, reps: Int?) = dao.updateReps(setId, reps)

    suspend fun updateDuration(setId: Long, durationSec: Int?) = dao.updateDuration(setId, durationSec)

    suspend fun updateType(setId: Long, type: SetType) = dao.updateType(setId, type.name)

    suspend fun deleteSet(id: Long) = dao.deleteSet(id)

    suspend fun removeExercise(workoutId: Long, exerciseOrder: Int) = dao.deleteExerciseSets(workoutId, exerciseOrder)

    /** Finaliza descartando séries não marcadas. Devolve false se não sobrou nenhuma série. */
    suspend fun finish(workoutId: Long): Boolean = db.withTransaction {
        val workout = dao.getWorkout(workoutId) ?: return@withTransaction false
        if (dao.getSets(workoutId).none { it.completed }) return@withTransaction false
        dao.deleteIncompleteSets(workoutId)
        dao.updateWorkout(workout.copy(finishedAt = System.currentTimeMillis()))
        true
    }

    suspend fun deleteWorkout(workoutId: Long) = db.withTransaction {
        dao.deleteSets(workoutId)
        dao.deleteWorkout(workoutId)
    }

    val measurements: Flow<List<BodyMeasurement>> = dao.observeMeasurements().map { list -> list.map { it.toDomain() } }

    suspend fun getMeasurement(date: LocalDate): BodyMeasurement? = dao.getMeasurement(date.toEpochDay())?.toDomain()

    suspend fun saveMeasurement(measurement: BodyMeasurement): Long {
        val existing = dao.getMeasurement(measurement.date.toEpochDay())
        return dao.upsertMeasurement(measurement.copy(id = existing?.id ?: 0).toEntity())
    }

    suspend fun deleteMeasurement(date: LocalDate) = dao.deleteMeasurement(date.toEpochDay())
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = enumOr(muscleGroup, MuscleGroup.OTHER),
    equipment = enumOr(equipment, Equipment.OTHER),
    kind = enumOr(kind, ExerciseKind.WEIGHT_REPS),
    restSeconds = restSeconds,
    archived = archived,
)

private fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    equipment = equipment.name,
    kind = kind.name,
    restSeconds = restSeconds,
    archived = archived,
)

private fun RoutineEntity.toDomain(exercises: List<RoutineExercise>) =
    Routine(id = id, name = name, position = position, note = note, exercises = exercises)

private fun Routine.toEntity() = RoutineEntity(id = id, name = name, position = position, note = note)

private fun RoutineExerciseEntity.toDomain(exercises: Map<Long, Exercise>): RoutineExercise? {
    val exercise = exercises[exerciseId] ?: return null
    return RoutineExercise(
        id = id,
        exercise = exercise,
        position = position,
        setCount = setCount,
        targetReps = targetReps,
        note = note,
    )
}

private fun WorkoutEntity.toDomain() = Workout(
    id = id,
    routineId = routineId,
    name = name,
    startedAt = startedAt,
    finishedAt = finishedAt,
    note = note,
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    exerciseOrder = exerciseOrder,
    setIndex = setIndex,
    type = enumOr(type, SetType.NORMAL),
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    completed = completed,
    completedAt = completedAt,
)

private fun WorkoutSet.toEntity() = WorkoutSetEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    exerciseOrder = exerciseOrder,
    setIndex = setIndex,
    type = type.name,
    weightKg = weightKg,
    reps = reps,
    durationSec = durationSec,
    completed = completed,
    completedAt = completedAt,
)

private fun BodyMeasurementEntity.toDomain(): BodyMeasurement {
    val values = mapOf(
        Circumference.NECK to neck,
        Circumference.SHOULDERS to shoulders,
        Circumference.CHEST to chest,
        Circumference.WAIST to waist,
        Circumference.ABDOMEN to abdomen,
        Circumference.HIPS to hips,
        Circumference.LEFT_ARM to leftArm,
        Circumference.RIGHT_ARM to rightArm,
        Circumference.LEFT_FOREARM to leftForearm,
        Circumference.RIGHT_FOREARM to rightForearm,
        Circumference.LEFT_THIGH to leftThigh,
        Circumference.RIGHT_THIGH to rightThigh,
        Circumference.LEFT_CALF to leftCalf,
        Circumference.RIGHT_CALF to rightCalf,
    )
    return BodyMeasurement(
        id = id,
        date = LocalDate.ofEpochDay(epochDay),
        weightKg = weightKg,
        bodyFatPct = bodyFatPct,
        circumferences = values.mapNotNull { (key, value) -> value?.let { key to it } }.toMap(),
    )
}

private fun BodyMeasurement.toEntity(): BodyMeasurementEntity {
    val c = circumferences
    return BodyMeasurementEntity(
        id = id,
        epochDay = date.toEpochDay(),
        weightKg = weightKg,
        bodyFatPct = bodyFatPct,
        neck = c[Circumference.NECK],
        shoulders = c[Circumference.SHOULDERS],
        chest = c[Circumference.CHEST],
        waist = c[Circumference.WAIST],
        abdomen = c[Circumference.ABDOMEN],
        hips = c[Circumference.HIPS],
        leftArm = c[Circumference.LEFT_ARM],
        rightArm = c[Circumference.RIGHT_ARM],
        leftForearm = c[Circumference.LEFT_FOREARM],
        rightForearm = c[Circumference.RIGHT_FOREARM],
        leftThigh = c[Circumference.LEFT_THIGH],
        rightThigh = c[Circumference.RIGHT_THIGH],
        leftCalf = c[Circumference.LEFT_CALF],
        rightCalf = c[Circumference.RIGHT_CALF],
    )
}

private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
