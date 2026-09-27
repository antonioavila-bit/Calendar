package org.fossify.calendar.security

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class FeatureGapsDeviceTest {
    private lateinit var context:Context
    private lateinit var initial:Roster
    private val template=ShiftTemplate("t","Patrol","Night","18:00","06:00","America/New_York",2,"h")
    @Before fun setup() {
        context=ApplicationProvider.getApplicationContext()
        context.deleteDatabase("security-schedule.db")
        context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().clear().commit()
        val people=listOf(Person("a","Alex","+12025550101"),Person("b","Blair","+12025550102"),Person("c","Casey","+12025550103"))
        val shifts=(1..3).map {i ->ScheduleEntry.shift(LocalDate.of(2026,10,i),if(i==2) "g" else "h","Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",1).copy(id="s$i")}
        initial=Roster(people,listOf(Post("h","Hotel"),Post("g","Gate")),shifts,listOf(Assignment("s1","a"),Assignment("s1","b"),Assignment("s2","b"),Assignment("s3","c")))
        ScheduleStore(context).use {it.save(0,initial)}
    }
    private fun read()=ScheduleStore(context).use {it.read()}
    private fun seedTemplate() {ScheduleStore(context).use {it.save(it.read().revision,initial.copy(templates=listOf(template)))}}
    private fun all(v:View):List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap {all(v.getChildAt(it))} else emptyList()
    private fun waitButton(s:ActivityScenario<ShiftActivity>,label:String) {
        repeat(150) {var found=false;s.onActivity {a->found=all(a.window.decorView).filterIsInstance<Button>().any {it.isShown && it.text.toString()==label}};if(found)return;SystemClock.sleep(100)}
        fail("Missing shown button $label")
    }
    private fun press(s:ActivityScenario<ShiftActivity>,label:String) {
        s.onActivity {a->
            var b=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}
            if(!b.isShown) {all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()=="Menu"}.performClick();b=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}}
            assertTrue(b.isShown);b.performClick()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    private fun field(s:ActivityScenario<ShiftActivity>,label:String,value:String) {s.onActivity {a->all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()==label}.setText(value)}}
    private fun choose(s:ActivityScenario<ShiftActivity>,value:String) {
        s.onActivity {a->val sp=all(a.window.decorView).filterIsInstance<Spinner>().first {v->(0 until v.count).any {v.getItemAtPosition(it).toString()==value}};sp.setSelection((0 until sp.count).first {sp.getItemAtPosition(it).toString()==value})}
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    private fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap {nodes(n.getChild(it))}
    private fun dialogText()=nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).joinToString("\n") {it.text?.toString().orEmpty()}
    private fun waitDialog(fragment:String) {repeat(150) {if(dialogText().contains(fragment))return;SystemClock.sleep(100)};fail("Missing dialog $fragment: ${dialogText()}")}
    private fun hierarchy():String = nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).joinToString("\n") {
        "${it.className} text=${it.text} visible=${it.isVisibleToUser} enabled=${it.isEnabled} clickable=${it.isClickable} checked=${it.isChecked} actions=${it.actionList}"
    }
    private fun capture(name:String) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val folder=requireNotNull(context.getExternalFilesDir(null))
        File(folder,"$name-hierarchy.txt").writeText(hierarchy())
        val image=requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {"Screenshot unavailable: $name"}
        File(folder,"$name.png").outputStream().use {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))}
        image.recycle()
    }
    private fun dialogClick(label:String) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        // A title/text match can be exposed before the dialog's actionable nodes are ready.
        // Reacquire the current node, wait for a visible enabled click action, and stop on the
        // FIRST accepted action. Never repeat a successful checkbox click or waive a failure.
        repeat(100) {
            val n=nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull {
                it.text?.toString()?.equals(label,true)==true && it.isVisibleToUser && it.isEnabled &&
                    it.actionList.any {a->a.id==AccessibilityNodeInfo.ACTION_CLICK}
            }
            val bounds=Rect()
            if(n!=null && n.refresh()) {
                n.getBoundsInScreen(bounds)
                if(!bounds.isEmpty && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    instrumentation.waitForIdleSync();return
                }
            }
            SystemClock.sleep(50)
        }
        capture("failed-dialog-click")
        fail("Could not click visible enabled dialog control '$label': ${hierarchy()}")
    }
    private fun waitChecked(label:String,checked:Boolean) {
        repeat(100) {
            if(nodes(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow).any {
                it.text?.toString()==label && it.isCheckable && it.isChecked==checked
            }) return
            SystemClock.sleep(50)
        }
        capture("failed-checkbox-state");fail("Expected $label checked=$checked")
    }
    private fun waitPickerClosed() {
        repeat(100) {if(!dialogText().contains("Select SMS recipients")) return;SystemClock.sleep(50)}
        fail("SMS picker was not dismissed")
    }
    private fun export(s:ActivityScenario<ShiftActivity>) {waitButton(s,"Required shifts");press(s,"Share / TXT");field(s,"From start date (YYYY-MM-DD)","2026-10-01");field(s,"Through start date (YYYY-MM-DD)","2026-10-03")}
    private fun cardIds(s:ActivityScenario<ShiftActivity>):List<String> {var result=emptyList<String>();s.onActivity {a->result=all(a.window.decorView).mapNotNull {it.contentDescription?.toString()?.takeIf {v->v.startsWith("shift-card:")}}};return result}
    @Test fun templateManagerCreateEditDuplicateDeleteDoesNotChangeShifts() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitButton(s,"Required shifts");press(s,"Templates");press(s,"Add template");field(s,"Template name","Patrol");press(s,"Save template");waitButton(s,"Add template")
            assertEquals(initial.shifts.toSet(),read().roster.shifts.toSet());assertEquals(1,read().roster.templates.size)
            press(s,"Edit Patrol");field(s,"Template required personnel","3");press(s,"Save template");waitButton(s,"Add template");assertEquals(3,read().roster.templates.single().required)
            press(s,"Edit Patrol");press(s,"Duplicate template");press(s,"Save template");waitButton(s,"Add template");assertEquals(2,read().roster.templates.size)
            press(s,"Edit Patrol copy");press(s,"Delete template");waitDialog("Existing shifts and assignments are kept unchanged");dialogClick("Delete");waitButton(s,"Add template")
            assertEquals(listOf("Patrol"),read().roster.templates.map {it.name});assertEquals(initial.assignments.toSet(),read().roster.assignments.toSet());capture("templates-manager")
        }
    }
    @Test fun templateApplyRequiresReviewCancelThenSave() {
        seedTemplate()
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitButton(s,"Required shifts");press(s,"Templates");press(s,"Use Patrol");field(s,"Template dates","Oct 4-5");field(s,"Template year","2026");press(s,"Review template shifts")
            waitDialog("2 new requirements");capture("template-apply-review");assertEquals(3,read().roster.shifts.size);dialogClick("Cancel");assertEquals(3,read().roster.shifts.size)
            press(s,"Review template shifts");waitDialog("2 new requirements");dialogClick("Save all");waitButton(s,"Copy a date range / week")
            val r=read().roster;assertEquals(5,r.shifts.size);assertEquals(initial.assignments.toSet(),r.assignments.toSet());assertEquals(2,r.shifts.first {it.startTime().dayOfMonth==4}.required)
        }
    }
    @Test fun pickerArbitrarySubsetPersistsAndOnlyCopiesOwnSchedules() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            export(s);press(s,"Choose people for SMS");waitDialog("Select SMS recipients");dialogClick("Alex");dialogClick("Casey");waitChecked("Alex",true);waitChecked("Casey",true);capture("selected-sms-recipients");dialogClick("Use selection")
            assertEquals(setOf("a","c"),context.getSharedPreferences("security-settings",0).getStringSet("sms-selected-ids",emptySet()))
            s.recreate();waitButton(s,"Choose people for SMS");press(s,"Copy selected SMS text")
            var text="";s.onActivity {a->text=(a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).text.toString()}
            assertTrue(text.contains("Recipient: Alex") && text.contains("Recipient: Casey"));assertFalse(text.contains("Blair") || text.contains("Gate"))
            press(s,"Prepare SMS one by one");waitDialog("Choose whose SMS to open");assertTrue(dialogText().contains("Alex"));assertFalse(dialogText().contains("Blair"));dialogClick("Cancel")
            assertFalse(context.getSharedPreferences("security-settings",0).contains("sms-status"))
        }
    }
    @Test fun pickerCancelAndEmptySelectionNeverSendToEveryone() {
        context.getSharedPreferences("security-settings",0).edit().putStringSet("sms-selected-ids",setOf("a")).commit()
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            export(s)
            press(s,"Choose people for SMS")
            waitDialog("Select SMS recipients")
            dialogClick("Blair")
            waitChecked("Blair",true)
            dialogClick("Cancel")
            waitPickerClosed()
            assertEquals(setOf("a"),context.getSharedPreferences("security-settings",0).getStringSet("sms-selected-ids",emptySet()))
            press(s,"Choose people for SMS")
            waitDialog("Select SMS recipients")
            dialogClick("Clear selection")
            waitChecked("Alex",false)
            dialogClick("Use selection")
            waitPickerClosed()
            assertEquals(emptySet<String>(),context.getSharedPreferences("security-settings",0).getStringSet("sms-selected-ids",emptySet()))
            press(s,"Review selected people's SMS");waitDialog("Choose at least one person");dialogClick("OK")
            assertFalse(context.getSharedPreferences("security-settings",0).contains("sms-status"))
        }
    }
    @Test fun boardSortAndDirectionPreserveFiltersAndRecreation() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitButton(s,"Required shifts");press(s,"Schedule");press(s,"Filters / search")
            field(s,"From start date (YYYY-MM-DD)","2026-10-01");field(s,"Through start date (YYYY-MM-DD)","2026-10-02")
            choose(s,"Post / location");choose(s,"Ascending");press(s,"Apply filters")
            assertEquals(listOf("shift-card:s2","shift-card:s1"),cardIds(s))
            press(s,"Filters / search");choose(s,"Descending");press(s,"Apply filters");assertEquals(listOf("shift-card:s1","shift-card:s2"),cardIds(s))
            s.recreate();waitButton(s,"Copy a date range / week");InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertEquals(listOf("shift-card:s1","shift-card:s2"),cardIds(s));capture("board-sorting");assertTrue(context.getSharedPreferences("security-settings",0).getBoolean("board-descending",false))
        }
    }
    @Test fun v1DatabaseMigrationPreservesDataAndRevision() {
        context.deleteDatabase("security-schedule.db")
        val helper=object:SQLiteOpenHelper(context,"security-schedule.db",null,1) {
            override fun onCreate(db:SQLiteDatabase) {
                db.execSQL("CREATE TABLE metadata (id INTEGER PRIMARY KEY, revision INTEGER NOT NULL)");db.execSQL("INSERT INTO metadata VALUES (1,7)")
                db.execSQL("CREATE TABLE people (id TEXT PRIMARY KEY,name TEXT NOT NULL,phone TEXT NOT NULL)")
                db.execSQL("CREATE TABLE posts (id TEXT PRIMARY KEY,name TEXT NOT NULL,notes TEXT NOT NULL)")
                db.execSQL("CREATE TABLE shifts (id TEXT PRIMARY KEY,post_id TEXT NOT NULL REFERENCES posts(id),label TEXT NOT NULL,start TEXT NOT NULL,end TEXT NOT NULL,zone TEXT NOT NULL,required INTEGER NOT NULL,notes TEXT NOT NULL)")
                db.execSQL("CREATE TABLE assignments (shift_id TEXT NOT NULL REFERENCES shifts(id),person_id TEXT NOT NULL REFERENCES people(id),PRIMARY KEY(shift_id,person_id))")
                db.execSQL("INSERT INTO people VALUES ('a','Alex','')");db.execSQL("INSERT INTO posts VALUES ('h','Hotel','')")
                db.execSQL("INSERT INTO shifts VALUES ('old','h','Night','2026-10-01T18:00','2026-10-02T06:00','America/New_York',1,'')")
                db.execSQL("INSERT INTO assignments VALUES ('old','a')")
            }
            override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int) {error("Unexpected")}
        }
        helper.writableDatabase;helper.close()
        ScheduleStore(context).use {store->val snapshot=store.read();assertEquals(7L,snapshot.revision);assertEquals(1,snapshot.roster.assignments.size);assertTrue(snapshot.roster.templates.isEmpty());store.save(7,ShiftTemplates.put(snapshot.roster,template));assertEquals(8L,store.read().revision);assertEquals("old",store.read().roster.shifts.single().id)}
    }
    @Test fun templateEncryptedBackupAndRestoreRoundTrip() {
        val r=initial.copy(templates=listOf(template,template.copy(id="generic",name="Generic",postId=null,weekdays=setOf(1,5))))
        val password="test-only-password".toCharArray();val bytes=ScheduleBackup.encrypt(r,password)
        val decoded=ScheduleBackup.decrypt(bytes,password);password.fill('\u0000');assertEquals(r,decoded)
        ScheduleStore(context).use {it.save(it.read().revision,decoded)}
        assertEquals(r.templates.toSet(),read().roster.templates.toSet());assertEquals(initial.shifts.toSet(),read().roster.shifts.toSet())
    }
    @Test fun legacyBackupAndMalformedTemplateBackupHandledSafely() {
        val legacy=JSONObject(String(ScheduleBackup.encode(initial),Charsets.UTF_8)).apply {put("version",1);remove("templates")}
        assertEquals(initial,ScheduleBackup.decode(legacy.toString().toByteArray()))
        val broken=JSONObject(String(ScheduleBackup.encode(initial.copy(templates=listOf(template))),Charsets.UTF_8))
        broken.getJSONArray("templates").getJSONObject(0).put("required",0)
        val before=read();assertTrue(runCatching {ScheduleBackup.decode(broken.toString().toByteArray())}.isFailure);assertEquals(before,read())
    }
    @Test fun staleTemplateSaveAndReferencedPostDeletionRejected() {
        val before=read();ScheduleStore(context).use {it.save(before.revision,ShiftTemplates.put(before.roster,template))}
        val after=read()
        ScheduleStore(context).use {store->assertTrue(runCatching {store.save(before.revision,before.roster)}.isFailure)}
        assertEquals(after,read());assertTrue(runCatching {ShiftTemplates.put(Roster(),template)}.isFailure)
    }
}
