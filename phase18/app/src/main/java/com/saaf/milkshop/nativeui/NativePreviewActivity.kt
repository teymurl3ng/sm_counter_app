package com.saaf.milkshop.nativeui

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.R
import com.saaf.milkshop.application.activity.NativeActivityFacade
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.application.authentication.NativeQuickUnlockFacade
import com.saaf.milkshop.application.cashclose.NativeCashCloseFacade
import com.saaf.milkshop.application.collection.NativeCollectionFacade
import com.saaf.milkshop.application.expense.NativeExpenseFacade
import com.saaf.milkshop.application.more.NativeMoreFacade
import com.saaf.milkshop.application.admin.NativeAdminWorkFacade
import com.saaf.milkshop.application.manager.NativeManagerWorkFacade
import com.saaf.milkshop.application.operations.NativeAttendanceFacade
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.application.operations.NativeMakeYogurtFacade
import com.saaf.milkshop.application.operations.NativeOperationsMoreFacade
import com.saaf.milkshop.application.operations.NativeOperationsWorkFacade
import com.saaf.milkshop.application.operations.NativeReceiveMilkFacade
import com.saaf.milkshop.application.operations.NativeRecordSpoilageFacade
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.LiveNativeOwnerCloudBackupGateway
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import com.saaf.milkshop.application.owner.NativeOwnerOverviewFacade
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.application.owner.NativeOwnerRecordsFacade
import com.saaf.milkshop.application.owner.NativeOwnerReportsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSecurityHistoryFacade
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.application.salarypayment.NativeSalaryPaymentFacade
import com.saaf.milkshop.application.vendorpayment.NativeVendorPaymentFacade
import com.saaf.milkshop.application.owner.NativeOwnerWorkFacade
import com.saaf.milkshop.application.sale.NativeSaleFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.cloud.HybridGoogleDriveAuthorization
import com.saaf.milkshop.nativeui.activity.NativeActivityState
import com.saaf.milkshop.nativeui.activity.NativeActivityViewModel
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationHost
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationState
import com.saaf.milkshop.nativeui.auth.NativeAuthenticationViewModel
import com.saaf.milkshop.nativeui.collection.NativeCollectionState
import com.saaf.milkshop.nativeui.collection.NativeCollectionViewModel
import com.saaf.milkshop.nativeui.expense.NativeExpenseState
import com.saaf.milkshop.nativeui.expense.NativeExpenseViewModel
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseState
import com.saaf.milkshop.nativeui.cashclose.NativeCashCloseViewModel
import com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentState
import com.saaf.milkshop.nativeui.salarypayment.NativeSalaryPaymentViewModel
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentState
import com.saaf.milkshop.nativeui.vendorpayment.NativeVendorPaymentViewModel
import com.saaf.milkshop.nativeui.more.NativeMoreState
import com.saaf.milkshop.nativeui.more.NativeMoreViewModel
import com.saaf.milkshop.nativeui.admin.AdminPreviewHost
import com.saaf.milkshop.nativeui.admin.AdminSection
import com.saaf.milkshop.nativeui.admin.NativeAdminWorkState
import com.saaf.milkshop.nativeui.admin.NativeAdminWorkViewModel
import com.saaf.milkshop.nativeui.manager.ManagerPreviewHost
import com.saaf.milkshop.nativeui.manager.ManagerSection
import com.saaf.milkshop.nativeui.manager.NativeManagerWorkState
import com.saaf.milkshop.nativeui.manager.NativeManagerWorkViewModel
import com.saaf.milkshop.nativeui.operations.NativeAttendanceState
import com.saaf.milkshop.nativeui.operations.NativeAttendanceViewModel
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesState
import com.saaf.milkshop.nativeui.operations.NativeDeliveriesViewModel
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtState
import com.saaf.milkshop.nativeui.operations.NativeMakeYogurtViewModel
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreState
import com.saaf.milkshop.nativeui.operations.NativeOperationsMoreViewModel
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkState
import com.saaf.milkshop.nativeui.operations.NativeOperationsWorkViewModel
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkState
import com.saaf.milkshop.nativeui.operations.NativeReceiveMilkViewModel
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageState
import com.saaf.milkshop.nativeui.operations.NativeRecordSpoilageViewModel
import com.saaf.milkshop.nativeui.operations.OperationsPreviewHost
import com.saaf.milkshop.nativeui.operations.OperationsSection
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewState
import com.saaf.milkshop.nativeui.owner.NativeOwnerOverviewViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerRecordsViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerReportsState
import com.saaf.milkshop.nativeui.owner.NativeOwnerReportsViewModel
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkState
import com.saaf.milkshop.nativeui.owner.NativeOwnerWorkViewModel
import com.saaf.milkshop.nativeui.owner.OwnerPreviewHost
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.sale.CashierPreviewHost
import com.saaf.milkshop.nativeui.sale.CashierSection
import com.saaf.milkshop.nativeui.sale.NativeSaleState
import com.saaf.milkshop.nativeui.sale.NativeSaleViewModel
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.nativeui.welcome.WelcomeScreen
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockDeviceGate
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockRecordStore
import com.saaf.milkshop.quickunlock.QuickUnlockService
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

