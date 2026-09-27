package org.fossify.calendar.security

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReadmeAcceptanceTest {
    private val people = listOf("Mark", "Bill", "Tate").map { Person(it, it) }
    private val hotel = Post("hotel", "Hotel")
    private val base = Roster(people, listOf(hotel))
    private val blocks = listOf("Mark\nOct 14-25\n6 PM-6 AM\nHotel", "Bill\nOct 26-Nov 8\n6 PM-6 AM\nHotel", "Tate\nSep 30, Oct 1, Oct 2\n6 PM-6 AM\nHotel")
    private fun review(text: String = blocks.joinToString("\n\n"), roster: Roster = base) = BulkScheduleReview.prepare(roster, text, 2026, "America/New_York", true, 1)
    private fun shift(day: Int, n: Int = 1, label: String = "Night", post: Post = hotel) = ScheduleEntry.shift(LocalDate.of(2026,10,day), post.id,label,LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",n)
    @Test fun allSixPersonnelOrdersYieldExactAssignments() {
        val permutations = listOf(listOf(0,1,2),listOf(0,2,1),listOf(1,0,2),listOf(1,2,0),listOf(2,0,1),listOf(2,1,0))
        permutations.forEach { order ->
            val preview = review(order.joinToString("\n\n") { blocks[it] })
            assertTrue(preview.canSave)
            val r = preview.requireValid()
            assertEquals(29,r.assignments.size)
            assertEquals(listOf(12,14,3),people.map { p -> r.assignments.count { it.personId == p.id } })
            people.forEachIndexed { i,p ->
                val expected = ScheduleEntry.dates(blocks[i].lines()[1],2026)
                assertEquals(expected,r.shifts.filter { s -> r.assignments.any { it.personId == p.id && it.shiftId == s.id } }.map { it.startTime().toLocalDate() }.sorted())
            }
        }
    }
    @Test fun literalReadmeBulletsAccepted() {
        val text = blocks.joinToString("\n") { b -> b.lines().mapIndexed { i,s -> if(i==0) "- $s" else "  - $s" }.joinToString("\n") }
        assertEquals(29,review(text).requireValid().assignments.size)
    }
    @Test fun unicodeAndAsteriskBulletsAccepted() { assertEquals(29,review(blocks.joinToString("\n\n") { b -> b.lines().joinToString("\n") { "• $it" } }).requireValid().assignments.size) }
    @Test fun pipeRowsRemainSupported() { assertEquals(29,review(blocks.joinToString("\n") { it.lines().joinToString(" | ") }).requireValid().assignments.size) }
    @Test fun overnightAndCrossMonthAreExplicit() {
        val r=review().requireValid()
        assertTrue(r.shifts.all { it.startTime().toLocalTime()==LocalTime.of(18,0) && it.endTime().toLocalTime()==LocalTime.of(6,0) && it.endTime().toLocalDate()==it.startTime().toLocalDate().plusDays(1) })
        assertTrue(review().render().contains("Mon, Nov 9, 2026"))
    }
    @Test fun reviewShowsEveryRequiredField() {
        val t=review().render()
        listOf("Parsed person: Mark","Date/range entered: Oct 14-25","Normalized start dates (12)","Shift time: 6 PM-6 AM","Post/location: Hotel","Assignments to create: 12","Conflicts: none","Ambiguous or missing fields: none","29 new assignments").forEach { assertTrue("Missing review field $it", t.contains(it)) }
    }
    @Test fun previewNeverMutatesInput() { review(); assertTrue(base.shifts.isEmpty()); assertTrue(base.assignments.isEmpty()) }
    @Test fun duplicatesAreCountedButNotAdded() { val r=review().requireValid();val p=review(roster=r);assertTrue(p.canSave);assertEquals(0,p.assignmentCount);assertEquals(29,p.rows.sumOf {it.duplicates});assertEquals(r,p.requireValid()) }
    @Test fun allMissingAndAmbiguousFieldsAreReported() {
        val p=review("Nobody | Oct 3 | 6-6 | Missing\nTate | | | \n"+blocks[1].lines().joinToString(" | "))
        assertFalse(p.canSave);val t=p.render()
        listOf("Unknown personnel","Unknown post","Shift time:","Dates are missing","Post/location is missing","ENTRY 3","Parsed person: Bill").forEach {assertTrue(t.contains(it))}
        assertTrue(runCatching {p.requireValid()}.isFailure);assertTrue(base.assignments.isEmpty())
    }
    @Test fun incompleteBlockDoesNotConsumeNextPerson() { val p=review("Mark\nOct 1\n6p-6a\n\n"+blocks[1]);assertFalse(p.canSave);assertEquals(2,p.rows.size);assertEquals("Bill",p.rows[1].fields[0]) }
    @Test fun missingRequirementBlocksWholeSave() {val p=BulkScheduleReview.prepare(base,blocks[0],2026,"America/New_York",false,1);assertFalse(p.canSave);assertEquals(12,p.rows.single().issues.size)}
    @Test fun yearMustBeExplicitlySelected() {assertFalse(BulkScheduleReview.prepare(base,blocks[0],null,"America/New_York",true,1).canSave)}
    @Test fun badZoneAndStaffingReported() {val p=BulkScheduleReview.prepare(base,blocks[0],2026,"Invalid",true,0);assertFalse(p.canSave);assertEquals(2,p.problems.size)}
    @Test fun mixedFormatsAreBlocked() {assertFalse(review(blocks[0]+"\n\nBill | Oct 2 | 6p-6a | Hotel").canSave)}
    @Test fun allConflictsAreListedAndWholeBatchBlocked() {
        val gate=Post("gate","Gate")
        val p=review("Mark | Oct 1-2 | 6p-6a | Hotel\nMark | Oct 1-2 | 7p-5a | Gate",base.copy(posts=listOf(hotel,gate)))
        assertFalse(p.canSave);assertEquals(2,p.conflicts.size);assertTrue(p.render().contains("Hotel overlaps"));assertTrue(runCatching {p.requireValid()}.isFailure)
    }
    @Test fun existingRequirementsAreKeptWithTheirStaffing() {
        val s=shift(14,2);val p=review(blocks[0],base.copy(shifts=listOf(s)))
        assertEquals(2,p.requireValid().shifts.first {it.id==s.id}.required)
        assertEquals("PARTIALLY FILLED",p.requireValid().coverage(s))
    }
    @Test fun exactUncoveredOctoberDates() {
        val r=review(roster=base.copy(shifts=(1..31).map {shift(it)})).requireValid()
        assertEquals((3..13).map {LocalDate.of(2026,10,it)},r.shifts.filter {r.open(it)>0}.map {it.startTime().toLocalDate()}.sorted())
    }
    @Test fun everyCoverageStateCanBeFiltered() {
        val a=shift(1);val b=shift(2,2);val c=shift(3);val d=shift(4)
        val r=base.copy(shifts=listOf(a,b,c,d),assignments=listOf(Assignment(b.id,"Mark"),Assignment(c.id,"Mark"),Assignment(d.id,"Mark"),Assignment(d.id,"Bill"))).validate()
        listOf("UNFILLED","PARTIALLY FILLED","FILLED","OVERSTAFFED").forEach { state ->assertEquals(1,ScheduleFilters.select(r,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31),coverage=state).size)}
    }
    @Test fun datePersonPostAndSearchFiltersCombine() {
        val r=review().requireValid()
        val entries=ScheduleFilters.select(r,LocalDate.of(2026,10,20),LocalDate.of(2026,10,23),"Mark",hotel.id,search="hotel")
        assertEquals(4,entries.size)
        assertTrue(ScheduleFilters.select(r,LocalDate.of(2026,10,20),LocalDate.of(2026,10,23),"Bill").isEmpty())
    }
    @Test fun customShiftTimeFilterAndInvalidDateRange() {
        val a=shift(1,1,"Day");val b=shift(2,1,"Night");val r=base.copy(shifts=listOf(a,b))
        assertEquals(listOf(b),ScheduleFilters.select(r,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31),pattern=ScheduleFilters.pattern(b)))
        assertTrue(runCatching {ScheduleFilters.select(r,LocalDate.of(2026,10,31),LocalDate.of(2026,10,1))}.isFailure)
    }
    @Test fun conflictsFilterFindsOnlyOverlappingShifts() {
        val a=shift(1);val gate=Post("gate","Gate");val b=shift(1,post=gate);val c=shift(2)
        val r=base.copy(posts=listOf(hotel,gate),shifts=listOf(a,b,c),assignments=listOf(Assignment(a.id,"Mark"),Assignment(b.id,"Mark"),Assignment(c.id,"Mark")))
        assertEquals(2,ScheduleFilters.select(r,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31),conflictsOnly=true).size)
    }
}
