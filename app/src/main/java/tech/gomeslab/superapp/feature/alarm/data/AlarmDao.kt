package tech.gomeslab.superapp.feature.alarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    // Retornos não-Unit evitam o bug do KSP2 "unexpected jvm signature V" em suspend fun.
    @Update
    suspend fun update(alarm: AlarmEntity): Int

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean): Int

    @Delete
    suspend fun delete(alarm: AlarmEntity): Int

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