class NativePreviewActivity : FragmentActivity() {
    private lateinit var applicationState: MilkShopApplication
    private lateinit var authenticationFacade: NativeAuthenticationFacade
    private lateinit var quickUnlockService: QuickUnlockService
    private lateinit var quickUnlockFacade: NativeQuickUnlockFacade
    private lateinit var saleFacade: NativeSaleFacade
    private lateinit var collectionFacade: NativeCollectionFacade
    private lateinit var expenseFacade: NativeExpenseFacade
    private lateinit var vendorPaymentFacade: NativeVendorPaymentFacade
    private lateinit var salaryPaymentFacade: NativeSalaryPaymentFacade
    private lateinit var cashCloseFacade: NativeCashCloseFacade
    private lateinit var activityFacade: NativeActivityFacade
    private lateinit var moreFacade: NativeMoreFacade
    private lateinit var operationsWorkFacade: NativeOperationsWorkFacade
    private lateinit var receiveMilkFacade: NativeReceiveMilkFacade
    private lateinit var makeYogurtFacade: NativeMakeYogurtFacade
    private lateinit var recordSpoilageFacade: NativeRecordSpoilageFacade
    private lateinit var deliveriesFacade: NativeDeliveriesFacade
    private lateinit var attendanceFacade: NativeAttendanceFacade
    private lateinit var operationsMoreFacade: NativeOperationsMoreFacade
    private lateinit var ownerOverviewFacade: NativeOwnerOverviewFacade
    private lateinit var ownerReportsFacade: NativeOwnerReportsFacade
    private lateinit var ownerWorkFacade: NativeOwnerWorkFacade
    private lateinit var managerWorkFacade: NativeManagerWorkFacade
    private lateinit var adminWorkFacade: NativeAdminWorkFacade
    private lateinit var ownerRecordsFacade: NativeOwnerRecordsFacade
    private lateinit var ownerMoreFacade: NativeOwnerMoreFacade
    private lateinit var ownerProductsFacade: NativeOwnerProductsFacade
    private lateinit var ownerCustomersFacade: NativeOwnerCustomersFacade
    private lateinit var ownerSuppliersFacade: NativeOwnerSuppliersFacade
    private lateinit var ownerEmployeesFacade: NativeOwnerEmployeesFacade
    private lateinit var ownerCostingPlansFacade: NativeOwnerCostingPlansFacade
    private lateinit var ownerShopSettingsFacade: NativeOwnerShopSettingsFacade
    private lateinit var ownerUsersRolesFacade: NativeOwnerUsersRolesFacade
    private lateinit var ownerSecurityHistoryFacade: NativeOwnerSecurityHistoryFacade
    private lateinit var ownerBackupFacade: NativeOwnerBackupFacade
    private var cloudAuthorization: HybridGoogleDriveAuthorization? = null

    private val authenticationViewModel by viewModels<NativeAuthenticationViewModel> {
        val app = application as MilkShopApplication
        NativeAuthenticationViewModel.Factory(
            facade = authenticationFacade,
            onSessionChanged = { app.currentSessionId = it },
        )
    }

