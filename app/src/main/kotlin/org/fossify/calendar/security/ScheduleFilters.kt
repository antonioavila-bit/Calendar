package org.fossify.calendar.security

import java.time.LocalDate
import java.util.Locale

// GPL-3.0-or-later. Shared, testable board selection and overlap reporting.
data class ShiftConflict(val personId: String, val first: Shift, val second: Shift)
object ScheduleFilters {
    fun pattern(s: Shift) = "${s.label} (${s.startTime().toLocalTime()}-${s.endTime().toLocalTime()})"
    fun conflicts(roster: Roster): List<ShiftConflict> {
        val byId = roster.shifts.associateBy { it.id }
        val results = mutableListOf<ShiftConflict>()
        roster.assignments.groupBy { it.personId }.forEach { (person, assignments) ->
            val shifts = assignments.mapNotNull { byId[it.shiftId] }.distinctBy { it.id }.sortedBy { it.startTime().toInstant() }
            val active = mutableListOf<Shift>()
            shifts.forEach { s ->
                active.removeAll { !it.endTime().toInstant().isAfter(s.startTime().toInstant()) }
                active.forEach { results.add(ShiftConflict(person, it, s)) }
                active.add(s)
            }
        }
        return results
    }
    fun select(roster: Roster, from: LocalDate, through: LocalDate, personId: String? = null,
        postId: String? = null, pattern: String? = null, coverage: String? = null,
        uncovered: Boolean = false, conflictsOnly: Boolean = false, search: String = ""): List<Shift> {
        require(!through.isBefore(from)) { "End date is before start date." }
        val conflicting = if (conflictsOnly) conflicts(roster).flatMap { listOf(it.first.id, it.second.id) }.toSet() else emptySet()
        val query = search.trim().lowercase(Locale.ROOT)
        return roster.shifts.filter { s ->
            val date = s.startTime().toLocalDate()
            !date.isBefore(from) && !date.isAfter(through) &&
                (personId == null || roster.assignments.any { it.shiftId == s.id && it.personId == personId }) &&
                (postId == null || s.postId == postId) && (pattern == null || pattern(s) == pattern) &&
                (coverage == null || roster.coverage(s) == coverage) && (!uncovered || roster.open(s) > 0) &&
                (!conflictsOnly || s.id in conflicting) &&
                (query.isEmpty() || listOf(date.toString(), s.label, s.notes,
                    roster.posts.first { it.id == s.postId }.name,
                    roster.assigned(s).joinToString(" ") { it.name }, roster.coverage(s))
                    .joinToString(" ").lowercase(Locale.ROOT).contains(query))
        }.sortedWith(compareBy<Shift> { it.startTime().toInstant() }.thenBy { it.postId })
    }
}
