package com.saaf.milkshop.application.owner

import androidx.room3.withReadTransaction
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import java.time.LocalDate

/** Phase 6 data foundation; B supplies accepted settings/state. No app route or worker yet. */
internal class CustomerFollowUpReader(private val db: MilkShopDatabase,private val security: UserSecurityService) {
    suspend fun load(session: String,today: LocalDate): List<FollowUpCustomer> = db.withReadTransaction {
        val actor=security.requirePermission(session,AppPermission.VIEW_FULL_FINANCIAL)
        require(AppRole.parse(actor.role) in setOf(AppRole.OWNER,AppRole.MANAGER,AppRole.ADMIN)) {"Follow-ups require leadership access."}
        val accounts=db.backupDataDao().accounts().filter {it.category=="Customer"}
        val profiles=db.customerProfileDao().all().associateBy {it.accountId}
        val balances=db.readModelDao().accountBalances().associate {it.accountId to it.balance}
        val statuses=CustomerCreditViews(db).load(accounts.associate {it.accountId to (balances[it.accountId] ?: 0.0)},today)
        accounts.map {account->FollowUpCustomer(account.accountId,account.uid,account.name.removePrefix("Customer: ").trim(),
            profiles[account.accountId]?.phone,profiles[account.accountId]?.active ?: true,money(balances[account.accountId] ?: 0.0),statuses[account.accountId]?.status)}
    }
}
