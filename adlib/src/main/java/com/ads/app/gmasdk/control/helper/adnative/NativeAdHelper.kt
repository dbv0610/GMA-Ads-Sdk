package com.ads.app.gmasdk.control.helper.adnative

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.helper.AdOptionVisibility
import com.ads.app.gmasdk.control.helper.AdsHelper
import com.ads.app.gmasdk.control.helper.adnative.callback.NativeAdCallback
import com.ads.app.gmasdk.control.helper.adnative.params.AdNativeState
import com.ads.app.gmasdk.control.helper.adnative.params.NativeAdParam
import com.ads.app.gmasdk.control.helper.adnative.preload.NativeAdPreload
import com.ads.app.gmasdk.control.helper.adnative.preload.NativeAdPreloadClientOption
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.loadNativeAdResultCallback
import com.ads.app.gmasdk.control.ads.populateNativeAdView
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class NativeAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    override val config: NativeAdConfig
) : AdsHelper<NativeAdConfig, NativeAdParam>(activity, lifecycleOwner, config) {

    companion object {
        const val TAG = "NativeAdHelper"
    }

    private val nativeAdCallback = NativeAdCallback()
    private val preload = NativeAdPreload.getInstance()
    private val adNativeState: MutableStateFlow<AdNativeState> =
        MutableStateFlow(if (canRequestAds()) AdNativeState.None else AdNativeState.Fail)
    private val resumeCount = AtomicInteger(0)
    private var shimmerLayoutView: ShimmerFrameLayout? = null
    private var nativeContentView: FrameLayout? = null
    private val lifecycleNativeCallback: AzAdCallback =
        nativeAdCallback.invokeListenerAdCallback(null)

    var adVisibility: AdOptionVisibility = AdOptionVisibility.GONE

    var nativeAd: ApNativeAd? = null
        private set

    var isEnablePreload: Boolean = false
        private set

    var preloadClientOption: NativeAdPreloadClientOption =
        NativeAdPreloadClientOption(preloadAfterShow = false, preloadBuffer = 1, preloadOnResume = false)
        private set

    var isEnableListNative: Boolean = false
        private set

    private var onCustomShowView: ((ApNativeAd) -> Unit)? = null

    init {
        // Flow 1: immediate lifecycle events — register/unregister preload callbacks and
        // hide views when ads cannot be shown.
        lifecycleEventState.onEach { event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val callback = if (isEnableListNative) {
                        preload.getNativeAdCallback(config.listId, config.layoutId)
                    } else {
                        preload.getNativeAdCallback(config.idAds, config.layoutId)
                    }
                    callback?.registerAdListener(lifecycleNativeCallback)
                }
                Lifecycle.Event.ON_STOP -> {
                    val callback = if (isEnableListNative) {
                        preload.getNativeAdCallback(config.listId, config.layoutId)
                    } else {
                        preload.getNativeAdCallback(config.idAds, config.layoutId)
                    }
                    callback?.unregisterAdListener(lifecycleNativeCallback)
                }
                Lifecycle.Event.ON_CREATE -> {
                    if (!canRequestAds()) {
                        nativeContentView?.let { checkAdVisibility(it, false) }
                        shimmerLayoutView?.let { checkAdVisibility(it, false) }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!canShowAds() && isActiveState()) {
                        cancel()
                    }
                }
                else -> Unit
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 2: debounced lifecycle events — reload on resume when appropriate.
        lifecycleEventState.debounce(300L).onEach { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount.incrementAndGet()
                logZ("resumeCount: ${resumeCount.get()}")
            }
            if (event == Lifecycle.Event.ON_RESUME
                && resumeCount.get() > 1
                && nativeAd != null
                && canRequestAds()
                && canReloadAd()
                && isActiveState()
            ) {
                requestAds(NativeAdParam.Request.ResumeRequest)
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 3: log state changes.
        adNativeState.onEach { it ->
            logZ("adNativeState: ${it::class.java.simpleName}")
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 4: drive UI based on state.
        adNativeState.onEach { adsParam ->
            handleShowAds(adsParam)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }

    // ---------------------------------------------------------------------------
    // Builder-style setters
    // ---------------------------------------------------------------------------

    fun setShimmerLayoutView(shimmerLayoutView: ShimmerFrameLayout): NativeAdHelper = apply {
        runCatching {
            this.shimmerLayoutView = shimmerLayoutView
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState >= Lifecycle.State.CREATED && currentState <= Lifecycle.State.RESUMED && !canRequestAds()) {
                checkAdVisibility(shimmerLayoutView, false)
            }
        }
    }

    fun setNativeContentView(nativeContentView: FrameLayout): NativeAdHelper = apply {
        runCatching {
            this.nativeContentView = nativeContentView
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState >= Lifecycle.State.CREATED && currentState <= Lifecycle.State.RESUMED && !canRequestAds()) {
                checkAdVisibility(nativeContentView, false)
            }
        }
    }

    fun setEnablePreload(isEnable: Boolean): NativeAdHelper = apply {
        isEnablePreload = isEnable
    }

    fun setEnableListNative(isEnable: Boolean): NativeAdHelper = apply {
        isEnableListNative = isEnable
    }

    fun setPreloadAdOption(option: NativeAdPreloadClientOption): NativeAdHelper = apply {
        preloadClientOption = option
    }

    fun setCustomContentView(onCustom: (ApNativeAd) -> Unit) {
        onCustomShowView = onCustom
    }

    // ---------------------------------------------------------------------------
    // Public accessors
    // ---------------------------------------------------------------------------

    fun getAdNativeState(): StateFlow<AdNativeState> = adNativeState.asStateFlow()

    fun getNativeAdConfig(): NativeAdConfig = config

    // ---------------------------------------------------------------------------
    // AdsHelper overrides
    // ---------------------------------------------------------------------------

    override fun requestAds(param: NativeAdParam) {
        AzAds.getInstance().runWhenReady {
            lifecycleOwner.lifecycleScope.launch {
                logZ("requestAds with param: ${param::class.java.simpleName}")
                if (!isActiveState()) {
                    logInterruptExecute("requestAds")
                    return@launch
                }
                when (param) {
                    is NativeAdParam.Request -> createOrGetAdPreload(activity)
                    else -> Unit
                }
            }
        }
    }

    override fun cancel() {
        logZ("cancel() called")
        flagActive.compareAndSet(true, false)
        lifecycleOwner.lifecycleScope.launch {
            adNativeState.value = AdNativeState.Cancel
        }
    }

    // ---------------------------------------------------------------------------
    // Ad lifecycle & callback wiring
    // ---------------------------------------------------------------------------

    fun registerAdListener(adCallback: AzAdCallback) {
        nativeAdCallback.registerAdListener(adCallback)
    }

    fun unregisterAdListener(adCallback: AzAdCallback) {
        nativeAdCallback.unregisterAdListener(adCallback)
    }

    fun unregisterAllAdListener() {
        nativeAdCallback.unregisterAllAdListener()
    }

    @Deprecated("Using cancel()")
    fun resetState() {
        logZ("resetState()")
        cancel()
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun handleShowAds(adsParam: AdNativeState) {
        nativeContentView?.let { view ->
            checkAdVisibility(view, adsParam !is AdNativeState.Cancel && canShowAds())
        }
        shimmerLayoutView?.let { shimmer ->
            checkAdVisibility(shimmer, adsParam is AdNativeState.Loading && nativeAd == null)
        }
        if (adsParam is AdNativeState.Loaded) {
            val content = nativeContentView
            val shimmer = shimmerLayoutView
            if (content != null && shimmer != null) {
                val custom = onCustomShowView
                if (custom != null) {
                    custom(adsParam.adNative)
                } else {
                    AzAds.getInstance().populateNativeAdView(activity, adsParam.adNative, content, shimmer)
                }
                if (AzAds.getInstance().isShowMessageTester) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Show native : ${config.idAds}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ensureEnablePreload { option ->
                if (option.preloadAfterShow) {
                    if (isEnableListNative) {
                        if (preload.getNativeAdBuffer(config.listId, config.layoutId).isEmpty()) {
                            preload.preload(activity, config.listId, config.layoutId, option.preloadBuffer)
                        }
                    } else {
                        if (preload.getNativeAdBuffer(config.idAds, config.layoutId).isEmpty()) {
                            preload.preload(activity, config.idAds, config.layoutId, option.preloadBuffer)
                        }
                    }
                }
            }
        }
    }

    private fun ensureEnablePreload(block: (NativeAdPreloadClientOption) -> Unit) {
        if (isEnablePreload) {
            block(preloadClientOption)
        }
    }

    private fun createNativeAds(activity: Activity) {
        if (canRequestAds()) {
            adNativeState.value = AdNativeState.Loading
            lifecycleOwner.lifecycleScope.launch {
                logZ("createNativeAds")
                if (!isActiveState()) {
                    logInterruptExecute("createNativeAds")
                    return@launch
                }
                AzAds.getInstance().loadNativeAdResultCallback(
                    context = activity,
                    id = config.idAds,
                    layoutCustomNative = config.layoutId,
                    callback = nativeAdCallback.invokeListenerAdCallback(object : AzAdCallback() {
                        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                            setAndUpdateNativeLoaded(nativeAd)
                        }
                        override fun onAdFailedToLoad(adError: ApAdError?) {
                            adNativeState.value = AdNativeState.Fail
                        }
                    })
                )
            }
        }
    }

    private fun createOrGetAdPreload(activity: Activity) {
        if (!canRequestAds()) return
        if (isEnablePreload) {
            if (isEnableListNative) {
                if (preload.isPreloadAvailable(config.listId, config.layoutId)) {
                    lifecycleOwner.lifecycleScope.launch {
                        val preloadedAd = preload.getAdNative(config.listId, config.layoutId)
                        if (preloadedAd != null) {
                            setAndUpdateNativeLoaded(preloadedAd)
                        } else {
                            createNativeAds(activity)
                        }
                    }
                } else {
                    createNativeAds(activity)
                }
            } else {
                if (preload.isPreloadAvailable(config.idAds, config.layoutId)) {
                    lifecycleOwner.lifecycleScope.launch {
                        val preloadedAd = preload.getAdNative(config.idAds, config.layoutId)
                        if (preloadedAd != null) {
                            setAndUpdateNativeLoaded(preloadedAd)
                        } else {
                            createNativeAds(activity)
                        }
                    }
                } else {
                    createNativeAds(activity)
                }
            }
        } else {
            createNativeAds(activity)
        }
    }

    private fun setAndUpdateNativeLoaded(nativeAd: ApNativeAd) {
        nativeAd.layoutCustomNative = config.getLayoutIdByMediationNativeAd(nativeAd.admobNativeAd)
        this.nativeAd = nativeAd
        lifecycleOwner.lifecycleScope.launch {
            adNativeState.value = AdNativeState.Loaded(nativeAd)
        }
    }

    private fun getDefaultCallback(): AzAdCallback {
        return object : AzAdCallback() {
            override fun onAdLoaded() {
                // handled via nativeAdCallback
            }
            override fun onAdFailedToLoad(adError: ApAdError?) {
                adNativeState.value = AdNativeState.Fail
            }
        }
    }

    private fun checkAdVisibility(view: View, isVisible: Boolean) {
        view.visibility = if (isVisible) {
            View.VISIBLE
        } else {
            when (adVisibility) {
                AdOptionVisibility.GONE -> View.GONE
                AdOptionVisibility.INVISIBLE -> View.INVISIBLE
            }
        }
    }
}
