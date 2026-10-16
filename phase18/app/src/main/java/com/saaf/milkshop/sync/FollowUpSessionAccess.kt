package com.saaf.milkshop.sync

import org.json.JSONObject

/** Valid only inside SyncRuntime's process-wide gate; server remains the authority. */
internal data class FollowUpSessionAccess(val binding: FollowUpBinding,val owner: Boolean,
    val call: suspend (String,JSONObject)->FollowUpRpcResult,val validate: suspend ()->Unit = {})
