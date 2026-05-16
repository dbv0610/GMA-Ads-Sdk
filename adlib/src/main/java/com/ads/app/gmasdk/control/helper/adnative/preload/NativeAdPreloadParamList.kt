package com.ads.app.gmasdk.control.helper.adnative.preload

import androidx.annotation.LayoutRes

data class NativeAdPreloadParamList(
    val listId: List<String>,
    @LayoutRes val layoutId: Int
) : NativeAdPreloadParam
