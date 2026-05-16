package com.ads.app.gmasdk.control.helper.adnative.params

import androidx.annotation.LayoutRes

data class NativeLayoutMediation(
    val mediationType: AdNativeMediation,
    @LayoutRes val layoutId: Int
)