    private val saleViewModel by viewModels<NativeSaleViewModel> {
        NativeSaleViewModel.Factory(
            facade = saleFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val collectionViewModel by viewModels<NativeCollectionViewModel> {
        NativeCollectionViewModel.Factory(
            facade = collectionFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val expenseViewModel by viewModels<NativeExpenseViewModel> {
        NativeExpenseViewModel.Factory(
            facade = expenseFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val vendorPaymentViewModel by viewModels<NativeVendorPaymentViewModel> {
        NativeVendorPaymentViewModel.Factory(
            facade = vendorPaymentFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val salaryPaymentViewModel by viewModels<NativeSalaryPaymentViewModel> {
        NativeSalaryPaymentViewModel.Factory(
            facade = salaryPaymentFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val cashCloseViewModel by viewModels<NativeCashCloseViewModel> {
        NativeCashCloseViewModel.Factory(
            facade = cashCloseFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val activityViewModel by viewModels<NativeActivityViewModel> {
        NativeActivityViewModel.Factory(
            facade = activityFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val moreViewModel by viewModels<NativeMoreViewModel> {
        NativeMoreViewModel.Factory(
            facade = moreFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val operationsWorkViewModel by viewModels<NativeOperationsWorkViewModel> {
        NativeOperationsWorkViewModel.Factory(
            facade = operationsWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val receiveMilkViewModel by viewModels<NativeReceiveMilkViewModel> {
        NativeReceiveMilkViewModel.Factory(
            facade = receiveMilkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val makeYogurtViewModel by viewModels<NativeMakeYogurtViewModel> {
        NativeMakeYogurtViewModel.Factory(
            facade = makeYogurtFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val recordSpoilageViewModel by viewModels<NativeRecordSpoilageViewModel> {
        NativeRecordSpoilageViewModel.Factory(
            facade = recordSpoilageFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val deliveriesViewModel by viewModels<NativeDeliveriesViewModel> {
        NativeDeliveriesViewModel.Factory(
            facade = deliveriesFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val attendanceViewModel by viewModels<NativeAttendanceViewModel> {
        NativeAttendanceViewModel.Factory(
            facade = attendanceFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val operationsMoreViewModel by viewModels<NativeOperationsMoreViewModel> {
        NativeOperationsMoreViewModel.Factory(
            facade = operationsMoreFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerOverviewViewModel by viewModels<NativeOwnerOverviewViewModel> {
        NativeOwnerOverviewViewModel.Factory(
            facade = ownerOverviewFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerReportsViewModel by viewModels<NativeOwnerReportsViewModel> {
        NativeOwnerReportsViewModel.Factory(
            facade = ownerReportsFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerWorkViewModel by viewModels<NativeOwnerWorkViewModel> {
        NativeOwnerWorkViewModel.Factory(
            facade = ownerWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val managerWorkViewModel by viewModels<NativeManagerWorkViewModel> {
        NativeManagerWorkViewModel.Factory(
            facade = managerWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val adminWorkViewModel by viewModels<NativeAdminWorkViewModel> {
        NativeAdminWorkViewModel.Factory(
            facade = adminWorkFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerRecordsViewModel by viewModels<NativeOwnerRecordsViewModel> {
        NativeOwnerRecordsViewModel.Factory(
            facade = ownerRecordsFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    private val ownerMoreViewModel by viewModels<NativeOwnerMoreViewModel> {
        NativeOwnerMoreViewModel.Factory(
            facade = ownerMoreFacade,
            productsFacade = ownerProductsFacade,
            customersFacade = ownerCustomersFacade,
            suppliersFacade = ownerSuppliersFacade,
            employeesFacade = ownerEmployeesFacade,
            costingPlansFacade = ownerCostingPlansFacade,
            shopSettingsFacade = ownerShopSettingsFacade,
            usersRolesFacade = ownerUsersRolesFacade,
            securityHistoryFacade = ownerSecurityHistoryFacade,
            backupFacade = ownerBackupFacade,
            onSessionExpired = authenticationViewModel::leaveAuthenticatedArea,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as MilkShopApplication
        applicationState = app
        authenticationFacade = NativeAuthenticationFacade(app.database, app.userSecurity)
        quickUnlockService = QuickUnlockService(
            database = app.database,
            security = app.userSecurity,
            store = AndroidQuickUnlockRecordStore(applicationContext),
            deviceGate = AndroidQuickUnlockDeviceGate(this),
        )
        quickUnlockFacade = NativeQuickUnlockFacade(quickUnlockService, authenticationFacade)
        saleFacade = NativeSaleFacade(app.database, app.userSecurity)
        collectionFacade = NativeCollectionFacade(app.database, app.userSecurity)
        expenseFacade = NativeExpenseFacade(app.database, app.userSecurity)
        vendorPaymentFacade = NativeVendorPaymentFacade(app.database, app.userSecurity)
        salaryPaymentFacade = NativeSalaryPaymentFacade(app.database, app.userSecurity)
        cashCloseFacade = NativeCashCloseFacade(app.database, app.userSecurity)
        activityFacade = NativeActivityFacade(app.database, app.userSecurity)
        moreFacade = NativeMoreFacade(app.database, app.userSecurity, quickUnlockService)
        operationsWorkFacade = NativeOperationsWorkFacade(app.database, app.userSecurity)
        receiveMilkFacade = NativeReceiveMilkFacade(app.database, app.userSecurity)
        makeYogurtFacade = NativeMakeYogurtFacade(app.database, app.userSecurity)
        recordSpoilageFacade = NativeRecordSpoilageFacade(app.database, app.userSecurity)
        deliveriesFacade = NativeDeliveriesFacade(app.database, app.userSecurity)
        attendanceFacade = NativeAttendanceFacade(app.database, app.userSecurity)
        operationsMoreFacade = NativeOperationsMoreFacade(app.database, app.userSecurity, quickUnlockService)
        ownerOverviewFacade = NativeOwnerOverviewFacade(
            database = app.database,
            security = app.userSecurity,
            cloudBackupSettings = app.cloudBackupSettings,
        )
        ownerReportsFacade = NativeOwnerReportsFacade(app.database, app.userSecurity)
        ownerWorkFacade = NativeOwnerWorkFacade(app.database, app.userSecurity)
        managerWorkFacade = NativeManagerWorkFacade(app.database, app.userSecurity)
        adminWorkFacade = NativeAdminWorkFacade(app.database, app.userSecurity)
        ownerRecordsFacade = NativeOwnerRecordsFacade(app.database, app.userSecurity)
        ownerMoreFacade = NativeOwnerMoreFacade(app.database, app.userSecurity, quickUnlockService)
        ownerProductsFacade = NativeOwnerProductsFacade(app.database, app.userSecurity)
        ownerCustomersFacade = NativeOwnerCustomersFacade(app.database, app.userSecurity)
        ownerSuppliersFacade = NativeOwnerSuppliersFacade(app.database, app.userSecurity)
        ownerEmployeesFacade = NativeOwnerEmployeesFacade(app.database, app.userSecurity)
        ownerCostingPlansFacade = NativeOwnerCostingPlansFacade(app.database, app.userSecurity)
        ownerShopSettingsFacade = NativeOwnerShopSettingsFacade(app.database, app.userSecurity)
        ownerUsersRolesFacade = NativeOwnerUsersRolesFacade(app.database, app.userSecurity)
        ownerSecurityHistoryFacade = NativeOwnerSecurityHistoryFacade(app.database, app.userSecurity)
        ownerBackupFacade = NativeOwnerBackupFacade(
            database = app.database,
            security = app.userSecurity,
            cloudBackupSettings = app.cloudBackupSettings,
            backupManager = app.backupManager,
            cloudGateway = LiveNativeOwnerCloudBackupGateway(
                app = app,
                authorization = HybridGoogleDriveAuthorization(this).also {
                    cloudAuthorization = it
                },
            ),
        )
        authenticationViewModel.attachQuickUnlock(quickUnlockFacade)
        val openOperator = intent.getBooleanExtra(EXTRA_OPEN_OPERATOR, false)
        val openOwner = intent.getBooleanExtra(EXTRA_OPEN_OWNER, false)
        val openManager = intent.getBooleanExtra(EXTRA_OPEN_MANAGER, false)
        val openAdmin = intent.getBooleanExtra(EXTRA_OPEN_ADMIN, false)
        setContent {
            SaafTheme {
                val authenticationState by authenticationViewModel.state.collectAsState()
                val saleState by saleViewModel.state.collectAsState()
                val collectionState by collectionViewModel.state.collectAsState()
                val expenseState by expenseViewModel.state.collectAsState()
                val vendorPaymentState by vendorPaymentViewModel.state.collectAsState()
                val salaryPaymentState by salaryPaymentViewModel.state.collectAsState()
                val cashCloseState by cashCloseViewModel.state.collectAsState()
                val activityState by activityViewModel.state.collectAsState()
                val moreState by moreViewModel.state.collectAsState()
                val operationsWorkState by operationsWorkViewModel.state.collectAsState()
                val receiveMilkState by receiveMilkViewModel.state.collectAsState()
                val makeYogurtState by makeYogurtViewModel.state.collectAsState()
                val recordSpoilageState by recordSpoilageViewModel.state.collectAsState()
                val deliveriesState by deliveriesViewModel.state.collectAsState()
                val attendanceState by attendanceViewModel.state.collectAsState()
                val operationsMoreState by operationsMoreViewModel.state.collectAsState()
                val ownerOverviewState by ownerOverviewViewModel.state.collectAsState()
                val ownerReportsState by ownerReportsViewModel.state.collectAsState()
                val ownerWorkState by ownerWorkViewModel.state.collectAsState()
                val managerWorkState by managerWorkViewModel.state.collectAsState()
                val adminWorkState by adminWorkViewModel.state.collectAsState()
                val ownerRecordsState by ownerRecordsViewModel.state.collectAsState()
                val ownerMoreState by ownerMoreViewModel.state.collectAsState()
                NativeFoundationPreview(
                    database = app.database,
                    security = app.userSecurity,
                    saleFacade = saleFacade,
                    collectionFacade = collectionFacade,
                    activityFacade = activityFacade,
                    authenticationState = authenticationState,
                    authenticationViewModel = authenticationViewModel,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
                    collectionState = collectionState,
                    collectionViewModel = collectionViewModel,
                    expenseState = expenseState,
                    expenseViewModel = expenseViewModel,
                    vendorPaymentState = vendorPaymentState,
                    vendorPaymentViewModel = vendorPaymentViewModel,
                    salaryPaymentState = salaryPaymentState,
                    salaryPaymentViewModel = salaryPaymentViewModel,
                    cashCloseState = cashCloseState,
                    cashCloseViewModel = cashCloseViewModel,
                    activityState = activityState,
                    activityViewModel = activityViewModel,
                    moreState = moreState,
                    moreViewModel = moreViewModel,
                    operationsWorkState = operationsWorkState,
                    operationsWorkViewModel = operationsWorkViewModel,
                    receiveMilkState = receiveMilkState,
                    receiveMilkViewModel = receiveMilkViewModel,
                    makeYogurtState = makeYogurtState,
                    makeYogurtViewModel = makeYogurtViewModel,
                    recordSpoilageState = recordSpoilageState,
                    recordSpoilageViewModel = recordSpoilageViewModel,
                    deliveriesState = deliveriesState,
                    deliveriesViewModel = deliveriesViewModel,
                    attendanceState = attendanceState,
                    attendanceViewModel = attendanceViewModel,
                    operationsMoreState = operationsMoreState,
                    operationsMoreViewModel = operationsMoreViewModel,
                    ownerOverviewState = ownerOverviewState,
                    ownerOverviewViewModel = ownerOverviewViewModel,
                    ownerReportsState = ownerReportsState,
                    ownerReportsViewModel = ownerReportsViewModel,
                    ownerWorkState = ownerWorkState,
                    ownerWorkViewModel = ownerWorkViewModel,
                    managerWorkState = managerWorkState,
                    managerWorkViewModel = managerWorkViewModel,
                    adminWorkState = adminWorkState,
                    adminWorkViewModel = adminWorkViewModel,
                    ownerRecordsState = ownerRecordsState,
                    ownerRecordsViewModel = ownerRecordsViewModel,
                    ownerMoreState = ownerMoreState,
                    ownerMoreViewModel = ownerMoreViewModel,
                    openOperatorOnStart = openOperator,
                    openOwnerOnStart = openOwner,
                    openManagerOnStart = openManager,
                    openAdminOnStart = openAdmin,
                )
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_OPERATOR = "open_operator"
        const val EXTRA_OPEN_OWNER = "open_owner"
        const val EXTRA_OPEN_MANAGER = "open_manager"
        const val EXTRA_OPEN_ADMIN = "open_admin"
    }

    override fun onStart() {
        super.onStart()
        if (::applicationState.isInitialized) {
            applicationState.nativeSessions.onForeground()
            authenticationViewModel.validateAuthenticatedSession()
        }
    }

    override fun onStop() {
        if (::applicationState.isInitialized) applicationState.nativeSessions.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        if (::quickUnlockFacade.isInitialized) authenticationViewModel.detachQuickUnlock(quickUnlockFacade)
        if (::quickUnlockService.isInitialized) quickUnlockService.close()
        if (::ownerBackupFacade.isInitialized) ownerBackupFacade.close()
        cloudAuthorization?.close()
        cloudAuthorization = null
        super.onDestroy()
    }
}

private enum class PreviewDestination { Welcome, Access }

@Composable
private fun NativeFoundationPreview(
    database: MilkShopDatabase,
    security: UserSecurityService,
    saleFacade: NativeSaleFacade,
    collectionFacade: NativeCollectionFacade,
    activityFacade: NativeActivityFacade,
    authenticationState: NativeAuthenticationState,
    authenticationViewModel: NativeAuthenticationViewModel,
    saleState: NativeSaleState,
    saleViewModel: NativeSaleViewModel,
    collectionState: NativeCollectionState,
    collectionViewModel: NativeCollectionViewModel,
    expenseState: NativeExpenseState,
    expenseViewModel: NativeExpenseViewModel,
    vendorPaymentState: NativeVendorPaymentState,
    vendorPaymentViewModel: NativeVendorPaymentViewModel,
    salaryPaymentState: NativeSalaryPaymentState,
    salaryPaymentViewModel: NativeSalaryPaymentViewModel,
    cashCloseState: NativeCashCloseState,
    cashCloseViewModel: NativeCashCloseViewModel,
    activityState: NativeActivityState,
    activityViewModel: NativeActivityViewModel,
    moreState: NativeMoreState,
    moreViewModel: NativeMoreViewModel,
    operationsWorkState: NativeOperationsWorkState,
    operationsWorkViewModel: NativeOperationsWorkViewModel,
    receiveMilkState: NativeReceiveMilkState,
    receiveMilkViewModel: NativeReceiveMilkViewModel,
    makeYogurtState: NativeMakeYogurtState,
    makeYogurtViewModel: NativeMakeYogurtViewModel,
    recordSpoilageState: NativeRecordSpoilageState,
    recordSpoilageViewModel: NativeRecordSpoilageViewModel,
    deliveriesState: NativeDeliveriesState,
    deliveriesViewModel: NativeDeliveriesViewModel,
    attendanceState: NativeAttendanceState,
    attendanceViewModel: NativeAttendanceViewModel,
    operationsMoreState: NativeOperationsMoreState,
    operationsMoreViewModel: NativeOperationsMoreViewModel,
    ownerOverviewState: NativeOwnerOverviewState,
    ownerOverviewViewModel: NativeOwnerOverviewViewModel,
    ownerReportsState: NativeOwnerReportsState,
    ownerReportsViewModel: NativeOwnerReportsViewModel,
    ownerWorkState: NativeOwnerWorkState,
    ownerWorkViewModel: NativeOwnerWorkViewModel,
    managerWorkState: NativeManagerWorkState,
    managerWorkViewModel: NativeManagerWorkViewModel,
    adminWorkState: NativeAdminWorkState,
    adminWorkViewModel: NativeAdminWorkViewModel,
    ownerRecordsState: NativeOwnerRecordsState,
    ownerRecordsViewModel: NativeOwnerRecordsViewModel,
    ownerMoreState: NativeOwnerMoreState,
    ownerMoreViewModel: NativeOwnerMoreViewModel,
    openOperatorOnStart: Boolean = false,
    openOwnerOnStart: Boolean = false,
    openManagerOnStart: Boolean = false,
    openAdminOnStart: Boolean = false,
) {
    var destination by rememberSaveable {
        mutableStateOf(
            if (openOperatorOnStart || openOwnerOnStart || openManagerOnStart || openAdminOnStart) {
                PreviewDestination.Access
            } else {
                PreviewDestination.Welcome
            },
        )
    }
    var cashierSection by rememberSaveable { mutableStateOf(CashierSection.Sell) }
    var operationsSection by rememberSaveable { mutableStateOf(OperationsSection.Work) }
    var ownerSection by rememberSaveable { mutableStateOf(OwnerSection.Overview) }
    var managerSection by rememberSaveable { mutableStateOf(ManagerSection.Work) }
    var adminSection by rememberSaveable { mutableStateOf(AdminSection.Work) }
    var wantOperator by rememberSaveable { mutableStateOf(openOperatorOnStart) }
    var wantOwner by rememberSaveable {
        mutableStateOf(openOwnerOnStart && !openOperatorOnStart)
    }
    var wantManager by rememberSaveable {
        mutableStateOf(openManagerOnStart && !openOperatorOnStart && !openOwnerOnStart)
    }
    var wantAdmin by rememberSaveable {
        mutableStateOf(
            openAdminOnStart && !openOperatorOnStart && !openOwnerOnStart && !openManagerOnStart,
        )
    }
    var operatorSwitchInFlight by remember { mutableStateOf(false) }
    var ownerSwitchInFlight by remember { mutableStateOf(false) }
    var managerSwitchInFlight by remember { mutableStateOf(false) }
    var adminSwitchInFlight by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val sellNotice = stringResource(R.string.native_nav_sell_notice)

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantOwner) {
        if (!wantOwner || ownerSwitchInFlight || wantOperator || wantManager || wantAdmin) {
            return@LaunchedEffect
        }
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.OWNER) {
                    wantOwner = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                ownerSwitchInFlight = true
                authenticationViewModel.openPreviewOwnerOverview()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                ownerSwitchInFlight = true
                authenticationViewModel.openPreviewOwnerOverview()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                ownerSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.OWNER
        ) {
            ownerSwitchInFlight = false
            wantOwner = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            ownerSwitchInFlight = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantOperator) {
        if (!wantOperator || operatorSwitchInFlight) return@LaunchedEffect
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.OPERATOR) {
                    wantOperator = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                operatorSwitchInFlight = true
                authenticationViewModel.openPreviewOperatorWork()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                operatorSwitchInFlight = true
                authenticationViewModel.openPreviewOperatorWork()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                operatorSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.OPERATOR
        ) {
            operatorSwitchInFlight = false
            wantOperator = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            operatorSwitchInFlight = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantManager) {
        if (!wantManager || managerSwitchInFlight || wantOperator || wantOwner || wantAdmin) {
            return@LaunchedEffect
        }
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.MANAGER) {
                    wantManager = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                managerSwitchInFlight = true
                authenticationViewModel.openPreviewManagerWork()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                managerSwitchInFlight = true
                authenticationViewModel.openPreviewManagerWork()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                managerSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.MANAGER
        ) {
            managerSwitchInFlight = false
            wantManager = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            managerSwitchInFlight = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState, wantAdmin) {
        if (!wantAdmin || adminSwitchInFlight || wantOperator || wantOwner || wantManager) {
            return@LaunchedEffect
        }
        when (val state = authenticationState) {
            is NativeAuthenticationState.Authenticated -> {
                if (state.identity.role == AppRole.ADMIN) {
                    wantAdmin = false
                    destination = PreviewDestination.Access
                    return@LaunchedEffect
                }
                adminSwitchInFlight = true
                authenticationViewModel.openPreviewAdminWork()
            }
            is NativeAuthenticationState.AccountPicker,
            is NativeAuthenticationState.OwnerSetup,
            -> {
                adminSwitchInFlight = true
                authenticationViewModel.openPreviewAdminWork()
            }
            is NativeAuthenticationState.Loading -> Unit
            is NativeAuthenticationState.RecoveryCode -> {
                authenticationViewModel.acknowledgeRecoveryCode()
            }
            else -> {
                adminSwitchInFlight = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(authenticationState) {
        if (authenticationState is NativeAuthenticationState.Authenticated &&
            authenticationState.identity.role == AppRole.ADMIN
        ) {
            adminSwitchInFlight = false
            wantAdmin = false
        }
        val failedPicker = authenticationState as? NativeAuthenticationState.AccountPicker
        val failedOwner = authenticationState as? NativeAuthenticationState.OwnerSetup
        if (failedPicker?.error != null || failedOwner?.error != null) {
            adminSwitchInFlight = false
        }
    }

    fun showPreviewNotice(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite,
            )
        }
    }

    fun closeCashierShell() {
        saleViewModel.close()
        collectionViewModel.close()
        expenseViewModel.close()
        vendorPaymentViewModel.close()
        salaryPaymentViewModel.close()
        cashCloseViewModel.close()
        activityViewModel.close()
        moreViewModel.close()
    }

    fun leaveToWelcome() {
        closeCashierShell()
        operationsWorkViewModel.close()
        receiveMilkViewModel.close()
        makeYogurtViewModel.close()
        recordSpoilageViewModel.close()
        deliveriesViewModel.close()
        attendanceViewModel.close()
        operationsMoreViewModel.close()
        ownerOverviewViewModel.close()
        ownerReportsViewModel.close()
        ownerWorkViewModel.close()
        managerWorkViewModel.close()
        adminWorkViewModel.close()
        ownerRecordsViewModel.close()
        ownerMoreViewModel.close()
        authenticationViewModel.leaveAuthenticatedArea()
        destination = PreviewDestination.Welcome
        cashierSection = CashierSection.Sell
        operationsSection = OperationsSection.Work
        ownerSection = OwnerSection.Overview
        managerSection = ManagerSection.Work
        adminSection = AdminSection.Work
    }

    fun leaveToAccountPicker() {
        closeCashierShell()
        operationsWorkViewModel.close()
        receiveMilkViewModel.close()
        makeYogurtViewModel.close()
        recordSpoilageViewModel.close()
        deliveriesViewModel.close()
        attendanceViewModel.close()
        operationsMoreViewModel.close()
        ownerOverviewViewModel.close()
        ownerReportsViewModel.close()
        ownerWorkViewModel.close()
        managerWorkViewModel.close()
        adminWorkViewModel.close()
        ownerRecordsViewModel.close()
        ownerMoreViewModel.close()
        authenticationViewModel.beginAccountSwitch()
        destination = PreviewDestination.Access
        managerSection = ManagerSection.Work
        adminSection = AdminSection.Work
    }

    val showingAuthenticated = destination == PreviewDestination.Access &&
        authenticationState is NativeAuthenticationState.Authenticated
    val authenticatedRole = (authenticationState as? NativeAuthenticationState.Authenticated)
        ?.identity?.role
    val showingOperator = showingAuthenticated && authenticatedRole == AppRole.OPERATOR
    val showingOwner = showingAuthenticated && authenticatedRole == AppRole.OWNER
    val showingManager = showingAuthenticated && authenticatedRole == AppRole.MANAGER
    val showingAdmin = showingAuthenticated && authenticatedRole == AppRole.ADMIN
    val showingCashier = showingAuthenticated && authenticatedRole == AppRole.CASHIER

    androidx.compose.runtime.LaunchedEffect(
        showingCashier,
        showingOperator,
        showingOwner,
        showingManager,
        showingAdmin,
    ) {
        if (!showingCashier && !showingOwner && !showingManager) closeCashierShell()
        if (!showingOperator && !showingOwner) {
            operationsWorkViewModel.close()
            receiveMilkViewModel.close()
            makeYogurtViewModel.close()
            recordSpoilageViewModel.close()
            deliveriesViewModel.close()
            attendanceViewModel.close()
            operationsMoreViewModel.close()
        }
        if (!showingOwner) {
            ownerOverviewViewModel.close()
            ownerReportsViewModel.close()
            ownerWorkViewModel.close()
        }
        if (!showingOwner && !showingManager) {
            ownerRecordsViewModel.close()
        }
        if (!showingOwner && !showingManager && !showingAdmin) {
            ownerMoreViewModel.close()
        }
        if (!showingManager) {
            managerWorkViewModel.close()
        }
        if (!showingAdmin) {
            adminWorkViewModel.close()
        }
    }

    SystemBarAppearance(
        useDarkIcons = when {
            destination == PreviewDestination.Welcome -> false
            else -> true
        },
    )

    when (destination) {
        PreviewDestination.Welcome -> WelcomeScreen(
            onContinue = { destination = PreviewDestination.Access },
        )
        PreviewDestination.Access -> {
            if (showingOwner) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                OwnerPreviewHost(
                    sessionHandle = authenticated.sessionHandle,
                    identity = authenticated.identity,
                    database = database,
                    security = security,
                    saleFacade = saleFacade,
                    collectionFacade = collectionFacade,
                    activityFacade = activityFacade,
                    snackbarHostState = snackbarHostState,
                    ownerSection = ownerSection,
                    onOwnerSectionChange = { ownerSection = it },
                    ownerOverviewState = ownerOverviewState,
                    ownerOverviewViewModel = ownerOverviewViewModel,
                    ownerReportsState = ownerReportsState,
                    ownerReportsViewModel = ownerReportsViewModel,
                    ownerWorkState = ownerWorkState,
                    ownerWorkViewModel = ownerWorkViewModel,
                    ownerRecordsState = ownerRecordsState,
                    ownerRecordsViewModel = ownerRecordsViewModel,
                    ownerMoreState = ownerMoreState,
                    ownerMoreViewModel = ownerMoreViewModel,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
                    collectionState = collectionState,
                    collectionViewModel = collectionViewModel,
                    expenseState = expenseState,
                    expenseViewModel = expenseViewModel,
                    vendorPaymentState = vendorPaymentState,
                    vendorPaymentViewModel = vendorPaymentViewModel,
                    salaryPaymentState = salaryPaymentState,
                    salaryPaymentViewModel = salaryPaymentViewModel,
                    cashCloseState = cashCloseState,
                    cashCloseViewModel = cashCloseViewModel,
                    receiveMilkState = receiveMilkState,
                    receiveMilkViewModel = receiveMilkViewModel,
                    makeYogurtState = makeYogurtState,
                    makeYogurtViewModel = makeYogurtViewModel,
                    recordSpoilageState = recordSpoilageState,
                    recordSpoilageViewModel = recordSpoilageViewModel,
                    deliveriesState = deliveriesState,
                    deliveriesViewModel = deliveriesViewModel,
                    onLeaveToWelcome = { leaveToWelcome() },
                    onLeaveToAccountPicker = { leaveToAccountPicker() },
                    onShowNotice = { showPreviewNotice(it) },
                )
            } else if (showingOperator) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                OperationsPreviewHost(
                    sessionHandle = authenticated.sessionHandle,
                    identity = authenticated.identity,
                    snackbarHostState = snackbarHostState,
                    operationsSection = operationsSection,
                    onOperationsSectionChange = { operationsSection = it },
                    operationsWorkState = operationsWorkState,
                    operationsWorkViewModel = operationsWorkViewModel,
                    receiveMilkState = receiveMilkState,
                    receiveMilkViewModel = receiveMilkViewModel,
                    makeYogurtState = makeYogurtState,
                    makeYogurtViewModel = makeYogurtViewModel,
                    recordSpoilageState = recordSpoilageState,
                    recordSpoilageViewModel = recordSpoilageViewModel,
                    deliveriesState = deliveriesState,
                    deliveriesViewModel = deliveriesViewModel,
                    attendanceState = attendanceState,
                    attendanceViewModel = attendanceViewModel,
                    operationsMoreState = operationsMoreState,
                    operationsMoreViewModel = operationsMoreViewModel,
                    onLeaveToWelcome = { leaveToWelcome() },
                    onLeaveToAccountPicker = { leaveToAccountPicker() },
                    onShowNotice = { showPreviewNotice(it) },
                )
            } else if (showingManager) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                ManagerPreviewHost(
                    sessionHandle = authenticated.sessionHandle,
                    identity = authenticated.identity,
                    snackbarHostState = snackbarHostState,
                    managerSection = managerSection,
                    onManagerSectionChange = { managerSection = it },
                    managerWorkState = managerWorkState,
                    managerWorkViewModel = managerWorkViewModel,
                    expenseState = expenseState,
                    expenseViewModel = expenseViewModel,
                    vendorPaymentState = vendorPaymentState,
                    vendorPaymentViewModel = vendorPaymentViewModel,
                    cashCloseState = cashCloseState,
                    cashCloseViewModel = cashCloseViewModel,
                    ownerRecordsState = ownerRecordsState,
                    ownerRecordsViewModel = ownerRecordsViewModel,
                    ownerMoreState = ownerMoreState,
                    ownerMoreViewModel = ownerMoreViewModel,
                    onLeaveToWelcome = { leaveToWelcome() },
                    onLeaveToAccountPicker = { leaveToAccountPicker() },
                    onShowNotice = { showPreviewNotice(it) },
                )
            } else if (showingAdmin) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                AdminPreviewHost(
                    sessionHandle = authenticated.sessionHandle,
                    identity = authenticated.identity,
                    snackbarHostState = snackbarHostState,
                    adminSection = adminSection,
                    onAdminSectionChange = { adminSection = it },
                    adminWorkState = adminWorkState,
                    adminWorkViewModel = adminWorkViewModel,
                    expenseState = expenseState,
                    expenseViewModel = expenseViewModel,
                    ownerRecordsState = ownerRecordsState,
                    ownerRecordsViewModel = ownerRecordsViewModel,
                    ownerMoreState = ownerMoreState,
                    ownerMoreViewModel = ownerMoreViewModel,
                    onLeaveToWelcome = { leaveToWelcome() },
                    onLeaveToAccountPicker = { leaveToAccountPicker() },
                    onShowNotice = { showPreviewNotice(it) },
                )
            } else if (showingCashier) {
                val authenticated = authenticationState as NativeAuthenticationState.Authenticated
                CashierPreviewHost(
                    sessionHandle = authenticated.sessionHandle,
                    identity = authenticated.identity,
                    database = database,
                    security = security,
                    saleFacade = saleFacade,
                    collectionFacade = collectionFacade,
                    activityFacade = activityFacade,
                    snackbarHostState = snackbarHostState,
                    cashierSection = cashierSection,
                    onCashierSectionChange = { cashierSection = it },
                    sellNotice = sellNotice,
                    saleState = saleState,
                    saleViewModel = saleViewModel,
                    collectionState = collectionState,
                    collectionViewModel = collectionViewModel,
                    activityState = activityState,
                    activityViewModel = activityViewModel,
                    moreState = moreState,
                    moreViewModel = moreViewModel,
                    expenseState = expenseState,
                    expenseViewModel = expenseViewModel,
                    onLeaveToWelcome = { leaveToWelcome() },
                    onLeaveToAccountPicker = { leaveToAccountPicker() },
                    onShowNotice = { showPreviewNotice(it) },
                )
            } else {
                when (authenticationState) {
                    is NativeAuthenticationState.AccountPicker -> BackHandler {
                        if (authenticationState.cancelReturnsToSession) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    }
                    is NativeAuthenticationState.MethodPicker -> BackHandler {
                        authenticationViewModel.chooseAnotherAccount()
                    }
                    is NativeAuthenticationState.QuickPin -> BackHandler {
                        authenticationViewModel.backFromQuickPin()
                    }
                    is NativeAuthenticationState.Passphrase -> BackHandler {
                        authenticationViewModel.backFromPassphrase()
                    }
                    is NativeAuthenticationState.RecoveryCode -> BackHandler(enabled = true) {}
                    else -> BackHandler {
                        if (authenticationViewModel.isAccountSwitchPending()) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    }
                }
                NativeAuthenticationHost(
                    state = authenticationState,
                    onRetry = authenticationViewModel::refreshStartup,
                    onBackToWelcome = {
                        val picker = authenticationState as? NativeAuthenticationState.AccountPicker
                        if (picker?.cancelReturnsToSession == true) {
                            authenticationViewModel.cancelAccountSwitch()
                        } else {
                            destination = PreviewDestination.Welcome
                        }
                    },
                    onSelectAccount = authenticationViewModel::selectAccount,
                    onChooseAnotherAccount = authenticationViewModel::chooseAnotherAccount,
                    onBackFromPassphrase = authenticationViewModel::backFromPassphrase,
                    onUseQuickPin = authenticationViewModel::useQuickPin,
                    onUsePhoneSecurity = authenticationViewModel::signInWithPhoneSecurity,
                    onUseFullPassphrase = authenticationViewModel::useFullPassphrase,
                    onBackFromQuickPin = authenticationViewModel::backFromQuickPin,
                    onQuickPinSignIn = authenticationViewModel::signInWithQuickPin,
                    onSignIn = authenticationViewModel::signIn,
                    onBootstrapOwner = authenticationViewModel::bootstrapOwner,
                    onAcknowledgeRecoveryCode = authenticationViewModel::acknowledgeRecoveryCode,
                )
            }
        }
    }
}

@Composable
private fun SystemBarAppearance(useDarkIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
