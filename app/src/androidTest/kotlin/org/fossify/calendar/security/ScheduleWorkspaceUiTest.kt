package org.fossify.calendar.security

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.YearMonth
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ScheduleWorkspaceUiTest {
    private lateinit var context:Context
    @Before fun clean() {
        context=ApplicationProvider.getApplicationContext()
        context.deleteDatabase("security-schedule.db")
        context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun all(root:View):List<View> = listOf(root)+if(root is ViewGroup) (0 until root.childCount).flatMap {all(root.getChildAt(it))} else emptyList()
    private fun waitFor(s:ActivityScenario<ShiftActivity>,label:String) {
        repeat(100) {
            var found=false;s.onActivity {a->found=all(a.window.decorView).filterIsInstance<TextView>().any {v->v.text.toString()==label}}
            if(found) return
            SystemClock.sleep(100)
        }
        fail("Did not find UI label: $label")
    }
    private fun press(s:ActivityScenario<ShiftActivity>,label:String) {s.onActivity {a->all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}.performClick()}}
    private fun settle() {InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450)}
    private fun capture(name:String) {
        settle()
        val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        requireNotNull(screenshot) {"Could not capture emulator evidence."}
        File(context.getExternalFilesDir(null),"$name.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)}
        screenshot.recycle()
    }
    @Test fun calendarCoverageAndWindowsClipboard() {
        val month=YearMonth.now();val p=Person("p1","Alex");val post=Post("h1","Hotel")
        val a=ScheduleEntry.shift(month.atDay(1),post.id,"Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",2)
        val b=ScheduleEntry.shift(month.atDay(2),post.id,"Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",1)
        ScheduleStore(context).use {it.save(0,Roster(listOf(p),listOf(post),listOf(a,b),listOf(Assignment(a.id,p.id))))}
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitFor(s,"Required shifts");capture("calendar")
            press(s,"Schedule");waitFor(s,"PARTIALLY FILLED · 1/2 assigned · 1 open");capture("schedule")
            press(s,"Uncovered");waitFor(s,"UNFILLED · 0/1 assigned · 1 open");capture("uncovered")
            press(s,"Share / TXT");waitFor(s,"Copy for Word");press(s,"Copy for Word");settle()
            s.onActivity {
                val clip=(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                val text=requireNotNull(clip).getItemAt(0).coerceToText(context).toString()
                assertTrue(text.contains("Hotel"));assertTrue(text.contains("Coverage: PARTIALLY FILLED (1/2)"));assertTrue(text.contains("Coverage: UNFILLED (0/1)"));assertTrue(text.contains("Alex"))
                val windows=("\uFEFF"+text.replace("\n","\r\n")).toByteArray(Charsets.UTF_8)
                assertEquals(0xEF,windows[0].toInt() and 255);assertTrue(windows.toString(Charsets.UTF_8).contains("\r\n"))
            }
            capture("text-export")
        }
    }
    @Test fun keyboardDraftSurvivesLandscapeAndPortrait() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitFor(s,"Required shifts");press(s,"Personnel");waitFor(s,"Add person");press(s,"Add person");waitFor(s,"Save person")
            s.onActivity {a->val field=all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()=="Name"};field.setText("Draft Person");field.requestFocus();(a.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT)}
            settle()
            s.onActivity {it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE};settle()
            s.onActivity {a->assertEquals("Draft Person",all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()=="Name"}.text.toString())}
            capture("landscape-entry")
            s.onActivity {it.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};settle()
            s.onActivity {a->assertEquals("Draft Person",all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()=="Name"}.text.toString())}
        }
    }
}
