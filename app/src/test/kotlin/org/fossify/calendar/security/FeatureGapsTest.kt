package org.fossify.calendar.security

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class FeatureGapsTest {
    private val hotel=Post("h","Hotel")
    private val gate=Post("g","Gate")
    private val people=listOf(Person("a","Alex","+12025550101"),Person("b","Blair","+12025550102"),Person("c","Casey","+12025550103"))
    private val base=Roster(people,listOf(hotel,gate))
    private val template=ShiftTemplate("t","Hotel nights","Night","18:00","06:00","America/New_York",2,hotel.id)
    private fun rejects(action:()->Unit) {assertTrue(runCatching(action).isFailure)}
    private fun s(day:Int,post:Post=hotel,n:Int=1,label:String="Night")=ScheduleEntry.shift(LocalDate.of(2026,10,day),post.id,label,LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",n)
    private fun populated():Roster {val a=s(1);val b=s(2,gate,2,"Day");val c=s(3);return base.copy(shifts=listOf(c,b,a),assignments=listOf(Assignment(a.id,"a"),Assignment(a.id,"b"),Assignment(b.id,"b"),Assignment(c.id,"c"))).validate()}
    @Test fun templateSavedIndependentlyOfShifts() {val r=ShiftTemplates.put(base,template);assertEquals(1,r.templates.size);assertTrue(r.shifts.isEmpty());assertTrue(base.templates.isEmpty())}
    @Test fun editTemplateDoesNotChangeGeneratedShiftOrAssignments() {val saved=ShiftTemplates.put(base,template);val a=ShiftTemplates.apply(saved,"t",listOf(LocalDate.of(2026,10,1))).roster;val r=a.copy(assignments=listOf(Assignment(a.shifts.single().id,"a")));val edited=ShiftTemplates.put(r,template.copy(required=4,start="19:00"));assertEquals(r.shifts,edited.shifts);assertEquals(r.assignments,edited.assignments);assertEquals(4,edited.templates.single().required)}
    @Test fun deleteTemplateDoesNotDeleteGeneratedShift() {val r=ShiftTemplates.apply(ShiftTemplates.put(base,template),"t",listOf(LocalDate.of(2026,10,1))).roster;assertEquals(r.shifts,ShiftTemplates.remove(r,"t").shifts);assertTrue(ShiftTemplates.remove(r,"t").templates.isEmpty())}
    @Test fun templateCopyHasIndependentIdentity() {val r=ShiftTemplates.put(ShiftTemplates.put(base,template),template.copy(id="copy",name="Hotel backup"));assertEquals(2,r.templates.size);assertEquals(template,r.templates.first());assertEquals(1,ShiftTemplates.remove(r,"t").templates.size)}
    @Test fun templateNamesAreUniqueCaseInsensitively() {rejects {ShiftTemplates.put(ShiftTemplates.put(base,template),template.copy(id="other",name=" hotel NIGHTS "))}}
    @Test fun badTimesWeekdaysStaffAndPostsRejected() {listOf(template.copy(end="18:00"),template.copy(start="29:00"),template.copy(weekdays=emptySet()),template.copy(weekdays=setOf(8)),template.copy(required=0),template.copy(postId="missing"),template.copy(zone="bad-zone")).forEach {t->rejects {ShiftTemplates.put(base,t)}}}
    @Test fun genericTemplateRequiresSelectedPost() {val r=ShiftTemplates.put(base,template.copy(postId=null));rejects {ShiftTemplates.apply(r,"t",listOf(LocalDate.of(2026,10,1)))};assertEquals(gate.id,ShiftTemplates.apply(r,"t",listOf(LocalDate.of(2026,10,1)),gate.id).added.single().postId)}
    @Test fun selectedWeekdaysAndOvernightEnd() {val r=ShiftTemplates.put(base,template.copy(weekdays=setOf(1,5)));val result=ShiftTemplates.apply(r,"t",ScheduleEntry.dates("Oct 1-7",2026));assertEquals(listOf(2,5),result.added.map {it.startTime().dayOfMonth});assertTrue(result.added.all {it.endTime().toLocalDate()==it.startTime().toLocalDate().plusDays(1)})}
    @Test fun duplicateTemplateApplicationPreservesStaffing() {val existing=s(1,n=5);val r=ShiftTemplates.put(base.copy(shifts=listOf(existing),assignments=listOf(Assignment(existing.id,"a"))),template);val result=ShiftTemplates.apply(r,"t",ScheduleEntry.dates("Oct 1-2",2026));assertEquals(1,result.skipped);assertEquals(1,result.added.size);assertEquals(existing,result.roster.shifts.first());assertEquals(r.assignments,result.roster.assignments)}
    @Test fun dstFailureLeavesInputUnmodified() {val r=ShiftTemplates.put(base,template.copy(start="01:30"));rejects {ShiftTemplates.apply(r,"t",ScheduleEntry.dates("Oct 31-Nov 1",2026))};assertTrue(r.shifts.isEmpty())}
    @Test fun noMatchingWeekdaysBlocked() {val r=ShiftTemplates.put(base,template.copy(weekdays=setOf(1)));rejects {ShiftTemplates.apply(r,"t",listOf(LocalDate.of(2026,10,1)))}}
    @Test fun missingTemplateAndRemovedPostBlocked() {rejects {ShiftTemplates.apply(base,"missing",listOf(LocalDate.of(2026,10,1)))};rejects {base.copy(posts=emptyList(),templates=listOf(template)).validate()}}
    @Test fun dateSortBothDirections() {val r=populated();assertEquals(listOf(1,2,3),ScheduleOrder.sort(r,r.shifts).map {it.startTime().dayOfMonth});assertEquals(listOf(3,2,1),ScheduleOrder.sort(r,r.shifts,BoardSort.START,true).map {it.startTime().dayOfMonth})}
    @Test fun postAndShiftSortBothDirections() {val r=populated();assertEquals(gate.id,ScheduleOrder.sort(r,r.shifts,BoardSort.POST).first().postId);assertEquals(hotel.id,ScheduleOrder.sort(r,r.shifts,BoardSort.POST,true).first().postId);assertEquals("Day",ScheduleOrder.sort(r,r.shifts,BoardSort.SHIFT).first().label);assertEquals("Night",ScheduleOrder.sort(r,r.shifts,BoardSort.SHIFT,true).first().label)}
    @Test fun personnelSortUsesAllNamesDeterministically() {val r=populated();assertEquals(listOf(1,2,3),ScheduleOrder.sort(r,r.shifts,BoardSort.PERSONNEL).map {it.startTime().dayOfMonth});assertEquals(3,ScheduleOrder.sort(r,r.shifts,BoardSort.PERSONNEL,true).first().startTime().dayOfMonth)}
    @Test fun coverageAndOpenPositionSort() {val r=populated();assertEquals("PARTIALLY FILLED",r.coverage(ScheduleOrder.sort(r,r.shifts,BoardSort.COVERAGE).first()));assertEquals("OVERSTAFFED",r.coverage(ScheduleOrder.sort(r,r.shifts,BoardSort.COVERAGE,true).first()));assertEquals(1,r.open(ScheduleOrder.sort(r,r.shifts,BoardSort.OPEN,true).first()))}
    @Test fun sortAfterFilterNeverRestoresExcludedRows() {val r=populated();val selected=ScheduleFilters.select(r,LocalDate.of(2026,10,1),LocalDate.of(2026,10,2),postId=hotel.id);BoardSort.values().forEach {by->assertEquals(selected,ScheduleOrder.sort(r,selected,by,true))};assertEquals(3,r.shifts.size)}
    @Test fun stableTiesDoNotDependOnInputOrder() {val a=s(1);val b=s(2);val r=base.copy(shifts=listOf(b,a));assertEquals(listOf(a,b),ScheduleOrder.sort(r,r.shifts,BoardSort.POST,true));assertEquals(listOf(a,b),ScheduleOrder.sort(r,r.shifts.reversed(),BoardSort.POST,true))}
    @Test fun emptyBoardAndUnknownSavedSortAreSafe() {assertTrue(ScheduleOrder.sort(base,emptyList()).isEmpty());assertEquals(BoardSort.START,BoardSort.from("obsolete"))}
    @Test fun arbitrarySmsSubsetOnlyContainsSelectedPeopleAndTheirShifts() {val r=populated();val messages=SmsBatch.prepare(r,r.shifts,setOf("c","a"));assertEquals(listOf("a","c"),messages.map {it.personId});assertFalse(messages.first().text.contains("Blair"));assertFalse(messages.first().text.contains("Gate"));assertTrue(messages.first().text.contains("Oct 1"));assertTrue(messages.last().text.contains("Oct 3"))}
    @Test fun emptySmsSelectionNeverMeansAll() {val r=populated();rejects {SmsBatch.prepare(r,r.shifts,emptySet())}}
    @Test fun missingSmsRecipientNeverSilentlySkipped() {val r=populated();rejects {SmsBatch.prepare(r,r.shifts,setOf("a","missing"))}}
    @Test fun smsNoMatchingShiftsBlocksWholeBatch() {val r=populated();rejects {SmsBatch.prepare(r,r.shifts.filter {it.startTime().dayOfMonth==1},setOf("a","c"))}}
    @Test fun smsReviewAndPhoneValidationRemainSeparate() {val r=populated().let {it.copy(people=it.people.map {p->if(p.id=="a") p.copy(phone="") else p})};val m=SmsBatch.prepare(r,r.shifts,setOf("a"));assertTrue(SmsBatch.review(m).contains("MISSING"));rejects {SmsBatch.number(m.single())}}
    @Test fun smsNumbersNormalizedAndInputNotMutated() {val m=PersonalSms("a","Alex","+1 (202) 555-0101","text");assertEquals("+12025550101",SmsBatch.number(m));assertEquals("+1 (202) 555-0101",m.phone)}
    @Test fun staleShiftSelectionBlocked() {val r=populated();rejects {SmsBatch.prepare(r,listOf(r.shifts.first().copy(required=99)),setOf("c"))}}
}
