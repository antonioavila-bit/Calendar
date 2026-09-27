package org.fossify.calendar.security

import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// GPL-3.0-or-later. Offline Security Calendar additions, 2026.
data class Person(val id: String = UUID.randomUUID().toString(), val name: String, val phone: String = "")
data class Post(val id: String = UUID.randomUUID().toString(), val name: String, val notes: String = "")
data class Shift(val id: String = UUID.randomUUID().toString(), val postId: String, val label: String,
    val start: String, val end: String, val zone: String, val required: Int, val notes: String = "") {
    fun startTime(): ZonedDateTime = strictZoned(LocalDateTime.parse(start), ZoneId.of(zone))
    fun endTime(): ZonedDateTime = strictZoned(LocalDateTime.parse(end), ZoneId.of(zone))
    fun key() = listOf(postId, start, end, zone).joinToString("|")
}
data class Assignment(val shiftId: String, val personId: String)
data class Roster(val people: List<Person> = emptyList(), val posts: List<Post> = emptyList(),
    val shifts: List<Shift> = emptyList(), val assignments: List<Assignment> = emptyList(), val templates: List<ShiftTemplate> = emptyList()) {
    fun assigned(s: Shift) = people.filter { p -> assignments.any { it.shiftId == s.id && it.personId == p.id } }
    fun open(s: Shift) = (s.required - assigned(s).size).coerceAtLeast(0)
    fun coverage(s: Shift): String = when (assigned(s).size) {
        0 -> "UNFILLED"
        in 1 until s.required -> "PARTIALLY FILLED"
        s.required -> "FILLED"
        else -> "OVERSTAFFED"
    }
    fun validate(): Roster {
        require(templates.size <= 2000 && people.size <= 2000 && posts.size <= 2000 && shifts.size <= 20000 && assignments.size <= 100000) { "Schedule exceeds supported size." }
        fun unique(values: List<String>, label: String) { require(values.size == values.toSet().size) { "Duplicate $label." } }
        unique(templates.map { it.id }, "template ID"); unique(templates.map { nameKey(it.name) }, "template name")
        templates.forEach { it.validate(posts) }
        unique(people.map { it.id }, "person ID"); unique(posts.map { it.id }, "post ID"); unique(shifts.map { it.id }, "shift ID")
        unique(people.map { nameKey(it.name) }, "person name"); unique(posts.map { nameKey(it.name) }, "post name")
        require((people.map { it.id } + posts.map { it.id } + shifts.map { it.id }).all { it.isNotBlank() && it.length <= 100 }) { "Invalid record ID." }
        require((people.map { it.name } + posts.map { it.name }).all { it.isNotBlank() && it.length <= 120 && '\n' !in it && '\r' !in it && '|' !in it }) { "Names must be 1–120 characters, without line breaks or |." }
        require(people.all { it.phone.length <= 60 }) { "Phone number is too long." }
        require(posts.all { it.notes.length <= 5000 }) { "Post notes are too long." }
        unique(shifts.map { it.key() }, "required shift (same post/time)")
        shifts.forEach { s ->
            require(posts.any { it.id == s.postId }) { "Shift references an unknown post." }
            require(s.required in 1..100 && s.label.isNotBlank() && s.label.length <= 120 && s.notes.length <= 5000) { "Invalid staffing requirement, label, or notes." }
            val duration = Duration.between(s.startTime(), s.endTime()).toMinutes()
            require(duration in 1..1500) { "Shift duration must be between 1 minute and 25 hours." }
        }
        unique(assignments.map { "${it.shiftId}|${it.personId}" }, "assignment")
        val byId = shifts.associateBy { it.id }
        assignments.forEach { a -> require(byId.containsKey(a.shiftId) && people.any { it.id == a.personId }) { "Assignment references missing data." } }
        assignments.groupBy { it.personId }.forEach { (personId, entries) ->
            var latestEnd: Instant? = null
            entries.map { byId.getValue(it.shiftId) }.sortedBy { it.startTime().toInstant() }.forEach { s ->
                val begins = s.startTime().toInstant()
                require(latestEnd == null || !begins.isBefore(latestEnd)) {
                    "Overlapping shifts for ${people.first { it.id == personId }.name} on ${s.startTime().toLocalDate()}. Nothing saved."
                }
                latestEnd = s.endTime().toInstant()
            }
        }
        return this
    }
}

