package com.ads.app.gmasdk.control.helper.banner.params

import com.google.android.libraries.ads.mobile.sdk.banner.AdView

sealed class AdBannerState {
    data object None : AdBannerState()
    data object Fail : AdBannerState()
    data object Loading : AdBannerState()
    data object Cancel : AdBannerState()
    data class Loaded(val adBanner: AdView) : AdBannerState()
}
