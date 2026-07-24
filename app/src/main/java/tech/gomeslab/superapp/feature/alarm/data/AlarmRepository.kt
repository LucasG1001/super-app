package tech.gomeslab.superapp.feature.alarm.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.toDomain
import tech.gomeslab.superapp.feature.alarm.domain.toEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val dao: AlarmDao,
) {
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getEnabled(): List<Alarm> = dao.getEnabled().map { it.toDomain() }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toDomain()

    suspend fun upsert(alarm: Alarm): Long =
        if (alarm.id == 0L) dao.insert(alarm.toEntity()) else {
            dao.update(alarm.toEntity()); alarm.id
        }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
