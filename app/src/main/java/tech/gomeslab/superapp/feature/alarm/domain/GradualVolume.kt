package tech.gomeslab.superapp.feature.alarm.domain

import kotlin.math.pow

// A audição é logarítmica: subir o volume linearmente soaria alto logo no início. Por isso a rampa
// é linear em decibéis (de START_DB até 0 dB) e convertida para o ganho linear do MediaPlayer.
object GradualVolume {

    private const val START_DB = -40f

    fun volumeAt(elapsedMillis: Long, durationMillis: Long): Float {
        if (durationMillis <= 0L) return 1f
        val progress = (elapsedMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        if (progress >= 1f) return 1f
        val db = START_DB * (1f - progress)
        return 10f.pow(db / 20f)
    }
}
