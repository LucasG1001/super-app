package tech.gomeslab.superapp.feature.workout.command

import tech.gomeslab.superapp.core.command.Command
import tech.gomeslab.superapp.core.command.CommandHandler
import tech.gomeslab.superapp.core.command.CommandResult
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.BodyMeasurement
import tech.gomeslab.superapp.feature.workout.domain.WorkoutStats
import tech.gomeslab.superapp.feature.workout.domain.parseDecimal
import tech.gomeslab.superapp.feature.workout.ui.durationLabel
import tech.gomeslab.superapp.feature.workout.ui.volumeLabel
import tech.gomeslab.superapp.feature.workout.ui.weightLabel
import java.time.LocalDate
import javax.inject.Inject

// log_weight → payload "weightKg" (aceita vírgula); last_workout → sem payload.
class WorkoutCommandHandler @Inject constructor(
    private val repository: WorkoutRepository,
) : CommandHandler {

    override val namespace: String = "workout"

    override suspend fun handle(command: Command): CommandResult = when (command.action) {
        "log_weight" -> logWeight(command)
        "last_workout" -> lastWorkout()
        else -> CommandResult.Failure("Ação de treino desconhecida: ${command.action}")
    }

    private suspend fun logWeight(command: Command): CommandResult {
        val weight = command.payload["weightKg"]?.let(::parseDecimal)?.takeIf { it > 0 }
            ?: return CommandResult.Failure("Peso inválido.")
        val today = LocalDate.now()
        val existing = repository.getMeasurement(today) ?: BodyMeasurement(date = today)
        repository.saveMeasurement(existing.copy(weightKg = weight))
        return CommandResult.Success("Peso registrado: ${weightLabel(weight)}.")
    }

    private suspend fun lastWorkout(): CommandResult {
        val last = repository.lastFinishedWorkout() ?: return CommandResult.Failure("Nenhum treino finalizado ainda.")
        val totals = WorkoutStats.totals(last)
        return CommandResult.Success(
            "${last.workout.name}: ${durationLabel(totals.durationMillis)}, ${totals.sets} séries, ${volumeLabel(totals.volumeKg)}.",
        )
    }
}
