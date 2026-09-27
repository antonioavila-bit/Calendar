package org.fossify.calendar.security

import java.time.LocalDate
import java.time.ZoneId

// GPL-3.0-or-later. A pure, all-or-nothing preview; never writes to storage.
data class BulkReviewRow(
    val number: Int, val fields: List<String>, val dates: List<LocalDate>,
    val assignments: Int, val requirements: Int, val duplicates: Int,
    val issues: List<String>
)
data class BulkReviewResult(
    val proposed: Roster, val affected: List<Shift>, val rows: List<BulkReviewRow>,
    val problems: List<String>, val conflicts: List<String>, val year: Int?, val zone: String
) {
    val canSave get() = problems.isEmpty() && conflicts.isEmpty() && rows.all { it.issues.isEmpty() }
    val assignmentCount get() = rows.sumOf { it.assignments }
    fun requireValid(): Roster {
        require(canSave) { render() }
        return proposed
    }
    fun render(): String = buildString {
        append("Year for undated entries: ${year ?: "MISSING"}\nTime zone: $zone\n")
        append("${assignmentCount} new assignments; ${rows.sumOf { it.requirements }} new required shifts; ${rows.sumOf { it.duplicates }} existing assignments skipped.\n")
        append(if (canSave) "Nothing saved yet. Review all entries before Save all.\n" else "BLOCKED: nothing can be saved until every issue is corrected. Counts below are provisional.\n")
        rows.forEach { row ->
            fun value(i: Int) = row.fields.getOrNull(i)?.ifBlank { "MISSING" } ?: "MISSING"
            append("\nENTRY ${row.number}\nParsed person: ${value(0)}\nDate/range entered: ${value(1)}\n")
            append("Normalized start dates (${row.dates.size}): ${row.dates.joinToString(", ")}\n")
            append("Shift time: ${value(2)}\nPost/location: ${value(3)}\n")
            append("Assignments to create: ${row.assignments}; requirements to create: ${row.requirements}; duplicates skipped: ${row.duplicates}\n")
            if (row.issues.isEmpty()) append("Entry fields: resolved.\n")
            else row.issues.forEach { append("ISSUE: $it\n") }
        }
        append("\nConflicts: ${if (conflicts.isEmpty()) "none detected among resolved entries" else conflicts.size}\n")
        conflicts.forEach { append("CONFLICT: $it\n") }
        append("Ambiguous or missing fields: ${if (problems.isEmpty() && rows.all { it.issues.isEmpty() }) "none" else "see issues above/below"}\n")
        problems.forEach { append("ISSUE: $it\n") }
        if (affected.isNotEmpty()) {
            append("\nSORTED SHIFT PREVIEW\n")
            append(ScheduleText.format(proposed, affected))
        }
    }
}

