package com.ads.app.gmasdk.control.helper.adnative.preload

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.LayoutRes
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.helper.adnative.callback.NativeAdCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NativeAdPreload private constructor() {

    private val executors = HashMap<IKeyPreload, NativeAdPreloadTemplate>()

    fun preload(activity: Activity, adId: String, @LayoutRes layoutId: Int, buffer: Int) {
        AzAds.getInstance().runWhenReady {
            if (!canRequestLoad(activity)) {
                Log.d("NativeAdPreload", "Do not preload because canRequestLoad = false")
                return@runWhenReady
            }
            val key = KeyPreload(adId, layoutId)
            val executor = executors.getOrPut(key) { NativeAdPreloadExecutor(SingleNativeAdPreloadParam(adId, layoutId)) }
            executePreload(key, executor, activity, buffer)
        }
    }

    fun preload(activity: Activity, listId: List<String>, @LayoutRes layoutId: Int, buffer: Int) {
        AzAds.getInstance().runWhenReady {
            if (!canRequestLoad(activity)) {
                Log.d("NativeAdPreload", "Do not preload because canRequestLoad = false")
                return@runWhenReady
            }
            val key = KeyPreloadList(listId, layoutId)
            val executor = executors.getOrPut(key) { NativeAdPreloadListExecutor(NativeAdPreloadParamList(listId, layoutId)) }
            executePreload(key, executor, activity, buffer)
        }
    }

    fun preload(activity: Activity, listId: List<String>, listTimeout: List<Long>, @LayoutRes layoutId: Int, buffer: Int) {
        AzAds.getInstance().runWhenReady {
            if (!canRequestLoad(activity)) {
                Log.d("NativeAdPreload", "Do not preload because canRequestLoad = false")
                return@runWhenReady
            }
            val key = KeyPreloadList(listId, layoutId)
            val executor = executors.getOrPut(key) { NativeAdPreloadListTimeOutExecutor(NativeAdPreloadParamListTimeOut(listId, listTimeout, layoutId)) }
            executePreload(key, executor, activity, buffer)
        }
    }

    fun preload(activity: Activity, adId: String, @LayoutRes layoutId: Int) =
        preload(activity, adId, layoutId, 1)

    fun preload(activity: Activity, listId: List<String>, @LayoutRes layoutId: Int) =
        preload(activity, listId, layoutId, 1)

    private fun executePreload(keyPreload: IKeyPreload, executor: NativeAdPreloadTemplate, activity: Activity, buffer: Int) {
        executors[keyPreload] = executor
        executor.execute(activity, buffer)
    }

    fun canRequestLoad(context: Context): Boolean =
        !AppPurchase.getInstance().isPurchased() && isOnline(context)

    fun getAdPreloadState(adId: String, @LayoutRes layoutId: Int): StateFlow<NativePreloadState> {
        return executors[KeyPreload(adId, layoutId)]?.getAdPreloadState()
            ?: MutableStateFlow(NativePreloadState.None)
    }

    fun getAdPreloadState(listId: List<String>, @LayoutRes layoutId: Int): StateFlow<NativePreloadState> {
        return executors[KeyPreloadList(listId, layoutId)]?.getAdPreloadState()
            ?: MutableStateFlow(NativePreloadState.None)
    }

    fun pollAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreload(adId, layoutId)]?.pollAdNative()

    fun pollAdNative(listId: List<String>, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreloadList(listId, layoutId)]?.pollAdNative()

    suspend fun pollOrAwaitAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreload(adId, layoutId)]?.pollOrAwaitAdNative()

    suspend fun pollOrAwaitAdNativeList(listId: List<String>, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreloadList(listId, layoutId)]?.pollOrAwaitAdNative()

    fun getAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreload(adId, layoutId)]?.getAdNative()

    fun getAdNative(listId: List<String>, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreloadList(listId, layoutId)]?.getAdNative()

    suspend fun getOrAwaitAdNative(adId: String, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreload(adId, layoutId)]?.getOrAwaitAdNative()

    suspend fun getOrAwaitAdNative(listId: List<String>, @LayoutRes layoutId: Int): ApNativeAd? =
        executors[KeyPreloadList(listId, layoutId)]?.getOrAwaitAdNative()

    fun isPreloadAvailable(adId: String, @LayoutRes layoutId: Int): Boolean =
        executors[KeyPreload(adId, layoutId)]?.isPreloadAvailable() == true

    fun isPreloadAvailable(listId: List<String>, @LayoutRes layoutId: Int): Boolean =
        executors[KeyPreloadList(listId, layoutId)]?.isPreloadAvailable() == true

    fun isPreloadInProcess(adId: String, @LayoutRes layoutId: Int): Boolean =
        executors[KeyPreload(adId, layoutId)]?.isInProgress() == true

    fun isPreloadInProcess(listId: List<String>, @LayoutRes layoutId: Int): Boolean =
        executors[KeyPreloadList(listId, layoutId)]?.isInProgress() == true

    fun getNativeAdBuffer(adId: String, @LayoutRes layoutId: Int): List<ApNativeAd> =
        executors[KeyPreload(adId, layoutId)]?.getNativeAdBuffer() ?: emptyList()

    fun getNativeAdBuffer(listId: List<String>, @LayoutRes layoutId: Int): List<ApNativeAd> =
        executors[KeyPreloadList(listId, layoutId)]?.getNativeAdBuffer() ?: emptyList()

    fun registerAdCallback(adId: String, @LayoutRes layoutId: Int, adCallback: AzAdCallback) {
        executors[KeyPreload(adId, layoutId)]?.registerAdCallback(adCallback)
    }

    fun registerAdCallBack(listId: List<String>, @LayoutRes layoutId: Int, adCallback: AzAdCallback) {
        executors[KeyPreloadList(listId, layoutId)]?.registerAdCallback(adCallback)
    }

    fun unRegisterAdCallback(adId: String, @LayoutRes layoutId: Int, adCallback: AzAdCallback) {
        executors[KeyPreload(adId, layoutId)]?.unregisterAdCallback(adCallback)
    }

    fun unRegisterAdCallback(listId: List<String>, @LayoutRes layoutId: Int, adCallback: AzAdCallback) {
        executors[KeyPreloadList(listId, layoutId)]?.unregisterAdCallback(adCallback)
    }

    internal fun getNativeAdCallback(adId: String, @LayoutRes layoutId: Int): NativeAdCallback? =
        executors[KeyPreload(adId, layoutId)]?.getNativeAdCallback()

    internal fun getNativeAdCallback(listId: List<String>, @LayoutRes layoutId: Int): NativeAdCallback? =
        executors[KeyPreloadList(listId, layoutId)]?.getNativeAdCallback()

    private fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.activeNetworkInfo?.isConnected == true
        } catch (t: Throwable) {
            false
        }
    }

    data class KeyPreload(val adId: String, @LayoutRes override val layoutId: Int) : IKeyPreload

    data class KeyPreloadList(val listId: List<String>, @LayoutRes override val layoutId: Int) : IKeyPreload

    interface IKeyPreload {
        val layoutId: Int
    }

    companion object {
        @Volatile private var _instance: NativeAdPreload? = null

        @JvmStatic
        fun getInstance(): NativeAdPreload =
            _instance ?: synchronized(this) {
                _instance ?: NativeAdPreload().also { _instance = it }
            }
    }
}
