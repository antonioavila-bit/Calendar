package org.fossify.calendar.security

import org.junit.Assert.*
import org.junit.Test

class CompactScheduleTextTest {
    private fun roster(dates:String="Oct 14-25"):Roster {
        val base=Roster(listOf(Person("p1","Alex"),Person("p2","Sam")),listOf(Post("h1","Hotel")))
        return ScheduleEntry.bulk(base,"Alex | $dates | 6p-6a | Hotel",2026,"America/New_York",true,1)
    }
    @Test fun consecutiveDaysCompress() {val r=roster();val t=CompactScheduleText.format(r,r.shifts,"p1");assertTrue("Oct 14-25, 2026" in t);assertTrue(t.length<300);assertTrue("next morning" in t)}
    @Test fun missingDateIsNotImplied() {val r=roster("Oct 14, Oct 16");val t=CompactScheduleText.format(r,r.shifts,"p1");assertFalse("14-16" in t);assertTrue("Oct 14, 2026; Oct 16, 2026" in t)}
    @Test fun personalMessageExcludesOthers() {val r=roster();val t=CompactScheduleText.format(r,r.shifts,"p1");assertFalse("Sam" in t);assertFalse("Assigned:" in t)}
    @Test fun noAccidentalOtherPersonShifts() {val r=roster();val t=CompactScheduleText.format(r,r.shifts,"p2");assertTrue("No assigned shifts" in t);assertFalse("Hotel" in t)}
}
