package org.fossify.calendar.security

// GPL-3.0-or-later. Stable sorting applied after filtering; never changes stored shifts.
enum class BoardSort(val title: String) {
    START("Start date/time"), POST("Post / location"), SHIFT("Shift / time"),
    PERSONNEL("Assigned personnel"), COVERAGE("Coverage status"), OPEN("Open positions");
    companion object { fun from(value: String?) = values().firstOrNull { it.name == value } ?: START }
}
object ScheduleOrder {
    fun sort(roster: Roster, shifts: List<Shift>, by: BoardSort = BoardSort.START, descending: Boolean = false): List<Shift> {
        val posts = roster.posts.associate { it.id to nameKey(it.name) }
        val names = roster.people.associate { it.id to nameKey(it.name) }
        val assigned = roster.assignments.groupBy { it.shiftId }.mapValues { (_, a) -> a.map { names.getValue(it.personId) }.sorted() }
        fun count(s: Shift) = assigned[s.id]?.size ?: 0
        fun status(s: Shift) = when { count(s) == 0 -> 0; count(s) < s.required -> 1; count(s) == s.required -> 2; else -> 3 }
        val primary: Comparator<Shift> = when (by) {
            BoardSort.START -> compareBy { it.startTime().toInstant() }
            BoardSort.POST -> compareBy { posts.getValue(it.postId) }
            BoardSort.SHIFT -> compareBy<Shift> { nameKey(it.label) }.thenBy { it.startTime().toLocalTime() }
            BoardSort.PERSONNEL -> compareBy { assigned[it.id]?.joinToString("\u0000") ?: "\uffff" }
            BoardSort.COVERAGE -> compareBy { status(it) }
            BoardSort.OPEN -> compareBy { (it.required - count(it)).coerceAtLeast(0) }
        }
        return shifts.sortedWith((if (descending) primary.reversed() else primary)
            .thenBy { it.startTime().toInstant() }.thenBy { posts.getValue(it.postId) }
            .thenBy { nameKey(it.label) }.thenBy { it.id })
    }
}
