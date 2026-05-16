package com.ads.app.gmasdk.control.helper.adnative.preload

import android.app.Activity
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadNativeAdResultCallback
import kotlinx.coroutines.launch

internal class NativeAdPreloadExecutor(private val param: SingleNativeAdPreloadParam) : NativeAdPreloadTemplate() {

    override fun requestAd(activity: Activity, buffer: Int) {
        logD("requestAd: adId:${param.idAd} - layoutId:${param.layoutId} - buffer:$buffer")
        val first = inProgress.compareAndSet(false, true)
        totalBuffer.addAndGet(buffer)
        if (first) {
            action(NativePreloadState.Start)
        }
        for (i in 0 until buffer) {
            coroutineScope.launch {
                AzAds.getInstance().loadNativeAdResultCallback(
                    activity,
                    param.idAd,
                    param.layoutId,
                    getDefaultNativeCallback()
                )
            }
        }
    }
}
