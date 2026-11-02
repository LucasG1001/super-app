package tech.gomeslab.superapp.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.MuscleGroup
import tech.gomeslab.superapp.feature.workout.domain.OneRepMax
import tech.gomeslab.superapp.feature.workout.domain.PersonalRecords
import tech.gomeslab.superapp.feature.workout.domain.PreviousSets
import tech.gomeslab.superapp.feature.workout.domain.RecordType
import tech.gomeslab.superapp.feature.workout.domain.Routine
import tech.gomeslab.superapp.feature.workout.domain.RoutineRotation
import tech.gomeslab.superapp.feature.workout.domain.SetType
import tech.gomeslab.superapp.feature.workout.domain.Workout
import tech.gomeslab.superapp.feature.workout.domain.WorkoutSet
import tech.gomeslab.superapp.feature.workout.domain.WorkoutStats
import tech.gomeslab.superapp.feature.workout.domain.WorkoutWithSets
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import tech.gomeslab.superapp.feature.workout.domain.parseDecimal

class WorkoutMathTest {

    private val bench = 1L
    private val squat = 2L

    private fun set(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        weight: Double,
        reps: Int,
        type: SetType = SetType.NORMAL,
        completed: Boolean = true,
    ) = WorkoutSet(
        workoutId = workoutId,
        exerciseId = exerciseId,
        exerciseOrder = 0,
        setIndex = index,
        type = type,
        weightKg = weight,
        reps = reps,
        completed = completed,
    )

    private fun workout(id: Long, startedAt: Long, vararg sets: WorkoutSet) =
        WorkoutWithSets(Workout(id = id, name = "Treino", startedAt = startedAt, finishedAt = startedAt + 3_600_000), sets.toList())

    @Test
    fun `epley one rep max`() {
        assertEquals(100.0, OneRepMax.epley(100.0, 1), 0.001)
        assertEquals(133.33, OneRepMax.epley(100.0, 10), 0.01)
        assertEquals(0.0, OneRepMax.epley(0.0, 10), 0.0)
    }

    @Test
    fun `volume ignores warmup and incomplete sets`() {
        val w = workout(
            1, 0,
            set(1, bench, 0, 40.0, 10, SetType.WARMUP),
            set(1, bench, 1, 60.0, 10),
            set(1, bench, 2, 60.0, 8),
            set(1, bench, 3, 60.0, 8, completed = false),
        )
        val totals = WorkoutStats.totals(w)
        assertEquals(1080.0, totals.volumeKg, 0.001)
        assertEquals(2, totals.sets)
    }

    @Test
    fun `records are detected against earlier workouts only`() {
        val first = workout(1, 1_000, set(1, bench, 0, 60.0, 10))
        val second = workout(2, 2_000, set(2, bench, 0, 65.0, 8), set(2, bench, 1, 50.0, 15))
        val records = PersonalRecords.detect(second, listOf(first, second))
        val types = records.map { it.type }.toSet()
        assertTrue(RecordType.HEAVIEST_WEIGHT in types)
        assertTrue(RecordType.BEST_ONE_REP_MAX in types)
        assertTrue(RecordType.MOST_REPS in types)
        assertTrue(RecordType.BEST_SET_VOLUME in types)
    }

    @Test
    fun `first time doing an exercise is not a record and warmups never count`() {
        val first = workout(1, 1_000, set(1, bench, 0, 60.0, 10))
        val second = workout(2, 2_000, set(2, squat, 0, 100.0, 5), set(2, bench, 0, 80.0, 10, SetType.WARMUP))
        assertTrue(PersonalRecords.detect(second, listOf(first, second)).isEmpty())
    }

    @Test
    fun `previous sets come from the most recent session with that exercise`() {
        val old = workout(1, 1_000, set(1, bench, 0, 50.0, 10))
        val recent = workout(2, 2_000, set(2, bench, 1, 62.5, 8), set(2, bench, 0, 60.0, 10))
        val other = workout(3, 3_000, set(3, squat, 0, 100.0, 5))
        val previous = PreviousSets.forExercise(bench, listOf(other, old, recent))
        assertEquals(listOf(60.0, 62.5), previous.map { it.weightKg })
        assertEquals(emptyList<WorkoutSet>(), PreviousSets.forExercise(bench, listOf(recent), beforeMillis = 2_000))
    }

    @Test
    fun `routine rotation cycles a b c`() {
        val a = Routine(id = 10, name = "A", position = 0)
        val b = Routine(id = 11, name = "B", position = 1)
        val c = Routine(id = 12, name = "C", position = 2)
        val routines = listOf(c, a, b)
        assertEquals("A", RoutineRotation.next(routines, null)?.name)
        assertEquals("B", RoutineRotation.next(routines, 10)?.name)
        assertEquals("A", RoutineRotation.next(routines, 12)?.name)
        assertEquals("A", RoutineRotation.next(routines, 999)?.name)
        assertNull(RoutineRotation.next(emptyList(), 10))
    }

    @Test
    fun `decimal parsing and formatting use comma`() {
        assertEquals(42.5, parseDecimal("42,5")!!, 0.0)
        assertEquals(42.5, parseDecimal(" 42.5 ")!!, 0.0)
        assertNull(parseDecimal("abc"))
        assertEquals("42,5", formatDecimal(42.5))
        assertEquals("60", formatDecimal(60.0))
    }

    @Test
    fun `exercise default rest`() {
        assertEquals(90, Exercise(name = "Supino", muscleGroup = MuscleGroup.CHEST).restSeconds)
    }
}
