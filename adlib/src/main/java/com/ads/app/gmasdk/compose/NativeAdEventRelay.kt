package com.ads.app.gmasdk.compose

import com.ads.app.gmasdk.control.ads.AzAdCallback
import java.util.concurrent.CopyOnWriteArrayList

class NativeAdEventRelay {

    private val listeners = CopyOnWriteArrayList<AzAdCallback>()

    fun addListener(callback: AzAdCallback) {
        listeners.addIfAbsent(callback)
    }

    fun removeListener(callback: AzAdCallback) {
        listeners.remove(callback)
    }

    fun clearListeners() {
        listeners.clear()
    }

    internal fun onAdImpression() {
        listeners.forEach { it.onAdImpression() }
    }

    internal fun onAdClicked() {
        listeners.forEach { it.onAdClicked() }
    }

    internal fun onAdClosed() {
        listeners.forEach { it.onAdClosed() }
    }
}