object BulkScheduleReview {
    private data class Input(val fields: List<String>, val issue: String? = null)
    private fun input(text: String): List<Input> {
        val clean = text.replace("\r\n", "\n").replace('\r', '\n').lines()
            .map { it.trim().replace(Regex("^[-*•]\\s+"), "") }
        val lines = clean.filter { it.isNotBlank() }
        if (lines.any { '|' in it }) {
            if (lines.any { '|' !in it }) return listOf(Input(lines.take(4), "Do not mix pipe rows with four-line blocks."))
            return lines.map { line ->
                val fields = line.split('|').map(String::trim)
                Input(fields, if (fields.size == 4) null else "Use four columns: name | dates | time | post.")
            }
        }
        // Blank lines separate blocks; never silently slide a missing field into the next person.
        val groups = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        clean.forEach { line ->
            if (line.isBlank()) { if (current.isNotEmpty()) { groups.add(current); current = mutableListOf() } }
            else current.add(line)
        }
        if (current.isNotEmpty()) groups.add(current)
        return groups.flatMap { group -> group.chunked(4).map { fields ->
            Input(fields, if (fields.size == 4) null else "Each person needs four lines: name, dates, time, post. A field is missing.")
        } }
    }
    fun prepare(roster: Roster, text: String, year: Int?, zone: String, createMissing: Boolean, required: Int?): BulkReviewResult {
        val problems = mutableListOf<String>()
        if (year == null || year !in 2000..2199) problems.add("Select a year between 2000 and 2199.")
        if (runCatching { ZoneId.of(zone) }.isFailure) problems.add("Choose a valid time zone.")
        if (createMissing && (required == null || required !in 1..100)) problems.add("New shifts require a staffing count between 1 and 100.")
        runCatching { roster.validate() }.exceptionOrNull()?.let { problems.add("Existing schedule: ${it.message}") }
        if (text.length > 64000) problems.add("Bulk entry is too long (maximum 64000 characters).")
        val inputs = if (text.length <= 64000) input(text) else emptyList()
        if (inputs.isEmpty()) problems.add("Enter at least one personnel schedule.")
        var next = roster
        val rows = mutableListOf<BulkReviewRow>()
        val affectedIds = linkedSetOf<String>()
        inputs.forEachIndexed { index, row ->
            val issues = mutableListOf<String>()
            row.issue?.let(issues::add)
            val f = (row.fields + List(4) { "" }).take(4)
            val person = roster.people.singleOrNull { nameKey(it.name) == nameKey(f[0]) }
            val post = roster.posts.singleOrNull { nameKey(it.name) == nameKey(f[3]) }
            if (person == null) issues.add(if (f[0].isBlank()) "Personnel name is missing." else "Unknown personnel '${f[0]}'. Add the exact name first.")
            if (post == null) issues.add(if (f[3].isBlank()) "Post/location is missing." else "Unknown post '${f[3]}'. Add the exact post first.")
            val dates = runCatching { ScheduleEntry.dates(f[1], year ?: 0) }.getOrElse {
                issues.add(if (f[1].isBlank()) "Dates are missing." else "Dates: ${it.message}"); emptyList()
            }
            val times = runCatching { ScheduleEntry.timeRange(f[2]) }.getOrElse {
                issues.add(if (f[2].isBlank()) "Shift time is missing." else "Shift time: ${it.message}"); null
            }
            var assignments = 0; var requirements = 0; var duplicates = 0
            if (issues.isEmpty() && problems.isEmpty() && person != null && post != null && times != null) {
                dates.forEach { day ->
                    runCatching {
                        val candidate = ScheduleEntry.shift(day, post.id, "Shift", times.first, times.second, zone, required ?: 1)
                        var shift = next.shifts.firstOrNull { it.key() == candidate.key() }
                        if (shift == null) {
                            require(createMissing) { "No required shift at ${post.name} on $day. Create requirements first, or enable missing-shift creation." }
                            shift = candidate; next = next.copy(shifts = next.shifts + shift); requirements++
                        }
                        val assignment = Assignment(shift.id, person.id)
                        if (assignment in next.assignments) duplicates++
                        else { next = next.copy(assignments = next.assignments + assignment); assignments++; affectedIds.add(shift.id) }
                    }.exceptionOrNull()?.let { issues.add(it.message ?: "Invalid shift on $day.") }
                }
            }
            rows.add(BulkReviewRow(index + 1, listOf(person?.name ?: f[0], f[1], f[2], post?.name ?: f[3]), dates, assignments, requirements, duplicates, issues))
        }
        val conflictPairs = runCatching { ScheduleFilters.conflicts(next) }.getOrDefault(emptyList())
        val conflicts = conflictPairs.map { c ->
            val person = next.people.first { it.id == c.personId }.name
            fun describe(s: Shift) = "${s.start} to ${s.end} (${s.zone}) at ${next.posts.first { it.id == s.postId }.name}"
            "$person: ${describe(c.first)} overlaps ${describe(c.second)}."
        }
        if (conflicts.isEmpty()) runCatching { next.validate() }.exceptionOrNull()?.let { problems.add(it.message ?: "Invalid schedule.") }
        val affected = next.shifts.filter { it.id in affectedIds }.sortedBy { it.startTime().toInstant() }
        if (affected.size > 500) problems.add("Review at most 500 changed shifts at a time. Split this batch.")
        return BulkReviewResult(next, affected.take(500), rows, problems, conflicts, year, zone)
    }
}
