package com.ads.app.gmasdk.control.helper.adnative.preload

import android.app.Activity
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadNativeList
import kotlinx.coroutines.launch

class NativeAdPreloadListExecutor(val param: NativeAdPreloadParamList) : NativeAdPreloadTemplate() {

    override val TAG: String = "NativeAdPreload_List"

    init {
        priorityOrder = param.listId
    }

    override fun requestAd(activity: Activity, buffer: Int) {
        logD("requestAd: list: ${param.listId} - buffer: $buffer")
        val first = inProgress.compareAndSet(false, true)
        totalBuffer.addAndGet(buffer)
        if (first) {
            action(NativePreloadState.Start)
        }
        for (i in 0 until buffer) {
            coroutineScope.launch {
                AzAds.getInstance().loadNativeList(
                    activity,
                    param.listId,
                    param.layoutId,
                    getDefaultNativeCallback()
                )
            }
        }
    }

    override fun messageLog(message: String): String = "[INFO] [List] $message"
}
