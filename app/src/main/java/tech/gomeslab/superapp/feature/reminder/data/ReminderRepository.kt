package tech.gomeslab.superapp.feature.reminder.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.gomeslab.superapp.core.data.AppDatabase
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderLog
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val dao = db.reminderDao()

    val reminders: Flow<List<Reminder>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun logsSince(from: LocalDateTime): Flow<List<ReminderLog>> =
        dao.observeLogsSince(from.toMillis()).map { list -> list.map { it.toDomain() } }

    suspend fun getEnabled(): List<Reminder> = dao.getEnabled().map { it.toDomain() }

    suspend fun getById(id: Long): Reminder? = dao.getById(id)?.toDomain()

    suspend fun upsert(reminder: Reminder): Long =
        if (reminder.id == 0L) dao.insert(reminder.toEntity()) else {
            dao.update(reminder.toEntity()); reminder.id
        }

    suspend fun delete(id: Long) = db.withTransaction {
        dao.deleteLogsFor(id)
        dao.deleteById(id)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun handledOccurrences(reminderId: Long): Set<LocalDateTime> =
        dao.getLogsFor(reminderId).map { it.occurrenceAt.toLocalDateTime() }.toSet()

    suspend fun isHandled(reminderId: Long, occurrence: LocalDateTime): Boolean =
        dao.getLog(reminderId, occurrence.toMillis()) != null

    /** Registra a ocorrência; "tomei" baixa o estoque. Devolve o lembrete atualizado. */
    suspend fun record(reminderId: Long, occurrence: LocalDateTime, status: LogStatus): Reminder? =
        db.withTransaction {
            val reminder = dao.getById(reminderId)?.toDomain() ?: return@withTransaction null
            val previous = dao.getLog(reminderId, occurrence.toMillis())
            dao.upsertLog(
                ReminderLogEntity(
                    id = previous?.id ?: 0,
                    reminderId = reminderId,
                    occurrenceAt = occurrence.toMillis(),
                    status = status.name,
                    actedAt = System.currentTimeMillis(),
                ),
            )
            val wasTaken = previous?.status == LogStatus.TAKEN.name
            val isTaken = status == LogStatus.TAKEN
            val stock = reminder.stock
            val newStock = when {
                stock == null -> null
                isTaken && !wasTaken -> (stock - 1).coerceAtLeast(0)
                !isTaken && wasTaken -> stock + 1
                else -> stock
            }
            if (newStock != stock) dao.setStock(reminderId, newStock)
            reminder.copy(stock = newStock)
        }

    suspend fun undo(reminderId: Long, occurrence: LocalDateTime) = db.withTransaction {
        val previous = dao.getLog(reminderId, occurrence.toMillis()) ?: return@withTransaction
        dao.deleteLog(reminderId, occurrence.toMillis())
        if (previous.status == LogStatus.TAKEN.name) {
            val stock = dao.getById(reminderId)?.stock
            if (stock != null) dao.setStock(reminderId, stock + 1)
        }
    }
}

private val zone: ZoneId get() = ZoneId.systemDefault()

private fun LocalDateTime.toMillis(): Long = atZone(zone).toInstant().toEpochMilli()

private fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

private fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    kind = ReminderKind.valueOf(kind),
    title = title,
    note = note,
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = endEpochDay?.let(LocalDate::ofEpochDay),
    times = timesCsv.split(',').filter { it.isNotBlank() }.map { LocalTime.ofSecondOfDay(it.toLong() * 60) },
    repeat = ReminderRepeat.valueOf(repeat),
    daysOfWeek = daysOfWeek,
    leadMinutes = leadMinutes,
    dose = dose,
    stock = stock,
    refillThreshold = refillThreshold,
    enabled = enabled,
    createdAt = createdAt,
)

private fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    kind = kind.name,
    title = title,
    note = note,
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate?.toEpochDay(),
    timesCsv = times.sorted().joinToString(",") { (it.toSecondOfDay() / 60).toString() },
    repeat = repeat.name,
    daysOfWeek = daysOfWeek,
    leadMinutes = leadMinutes,
    dose = dose,
    stock = stock,
    refillThreshold = refillThreshold,
    enabled = enabled,
    createdAt = createdAt,
)

private fun ReminderLogEntity.toDomain(): ReminderLog = ReminderLog(
    id = id,
    reminderId = reminderId,
    occurrence = occurrenceAt.toLocalDateTime(),
    status = LogStatus.valueOf(status),
    actedAt = actedAt,
)
