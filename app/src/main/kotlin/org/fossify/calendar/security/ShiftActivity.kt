package org.fossify.calendar.security

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.fossify.calendar.BuildConfig
import org.json.JSONObject
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

// GPL-3.0-or-later. Dedicated shift workspace; requirements are not inferred from ordinary calendar events.
class ShiftActivity : Activity() {
    private lateinit var body: LinearLayout
    private lateinit var store: ScheduleStore
    private val io = Executors.newSingleThreadExecutor()
    private var current = StoredRoster(0,Roster())
    private var ready = false
    private var busy = false
    private var screen = "calendar"
    private var month = YearMonth.now()
    private var day: LocalDate? = null
    private val drafts = mutableMapOf<String,Map<String,String>>()
    private val controls = linkedMapOf<String,()->String>()
    private var pendingFile: String? = null
    private var pendingName: String? = null
    private val prefs by lazy { getSharedPreferences("security-settings",MODE_PRIVATE) }
    private val navy = Color.rgb(12,43,77)
    private fun dp(n: Int) = (resources.displayMetrics.density*n).toInt()
    private val r get() = current.roster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime=insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left,bars.top,bars.right,maxOf(bars.bottom,ime.bottom)); insets
        }
        body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(24)) }
        val scroll=ScrollView(this).apply { isFillViewport=true; addView(body) }
        root.addView(scroll,LinearLayout.LayoutParams(-1,-1)); setContentView(root)
        savedInstanceState?.let { b ->
            screen=b.getString("screen","calendar"); month=YearMonth.parse(b.getString("month",month.toString()))
            day=b.getString("day")?.let(LocalDate::parse);pendingFile=b.getString("pendingFile");pendingName=b.getString("pendingName")
            runCatching { val all=JSONObject(b.getString("drafts","{}"));all.keys().forEach { key -> val fields=all.getJSONObject(key);drafts[key]=fields.keys().asSequence().associateWith { fields.getString(it) } } }
        }
        val destination=screen
        store=ScheduleStore(applicationContext)
        text("Opening Shift Calendar…",22)
        background({store.read()}) { result ->
            current=result;ready=true;render(destination)
            ShiftNotifications.reschedule(this,r)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        rememberDraft()
        outState.putString("screen",screen);outState.putString("month",month.toString());outState.putString("day",day?.toString())
        outState.putString("pendingFile",pendingFile);outState.putString("pendingName",pendingName)
        val all=JSONObject(); drafts.forEach { (key,fields) -> all.put(key,JSONObject(fields)) };outState.putString("drafts",all.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { io.execute { store.close() };io.shutdown();super.onDestroy() }
    @Deprecated("Legacy back navigation")
    override fun onBackPressed() { if(screen!="calendar") calendar() else super.onBackPressed() }
    private fun rememberDraft() { if(controls.isNotEmpty()) drafts[screen]=controls.mapValues { it.value() } }
    private fun render(target: String) {
        when {
            target=="people" -> people()
            target=="posts" -> posts()
            target=="templates" -> templates()
            target.startsWith("template-use:") -> r.templates.firstOrNull { it.id==target.substringAfter(':') }?.let(::useTemplate) ?: templates()
            target.startsWith("template:") -> editTemplate(r.templates.firstOrNull { it.id==target.substringAfter(':') })
            target=="bulk" -> bulk()
            target=="required" -> required()
            target=="export" -> export()
            target=="settings" -> settings()
            target=="board" -> board()
            target=="uncovered" -> board(true)
            target.startsWith("person:") -> editPerson(r.people.firstOrNull { it.id==target.substringAfter(':') })
            target.startsWith("post:") -> editPost(r.posts.firstOrNull { it.id==target.substringAfter(':') })
            target.startsWith("shift:") -> r.shifts.firstOrNull { it.id==target.substringAfter(':') }?.let(::editShift) ?: board()
            else -> calendar()
        }
    }
    private fun page(title:String,tag:String) {
        rememberDraft();controls.clear();screen=tag;body.removeAllViews()
        val header=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(applicationInfo.icon);contentDescription="Shift Calendar" },LinearLayout.LayoutParams(dp(40),dp(40)))
        header.addView(TextView(this).apply { text="Shift Calendar\n${BuildConfig.VERSION_NAME}";textSize=16f;setTextColor(navy);setPadding(dp(8),0,dp(4),0) },LinearLayout.LayoutParams(0,-2,1f))
        body.addView(header)
        val navigation=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=View.GONE}
        button("Menu",header) {navigation.visibility=if(navigation.visibility==View.VISIBLE) View.GONE else View.VISIBLE}.apply {
            minWidth=0;minimumWidth=0;setPadding(dp(4),0,dp(4),0);contentDescription="Show or hide navigation menu"
            layoutParams=LinearLayout.LayoutParams(dp(72),dp(48))
        }
        body.addView(navigation)
        buttons("Calendar" to {calendar()},"Schedule" to {board()},"Uncovered" to {board(true)},"Personnel" to {people()},"Posts" to {posts()},"Templates" to {templates()},"Share / TXT" to {export()},"Settings / backup" to {settings()},"Enter schedules" to {bulk()},parent=navigation)
        text(title,20,true)
    }
    private fun text(value:String,size:Int=16,bold:Boolean=false,parent:LinearLayout=body):TextView = TextView(this).also {
        it.text=value;it.textSize=size.toFloat();it.setTextColor(navy);it.setPadding(0,dp(7),0,dp(7));it.setTextIsSelectable(true)
        if(bold) it.setTypeface(null,Typeface.BOLD);parent.addView(it,LinearLayout.LayoutParams(-1,-2))
    }
    private fun button(label:String,parent:LinearLayout=body,action:()->Unit):Button = Button(this).also {
        it.text=label;it.isAllCaps=false;it.minHeight=dp(48);it.setOnClickListener { if(!busy && ready) guard(action) }
        parent.addView(it,LinearLayout.LayoutParams(-1,-2))
    }
    private fun buttons(vararg entries:Pair<String,()->Unit>,parent:LinearLayout=body) {
        entries.toList().chunked(2).forEach { pair ->
            val row=LinearLayout(this);parent.addView(row,LinearLayout.LayoutParams(-1,-2))
            pair.forEach { (label,action) -> button(label,row,action).layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        }
    }
    private fun field(label:String,value:String="",multi:Boolean=false,type:Int=InputType.TYPE_CLASS_TEXT,parent:LinearLayout=body):EditText {
        text(label,14,true,parent)
        val e=EditText(this).apply {
            id=View.generateViewId();setText(drafts[screen]?.get(label)?:value);hint=label;textSize=17f
            inputType=type or (if(multi) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
            if(multi) { minLines=4;maxLines=12;gravity=Gravity.TOP } else setSingleLine(true)
            filters=arrayOf(InputFilter.LengthFilter(if(label=="Schedule entries") 64000 else 8000))
            setPadding(dp(10),dp(10),dp(10),dp(10));setTextColor(Color.BLACK)
            HandwritingInput.attach(this) { prefs.getBoolean("handwriting",true) }
        }
        parent.addView(e,LinearLayout.LayoutParams(-1,-2));controls[label]={e.text.toString()};return e
    }
    private fun spinner(label:String,values:List<String>,parent:LinearLayout=body):Spinner {
        text(label,14,true,parent)
        val s=Spinner(this).apply { adapter=ArrayAdapter(this@ShiftActivity,android.R.layout.simple_spinner_dropdown_item,values);minimumHeight=dp(48) }
        values.indexOf(drafts[screen]?.get(label)).takeIf { it>=0 }?.let { s.setSelection(it) }
        parent.addView(s,LinearLayout.LayoutParams(-1,-2));controls[label]={s.selectedItem?.toString().orEmpty()};return s
    }
    private fun check(label:String,initial:Boolean=false):CheckBox = CheckBox(this).also {
        it.text=label;it.textSize=16f;it.isChecked=drafts[screen]?.get(label)?.toBooleanStrictOrNull()?:initial;it.minHeight=dp(48)
        body.addView(it,LinearLayout.LayoutParams(-1,-2));controls[label]={it.isChecked.toString()}
    }
    private fun keyboard() { val view=currentFocus;view?.clearFocus();(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view?.windowToken?:body.windowToken,0) }
    private fun formActions() { buttons("Hide keyboard" to {keyboard()},"Cancel / calendar" to {calendar()}) }
    private fun guard(action:()->Unit) { try { action() } catch(e:Exception) { message(e.message?:"The operation could not be completed. Existing data is unchanged.") } }
    private fun message(value:String) { AlertDialog.Builder(this).setTitle("Shift Calendar").setMessage(value).setPositiveButton("OK",null).show() }
    private fun ask(title:String,value:String,yes:String="Confirm",action:()->Unit) {
        val content=TextView(this).apply { text=value;textSize=16f;setPadding(dp(18),dp(12),dp(18),dp(12));setTextIsSelectable(true) }
        AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply {addView(content)}).setNegativeButton("Cancel",null).setPositiveButton(yes) { _,_ -> guard(action) }.show()
    }
    private fun <T> background(work:()->T,done:(T)->Unit) {
        if(busy) return
        busy=true
        io.execute {
            val result=runCatching(work)
            runOnUiThread {
                busy=false
                if(!isDestroyed && !isFinishing) result.fold(onSuccess={done(it)},onFailure={message(it.message?:"Operation failed. Existing data was not erased.")})
            }
        }
    }
    private fun save(next:Roster,revision:Long=current.revision,after:()->Unit={board()}) {
        val oldScreen=screen
        background({store.save(revision,next);store.read()}) {
            current=it;drafts.remove(oldScreen);controls.clear();ShiftNotifications.reschedule(this,r);after()
            Toast.makeText(this,"Saved on this device",Toast.LENGTH_SHORT).show()
        }
    }
    private fun review(next:Roster,affected:List<Shift>,description:String) {
        next.validate();require(affected.size<=500) { "Review at most 500 shifts at a time. Split this batch." }
        val revision=current.revision
        ask("Review before saving",description+"\n\n"+ScheduleText.format(next,affected),"Save all") { save(next,revision) }
    }
    private fun monthShifts() = r.shifts.filter { YearMonth.from(it.startTime())==month }
    private fun summary(shifts:List<Shift>,parent:LinearLayout=body) {
        if(shifts.isEmpty()) text("No required shifts in this selection. Add requirements first; a blank calendar does not prove coverage.",parent=parent)
        else text("${shifts.size} shifts · ${shifts.count { r.open(it)>0 }} uncovered · ${shifts.sumOf { r.open(it) }} open",15,true,parent)
    }
    private fun calendar() {
        page(month.format(DateTimeFormatter.ofPattern("MMMM uuuu",Locale.US)),"calendar")
        val months=LinearLayout(this);body.addView(months)
        listOf<Pair<String,()->Unit>>("Previous" to {month=month.minusMonths(1);day=null;calendar()},"Today" to {month=YearMonth.now();day=LocalDate.now();calendar()},"Next" to {month=month.plusMonths(1);day=null;calendar()}).forEach { (label,action) ->
            button(label,months,action).apply {minWidth=0;minimumWidth=0;setPadding(dp(2),0,dp(2),0);layoutParams=LinearLayout.LayoutParams(0,dp(48),1f)}
        }
        val shifts=monthShifts();summary(shifts)
        text("! = open positions. Tap a shift's start date.",12).setPadding(0,dp(2),0,dp(2))
        val offset=month.atDay(1).dayOfWeek.value%7
        val headings=LinearLayout(this);body.addView(headings)
        listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { label -> headings.addView(TextView(this).apply { text=label;gravity=Gravity.CENTER;textSize=12f },LinearLayout.LayoutParams(0,dp(24),1f)) }
        val cells=List(offset){0}+(1..month.lengthOfMonth()).toList()
        cells.chunked(7).forEach { week ->
            val row=LinearLayout(this);body.addView(row)
            (0..6).forEach { index ->
                val n=week.getOrElse(index){0}
                val v=Button(this).apply {
                    minWidth=0;minimumWidth=0;minHeight=dp(48);setPadding(0,0,0,0);textSize=13f;isAllCaps=false
                    if(n>0) {
                        val date=month.atDay(n);val open=shifts.filter { it.startTime().toLocalDate()==date }.sumOf { r.open(it) }
                        text=if(open>0) "$n\n!$open" else n.toString();setTextColor(if(open>0) Color.rgb(164,29,42) else navy)
                        contentDescription="$date, $open open positions";isSelected=date==day
                        setOnClickListener { if(!busy) {day=date;calendar()} }
                    } else {text="";visibility=View.INVISIBLE}
                }
                row.addView(v,LinearLayout.LayoutParams(0,dp(48),1f))
            }
        }
        buttons("Required shifts" to {required()},"Enter schedules" to {bulk()})
        day?.let { selected -> text(selected.toString(),20,true);val entries=shifts.filter { it.startTime().toLocalDate()==selected };summary(entries);entries.forEach(::shiftCard) }
        if(day==null) text("Choose a date, or open Schedule to view this month's full roster.")
    }
    private fun board(uncovered:Boolean=false) {
        val tag=if(uncovered) "uncovered" else "board"
        page(if(uncovered) "Uncovered shifts — $month" else "Schedule — $month",tag)
        fun moveMonth(offset:Long) {controls.clear();drafts.remove(tag);month=month.plusMonths(offset);board(uncovered)}
        buttons("Previous month" to {moveMonth(-1)},"Next month" to {moveMonth(1)},"Required shifts" to {required()},"Enter schedules" to {bulk()})
        val filters=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=View.GONE}
        button("Filters / search") {filters.visibility=if(filters.visibility==View.VISIBLE) View.GONE else View.VISIBLE}
        body.addView(filters)
        val from=field("From start date (YYYY-MM-DD)",month.atDay(1).toString(),parent=filters)
        val through=field("Through start date (YYYY-MM-DD)",month.atEndOfMonth().toString(),parent=filters)
        val person=spinner("Filter person",listOf("All personnel")+r.people.map {it.name},filters)
        val post=spinner("Filter post",listOf("All posts")+r.posts.map {it.name},filters)
        val patterns=r.shifts.map(ScheduleFilters::pattern).distinct().sorted()
        val pattern=spinner("Shift / time",listOf("All shifts")+patterns,filters)
        val status=spinner("Coverage status",listOf("All statuses","UNFILLED","PARTIALLY FILLED","FILLED","OVERSTAFFED","CONFLICTS"),filters)
        val search=field("Search names, posts, shifts or notes",parent=filters)
        val sort=spinner("Sort by",BoardSort.values().map {it.title},filters)
        val direction=spinner("Sort direction",listOf("Ascending","Descending"),filters)
        if(drafts[tag]?.get("Sort by")==null) sort.setSelection(BoardSort.from(prefs.getString("board-sort",null)).ordinal)
        if(drafts[tag]?.get("Sort direction")==null) direction.setSelection(if(prefs.getBoolean("board-descending",false)) 1 else 0)
        val list=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL};body.addView(list)
        fun show() {
            val selectedStatus=status.selectedItem.toString()
            val filtered=ScheduleFilters.select(r,LocalDate.parse(from.text.toString().trim()),LocalDate.parse(through.text.toString().trim()),
                r.people.getOrNull(person.selectedItemPosition-1)?.id,r.posts.getOrNull(post.selectedItemPosition-1)?.id,
                patterns.getOrNull(pattern.selectedItemPosition-1),selectedStatus.takeUnless {it=="All statuses"||it=="CONFLICTS"},
                uncovered,selectedStatus=="CONFLICTS",search.text.toString())
            list.removeAllViews()
            text("${from.text} through ${through.text}",14,parent=list)
            summary(filtered,list)
            if(selectedStatus=="CONFLICTS") text("Conflicting assignments are blocked before saving. This filter checks for overlaps in saved assignments.",14,parent=list)
            val order=BoardSort.values()[sort.selectedItemPosition]
            val descending=direction.selectedItemPosition==1
            prefs.edit().putString("board-sort",order.name).putBoolean("board-descending",descending).apply()
            text("Sorted by ${order.title} · ${direction.selectedItem}",14,parent=list)
            ScheduleOrder.sort(r,filtered,order,descending).forEach {shiftCard(it,list)}
        }
        button("Apply filters",filters) {show();keyboard();filters.visibility=View.GONE}
        val listener=object:AdapterView.OnItemSelectedListener {override fun onNothingSelected(parent:AdapterView<*>?) {};override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){guard {show()}}}
        listOf(person,post,pattern,status,sort,direction).forEach {it.onItemSelectedListener=listener};guard {show()}
        button("Copy a date range / week") {copyRange()}
    }
    private fun shiftCard(s:Shift,parent:LinearLayout=body) {
        val card=LinearLayout(this).apply {
            contentDescription="shift-card:${s.id}"
            orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(12))
            background=GradientDrawable().apply {setColor(Color.rgb(242,246,250));cornerRadius=dp(10).toFloat();setStroke(dp(1),Color.rgb(209,218,230))}
        }
        parent.addView(card,LinearLayout.LayoutParams(-1,-2).apply {topMargin=dp(10)})
        text("${s.startTime().toLocalDate()} · ${r.posts.first {it.id==s.postId}.name}",18,true,card)
        val t=DateTimeFormatter.ofPattern("h:mm a",Locale.US)
        text("${s.label}: ${s.startTime().format(t)} → ${s.endTime().toLocalDate()} ${s.endTime().format(t)}\n${s.zone}",15,parent=card)
        text("Assigned: "+r.assigned(s).joinToString("; "){it.name}.ifBlank {"UNASSIGNED"},parent=card)
        text("${r.coverage(s)} · ${r.assigned(s).size}/${s.required} assigned · ${r.open(s)} open",16,true,card)
        if(s.notes.isNotBlank()) text(s.notes,14,parent=card)
        buttons("Assign personnel" to {assign(s)},"Edit shift" to {editShift(s)},parent=card)
        button("Share this shift",card) {
            val text=ScheduleText.format(r,listOf(s),compact=true)
            AlertDialog.Builder(this).setTitle("Share selected shift")
                .setItems(arrayOf("Copy text","Manual SMS","Share text","Send individual SMS (opt-in)")) {_,which -> guard {
                    when(which) {0->copyText(text);1->SmsSharing.compose(this,text,"");2->shareText(text);3->SmsSharing.direct(this,r,listOf(s),null)}
                }}.setNegativeButton("Cancel",null).show()
        }
    }
    private fun people() {
        page("Personnel","people");button("Add person") {editPerson(null)}
        if(r.people.isEmpty()) text("Add names before entering their schedules. Phone numbers are optional unless sending SMS.")
        r.people.forEach { p -> button(p.name+if(p.phone.isNotBlank()) " · ${p.phone}" else "") {editPerson(p)} }
    }
    private fun editPerson(p:Person?) {
        page(if(p==null) "Add person" else "Edit person","person:${p?.id?:"new"}");formActions()
        val name=field("Name",p?.name.orEmpty());val phone=field("Phone number (optional)",p?.phone.orEmpty(),type=InputType.TYPE_CLASS_PHONE)
        button("Save person") {val updated=Person(p?.id?:UUID.randomUUID().toString(),name.text.toString().trim(),phone.text.toString().trim());save(r.copy(people=r.people.filterNot {it.id==updated.id}+updated),after={people()})}
        if(p!=null) button("Delete person") {require(r.assignments.none {it.personId==p.id}) {"Unassign this person from their shifts first."};ask("Delete person?",p.name,"Delete") {save(r.copy(people=r.people.filterNot {it.id==p.id}),after={people()})} }
    }
    private fun posts() {
        page("Posts / locations","posts");button("Add post") {editPost(null)}
        if(r.posts.isEmpty()) text("Create each post or location where shifts need coverage.")
        r.posts.forEach { p -> button(p.name) {editPost(p)} }
    }
    private fun editPost(p:Post?) {
        page(if(p==null) "Add post" else "Edit post","post:${p?.id?:"new"}");formActions()
        val name=field("Post name",p?.name.orEmpty());val notes=field("Post notes",p?.notes.orEmpty(),true)
        button("Save post") {val updated=Post(p?.id?:UUID.randomUUID().toString(),name.text.toString().trim(),notes.text.toString());save(r.copy(posts=r.posts.filterNot {it.id==updated.id}+updated),after={posts()})}
        if(p!=null) button("Delete post") {require(r.shifts.none {it.postId==p.id} && r.templates.none {it.postId==p.id}) {"This post is used by shifts or templates. Remove those requirements or change the templates first."};ask("Delete post?",p.name,"Delete") {save(r.copy(posts=r.posts.filterNot {it.id==p.id}),after={posts()})} }
    }
    private fun templates() {
        page("Saved shift templates","templates")
        text("Save a reusable pattern without creating shifts. Applying it later creates independent required shifts; editing or deleting the template never changes existing shifts.",14)
        button("Add template") {editTemplate(null)}
        if(r.templates.isEmpty()) text("No templates yet. Save a name, times, staffing count and weekdays; the post can be chosen when applying.")
        r.templates.sortedBy {nameKey(it.name)}.forEach { t ->
            text(t.name,18,true)
            text("${t.label} · ${t.start}–${t.end} · ${t.required} staff\n${t.postId?.let {id -> r.posts.first {it.id==id}.name} ?: "Choose post when applying"} · ${t.zone}\n"+t.weekdays.sorted().joinToString {DayOfWeek.of(it).name},14)
            buttons("Use ${t.name}" to {useTemplate(t)},"Edit ${t.name}" to {editTemplate(t)})
        }
    }
    private fun editTemplate(t:ShiftTemplate?,duplicate:Boolean=false) {
        if(duplicate) drafts.remove("template:new")
        val editing=t!=null && !duplicate
        page(if(editing) "Edit shift template" else "Add shift template","template:${if(editing) t!!.id else "new"}")
        formActions()
        val name=field("Template name",if(duplicate) "${t!!.name} copy" else t?.name.orEmpty())
        val label=field("Template shift name",t?.label ?: "Night")
        val times=field("Template shift time",t?.let {"${it.start}-${it.end}"} ?: "6p-6a")
        val zone=field("Template time zone",t?.zone ?: prefs.getString("zone",ZoneId.systemDefault().id).orEmpty())
        val count=field("Template required personnel",(t?.required ?: 1).toString(),type=InputType.TYPE_CLASS_NUMBER)
        val post=spinner("Template default post",listOf("Choose when applying")+r.posts.map {it.name})
        if(drafts[screen]?.get("Template default post")==null) post.setSelection(r.posts.indexOfFirst {it.id==t?.postId}+1)
        val weekdays=DayOfWeek.values().map {it to check("Template "+it.name.lowercase(Locale.US).replaceFirstChar(Char::uppercase),it.value in (t?.weekdays ?: (1..7).toSet()))}
        val notes=field("Template notes",t?.notes.orEmpty(),true)
        button("Save template") {
            val time=ScheduleEntry.timeRange(times.text.toString())
            val updated=ShiftTemplate(if(editing) t!!.id else UUID.randomUUID().toString(),name.text.toString().trim(),label.text.toString().trim(),time.first.toString(),time.second.toString(),zone.text.toString().trim(),count.text.toString().toInt(),r.posts.getOrNull(post.selectedItemPosition-1)?.id,weekdays.filter {it.second.isChecked}.map {it.first.value}.toSet(),notes.text.toString())
            save(ShiftTemplates.put(r,updated),after={templates()})
        }
        if(editing) {
            button("Duplicate template") {editTemplate(t,true)}
            button("Delete template") {ask("Delete template?","Delete ${t!!.name}? Existing shifts and assignments are kept unchanged.","Delete") {save(ShiftTemplates.remove(r,t.id),after={templates()})}}
        }
    }
    private fun useTemplate(t:ShiftTemplate) {
        page("Apply template: ${t.name}","template-use:${t.id}");formActions()
        if(r.posts.isEmpty()) {text("Add a post before applying a template.");button("Add post") {editPost(null)};return}
        text("${t.label} · ${t.start}–${t.end} · ${t.required} staff · ${t.zone}\nDays: "+t.weekdays.sorted().joinToString {DayOfWeek.of(it).name},14)
        val post=spinner("Post for generated shifts",r.posts.map {it.name})
        if(drafts[screen]?.get("Post for generated shifts")==null) post.setSelection(r.posts.indexOfFirst {it.id==t.postId}.coerceAtLeast(0))
        val dates=field("Template dates","${month.atDay(1)}..${month.atEndOfMonth()}")
        val year=field("Template year",month.year.toString(),type=InputType.TYPE_CLASS_NUMBER)
        button("Review template shifts") {
            val proposal=ShiftTemplates.apply(r,t.id,ScheduleEntry.dates(dates.text.toString(),year.text.toString().toInt()),r.posts[post.selectedItemPosition].id)
            require(proposal.added.isNotEmpty()) {"All ${proposal.skipped} requirements already exist. Existing staffing and assignments are unchanged."}
            review(proposal.roster,proposal.added,"Template ${t.name}: ${proposal.added.size} new requirements; ${proposal.skipped} existing requirements skipped. No personnel assigned automatically. Existing shifts will not change.")
        }
    }
    private fun required() {
        page("Create required shifts","required");formActions()
        if(r.posts.isEmpty()) {button("Add a post first") {editPost(null)};return}
        button("Saved templates") {templates()}
        text("Create the work that must be covered, even when no one is assigned. Repeating patterns are generated for the selected date range.")
        val post=spinner("Post",r.posts.map {it.name})
        val label=field("Shift name","Night")
        val dates=field("Dates", "${month.atDay(1)}..${month.atEndOfMonth()}")
        val year=field("Year for dates without a year",month.year.toString(),type=InputType.TYPE_CLASS_NUMBER)
        val times=field("Shift time","6p-6a")
        val zone=field("Time zone",prefs.getString("zone",ZoneId.systemDefault().id).orEmpty())
        val needed=field("Required personnel","1",type=InputType.TYPE_CLASS_NUMBER)
        val weekdays=DayOfWeek.values().map { it to check(it.name.lowercase(Locale.US).replaceFirstChar(Char::uppercase),true) }
        val notes=field("Shift notes","",true)
        button("Reuse an existing shift pattern") {
            val patterns=r.shifts.distinctBy {listOf(it.postId,it.label,it.startTime().toLocalTime(),it.endTime().toLocalTime(),it.required)}
            if(patterns.isEmpty()) message("Save a required shift first; its pattern will be reusable here.")
            else AlertDialog.Builder(this).setTitle("Choose pattern").setItems(patterns.map { "${r.posts.first {p->p.id==it.postId}.name} · ${it.label} · ${it.startTime().toLocalTime()}–${it.endTime().toLocalTime()} · ${it.required} staff" }.toTypedArray()) { _,index ->
                val s=patterns[index];post.setSelection(r.posts.indexOfFirst {it.id==s.postId});label.setText(s.label);times.setText("${s.startTime().toLocalTime()}-${s.endTime().toLocalTime()}");zone.setText(s.zone);needed.setText(s.required.toString());notes.setText(s.notes)
            }.show()
        }
        button("Review required shifts") {
            val pair=ScheduleEntry.timeRange(times.text.toString());val selectedDays=weekdays.filter {it.second.isChecked}.map {it.first}
            val additions=ScheduleEntry.dates(dates.text.toString(),year.text.toString().toInt()).filter {it.dayOfWeek in selectedDays}.map {d -> ScheduleEntry.shift(d,r.posts[post.selectedItemPosition].id,label.text.toString(),pair.first,pair.second,zone.text.toString().trim(),needed.text.toString().toInt(),notes.text.toString())}.filter {new->r.shifts.none {it.key()==new.key()}}
            require(additions.isNotEmpty()) {"No new requirements: dates may already exist, or no weekdays were selected."}
            review(r.copy(shifts=r.shifts+additions),additions,"${additions.size} new required shifts. Matching existing requirements are kept unchanged. No personnel assigned yet.")
        }
    }
    private fun assign(s:Shift) {
        require(r.people.isNotEmpty()) {"Add personnel first."}
        val checked=BooleanArray(r.people.size) {i -> r.assignments.any {it.shiftId==s.id && it.personId==r.people[i].id}}
        AlertDialog.Builder(this).setTitle("Assign ${s.startTime().toLocalDate()} · ${s.label}")
            .setMultiChoiceItems(r.people.map {it.name}.toTypedArray(),checked) {_,which,value -> checked[which]=value}
            .setNegativeButton("Cancel",null).setPositiveButton("Review") {_,_ -> guard {
                val next=r.copy(assignments=r.assignments.filterNot {it.shiftId==s.id}+r.people.filterIndexed {index,_->checked[index]}.map {Assignment(s.id,it.id)})
                review(next,listOf(s),"Review assignments. Unchecked people will be unassigned, not deleted. Conflicting assignments are rejected.")
            }}.show()
    }
    private fun editShift(s:Shift) {
        page("Edit required shift","shift:${s.id}");formActions()
        val post=spinner("Post",r.posts.map {it.name});if(drafts[screen]?.get("Post")==null) post.setSelection(r.posts.indexOfFirst {it.id==s.postId})
        val label=field("Shift name",s.label);val date=field("Start date (YYYY-MM-DD)",s.startTime().toLocalDate().toString());val times=field("Shift time","${s.startTime().toLocalTime()}-${s.endTime().toLocalTime()}")
        val zone=field("Time zone",s.zone);val needed=field("Required personnel",s.required.toString(),type=InputType.TYPE_CLASS_NUMBER);val notes=field("Shift notes",s.notes,true)
        button("Review change") {val pair=ScheduleEntry.timeRange(times.text.toString());val nextShift=ScheduleEntry.shift(LocalDate.parse(date.text.toString().trim()),r.posts[post.selectedItemPosition].id,label.text.toString(),pair.first,pair.second,zone.text.toString().trim(),needed.text.toString().toInt(),notes.text.toString()).copy(id=s.id);review(r.copy(shifts=r.shifts.map {if(it.id==s.id) nextShift else it}),listOf(nextShift),"Existing personnel remain assigned. Conflicts will block this edit.")}
        button("Remove this requirement") {ask("Remove required shift?","This removes the work requirement and all its assignments. It will NOT appear as an uncovered shift. To keep the requirement open, unassign personnel instead.","Remove requirement") {save(r.copy(shifts=r.shifts.filterNot {it.id==s.id},assignments=r.assignments.filterNot {it.shiftId==s.id}))}}
    }
    private fun bulk() {
        page("Personnel-first schedule entry","bulk");formActions()
        if(r.people.isEmpty()||r.posts.isEmpty()) {text("Add personnel and posts first, then return here. Names must match those saved records.");return}
        text("Enter four lines per person: name, dates, time, post. Repeat in any order. Bullets copied from the README are accepted. Or use Name | Dates | 6p-6a | Post. Nothing is saved before review.")
        val year=field("Year for dates without a year",month.year.toString(),type=InputType.TYPE_CLASS_NUMBER)
        val zone=field("Time zone",prefs.getString("zone",ZoneId.systemDefault().id).orEmpty())
        val entries=field("Schedule entries","",true)
        text("Example syntax:\n${r.people.first().name}\nOct 14 through Oct 25\n6p-6a\n${r.posts.first().name}",14)
        val create=check("Also create any missing required shifts",false)
        val needed=field("Required personnel for new shifts only","1",type=InputType.TYPE_CLASS_NUMBER)
        button("Review and sort schedules") {
            keyboard()
            val snapshot=current
            val input=entries.text.toString();val selectedYear=year.text.toString().toIntOrNull()
            val selectedZone=zone.text.toString().trim();val createMissing=create.isChecked
            val staffing=needed.text.toString().toIntOrNull()
            background({BulkScheduleReview.prepare(snapshot.roster,input,selectedYear,selectedZone,createMissing,staffing)}) { result ->
                val canSave=result.canSave && result.affected.isNotEmpty()
                ask("Review before saving",result.render(),if(canSave) "Save all" else "Back to entry") {
                    if(canSave) save(result.requireValid(),snapshot.revision)
                }
            }
        }
    }

    private fun copyRange() {
        page("Copy day / week / range","copy");formActions()
        val dates=field("Dates to copy", "${month.atDay(1)}..${month.atDay(minOf(7,month.lengthOfMonth()))}")
        val days=field("Move copied shifts forward by days","7",type=InputType.TYPE_CLASS_NUMBER)
        val people=check("Also copy the assigned personnel",false)
        button("Review copies") {
            val selected=ScheduleEntry.dates(dates.text.toString(),month.year).toSet();val offset=days.text.toString().toLong();require(offset in 1..366) {"Use an offset of 1–366 days."}
            var next=r;val added=mutableListOf<Shift>()
            r.shifts.filter {it.startTime().toLocalDate() in selected}.forEach {s ->
                val copy=ScheduleEntry.shift(s.startTime().toLocalDate().plusDays(offset),s.postId,s.label,s.startTime().toLocalTime(),s.endTime().toLocalTime(),s.zone,s.required,s.notes)
                require(next.shifts.none {it.key()==copy.key()}) {"A copied requirement already exists at ${copy.start}. Nothing was saved."}
                next=next.copy(shifts=next.shifts+copy,assignments=next.assignments+if(people.isChecked) r.assignments.filter {it.shiftId==s.id}.map {it.copy(shiftId=copy.id)} else emptyList());added.add(copy)
            }
            require(added.isNotEmpty()) {"No requirements in the selected dates."};review(next,added,"${added.size} copied required shifts. Original shifts are unchanged.")
        }
    }
    private fun export() {
        page("SMS, sharing and Windows text export","export");formActions()
        val from=field("From start date (YYYY-MM-DD)",month.atDay(1).toString());val through=field("Through start date (YYYY-MM-DD)",month.atEndOfMonth().toString())
        val person=spinner("Personnel",listOf("All personnel")+r.people.map {it.name});val post=spinner("Post",listOf("All posts")+r.posts.map {it.name});val grouping=spinner("Group printable text by",listOf("Date","Post","Person"))
        val gaps=check("Only uncovered / partially filled shifts",false)
        val stamp=check("Include generated-at timestamp",false)
        fun selection(includePerson:Boolean=true):Pair<List<Shift>,Person?> {
            val first=LocalDate.parse(from.text.toString().trim());val last=LocalDate.parse(through.text.toString().trim());require(!last.isBefore(first)) {"End date is before start date."}
            val p=if(includePerson) r.people.getOrNull(person.selectedItemPosition-1) else null;val location=r.posts.getOrNull(post.selectedItemPosition-1)
            val selected=r.shifts.filter {s -> val date=s.startTime().toLocalDate();!date.isBefore(first)&&!date.isAfter(last)&&(location==null||s.postId==location.id)&&(p==null||r.assignments.any {it.shiftId==s.id&&it.personId==p.id})&&(!gaps.isChecked||r.open(s)>0)}
            require(selected.isNotEmpty()) {"No shifts match the selection."};return selected to p
        }
        fun printable():String {val (shifts,p)=selection();return (if(stamp.isChecked) "Generated: ${ZonedDateTime.now()}\n\n" else "")+ScheduleText.format(r,shifts,p?.id,false,grouping.selectedItem.toString())}
        text("TXT is a readable document, not a database file. Save locally and transfer it by USB to Windows 11; open in Notepad or paste into Word. External sharing apps may use their own network connection.",14)
        buttons("Preview text" to {message(printable())},"Copy for Word" to {copyText(printable())},"Save .txt" to {val bytes=("\uFEFF"+printable().replace("\r\n","\n").replace("\n","\r\n")).toByteArray(Charsets.UTF_8);createDocument(bytes,"Shift-Calendar-${from.text}-${through.text}.txt","text/plain")},"Share text" to {shareText(printable())})
        button("Manual SMS — choose recipient in Messages") {val(s,p)=selection();SmsSharing.compose(this,ScheduleText.format(r,s,p?.id,true),"")}
        button("Prepare selected person's SMS") {val(s,p)=selection();require(p!=null) {"Select one person above, or use Manual SMS for a full roster."};SmsSharing.compose(this,ScheduleText.format(r,s,p.id,true),p.phone)}
        button("Send individual SMS automatically (opt-in)") {
            val(s,p)=selection();SmsSharing.direct(this,r,s,p)
        }
        text("Choose a combination of people for SMS",18,true)
        text("This picker uses the dates, post and uncovered filter above, independently of the single-person text-export filter. Each selected person receives only their own shifts.",14)
        val chosen=prefs.getStringSet("sms-selected-ids",emptySet()).orEmpty().toMutableSet()
        val selectedText=text("")
        fun showRecipients() {selectedText.text="Selected for SMS: "+r.people.filter {it.id in chosen}.joinToString {it.name}.ifBlank {"None — choose people"}}
        showRecipients()
        button("Choose people for SMS") {SmsRecipientPicker.show(this,r.people,chosen) {ids ->chosen.clear();chosen.addAll(ids);prefs.edit().putStringSet("sms-selected-ids",ids.toSet()).apply();showRecipients()}}
        fun selectedMessages():List<PersonalSms> = SmsBatch.prepare(r,selection(false).first,chosen.toSet())
        button("Review selected people's SMS") {message("Nothing sent. Each recipient gets only their own assignments.\n\n"+SmsBatch.review(selectedMessages()))}
        button("Copy selected SMS text") {copyText(SmsBatch.review(selectedMessages()))}
        button("Prepare SMS one by one") {
            val messages=selectedMessages()
            AlertDialog.Builder(this).setTitle("Choose whose SMS to open").setItems(messages.map {it.name}.toTypedArray()) {_,index ->guard {val m=messages[index];SmsSharing.compose(this,m.text,SmsBatch.number(m))}}.setNegativeButton("Cancel",null).show()
        }
        button("Send selected automatically (opt-in)") {val shifts=selection(false).first;SmsBatch.prepare(r,shifts,chosen.toSet());SmsSharing.direct(this,r,shifts,null,chosen.toSet())}
        text("SMS needs a messaging-capable phone/SIM and carrier service. A Wi-Fi-only tablet can still copy or export the schedule for transfer to a phone. No SMS is sent just by editing a schedule.",14)
    }
    private fun copyText(text:String) {(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Shift Calendar",text));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show()}
    private fun shareText(text:String) {startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,text)},"Share schedule"))}
    private fun createDocument(bytes:ByteArray,name:String,mime:String) {
        val local=File(filesDir,"pending-security-export");local.writeBytes(bytes);pendingFile=local.name;pendingName=name
        try {startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {addCategory(Intent.CATEGORY_OPENABLE);type=mime;putExtra(Intent.EXTRA_TITLE,name);putExtra(Intent.EXTRA_LOCAL_ONLY,true)},410)}
        catch(e:ActivityNotFoundException) {local.delete();pendingFile=null;throw IllegalArgumentException("No document picker is installed. Use Copy or Share instead.",e)}
    }
    @Deprecated("Activity result compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==410) {
            val local=pendingFile?.let {File(filesDir,it)};pendingFile=null
            if(resultCode!=RESULT_OK) {local?.delete();return}
            val uri=data?.data ?: return
            background({require(local!=null&&local.exists()) {"Export expired. Generate it again."};contentResolver.openOutputStream(uri,"w").use {output ->requireNotNull(output) {"Could not open the selected file."};local.inputStream().use {it.copyTo(output)}};local.delete()}) {message("Saved ${pendingName.orEmpty()}. Transfer it by USB or another offline method.")}
        } else if(requestCode==411 && resultCode==RESULT_OK) {
            val uri=data?.data?:return
            background({contentResolver.openInputStream(uri).use {ScheduleBackup.readBounded(requireNotNull(it))}}) {bytes -> password("Restore backup password") {pass ->
                background({try {ScheduleBackup.decrypt(bytes,pass)} finally {pass.fill('\u0000');bytes.fill(0)}}) {next ->
                    val revision=current.revision
                    ask("Replace local schedule?","Backup contains ${next.people.size} people, ${next.posts.size} posts and ${next.shifts.size} required shifts and ${next.templates.size} templates. This replaces the current schedule. Make your own backup first. The replacement is validated and atomic.","Restore") {save(next,revision)}
                }
            }}
        }
    }
    private fun password(title:String,action:(CharArray)->Unit) {
        val e=EditText(this).apply {inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;setSingleLine(true);hint="Password"}
        AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("Cancel",null).setPositiveButton("Continue") {_,_ -> val chars=e.text.toString().toCharArray();e.text.clear();guard {action(chars)}}.show()
    }
    private fun settings() {
        page("Settings, backup and help","settings")
        val handwriting=check("Use supported device handwriting",prefs.getBoolean("handwriting",true))
        handwriting.setOnCheckedChangeListener {_,enabled ->prefs.edit().putBoolean("handwriting",enabled).apply()}
        val direct=check("Enable direct SMS (off by default)",prefs.getBoolean("direct-sms",false))
        direct.setOnCheckedChangeListener {_,enabled ->prefs.edit().putBoolean("direct-sms",enabled).apply()}
        val reminders=check("Remind me before required shifts",prefs.getBoolean("reminders",false))
        reminders.setOnCheckedChangeListener {_,enabled ->prefs.edit().putBoolean("reminders",enabled).apply();if(enabled) ShiftNotifications.requestPermission(this);ShiftNotifications.reschedule(this,r)}
        val lead=field("Reminder lead minutes",prefs.getInt("reminder-minutes",30).toString(),type=InputType.TYPE_CLASS_NUMBER)
        val zone=field("Default time zone",prefs.getString("zone",ZoneId.systemDefault().id).orEmpty())
        button("Save settings") {val minutes=lead.text.toString().toInt();require(minutes in 0..1440) {"Use 0–1440 minutes."};val id=ZoneId.of(zone.text.toString().trim()).id;prefs.edit().putInt("reminder-minutes",minutes).putString("zone",id).apply();ShiftNotifications.reschedule(this,r);message("Settings saved. Existing shifts keep their recorded time zone.")}
        text("Handwriting uses the installed Samsung/Android keyboard. No recognizer or language model is downloaded by this app. On older devices, choose handwriting in Samsung Keyboard. Keyboard entry always remains available. Verify handwriting with Internet disabled on each device.",14)
        buttons("Encrypted backup" to {val snapshot=r;password("New backup password (10+ characters)") {pass ->background({try {ScheduleBackup.encrypt(snapshot,pass)} finally {pass.fill('\u0000')}}) {bytes ->createDocument(bytes,"Shift-Calendar-${LocalDate.now()}.scbackup","application/octet-stream")}}},"Restore backup" to {startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {addCategory(Intent.CATEGORY_OPENABLE);type="*/*";putExtra(Intent.EXTRA_LOCAL_ONLY,true)},411)})
        button("Add home-screen shortcut") {if(android.os.Build.VERSION.SDK_INT>=26) {val manager=getSystemService(android.content.pm.ShortcutManager::class.java);if(manager.isRequestPinShortcutSupported) {val shortcut=android.content.pm.ShortcutInfo.Builder(this,"shift-calendar").setShortLabel("Shift Calendar").setIcon(android.graphics.drawable.Icon.createWithResource(this,applicationInfo.icon)).setIntent(Intent(this,ShiftActivity::class.java).setAction(Intent.ACTION_MAIN)).build();manager.requestPinShortcut(shortcut,null)} else message("Your launcher does not support pinning. Long-press Shift Calendar in the app drawer and add it to Home.")}}
        button("Quick start / functions") {message("1. Add Personnel and Posts.\n2. Create Required shifts for dates, weekdays, time, post and staffing count.\n3. Assign several people on each shift, or use Enter schedules for successive personnel blocks.\n4. Review before saving. Duplicate assignments are skipped; overlaps are rejected.\n5. Uncovered shows required shifts with missing personnel.\n6. Templates saves independent patterns; Use selects dates/post and previews requirements. Edits never change earlier shifts.\n7. Schedule > Filters / search selects sorting and direction.\n8. Share / TXT > Choose people for SMS selects any combination; review, copy, open each composer or confirm opt-in direct SMS. TXT remains available for Word.\n9. Make encrypted backups and keep the password safe.\n\nShifts belong to their start dates. A 6 PM–6 AM shift ends the following morning. Recurring requirements are generated for the chosen range, not forever. Copy a week or reuse a pattern to extend coverage.\n\nA composer handoff is not proof of sending or delivery. Automatic SMS sends only after opt-in, permission and a final recipient/segment review.")}
        button("License / source information") {message("Shift Calendar is an independent customization of Fossify Calendar, GNU GPL version 3. Original copyright notices and license are retained. No warranty. The corresponding source and build scripts are in antonioavila-bit/Calendar, security/offline-shifts-v1. See LICENSE and docs/THIRD_PARTY_NOTICES.md in the source archive. The handwriting adapter follows the owner's read/copy-only Inventory-App pattern. This build is a hardware-test preview, not a production-accepted release.")}
        text("Last direct-SMS status: "+prefs.getString("sms-status","No direct SMS submitted."),14)
    }
}
