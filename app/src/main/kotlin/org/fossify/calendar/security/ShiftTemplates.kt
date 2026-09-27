package org.fossify.calendar.security

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

// GPL-3.0-or-later. Templates are independent records; applying one creates snapshots.
data class ShiftTemplate(
    val id: String = UUID.randomUUID().toString(), val name: String, val label: String,
    val start: String, val end: String, val zone: String, val required: Int,
    val postId: String? = null, val weekdays: Set<Int> = (1..7).toSet(), val notes: String = ""
) {
    fun validate(posts: List<Post>): ShiftTemplate {
        require(id.isNotBlank() && id.length <= 100) { "Invalid template ID." }
        require(listOf(name, label).all { it.isNotBlank() && it.length <= 120 && '\n' !in it && '\r' !in it && '|' !in it }) { "Template name and shift name must be 1–120 characters without line breaks or |." }
        require(LocalTime.parse(start) != LocalTime.parse(end)) { "Template start and end times cannot be identical." }
        ZoneId.of(zone)
        require(required in 1..100) { "Template staffing must be 1–100." }
        require(weekdays.isNotEmpty() && weekdays.all { it in 1..7 }) { "Select at least one valid weekday." }
        require(notes.length <= 5000) { "Template notes are too long." }
        require(postId == null || posts.any { it.id == postId }) { "Template references an unknown post." }
        return this
    }
}
data class TemplateApplication(val roster: Roster, val added: List<Shift>, val skipped: Int)
object ShiftTemplates {
    fun put(roster: Roster, template: ShiftTemplate): Roster =
        roster.copy(templates = roster.templates.filterNot { it.id == template.id } + template).validate()
    fun remove(roster: Roster, id: String): Roster {
        require(roster.templates.any { it.id == id }) { "Template no longer exists." }
        return roster.copy(templates = roster.templates.filterNot { it.id == id }).validate()
    }
    fun apply(roster: Roster, id: String, dates: List<LocalDate>, postId: String? = null): TemplateApplication {
        roster.validate()
        val t = requireNotNull(roster.templates.singleOrNull { it.id == id }) { "Template no longer exists." }
        val location = postId ?: t.postId
        require(location != null && roster.posts.any { it.id == location }) { "Choose a post for this template." }
        require(dates.isNotEmpty() && dates.distinct().size <= 366) { "Choose 1–366 dates." }
        val matching = dates.distinct().sorted().filter { it.dayOfWeek.value in t.weekdays }
        require(matching.isNotEmpty()) { "No dates match this template's weekdays." }
        val keys = roster.shifts.map { it.key() }.toMutableSet()
        var skipped = 0
        val additions = matching.mapNotNull { day ->
            val s = ScheduleEntry.shift(day, location, t.label, LocalTime.parse(t.start), LocalTime.parse(t.end), t.zone, t.required, t.notes)
            if (keys.add(s.key())) s else { skipped++; null }
        }
        // A DST error or invalid requirement rejects the entire proposal before any write.
        return TemplateApplication(roster.copy(shifts = roster.shifts + additions).validate(), additions, skipped)
    }
}
