package tech.gomeslab.superapp.feature.alarm.command

import tech.gomeslab.superapp.core.command.Command
import tech.gomeslab.superapp.core.command.CommandHandler
import tech.gomeslab.superapp.core.command.CommandResult
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import javax.inject.Inject

// Executa comandos do namespace "alarm". Hoje acionado localmente pelo CommandDispatcher;
// na fase 2 será a porta de entrada do backend via FCM para controlar o despertador.
class AlarmCommandHandler @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : CommandHandler {

    override val namespace: String = "alarm"

    override suspend fun handle(command: Command): CommandResult = when (command.action) {
        "create" -> create(command)
        "toggle" -> toggle(command)
        "delete" -> delete(command)
        "trigger" -> trigger(command)
        else -> CommandResult.Failure("Ação de alarme desconhecida: ${command.action}")
    }

    private suspend fun create(command: Command): CommandResult {
        val hour = command.payload["hour"]?.toIntOrNull()
        val minute = command.payload["minute"]?.toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return CommandResult.Failure("Horário inválido para o alarme.")
        }
        val alarm = Alarm(
            hour = hour,
            minute = minute,
            label = command.payload["label"].orEmpty(),
            daysOfWeek = command.payload["daysOfWeek"]?.toIntOrNull() ?: 0,
            snoozeMinutes = command.payload["snoozeMinutes"]?.toIntOrNull() ?: 5,
            vibrate = command.payload["vibrate"]?.toBooleanStrictOrNull() ?: true,
            enabled = true,
            createdAt = System.currentTimeMillis(),
        )
        val id = repository.upsert(alarm)
        scheduler.schedule(alarm.copy(id = id))
        return CommandResult.Success("Alarme criado.")
    }

    private suspend fun toggle(command: Command): CommandResult {
        val id = command.payload["id"]?.toLongOrNull()
            ?: return CommandResult.Failure("Id do alarme ausente.")
        val enabled = command.payload["enabled"]?.toBooleanStrictOrNull() ?: true
        val alarm = repository.getById(id) ?: return CommandResult.Failure("Alarme não encontrado.")
        repository.setEnabled(id, enabled)
        val updated = alarm.copy(enabled = enabled)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(id)
        return CommandResult.Success(if (enabled) "Alarme ativado." else "Alarme desativado.")
    }

    private suspend fun delete(command: Command): CommandResult {
        val id = command.payload["id"]?.toLongOrNull()
            ?: return CommandResult.Failure("Id do alarme ausente.")
        scheduler.cancel(id)
        repository.delete(id)
        return CommandResult.Success("Alarme removido.")
    }

    private suspend fun trigger(command: Command): CommandResult {
        val id = command.payload["id"]?.toLongOrNull()
            ?: return CommandResult.Failure("Id do alarme ausente.")
        repository.getById(id) ?: return CommandResult.Failure("Alarme não encontrado.")
        scheduler.triggerNow(id)
        return CommandResult.Success("Alarme disparado.")
    }
}
