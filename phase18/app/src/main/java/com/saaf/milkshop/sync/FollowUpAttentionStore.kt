package com.saaf.milkshop.sync

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/** Derived count only; never contact/financial facts or primary authority. Excluded from portable backup. */
internal data class FollowUpAttention(val count: Int?=null,val stale: Boolean=true,val checkedOn: String?=null,val attemptedOn: String?=null)
internal class FollowUpAttentionStore(context: Context,private val root: File=File(context.noBackupFilesDir,"followup_attention")) {
    private fun file(key: String): File {require(key.matches(Regex("[a-f0-9]{64}")));return File(root,"$key.json")}
    private fun state(key: String)=states.computeIfAbsent(file(key).absolutePath) {
        val value=runCatching {
            val p=JSONObject(AtomicFile(file(key)).readFully().toString(Charsets.UTF_8))
            require(p.getInt("version")==1&&p.getString("binding")==key)
            val count=if(p.isNull("count")) null else (p.get("count") as Int).also {require(it>=0)}
            fun date(k: String)=if(p.isNull(k)) null else p.getString(k).also {LocalDate.parse(it)}
            FollowUpAttention(count,p.get("stale") as Boolean,date("checked_on"),date("attempted_on")).copy(stale=true)
        }.getOrDefault(FollowUpAttention())
        MutableStateFlow(value)
    }
    fun current(key: String): FollowUpAttention=state(key).value
    fun observe(key: String): StateFlow<FollowUpAttention> =state(key)
    fun update(key: String,change: (FollowUpAttention)->FollowUpAttention) {
        val node=state(key)
        synchronized(node) {
            val value=change(node.value);require(value.count==null||value.count>=0)
            val target=file(key);check(root.isDirectory||root.mkdirs())
            val p=JSONObject().put("version",1).put("binding",key).put("count",value.count ?: JSONObject.NULL).put("stale",value.stale)
                .put("checked_on",value.checkedOn ?: JSONObject.NULL).put("attempted_on",value.attemptedOn ?: JSONObject.NULL)
            val atomic=AtomicFile(target);val stream=atomic.startWrite()
            try {stream.write(p.toString().toByteArray());atomic.finishWrite(stream)} catch(failure: Exception) {atomic.failWrite(stream);throw failure}
            node.value=value
        }
    }
    fun stale(key: String)=update(key) {it.copy(stale=true)}
    fun clear(key: String)=update(key) {FollowUpAttention()}
    internal fun invalidateAll(clear: Boolean=false) {
        val keys=(root.listFiles()?.map {it.name.removeSuffix(".json")} ?: emptyList())+
            states.keys.filter {File(it).parentFile==root}.map {File(it).name.removeSuffix(".json")}
        keys.filter {it.matches(Regex("[a-f0-9]{64}"))}.distinct().forEach {if(clear) clear(it) else stale(it)}
    }
    internal fun forgetMemoryForTest(key: String) {states.remove(file(key).absolutePath)}
    companion object {private val states=ConcurrentHashMap<String,MutableStateFlow<FollowUpAttention>>()}
}
