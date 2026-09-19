package com.saaf.milkshop.nativeui

/** Stable identifiers shared by interaction contracts and device tests. */
object NativeUiTags {
    const val WelcomeContinue = "welcome_continue"

    const val AuthStartupRetry = "auth_startup_retry"
    const val AuthError = "auth_error"
    const val AuthBackToWelcome = "auth_back_to_welcome"
    const val AuthPassphraseBack = "auth_passphrase_back"
    const val AuthMethodBack = "auth_method_back"
    const val AuthChooseAnotherAccount = "auth_choose_another_account"
    const val AuthQuickPinMethod = "auth_quick_pin_method"
    const val AuthPhoneSecurityMethod = "auth_phone_security_method"
    const val AuthFullPassphraseMethod = "auth_full_passphrase_method"
    const val AuthQuickPinBack = "auth_quick_pin_back"
    const val AuthQuickPinInput = "auth_quick_pin_input"
    const val AuthQuickPinSubmit = "auth_quick_pin_submit"
    const val AuthQuickPinUsePassphrase = "auth_quick_pin_use_passphrase"
    const val AuthPassphraseInput = "auth_passphrase_input"
    const val AuthSignIn = "auth_sign_in"
    const val AuthOwnerLoginInput = "auth_owner_login_input"
    const val AuthOwnerDisplayInput = "auth_owner_display_input"
    const val AuthOwnerPassphraseInput = "auth_owner_passphrase_input"
    const val AuthOwnerConfirmationInput = "auth_owner_confirmation_input"
    const val AuthCreateOwner = "auth_create_owner"
    const val AuthRecoveryAcknowledge = "auth_recovery_acknowledge"

    fun authAccount(loginName: String): String = "auth_account_$loginName"

    const val CashierProductMilk = "cashier_product_milk"
    const val CashierProductYogurt = "cashier_product_yogurt"
    const val CashierModeQuantity = "cashier_mode_quantity"
    const val CashierModeAmount = "cashier_mode_amount"
    const val CashierEntryInput = "cashier_entry_input"
    const val CashierDecrease = "cashier_decrease"
    const val CashierIncrease = "cashier_increase"
    const val CashierPaymentPaidNow = "cashier_payment_paid_now"
    const val CashierPaymentCustomer = "cashier_payment_customer"
    const val CashierCustomerSelector = "cashier_customer_selector"
    const val CashierCustomerAhmed = "cashier_customer_ahmed"
    const val CashierCustomerBilal = "cashier_customer_bilal"
    const val CashierCustomerCancel = "cashier_customer_cancel"
    const val CashierSecondaryDetails = "cashier_secondary_details"
    const val CashierRecordSale = "cashier_record_sale"
    const val CashierSaleSuccess = "cashier_sale_success"
    const val CashierSaleError = "cashier_sale_error"
    const val CashierRetryLoad = "cashier_retry_load"
    const val CashierDiscardDraft = "cashier_discard_draft"
    const val CashierKeepDraft = "cashier_keep_draft"
    const val CashierNavSell = "cashier_nav_sell"
    const val CashierNavCollect = "cashier_nav_collect"
    const val CashierNavActivity = "cashier_nav_activity"
    const val CashierNavMore = "cashier_nav_more"

    fun cashierProduct(name: String): String = when (name.trim().lowercase()) {
        "milk" -> CashierProductMilk
        "yogurt" -> CashierProductYogurt
        else -> "cashier_product_${name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
    }

    fun cashierCustomer(accountId: Long): String = "cashier_customer_$accountId"
}
