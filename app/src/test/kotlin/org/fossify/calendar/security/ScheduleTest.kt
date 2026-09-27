package org.fossify.calendar.security

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleTest {
    private val p = Person(id="p1", name="Alex")
    private val post = Post(id="h1", name="Hotel")
    private val base = Roster(listOf(p),listOf(post))
    private fun shift(day:String="2026-10-01", start:String="18:00", end:String="06:00", n:Int=1) = ScheduleEntry.shift(LocalDate.parse(day),post.id,"Night",LocalTime.parse(start),LocalTime.parse(end),"America/New_York",n)
    private fun rejects(body:()->Unit) { assertTrue(runCatching(body).isFailure) }
    @Test fun overnight() { val s=shift(); assertEquals("2026-10-02",s.endTime().toLocalDate().toString()); assertEquals(12L,Duration.between(s.startTime(),s.endTime()).toHours()) }
    @Test fun timeNormalization() { assertEquals(LocalTime.of(18,0),ScheduleEntry.time("6p")); assertEquals(LocalTime.MIDNIGHT,ScheduleEntry.time("12 AM")) }
    @Test fun bareTimeRejected() { rejects { ScheduleEntry.time("6") } }
    @Test fun invalidTimes() { rejects { ScheduleEntry.time("25:00") }; rejects { ScheduleEntry.time("13pm") } }
    @Test fun equalEndpoints() { rejects { ScheduleEntry.timeRange("6a-6a") } }
    @Test fun crossMonth() { assertEquals(14,ScheduleEntry.dates("Oct 26 through Nov 8",2026).size) }
    @Test fun crossYear() { assertEquals(LocalDate.of(2027,1,2),ScheduleEntry.dates("Dec 31-Jan 2",2026).last()) }
    @Test fun explicitYear() { rejects { ScheduleEntry.dates("Dec 31 2026 through Jan 2 2026",2026) } }
    @Test fun commaDates() { assertEquals(3,ScheduleEntry.dates("Sep 30, Oct 1, Oct 2, Oct 1",2026).size) }
    @Test fun shortRange() { assertEquals(12,ScheduleEntry.dates("Oct 14-25",2026).size) }
    @Test fun isoDates() { assertEquals(3,ScheduleEntry.dates("2026-10-01..2026-10-03",2026).size) }
    @Test fun leapDates() { assertEquals(1,ScheduleEntry.dates("Feb 29",2028).size); rejects { ScheduleEntry.dates("Feb 29",2026) } }
    @Test fun rangeLimit() { rejects { ScheduleEntry.dates("2026-01-01..2028-01-01",2026) } }
    @Test fun emptyCoverage() { val s=shift(); val r=base.copy(shifts=listOf(s)).validate(); assertEquals(1,r.open(s)); assertEquals("UNFILLED",r.coverage(s)) }
    @Test fun partial() { val s=shift(n=2); val r=base.copy(shifts=listOf(s),assignments=listOf(Assignment(s.id,p.id))).validate(); assertEquals(1,r.open(s)); assertEquals("PARTIALLY FILLED",r.coverage(s)) }
    @Test fun overstaffed() { val s=shift(); val r=base.copy(people=base.people+Person(id="p2",name="Sam"),shifts=listOf(s),assignments=listOf(Assignment(s.id,"p1"),Assignment(s.id,"p2"))).validate(); assertEquals("OVERSTAFFED",r.coverage(s)) }
    @Test fun overlap() { val a=shift();val b=shift("2026-10-02","05:00","10:00");rejects { base.copy(shifts=listOf(a,b),assignments=listOf(Assignment(a.id,"p1"),Assignment(b.id,"p1"))).validate() } }
    @Test fun adjacentAllowed() { val a=shift();val b=shift("2026-10-02","06:00","10:00");base.copy(shifts=listOf(a,b),assignments=listOf(Assignment(a.id,"p1"),Assignment(b.id,"p1"))).validate() }
    @Test fun duplicateAssignment() { val s=shift(); rejects { base.copy(shifts=listOf(s),assignments=listOf(Assignment(s.id,"p1"),Assignment(s.id,"p1"))).validate() } }
    @Test fun autumnDuration() { val s=shift("2026-10-31");assertEquals(13L,Duration.between(s.startTime(),s.endTime()).toHours()) }
    @Test fun springDuration() { val s=shift("2026-03-07");assertEquals(11L,Duration.between(s.startTime(),s.endTime()).toHours()) }
    @Test fun ambiguousDst() { rejects { shift("2026-11-01","01:30","06:00") } }
    @Test fun missingDst() { rejects { shift("2026-03-08","02:30","06:00") } }
    @Test fun bulkPure() { val next=ScheduleEntry.bulk(base,"Alex | Oct 14-25 | 6p-6a | Hotel",2026,"America/New_York",true,1);assertEquals(12,next.shifts.size);assertTrue(base.shifts.isEmpty()) }
    @Test fun bulkIdempotent() { val t="Alex | Oct 14-25 | 6p-6a | Hotel";val r=ScheduleEntry.bulk(base,t,2026,"America/New_York",true,1);assertEquals(r,ScheduleEntry.bulk(r,t,2026,"America/New_York",false,1)) }
    @Test fun missingRequirement() { rejects { ScheduleEntry.bulk(base,"Alex | Oct 1 | 6p-6a | Hotel",2026,"America/New_York",false,1) } }
    @Test fun missingPerson() { rejects { ScheduleEntry.bulk(base,"Alec | Oct 1 | 6p-6a | Hotel",2026,"America/New_York",true,1) } }
    @Test fun fourLineBlocks() { val r=ScheduleEntry.bulk(base,"Alex\nOct 1-2\n6p-6a\nHotel\n\nAlex\nOct 3\n6p-6a\nHotel",2026,"America/New_York",true,1);assertEquals(3,r.shifts.size) }
    @Test fun unicodePrint() { val s=shift();val r=base.copy(people=listOf(p.copy(name="Álex")),shifts=listOf(s),assignments=listOf(Assignment(s.id,"p1"))).validate();val t=ScheduleText.format(r,listOf(s));assertTrue("Álex" in t && "Oct 2, 2026" in t && "Coverage: FILLED (1/1)" in t) }
    @Test fun personnelPrivacy() { val s=shift();val r=base.copy(people=base.people+Person(id="p2",name="Sam"),shifts=listOf(s),assignments=listOf(Assignment(s.id,"p1"),Assignment(s.id,"p2"))).validate();assertFalse("Sam" in ScheduleText.format(r,listOf(s),"p1",true)) }
}
