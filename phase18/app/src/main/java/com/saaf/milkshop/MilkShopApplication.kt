package com.saaf.milkshop

import android.app.Application
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService

class MilkShopApplication : Application() {
    val database: MilkShopDatabase by lazy {
        MilkShopDatabase.create(applicationContext)
    }
    val nativeSessions: NativeSessionManager by lazy { NativeSessionManager() }
    val userSecurity: UserSecurityService by lazy {
        UserSecurityService(database, nativeSessions)
    }
    @Volatile var currentSessionId: String? = null
}
