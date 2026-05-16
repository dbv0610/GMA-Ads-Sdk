package com.ads.app.gmasdk.control.helper.banner

import com.ads.app.gmasdk.control.helper.IAdsConfig

open class BannerAdConfig(
    override val idAds: String,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean
) : IAdsConfig {
    var collapsibleGravity: String? = null
    var listId: List<String> = emptyList()
    var usingInlineBanner: Boolean = false
    var maxHeight: Int = 50

    fun setListId(list: List<String>): BannerAdConfig {
        listId = list
        return this
    }

    fun setUsingInlineBanner(usingInline: Boolean): BannerAdConfig {
        usingInlineBanner = usingInline
        return this
    }

    fun setMaxHeight(maxHeightDp: Int): BannerAdConfig {
        maxHeight = maxHeightDp
        return this
    }

    fun asInlineBanner(maxHeightDp: Int = 50): BannerAdConfig {
        usingInlineBanner = true
        maxHeight = maxHeightDp
        return this
    }
}
