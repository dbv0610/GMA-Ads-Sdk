package com.ads.app.gmasdk.control.helper.adnative.callback

import com.ads.app.gmasdk.control.ads.AzAdCallback
import java.util.concurrent.CopyOnWriteArrayList

class NativeAdCallback {
    private val listAdCallback = CopyOnWriteArrayList<AzAdCallback>()

    fun registerAdListener(adCallback: AzAdCallback) {
        listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: AzAdCallback) {
        listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        listAdCallback.clear()
    }

    private fun invokeAdListener(action: (AzAdCallback) -> Unit) {
        listAdCallback.forEach { action(it) }
    }

    fun invokeListenerAdCallback(internalAdCallback: AzAdCallback?): AzAdCallback {
        return object : AzAdCallback() {
            override fun onAdLoaded() {
                internalAdCallback?.onAdLoaded()
                invokeAdListener { it.onAdLoaded() }
            }
            override fun onAdFailedToLoad(adError: com.ads.app.gmasdk.control.ads.wrapper.ApAdError?) {
                internalAdCallback?.onAdFailedToLoad(adError)
                invokeAdListener { it.onAdFailedToLoad(adError) }
            }
            override fun onNativeAdLoaded(nativeAd: com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd) {
                internalAdCallback?.onNativeAdLoaded(nativeAd)
                invokeAdListener { it.onNativeAdLoaded(nativeAd) }
            }
            override fun onAdClicked() {
                internalAdCallback?.onAdClicked()
                invokeAdListener { it.onAdClicked() }
            }
            override fun onAdImpression() {
                internalAdCallback?.onAdImpression()
                invokeAdListener { it.onAdImpression() }
            }
        }
    }
}
