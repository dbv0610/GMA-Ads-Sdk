package com.ads.app.gmasdk.control.helper.banner.params

import com.ads.app.gmasdk.control.helper.params.IAdsParam
import com.google.android.libraries.ads.mobile.sdk.banner.AdView

sealed class BannerAdParam : IAdsParam {
    data class Ready(val bannerAds: AdView) : BannerAdParam()

    data object Request : BannerAdParam() {
        @JvmStatic
        fun create(): Request = Request
    }

    data class Clickable(val minimumTimeKeepAdsDisplay: Long) : BannerAdParam()
}
