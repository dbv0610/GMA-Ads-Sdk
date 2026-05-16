package com.ads.app.gmasdk.control.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.app.gmasdk.control.event.AzLogEventManager
import com.ads.app.gmasdk.control.event.FirebaseAnalyticsUtil
import com.ads.app.gmasdk.control.util.AppUtil
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams

class PurchaseProcessor(
    private val connectionManager: BillingConnectionManager,
    private val repository: ProductDetailsRepository,
    private val verifier: PurchaseVerifier
) {

    var onPurchaseStatusUpdate: (() -> Unit)? = null

    private var idPurchaseCurrent: String = ""
    private var typeIap: Int = 0
    private var retryConsumeTimes: Int = 0
    private var purchaseEventCallback: ((PurchaseEvent) -> Unit)? = null

    fun purchase(activity: Activity, productId: String, onResult: (PurchaseEvent) -> Unit) {
        if (AppUtil.VARIANT_DEV) {
            val productDetails = repository.getINAPProductDetails(productId)
            PurchaseDevBottomSheet(1, productId, productDetails, activity as Context, onResult).show()
            return
        }
        if (!repository.hasINAPProducts()) {
            onResult(PurchaseEvent.Error(6, "Billing error init"))
            return
        }
        val productDetails = repository.getINAPProductDetails(productId)
        if (productDetails == null) {
            onResult(PurchaseEvent.Error(6, "Not found item with id: $productId"))
            return
        }
        Log.d(TAG, "purchase: $productDetails")
        purchaseEventCallback = onResult
        idPurchaseCurrent = productId
        typeIap = 1

        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()

        val client = connectionManager.getBillingClient()
        if (client == null) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(6, "Billing not connected"))
            return
        }
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(result.responseCode, billingResultToMessage(result.responseCode)))
        }
    }

    fun subscribe(activity: Activity, subsId: String, onResult: (PurchaseEvent) -> Unit) {
        if (AppUtil.VARIANT_DEV) {
            val subsDetails = repository.getSubsProductDetails(subsId)
            PurchaseDevBottomSheet(2, subsId, subsDetails, activity as Context, onResult).show()
            return
        }
        if (!repository.hasSubsProducts()) {
            onResult(PurchaseEvent.Error(6, "Billing error init"))
            return
        }
        val skuDetails = repository.getSubsProductDetails(subsId)
        if (skuDetails == null) {
            onResult(PurchaseEvent.Error(6, "Product ID invalid"))
            return
        }
        val offers = skuDetails.subscriptionOfferDetails
        if (offers.isNullOrEmpty()) {
            onResult(PurchaseEvent.Error(6, "Can't find offer for this subscription"))
            return
        }
        val offerToken = selectOfferToken(skuDetails, subsId)
        Log.d(TAG, "subscribe: offerToken: $offerToken")
        purchaseEventCallback = onResult
        idPurchaseCurrent = subsId
        typeIap = 2

        val paramsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(skuDetails)
                .setOfferToken(offerToken)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(paramsList)
            .build()

        val billingClient = connectionManager.getBillingClient()
        val result = billingClient?.launchBillingFlow(activity, flowParams)
        val responseCode = result?.responseCode ?: 6
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(responseCode, billingResultToMessage(responseCode)))
        }
    }

    fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.e(TAG, "onPurchasesUpdated code: ${billingResult.responseCode}")
        val responseCode = billingResult.responseCode
        when {
            responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
            responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseEventCallback?.invoke(PurchaseEvent.Cancelled)
                purchaseEventCallback = null
                Log.d(TAG, "onPurchasesUpdated: USER_CANCELED")
            }
            else -> {
                purchaseEventCallback?.invoke(
                    PurchaseEvent.Error(billingResult.responseCode, billingResult.debugMessage)
                )
                purchaseEventCallback = null
                Log.d(TAG, "onPurchasesUpdated: ...")
            }
        }
    }

    fun upgradeSubscription(
        activity: Activity,
        newSubsId: String,
        oldPurchaseToken: String,
        replacementMode: Int = 0,
        onResult: (PurchaseEvent) -> Unit
    ) {
        purchaseEventCallback = onResult
        val skuDetails = repository.getSubsProductDetails(newSubsId)
        if (skuDetails == null) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(6, "Product ID invalid: $newSubsId"))
            return
        }
        val offerToken = selectOfferToken(skuDetails, newSubsId)
        idPurchaseCurrent = newSubsId
        typeIap = 2

        val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(oldPurchaseToken)
            .setSubscriptionReplacementMode(replacementMode)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(skuDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .setSubscriptionUpdateParams(updateParams)
            .build()

        val client = connectionManager.getBillingClient()
        if (client == null) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(6, "Billing not connected"))
            return
        }
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseEventCallback = null
            onResult(PurchaseEvent.Error(result.responseCode, billingResultToMessage(result.responseCode)))
        }
    }

    private fun selectOfferToken(skuDetails: ProductDetails, subsId: String): String {
        val subsDetail = skuDetails.subscriptionOfferDetails ?: return ""
        val trialId = repository.getTrialId(subsId)
        for (item in subsDetail) {
            val offerId = item.offerId
            if (offerId != null && offerId == trialId) {
                return item.offerToken
            }
        }
        return subsDetail.last().offerToken
    }

    fun handlePurchase(purchase: Purchase) {
        val priceMicros = repository.getPriceWithoutCurrency(idPurchaseCurrent, typeIap).toLong()
        val price = priceMicros / 1_000_000.0
        val currency = repository.getCurrency(idPurchaseCurrent, typeIap)

        if (typeIap == 2) {
            AzLogEventManager.onTrackRevenueSubscription(
                priceMicros,
                currency,
                idPurchaseCurrent,
                purchase.orderId.toString(),
                purchase.signature,
                purchase.purchaseToken
            )
        } else {
            AzLogEventManager.onTrackRevenuePurchase(
                price.toFloat(),
                currency,
                idPurchaseCurrent,
                purchase.purchaseToken,
                typeIap
            )
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                Log.d(TAG, "handlePurchase: PENDING — awaiting payment completion")
                purchaseEventCallback?.invoke(PurchaseEvent.Pending(idPurchaseCurrent))
                purchaseEventCallback = null
                return
            }
            Purchase.PurchaseState.PURCHASED -> {
                val consume = repository.shouldConsume(idPurchaseCurrent)
                if (!consume) {
                    verifier.isPurchased = true
                }
                purchaseEventCallback?.invoke(
                    PurchaseEvent.Success(purchase.orderId, idPurchaseCurrent, purchase.originalJson)
                )
                purchaseEventCallback = null

                val billingClient = connectionManager.getBillingClient()
                if (consume) {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.consumeAsync(consumeParams) { billingResult, _ ->
                        Log.d(TAG, "onConsumeResponse: ${billingResult.debugMessage}")
                    }
                } else {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    if (!purchase.isAcknowledged) {
                        billingClient?.acknowledgePurchase(ackParams) { billingResult ->
                            Log.d(TAG, "onAcknowledgePurchaseResponse: ${billingResult.debugMessage}")
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                val orderId = purchase.orderId
                                if (orderId != null) {
                                    FirebaseAnalyticsUtil.logConfirmPurchaseGoogle(
                                        orderId,
                                        idPurchaseCurrent,
                                        purchase.purchaseToken
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Log.w(TAG, "handlePurchase: unknown state ${purchase.purchaseState}, skipping")
            }
        }
    }

    fun consumePurchase(productId: String) {
        Log.d(TAG, "consumePurchase: $productId")
        retryConsumeTimes = 0
        val billingClient = connectionManager.getBillingClient()
        val queryParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient?.queryPurchasesAsync(queryParams) { billingResult, list ->
            var pc: Purchase? = null
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in list) {
                    if (purchase.products.contains(productId)) {
                        pc = purchase
                    }
                }
            }
            if (pc == null) return@queryPurchasesAsync
            try {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(pc.purchaseToken)
                    .build()
                billingClient.consumeAsync(consumeParams) { result, _ ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "onConsumeResponse: OK")
                        retryConsumeTimes = 0
                        onPurchaseStatusUpdate?.invoke()
                    } else {
                        Log.e(TAG, "consumePurchase: error $result")
                        if (retryConsumeTimes >= MAX_RETRY_CONSUME_TIMES) {
                            retryConsumeTimes = 0
                        } else {
                            retryConsumeTimes++
                            consumePurchase(productId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "consumePurchase: error", e)
            }
        }
    }

    private fun billingResultToMessage(responseCode: Int): String = when (responseCode) {
        3 -> "Billing not supported for type of request"
        6 -> "Error completing request"
        2 -> "Network Connection down"
        1 -> "Request Canceled"
        -2 -> "Error processing request."
        7 -> "Selected item is already owned"
        4 -> "Item not available"
        -1 -> "Play Store service is not connected now"
        -3 -> "Timeout"
        0 -> "Subscribed Successfully"
        5, 8 -> ""
        else -> ""
    }

    companion object {
        private const val TAG = "PurchaseProcessor"
        private const val MAX_RETRY_CONSUME_TIMES = 1
    }
}
