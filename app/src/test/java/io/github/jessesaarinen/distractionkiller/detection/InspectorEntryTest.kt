package io.github.jessesaarinen.distractionkiller.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** The inspector log is saved one entry per line, so it survives the app being closed. */
class InspectorEntryTest {

    @Test
    fun entryComesBackFromItsLine() {
        val entry = InspectorEntry(
            timeMillis = 1_761_508_800_000,
            app = "TikTok",
            kind = EventKind.LEVEL1,
            detail = "used today 1800s",
        )
        assertEquals(entry, InspectorEntry.fromLine(entry.toLine()))
    }

    @Test
    fun emptyFieldsComeBackAsNull() {
        val entry = InspectorEntry(1L, "TikTok", EventKind.FRONT)
        assertEquals(entry, InspectorEntry.fromLine(entry.toLine()))
    }

    @Test
    fun tabsAndLineBreaksDoNotBreakTheFile() {
        val entry = InspectorEntry(1L, "app", EventKind.ERROR, detail = "event: Boom\nsecond line\tand a tab")
        val line = entry.toLine()
        assertFalse(line.contains('\n'))
        assertEquals("event: Boom second line and a tab", InspectorEntry.fromLine(line)?.detail)
    }

    @Test
    fun brokenLinesAreSkipped() {
        assertNull(InspectorEntry.fromLine(""))
        assertNull(InspectorEntry.fromLine("1\tapp\tNOTE"))
        assertNull(InspectorEntry.fromLine("soon\tapp\tNOTE\t"))
        assertNull(InspectorEntry.fromLine("1\tapp\tNO_SUCH_KIND\t"))
    }

    @Test
    fun linesOfTheOldScrollWatcherAreSkipped() {
        assertNull(InspectorEntry.fromLine("1\tcom.zhiliaoapp.musically\tSCROLLED\tviewpager\t\tdy=291"))
        assertNull(InspectorEntry.fromLine("1\tapp\tNOTE\t\t\tservice started"))
    }
}
