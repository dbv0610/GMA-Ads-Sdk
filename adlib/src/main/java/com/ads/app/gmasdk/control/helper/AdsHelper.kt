package com.ads.app.gmasdk.control.helper

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.helper.params.IAdsParam
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean

abstract class AdsHelper<C : IAdsConfig, P : IAdsParam>(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    protected open val config: C
) {
    private var tag: String = context.javaClass.simpleName
    internal val flagActive = AtomicBoolean(false)
    internal val lifecycleEventState: MutableStateFlow<Lifecycle.Event> = MutableStateFlow(Lifecycle.Event.ON_ANY)
    var flagUserEnableReload: Boolean = true
        set(value) {
            field = value
            logZ("setFlagUserEnableReload($flagUserEnableReload")
        }

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                lifecycleEventState.value = event
                if (event == Lifecycle.Event.ON_DESTROY) {
                    lifecycleOwner.lifecycle.removeObserver(this)
                }
            }
        })
    }

    open fun canShowAds(): Boolean = !AppPurchase.getInstance().isPurchased() && config.canShowAds

    open fun canRequestAds(): Boolean = canShowAds() && isOnline()

    abstract fun requestAds(param: P)

    abstract fun cancel()

    fun setTagForDebug(tag: String) { this.tag = tag }

    fun isActiveState(): Boolean = flagActive.get()

    fun canReloadAd(): Boolean = config.canReloadAds && flagUserEnableReload

    internal fun logZ(message: String) {
        Log.d(javaClass.simpleName, "$tag: $message")
    }

    internal fun logInterruptExecute(message: String) {
        logZ("$message not execute because has called cancel()")
    }

    internal fun isOnline(): Boolean {
        val netInfo = runCatching {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        }.getOrNull()
        return netInfo != null && netInfo.isConnected
    }
}
