package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.data.FollowUpPendingEntity
import com.saaf.milkshop.domain.FollowUpCustomer
import com.saaf.milkshop.sync.*
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

internal fun followUpActionLabel(kind: String): String=when(kind) {
    "POLICY"->"Shop follow-up settings changed";"PREFERENCE"->"Customer schedule or pause changed"
    "REVIEW"->"Follow-up marked reviewed";"SNOOZE"->"Follow-up postponed";"ACQUIRE"->"Message preparation reserved"
    "RENEW"->"Message preparation extended";"START"->"Message app handoff started";"RELEASE"->"Message preparation cancelled"
    "RESOLVE"->"Message handoff reviewed";"REVIEWED"->"Handoff marked reviewed by staff"
    "REPORTED_NOT_OPENED"->"Staff reported no composer opened";"RETRY_UNCERTAIN"->"Staff allowed another attempt"
    "DIGEST"->"Owner alert reserved";else->"Request needs review"
}

internal fun followUpActivity(remote: FollowUpRemoteSnapshot,binding: FollowUpBinding,customers: List<FollowUpCustomer>): List<FollowUpActivityRow> {
    val names=customers.associate {it.uid to it.name}
    return remote.actions.asReversed().map {action->FollowUpActivityRow(action.sequence,followUpActionLabel(action.kind),
        action.customerUid?.let {names[it] ?: "Customer not available on this device"},
        "${if(action.actor==binding.staff) "Your connected account" else "Another staff account"} · ${if(action.installation==binding.installation) "This device" else "Another device"}",
        action.acceptedAt.atZone(remote.zone ?: java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm")))}
}

internal fun followUpRequests(rows: List<FollowUpPendingEntity>,a: FollowUpSessionAccess,customers: List<FollowUpCustomer>,online: Boolean): List<FollowUpRequestRow> {
    val names=customers.associate {it.uid to it.name}
    return rows.map {row->
        val operation=runCatching {JSONObject(row.operationJson).also {FollowUpWire.operation(it)}}.getOrNull()
        val kind=operation?.getString("kind") ?: "UNKNOWN"
        val same=row.bindingKey==a.binding.key
        FollowUpRequestRow(row.commandUid,followUpActionLabel(kind),if(same) operation?.optString("customer_uid")?.takeIf {it.isNotBlank()}?.let {names[it] ?: "Customer not available on this device"} else null,
            row.restored,same,online&&same&&operation!=null&&(kind!="POLICY"||a.owner),online&&(same&&kind!="POLICY"||a.owner))
    }
}

internal fun followUpClaims(r: FollowUpRemoteSnapshot,a: FollowUpSessionAccess,customers: List<FollowUpCustomer>): List<FollowUpClaimRow> {
    val byUid=customers.associateBy {it.uid}
    return r.claims.filter {it.phase=="HANDOFF_STARTED"||(it.leaseUntil ?: Instant.MIN)>Instant.ofEpochMilli(r.serverTime)}.map {claim->
        val customer=byUid[claim.customerUid];val own=claim.actor==a.binding.staff&&claim.installation==a.binding.installation
        FollowUpClaimRow(claim.attemptUid,customer?.accountId,customer?.name ?: "Customer not available on this device",claim.phase,
            customer!=null&&(own||a.owner&&claim.phase=="HANDOFF_STARTED"))
    }
}
