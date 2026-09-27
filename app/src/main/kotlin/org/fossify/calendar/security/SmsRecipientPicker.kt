package org.fossify.calendar.security

import android.app.Activity
import android.app.AlertDialog

// GPL-3.0-or-later. Selection has no transport side effects; Cancel leaves it untouched.
object SmsRecipientPicker {
    fun show(activity: Activity, people: List<Person>, selected: Set<String>, done: (Set<String>) -> Unit) {
        require(people.isNotEmpty()) { "Add personnel before selecting SMS recipients." }
        val rows=people.sortedWith(compareBy<Person> {nameKey(it.name)}.thenBy {it.id})
        val checked=BooleanArray(rows.size) {rows[it].id in selected}
        val dialog=AlertDialog.Builder(activity).setTitle("Select SMS recipients")
            .setMultiChoiceItems(rows.map {it.name+if(it.phone.isBlank()) " (no phone number)" else ""}.toTypedArray(),checked) {_,i,value ->checked[i]=value}
            .setNegativeButton("Cancel",null)
            .setNeutralButton("Clear selection",null)
            .setPositiveButton("Use selection") {_,_ ->done(rows.filterIndexed {i,_->checked[i]}.map {it.id}.toSet())}
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                checked.indices.forEach {checked[it]=false;dialog.listView.setItemChecked(it,false)}
            }
        }
        dialog.show()
    }
}
