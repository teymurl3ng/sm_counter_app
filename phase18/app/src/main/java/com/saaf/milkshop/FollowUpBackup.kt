package com.saaf.milkshop

import com.saaf.milkshop.data.*
import com.saaf.milkshop.sync.FollowUpWire
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** A backup can recover request identity, never authority, a primary assignment or an external grant. */
internal object FollowUpBackup {
    suspend fun write(db: MilkShopDatabase,data: JSONObject,includeSecurity: Boolean) {
        val pending=db.followUpDataDao().commands()
        require(includeSecurity||pending.isEmpty()) {"Unfinished reminder requests require an encrypted backup."}
        data.put("followup_commands",JSONArray(pending.map {row->JSONObject().put("command_uid",row.commandUid)
            .put("binding_key",row.bindingKey).put("operation_json",row.operationJson).put("created_at",row.createdAt)}))
    }
    suspend fun clear(db: MilkShopDatabase) {db.followUpDataDao().clearCache();db.followUpDataDao().clearCommands()}
    suspend fun restore(db: MilkShopDatabase,data: JSONObject,schema: Int) {
        if(schema<14) {require(!data.has("followup_commands")) {"Reminder requests cannot use a legacy backup schema."};return}
        val rows=data.getJSONArray("followup_commands")
        for(i in 0 until rows.length()) {
            val p=rows.getJSONObject(i)
            require(p.keys().asSequence().toSet()==setOf("command_uid","binding_key","operation_json","created_at"))
            val uid=FollowUpWire.uuid(FollowUpWire.text(p,"command_uid"))
            val binding=FollowUpWire.text(p,"binding_key").also {require(it.matches(Regex("[a-f0-9]{64}")))}
            val operation=FollowUpWire.text(p,"operation_json",16384).also {FollowUpWire.operation(JSONObject(it))}
            val created=FollowUpWire.text(p,"created_at").also {Instant.parse(it)}
            db.followUpDataDao().insertCommand(FollowUpPendingEntity(uid,binding,operation,created,restored=true,lastError="followup_restored_review_required"))
        }
    }
}
