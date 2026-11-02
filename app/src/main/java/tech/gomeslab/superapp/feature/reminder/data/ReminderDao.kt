package tech.gomeslab.superapp.feature.reminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAt")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("UPDATE reminders SET stock = :stock WHERE id = :id")
    suspend fun setStock(id: Long, stock: Int?): Int

    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean): Int

    @Query("SELECT * FROM reminder_logs WHERE occurrenceAt >= :fromMillis ORDER BY occurrenceAt")
    fun observeLogsSince(fromMillis: Long): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId")
    suspend fun getLogsFor(reminderId: Long): List<ReminderLogEntity>

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId AND occurrenceAt = :occurrenceAt")
    suspend fun getLog(reminderId: Long, occurrenceAt: Long): ReminderLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: ReminderLogEntity): Long

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId AND occurrenceAt = :occurrenceAt")
    suspend fun deleteLog(reminderId: Long, occurrenceAt: Long): Int

    @Query("DELETE FROM reminder_logs WHERE reminderId = :reminderId")
    suspend fun deleteLogsFor(reminderId: Long): Int
}
