package org.fossify.calendar.security

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// GPL-3.0-or-later. Normalized, application-private storage; no OS calendar provider.
data class StoredRoster(val revision: Long, val roster: Roster)
class ScheduleStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "security-schedule.db", null, 2) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE metadata (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL)")
        db.execSQL("INSERT INTO metadata VALUES (1,0)")
        db.execSQL("CREATE TABLE people (id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT NOT NULL)")
        db.execSQL("CREATE TABLE posts (id TEXT PRIMARY KEY, name TEXT NOT NULL, notes TEXT NOT NULL)")
        db.execSQL("CREATE TABLE shifts (id TEXT PRIMARY KEY, post_id TEXT NOT NULL REFERENCES posts(id), label TEXT NOT NULL, start TEXT NOT NULL, end TEXT NOT NULL, zone TEXT NOT NULL, required INTEGER NOT NULL CHECK(required BETWEEN 1 AND 100), notes TEXT NOT NULL, UNIQUE(post_id,start,end,zone))")
        db.execSQL("CREATE TABLE assignments (shift_id TEXT NOT NULL REFERENCES shifts(id) ON DELETE CASCADE, person_id TEXT NOT NULL REFERENCES people(id), PRIMARY KEY(shift_id,person_id))")
        db.execSQL("CREATE INDEX assignments_person ON assignments(person_id)")
        db.execSQL("CREATE INDEX shifts_start ON shifts(start)")
        createTemplates(db)
    }
    private fun createTemplates(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE templates (id TEXT PRIMARY KEY, name TEXT NOT NULL, label TEXT NOT NULL, start TEXT NOT NULL, end TEXT NOT NULL, zone TEXT NOT NULL, required INTEGER NOT NULL CHECK(required BETWEEN 1 AND 100), post_id TEXT REFERENCES posts(id), weekdays TEXT NOT NULL, notes TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion == 1 && newVersion == 2) { "Unsupported database upgrade $oldVersion to $newVersion. Existing data was not erased." }
        createTemplates(db) // SQLiteOpenHelper runs this migration transactionally; no old data is deleted.
    }
    private fun revision(db: SQLiteDatabase): Long = db.rawQuery("SELECT revision FROM metadata WHERE id=1", null).use { c -> check(c.moveToFirst()); c.getLong(0) }
    fun read(): StoredRoster {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val people = mutableListOf<Person>(); val posts = mutableListOf<Post>(); val shifts = mutableListOf<Shift>(); val assignments = mutableListOf<Assignment>(); val templates = mutableListOf<ShiftTemplate>()
            db.rawQuery("SELECT id,name,phone FROM people ORDER BY name", null).use { c -> while(c.moveToNext()) people.add(Person(c.getString(0),c.getString(1),c.getString(2))) }
            db.rawQuery("SELECT id,name,notes FROM posts ORDER BY name", null).use { c -> while(c.moveToNext()) posts.add(Post(c.getString(0),c.getString(1),c.getString(2))) }
            db.rawQuery("SELECT id,post_id,label,start,end,zone,required,notes FROM shifts ORDER BY start,post_id", null).use { c -> while(c.moveToNext()) shifts.add(Shift(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getInt(6),c.getString(7))) }
            db.rawQuery("SELECT shift_id,person_id FROM assignments ORDER BY shift_id,person_id", null).use { c -> while(c.moveToNext()) assignments.add(Assignment(c.getString(0),c.getString(1))) }
            db.rawQuery("SELECT id,name,label,start,end,zone,required,post_id,weekdays,notes FROM templates ORDER BY name,id", null).use { c ->
                while(c.moveToNext()) templates.add(ShiftTemplate(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getInt(6),if(c.isNull(7)) null else c.getString(7),c.getString(8).split(',').map(String::toInt).toSet(),c.getString(9)))
            }
            val result = StoredRoster(revision(db), Roster(people,posts,shifts,assignments,templates).validate())
            db.setTransactionSuccessful()
            return result
        } finally { db.endTransaction() }
    }
    fun save(expectedRevision: Long, roster: Roster) {
        roster.validate()
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(revision(db) == expectedRevision) { "The schedule changed after this preview. Reload and review again; nothing was overwritten." }
            listOf("assignments","shifts","templates","people","posts").forEach { db.delete(it,null,null) }
            fun insert(table: String, vararg fields: Pair<String,Any?>) {
                val values = ContentValues()
                fields.forEach { (k,v) -> when(v) { null -> values.putNull(k); is Int -> values.put(k,v); is Long -> values.put(k,v); else -> values.put(k,v.toString()) } }
                db.insertOrThrow(table,null,values)
            }
            roster.people.forEach { insert("people","id" to it.id,"name" to it.name,"phone" to it.phone) }
            roster.posts.forEach { insert("posts","id" to it.id,"name" to it.name,"notes" to it.notes) }
            roster.templates.forEach { insert("templates","id" to it.id,"name" to it.name,"label" to it.label,"start" to it.start,"end" to it.end,"zone" to it.zone,"required" to it.required,"post_id" to it.postId,"weekdays" to it.weekdays.sorted().joinToString(","),"notes" to it.notes) }
            roster.shifts.forEach { insert("shifts","id" to it.id,"post_id" to it.postId,"label" to it.label,"start" to it.start,"end" to it.end,"zone" to it.zone,"required" to it.required,"notes" to it.notes) }
            roster.assignments.forEach { insert("assignments","shift_id" to it.shiftId,"person_id" to it.personId) }
            db.execSQL("UPDATE metadata SET revision=revision+1 WHERE id=1")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}

