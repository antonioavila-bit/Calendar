package org.fossify.calendar.security

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReadmeAcceptanceUiTest {
    private lateinit var context: Context
    private val blocks = "- Tate\n  - Sep 30, Oct 1, Oct 2\n  - 6 PM-6 AM\n  - Hotel\n- Bill\n  - Oct 26-Nov 8\n  - 6 PM-6 AM\n  - Hotel\n- Mark\n  - Oct 14-25\n  - 6 PM-6 AM\n  - Hotel"
    @Before fun setup() {
        context=ApplicationProvider.getApplicationContext()
        context.deleteDatabase("security-schedule.db")
        context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().clear().commit()
        val people=listOf("Mark","Bill","Tate").map {Person(it,it)}
        val hotel=Post("hotel","Hotel");val gate=Post("gate","Gate")
        val shifts=(0L..39L).map {ScheduleEntry.shift(LocalDate.of(2026,9,30).plusDays(it),hotel.id,"Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",1)}
        ScheduleStore(context).use {it.save(0,Roster(people,listOf(hotel,gate),shifts))}
    }
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap {all(v.getChildAt(it))} else emptyList()
    private fun waitFor(s:ActivityScenario<ShiftActivity>,text:String) {
        repeat(150) {
            var ready=false;s.onActivity {a->ready=all(a.window.decorView).filterIsInstance<TextView>().any {it.text.toString()==text}}
            if(ready) return
            SystemClock.sleep(100)
        };fail("Missing activity control $text")
    }
    private fun press(s:ActivityScenario<ShiftActivity>,text:String) {
        s.onActivity {a->
            var target=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==text}
            if(!target.isShown) {all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()=="Menu"}.performClick();target=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==text}}
            assertTrue(target.isShown);target.performClick()
        }
    }
    private fun field(s:ActivityScenario<ShiftActivity>,label:String,value:String) {s.onActivity {a->all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()==label}.setText(value)}}
    private fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap {nodes(n.getChild(it))}
    private fun dialogText():String = nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).joinToString("\n") {it.text?.toString().orEmpty()}
    private fun waitDialog(fragment:String):String {
        repeat(150) {val t=dialogText();if(t.contains(fragment)) return t;SystemClock.sleep(100)}
        fail("Missing dialog text $fragment; actual=${dialogText()}");return ""
    }
    private fun dialogButton(label:String) {
        val button=nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).first {it.text?.toString()?.equals(label,true)==true}
        assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    private fun enter(s:ActivityScenario<ShiftActivity>,input:String) {
        waitFor(s,"Required shifts");press(s,"Enter schedules");waitFor(s,"Review and sort schedules")
        field(s,"Year for dates without a year","2026");field(s,"Time zone","America/New_York");field(s,"Schedule entries",input)
        press(s,"Review and sort schedules")
    }
    @Test fun exactReadmeBulletsPreviewBeforeSavingThenCorrectCoverage() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            enter(s,blocks)
            val t=waitDialog("29 new assignments")
            listOf("Parsed person: Mark","Parsed person: Bill","Parsed person: Tate","Date/range entered: Oct 14-25","Shift time: 6 PM-6 AM","Post/location: Hotel","Conflicts: none","Ambiguous or missing fields: none").forEach {assertTrue("Missing $it",t.contains(it))}
            ScheduleStore(context).use {assertTrue(it.read().roster.assignments.isEmpty())}
            dialogButton("Save all");waitFor(s,"Copy a date range / week")
            ScheduleStore(context).use {store->val r=store.read().roster;assertEquals(29,r.assignments.size);assertEquals((3..13).map {LocalDate.of(2026,10,it)},r.shifts.filter {r.open(it)>0}.map {it.startTime().toLocalDate()}.sorted())}
        }
    }
    @Test fun cancellingPreviewDoesNotPersistAnything() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->enter(s,blocks);waitDialog("29 new assignments");dialogButton("Cancel");ScheduleStore(context).use {assertEquals(1L,it.read().revision);assertTrue(it.read().roster.assignments.isEmpty())}}
    }
    @Test fun multipleBadFieldsAndLaterValidEntryAppearWithoutSaveButton() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            enter(s,"Missing Person | Oct 14 | 6-6 | Unknown Post\nBill | Oct 26 | 6p-6a | Hotel")
            val t=waitDialog("BLOCKED:")
            listOf("Unknown personnel","Unknown post","Shift time:","ENTRY 2","Parsed person: Bill").forEach {assertTrue(t.contains(it))}
            assertFalse(nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).any {it.text?.toString()?.equals("Save all",true)==true})
            dialogButton("Back to entry");ScheduleStore(context).use {assertTrue(it.read().roster.assignments.isEmpty())}
        }
    }
    @Test fun boardHasDateTimeCoverageAndSearchFilters() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitFor(s,"Required shifts");press(s,"Schedule");waitFor(s,"Filters / search");press(s,"Filters / search")
            field(s,"From start date (YYYY-MM-DD)","2026-10-03");field(s,"Through start date (YYYY-MM-DD)","2026-10-04");field(s,"Search names, posts, shifts or notes","Hotel")
            s.onActivity {a->
                val spinners=all(a.window.decorView).filterIsInstance<Spinner>()
                val status=spinners.first {sp->(0 until sp.count).any {sp.getItemAtPosition(it).toString()=="PARTIALLY FILLED"}}
                status.setSelection((0 until status.count).first {status.getItemAtPosition(it).toString()=="UNFILLED"})
                val pattern=spinners.first {sp->(0 until sp.count).any {sp.getItemAtPosition(it).toString()=="Night (18:00-06:00)"}}
                pattern.setSelection((0 until pattern.count).first {pattern.getItemAtPosition(it).toString()=="Night (18:00-06:00)"})
            }
            press(s,"Apply filters");waitFor(s,"2 shifts · 2 uncovered · 2 open")
        }
    }
}