fun nameKey(value: String) = value.trim().lowercase(Locale.ROOT)
fun strictZoned(local: LocalDateTime, zone: ZoneId): ZonedDateTime {
    val offsets = zone.rules.getValidOffsets(local)
    require(offsets.size == 1) { "Time $local in $zone is skipped or ambiguous due to a clock change. Choose an unambiguous time." }
    return ZonedDateTime.ofStrict(local, offsets.single(), zone)
}

object ScheduleEntry {
    fun time(raw: String): LocalTime {
        val t = raw.trim().lowercase(Locale.US).replace(".", "").replace(" ", "")
        val m = Regex("^(\\d{1,2})(?::(\\d{2}))?([ap]m?)?$").matchEntire(t)
            ?: throw IllegalArgumentException("Use a time such as 6 PM, 6a, or 18:00.")
        var h = m.groupValues[1].toInt()
        val minute = m.groupValues[2].ifBlank { "0" }.toInt()
        val suffix = m.groupValues[3]
        if (suffix.isNotEmpty()) {
            require(h in 1..12) { "12-hour times must be between 1 and 12." }
            h %= 12
            if (suffix.startsWith("p")) h += 12
        } else require(t.contains(":")) { "Add AM/PM, or use 24-hour HH:mm time." }
        return LocalTime.of(h, minute)
    }
    fun timeRange(raw: String): Pair<LocalTime, LocalTime> {
        val parts = raw.trim().split(Regex("\\s*(?:to|[-–])\\s*", RegexOption.IGNORE_CASE))
        require(parts.size == 2) { "Use a shift time such as 6p-6a." }
        val result = time(parts[0]) to time(parts[1])
        require(result.first != result.second) { "Start and end times cannot be identical." }
        return result
    }
    private fun date(raw: String, year: Int, month: Month? = null): LocalDate {
        val text = raw.trim().replace(Regex("(?<=\\d)(st|nd|rd|th)", RegexOption.IGNORE_CASE), "")
        if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(text)) return LocalDate.parse(text)
        if (Regex("\\d{1,2}").matches(text) && month != null) return LocalDate.of(year, month, text.toInt())
        val m = Regex("([A-Za-z]+)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?").matchEntire(text)
            ?: throw IllegalArgumentException("Use dates such as Oct 14, Oct 14 2026, or 2026-10-14. Year is selected above.")
        val matched = Month.values().firstOrNull { it.name.lowercase(Locale.US).startsWith(m.groupValues[1].lowercase(Locale.US)) && m.groupValues[1].length >= 3 }
            ?: throw IllegalArgumentException("Unknown month: ${m.groupValues[1]}")
        return LocalDate.of(m.groupValues[3].ifBlank { year.toString() }.toInt(), matched, m.groupValues[2].toInt())
    }
    fun dates(raw: String, year: Int): List<LocalDate> {
        require(year in 2000..2199) { "Select a year between 2000 and 2199." }
        require(raw.length <= 8000) { "Date input is too long." }
        val found = linkedSetOf<LocalDate>()
        var inheritedMonth: Month? = null
        raw.split(',').forEach { item ->
            var normalized = item.trim()
            // Oct 14-25 and Dec 31-Jan 2; never split the hyphens inside an ISO date.
            if (!Regex("^\\d{4}").containsMatchIn(normalized)) {
                normalized = normalized.replace(Regex("\\s*[-–]\\s*"), " through ")
            }
            val bounds = normalized.split(Regex("\\s+(?:through|to)\\s+|\\.\\.", RegexOption.IGNORE_CASE))
            require(bounds.size in 1..2 && bounds.none { it.isBlank() }) { "Invalid date range." }
            val first = date(bounds[0], year, inheritedMonth)
            var last = if (bounds.size == 2) date(bounds[1], first.year, first.month) else first
            val lastHasYear = bounds.size == 2 && Regex("\\d{4}").containsMatchIn(bounds[1])
            if (last.isBefore(first) && !lastHasYear && last.monthValue < first.monthValue) last = last.plusYears(1)
            require(!last.isBefore(first) && Duration.between(first.atStartOfDay(), last.atStartOfDay()).toDays() <= 365) { "Range must be forward and at most 366 days." }
            var day = first
            while (!day.isAfter(last)) { found.add(day); day = day.plusDays(1) }
            inheritedMonth = last.month
            require(found.size <= 366) { "A single entry may cover at most 366 dates." }
        }
        require(found.isNotEmpty()) { "Enter at least one date." }
        return found.sorted()
    }
    fun shift(day: LocalDate, postId: String, label: String, start: LocalTime, end: LocalTime, zone: String, required: Int, notes: String = ""): Shift {
        require(start != end) { "Start and end times cannot be identical." }
        val startDateTime = day.atTime(start)
        val endDateTime = (if (end < start) day.plusDays(1) else day).atTime(end)
        val result = Shift(postId = postId, label = label.trim(), start = startDateTime.toString(), end = endDateTime.toString(), zone = zone, required = required, notes = notes)
        result.startTime(); result.endTime()
        return result
    }
    // Strict, reviewable grammar. No remote AI, fuzzy person matching, or silent field guessing.
    fun bulk(roster: Roster, text: String, year: Int, zone: String, createMissing: Boolean, required: Int): Roster {
        return BulkScheduleReview.prepare(roster, text, year, zone, createMissing, required).requireValid()
    }
}

