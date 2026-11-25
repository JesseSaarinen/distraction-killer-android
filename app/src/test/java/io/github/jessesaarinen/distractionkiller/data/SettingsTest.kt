package io.github.jessesaarinen.distractionkiller.data

import io.github.jessesaarinen.distractionkiller.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The times the timer uses, from the choices on the settings screen (tasks N10, N21 and N22). */
class SettingsTest {

    @Test
    fun defaultsAreOneMinuteRepeatAndSetupNotDone() {
        val settings = Settings()
        assertEquals(60, settings.repeatSeconds)
        assertTrue(settings.interruptionsOn)
        assertFalse(settings.setupDone)
    }

    @Test
    fun chosenDailyTimeIsUsed() {
        val settings = Settings()
        assertEquals(45 * 60, settings.allowanceSeconds(READER.copy(allowanceMin = 45)))
    }

    @Test
    fun demoModeUsesTheDemoSecondsForEveryApp() {
        val settings = Settings(demoMode = true, demoAllowanceSec = 30)
        assertEquals(30, settings.allowanceSeconds(READER))
        assertEquals(30, settings.allowanceSeconds(READER.copy(allowanceMin = 120)))
    }

    @Test
    fun repeatTimeStaysWithinThePicker() {
        assertEquals(Config.REPEAT_MIN_SECONDS, Settings.clampRepeat(0))
        assertEquals(90, Settings.clampRepeat(90))
        assertEquals(Config.REPEAT_MAX_MINUTES * 60 + 55, Settings.clampRepeat(10_000))
    }

    @Test
    fun newDemoStartsTheSetupAgainWithShortTimes() {
        val used = Settings(apps = listOf(READER), repeatSeconds = 600, setupDone = true, tester = 3, interruptionsOn = false)
        val demo = used.newDemo()
        assertTrue(demo.demoMode)
        assertFalse(demo.setupDone)
        assertTrue(demo.interruptionsOn)
        assertEquals(0, demo.tester)
        assertEquals(Config.DEMO_REPEAT_SECONDS, demo.repeatSeconds)
        assertEquals(Config.DEFAULT_DEMO_ALLOWANCE_SECONDS, demo.demoAllowanceSec)
        assertEquals(listOf("Instagram", "TikTok", "YouTube"), demo.watchedLabels)
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
        assertEquals(settings.repeatSeconds, Settings.clampRepeat(settings.repeatSeconds))
        assertEquals(0, settings.repeatSeconds % Config.REPEAT_STEP_SECONDS)
        assertTrue(settings.demoAllowanceSec in Config.DEMO_ALLOWANCE_OPTIONS_SECONDS)
        assertEquals(0, Config.DEFAULT_ALLOWANCE_MIN % Config.ALLOWANCE_STEP_MIN)
    }

    private companion object {
        const val TIKTOK = "com.zhiliaoapp.musically"
        val READER = WatchedApp("com.example.reader", "Reader", 30)
        val ALBUM = WatchedApp("com.example.album", "album", 30)
    }
}
