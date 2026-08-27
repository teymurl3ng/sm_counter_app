package com.saaf.milkshop

import androidx.activity.ComponentActivity
import android.app.AlertDialog
import android.content.Context
import android.widget.CheckBox
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.HistoryDataRepository
import com.saaf.milkshop.data.ManagementDataRepository
import com.saaf.milkshop.data.BudgetDataRepository
import com.saaf.milkshop.data.TransactionReferenceDataRepository
import com.saaf.milkshop.data.DashboardDataRepository
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.application.transactions.DomainTransactionOperationRepository
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.domain.BudgetManagementService
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.OPERATING_BUDGET_CATEGORIES
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.SettingKeys
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.presentation.management.PeopleAccountsActions
import com.saaf.milkshop.presentation.management.PeopleAccountsUi
import com.saaf.milkshop.presentation.records.HistoryCorrectionsActions
import com.saaf.milkshop.presentation.records.HistoryCorrectionsUi
import com.saaf.milkshop.presentation.records.HistoryCorrectionsViewModel
import com.saaf.milkshop.presentation.settings.AppPreferencesUi
import com.saaf.milkshop.presentation.settings.SettingsViewModel
import com.saaf.milkshop.presentation.backup.BackupUi
import com.saaf.milkshop.presentation.backup.BackupViewModel
import com.saaf.milkshop.presentation.shell.AppShellUi
import com.saaf.milkshop.presentation.security.SecurityUi
import com.saaf.milkshop.presentation.budgets.BudgetUi
import com.saaf.milkshop.presentation.dashboard.DashboardUi
import com.saaf.milkshop.presentation.dashboard.DashboardViewModel
import com.saaf.milkshop.presentation.transactions.TransactionsUi
import com.saaf.milkshop.presentation.transactions.TransactionStateUi
import com.saaf.milkshop.presentation.transactions.PurchaseViewModel
import com.saaf.milkshop.presentation.transactions.PurchaseTransactionAction
import com.saaf.milkshop.presentation.transactions.SaleTransactionAction
import com.saaf.milkshop.presentation.transactions.SaleViewModel
import com.saaf.milkshop.presentation.transactions.SimpleTransactionsViewModel
import com.saaf.milkshop.presentation.transactions.SimpleTransactionActions
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var budgetUi: BudgetUi
    private lateinit var database: MilkShopDatabase

    private lateinit var dashboardUi: DashboardUi
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var managementDataRepository: ManagementDataRepository
    private lateinit var businessSettingsDataRepository: com.saaf.milkshop.data.BusinessSettingsDataRepository
    private lateinit var correctionDataRepository: com.saaf.milkshop.data.CorrectionDataRepository
    private lateinit var backupDataRepository: com.saaf.milkshop.data.BackupDataRepository
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var backupViewModel: BackupViewModel
    private lateinit var saleViewModel: SaleViewModel
    private lateinit var historyCorrectionsViewModel: HistoryCorrectionsViewModel
    private lateinit var appShellUi: AppShellUi
    private lateinit var businessSettingsMilkRateVendorInput: EditText
    private lateinit var businessSettingsDeliveryFeeVendorInput: EditText
    private lateinit var businessSettingsTsStandardInput: EditText
    private lateinit var businessSettingsRetailRateMilkInput: EditText
    private lateinit var businessSettingsRetailRateYogurtInput: EditText
    private lateinit var businessSettingsDefaultSurchargeInput: EditText
    private lateinit var saveBusinessSettingsButton: Button
    private lateinit var businessSettingsErrorText: TextView
    private lateinit var businessSettingsStatusText: TextView
    private lateinit var manualBackupButton: Button
    private lateinit var restoreBackupButton: Button
    private lateinit var backupStatusText: TextView
    private lateinit var appPreferencesButton: Button
    private lateinit var manageUsersButton: Button
    private lateinit var changePassphraseButton: Button
    private lateinit var logoutButton: Button
    private lateinit var currentUserText: TextView
    private lateinit var navigationMenuButton: Button
    private lateinit var appRoot: View
    private lateinit var navigationSectionLabel: TextView
    private lateinit var screenHost: FrameLayout
    private lateinit var mainScrollView: ScrollView
    private lateinit var dashboardScreenScroll: ScrollView
    private lateinit var planningScreenScroll: ScrollView
    private lateinit var backupScreenScroll: ScrollView
    private lateinit var settingsScreenScroll: ScrollView
    private lateinit var managementScreenScroll: ScrollView
    private lateinit var transactionsScreenScroll: ScrollView
    private lateinit var recordsScreenScroll: ScrollView
    private val appPreferences by lazy { getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE) }
    private var pendingPreferenceRecreate = false
    private var correctionTargetEvent: EventEntity? = null
    private val backupManager by lazy { BackupManager(applicationContext) }
    private var pendingRestoreUri: android.net.Uri? = null
    private var pendingPreSetupRestore = false
    private lateinit var backupUi: BackupUi
    private lateinit var vendorSpinner: Spinner
    private lateinit var rawLitersInput: EditText
    private lateinit var fatInput: EditText
    private lateinit var lrInput: EditText
    private lateinit var snfText: TextView
    private lateinit var tsText: TextView
    private lateinit var adjustedLitersText: TextView
    private lateinit var amountOwedText: TextView
    private lateinit var purchaseErrorText: TextView
    private lateinit var purchaseStatusText: TextView
    private lateinit var savePurchaseButton: Button

    private lateinit var milkUsedInput: EditText
    private lateinit var yogurtProducedInput: EditText
    private lateinit var productionErrorText: TextView
    private lateinit var productionStatusText: TextView
    private lateinit var saveProductionButton: Button

    private lateinit var vendorPaymentSpinner: Spinner
    private lateinit var paymentAccountSpinner: Spinner
    private lateinit var paymentAmountInput: EditText
    private lateinit var vendorPaymentErrorText: TextView
    private lateinit var vendorPaymentStatusText: TextView
    private lateinit var saveVendorPaymentButton: Button

    private lateinit var saleProductSpinner: Spinner
    private lateinit var saleInputModeRadioGroup: android.widget.RadioGroup
    private lateinit var saleQuantityRadioButton: android.widget.RadioButton
    private lateinit var saleValueRadioButton: android.widget.RadioButton
    private lateinit var saleQuantityInput: EditText
    private lateinit var saleValueInput: EditText
    private lateinit var saleRateInput: EditText
    private lateinit var saleChannelSpinner: Spinner
    private lateinit var salePaymentModeRadioGroup: android.widget.RadioGroup
    private lateinit var saleAccountSpinner: Spinner
    private lateinit var saleCustomerSpinner: Spinner
    private lateinit var addCustomerButton: Button
    private lateinit var saleSurchargeCheckbox: android.widget.CheckBox
    private lateinit var saleSurchargeInput: EditText
    private lateinit var saleNoteInput: EditText
    private lateinit var saleCalculationText: TextView
    private lateinit var saleErrorText: TextView
    private lateinit var saleStatusText: TextView
    private lateinit var saveSaleButton: Button

    private lateinit var creditCollectionCustomerSpinner: Spinner
    private lateinit var creditCollectionAccountSpinner: Spinner
    private lateinit var creditCollectionAmountInput: EditText
    private lateinit var creditCollectionErrorText: TextView
    private lateinit var creditCollectionStatusText: TextView
    private lateinit var saveCreditCollectionButton: Button

    private lateinit var expenseCategorySpinner: Spinner
    private lateinit var expenseAccountSpinner: Spinner
    private lateinit var expenseAmountInput: EditText
    private lateinit var expenseErrorText: TextView
    private lateinit var expenseStatusText: TextView
    private lateinit var saveExpenseButton: Button

    private lateinit var salaryEmployeeSpinner: Spinner
    private lateinit var salaryPaymentAccountSpinner: Spinner
    private lateinit var salaryAmountInput: EditText
    private lateinit var addEmployeeButton: Button
    private lateinit var salaryErrorText: TextView
    private lateinit var salaryStatusText: TextView
    private lateinit var saveSalaryButton: Button

    private lateinit var peopleAccountsUi: PeopleAccountsUi
    private lateinit var peopleAccountsActions: PeopleAccountsActions
    private lateinit var historyCorrectionsActions: HistoryCorrectionsActions
    private lateinit var transactionStateUi: TransactionStateUi
    private lateinit var appPreferencesUi: AppPreferencesUi
    private lateinit var historyCorrectionsUi: HistoryCorrectionsUi
    private lateinit var transactionsUi: TransactionsUi
    private lateinit var purchaseTransactionAction: PurchaseTransactionAction
    private lateinit var saleTransactionAction: SaleTransactionAction
    private lateinit var simpleTransactionActions: SimpleTransactionActions
    private lateinit var purchaseViewModel: PurchaseViewModel
    private lateinit var simpleTransactionsViewModel: SimpleTransactionsViewModel
    private lateinit var security: UserSecurityService
    private lateinit var securityUi: SecurityUi
    private var currentSessionId: String? = null
    private var authenticationStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        appShellUi = AppShellUi(this, appPreferences)
        appShellUi.applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appShellUi.applySavedDisplayDensity()

        val milkShopApplication = application as MilkShopApplication
        database = milkShopApplication.database
        security = milkShopApplication.userSecurity
        managementDataRepository = ManagementDataRepository(database)
        businessSettingsDataRepository = com.saaf.milkshop.data.BusinessSettingsDataRepository(database)
        correctionDataRepository = com.saaf.milkshop.data.CorrectionDataRepository(database)
        backupDataRepository = com.saaf.milkshop.data.BackupDataRepository(applicationContext)
        purchaseViewModel = ViewModelProvider(this)[PurchaseViewModel::class.java]
        simpleTransactionsViewModel = ViewModelProvider(this)[SimpleTransactionsViewModel::class.java]
        dashboardViewModel = ViewModelProvider(
            this,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(DashboardDataRepository(database)) as T
            },
        )[DashboardViewModel::class.java]
        saleViewModel = ViewModelProvider(this)[SaleViewModel::class.java]
        historyCorrectionsViewModel = ViewModelProvider(this)[HistoryCorrectionsViewModel::class.java]
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
        backupViewModel = ViewModelProvider(this)[BackupViewModel::class.java]
        bindViews()
        appRoot.visibility = View.INVISIBLE
        appShellUi.setupWindowInsets()
        appShellUi.setupNavigation()
        setupActions()
        loadInitialState()
        securityUi = SecurityUi(
            activity = this,
            security = security,
            uiScope = uiScope,
            onAuthenticated = ::onAuthenticated,
            onSignedOut = ::onSignedOut,
            onRestoreBeforeSetup = ::showPreSetupRestoreSource,
        )
        securityUi.start(milkShopApplication.currentSessionId)
        authenticationStarted = true
    }

    private fun navigateToSection(section: String, persist: Boolean) {
        appShellUi.navigateToSection(section, persist)
    }

    private fun bindViews() {
        dashboardUi = DashboardUi(
            activity = this,
            uiScope = uiScope,
            preferences = appPreferences,
            formatMoney = ::formatMoney,
            formatQuantity = ::formatQuantity,
            viewModel = dashboardViewModel,
        )
        businessSettingsMilkRateVendorInput = findViewById(R.id.businessSettingsMilkRateVendorInput)
        businessSettingsDeliveryFeeVendorInput = findViewById(R.id.businessSettingsDeliveryFeeVendorInput)
        businessSettingsTsStandardInput = findViewById(R.id.businessSettingsTsStandardInput)
        businessSettingsRetailRateMilkInput = findViewById(R.id.businessSettingsRetailRateMilkInput)
        businessSettingsRetailRateYogurtInput = findViewById(R.id.businessSettingsRetailRateYogurtInput)
        businessSettingsDefaultSurchargeInput = findViewById(R.id.businessSettingsDefaultSurchargeInput)
        saveBusinessSettingsButton = findViewById(R.id.saveBusinessSettingsButton)
        manualBackupButton = findViewById(R.id.manualBackupButton)
        restoreBackupButton = findViewById(R.id.restoreBackupButton)
        backupStatusText = findViewById(R.id.backupStatusText)
        backupUi = BackupUi(
            activity = this,
            backupManager = backupManager,
            uiScope = uiScope,
            viewModel = backupViewModel,
            onChooseExternalBackup = {
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        type = "application/octet-stream"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    },
                    REQUEST_RESTORE_BACKUP,
                )
            },
            onRestoreRequested = { uri, selection -> performRestore(uri, selection) },
        )
        appPreferencesButton = findViewById(R.id.appPreferencesButton)
        manageUsersButton = findViewById(R.id.manageUsersButton)
        changePassphraseButton = findViewById(R.id.changePassphraseButton)
        logoutButton = findViewById(R.id.logoutButton)
        currentUserText = findViewById(R.id.currentUserText)
        appRoot = findViewById(R.id.appRoot)
        navigationMenuButton = findViewById(R.id.navigationMenuButton)
        navigationSectionLabel = findViewById(R.id.navigationSectionLabel)
        screenHost = findViewById(R.id.screenHost)
        dashboardScreenScroll = findViewById(R.id.dashboardScreenScroll)
        planningScreenScroll = findViewById(R.id.planningScreenScroll)
        backupScreenScroll = findViewById(R.id.backupScreenScroll)
        settingsScreenScroll = findViewById(R.id.settingsScreenScroll)
        managementScreenScroll = findViewById(R.id.managementScreenScroll)
        transactionsScreenScroll = findViewById(R.id.transactionsScreenScroll)
        recordsScreenScroll = findViewById(R.id.recordsScreenScroll)
        mainScrollView = dashboardScreenScroll
        businessSettingsErrorText = findViewById(R.id.businessSettingsErrorText)
        businessSettingsStatusText = findViewById(R.id.businessSettingsStatusText)
        historyCorrectionsUi = HistoryCorrectionsUi(
            activity = this,
            onHistoryEventClicked = { event -> historyCorrectionsActions.showTransactionDetail(event) },
            onCorrectionRequested = { event -> event?.let(::showCorrectionEditor) },
            onSaveCorrectionRequested = ::saveCorrection,
            onCancelCorrectionRequested = ::hideCorrectionEditor,
            onCorrectionSaleInputModeChanged = ::updateCorrectionSaleInputMode,
            createSpinnerAdapter = { items -> appShellUi.createSpinnerAdapter(items) },
        )
        historyCorrectionsActions = HistoryCorrectionsActions(
            repository = HistoryDataRepository(database),
            uiScope = uiScope,
            historyCorrectionsUi = historyCorrectionsUi,
            viewModel = historyCorrectionsViewModel,
            onCorrectionTargetChanged = { correctionTargetEvent = it },
        )

        vendorSpinner = findViewById(R.id.vendorSpinner)
        rawLitersInput = findViewById(R.id.rawLitersInput)
        fatInput = findViewById(R.id.fatInput)
        lrInput = findViewById(R.id.lrInput)
        snfText = findViewById(R.id.snfText)
        tsText = findViewById(R.id.tsText)
        adjustedLitersText = findViewById(R.id.adjustedLitersText)
        amountOwedText = findViewById(R.id.amountOwedText)
        purchaseErrorText = findViewById(R.id.purchaseErrorText)
        purchaseStatusText = findViewById(R.id.purchaseStatusText)
        savePurchaseButton = findViewById(R.id.savePurchaseButton)

        milkUsedInput = findViewById(R.id.milkUsedInput)
        yogurtProducedInput = findViewById(R.id.yogurtProducedInput)
        productionErrorText = findViewById(R.id.productionErrorText)
        productionStatusText = findViewById(R.id.productionStatusText)
        saveProductionButton = findViewById(R.id.saveProductionButton)

        vendorPaymentSpinner = findViewById(R.id.vendorPaymentSpinner)
        paymentAccountSpinner = findViewById(R.id.paymentAccountSpinner)
        paymentAmountInput = findViewById(R.id.paymentAmountInput)
        vendorPaymentErrorText = findViewById(R.id.vendorPaymentErrorText)
        vendorPaymentStatusText = findViewById(R.id.vendorPaymentStatusText)
        saveVendorPaymentButton = findViewById(R.id.saveVendorPaymentButton)

        saleProductSpinner = findViewById(R.id.saleProductSpinner)
        saleInputModeRadioGroup = findViewById(R.id.saleInputModeRadioGroup)
        saleQuantityRadioButton = findViewById(R.id.saleQuantityRadioButton)
        saleValueRadioButton = findViewById(R.id.saleValueRadioButton)
        saleQuantityInput = findViewById(R.id.saleQuantityInput)
        saleValueInput = findViewById(R.id.saleValueInput)
        saleRateInput = findViewById(R.id.saleRateInput)
        saleChannelSpinner = findViewById(R.id.saleChannelSpinner)
        salePaymentModeRadioGroup = findViewById(R.id.salePaymentModeRadioGroup)
        saleAccountSpinner = findViewById(R.id.saleAccountSpinner)
        saleCustomerSpinner = findViewById(R.id.saleCustomerSpinner)
        addCustomerButton = findViewById(R.id.addCustomerButton)
        saleSurchargeCheckbox = findViewById(R.id.saleSurchargeCheckbox)
        saleSurchargeInput = findViewById(R.id.saleSurchargeInput)
        saleNoteInput = findViewById(R.id.saleNoteInput)
        saleCalculationText = findViewById(R.id.saleCalculationText)
        saleErrorText = findViewById(R.id.saleErrorText)
        saleStatusText = findViewById(R.id.saleStatusText)
        saveSaleButton = findViewById(R.id.saveSaleButton)

        creditCollectionCustomerSpinner = findViewById(R.id.creditCollectionCustomerSpinner)
        creditCollectionAccountSpinner = findViewById(R.id.creditCollectionAccountSpinner)
        creditCollectionAmountInput = findViewById(R.id.creditCollectionAmountInput)
        creditCollectionErrorText = findViewById(R.id.creditCollectionErrorText)
        creditCollectionStatusText = findViewById(R.id.creditCollectionStatusText)
        saveCreditCollectionButton = findViewById(R.id.saveCreditCollectionButton)

        expenseCategorySpinner = findViewById(R.id.expenseCategorySpinner)
        expenseAccountSpinner = findViewById(R.id.expenseAccountSpinner)
        expenseAmountInput = findViewById(R.id.expenseAmountInput)
        expenseErrorText = findViewById(R.id.expenseErrorText)
        expenseStatusText = findViewById(R.id.expenseStatusText)
        saveExpenseButton = findViewById(R.id.saveExpenseButton)

        salaryEmployeeSpinner = findViewById(R.id.salaryEmployeeSpinner)
        salaryPaymentAccountSpinner = findViewById(R.id.salaryPaymentAccountSpinner)
        salaryAmountInput = findViewById(R.id.salaryAmountInput)
        addEmployeeButton = findViewById(R.id.addEmployeeButton)
        salaryErrorText = findViewById(R.id.salaryErrorText)
        salaryStatusText = findViewById(R.id.salaryStatusText)
        saveSalaryButton = findViewById(R.id.saveSalaryButton)

        peopleAccountsActions = PeopleAccountsActions(
            context = this,
            repository = managementDataRepository,
            uiScope = uiScope,
            reloadVendors = { transactionStateUi.reloadVendors() },
            reloadSalaryData = { transactionStateUi.reloadSalaryData() },
            reloadSaleData = { transactionStateUi.reloadSaleData() },
            refreshDashboard = ::refreshDashboard,
            showVendorError = { message -> peopleAccountsUi.showVendorError(message) },
            clearVendorError = { peopleAccountsUi.clearVendorError() },
            showCustomerError = { message -> peopleAccountsUi.showCustomerError(message) },
            clearCustomerError = { peopleAccountsUi.clearCustomerError() },
            showEmployeeError = { message -> peopleAccountsUi.showEmployeeError(message) },
            clearEmployeeError = { peopleAccountsUi.clearEmployeeError() },
            authorize = { requirePermission(it); Unit },
        )
        peopleAccountsUi = PeopleAccountsUi(
            activity = this,
            formatMoney = ::formatMoney,
            onVendorCreated = peopleAccountsActions::createVendor,
            onVendorRenamed = peopleAccountsActions::renameVendor,
            onCustomerCreated = peopleAccountsActions::createCustomer,
            onCustomerRenamed = peopleAccountsActions::renameCustomer,
            onEmployeeCreated = peopleAccountsActions::createEmployee,
            onEmployeeRenamed = peopleAccountsActions::renameEmployee,
        )
        transactionStateUi = TransactionStateUi(
            activity = this,
            referenceDataRepository = TransactionReferenceDataRepository(database),
            purchaseViewModel = purchaseViewModel,
            saleViewModel = saleViewModel,
            simpleTransactionsViewModel = simpleTransactionsViewModel,
            peopleAccountsUi = peopleAccountsUi,
            createSpinnerAdapter = { items -> appShellUi.createSpinnerAdapter(items) },
            clearPurchaseMessageIfEditing = ::clearPurchaseMessageIfEditing,
        )
        appPreferencesUi = AppPreferencesUi(
            activity = this,
            preferences = appPreferences,
            viewModel = settingsViewModel,
            onPreferencesSaved = {
                dashboardUi.reloadPreferredAnalyticsPeriod()
            },
        )
    }

    private fun setupActions() {
        val transactionOperations = DomainTransactionOperationRepository(database)
        dashboardUi.setup()
        budgetUi = BudgetUi(this, BudgetDataRepository(database), uiScope) { permission ->
            requirePermission(permission)
            Unit
        }
        budgetUi.setup()
        historyCorrectionsUi.setup()
        findViewById<Button>(R.id.addVendorButton).setOnClickListener {
            showAddVendorDialog()
        }
        saveBusinessSettingsButton.setOnClickListener {
            saveBusinessSettings()
        }
        appPreferencesButton.setOnClickListener {
            appPreferencesUi.showAppPreferencesDialog()
        }
        findViewById<Button>(R.id.addVendorManagementButton).setOnClickListener {
            peopleAccountsUi.showAddVendorDialog()
        }
        findViewById<Button>(R.id.addCustomerManagementButton).setOnClickListener {
            peopleAccountsUi.showAddCustomerDialog()
        }
        findViewById<Button>(R.id.addEmployeeManagementButton).setOnClickListener {
            peopleAccountsUi.showAddEmployeeDialog()
        }
        purchaseTransactionAction = PurchaseTransactionAction(
            activity = this,
            transactionOperations = transactionOperations,
            uiScope = uiScope,
            selectedVendorAccountId = transactionStateUi::selectedVendorAccountId,
            vendorsExist = transactionStateUi::vendorsExist,
            clearPurchaseMessageIfEditing = ::clearPurchaseMessageIfEditing,
            clearPurchaseInputs = transactionStateUi::clearPurchaseInputs,
            showPurchaseError = ::showPurchaseError,
            userMessageFor = ::userMessageFor,
            queueAutomaticLocalBackup = ::queueAutomaticLocalBackup,
            refreshDashboard = ::refreshDashboard,
            refreshTransactionHistory = historyCorrectionsActions::refreshTransactionHistory,
            authorize = { requirePermission(it); Unit },
        )

        saleTransactionAction = SaleTransactionAction(
            activity = this,
            transactionOperations = transactionOperations,
            uiScope = uiScope,
            selectedProductId = transactionStateUi::selectedProductId,
            selectedCustomerAccountId = transactionStateUi::selectedSaleCustomerAccountId,
            selectedPaymentAccountId = transactionStateUi::selectedSalePaymentAccountId,
            userMessageFor = ::userMessageForSale,
            showError = ::showSaleError,
            queueAutomaticLocalBackup = ::queueAutomaticLocalBackup,
            refreshDashboard = ::refreshDashboard,
            refreshTransactionHistory = historyCorrectionsActions::refreshTransactionHistory,
            formatMoney = ::formatMoney,
            updateActionState = transactionStateUi::updateSaleActionState,
            clearInputs = {
                saleQuantityInput.text.clear()
                saleValueInput.text.clear()
                saleNoteInput.text.clear()
            },
            authorize = { requirePermission(it); Unit },
        )

        simpleTransactionActions = SimpleTransactionActions(
            activity = this,
            transactionOperations = transactionOperations,
            uiScope = uiScope,
            selectedVendorAccountId = transactionStateUi::selectedVendorPaymentVendorAccountId,
            vendorsExist = transactionStateUi::vendorsExist,
            selectedCustomerAccountId = transactionStateUi::selectedCustomerAccountId,
            customersExist = transactionStateUi::customersExist,
            selectedPaymentAccountForVendorPayment = transactionStateUi::selectedPaymentAccountForVendorPayment,
            selectedPaymentAccountForCreditCollection = transactionStateUi::selectedPaymentAccountForCreditCollection,
            selectedPaymentAccountForExpense = transactionStateUi::selectedPaymentAccountForExpense,
            selectedPaymentAccountForSalary = transactionStateUi::selectedPaymentAccountForSalary,
            selectedExpenseCategory = transactionStateUi::selectedExpenseCategory,
            paymentAccountsExist = transactionStateUi::paymentAccountsExist,
            selectedEmployeeAccountId = transactionStateUi::selectedEmployeeAccountId,
            salaryEmployeesExist = transactionStateUi::salaryEmployeesExist,
            showCreditCollectionError = ::showCreditCollectionError,
            userMessageForCreditCollection = ::userMessageForCreditCollection,
            updateCreditCollectionActionState = transactionStateUi::updateCreditCollectionActionState,
            showExpenseError = ::showExpenseError,
            userMessageForExpense = ::userMessageForExpense,
            showSalaryError = ::showSalaryError,
            userMessageForSalary = ::userMessageForSalary,
            updateSalaryActionState = transactionStateUi::updateSalaryActionState,
            showVendorPaymentError = ::showVendorPaymentError,
            userMessageForVendorPayment = ::userMessageForVendorPayment,
            showProductionError = ::showProductionError,
            userMessageForProduction = ::userMessageForProduction,
            queueAutomaticLocalBackup = ::queueAutomaticLocalBackup,
            refreshDashboard = ::refreshDashboard,
            refreshTransactionHistory = historyCorrectionsActions::refreshTransactionHistory,
            authorize = { requirePermission(it); Unit },
        )

        transactionsUi = TransactionsUi(
            activity = this,
            onSavePurchase = { purchaseTransactionAction.execute() },
            onSaveProduction = { simpleTransactionActions.saveProduction() },
            onSaveVendorPayment = { simpleTransactionActions.saveVendorPayment() },
            onSaveSale = { saleTransactionAction.execute() },
            onAddCustomer = ::showAddCustomerDialog,
            onSaveCreditCollection = { simpleTransactionActions.saveCreditCollection() },
            onSaveExpense = { simpleTransactionActions.saveExpense() },
            onAddEmployee = ::showAddEmployeeDialog,
            onSaveSalary = { simpleTransactionActions.saveSalaryPayment() },
            onPurchaseEdited = transactionStateUi::updatePurchasePreviewAndClearMessage,
            onProductionEdited = transactionStateUi::clearProductionError,
            onVendorPaymentEdited = transactionStateUi::clearVendorPaymentError,
            onCreditCollectionEdited = transactionStateUi::clearCreditCollectionError,
            onExpenseEdited = transactionStateUi::clearExpenseError,
            onSalaryEdited = transactionStateUi::clearSalaryError,
            onSaleEdited = transactionStateUi::updateSalePreviewAndClearError,
            onPurchaseVendorSelected = transactionStateUi::onPurchaseVendorSelected,
            onSaleSelectionChanged = transactionStateUi::onSaleSelectionChanged,
            onSaleProductSelected = transactionStateUi::onSaleProductSelected,
            onSaleInputModeChanged = transactionStateUi::onSaleInputModeChanged,
            onSalePaymentModeChanged = transactionStateUi::onSalePaymentModeChanged,
            onSaleSurchargeChanged = transactionStateUi::onSaleSurchargeChanged,
        )
        transactionsUi.setup()

        manualBackupButton.setOnClickListener {
            uiScope.launch {
                runCatching { requirePermission(AppPermission.CREATE_EXPORT_BACKUP) }
                    .onSuccess {
                        startActivityForResult(
                            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_TITLE, BackupManager.displayName(this@MainActivity))
                            },
                            REQUEST_CREATE_BACKUP,
                        )
                    }
                    .onFailure { backupStatusText.text = it.message }
            }
        }

        restoreBackupButton.setOnClickListener {
            uiScope.launch {
                runCatching {
                    requirePermission(AppPermission.RESTORE_BACKUP)
                    currentSessionId?.let { security.sessionManager().requireRecentAuthentication(it) }
                        ?: throw com.saaf.milkshop.domain.SecurityFailure.SessionExpired()
                }.onSuccess { backupUi.showRestoreSourceDialog() }
                    .onFailure {
                        val sessionId = currentSessionId
                        if (it is com.saaf.milkshop.domain.SecurityFailure.RecentAuthenticationRequired && sessionId != null) {
                            securityUi.showReauthentication(sessionId) { backupUi.showRestoreSourceDialog() }
                        } else backupStatusText.text = it.message
                    }
            }
        }
        manageUsersButton.setOnClickListener {
            currentSessionId?.let(securityUi::showUserManagement)
        }
        changePassphraseButton.setOnClickListener {
            currentSessionId?.let(securityUi::showChangeOwnPassphrase)
        }
        logoutButton.setOnClickListener {
            currentSessionId?.let(securityUi::logout)
        }
    }

    private fun onAuthenticated(sessionId: String, user: AppUserEntity) {
        currentSessionId = sessionId
        (application as MilkShopApplication).currentSessionId = sessionId
        currentUserText.text = getString(R.string.current_user_format, user.displayName, user.role)
        val role = AppRole.parse(user.role)
        appShellUi.setAllowedSections(buildSet {
            add("dashboard")
            add("settings")
            if (RolePermissions.allows(role, AppPermission.VIEW_FULL_FINANCIAL)) add("budgets")
            if (RolePermissions.allows(role, AppPermission.VIEW_OPERATIONAL)) {
                add("transactions")
                add("records")
            }
            if (RolePermissions.allows(role, AppPermission.MANAGE_MASTER_DATA) || RolePermissions.allows(role, AppPermission.ATTENDANCE)) add("management")
            if (RolePermissions.allows(role, AppPermission.CREATE_EXPORT_BACKUP) || RolePermissions.allows(role, AppPermission.RESTORE_BACKUP)) add("backup")
        })
        appShellUi.scrollToPreferredOpeningSection()
        manageUsersButton.visibility = if (
            RolePermissions.allows(role, AppPermission.MANAGE_STAFF_USERS)
        ) View.VISIBLE else View.GONE
        findViewById<View>(R.id.businessSettingsCard).visibility = if (
            RolePermissions.allows(role, AppPermission.VIEW_FULL_FINANCIAL) ||
            RolePermissions.allows(role, AppPermission.MANAGE_BUSINESS_SETTINGS)
        ) View.VISIBLE else View.GONE
        saveBusinessSettingsButton.isEnabled = RolePermissions.allows(role, AppPermission.MANAGE_BUSINESS_SETTINGS)
        manualBackupButton.isEnabled = RolePermissions.allows(role, AppPermission.CREATE_EXPORT_BACKUP)
        restoreBackupButton.isEnabled = RolePermissions.allows(role, AppPermission.RESTORE_BACKUP)
        findViewById<Button>(R.id.savePurchaseButton).isEnabled = RolePermissions.allows(role, AppPermission.PURCHASE)
        findViewById<Button>(R.id.saveProductionButton).isEnabled = RolePermissions.allows(role, AppPermission.PRODUCTION_AND_SPOILAGE)
        findViewById<Button>(R.id.saveVendorPaymentButton).isEnabled = RolePermissions.allows(role, AppPermission.SUPPLIER_PAYMENT)
        findViewById<Button>(R.id.saveSaleButton).isEnabled = RolePermissions.allows(role, AppPermission.SALE_AND_COLLECTION)
        findViewById<Button>(R.id.saveCreditCollectionButton).isEnabled = RolePermissions.allows(role, AppPermission.SALE_AND_COLLECTION)
        findViewById<Button>(R.id.saveExpenseButton).isEnabled = RolePermissions.allows(role, AppPermission.EXPENSE)
        findViewById<Button>(R.id.saveSalaryButton).isEnabled = RolePermissions.allows(role, AppPermission.SALARY_PAYMENT)
        appRoot.visibility = View.VISIBLE
    }

    private fun onSignedOut() {
        currentSessionId = null
        (application as MilkShopApplication).currentSessionId = null
        appRoot.visibility = View.INVISIBLE
    }

    private fun showPreSetupRestoreSource() {
        uiScope.launch {
            val locals = withContext(Dispatchers.IO) { backupManager.localBackups() }
            val labels = locals.map { it.second.fileName }.toMutableList().apply {
                add("Choose portable backup…")
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.backup_restore_button)
                .setItems(labels.toTypedArray()) { _, which ->
                    if (which < locals.size) {
                        restorePreSetupLocal(Uri.fromFile(locals[which].first))
                    } else {
                        pendingPreSetupRestore = true
                        startActivityForResult(
                            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                type = "application/octet-stream"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            },
                            REQUEST_RESTORE_BACKUP,
                        )
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> securityUi.start(null) }
                .show()
        }
    }

    private fun restorePreSetupLocal(uri: Uri) {
        uiScope.launch {
            val sessions = (application as MilkShopApplication).nativeSessions
            val result = backupManager.restoreLocalSnapshot(
                database,
                uri,
                BackupManager.RestoreSelection(true, true, true, security = true),
                sessions,
            )
            result.onSuccess {
                (application as MilkShopApplication).currentSessionId = null
                securityUi.start(null)
            }.onFailure { showSecurityOperationError(it) { securityUi.start(null) } }
        }
    }

    private fun showPortableExportPassphrase(uri: Uri) {
        val passphrase = EditText(this).apply {
            hint = getString(R.string.passphrase_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmation = EditText(this).apply {
            hint = getString(R.string.confirm_passphrase_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 8, 40, 0)
            addView(passphrase)
            addView(confirmation)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Encrypt portable backup")
            .setMessage("Use a separate passphrase you can enter on the replacement device.")
            .setView(fields)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (passphrase.text.toString() != confirmation.text.toString()) {
                    confirmation.error = "Passphrases do not match"
                    return@setOnClickListener
                }
                uiScope.launch {
                    val secret = passphrase.text.toString().toCharArray()
                    val result = try {
                        backupManager.exportEncryptedSnapshot(database, uri, secret)
                    } finally {
                        secret.fill('\u0000')
                    }
                    result.onSuccess {
                        dialog.dismiss()
                        backupStatusText.text = getString(R.string.backup_success)
                    }.onFailure { backupStatusText.text = it.message }
                }
            }
        }
        dialog.show()
    }

    private fun showPortableRestorePassphrase(uri: Uri) {
        val passphrase = EditText(this).apply {
            hint = getString(R.string.passphrase_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.backup_restore_button)
            .setMessage("Enter the portable backup passphrase.")
            .setView(passphrase)
            .setNegativeButton(android.R.string.cancel) { _, _ -> securityUi.start(null) }
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                uiScope.launch {
                    val secret = passphrase.text.toString().toCharArray()
                    val result = try {
                        backupManager.restoreEncrypted(
                            database,
                            uri,
                            BackupManager.RestoreSelection(true, true, true, security = true),
                            secret,
                            (application as MilkShopApplication).nativeSessions,
                        )
                    } finally {
                        secret.fill('\u0000')
                    }
                    result.onSuccess {
                        dialog.dismiss()
                        (application as MilkShopApplication).currentSessionId = null
                        securityUi.start(null)
                    }.onFailure { passphrase.error = it.message }
                }
            }
        }
        dialog.show()
    }

    private fun restorePreSetupExternal(uri: Uri) {
        uiScope.launch {
            val legacyMeta = backupManager.readBackupMeta(uri)
            if (legacyMeta.isSuccess && legacyMeta.getOrThrow().formatVersion == BackupManager.LEGACY_FORMAT_VERSION) {
                backupManager.restore(
                    database,
                    uri,
                    BackupManager.RestoreSelection(true, true, true),
                ).onSuccess {
                    securityUi.start(null)
                }.onFailure { showSecurityOperationError(it) { securityUi.start(null) } }
            } else {
                showPortableRestorePassphrase(uri)
            }
        }
    }

    private fun showSecurityOperationError(error: Throwable, after: () -> Unit = {}) {
        AlertDialog.Builder(this)
            .setMessage(error.message ?: "The operation could not be completed")
            .setPositiveButton(android.R.string.ok) { _, _ -> after() }
            .show()
    }

    @Deprecated("Kept for the existing document-provider flow; migrate to Activity Result APIs with the hybrid shell.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            if (pendingPreSetupRestore) {
                pendingPreSetupRestore = false
                securityUi.start(null)
            }
            return
        }
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_CREATE_BACKUP -> showPortableExportPassphrase(uri)
            REQUEST_RESTORE_BACKUP -> {
                if (pendingPreSetupRestore) {
                    pendingPreSetupRestore = false
                    restorePreSetupExternal(uri)
                } else {
                    backupUi.showRestoreSelectionDialog(uri)
                }
            }
        }
    }

    private suspend fun requirePermission(permission: AppPermission): AppUserEntity {
        val sessionId = currentSessionId ?: throw com.saaf.milkshop.domain.SecurityFailure.SessionExpired()
        return security.requirePermission(sessionId, permission)
    }

    private fun saveBusinessSettings() {
        businessSettingsErrorText.visibility = View.GONE
        businessSettingsStatusText.text = ""

        fun parse(label: String, input: EditText): Double {
            return input.text.toString().trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("$label must be a valid number.")
        }

        val settings = try {
            BusinessSettings(
                milkRateVendor = parse("Vendor milk rate", businessSettingsMilkRateVendorInput),
                deliveryFeeVendor = parse("Vendor delivery fee", businessSettingsDeliveryFeeVendorInput),
                tsStandard = parse("TS standard", businessSettingsTsStandardInput),
                retailRateMilk = parse("Retail milk rate", businessSettingsRetailRateMilkInput),
                retailRateYogurt = parse("Retail yogurt rate", businessSettingsRetailRateYogurtInput),
                defaultDeliverySurcharge = parse("Default delivery surcharge", businessSettingsDefaultSurchargeInput),
            )
        } catch (error: IllegalArgumentException) {
            settingsViewModel.setBusinessSettingsError(error.message ?: "Invalid setting value.")
            businessSettingsErrorText.text = settingsViewModel.businessSettingsError
            businessSettingsErrorText.visibility = View.VISIBLE
            return
        }

        uiScope.launch {
            try {
                requirePermission(AppPermission.MANAGE_BUSINESS_SETTINGS)
                businessSettingsDataRepository.update(settings)
                settingsViewModel.setBusinessSettings(settings)
                settingsViewModel.markBusinessSettingsSaved()
                loadBusinessSettings()
                transactionStateUi.reloadAfterBusinessSettingsSaved()
                businessSettingsStatusText.text = getString(R.string.business_settings_saved)
            } catch (error: Throwable) {
                settingsViewModel.setBusinessSettingsError(error.message ?: "Unable to save settings.")
                businessSettingsErrorText.text = settingsViewModel.businessSettingsError
                businessSettingsErrorText.visibility = View.VISIBLE
            }
        }
    }

    private fun loadInitialState() {
        uiScope.launch {
            loadBusinessSettings()
            transactionStateUi.loadAll()
            refreshDashboard()
            historyCorrectionsActions.refreshTransactionHistory()
            transactionStateUi.updatePurchasePreview()
            appShellUi.scrollToPreferredOpeningSection()
        }
    }

    private suspend fun loadBusinessSettings() {
        val settings = businessSettingsDataRepository.get()
        settingsViewModel.setBusinessSettings(settings)
        withContext(Dispatchers.Main) {
            businessSettingsMilkRateVendorInput.setText(settings.milkRateVendor.toString())
            businessSettingsDeliveryFeeVendorInput.setText(settings.deliveryFeeVendor.toString())
            businessSettingsTsStandardInput.setText(settings.tsStandard.toString())
            businessSettingsRetailRateMilkInput.setText(settings.retailRateMilk.toString())
            businessSettingsRetailRateYogurtInput.setText(settings.retailRateYogurt.toString())
            businessSettingsDefaultSurchargeInput.setText(settings.defaultDeliverySurcharge.toString())
            businessSettingsErrorText.visibility = View.GONE
        }
    }

    private fun showExpenseError(message: String) {
        expenseErrorText.text = message
        expenseErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForExpense(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.expense_error_prefix, message)
        } else {
            getString(R.string.expense_error_generic)
        }
    }

    private fun showSalaryError(message: String) {
        salaryErrorText.text = message
        salaryErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForSalary(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.salary_error_prefix, message)
        } else {
            getString(R.string.salary_error_generic)
        }
    }

    private fun showAddEmployeeDialog() {
        val input = EditText(this).apply { hint = getString(R.string.employee_name_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_employee_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_employee_button) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    showSalaryError(getString(R.string.employee_name_required))
                    return@setPositiveButton
                }
                createEmployee(name)
            }
            .show()
    }

    private fun createEmployee(name: String) {
        uiScope.launch {
            try {
                requirePermission(AppPermission.MANAGE_MASTER_DATA)
                managementDataRepository.createEmployee(name)
                salaryErrorText.visibility = View.GONE
                transactionStateUi.reloadSalaryData()
            } catch (_: Exception) {
                showSalaryError(getString(R.string.employee_create_failed))
            }
        }
    }

    private fun showCreditCollectionError(message: String) {
        creditCollectionErrorText.text = message
        creditCollectionErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForCreditCollection(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.credit_collection_error_prefix, message)
        } else {
            getString(R.string.credit_collection_error_generic)
        }
    }

    private fun showSaleError(message: String) {
        saleErrorText.text = message
        saleErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForSale(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) getString(R.string.sale_error_prefix, message)
        else getString(R.string.sale_error_generic)
    }

    private fun showAddCustomerDialog() {
        val input = EditText(this).apply { hint = getString(R.string.customer_name_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_customer_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_customer_button) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    showSaleError(getString(R.string.customer_name_required))
                    return@setPositiveButton
                }
                createCustomer(name)
            }
            .show()
    }

    private fun createCustomer(name: String) {
        uiScope.launch {
            try {
                requirePermission(AppPermission.MANAGE_MASTER_DATA)
                managementDataRepository.createCustomer(name)
                saleErrorText.visibility = View.GONE
                transactionStateUi.reloadSaleData()
            } catch (_: Exception) {
                showSaleError(getString(R.string.customer_create_failed))
            }
        }
    }

    private fun showCorrectionEditor(event: EventEntity) {
        correctionTargetEvent = event
        historyCorrectionsUi.showCorrectionEditor(event)
    }

    private fun hideCorrectionEditor() {
        historyCorrectionsUi.hideCorrectionEditor()
    }

    private fun updateCorrectionSaleInputMode() {
        historyCorrectionsUi.updateCorrectionSaleInputMode()
    }

    private fun showCorrectionError(message: String) {
        historyCorrectionsUi.showCorrectionError(message)
    }

    private fun userMessageForCorrection(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) getString(R.string.correction_error_prefix, message)
        else getString(R.string.correction_error_generic)
    }

    private fun saveCorrection() {
        val target = correctionTargetEvent ?: run {
            showCorrectionError(getString(R.string.correction_error_no_event))
            return
        }
        historyCorrectionsUi.clearCorrectionMessage()
        historyCorrectionsUi.setCorrectionStatus(getString(R.string.correction_saving))

        val occurredAt = target.occurredAt
        val correctionUiInput = historyCorrectionsUi.correctionInput()
        val notes = correctionUiInput.notes
        val input = try {
            when (target.eventType) {
                VendorPurchaseService.EVENT_TYPE_PURCHASE_RECEIPT -> {
                    val raw = correctionUiInput.purchaseRawLiters.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_purchase_raw))
                    val fat = correctionUiInput.purchaseFat.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_purchase_fat))
                    val lr = correctionUiInput.purchaseLr.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_purchase_lr))
                    require(raw > 0.0) { getString(R.string.correction_error_purchase_raw_positive) }
                    require(fat >= 0.0) { getString(R.string.correction_error_purchase_fat_negative) }
                    require(lr > 0.0) { getString(R.string.correction_error_purchase_lr_positive) }
                    CorrectionInput.Purchase(
                        originalEventId = target.eventId,
                        occurredAt = occurredAt,
                        rawLiters = raw,
                        fatPercentage = fat,
                        lrReading = lr,
                        notes = notes,
                    )
                }
                YogurtProductionService.EVENT_TYPE_PRODUCTION -> {
                    val milk = correctionUiInput.productionMilk.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_production_milk))
                    val yogurt = correctionUiInput.productionYogurt.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_production_yogurt))
                    require(milk > 0.0) { getString(R.string.correction_error_production_milk_positive) }
                    require(yogurt > 0.0) { getString(R.string.correction_error_production_yogurt_positive) }
                    CorrectionInput.Production(
                        originalEventId = target.eventId,
                        occurredAt = occurredAt,
                        milkLitersUsed = milk,
                        yogurtKgProduced = yogurt,
                        notes = notes,
                    )
                }
                SaleService.EVENT_TYPE_SALE -> {
                    val rate = correctionUiInput.saleRate.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_sale_rate))
                    val surcharge = correctionUiInput.saleSurcharge.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_sale_surcharge))
                    require(rate > 0.0) { getString(R.string.correction_error_sale_rate_positive) }
                    require(surcharge >= 0.0) { getString(R.string.correction_error_sale_surcharge_negative) }
                    val channel = correctionUiInput.saleChannel
                    if (correctionUiInput.saleQuantityMode) {
                        val quantity = correctionUiInput.saleQuantity.toDoubleOrNull()
                            ?: error(getString(R.string.correction_error_sale_quantity))
                        require(quantity > 0.0) { getString(R.string.correction_error_sale_quantity_positive) }
                        CorrectionInput.Sale(
                            originalEventId = target.eventId,
                            occurredAt = occurredAt,
                            unitRate = rate,
                            quantity = quantity,
                            value = null,
                            surchargeAmount = surcharge,
                            channel = channel,
                            notes = notes,
                        )
                    } else {
                        val value = correctionUiInput.saleValue.toDoubleOrNull()
                            ?: error(getString(R.string.correction_error_sale_value))
                        require(value > 0.0) { getString(R.string.correction_error_sale_value_positive) }
                        CorrectionInput.Sale(
                            originalEventId = target.eventId,
                            occurredAt = occurredAt,
                            unitRate = rate,
                            quantity = null,
                            value = value,
                            surchargeAmount = surcharge,
                            channel = channel,
                            notes = notes,
                        )
                    }
                }
                CreditCollectionService.EVENT_TYPE_CREDIT_COLLECTION,
                ExpenseService.EVENT_TYPE_EXPENSE,
                SalaryPaymentService.EVENT_TYPE_SALARY_PAYMENT,
                VendorPaymentService.EVENT_TYPE_VENDOR_PAYMENT -> {
                    val amount = correctionUiInput.monetaryAmount.toDoubleOrNull()
                        ?: error(getString(R.string.correction_error_monetary_amount))
                    require(amount > 0.0) { getString(R.string.correction_error_monetary_amount_positive) }
                    CorrectionInput.Monetary(
                        originalEventId = target.eventId,
                        occurredAt = occurredAt,
                        amount = amount,
                        notes = notes,
                    )
                }
                else -> error(getString(R.string.correction_error_unsupported))
            }
        } catch (e: Exception) {
            historyCorrectionsUi.setCorrectionStatus("")
            showCorrectionError(e.message ?: getString(R.string.correction_error_generic))
            return
        }

        uiScope.launch {
            try {
                requirePermission(AppPermission.CORRECTION)
                val result = correctionDataRepository.correct(input)
                queueAutomaticLocalBackup()
                refreshDashboard()
                historyCorrectionsActions.refreshTransactionHistory()
                historyCorrectionsActions.showTransactionDetailById(result.eventId)
                withContext(Dispatchers.Main) {
                    historyCorrectionsUi.hideCorrectionEditorAndShowStatus(
                        getString(R.string.correction_saved, result.eventId)
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    historyCorrectionsUi.setCorrectionStatus("")
                    showCorrectionError(userMessageForCorrection(e))
                }
            }
        }
    }

    private suspend fun refreshDashboard() {
        dashboardUi.refreshDashboard()
    }

    private fun String.toLocalDateOrNull(): LocalDate? = try { LocalDate.parse(this.trim()) } catch (_: DateTimeParseException) { null }

    private fun trimNumber(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value)

    private fun showVendorPaymentError(message: String) {
        vendorPaymentErrorText.text = message
        vendorPaymentErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForVendorPayment(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.vendor_payment_error_prefix, message)
        } else {
            getString(R.string.vendor_payment_error_generic)
        }
    }

    private fun showProductionError(message: String) {
        productionErrorText.text = message
        productionErrorText.visibility = View.VISIBLE
    }

    private fun userMessageForProduction(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.production_error_prefix, message)
        } else {
            getString(R.string.production_error_generic)
        }
    }

    private fun clearPurchaseMessageIfEditing() {
        purchaseErrorText.visibility = View.GONE
    }

    private fun showPurchaseError(message: String) {
        purchaseErrorText.text = message
        purchaseErrorText.visibility = View.VISIBLE
    }

    private fun showAddVendorDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.vendor_name_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_vendor_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_vendor_button) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    showPurchaseError(getString(R.string.vendor_name_required))
                    return@setPositiveButton
                }
                createVendor(name)
            }
            .show()
    }

    private fun createVendor(name: String) {
        uiScope.launch {
            try {
                requirePermission(AppPermission.MANAGE_MASTER_DATA)
                managementDataRepository.createVendor(name)
                purchaseErrorText.visibility = View.GONE
                transactionStateUi.reloadVendors()
            } catch (_: Exception) {
                showPurchaseError(getString(R.string.vendor_create_failed))
            }
        }
    }

    private fun userMessageFor(error: Exception): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotEmpty()) {
            getString(R.string.purchase_error_prefix, message)
        } else {
            getString(R.string.purchase_error_generic)
        }
    }

    private fun performRestore(uri: android.net.Uri, selection: BackupManager.RestoreSelection) {
        uiScope.launch {
            try {
                requirePermission(AppPermission.RESTORE_BACKUP)
                currentSessionId?.let { security.sessionManager().requireRecentAuthentication(it) }
                    ?: throw com.saaf.milkshop.domain.SecurityFailure.SessionExpired()
            } catch (error: Exception) {
                val sessionId = currentSessionId
                if (error is com.saaf.milkshop.domain.SecurityFailure.RecentAuthenticationRequired && sessionId != null) {
                    securityUi.showReauthentication(sessionId) { performRestore(uri, selection) }
                } else backupStatusText.text = error.message
                return@launch
            }
            backupViewModel.beginRestore()
            backupStatusText.text = backupViewModel.state.statusMessage
            manualBackupButton.isEnabled = false
            restoreBackupButton.isEnabled = false
            backupDataRepository.restore(database, uri, selection).onSuccess {
                backupViewModel.setStatus(getString(R.string.backup_restore_success))
                backupStatusText.text = backupViewModel.state.statusMessage
                queueAutomaticLocalBackup()
                refreshDashboard()
                historyCorrectionsActions.refreshTransactionHistory()
                budgetUi.refreshAfterExternalDataChange()
                loadBusinessSettings()
            }.onFailure { error ->
                val failure = error as? BackupManager.BackupFailure
                val message = getString(
                    R.string.backup_restore_failed,
                    failure?.reason ?: (error.message ?: error.javaClass.simpleName),
                    failure?.recovery ?: "The pre-restore safety backup remains available.",
                )
                backupViewModel.setStatus(message)
                backupStatusText.text = backupViewModel.state.statusMessage
            }
            backupViewModel.finishRestore()
            manualBackupButton.isEnabled = !backupViewModel.state.isBusy
            restoreBackupButton.isEnabled = !backupViewModel.state.isBusy
        }
    }

    private fun queueAutomaticLocalBackup() {
        uiScope.launch(Dispatchers.IO) {
            backupDataRepository.createLocalSnapshot(database).onFailure { error ->
                withContext(Dispatchers.Main) {
                    val failure = error as? BackupManager.BackupFailure
                    backupStatusText.text = getString(
                        R.string.backup_failed,
                        failure?.reason ?: (error.message ?: error.javaClass.simpleName),
                        failure?.recovery ?: "The previous valid local backup was kept.",
                    )
                }
            }
        }
    }

    private companion object {
        const val REQUEST_CREATE_BACKUP = 9101
        const val REQUEST_RESTORE_BACKUP = 9102
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }

    override fun onStop() {
        if (::security.isInitialized) security.sessionManager().onBackground()
        if (::appRoot.isInitialized) appRoot.visibility = View.INVISIBLE
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (!authenticationStarted || !::securityUi.isInitialized) return
        security.sessionManager().onForeground()
        val sessionId = currentSessionId ?: return
        uiScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    security.requirePermission(sessionId, AppPermission.VIEW_OPERATIONAL)
                }
            }.onSuccess { onAuthenticated(sessionId, it) }
                .onFailure {
                    onSignedOut()
                    securityUi.start(null)
                }
        }
    }

    private fun formatQuantity(value: Double, unit: String): String =
        String.format(Locale.US, "%.2f %s", value, unit)

    private fun formatMoney(value: Double): String =
        String.format(Locale.US, "₨ %.2f", value)

}
