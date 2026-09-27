package org.fossify.calendar.security

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.time.Instant

object ShiftNotifications {
    private const val CHANNEL="shift-reminders"
    fun requestPermission(activity:Activity) {
        if(Build.VERSION.SDK_INT>=33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),513)
    }
    private fun pending(context:Context,id:String):PendingIntent = PendingIntent.getBroadcast(context,0,
        Intent(context,ShiftReceiver::class.java).setAction("SCAL_REMINDER").setData(Uri.parse("scal-shift:$id")).putExtra("shiftId",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun reschedule(context:Context,r:Roster) {
        val prefs=context.getSharedPreferences("security-settings",Context.MODE_PRIVATE)
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs.getStringSet("reminder-ids",emptySet()).orEmpty().forEach {alarm.cancel(pending(context,it))}
        if(!prefs.getBoolean("reminders",false)) {prefs.edit().putStringSet("reminder-ids",emptySet()).apply();return}
        val now=Instant.now();val lead=prefs.getInt("reminder-minutes",30).toLong()
        val upcoming=r.shifts.filter {val due=it.startTime().toInstant().minusSeconds(lead*60);due.isAfter(now)&&due.isBefore(now.plusSeconds(90L*86400))}
        upcoming.forEach {s ->
            val due=s.startTime().toInstant().minusSeconds(lead*60).toEpochMilli()
            if(Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,pending(context,s.id))
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,pending(context,s.id))
        }
        prefs.edit().putStringSet("reminder-ids",upcoming.map {it.id}.toSet()).apply()
    }
    fun notify(context:Context,roster:Roster,id:String) {
        val prefs=context.getSharedPreferences("security-settings",Context.MODE_PRIVATE)
        if(!prefs.getBoolean("reminders",false)) return
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        val shift=roster.shifts.firstOrNull {it.id==id}?:return
        val now=Instant.now();val due=shift.startTime().toInstant().minusSeconds(prefs.getInt("reminder-minutes",30)*60L)
        if(now.isBefore(due)||now.isAfter(shift.endTime().toInstant())) return
        val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Shift reminders",NotificationManager.IMPORTANCE_DEFAULT))
        val content="${roster.posts.first {it.id==shift.postId}.name} · ${shift.label} · ${shift.startTime().toLocalTime()} · ${roster.coverage(shift)}"
        val open=PendingIntent.getActivity(context,0,Intent(context,ShiftActivity::class.java).setAction(Intent.ACTION_MAIN).setData(Uri.parse("scal-open:$id")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Shift Calendar").setContentText(content).setStyle(Notification.BigTextStyle().bigText(content)).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build()
        manager.notify(id,0,notification)
    }
}

class ShiftReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action=="SCAL_SMS_RESULT") {
            val status=if(resultCode==Activity.RESULT_OK) "Most recent SMS segment sent to carrier; delivery not confirmed." else "An SMS segment failed (code $resultCode). Check before retrying; no automatic retry."
            context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().putString("sms-status",status).apply();return
        }
        if(intent.action !in setOf("SCAL_REMINDER",Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED)) return
        val pending=goAsync()
        Thread {
            val store=ScheduleStore(context)
            try {
                val r=store.read().roster
                if(intent.action=="SCAL_REMINDER") intent.getStringExtra("shiftId")?.let {ShiftNotifications.notify(context,r,it)}
                else ShiftNotifications.reschedule(context,r)
            } catch(_:Exception) {
                context.getSharedPreferences("security-settings",Context.MODE_PRIVATE).edit().putString("reminder-error","A reminder could not be processed. Open the app to check the schedule.").apply()
            } finally {store.close();pending.finish()}
        }.start()
    }
}
