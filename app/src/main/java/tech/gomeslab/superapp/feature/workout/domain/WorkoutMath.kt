package tech.gomeslab.superapp.feature.workout.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object OneRepMax {
    /** Fórmula de Epley. Com 1 repetição, o próprio peso. */
    fun epley(weightKg: Double, reps: Int): Double = when {
        reps <= 0 || weightKg <= 0.0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }
}

data class WorkoutTotals(val volumeKg: Double, val sets: Int, val durationMillis: Long)

object WorkoutStats {
    fun totals(workout: WorkoutWithSets, now: Long = System.currentTimeMillis()): WorkoutTotals {
        val counted = workout.sets.filter { it.countsForStats }
        return WorkoutTotals(
            volumeKg = counted.sumOf { it.volume },
            sets = counted.size,
            durationMillis = (workout.workout.finishedAt ?: now) - workout.workout.startedAt,
        )
    }
}

enum class RecordType(val label: String) {
    HEAVIEST_WEIGHT("Maior carga"),
    BEST_ONE_REP_MAX("Melhor 1RM estimado"),
    BEST_SET_VOLUME("Melhor série (volume)"),
    MOST_REPS("Mais repetições"),
}

data class PersonalRecord(val exerciseId: Long, val type: RecordType, val value: Double)

data class ExerciseBests(
    val heaviestWeight: Double = 0.0,
    val bestOneRepMax: Double = 0.0,
    val bestSetVolume: Double = 0.0,
    val mostReps: Int = 0,
) {
    fun value(type: RecordType): Double = when (type) {
        RecordType.HEAVIEST_WEIGHT -> heaviestWeight
        RecordType.BEST_ONE_REP_MAX -> bestOneRepMax
        RecordType.BEST_SET_VOLUME -> bestSetVolume
        RecordType.MOST_REPS -> mostReps.toDouble()
    }
}

object PersonalRecords {

    fun bests(sets: List<WorkoutSet>): ExerciseBests {
        val counted = sets.filter { it.countsForStats }
        return ExerciseBests(
            heaviestWeight = counted.maxOfOrNull { it.weightKg ?: 0.0 } ?: 0.0,
            bestOneRepMax = counted.maxOfOrNull { OneRepMax.epley(it.weightKg ?: 0.0, it.reps ?: 0) } ?: 0.0,
            bestSetVolume = counted.maxOfOrNull { it.volume } ?: 0.0,
            mostReps = counted.maxOfOrNull { it.reps ?: 0 } ?: 0,
        )
    }

    /**
     * Recordes batidos em [workout] comparando com os treinos anteriores a ele. A primeira vez que
     * um exercício é feito não gera recorde (não há com o que comparar).
     */
    fun detect(workout: WorkoutWithSets, history: List<WorkoutWithSets>): List<PersonalRecord> {
        val earlier = history.filter {
            it.workout.id != workout.workout.id && it.workout.startedAt < workout.workout.startedAt
        }
        return workout.sets.filter { it.countsForStats }.groupBy { it.exerciseId }.flatMap { (exerciseId, sets) ->
            val previous = earlier.flatMap { w -> w.sets.filter { it.exerciseId == exerciseId } }
            if (previous.none { it.countsForStats }) return@flatMap emptyList()
            val before = bests(previous)
            val now = bests(sets)
            RecordType.entries.mapNotNull { type ->
                val value = now.value(type)
                if (value > 0.0 && value > before.value(type)) PersonalRecord(exerciseId, type, value) else null
            }
        }
    }
}

object PreviousSets {
    /** Séries concluídas do exercício na sessão finalizada mais recente (anterior a [beforeMillis]). */
    fun forExercise(exerciseId: Long, history: List<WorkoutWithSets>, beforeMillis: Long = Long.MAX_VALUE): List<WorkoutSet> =
        history
            .filter { it.workout.startedAt < beforeMillis }
            .sortedByDescending { it.workout.startedAt }
            .firstNotNullOfOrNull { w ->
                w.sets.filter { it.exerciseId == exerciseId && it.completed }
                    .sortedBy { it.setIndex }
                    .takeIf { it.isNotEmpty() }
            }
            .orEmpty()
}

object RoutineRotation {
    /** Próxima ficha da sequência (A→B→C→A). Sem histórico (ou ficha removida), a primeira. */
    fun next(routines: List<Routine>, lastRoutineId: Long?): Routine? {
        val ordered = routines.sortedWith(compareBy({ it.position }, { it.id }))
        if (ordered.isEmpty()) return null
        val index = ordered.indexOfFirst { it.id == lastRoutineId }
        return if (index == -1) ordered.first() else ordered[(index + 1) % ordered.size]
    }
}

object WeekSummary {
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    fun trainedDays(today: LocalDate, workouts: List<Workout>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> {
        val start = weekStart(today)
        val end = start.plusDays(6)
        return workouts
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .filter { !it.isBefore(start) && !it.isAfter(end) }
            .toSet()
    }
}

fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun formatDecimal(value: Double): String {
    val rounded = Math.round(value * 100) / 100.0
    val text = if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    return text.replace('.', ',')
}
