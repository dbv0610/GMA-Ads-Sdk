package com.ads.app.gmasdk.control.helper.adnative

import android.util.Log
import androidx.annotation.LayoutRes
import com.ads.app.gmasdk.control.helper.IAdsConfig
import com.ads.app.gmasdk.control.helper.adnative.params.AdNativeMediation
import com.ads.app.gmasdk.control.helper.adnative.params.NativeLayoutMediation
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

class NativeAdConfig(
    override val idAds: String,
    override val canShowAds: Boolean,
    override val canReloadAds: Boolean,
    @LayoutRes val layoutId: Int
) : IAdsConfig {
    var listLayoutByMediation: List<NativeLayoutMediation> = emptyList()
    var listId: List<String> = emptyList()
    var listTimeout: List<Long> = emptyList()

    fun setListId(list: List<String>): NativeAdConfig {
        listId = list
        return this
    }

    fun setListTimeout(list: List<Long>): NativeAdConfig {
        listTimeout = list
        return this
    }

    fun setLayoutMediation(vararg layoutMediation: NativeLayoutMediation): NativeAdConfig {
        listLayoutByMediation = layoutMediation.toList()
        return this
    }

    fun setLayoutMediation(listLayoutMediation: List<NativeLayoutMediation>): NativeAdConfig {
        listLayoutByMediation = listLayoutMediation
        return this
    }

    @LayoutRes
    fun getLayoutIdByMediationNativeAd(nativeAd: NativeAd?): Int {
        if (listLayoutByMediation.isNotEmpty() && nativeAd != null) {
            val currentMediation = AdNativeMediation.get(nativeAd)
            for (it in listLayoutByMediation) {
                if (currentMediation == it.mediationType) {
                    Log.d("NativeAdHelper", "show with mediation ${it.mediationType.name}")
                    return it.layoutId
                }
            }
        }
        return layoutId
    }
}
