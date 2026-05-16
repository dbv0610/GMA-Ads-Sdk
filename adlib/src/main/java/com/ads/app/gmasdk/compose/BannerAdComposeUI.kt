package com.ads.app.gmasdk.compose

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadBannerList
import com.ads.app.gmasdk.control.ads.requestLoadBanner
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.helper.banner.BannerAdConfig
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BannerAdDisplayState {
    data object Idle : BannerAdDisplayState()
    data object Loading : BannerAdDisplayState()
    data class Loaded(val adView: AdView, val adUnitId: String = "") : BannerAdDisplayState()
    data class Error(val error: LoadAdError?) : BannerAdDisplayState()
    data object Cancelled : BannerAdDisplayState()
}

@Stable
class BannerAdHolder internal constructor(
    private val _state: MutableStateFlow<BannerAdDisplayState>,
    private var job: Job?,
    private val restartBlock: (() -> Job)?,
    private val onDispose: () -> Unit,
) {
    val state: StateFlow<BannerAdDisplayState> = _state.asStateFlow()
    val currentState: BannerAdDisplayState get() = _state.value
    val isLoaded: Boolean get() = currentState is BannerAdDisplayState.Loaded
    val adView: AdView? get() = (currentState as? BannerAdDisplayState.Loaded)?.adView

    fun cancel() {
        job?.cancel()
        destroyCurrentAd()
        _state.value = BannerAdDisplayState.Cancelled
    }

    fun reload() {
        job?.cancel()
        destroyCurrentAd()
        _state.value = BannerAdDisplayState.Idle
        job = restartBlock?.invoke()
    }

    internal fun dispose() {
        job?.cancel()
        destroyCurrentAd()
        onDispose()
    }

    private fun destroyCurrentAd() {
        (_state.value as? BannerAdDisplayState.Loaded)?.adView?.destroy()
    }

    companion object {
        @VisibleForTesting
        fun testInstance(
            state: MutableStateFlow<BannerAdDisplayState> = MutableStateFlow(BannerAdDisplayState.Idle)
        ): BannerAdHolder = BannerAdHolder(
            _state = state,
            job = null,
            restartBlock = null,
            onDispose = {}
        )
    }
}

@Composable
fun rememberBannerAd(
    config: BannerAdConfig,
    autoReloadOnResume: Boolean = true,
    callback: AdCallback? = null,
): BannerAdHolder {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val holder = remember(config.idAds, config.listId.hashCode()) {
        createBannerHolder(activity = activity, config = config, scope = scope, userCallback = callback)
    }

    var resumeCount by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumeCount++
                    if (resumeCount == 1) {
                        if (holder.currentState is BannerAdDisplayState.Idle) holder.reload()
                    } else if (autoReloadOnResume) {
                        if (holder.currentState !is BannerAdDisplayState.Loading) holder.reload()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> holder.dispose()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.dispose()
        }
    }

    return holder
}

