package com.ads.app.gmasdk.control.helper.adnative.params

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

enum class AdNativeMediation(val clazz: String) {
    ADMOB("AdMobAdapter"),
    FACEBOOK("FacebookMediationAdapter"),
    APPLOVIN("AppLovinMediationAdapter"),
    MINTEGRAL("MintegralMediationAdapter"),
    PANGLE("PangleMediationAdapter"),
    VUNGLE("VungleMediationAdapter");

    companion object {
        fun get(nativeAd: NativeAd): AdNativeMediation? {
            val adapterClassName = nativeAd.getResponseInfo().adapterClassName ?: return null
            return entries.firstOrNull { adapterClassName.contains(it.clazz) }
        }
    }
}
