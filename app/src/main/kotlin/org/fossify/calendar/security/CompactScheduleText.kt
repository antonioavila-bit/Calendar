package org.fossify.calendar.security

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Offline SMS rendering. Consecutive identical assignments become date ranges, never guessed gaps. */
object CompactScheduleText {
    private val date = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US)
    private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private data class Pattern(val postId:String,val label:String,val start:String,val end:String,val zone:String,val overnight:Boolean,val staff:String,val coverage:String)
    fun format(roster:Roster,shifts:List<Shift>,personId:String?=null):String {
        val person=personId?.let {id -> requireNotNull(roster.people.firstOrNull {it.id==id}) {"Unknown personnel selection."}}
        val selected=shifts.filter {s->person==null||roster.assignments.any {it.shiftId==s.id&&it.personId==person.id}}
        val out=StringBuilder("SHIFT CALENDAR")
        if(person!=null) out.append(" - ").append(person.name)
        out.append("\nDates below are shift START dates.\n")
        if(selected.isEmpty()) return out.append("No assigned shifts in this selection.\n").toString()
        val groups=selected.sortedBy {it.startTime().toInstant()}.groupBy {s ->
            Pattern(s.postId,s.label,s.startTime().format(time),s.endTime().format(time),s.zone,s.startTime().toLocalDate()!=s.endTime().toLocalDate(),
                if(person==null) roster.assigned(s).sortedBy {it.name}.joinToString("; "){it.name}.ifBlank {"UNASSIGNED"} else "",
                if(person==null) "${roster.coverage(s)} ${roster.assigned(s).size}/${s.required}" else "")
        }
        groups.forEach {(pattern,entries)->
            val days=entries.map {it.startTime().toLocalDate()}.distinct().sorted()
            val ranges=mutableListOf<Pair<LocalDate,LocalDate>>()
            var first=days.first();var last=first
            days.drop(1).forEach {d->if(d==last.plusDays(1)) last=d else {ranges.add(first to last);first=d;last=d}}
            ranges.add(first to last)
            val dates=ranges.joinToString("; ") {(a,b)->when {
                a==b -> a.format(date)
                a.year==b.year&&a.month==b.month -> "${a.format(shortDate)}-${b.dayOfMonth}, ${b.year}"
                a.year==b.year -> "${a.format(shortDate)}-${b.format(date)}"
                else -> "${a.format(date)} to ${b.format(date)}"
            }}
            out.append('\n').append(roster.posts.first {it.id==pattern.postId}.name).append(" - ").append(pattern.label).append('\n')
            out.append(dates).append('\n').append(pattern.start).append(" to ").append(pattern.end)
            if(pattern.overnight) out.append(" next morning")
            out.append(" ("+pattern.zone+")\n")
            if(person==null) out.append("Assigned: ").append(pattern.staff).append("\nCoverage: ").append(pattern.coverage).append('\n')
        }
        return out.toString()
    }
}
