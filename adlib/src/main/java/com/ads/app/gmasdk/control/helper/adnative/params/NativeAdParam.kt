package com.ads.app.gmasdk.control.helper.adnative.params

import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.helper.params.IAdsParam

sealed class NativeAdParam : IAdsParam {
    data class Ready(val nativeAd: ApNativeAd) : NativeAdParam()

    sealed class Request : NativeAdParam() {
        data object CreateRequest : Request()
        data object ResumeRequest : Request()

        companion object {
            @JvmStatic
            fun create(): Request = CreateRequest
        }
    }
}
