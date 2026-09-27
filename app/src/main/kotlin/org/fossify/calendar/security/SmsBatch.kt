package org.fossify.calendar.security

// GPL-3.0-or-later. Explicit recipient IDs; empty selection NEVER means everybody.
data class PersonalSms(val personId: String, val name: String, val phone: String, val text: String)
object SmsBatch {
    fun prepare(roster: Roster, shifts: List<Shift>, ids: Set<String>): List<PersonalSms> {
        require(ids.isNotEmpty()) { "Choose at least one person for SMS." }
        val people = roster.people.filter { it.id in ids }.sortedWith(compareBy<Person> { nameKey(it.name) }.thenBy { it.id })
        require(people.size == ids.size) { "A selected person no longer exists. Choose recipients again." }
        val selected = shifts.distinctBy { it.id }
        require(selected.all { s -> roster.shifts.any { it == s } }) { "Schedule selection is stale. Reload before sharing." }
        val noShifts = people.filter { p -> selected.none { s -> roster.assignments.any { it.personId == p.id && it.shiftId == s.id } } }
        require(noShifts.isEmpty()) { "No matching shifts for: ${noShifts.joinToString { it.name }}. Change the date/post filters or your selection. No messages prepared." }
        return people.map { p ->
            val personal = selected.filter { s -> roster.assignments.any { it.personId == p.id && it.shiftId == s.id } }
            PersonalSms(p.id, p.name, p.phone, ScheduleText.format(roster, personal, p.id, true))
        }
    }
    fun number(message: PersonalSms): String {
        val value = message.phone.replace(Regex("[\\s()\\-]"), "")
        require(Regex("\\+?[0-9]{7,15}").matches(value)) { "Enter a valid full SMS phone number for ${message.name}." }
        return value
    }
    fun review(messages: List<PersonalSms>): String = messages.joinToString("\n\n--------------------\n\n") {
        "Recipient: ${it.name}\nPhone: ${it.phone.ifBlank { "MISSING — add a phone number before sending" }}\n${it.text}"
    }
}