@Composable
fun BannerAdCard(
    holder: BannerAdHolder,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { DefaultBannerAdLoading() },
    error: @Composable (LoadAdError?) -> Unit = {},
    onStateChange: ((BannerAdDisplayState) -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier) { loading() }
        return
    }

    val state by holder.state.collectAsStateWithLifecycle(BannerAdDisplayState.Idle)
    var loadStartTime by remember { mutableStateOf(0L) }
    var loadTimeMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state) {
        onStateChange?.invoke(state)
        when (state) {
            is BannerAdDisplayState.Loading -> loadStartTime = System.currentTimeMillis()
            is BannerAdDisplayState.Loaded -> {
                if (loadStartTime > 0) {
                    loadTimeMs = System.currentTimeMillis() - loadStartTime
                    loadStartTime = 0L
                }
            }
            else -> Unit
        }
    }

    val contentModifier = remember(state) {
        if (state is BannerAdDisplayState.Cancelled || state is BannerAdDisplayState.Error) {
            Modifier.height(0.dp)
        } else {
            Modifier.wrapContentHeight()
        }
    }

    Box(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        Box(contentModifier.fillMaxWidth()) {
            when (val currentState = state) {
                is BannerAdDisplayState.Loading,
                is BannerAdDisplayState.Idle -> loading()
                is BannerAdDisplayState.Error -> error(currentState.error)
                is BannerAdDisplayState.Loaded -> {
                    BannerAdViewComposable(adView = currentState.adView, modifier = Modifier.fillMaxWidth())
                }
                is BannerAdDisplayState.Cancelled -> Unit
            }
        }

        if (AzAds.getInstance().isShowMessageTester) {
            val debugInfo = remember(state, loadTimeMs) {
                AdDebugInfo(
                    adType = "Banner",
                    state = state.debugLabel(),
                    adUnitId = (state as? BannerAdDisplayState.Loaded)?.adUnitId,
                    loadTimeMs = loadTimeMs
                )
            }
            AdDebugOverlayCompose(info = debugInfo, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

private fun BannerAdDisplayState.debugLabel(): String = when (this) {
    is BannerAdDisplayState.Idle -> "Idle"
    is BannerAdDisplayState.Loading -> "Loading"
    is BannerAdDisplayState.Loaded -> "Loaded"
    is BannerAdDisplayState.Error -> "Error"
    is BannerAdDisplayState.Cancelled -> "Cancelled"
}

@Composable
fun BannerAd(
    config: BannerAdConfig,
    modifier: Modifier = Modifier,
    autoReloadOnResume: Boolean = true,
    loading: @Composable () -> Unit = { DefaultBannerAdLoading() },
    error: @Composable (LoadAdError?) -> Unit = {},
    onStateChange: ((BannerAdDisplayState) -> Unit)? = null,
    callback: AdCallback? = null,
) {
    val holder = rememberBannerAd(config = config, autoReloadOnResume = autoReloadOnResume, callback = callback)
    BannerAdCard(holder = holder, modifier = modifier, loading = loading, error = error, onStateChange = onStateChange)
}

@Composable
private fun BannerAdViewComposable(adView: AdView, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE1E1E1)))
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = {
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView
            },
            update = { view ->
                val parent = view.parent
                if (parent is ViewGroup && parent !is androidx.compose.ui.platform.ComposeView) {
                    parent.removeView(view)
                }
            }
        )
    }
    DisposableEffect(adView) {
        onDispose { (adView.parent as? ViewGroup)?.removeView(adView) }
    }
}

private val ShimmerPlaceholderColor = Color(0xFFE0E0E0)

@Composable
fun DefaultBannerAdLoading() {
    val shimmer = rememberShimmerState()
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE1E1E1))
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ShimmerPlaceholderColor)
                    .shimmer(state = shimmer, visible = true, shape = RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ShimmerPlaceholderColor)
                        .shimmer(state = shimmer, visible = true, shape = RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ShimmerPlaceholderColor)
                        .shimmer(state = shimmer, visible = true, shape = RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ShimmerPlaceholderColor)
                    .shimmer(state = shimmer, visible = true, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun createBannerHolder(
    activity: Activity,
    config: BannerAdConfig,
    scope: CoroutineScope,
    userCallback: AdCallback? = null,
): BannerAdHolder {
    val state = MutableStateFlow<BannerAdDisplayState>(BannerAdDisplayState.Idle)

    val launchOnce: () -> Job = {
        scope.launch(Dispatchers.Main.immediate) {
            state.value = BannerAdDisplayState.Loading

            val useList = config.listId.isNotEmpty()
            val callback = object : AdCallback() {
                override fun onBannerLoaded(adView: AdView) {
                    val loadedUnitId = if (useList) config.listId.firstOrNull() ?: config.idAds else config.idAds
                    state.value = BannerAdDisplayState.Loaded(adView, loadedUnitId)
                    userCallback?.onBannerLoaded(adView)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    state.value = BannerAdDisplayState.Error(loadAdError)
                    userCallback?.onAdFailedToLoad(loadAdError)
                }
                override fun onAdLoaded() = userCallback?.onAdLoaded() ?: Unit
                override fun onAdClicked() = userCallback?.onAdClicked() ?: Unit
                override fun onAdImpression() = userCallback?.onAdImpression() ?: Unit
                override fun onAdClosed() = userCallback?.onAdClosed() ?: Unit
            }

            if (useList) {
                AzAds.getInstance().loadBannerList(
                    activity = activity,
                    listId = config.listId,
                    collapsibleGravity = config.collapsibleGravity,
                    adCallback = callback
                )
            } else {
                AzAds.getInstance().requestLoadBanner(
                    activity = activity,
                    idBannerAd = config.idAds,
                    collapsibleGravity = config.collapsibleGravity,
                    useInlineAdaptive = config.usingInlineBanner,
                    maxHeight = config.maxHeight,
                    adCallback = callback
                )
            }
        }
    }

    return BannerAdHolder(
        _state = state,
        job = null,
        restartBlock = launchOnce,
        onDispose = { (state.value as? BannerAdDisplayState.Loaded)?.adView?.destroy() },
    )
}