object ScheduleText {
    private val date = DateTimeFormatter.ofPattern("EEE, MMM d, uuuu", Locale.US)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    fun format(roster: Roster, shifts: List<Shift>, personId: String? = null, compact: Boolean = false, group: String = "Date"): String {
        if (compact) return CompactScheduleText.format(roster, shifts, personId)
        val selectedPerson = personId?.let { id -> requireNotNull(roster.people.firstOrNull { it.id == id }) { "Unknown personnel selection." } }
        val selected = shifts.filter { s -> selectedPerson == null || roster.assignments.any { it.shiftId == s.id && it.personId == selectedPerson.id } }
            .sortedWith(compareBy<Shift> { it.startTime().toInstant() }.thenBy { roster.posts.firstOrNull { p -> p.id == it.postId }?.name ?: "" })
        val b = StringBuilder("SHIFT CALENDAR\n")
        selectedPerson?.let { b.append(it.name).append('\n') }
        if (selected.isEmpty()) return b.append("No shifts match this selection.\n").toString()
        b.append("Shift dates are START dates. Overnight end dates are shown.\n\n")
        val groups: Map<String, List<Shift>> = when (group) {
            "Post" -> selected.groupBy { s -> roster.posts.first { it.id == s.postId }.name }.toSortedMap()
            "Person" -> {
                val out = linkedMapOf<String, List<Shift>>()
                (if (selectedPerson != null) listOf(selectedPerson) else roster.people.sortedBy { it.name }).forEach { p ->
                    val entries = selected.filter { s -> roster.assignments.any { it.shiftId == s.id && it.personId == p.id } }
                    if (entries.isNotEmpty()) out[p.name] = entries
                }
                val unfilled = selected.filter { roster.open(it) > 0 }
                if (selectedPerson == null && unfilled.isNotEmpty()) out["OPEN POSITIONS"] = unfilled
                out
            }
            else -> selected.groupBy { it.startTime().toLocalDate().format(date) }
        }
        groups.forEach { (heading, entries) ->
            b.append(heading.uppercase(Locale.US)).append('\n')
            entries.forEach { s ->
                val start = s.startTime(); val end = s.endTime()
                val post = roster.posts.first { it.id == s.postId }.name
                b.append(start.format(date)).append(" | ").append(post).append('\n')
                b.append(s.label).append(": ").append(start.format(time)).append(" to ")
                if (start.toLocalDate() != end.toLocalDate()) b.append(end.format(date)).append(' ')
                b.append(end.format(time)).append(" (").append(s.zone).append(")\n")
                if (selectedPerson == null) b.append("Assigned: ").append(roster.assigned(s).joinToString("; ") { it.name }.ifBlank { "UNASSIGNED" }).append('\n')
                b.append("Coverage: ").append(roster.coverage(s)).append(" (").append(roster.assigned(s).size).append('/').append(s.required).append(")\n")
                if (s.notes.isNotBlank()) b.append("Notes: ").append(s.notes).append('\n')
                b.append('\n')
            }
        }
        return b.toString()
    }
}
