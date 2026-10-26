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
            packageName = "com.zhiliaoapp.musically",
            kind = EventKind.SCROLLED,
            viewId = "viewpager",
            className = "androidx.viewpager.widget.ViewPager",
            detail = "dy=291 → swipe (in the feed)",
        )
        assertEquals(entry, InspectorEntry.fromLine(entry.toLine()))
    }

    @Test
    fun emptyFieldsComeBackAsNull() {
        val entry = InspectorEntry(1L, "io.github.jessesaarinen.distractionkiller", EventKind.NOTE, detail = "service started")
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
        assertNull(InspectorEntry.fromLine("soon\tapp\tNOTE\t\t\t"))
        assertNull(InspectorEntry.fromLine("1\tapp\tNO_SUCH_KIND\t\t\t"))
    }
}
