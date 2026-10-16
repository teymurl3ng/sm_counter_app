package com.saaf.milkshop.sync

import com.saaf.milkshop.domain.*
import org.json.JSONObject
import java.security.MessageDigest
import java.time.*
import java.util.UUID

internal data class FollowUpBinding(val environment: String,val server: String,val shop: String,val staff: String,val installation: String) {
    init {require(environment in setOf("dev","prod"));require(server.isNotBlank());FollowUpWire.uuid(shop);FollowUpWire.uuid(staff);require(installation.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,199}")))}
    val key: String get()=MessageDigest.getInstance("SHA-256").digest(listOf(environment,server.trimEnd('/'),shop,staff,installation)
        .joinToString(":") {"${it.length}:$it"}.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it)}
}
internal data class FollowUpHeads(val sync: Long,val credit: Long,val metadata: Long,val policyRevision: String?,val date: LocalDate?)
internal data class FollowUpRemoteClaim(val customerUid: String,val attemptUid: String,val actor: String,val installation: String,
    val cycle: String,val phase: String,val leaseUntil: Instant?,val startedAt: Instant?,val channel: String?)
internal data class FollowUpRemoteSnapshot(val shop: String,val zone: ZoneId?,val serverTime: Long,val heads: FollowUpHeads,
    val policy: FollowUpPolicy?,val primaryInstallation: String?,val primaryActor: String?,
    val preferences: Map<String,CustomerFollowUpPreference>,val acknowledgments: Map<String,FollowUpAcknowledgment>,
    val customerRevisions: Map<String,String>,val claims: List<FollowUpRemoteClaim>,val json: String)
internal data class FollowUpReceipt(val replayed: Boolean,val launch: Boolean,val notify: Boolean,val result: JSONObject)

