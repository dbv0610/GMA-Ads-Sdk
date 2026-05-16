package com.ads.app.gmasdk.control.ads.wrapper

abstract class ApAdBase(var status: StatusAd = StatusAd.AD_INIT) {
    abstract fun isReady(): Boolean
    fun isNotReady() = !isReady()
    fun isLoading() = status == StatusAd.AD_LOADING
    fun isLoadFail() = status == StatusAd.AD_LOAD_FAIL
}
