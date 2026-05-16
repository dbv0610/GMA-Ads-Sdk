package com.ads.app.gmasdk.control.helper.adnative.preload

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.helper.adnative.callback.NativeAdCallback
import com.ads.app.gmasdk.control.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class NativeAdPreloadTemplate {

    protected open val TAG: String = "NativeAdPreload"

    private val nativeAdCallback = NativeAdCallback()
    private val adPreloadState: MutableStateFlow<NativePreloadState> =
        MutableStateFlow(NativePreloadState.None)
    private val adPreloadLiveData: MutableLiveData<NativePreloadState> =
        MutableLiveData(NativePreloadState.None)
    private val queueNativeAd = ArrayDeque<ApNativeAd>()
    protected val inProgress = AtomicBoolean(false)
    protected val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val counter = AtomicInteger(0)
    protected var action: (NativePreloadState) -> Unit = {}
    protected val totalBuffer = AtomicInteger(0)
    protected var priorityOrder: List<String>? = null
    private val queueLock = Any()

    private inline fun <T> withQueueLock(block: () -> T): T = synchronized(queueLock) { block() }

    protected fun debugPrintQueue(label: String) {
        val snapshot = withQueueLock { queueNativeAd.toList() }
        if (snapshot.isEmpty()) {
            AppLogger.d(TAG, messageLog(label))
            return
        }
        val items = snapshot.mapIndexed { idx, ad ->
            val id = AdUnitTagger.idOf(ad) ?: "?"
            val hex = Integer.toHexString(System.identityHashCode(ad))
            "$idx:$id:$hex"
        }.joinToString(" → ")
        AppLogger.d(TAG, messageLog("$label [${snapshot.size}] $items"))
    }

    protected fun debugPrintQueueCounts(label: String) {
        val ids = withQueueLock { queueNativeAd.map { AdUnitTagger.idOf(it) ?: "?" } }
        val total = ids.size
        val counts = ids.groupingBy { it }.eachCount()
            .toList()
            .sortedWith(compareBy { (id, _) ->
                priorityOrder?.indexOf(id)?.takeIf { it >= 0 } ?: Int.MAX_VALUE
            })
            .joinToString(", ") { (id, c) -> "$id:$c" }
        AppLogger.d(TAG, messageLog("$label $counts total=$total"))
    }

    fun pollAdNative(): ApNativeAd? {
        val ad = withQueueLock { queueNativeAd.removeFirstOrNull() }
        logD("pollAdNative: $ad")
        logSizeQueue()
        return ad
    }

    fun getAdNative(): ApNativeAd? {
        val ad = withQueueLock { queueNativeAd.firstOrNull() }
        logD("getAdNative: $ad")
        logSizeQueue()
        return ad
    }

    suspend fun pollOrAwaitAdNative(): ApNativeAd? {
        if (!queueNativeAd.isEmpty()) return pollAdNative()
        if (!isInProgress()) return null
        val state = adPreloadState.firstOrNull { it is NativePreloadState.Consume || it is NativePreloadState.Complete }
        Log.i(TAG, "isInProgress pollOrAwaitAdNative: $state")
        return pollAdNative()
    }

    suspend fun getOrAwaitAdNative(): ApNativeAd? {
        if (queueNativeAd.isEmpty()) {
            if (!isInProgress()) return null
            return adPreloadState
                .map { state ->
                    when (state) {
                        is NativePreloadState.Consume, NativePreloadState.Complete -> getAdNative()
                        NativePreloadState.None, NativePreloadState.Start, NativePreloadState.Error -> null
                    }
                }
                .firstOrNull { it != null }
        }
        return pollAdNative()
    }

    fun isInProgress(): Boolean = inProgress.get()

    fun isPreloadAvailable(): Boolean {
        val hasQueue = withQueueLock { queueNativeAd.isNotEmpty() }
        return isInProgress() || hasQueue
    }

    fun getNativeAdBuffer(): List<ApNativeAd> = withQueueLock { queueNativeAd.toList() }

    protected abstract fun requestAd(activity: Activity, buffer: Int)

    fun registerAdCallback(adCallback: AzAdCallback) {
        nativeAdCallback.registerAdListener(adCallback)
    }

    fun unregisterAdCallback(adCallback: AzAdCallback) {
        nativeAdCallback.unregisterAdListener(adCallback)
    }

    fun getNativeAdCallback(): NativeAdCallback = nativeAdCallback

    protected fun getDefaultNativeCallback(): AzAdCallback {
        counter.incrementAndGet()
        return nativeAdCallback.invokeListenerAdCallback(object : AzAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                withQueueLock { queueNativeAd.addLast(nativeAd) }
                val order = priorityOrder
                if (order != null) resortQueueByPriority(order)
                action(NativePreloadState.Consume(nativeAd))
                checkCompleted()
            }

            override fun onAdFailedToLoad(adError: ApAdError?) {
                action(NativePreloadState.Error)
                checkCompleted()
            }
        })
    }

    private fun checkCompleted() {
        val remaining = counter.decrementAndGet()
        if (remaining <= 0 && totalBuffer.get() > 0) {
            inProgress.set(false)
            action(NativePreloadState.Complete)
        }
    }

    private fun resortQueueByPriority(priorityOrder: List<String>) {
        val snapshot = withQueueLock { queueNativeAd.toList() }
        if (snapshot.isEmpty()) return
        val originalIndex = snapshot.withIndex().associate { (idx, ad) -> ad to idx }
        val priorityIndex = priorityOrder.withIndex().associate { (idx, id) -> id to idx }
        val sorted = snapshot.sortedWith(
            compareBy<ApNativeAd> { ad ->
                val id = AdUnitTagger.idOf(ad)
                if (id != null) priorityIndex[id] ?: Int.MAX_VALUE else Int.MAX_VALUE
            }.thenBy { ad -> originalIndex[ad] ?: Int.MAX_VALUE }
        )
        withQueueLock {
            queueNativeAd.clear()
            queueNativeAd.addAll(sorted)
        }
    }

    fun execute(activity: Activity, buffer: Int) {
        action = { state ->
            Log.d("${TAG}_STATE", "state execute =>> $state")
            adPreloadLiveData.postValue(state)
            adPreloadState.value = state
        }
        requestAd(activity, buffer)
    }

    fun getAdPreloadState(): StateFlow<NativePreloadState> = adPreloadState.asStateFlow()

    fun getAdPreloadLiveData(): LiveData<NativePreloadState> = adPreloadLiveData

    fun release() {
        withQueueLock { queueNativeAd.clear() }
        adPreloadState.value = NativePreloadState.None
        adPreloadLiveData.postValue(NativePreloadState.None)
    }

    protected fun logD(message: String) {
        Log.d("${TAG}_INFO", messageLog(message))
    }

    private fun logSizeQueue() {
        val size = withQueueLock { queueNativeAd.size }
        logD("queue size: $size")
        debugPrintQueue("after-resort")
        debugPrintQueueCounts("after-resort")
    }

    protected open fun messageLog(message: String): String = "[INFO] $message"
}
