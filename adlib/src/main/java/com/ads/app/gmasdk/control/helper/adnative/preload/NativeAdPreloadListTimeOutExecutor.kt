package com.ads.app.gmasdk.control.helper.adnative.preload

import android.app.Activity
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadNativeListTimeOut
import kotlinx.coroutines.launch

class NativeAdPreloadListTimeOutExecutor(val param: NativeAdPreloadParamListTimeOut) : NativeAdPreloadTemplate() {

    override val TAG: String = "NativeAdPreload_List_Timeout"

    init {
        priorityOrder = param.listId
    }

    override fun requestAd(activity: Activity, buffer: Int) {
        logD("requestAd: list_timeout: ${param.listId} - buffer: $buffer")
        val first = inProgress.compareAndSet(false, true)
        totalBuffer.addAndGet(buffer)
        if (first) {
            action(NativePreloadState.Start)
        }
        for (i in 0 until buffer) {
            coroutineScope.launch {
                AzAds.getInstance().loadNativeListTimeOut(
                    activity,
                    param.listId,
                    param.listTimeOut,
                    param.layoutId,
                    getDefaultNativeCallback()
                )
            }
        }
    }

    override fun messageLog(message: String): String = "[INFO] [List_Timeout] $message"
}
