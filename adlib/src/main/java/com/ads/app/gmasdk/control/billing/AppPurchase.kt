package com.ads.app.gmasdk.control.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import com.ads.app.gmasdk.control.util.AppUtil
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppPurchase private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timeoutJob: Job? = null

    private var connectionManager: BillingConnectionManager? = null
    private var repository: ProductDetailsRepository? = null
    private var verifier: PurchaseVerifier? = null
    private var processor: PurchaseProcessor? = null

    private var billingListener: BillingListener? = null
    private var updatePurchaseListener: UpdatePurchaseListener? = null
    private var enableTrackingRevenue: Boolean = false
    private var discount: Double = 1.0

    val billingState: StateFlow<BillingState>?
        get() = connectionManager?.state

    fun isPurchasedFlow(): StateFlow<Boolean>? = verifier?.isPurchasedFlow()

    fun setBillingListener(billingListener: BillingListener?) {
        this.billingListener = billingListener
        if (isAvailable() || getInitBillingFinish()) {
            billingListener?.onInitBillingFinished(0)
        }
    }

    fun setBillingListener(billingListener: BillingListener?, timeout: Int) {
        Log.d(TAG, "setBillingListener: timeout=$timeout")
        this.billingListener = billingListener
        if (getInitBillingFinish() || isAvailable()) {
            Log.d(TAG, "setBillingListener: already finished")
            billingListener?.onInitBillingFinished(0)
            return
        }
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(timeout.toLong())
            Log.d(TAG, "setBillingListener: timeout fired")
            billingListener?.onInitBillingFinished(BillingClient.BillingResponseCode.ERROR)
        }
    }

    fun setUpdatePurchaseListener(listener: UpdatePurchaseListener?) {
        this.updatePurchaseListener = listener
    }

    fun setEnableTrackingRevenue(enable: Boolean) {
        enableTrackingRevenue = enable
    }

    fun getEnableTrackingRevenue(): Boolean = enableTrackingRevenue

    fun setDiscount(discount: Double) {
        this.discount = discount
    }

    fun getDiscount(): Double = discount

    fun initBilling(application: Application, purchaseItemList: MutableList<PurchaseItem>) {
        if (connectionManager != null) {
            Log.w(TAG, "initBilling: already initialized, skipping")
            return
        }
        if (AppUtil.VARIANT_DEV) {
            purchaseItemList.add(PurchaseItem("android.test.purchased", 1, "", false))
        }
        val repo = ProductDetailsRepository()
        val vfy = PurchaseVerifier()
        val connMgr = BillingConnectionManager()
        repository = repo
        verifier = vfy
        connectionManager = connMgr

        repo.syncPurchaseItemsToListProduct(purchaseItemList)

        val proc = PurchaseProcessor(connMgr, repo, vfy).apply {
            onPurchaseStatusUpdate = { triggerUpdatePurchaseStatus() }
        }
        processor = proc

        connMgr.connect(
            application,
            purchasesUpdatedListener = { billingResult, purchases ->
                proc.onPurchasesUpdated(billingResult, purchases)
            },
            onSetupFinished = { billingResult: BillingResult, isFirstSetup: Boolean ->
                if (isFirstSetup) {
                    vfy.verifyAndRefresh(
                        connMgr.getBillingClient(),
                        repo.listINAPId,
                        repo.listSubscriptionId,
                        repo.productIdMap,
                        shouldConsume = { productId -> repo.shouldConsume(productId) },
                        onComplete = { code ->
                            timeoutJob?.cancel()
                            billingListener?.onInitBillingFinished(code)
                        }
                    )
                }
                when (billingResult.responseCode) {
                    0 -> {
                        val client = connMgr.getBillingClient()
                        if (client != null) {
                            repo.queryProducts(client)
                        }
                    }
                    2, 6 -> Log.e(TAG, "onBillingSetupFinished: ERROR")
                }
            }
        )
    }

    private fun triggerUpdatePurchaseStatus() {
        val repo = repository ?: return
        val vfy = verifier ?: return
        vfy.updatePurchaseStatus(
            connectionManager?.getBillingClient(),
            repo.listINAPId,
            repo.listSubscriptionId,
            repo.productIdMap,
            shouldConsume = { productId -> repo.shouldConsume(productId) },
            onComplete = { updatePurchaseListener?.onUpdateFinished() }
        )
    }

    fun isAvailable(): Boolean = connectionManager?.isAvailable ?: false

    fun getInitBillingFinish(): Boolean = connectionManager?.isInitBillingFinish ?: false

    fun isPurchased(): Boolean = verifier?.isPurchased ?: false

    fun setPurchase(purchase: Boolean) {
        verifier?.isPurchased = purchase
    }

    fun getOwnerIdSubs(): List<PurchaseResult> = verifier?.getOwnerIdSubs() ?: emptyList()

    fun getOwnerIdInApp(): List<PurchaseResult> = verifier?.getOwnerIdInApp() ?: emptyList()

    fun purchase(activity: Activity, productId: String, onResult: (PurchaseEvent) -> Unit) {
        val proc = processor
        if (proc != null) {
            proc.purchase(activity, productId, onResult)
        } else {
            onResult(PurchaseEvent.Error(6, "Billing not initialized"))
        }
    }

    fun subscribe(activity: Activity, subsId: String, onResult: (PurchaseEvent) -> Unit) {
        val proc = processor
        if (proc != null) {
            proc.subscribe(activity, subsId, onResult)
        } else {
            onResult(PurchaseEvent.Error(6, "Billing not initialized"))
        }
    }

    fun upgradeSubscription(
        activity: Activity,
        newSubsId: String,
        oldPurchaseToken: String,
        replacementMode: Int = 0,
        onResult: (PurchaseEvent) -> Unit
    ) {
        val proc = processor
        if (proc != null) {
            proc.upgradeSubscription(activity, newSubsId, oldPurchaseToken, replacementMode, onResult)
        } else {
            onResult(PurchaseEvent.Error(6, "Billing not initialized"))
        }
    }

    fun getSubscriptionPurchaseToken(subsId: String): String? =
        verifier?.getSubscriptionPurchaseToken(subsId)

    fun consumePurchase(productId: String) {
        processor?.consumePurchase(productId)
    }

    fun updatePurchaseStatus() {
        triggerUpdatePurchaseStatus()
    }

    fun getProductInfoList(): List<BillingProductInfo> =
        repository?.getProductInfoList() ?: emptyList()

    fun getInAppProductIds(): List<String> = repository?.getInAppProductIds() ?: emptyList()

    fun getSubscriptionProductIds(): List<String> = repository?.getSubscriptionProductIds() ?: emptyList()

    fun getAllProductIds(): List<String> = repository?.getAllProductIds() ?: emptyList()

    fun getPrice(productId: String): String =
        repository?.getPrice(productId) ?: ""

    fun getName(productId: String, typeIap: Int): String =
        repository?.getName(productId, typeIap) ?: ""

    fun getPriceSub(productId: String): String =
        repository?.getPriceSub(productId) ?: ""

    fun getPeriod(productId: String): String =
        repository?.getPeriod(productId) ?: ""

    fun getTrialPeriod(productId: String): String =
        repository?.getTrialPeriod(productId) ?: ""

    fun getIntroductorySubPrice(productId: String, offerId: String? = null): String =
        repository?.getIntroductorySubPrice(productId, offerId) ?: ""

    fun getCurrency(productId: String, typeIAP: Int): String =
        repository?.getCurrency(productId, typeIAP) ?: ""

    fun getPriceWithoutCurrency(productId: String, typeIAP: Int): Double =
        repository?.getPriceWithoutCurrency(productId, typeIAP) ?: 0.0

    fun getPriceWithCurrency(productId: String, typeIAP: Int): String =
        repository?.getPriceWithCurrency(productId, typeIAP) ?: ""

    fun getPriceWithCurrency(productId: String, typeIAP: Int, sale: Double): String =
        repository?.getPriceWithCurrency(productId, typeIAP, sale) ?: ""

    fun getPricePricingPhaseList(productId: String): List<ProductDetails.PricingPhase>? =
        repository?.getPricePricingPhaseList(productId)

    fun getListInAppId(): List<String> = repository?.getListInAppId() ?: emptyList()

    fun getListSubId(): List<String> = repository?.getListSubId() ?: emptyList()

    object TYPE_IAP {
        const val PURCHASE = 1
        const val SUBSCRIPTION = 2
    }

    @Retention(AnnotationRetention.SOURCE)
    annotation class TypeIAPAnnotation

    companion object {
        private const val TAG = "PurchaseEG"
        const val PRODUCT_ID_TEST = "android.test.purchased"

        @Volatile
        private var instance: AppPurchase? = null

        @JvmStatic
        fun getInstance(): AppPurchase =
            instance ?: synchronized(AppPurchase::class.java) {
                instance ?: AppPurchase().also { instance = it }
            }
    }
}
