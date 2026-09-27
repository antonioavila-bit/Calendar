package org.fossify.calendar.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.io.File
import android.graphics.Bitmap

@RunWith(AndroidJUnit4::class)
class ScheduleDeviceTest {
    private lateinit var context:Context
    @Before fun clean() {
        context=ApplicationProvider.getApplicationContext()
        context.deleteDatabase("security-schedule.db")
        context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun sample():Roster {
        val p=Person("p1","Álex","");val post=Post("l1","Hotel")
        val a=ScheduleEntry.shift(LocalDate.of(2026,10,1),post.id,"Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",2)
        val b=ScheduleEntry.shift(LocalDate.of(2026,10,2),post.id,"Night",LocalTime.of(18,0),LocalTime.of(6,0),"America/New_York",2)
        return Roster(listOf(p),listOf(post),listOf(a,b),listOf(Assignment(a.id,p.id))).validate()
    }
    private fun rejects(body:()->Unit) { assertTrue(runCatching(body).isFailure) }
    @Test fun databaseRoundTrip() {
        val r=sample();ScheduleStore(context).use {it.save(0,r)}
        ScheduleStore(context).use {val loaded=it.read();assertEquals(1,loaded.revision);assertEquals(r.shifts.map {s->s.key()},loaded.roster.shifts.map {s->s.key()});assertEquals(3,loaded.roster.shifts.sumOf {s->loaded.roster.open(s)});assertEquals("Álex",loaded.roster.people.single().name)}
    }
    @Test fun staleEditCannotOverwrite() {
        ScheduleStore(context).use {s->s.save(0,sample());rejects {s.save(0,Roster())};assertEquals(2,s.read().roster.shifts.size);assertEquals(1,s.read().revision)}
    }
    @Test fun invalidSnapshotLeavesDatabaseIntact() {
        ScheduleStore(context).use {s->val r=sample();s.save(0,r);rejects {s.save(1,r.copy(assignments=listOf(Assignment("missing","p1"))))};assertEquals(2,s.read().roster.shifts.size);assertEquals(1,s.read().revision)}
    }
    @Test fun encryptedUnicodeRoundTrip() {
        val r=sample();val bytes=ScheduleBackup.encrypt(r,"a strong test password".toCharArray());val decoded=ScheduleBackup.decrypt(bytes,"a strong test password".toCharArray());assertEquals(r,decoded);assertFalse(bytes.toString(Charsets.UTF_8).contains("Álex"))
    }
    @Test fun wrongPasswordDoesNotDecrypt() {val bytes=ScheduleBackup.encrypt(sample(),"a strong test password".toCharArray());rejects {ScheduleBackup.decrypt(bytes,"another wrong password".toCharArray())}}
    @Test fun alteredBackupDoesNotDecrypt() {val bytes=ScheduleBackup.encrypt(sample(),"a strong test password".toCharArray());bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte();rejects {ScheduleBackup.decrypt(bytes,"a strong test password".toCharArray())}}
    @Test fun backupRejectsInvalidReference() {val bad=ScheduleBackup.encode(sample()).toString(Charsets.UTF_8).replace("\"personId\":\"p1\"","\"personId\":\"missing\"");rejects {ScheduleBackup.decode(bad.toByteArray())}}
    @Test fun networkAndCalendarPermissionsAbsent() {
        @Suppress("DEPRECATION") val p=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_PERMISSIONS)
        val permissions=p.requestedPermissions.orEmpty().toSet()
        assertFalse("android.permission.INTERNET" in permissions)
        assertFalse("android.permission.READ_CALENDAR" in permissions)
        assertFalse("android.permission.WRITE_CALENDAR" in permissions)
        assertEquals(0,context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
    private fun all(root:View):List<View> = listOf(root)+if(root is ViewGroup) (0 until root.childCount).flatMap {all(root.getChildAt(it))} else emptyList()
    private fun waitFor(s:ActivityScenario<ShiftActivity>,label:String) {
        repeat(100) {
            // Wait for an actionable button, not the identically named Add person form title.
            // The list button is recreated only after the asynchronous database save finishes.
            var found=false;s.onActivity {a->found=all(a.window.decorView).filterIsInstance<Button>().any {v->v.isShown && v.text.toString()==label}}
            if(found) return
            SystemClock.sleep(100)
        }
        fail("Did not find UI label: $label")
    }
    private fun press(s:ActivityScenario<ShiftActivity>,label:String) {s.onActivity {a->
        var target=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}
        if(!target.isShown) {
            all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()=="Menu"}.performClick()
            target=all(a.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}
        }
        assertTrue("Navigation target is hidden: $label",target.isShown)
        target.performClick()
    }}
    @Test fun launcherAndPersonEntryPersistWithoutNetwork() {
        ActivityScenario.launch(ShiftActivity::class.java).use {s->
            waitFor(s,"Required shifts");press(s,"Personnel");waitFor(s,"Add person");press(s,"Add person");waitFor(s,"Save person")
            s.onActivity {a->all(a.window.decorView).filterIsInstance<EditText>().first {it.hint.toString()=="Name"}.setText("Test Person")}
            press(s,"Save person");waitFor(s,"Add person")
            ScheduleStore(context).use {assertEquals("Test Person",it.read().roster.people.single().name)}
            press(s,"Calendar");waitFor(s,"Required shifts")
            val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
            val screenshot=automation.takeScreenshot()
            if(screenshot!=null) {File(context.getExternalFilesDir(null),"shift-calendar-screen.png").outputStream().use {screenshot.compress(Bitmap.CompressFormat.PNG,100,it)};screenshot.recycle()}
        }
    }
    @Test fun dataSurvivesActivityRecreation() {
        ScheduleStore(context).use {it.save(0,sample())}
        ActivityScenario.launch(ShiftActivity::class.java).use {s->waitFor(s,"Required shifts");s.recreate();waitFor(s,"Required shifts");ScheduleStore(context).use {assertEquals(2,it.read().roster.shifts.size)}}
    }
}
