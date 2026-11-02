package tech.gomeslab.superapp.feature.reminder.command

import tech.gomeslab.superapp.core.command.Command
import tech.gomeslab.superapp.core.command.CommandHandler
import tech.gomeslab.superapp.core.command.CommandResult
import tech.gomeslab.superapp.feature.reminder.ReminderActions
import tech.gomeslab.superapp.feature.reminder.ReminderScheduler
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

// Payload (strings, compatível com FCM): create → kind (TASK|MEDICINE|EVENT), title, date (yyyy-MM-dd),
// time (HH:mm), note, leadMinutes; complete/delete → id.
class ReminderCommandHandler @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val actions: ReminderActions,
) : CommandHandler {

    override val namespace: String = "reminder"

    override suspend fun handle(command: Command): CommandResult = when (command.action) {
        "create" -> create(command)
        "complete" -> complete(command)
        "delete" -> delete(command)
        else -> CommandResult.Failure("Ação de lembrete desconhecida: ${command.action}")
    }

    private suspend fun create(command: Command): CommandResult {
        val title = command.payload["title"]?.trim().orEmpty()
        if (title.isBlank()) return CommandResult.Failure("Lembrete sem título.")
        val kind = command.payload["kind"]?.uppercase()
            ?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }
            ?: ReminderKind.TASK
        val date = command.payload["date"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        val time = command.payload["time"]?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: return CommandResult.Failure("Horário inválido para o lembrete.")
        val reminder = Reminder(
            kind = kind,
            title = title,
            note = command.payload["note"].orEmpty(),
            startDate = date,
            times = listOf(time),
            leadMinutes = command.payload["leadMinutes"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            createdAt = System.currentTimeMillis(),
        )
        val id = repository.upsert(reminder)
        scheduler.scheduleNext(reminder.copy(id = id))
        return CommandResult.Success("Lembrete criado.")
    }

    private suspend fun complete(command: Command): CommandResult {
        val reminder = command.payload["id"]?.toLongOrNull()?.let { repository.getById(it) }
            ?: return CommandResult.Failure("Lembrete não encontrado.")
        val now = LocalDateTime.now()
        val handled = repository.handledOccurrences(reminder.id)
        val occurrence = ReminderSchedule.occurrencesOn(now.toLocalDate(), reminder)
            .lastOrNull { !it.isAfter(now) && it !in handled }
            ?: ReminderSchedule.nextOccurrence(now, reminder, handled)
            ?: return CommandResult.Failure("Nada pendente para este lembrete.")
        val status = if (reminder.kind == ReminderKind.MEDICINE) LogStatus.TAKEN else LogStatus.DONE
        actions.record(reminder.id, occurrence, status)
        return CommandResult.Success("Lembrete concluído.")
    }

    private suspend fun delete(command: Command): CommandResult {
        val id = command.payload["id"]?.toLongOrNull()
            ?: return CommandResult.Failure("Id do lembrete inválido.")
        if (repository.getById(id) == null) return CommandResult.Failure("Lembrete não encontrado.")
        actions.delete(id)
        return CommandResult.Success("Lembrete removido.")
    }
}