object ScheduleBackup {
    private const val MAX_FILE_BYTES = 10*1024*1024
    private val magic = "SCAL1".toByteArray(Charsets.US_ASCII)
    private fun array(values: List<JSONObject>) = JSONArray().apply { values.forEach { put(it) } }
    private fun obj(vararg values: Pair<String,Any>) = JSONObject().apply { values.forEach { put(it.first,it.second) } }
    fun encode(r: Roster): ByteArray {
        r.validate()
        val bytes = obj("format" to "OfflineSecurityCalendar", "version" to 2,
            "people" to array(r.people.map { obj("id" to it.id,"name" to it.name,"phone" to it.phone) }),
            "posts" to array(r.posts.map { obj("id" to it.id,"name" to it.name,"notes" to it.notes) }),
            "shifts" to array(r.shifts.map { obj("id" to it.id,"postId" to it.postId,"label" to it.label,"start" to it.start,"end" to it.end,"zone" to it.zone,"required" to it.required,"notes" to it.notes) }),
            "templates" to array(r.templates.map { obj("id" to it.id,"name" to it.name,"label" to it.label,"start" to it.start,"end" to it.end,"zone" to it.zone,"required" to it.required,"postId" to (it.postId ?: JSONObject.NULL),"weekdays" to JSONArray(it.weekdays.sorted()),"notes" to it.notes) }),
            "assignments" to array(r.assignments.map { obj("shiftId" to it.shiftId,"personId" to it.personId) })
        ).toString().toByteArray(Charsets.UTF_8)
        // Header + salt + nonce + GCM tag are 49 bytes. Never create an unrestorable oversized backup.
        if (bytes.size > MAX_FILE_BYTES-49) {
            bytes.fill(0)
            throw IllegalArgumentException("This preview supports backups up to 10 MB. No backup was created; your local schedule is unchanged.")
        }
        return bytes
    }
    fun decode(bytes: ByteArray): Roster {
        require(bytes.size <= MAX_FILE_BYTES) { "Backup is too large." }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val version = root.getInt("version")
        require(root.getString("format") == "OfflineSecurityCalendar" && version in 1..2) { "Unsupported backup format." }
        fun rows(key: String, limit: Int): List<JSONObject> { val a=root.getJSONArray(key); require(a.length()<=limit); return (0 until a.length()).map { a.getJSONObject(it) } }
        fun JSONObject.text(key: String): String { require(opt(key) is String) { "Invalid backup field: $key" }; return getString(key) }
        return Roster(
            rows("people",2000).map { Person(it.text("id"),it.text("name"),it.text("phone")) },
            rows("posts",2000).map { Post(it.text("id"),it.text("name"),it.text("notes")) },
            rows("shifts",20000).map { require(it.opt("required") is Int); Shift(it.text("id"),it.text("postId"),it.text("label"),it.text("start"),it.text("end"),it.text("zone"),it.getInt("required"),it.text("notes")) },
            rows("assignments",100000).map { Assignment(it.text("shiftId"),it.text("personId")) },
            if (version == 1) { require(!root.has("templates")) { "Unexpected template data in a version 1 backup." }; emptyList() }
            else rows("templates",2000).map {
                require(it.opt("required") is Int)
                val days=it.getJSONArray("weekdays")
                require(days.length() in 1..7)
                val values=(0 until days.length()).map { i -> require(days.get(i) is Int); days.getInt(i) }
                require(values.distinct().size==values.size) { "Duplicate template weekday." }
                require(it.has("postId")) { "Missing template post field." }
                ShiftTemplate(it.text("id"),it.text("name"),it.text("label"),it.text("start"),it.text("end"),it.text("zone"),it.getInt("required"),if(it.isNull("postId")) null else it.text("postId"),values.toSet(),it.text("notes"))
            }
        ).validate()
    }
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password,salt,210000,256)
        return try { val bytes=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded; try { SecretKeySpec(bytes,"AES") } finally { bytes.fill(0) } } finally { spec.clearPassword() }
    }
    fun encrypt(r: Roster, password: CharArray): ByteArray {
        require(password.size >= 10) { "Use a backup password with at least 10 characters. It cannot be recovered." }
        val random=SecureRandom(); val salt=ByteArray(16).also(random::nextBytes); val nonce=ByteArray(12).also(random::nextBytes)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,nonce)); cipher.updateAAD(magic)
        val plain=encode(r)
        return try { magic+salt+nonce+cipher.doFinal(plain) } finally { plain.fill(0) }
    }
    fun decrypt(bytes: ByteArray, password: CharArray): Roster {
        require(bytes.size in 49..MAX_FILE_BYTES && bytes.copyOfRange(0,5).contentEquals(magic)) { "Not a supported encrypted schedule backup." }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(password,bytes.copyOfRange(5,21)),GCMParameterSpec(128,bytes.copyOfRange(21,33))); cipher.updateAAD(magic)
        val plain=try { cipher.doFinal(bytes.copyOfRange(33,bytes.size)) } catch(e: Exception) { throw IllegalArgumentException("Wrong password or damaged backup. Existing data is unchanged.",e) }
        return try { decode(plain) } finally { plain.fill(0) }
    }
    fun readBounded(input: InputStream): ByteArray {
        val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val count=input.read(buffer); if(count<0) break; require(output.size()+count<=MAX_FILE_BYTES) { "File exceeds 10 MB." }; output.write(buffer,0,count) }
        return output.toByteArray()
    }
}
