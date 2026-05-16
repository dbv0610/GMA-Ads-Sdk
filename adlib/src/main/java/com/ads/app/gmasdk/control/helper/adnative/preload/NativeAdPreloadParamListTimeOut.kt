package com.ads.app.gmasdk.control.helper.adnative.preload

import androidx.annotation.LayoutRes

data class NativeAdPreloadParamListTimeOut(
    val listId: List<String>,
    val listTimeOut: List<Long>,
    @LayoutRes val layoutId: Int
) : NativeAdPreloadParam