/** Protocol v1 accepts exact types and identities; no JSON coercion on contractual facts. */
internal object FollowUpWire {
    fun uuid(value: String): String {require(UUID.fromString(value).toString()==value);return value}
    fun text(p: JSONObject,k: String,max: Int=200): String=(p.get(k) as? String)?.also {require(it.isNotBlank()&&it.length<=max)} ?: error("Invalid follow-up text")
    private fun nullableText(p: JSONObject,k: String,max: Int=200): String? {require(p.has(k));return if(p.isNull(k)) null else text(p,k,max)}
    private fun uid(p: JSONObject,k: String)=uuid(text(p,k))
    private fun nullableUid(p: JSONObject,k: String)=nullableText(p,k)?.let(::uuid)
    fun integer(p: JSONObject,k: String): Long {val v=p.get(k);require(v is Long||v is Int);return (v as Number).toLong()}
    fun bool(p: JSONObject,k: String)=p.get(k) as? Boolean ?: error("Invalid follow-up boolean")
    private fun keys(p: JSONObject,vararg k: String) {require(p.keys().asSequence().toSet()==k.toSet()) {"Unexpected follow-up fields"}}
    private fun day(p: JSONObject,k: String)=LocalDate.parse(text(p,k))
    private fun nullableDay(p: JSONObject,k: String)=nullableText(p,k)?.let(LocalDate::parse)
    private fun time(p: JSONObject,k: String)=LocalTime.parse(text(p,k)).also {require(it.second==0 && it.nano==0)}
    private fun nullableInstant(p: JSONObject,k: String)=nullableText(p,k)?.let(Instant::parse)
    private fun installation(p: JSONObject,k: String)=text(p,k).also {require(it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,199}")))}
    fun heads(p: JSONObject): FollowUpHeads {
        keys(p,"sync","credit","metadata","policy_revision","business_date")
        return FollowUpHeads(integer(p,"sync"),integer(p,"credit"),integer(p,"metadata"),nullableUid(p,"policy_revision"),nullableDay(p,"business_date"))
            .also {require(it.sync>=0&&it.credit>=0&&it.metadata>=0)}
    }
    private fun schedule(p: JSONObject): StatementReviewSchedule=when(text(p,"kind")) {
        "WEEKLY"->{keys(p,"kind","weekday");StatementReviewSchedule.Weekly(DayOfWeek.of(integer(p,"weekday").toInt().also {require(it.toLong()==integer(p,"weekday"))}))}
        "MONTHLY"->{keys(p,"kind","day");StatementReviewSchedule.Monthly(integer(p,"day").also {require(it in 0..31)}.toInt())}
        "INTERVAL"->{keys(p,"kind","days","first_date");StatementReviewSchedule.Interval(integer(p,"days").also {require(it in 1..365)}.toInt(),day(p,"first_date"))}
        else->error("Unsupported follow-up cadence")
    }
    fun snapshot(p: JSONObject,shop: String): FollowUpRemoteSnapshot {
        keys(p,"ok","contract_version","shop_id","shop_timezone","heads","server_time_epoch_ms","policy","customers","claims","actions")
        require(bool(p,"ok")&&integer(p,"contract_version")==1L&&uid(p,"shop_id")==shop)
        val zone=nullableText(p,"shop_timezone")?.let(ZoneId::of)
        val now=integer(p,"server_time_epoch_ms").also {require(it>0)}
        val h=heads(p.getJSONObject("heads"))
        require(h.date==zone?.let {Instant.ofEpochMilli(now).atZone(it).toLocalDate()})
        val policy=if(p.isNull("policy")) null else p.getJSONObject("policy")
        var primary: String?=null;var actor: String?=null
        val model=policy?.let {r->
            keys(r,"revision","schedule_revision","effective_from","enabled","weekday","repeat_days","preferred_time","quiet_start","quiet_end","primary_installation","primary_actor","updated_by","updated_at")
            require(uid(r,"revision")==h.policyRevision&&h.date!=null)
            primary=nullableText(r,"primary_installation");actor=nullableUid(r,"primary_actor")
            require((primary==null)==(actor==null));primary?.let {require(it.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,199}")))}
            uid(r,"updated_by");Instant.parse(text(r,"updated_at"))
            FollowUpPolicy(uid(r,"schedule_revision"),day(r,"effective_from"),bool(r,"enabled"),
                StatementReviewSchedule.Weekly(DayOfWeek.of(integer(r,"weekday").also {require(it in 1..7)}.toInt())),
                integer(r,"repeat_days").also {require(it in 1..30)}.toInt(),time(r,"preferred_time"),time(r,"quiet_start"),time(r,"quiet_end"))
                .also {require(!it.isQuiet(it.preferredTime));require(!it.enabled||primary!=null);require(it.effectiveFrom<=h.date)}
        }
        require((policy==null)==(h.policyRevision==null))
        val prefs=linkedMapOf<String,CustomerFollowUpPreference>();val acks=linkedMapOf<String,FollowUpAcknowledgment>();val revisions=linkedMapOf<String,String>()
        val customers=p.getJSONArray("customers")
        for(i in 0 until customers.length()) {
            val r=customers.getJSONObject(i)
            keys(r,"customer_uid","revision","schedule_revision","effective_from","schedule","paused","snoozed_until","statement_cycle","reviewed_on")
            val customer=text(r,"customer_uid");require(customer !in revisions);revisions[customer]=uid(r,"revision")
            val rev=nullableUid(r,"schedule_revision");val effective=nullableDay(r,"effective_from")
            require((rev==null)==(effective==null));require(effective==null||h.date!=null&&effective<=h.date)
            val cadence=if(r.isNull("schedule")) null else schedule(r.getJSONObject("schedule"))
            require(cadence==null||rev!=null)
            val since=effective ?: model?.effectiveFrom ?: error("Customer state without policy")
            prefs[customer]=CustomerFollowUpPreference(rev ?: "shop",since,cadence,bool(r,"paused"),nullableDay(r,"snoozed_until"))
            val reviewed=nullableDay(r,"reviewed_on");require(reviewed==null||h.date!=null&&reviewed<=h.date)
            acks[customer]=FollowUpAcknowledgment(nullableText(r,"statement_cycle",1024),reviewed)
        }
        val claims=mutableListOf<FollowUpRemoteClaim>();val attempts=mutableSetOf<String>();val customerClaims=mutableSetOf<String>()
        val remote=p.getJSONArray("claims")
        for(i in 0 until remote.length()) {
            val r=remote.getJSONObject(i)
            keys(r,"customer_uid","attempt_uid","actor_id","installation","cycle","phase","lease_until","started_at","channel")
            val c=FollowUpRemoteClaim(text(r,"customer_uid"),uid(r,"attempt_uid"),uid(r,"actor_id"),installation(r,"installation"),text(r,"cycle",1024),text(r,"phase"),nullableInstant(r,"lease_until"),nullableInstant(r,"started_at"),nullableText(r,"channel"))
            require(customerClaims.add(c.customerUid)&&attempts.add(c.attemptUid))
            require(if(c.phase=="PREPARING") c.leaseUntil!=null&&c.startedAt==null&&c.channel==null else
                c.phase=="HANDOFF_STARTED"&&c.leaseUntil==null&&c.startedAt!=null&&c.channel in setOf("SMS","WHATSAPP"))
            claims+=c
        }
        val actions=p.getJSONArray("actions");require(actions.length()<=100);var seq=0L
        for(i in 0 until actions.length()) {
            val r=actions.getJSONObject(i)
            keys(r,"seq","command_uid","kind","actor_id","installation","customer_uid","attempt_uid","cycle","accepted_at")
            val next=integer(r,"seq");require(next>seq&&next<=h.metadata);seq=next
            uid(r,"command_uid");uid(r,"actor_id");installation(r,"installation");nullableText(r,"customer_uid");nullableUid(r,"attempt_uid");nullableText(r,"cycle",1024);Instant.parse(text(r,"accepted_at"))
            require(text(r,"kind") in kinds+setOf("REVIEWED","REPORTED_NOT_OPENED","RETRY_UNCERTAIN"))
        }
        require(seq==h.metadata)
        return FollowUpRemoteSnapshot(shop,zone,now,h,model,primary,actor,prefs,acks,revisions,claims,p.toString())
    }
    private val kinds=setOf("POLICY","PREFERENCE","REVIEW","SNOOZE","ACQUIRE","RENEW","START","RELEASE","RESOLVE","DIGEST")
    fun operation(p: JSONObject): String {
        require(p.toString().toByteArray(Charsets.UTF_8).size<=16384)
        val kind=text(p,"kind");require(kind in kinds)
        val expected=when(kind) {
            "POLICY"->setOf("kind","expected_revision","value","make_primary","phones_updated")
            "PREFERENCE"->setOf("kind","customer_uid","expected_customer_revision","value")
            "REVIEW"->setOf("kind","customer_uid","expected_customer_revision","expected_heads","cycle")
            "SNOOZE"->setOf("kind","customer_uid","expected_customer_revision","expected_heads","until")
            "ACQUIRE"->setOf("kind","customer_uid","expected_heads","expected_customer_revision","attempt_uid","cycle")
            "START"->setOf("kind","customer_uid","expected_heads","expected_customer_revision","attempt_uid","channel")
            "RESOLVE"->setOf("kind","customer_uid","attempt_uid","outcome","acknowledge_uncertainty","expected_heads","expected_customer_revision")
            "DIGEST"->setOf("kind","expected_heads","has_work")
            else->setOf("kind","customer_uid","attempt_uid")
        }
        require(p.keys().asSequence().toSet()==expected)
        if("expected_heads" in expected) heads(p.getJSONObject("expected_heads"))
        if("customer_uid" in expected) text(p,"customer_uid")
        if("expected_customer_revision" in expected) nullableUid(p,"expected_customer_revision")
        if("attempt_uid" in expected) uid(p,"attempt_uid")
        if("cycle" in expected) text(p,"cycle",1024)
        when(kind) {
            "POLICY"->{
                nullableUid(p,"expected_revision");bool(p,"make_primary");bool(p,"phones_updated")
                val v=p.getJSONObject("value");keys(v,"enabled","weekday","repeat_days","preferred_time","quiet_start","quiet_end")
                bool(v,"enabled");require(integer(v,"weekday") in 1..7);require(integer(v,"repeat_days") in 1..30)
                val pref=time(v,"preferred_time");val start=time(v,"quiet_start");val end=time(v,"quiet_end");require(start!=end)
                require(if(start<end) pref<start||pref>=end else pref<start&&pref>=end)
            }
            "PREFERENCE"->{val v=p.getJSONObject("value");keys(v,"schedule","paused");bool(v,"paused");if(!v.isNull("schedule")) schedule(v.getJSONObject("schedule")) else require(v.has("schedule"))}
            "START"->require(text(p,"channel") in setOf("SMS","WHATSAPP"))
            "SNOOZE"->day(p,"until")
            "RESOLVE"->{require(text(p,"outcome") in setOf("REVIEWED","REPORTED_NOT_OPENED","RETRY_UNCERTAIN"));require(bool(p,"acknowledge_uncertainty"))}
            "DIGEST"->bool(p,"has_work")
        }
        return kind
    }
    fun receipt(p: JSONObject,request: JSONObject): FollowUpReceipt {
        val kind=operation(request);val replay=bool(p,"replayed")
        val top=setOf("ok","replayed","launch_authorized","notify_authorized","result")+(if(replay) emptySet() else setOf("heads"))
        require(p.keys().asSequence().toSet()==top&&bool(p,"ok"))
        val launch=bool(p,"launch_authorized");val notify=bool(p,"notify_authorized")
        require(!launch||kind=="START"&&!replay);require(!notify||kind=="DIGEST"&&!replay)
        val result=p.getJSONObject("result")
        when(kind) {
            "POLICY","PREFERENCE"->{keys(result,"revision","schedule_revision");uid(result,"revision");if(kind=="POLICY") uid(result,"schedule_revision") else nullableUid(result,"schedule_revision")}
            "ACQUIRE"->{keys(result,"attempt_uid","phase","lease_until");require(text(result,"phase")=="PREPARING");Instant.parse(text(result,"lease_until"))}
            "START","RENEW","RELEASE"->{keys(result,"attempt_uid","phase");require(text(result,"phase")==when(kind){"START"->"HANDOFF_STARTED";"RENEW"->"PREPARING";else->"RELEASED"});require(kind!="START"||replay||launch)}
            "REVIEW","SNOOZE"->{keys(result,"revision","reviewed_on","snoozed_until");uid(result,"revision");nullableDay(result,"reviewed_on");nullableDay(result,"snoozed_until");require(if(kind=="SNOOZE") result.get("snoozed_until")==request.get("until") else nullableDay(result,"reviewed_on")==heads(request.getJSONObject("expected_heads")).date)}
            "RESOLVE"->{keys(result,"attempt_uid","operator_report");require(text(result,"operator_report")==text(request,"outcome"))}
            "DIGEST"->{keys(result,"business_date","reserved");require(day(result,"business_date")==heads(request.getJSONObject("expected_heads")).date);val reserved=bool(result,"reserved");require(replay||notify==reserved)}
        }
        if(result.has("attempt_uid")) require(uid(result,"attempt_uid")==uid(request,"attempt_uid"))
        if(!replay) {
            val h=heads(p.getJSONObject("heads"))
            if(request.has("expected_heads")) {val old=heads(request.getJSONObject("expected_heads"));require(h.sync==old.sync&&h.credit==old.credit&&h.metadata>old.metadata&&h.policyRevision==old.policyRevision&&h.date==old.date)}
        }
        return FollowUpReceipt(replay,launch,notify,JSONObject(result.toString()))
    }
}
