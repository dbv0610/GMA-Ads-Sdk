package com.ads.app.gmasdk.control.billing

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import java.text.NumberFormat
import java.util.Currency
import java.util.concurrent.ConcurrentHashMap

class ProductDetailsRepository {

    private val skuDetailsINAPMap = ConcurrentHashMap<String, ProductDetails>()
    private val skuDetailsSubsMap = ConcurrentHashMap<String, ProductDetails>()
    val productIdMap = ConcurrentHashMap<QueryProductDetailsParams.Product, String>()

    var listINAPId: List<QueryProductDetailsParams.Product> = emptyList()
        private set
    var listSubscriptionId: List<QueryProductDetailsParams.Product> = emptyList()
        private set
    private var purchaseItems: List<PurchaseItem> = emptyList()

    fun syncPurchaseItemsToListProduct(items: List<PurchaseItem>) {
        purchaseItems = items
        val listInApp = mutableListOf<QueryProductDetailsParams.Product>()
        val listSubs = mutableListOf<QueryProductDetailsParams.Product>()
        productIdMap.clear()
        for (item in items) {
            val productType = if (item.type == 1) "inapp" else "subs"
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(item.itemId)
                .setProductType(productType)
                .build()
            if (item.type == 1) listInApp.add(product) else listSubs.add(product)
            productIdMap[product] = item.itemId
        }
        listINAPId = listInApp
        listSubscriptionId = listSubs
        Log.d(TAG, "syncPurchaseItemsToListProduct: INAPP=${listInApp.size}, SUBS=${listSubs.size}")
    }

    fun queryProducts(billingClient: BillingClient) {
        if (listINAPId.isNotEmpty()) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listINAPId)
                .build()
            billingClient.queryProductDetailsAsync(params) { _, result ->
                Log.d(TAG, "onSkuINAPDetailsResponse: ${result.productDetailsList.size}")
                for (d in result.productDetailsList) {
                    skuDetailsINAPMap[d.productId] = d
                }
            }
        }
        if (listSubscriptionId.isNotEmpty()) {
            for (item in listSubscriptionId) {
                Log.d(TAG, "queryProducts SUBS: ${productIdMap[item] ?: ""}")
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listSubscriptionId)
                .build()
            billingClient.queryProductDetailsAsync(params) { _, result ->
                Log.d(TAG, "onSkuSubsDetailsResponse: ${result.productDetailsList.size}")
                for (d in result.productDetailsList) {
                    skuDetailsSubsMap[d.productId] = d
                }
            }
        }
    }

    fun getINAPProductDetails(productId: String): ProductDetails? = skuDetailsINAPMap[productId]

    fun getSubsProductDetails(productId: String): ProductDetails? = skuDetailsSubsMap[productId]

    fun hasINAPProducts(): Boolean = skuDetailsINAPMap.isNotEmpty()

    fun hasSubsProducts(): Boolean = skuDetailsSubsMap.isNotEmpty()

    fun shouldConsume(productId: String): Boolean =
        purchaseItems.firstOrNull { it.itemId == productId }?.consume ?: false

    fun getTrialId(subsId: String): String? =
        purchaseItems.firstOrNull { it.itemId == subsId }?.trialId

    fun getPrice(productId: String): String? {
        val d = skuDetailsINAPMap[productId] ?: return ""
        Log.e(TAG, "getPrice: ${d.oneTimePurchaseOfferDetails?.formattedPrice}")
        return d.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
    }

    fun getName(productId: String, typeIap: Int): String? {
        val d = if (typeIap == 2) skuDetailsSubsMap[productId] else skuDetailsINAPMap[productId]
        return d?.name ?: ""
    }

    fun getPriceSub(productId: String): String {
        val d = skuDetailsSubsMap[productId] ?: return ""
        val offers = d.subscriptionOfferDetails ?: return ""
        val phases = offers.last().pricingPhases.pricingPhaseList
        Log.e(TAG, "getPriceSub: ${phases.last().formattedPrice}")
        return phases.last().formattedPrice
    }

    fun getPeriod(productId: String): String {
        return try {
            val d = skuDetailsSubsMap[productId] ?: return ""
            val offers = d.subscriptionOfferDetails ?: return ""
            offers.last().pricingPhases.pricingPhaseList.first().billingPeriod
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getTrialPeriod(productId: String): String {
        return try {
            val d = skuDetailsSubsMap[productId] ?: return ""
            val offers = d.subscriptionOfferDetails ?: return ""
            for (offer in offers) {
                for (phase in offer.pricingPhases.pricingPhaseList) {
                    if (phase.priceAmountMicros == 0L && phase.billingCycleCount == 1) {
                        return phase.billingPeriod
                    }
                }
            }
            ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getPricePricingPhaseList(productId: String): List<ProductDetails.PricingPhase>? {
        val d = skuDetailsSubsMap[productId] ?: return null
        val offers = d.subscriptionOfferDetails ?: return null
        return offers.last().pricingPhases.pricingPhaseList
    }

    fun getIntroductorySubPrice(productId: String): String {
        val d = skuDetailsSubsMap[productId] ?: return ""
        val oneTime = d.oneTimePurchaseOfferDetails
        val subsOffers = d.subscriptionOfferDetails
        return when {
            oneTime != null -> oneTime.formattedPrice
            subsOffers != null -> {
                val phases = subsOffers.last().pricingPhases.pricingPhaseList
                phases.last().formattedPrice
            }
            else -> ""
        }
    }

    fun getCurrency(productId: String, typeIAP: Int): String {
        val d = if (typeIAP == 1) skuDetailsINAPMap[productId] else skuDetailsSubsMap[productId]
        d ?: return ""
        return if (typeIAP == 1) {
            d.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: ""
        } else {
            val offers = d.subscriptionOfferDetails ?: return ""
            val phases = offers.last().pricingPhases.pricingPhaseList
            phases.last().priceCurrencyCode
        }
    }

    fun getPriceWithoutCurrency(productId: String, typeIAP: Int): Double {
        val d = if (typeIAP == 1) skuDetailsINAPMap[productId] else skuDetailsSubsMap[productId]
        d ?: return 0.0
        return if (typeIAP == 1) {
            d.oneTimePurchaseOfferDetails?.priceAmountMicros?.toDouble() ?: 0.0
        } else {
            val offers = d.subscriptionOfferDetails ?: return 0.0
            val phases = offers.last().pricingPhases.pricingPhaseList
            phases.last().priceAmountMicros.toDouble()
        }
    }

    fun getPriceWithCurrency(productId: String, typeIAP: Int): String {
        return try {
            val price = getPriceWithoutCurrency(productId, typeIAP) / 1_000_000.0
            formatCurrency(price, getCurrency(productId, typeIAP))
        } catch (e: Exception) {
            ""
        }
    }

    fun getPriceWithCurrency(productId: String, typeIAP: Int, sale: Double): String {
        return try {
            val price = getPriceWithoutCurrency(productId, typeIAP) / 1_000_000.0
            formatCurrency(price / sale, getCurrency(productId, typeIAP))
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatCurrency(price: Double, currency: String): String {
        if (currency.isEmpty()) return ""
        val format = NumberFormat.getCurrencyInstance().apply {
            maximumFractionDigits = 0
            setCurrency(Currency.getInstance(currency))
        }
        return format.format(price)
    }

    fun getListInAppId(): List<String> = listINAPId.map { productIdMap[it] ?: "" }

    fun getListSubId(): List<String> = listSubscriptionId.map { productIdMap[it] ?: "" }

    companion object {
        private const val TAG = "ProductDetailsRepo"
    }
}
