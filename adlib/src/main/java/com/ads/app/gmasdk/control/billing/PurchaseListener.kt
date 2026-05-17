package com.ads.app.gmasdk.control.billing

interface PurchaseListener {
    fun onProductPurchased(productId: String?, transactionDetails: String)
    fun displayErrorMessage(errorMsg: String)
    fun onUserCancelBilling()
}
