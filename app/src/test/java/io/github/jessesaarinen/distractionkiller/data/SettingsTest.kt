package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The times the timer uses, from the choices on the settings screen (task N10). */
class SettingsTest {

    @Test
    fun defaultsAreOneMinuteAndThirtySeconds() {
        val settings = Settings()
        assertEquals(60, settings.level1Seconds)
        assertEquals(30, settings.level2DelaySeconds)
        assertTrue(settings.interruptionsOn)
    }

    @Test
    fun chosenTimesAreUsed() {
        val settings = Settings(intervalMin = 5, level2AfterSec = 120)
        assertEquals(300, settings.level1Seconds)
        assertEquals(120, settings.level2DelaySeconds)
    }

    @Test
    fun demoModeUsesItsOwnShortTimes() {
        val settings = Settings(intervalMin = 15, level2AfterSec = 120, demoMode = true)
        assertEquals(Config.DEMO_LEVEL1_SECONDS, settings.level1Seconds)
        assertEquals(Config.DEMO_LEVEL2_DELAY_SECONDS, settings.level2DelaySeconds)
    }

    @Test
    fun defaultsAreAmongTheChoices() {
        val settings = Settings()
        assertTrue(settings.intervalMin in Config.INTERVAL_OPTIONS_MIN)
        assertTrue(settings.level2AfterSec in Config.LEVEL2_DELAY_OPTIONS_SECONDS)
    }
}
