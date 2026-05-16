package com.ads.app.gmasdk.control.helper.adnative.preload

import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd

sealed class NativePreloadState {
    data object None : NativePreloadState()
    data object Start : NativePreloadState()
    data class Consume(val apNativeAd: ApNativeAd) : NativePreloadState()
    data object Complete : NativePreloadState()
    data object Error : NativePreloadState()
}
