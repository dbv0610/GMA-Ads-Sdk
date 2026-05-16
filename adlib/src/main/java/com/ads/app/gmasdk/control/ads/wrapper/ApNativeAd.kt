package com.ads.app.gmasdk.control.ads.wrapper

import android.view.View
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

class ApNativeAd : ApAdBase {
    var layoutCustomNative: Int = 0
    var nativeView: View? = null

    @JvmField
    var admobNativeAd: NativeAd? = null

    constructor() : super()

    constructor(status: StatusAd) : super(status)

    constructor(layoutCustomNative: Int, nativeView: View) : super(StatusAd.AD_LOADED) {
        this.layoutCustomNative = layoutCustomNative
        this.nativeView = nativeView
    }

    constructor(layoutCustomNative: Int, admobNativeAd: NativeAd) : super(StatusAd.AD_LOADED) {
        this.layoutCustomNative = layoutCustomNative
        this.admobNativeAd = admobNativeAd
    }

    fun setAdmobNativeAd(admobNativeAd: NativeAd?) {
        this.admobNativeAd = admobNativeAd
        if (admobNativeAd != null) status = StatusAd.AD_LOADED
    }

    override fun isReady() = nativeView != null || admobNativeAd != null

    override fun toString() =
        "Status:$status == nativeView:$nativeView == admobNativeAd:$admobNativeAd"
}
