package com.ads.app.gmasdk.control.helper.adnative.params

import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd

sealed class AdNativeState {
    data object None : AdNativeState()
    data object Fail : AdNativeState()
    data object Loading : AdNativeState()
    data object Cancel : AdNativeState()
    data class Loaded(val adNative: ApNativeAd) : AdNativeState()
}
