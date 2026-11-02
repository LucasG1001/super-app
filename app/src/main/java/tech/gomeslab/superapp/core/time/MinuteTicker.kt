package tech.gomeslab.superapp.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emite o horário atual agora e a cada virada de minuto. */
fun minuteTicker(): Flow<Long> = flow {
    while (true) {
        val now = System.currentTimeMillis()
        emit(now)
        delay(60_000 - now % 60_000)
    }
}
