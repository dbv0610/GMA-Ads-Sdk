package com.ads.app.gmasdk.control.helper.adnative.preload

import androidx.annotation.LayoutRes

data class SingleNativeAdPreloadParam(
    val idAd: String,
    @LayoutRes val layoutId: Int
) : NativeAdPreloadParam
