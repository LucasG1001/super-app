package tech.gomeslab.superapp.feature.workout.domain

import java.time.LocalDate

enum class MuscleGroup(val label: String) {
    CHEST("Peito"),
    BACK("Costas"),
    SHOULDERS("Ombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    LEGS("Pernas"),
    GLUTES("Glúteos"),
    CALVES("Panturrilha"),
    ABS("Abdômen"),
    FOREARMS("Antebraço"),
    CARDIO("Cardio"),
    OTHER("Outro"),
}

enum class Equipment(val label: String) {
    BARBELL("Barra"),
    DUMBBELL("Halter"),
    MACHINE("Máquina"),
    CABLE("Cabo"),
    BODYWEIGHT("Peso corporal"),
    OTHER("Outro"),
}

enum class ExerciseKind(val label: String) {
    WEIGHT_REPS("Carga e repetições"),
    REPS("Só repetições"),
    DURATION("Tempo"),
}

enum class SetType(val label: String, val short: String) {
    NORMAL("Normal", ""),
    WARMUP("Aquecimento", "A"),
    DROP("Drop set", "D"),
    FAILURE("Até a falha", "F"),
}

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment = Equipment.OTHER,
    val kind: ExerciseKind = ExerciseKind.WEIGHT_REPS,
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val archived: Boolean = false,
) {
    companion object {
        const val DEFAULT_REST_SECONDS = 90
    }
}

data class RoutineExercise(
    val id: Long = 0,
    val exercise: Exercise,
    val position: Int,
    val setCount: Int = 3,
    val targetReps: String = "",
    val note: String = "",
)

data class Routine(
    val id: Long = 0,
    val name: String,
    val position: Int = 0,
    val note: String = "",
    val exercises: List<RoutineExercise> = emptyList(),
)

data class WorkoutSet(
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseOrder: Int,
    val setIndex: Int,
    val type: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
) {
    val countsForStats: Boolean get() = completed && type != SetType.WARMUP
    val volume: Double get() = (weightKg ?: 0.0) * (reps ?: 0)
}

data class Workout(
    val id: Long = 0,
    val routineId: Long? = null,
    val name: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val note: String = "",
) {
    val isActive: Boolean get() = finishedAt == null
}

/** Treino com séries — usado no histórico, nos recordes e no "anterior". */
data class WorkoutWithSets(val workout: Workout, val sets: List<WorkoutSet>)

enum class Circumference(val label: String) {
    NECK("Pescoço"),
    SHOULDERS("Ombros"),
    CHEST("Peito"),
    WAIST("Cintura"),
    ABDOMEN("Abdômen"),
    HIPS("Quadril"),
    LEFT_ARM("Braço esquerdo"),
    RIGHT_ARM("Braço direito"),
    LEFT_FOREARM("Antebraço esquerdo"),
    RIGHT_FOREARM("Antebraço direito"),
    LEFT_THIGH("Coxa esquerda"),
    RIGHT_THIGH("Coxa direita"),
    LEFT_CALF("Panturrilha esquerda"),
    RIGHT_CALF("Panturrilha direita"),
}

data class BodyMeasurement(
    val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val circumferences: Map<Circumference, Double> = emptyMap(),
)
