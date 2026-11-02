package tech.gomeslab.superapp.feature.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.gomeslab.superapp.feature.alarm.domain.GradualVolume

class GradualVolumeTest {

    private val fifteenMinutes = 15 * 60_000L

    @Test
    fun `disabled ramp plays at full volume`() {
        assertEquals(1f, GradualVolume.volumeAt(0, 0), 0f)
    }

    @Test
    fun `starts very quiet and ends at full volume`() {
        assertEquals(0.01f, GradualVolume.volumeAt(0, fifteenMinutes), 0.001f)
        assertEquals(1f, GradualVolume.volumeAt(fifteenMinutes, fifteenMinutes), 0f)
        assertEquals(1f, GradualVolume.volumeAt(fifteenMinutes * 2, fifteenMinutes), 0f)
    }

    @Test
    fun `volume always increases over time`() {
        var previous = -1f
        for (step in 0..30) {
            val volume = GradualVolume.volumeAt(fifteenMinutes * step / 30, fifteenMinutes)
            assertTrue(volume > previous)
            previous = volume
        }
    }

    @Test
    fun `halfway is still gentle`() {
        assertEquals(0.1f, GradualVolume.volumeAt(fifteenMinutes / 2, fifteenMinutes), 0.001f)
    }
}
