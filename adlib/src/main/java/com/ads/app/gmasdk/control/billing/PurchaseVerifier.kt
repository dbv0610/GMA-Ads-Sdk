package com.ads.app.gmasdk.control.billing

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class PurchaseVerifier {

    private val _isPurchased: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isPurchasedFlow: StateFlow<Boolean> = _isPurchased.asStateFlow()

    private val ownerLock = Any()
    private val _ownerIdSubs: MutableList<PurchaseResult> = mutableListOf()
    private val _ownerIdInApp: MutableList<PurchaseResult> = mutableListOf()

    @Volatile
    private var isVerifyINAP: Boolean = false
    @Volatile
    private var isVerifySUBS: Boolean = false

    fun isPurchasedFlow(): StateFlow<Boolean> = isPurchasedFlow

    var isPurchased: Boolean
        get() = _isPurchased.value
        set(value) {
            _isPurchased.value = value
        }


    fun getOwnerIdSubs(): List<PurchaseResult> = synchronized(ownerLock) {
        _ownerIdSubs.toList()
    }

    fun getOwnerIdInApp(): List<PurchaseResult> = synchronized(ownerLock) {
        _ownerIdInApp.toList()
    }

    fun verifyAndRefresh(
        billingClient: BillingClient?,
        listINAPId: List<QueryProductDetailsParams.Product>,
        listSubsId: List<QueryProductDetailsParams.Product>,
        productIdMap: ConcurrentHashMap<QueryProductDetailsParams.Product, String>,
        shouldConsume: (String) -> Boolean,
        onComplete: ((Int) -> Unit)?
    ) {
        isVerifyINAP = false
        isVerifySUBS = false

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, list ->
            Log.d(TAG, "verifyPurchased INAPP code:${billingResult.responseCode} size:${list.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.purchaseState == 2) {
                        Log.d(TAG, "verifyPurchased INAPP: PENDING purchase ${purchase.products}")
                    } else {
                        for (id in listINAPId) {
                            val productId = productIdMap[id] ?: ""
                            if (purchase.products.contains(productId)) {
                                if (shouldConsume(productId)) {
                                    Log.i(TAG, "Re-consuming uncompleted consumable: $productId")
                                    val params = ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                    billingClient.consumeAsync(params) { r, _ ->
                                        Log.d(TAG, "Re-consume result: ${r.debugMessage}")
                                    }
                                } else {
                                    Log.i(
                                        TAG,
                                        "verifyPurchased INAPP: Order Id: ${purchase.orderId}"
                                    )
                                    addOrUpdateOwnerIdInApp(
                                        PurchaseResult.fromPurchase(purchase),
                                        productId
                                    )
                                    isPurchased = true
                                }
                            }
                        }
                    }
                }
            }
            isVerifyINAP = true
            if (isVerifySUBS) {
                onComplete?.invoke(billingResult.responseCode)
            }
        }

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, list ->
            Log.d(TAG, "verifyPurchased SUBS code:${billingResult.responseCode} size:${list.size}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.purchaseState == 2) {
                        Log.d(TAG, "verifyPurchased SUBS: PENDING purchase ${purchase.products}")
                    } else {
                        for (id in listSubsId) {
                            val productId = productIdMap[id] ?: ""
                            if (purchase.products.contains(productId)) {
                                addOrUpdateOwnerIdSub(
                                    PurchaseResult.fromPurchase(purchase),
                                    productId
                                )
                                Log.d(TAG, "verifyPurchased SUBS: true")
                                isPurchased = true
                            }
                        }
                    }
                }
            }
            isVerifySUBS = true
            if (isVerifyINAP) {
                onComplete?.invoke(billingResult.responseCode)
            }
        }
    }

    fun updatePurchaseStatus(
        billingClient: BillingClient?,
        listINAPId: List<QueryProductDetailsParams.Product>,
        listSubsId: List<QueryProductDetailsParams.Product>,
        productIdMap: ConcurrentHashMap<QueryProductDetailsParams.Product, String>,
        shouldConsume: (String) -> Boolean,
        onComplete: (() -> Unit)?
    ) {
        var doneInapp = false
        var doneSubs = false

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, list ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    for (id in listINAPId) {
                        val productId = productIdMap[id] ?: ""
                        if (purchase.products.contains(productId)) {
                            if (shouldConsume(productId)) {
                                Log.i(TAG, "updatePurchaseStatus: re-consuming: $productId")
                                val params = ConsumeParams.newBuilder()
                                    .setPurchaseToken(purchase.purchaseToken)
                                    .build()
                                billingClient.consumeAsync(params) { r, _ ->
                                    Log.d(TAG, "Re-consume result: ${r.debugMessage}")
                                }
                            } else {
                                addOrUpdateOwnerIdInApp(
                                    PurchaseResult.fromPurchase(purchase),
                                    productId
                                )
                            }
                        }
                    }
                }
            }
            doneInapp = true
            isPurchased = (getOwnerIdSubs().isNotEmpty() || getOwnerIdInApp().isNotEmpty())
            if (doneSubs) {
                onComplete?.invoke()
            }
        }

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, list ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    for (id in listSubsId) {
                        val productId = productIdMap[id] ?: ""
                        if (purchase.products.contains(productId)) {
                            addOrUpdateOwnerIdSub(PurchaseResult.fromPurchase(purchase), productId)
                        }
                    }
                }
            }
            doneSubs = true
            isPurchased = (getOwnerIdSubs().isNotEmpty() || getOwnerIdInApp().isNotEmpty())
            if (doneInapp) {
                onComplete?.invoke()
            }
        }
    }

    private fun addOrUpdateOwnerIdInApp(purchaseResult: PurchaseResult, id: String) {
        synchronized(ownerLock) {
            val index = _ownerIdInApp.indexOfFirst { it.productId?.contains(id) == true }
            if (index >= 0) {
                _ownerIdInApp[index] = purchaseResult
            } else {
                _ownerIdInApp.add(purchaseResult)
            }
        }
    }

    private fun addOrUpdateOwnerIdSub(purchaseResult: PurchaseResult, id: String) {
        synchronized(ownerLock) {
            val index = _ownerIdSubs.indexOfFirst { it.productId?.contains(id) == true }
            if (index >= 0) {
                _ownerIdSubs[index] = purchaseResult
            } else {
                _ownerIdSubs.add(purchaseResult)
            }
        }
    }

    fun getSubscriptionPurchaseToken(subsId: String): String? = synchronized(ownerLock) {
        _ownerIdSubs.find { it.productId?.contains(subsId) == true }?.purchaseToken
    }

    companion object {
        private const val TAG = "PurchaseVerifier"
    }
}
