package com.ads.app.gmasdk.control.helper.banner

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadBannerList
import com.ads.app.gmasdk.control.ads.requestLoadBanner
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.helper.AdsHelper
import com.ads.app.gmasdk.control.helper.banner.params.AdBannerState
import com.ads.app.gmasdk.control.helper.banner.params.BannerAdParam
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

open class BannerAdHelper(
    private val activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    override val config: BannerAdConfig
) : AdsHelper<BannerAdConfig, BannerAdParam>(activity, lifecycleOwner, config) {

    protected val adBannerState: MutableStateFlow<AdBannerState> =
        MutableStateFlow(if (canRequestAds()) AdBannerState.None else AdBannerState.Fail)

    protected var timeShowAdImpression: Long = 0L

    private val listAdCallback = CopyOnWriteArrayList<AdCallback>()
    private val resumeCount = AtomicInteger(0)
    private var shimmerLayoutView: ShimmerFrameLayout? = null
    private var bannerContentView: FrameLayout? = null

    var bannerAdView: AdView? = null

    var isEnableListBanner: Boolean = false
        private set

    init {
        registerAdListener(getDefaultCallback())

        // Flow 1: immediate lifecycle events — hide views on create when ads can't show,
        // cancel when ads should no longer show on resume, detach ad on stop, reattach on start.
        lifecycleEventState.onEach { event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    if (!canRequestAds()) {
                        bannerContentView?.visibility = View.GONE
                        shimmerLayoutView?.visibility = View.GONE
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!canShowAds() && isActiveState()) {
                        cancel()
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    runCatching {
                        val adView = bannerAdView
                        if (adView != null) {
                            val parent = adView.parent as? ViewGroup
                            parent?.removeView(adView)
                        }
                    }
                }
                Lifecycle.Event.ON_START -> {
                    val content = bannerContentView
                    val adView = bannerAdView
                    if (canShowAds() && content != null && adView != null) {
                        showAd(content, adView)
                    }
                }
                else -> Unit
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 2: debounced lifecycle events — reload banner on resume when appropriate.
        lifecycleEventState.debounce(300L).onEach { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount.incrementAndGet()
                logZ("resumeCount: ${resumeCount.get()}")
                if (!isActiveState()) {
                    logInterruptExecute("Request when resume")
                }
            }
            if (event == Lifecycle.Event.ON_RESUME
                && resumeCount.get() > 1
                && bannerAdView != null
                && canRequestAds()
                && canReloadAd()
                && isActiveState()
            ) {
                logZ("requestAds on resume")
                requestAds(BannerAdParam.Request)
            }
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 3: log state changes.
        adBannerState.onEach { it ->
            logZ("adBannerState: ${it::class.java.simpleName}")
        }.launchIn(lifecycleOwner.lifecycleScope)

        // Flow 4: drive UI based on state.
        adBannerState.onEach { adsParam ->
            handleShowAds(adsParam)
        }.launchIn(lifecycleOwner.lifecycleScope)
    }

    // ---------------------------------------------------------------------------
    // Public accessors
    // ---------------------------------------------------------------------------

    fun getBannerState(): StateFlow<AdBannerState> = adBannerState.asStateFlow()

    fun getBannerAdConfig(): BannerAdConfig = config

    // ---------------------------------------------------------------------------
    // Builder-style setters
    // ---------------------------------------------------------------------------

    fun setShimmerLayoutView(shimmerLayoutView: ShimmerFrameLayout): BannerAdHelper = apply {
        runCatching {
            this.shimmerLayoutView = shimmerLayoutView
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState >= Lifecycle.State.CREATED && currentState <= Lifecycle.State.RESUMED && !canRequestAds()) {
                shimmerLayoutView.visibility = View.GONE
            }
        }
    }

    fun setEnableListBanner(isEnable: Boolean): BannerAdHelper = apply {
        isEnableListBanner = isEnable
    }

    fun setBannerContentView(nativeContentView: FrameLayout): BannerAdHelper = apply {
        runCatching {
            bannerContentView = nativeContentView
            shimmerLayoutView = nativeContentView.findViewById(com.ads.app.gmasdk.R.id.shimmer_container_banner)
            val currentState = lifecycleOwner.lifecycle.currentState
            if (currentState >= Lifecycle.State.CREATED && currentState <= Lifecycle.State.RESUMED) {
                if (!canRequestAds()) {
                    nativeContentView.visibility = View.GONE
                    shimmerLayoutView?.visibility = View.GONE
                }
                val bannerAd = bannerAdView
                if (canShowAds() && bannerAd != null) {
                    showAd(nativeContentView, bannerAd)
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // AdsHelper overrides
    // ---------------------------------------------------------------------------

    override fun requestAds(param: BannerAdParam) {
        AzAds.getInstance().runWhenReady {
            logZ("requestAds with param: ${param::class.java.simpleName}")
            if (canRequestAds()) {
                lifecycleOwner.lifecycleScope.launch {
                    when (param) {
                        is BannerAdParam.Request -> loadBannerAd()
                        is BannerAdParam.Ready -> {
                            bannerAdView = param.bannerAds
                            adBannerState.value = AdBannerState.Loaded(param.bannerAds)
                        }
                        else -> Unit
                    }
                }
            } else if (!isOnline() && bannerAdView == null) {
                cancel()
            }
        }
    }

    override fun cancel() {
        logZ("cancel() called")
        flagActive.compareAndSet(true, false)
        bannerAdView = null
        lifecycleOwner.lifecycleScope.launch {
            adBannerState.value = AdBannerState.Cancel
        }
    }

    // ---------------------------------------------------------------------------
    // Ad loading
    // ---------------------------------------------------------------------------

    protected open fun loadBannerAd() {
        if (!canRequestAds()) return
        adBannerState.value = AdBannerState.Loading
        if (isEnableListBanner) {
            logZ("loadBannerAd List")
            AzAds.getInstance().loadBannerList(
                activity = activity,
                listId = config.listId,
                collapsibleGravity = config.collapsibleGravity,
                useInlineAdaptive = config.usingInlineBanner,
                maxHeight = config.maxHeight,
                adCallback = invokeListenerAdCallback()
            )
        } else {
            logZ("loadBannerAd")
            AzAds.getInstance().requestLoadBanner(
                activity = activity,
                idBannerAd = config.idAds,
                collapsibleGravity = config.collapsibleGravity,
                useInlineAdaptive = config.usingInlineBanner,
                maxHeight = config.maxHeight,
                adCallback = invokeListenerAdCallback()
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Callback management
    // ---------------------------------------------------------------------------

    fun registerAdListener(adCallback: AdCallback) {
        listAdCallback.add(adCallback)
    }

    fun unregisterAdListener(adCallback: AdCallback) {
        listAdCallback.remove(adCallback)
    }

    fun unregisterAllAdListener() {
        listAdCallback.clear()
    }

    protected open fun getDefaultCallback(): AdCallback {
        return object : AdCallback() {
            override fun onAdLoaded() {
                // default: no-op, subclasses can override
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                adBannerState.value = AdBannerState.Fail
            }
        }
    }

    protected fun invokeListenerAdCallback(): AdCallback {
        return object : AdCallback() {
            override fun onAdLoaded() {
                invokeAdListener { it.onAdLoaded() }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                invokeAdListener { it.onAdFailedToLoad(loadAdError) }
            }

            override fun onAdClicked() {
                invokeAdListener { it.onAdClicked() }
            }

            override fun onAdImpression() {
                invokeAdListener { it.onAdImpression() }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    private fun handleShowAds(adsParam: AdBannerState) {
        bannerContentView?.let { view ->
            val shouldBeGone = adsParam is AdBannerState.Cancel || !canShowAds()
            view.visibility = if (shouldBeGone) View.GONE else View.VISIBLE
        }
        shimmerLayoutView?.let { shimmer ->
            val showShimmer = adsParam is AdBannerState.Loading && bannerAdView == null
            shimmer.visibility = if (showShimmer) View.VISIBLE else View.GONE
        }
        if (adsParam is AdBannerState.Loaded) {
            val content = bannerContentView
            if (content != null) {
                showAd(content, adsParam.adBanner)
            }
        }
    }

    private fun showAd(bannerContentView: FrameLayout, adView: AdView) {
        val container = bannerContentView as ViewGroup
        if (container.indexOfChild(adView) != -1) {
            logZ("bannerContentView has contains adView")
            return
        }

        bannerContentView.setBackgroundColor(0)

        val heightDivider = bannerContentView.context.resources
            .getDimensionPixelOffset(com.ads.app.gmasdk.R.dimen._1sdp)

        bannerContentView.let { it ->
            removeBannerCollapseIfNeed(it as ViewGroup)
            val oldHeight = adView.height
            val placeholder = View(it.context)
            val divider = View(it.context)
            divider.setBackgroundColor(-1973791)

            it.removeAllViews()
            it.addView(placeholder, 0, oldHeight)

            // detach adView from its current parent if needed
            val adViewParent = adView.parent as? ViewGroup
            adViewParent?.removeView(adView)

            it.addView(adView, -1, ViewGroup.LayoutParams.WRAP_CONTENT)
            adView.updateLayoutParams<FrameLayout.LayoutParams> {
                setMargins(0, heightDivider, 0, 0)
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            }

            it.addView(divider, -1, heightDivider)
        }
    }

    private fun removeBannerCollapseIfNeed(layout: ViewGroup) {
        val gravity = config.collapsibleGravity
        if (!gravity.isNullOrEmpty()) {
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is AdView) {
                    child.destroy()
                    child.visibility = View.GONE
                    layout.removeView(child)
                    return
                }
            }
        }
    }

    private fun invokeAdListener(action: (AdCallback) -> Unit) {
        listAdCallback.forEach { action(it) }
    }
}

// ---------------------------------------------------------------------------
// Inline extension helper (mirrors androidx.core.view)
// ---------------------------------------------------------------------------
private inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    val params = layoutParams as T
    block(params)
    layoutParams = params
}
