package org.fossify.calendar.security

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.UUID

// The scheduling app has no INTERNET permission. The user's separate SMS app/carrier handles transport.
object SmsSharing {
    fun compose(activity:Activity,text:String,phone:String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",phone.trim(),null)).putExtra("sms_body",text))
        } catch(e:ActivityNotFoundException) {
            throw IllegalArgumentException("No SMS composer is available on this device. Use Copy, Share or TXT export and transfer the schedule to a messaging-capable phone.",e)
        }
    }
    fun direct(activity:Activity,roster:Roster,shifts:List<Shift>,person:Person?) {
        val prefs=activity.getSharedPreferences("security-settings",Context.MODE_PRIVATE)
        require(prefs.getBoolean("direct-sms",false)) {"Direct SMS is off. Enable it in Settings only when you want the app to send after your final confirmation."}
        require(activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {"This tablet/phone has no carrier SMS hardware. Use Copy or export."}
        if(activity.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.SEND_SMS),512)
            Toast.makeText(activity,"After granting permission, press automatic SMS again to review recipients.",Toast.LENGTH_LONG).show();return
        }
        val subscription=SubscriptionManager.getDefaultSmsSubscriptionId()
        require(SubscriptionManager.isValidSubscriptionId(subscription)) {"Choose a default SMS SIM in Android settings, then retry. No messages were sent."}
        @Suppress("DEPRECATION") val manager=SmsManager.getSmsManagerForSubscriptionId(subscription)
        val people=if(person!=null) listOf(person) else roster.people.filter {p -> shifts.any {s->roster.assignments.any {it.shiftId==s.id&&it.personId==p.id}}}
        require(people.size in 1..20) {"Select 1–20 recipients at a time."}
        data class Message(val name:String,val phone:String,val parts:ArrayList<String>)
        val messages=people.map {p ->
            val number=p.phone.replace(Regex("[\\s()\\-]"),"")
            require(Regex("\\+?[0-9]{7,15}").matches(number)) {"Enter a valid full SMS phone number for ${p.name}. No messages sent."}
            val personal=shifts.filter {s->roster.assignments.any {it.shiftId==s.id&&it.personId==p.id}}
            val parts=manager.divideMessage(ScheduleText.format(roster,personal,p.id,true))
            require(parts.size<=20) {"${p.name}'s schedule is more than 20 SMS segments. Select a shorter date range or export TXT."}
            Message(p.name,number,parts)
        }
        require(messages.sumOf {it.parts.size}<=100) {"This batch exceeds 100 SMS segments. Reduce the range/recipient count."}
        val review=messages.joinToString("\n\n") {m -> "${m.name} · ${m.phone} · ${m.parts.size} SMS segments\n"+m.parts.joinToString("")}
        val view=TextView(activity).apply {text="Send ${messages.size} individual messages (${messages.sumOf {it.parts.size}} carrier SMS segments)? Carrier charges may apply. Each person receives only their own assignments. No automatic retry or delivery guarantee.\n\n$review";textSize=16f;setPadding(24,16,24,16);setTextIsSelectable(true)}
        AlertDialog.Builder(activity).setTitle("Confirm automatic SMS batch").setView(ScrollView(activity).apply {addView(view)}).setNegativeButton("Cancel",null).setPositiveButton("Send now") {_,_ ->
            var submitted=0
            try {
                messages.forEach {m ->
                    val sent=ArrayList<PendingIntent>()
                    m.parts.forEach {_->sent.add(PendingIntent.getBroadcast(activity,0,Intent(activity,ShiftReceiver::class.java).setAction("SCAL_SMS_RESULT").setData(Uri.parse("scal-sms:"+UUID.randomUUID())),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))}
                    manager.sendMultipartTextMessage(m.phone,null,m.parts,sent,null);submitted++
                }
                prefs.edit().putString("sms-status","Submitted $submitted recipient messages to Android; carrier delivery not verified.").apply()
                Toast.makeText(activity,"Submitted $submitted messages. Delivery is not verified.",Toast.LENGTH_LONG).show()
            } catch(e:Exception) {
                prefs.edit().putString("sms-status","Batch stopped after $submitted submissions. Some messages may already have been sent. No automatic retry.").apply()
                AlertDialog.Builder(activity).setTitle("SMS batch stopped").setMessage("$submitted messages were submitted before an error. Some may already have been sent. Check your device/carrier before retrying; the app will not retry automatically.").setPositiveButton("OK",null).show()
            }
        }.show()
    }
}
