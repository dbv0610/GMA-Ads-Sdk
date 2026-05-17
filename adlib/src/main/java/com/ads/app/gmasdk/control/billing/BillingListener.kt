package com.ads.app.gmasdk.control.billing

fun interface BillingListener {
    fun onInitBillingFinished(resultCode: Int)
}
