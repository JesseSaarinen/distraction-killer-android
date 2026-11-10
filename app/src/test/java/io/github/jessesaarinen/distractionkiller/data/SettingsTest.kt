package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The times the timer uses, from the choices on the settings screen (tasks N10 and N21). */
class SettingsTest {

    @Test
    fun defaultsAreFiveMinutesAndThirtySeconds() {
        val settings = Settings()
        assertEquals(300, settings.intervalSeconds)
        assertEquals(30, settings.level2DelaySeconds)
        assertTrue(settings.interruptionsOn)
    }

    @Test
    fun chosenTimesAreUsed() {
        val settings = Settings(intervalMin = 10, level2AfterSec = 120)
        assertEquals(600, settings.intervalSeconds)
        assertEquals(120, settings.level2DelaySeconds)
        assertEquals(45 * 60, settings.allowanceSeconds(READER.copy(allowanceMin = 45)))
    }

    @Test
    fun demoModeUsesItsOwnShortTimes() {
        val settings = Settings(intervalMin = 15, level2AfterSec = 120, demoMode = true)
        assertEquals(Config.DEMO_INTERVAL_SECONDS, settings.intervalSeconds)
        assertEquals(Config.DEMO_LEVEL2_DELAY_SECONDS, settings.level2DelaySeconds)
        assertEquals(Config.DEMO_ALLOWANCE_SECONDS, settings.allowanceSeconds(READER))
    }

    @Test
    fun defaultAppsAreWatchedWithThirtyMinutes() {
        val settings = Settings()
        assertEquals(listOf("Instagram", "TikTok", "YouTube"), settings.watchedLabels)
        assertEquals(30, settings.watchedApp(TIKTOK)?.allowanceMin)
        assertNull(settings.watchedApp(READER.packageName))
    }

    @Test
    fun addedAppsAreSortedAndNotTwice() {
        val settings = Settings(apps = emptyList()).addApp(READER).addApp(ALBUM).addApp(READER)
        assertEquals(listOf("album", "Reader"), settings.watchedLabels)
    }

    @Test
    fun removedAppIsNotWatched() {
        val settings = Settings().removeApp(TIKTOK)
        assertNull(settings.watchedApp(TIKTOK))
        assertEquals(listOf("Instagram", "YouTube"), settings.watchedLabels)
    }

    @Test
    fun dailyTimeChangesOnlyForThatApp() {
        val settings = Settings().setAllowance(TIKTOK, 90)
        assertEquals(90, settings.watchedApp(TIKTOK)?.allowanceMin)
        assertEquals(30, settings.watchedApp("com.instagram.android")?.allowanceMin)
    }

    @Test
    fun appsHaveTheirNamesInTheLogs() {
        val settings = Settings().addApp(READER)
        assertEquals("Reader", settings.appLabel(READER.packageName))
        assertEquals("TikTok", settings.appLabel(TIKTOK))
        assertEquals("com.example.other", settings.appLabel("com.example.other"))
    }

    @Test
    fun defaultsAreAmongTheChoices() {
        val settings = Settings()
        assertTrue(settings.intervalMin in Config.INTERVAL_OPTIONS_MIN)
        assertTrue(settings.level2AfterSec in Config.LEVEL2_DELAY_OPTIONS_SECONDS)
        assertEquals(0, Config.DEFAULT_ALLOWANCE_MIN % Config.ALLOWANCE_STEP_MIN)
    }

    private companion object {
        const val TIKTOK = "com.zhiliaoapp.musically"
        val READER = WatchedApp("com.example.reader", "Reader", 30)
        val ALBUM = WatchedApp("com.example.album", "album", 30)
    }
}
