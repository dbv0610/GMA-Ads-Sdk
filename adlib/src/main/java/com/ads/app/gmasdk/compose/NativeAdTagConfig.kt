package com.ads.app.gmasdk.compose

class NativeAdTagConfig(
    val tag: String,
    val idAds: String,
    val canShowAds: Boolean = true,
    val canReloadAds: Boolean = true,
) {
    var listId: List<String> = emptyList()
        private set

    fun setListId(list: List<String>) = apply { this.listId = list }

    fun getAllAdUnitIds(): List<String> = listId.ifEmpty { listOf(idAds) }

    companion object {
        fun simple(tag: String, adUnitId: String, canShowAds: Boolean = true) =
            NativeAdTagConfig(tag = tag, idAds = adUnitId, canShowAds = canShowAds)

        fun waterfall(tag: String, adUnitIds: List<String>, canShowAds: Boolean = true) =
            NativeAdTagConfig(
                tag = tag,
                idAds = adUnitIds.firstOrNull() ?: "",
                canShowAds = canShowAds
            ).setListId(adUnitIds)
    }
}
