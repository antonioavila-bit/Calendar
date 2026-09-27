package org.fossify.calendar.security

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

// GPL-3.0-or-later. Selection has no transport side effects; Cancel leaves it untouched.
object SmsRecipientPicker {
    fun show(activity: Activity, people: List<Person>, selected: Set<String>, done: (Set<String>) -> Unit) {
        require(people.isNotEmpty()) { "Add personnel before selecting SMS recipients." }
        val rows=people.sortedWith(compareBy<Person> {nameKey(it.name)}.thenBy {it.id})
        val checked=BooleanArray(rows.size) {rows[it].id in selected}
        fun dp(value:Int)=(activity.resources.displayMetrics.density*value).toInt()
        val panel=LinearLayout(activity).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(4),dp(18),dp(4))
        }
        val count=TextView(activity).apply {textSize=14f;setPadding(0,dp(4),0,dp(4))}
        fun refreshCount() {count.text="Selected: ${checked.count {it}} of ${rows.size}"}
        refreshCount();panel.addView(count)
        // Use explicit CheckBoxes instead of CheckedTextView rows whose click action can
        // disappear from the native multiple-choice dialog's accessibility hierarchy.
        // Recycle rows for larger rosters, but bind without firing an old row's listener.
        val adapter=object:BaseAdapter() {
            override fun getCount()=rows.size
            override fun getItem(position:Int)=rows[position]
            override fun getItemId(position:Int)=position.toLong()
            override fun getView(position:Int,convertView:View?,parent:ViewGroup):View {
                val box=(convertView as? CheckBox) ?: CheckBox(activity).apply {
                    minHeight=dp(48);textSize=17f;isAllCaps=false
                    setPadding(dp(4),dp(6),dp(8),dp(6))
                    layoutParams=AbsListView.LayoutParams(-1,-2)
                }
                box.setOnCheckedChangeListener(null)
                box.text=rows[position].name+if(rows[position].phone.isBlank()) " (no phone number)" else ""
                box.isChecked=checked[position]
                box.setOnCheckedChangeListener {_,value->checked[position]=value;refreshCount()}
                return box
            }
        }
        panel.addView(Button(activity).apply {
            text="Clear selection";isAllCaps=false;minHeight=dp(48)
            setOnClickListener {checked.fill(false);adapter.notifyDataSetChanged();refreshCount()}
        },LinearLayout.LayoutParams(-1,-2))
        val list=ListView(activity).apply {
            setItemsCanFocus(true)
            this.adapter=adapter
            dividerHeight=0
            contentDescription="SMS recipients"
        }
        val listHeight=minOf(dp(320),dp(rows.size.coerceAtMost(6)*56),activity.resources.displayMetrics.heightPixels*2/5)
        panel.addView(list,LinearLayout.LayoutParams(-1,listHeight))
        AlertDialog.Builder(activity).setTitle("Select SMS recipients").setView(panel)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Use selection") {_,_ ->done(rows.filterIndexed {i,_->checked[i]}.map {it.id}.toSet())}
            .show()
    }
}
