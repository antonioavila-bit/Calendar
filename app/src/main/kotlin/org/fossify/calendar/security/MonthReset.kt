package org.fossify.calendar.security

import java.time.YearMonth

// GPL-3.0-or-later. Months are determined by each shift's local START date.
object MonthReset {
    fun clear(roster: Roster, month: YearMonth): Roster {
        val ids=roster.shifts.filter {YearMonth.from(it.startTime())==month}.map {it.id}.toSet()
        require(ids.isNotEmpty()) {"There are no saved shifts to clear in this month."}
        return roster.copy(shifts=roster.shifts.filterNot {it.id in ids},
            assignments=roster.assignments.filterNot {it.shiftId in ids}).validate()
    }
}
