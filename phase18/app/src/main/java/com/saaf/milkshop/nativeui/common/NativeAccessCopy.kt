package com.saaf.milkshop.nativeui.common

/** Local app sessions and the installation's business transport are separate. */
object NativeAccessCopy {
    const val LOCK_TITLE = "Lock app"
    const val LOCK_DETAIL = "End your session. Business sync is unchanged."
    const val SYNC_DETAIL = "Business sync is separate from the local account using this phone."
    const val STOP_SYNC_DETAIL = "Uploads and downloads stop until this phone reconnects. Local accounts, saved records and unsent entries remain."
    const val SERVER_STATUS_DETAIL = "This server status concerns business sync. It does not disable local accounts."
    fun offlineWork(daysLeft: Int): String = if (daysLeft <= 0) {
        "Daily work continues; reconnect for privileged actions"
    } else {
        "$daysLeft day(s) before privileged actions need reconnection"
    }
}
