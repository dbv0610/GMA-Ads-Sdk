package com.ads.app.gmasdk.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadNativeAdResultCallback
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.helper.AdOptionVisibility
import com.ads.app.gmasdk.control.helper.adnative.preload.NativeAdPreload
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

sealed class NativeAdDisplayState {
    data object Idle : NativeAdDisplayState()
    data object Loading : NativeAdDisplayState()
    data class Success(
        val ad: ApNativeAd,
        val adUnitId: String,
        val fromPreload: Boolean,
        val eventRelay: NativeAdEventRelay? = null
    ) : NativeAdDisplayState()
    data class Error(val error: LoadAdError?) : NativeAdDisplayState()
    data class Cancelled(val adOptionVisibility: AdOptionVisibility = AdOptionVisibility.GONE) :
        NativeAdDisplayState()
}

@Stable
class NativeAdPreloadHolder internal constructor(
    val tag: String,
    private val _state: MutableStateFlow<NativeAdDisplayState>,
    private var job: Job?,
    private val restartBlock: (() -> Job)?,
    private val adOptionVisibility: AdOptionVisibility = AdOptionVisibility.GONE,
    private val onDispose: () -> Unit
) {
    val state: StateFlow<NativeAdDisplayState> = _state.asStateFlow()
    val currentState: NativeAdDisplayState get() = _state.value
    val isLoaded: Boolean get() = currentState is NativeAdDisplayState.Success
    val isLoading: Boolean get() = currentState is NativeAdDisplayState.Loading
    val isCancelled: Boolean get() = currentState is NativeAdDisplayState.Cancelled
    val adVisibility: AdOptionVisibility get() = adOptionVisibility
    val ad: ApNativeAd? get() = (currentState as? NativeAdDisplayState.Success)?.ad
    val eventRelay: NativeAdEventRelay? get() = (currentState as? NativeAdDisplayState.Success)?.eventRelay

    fun request() {
        when (currentState) {
            is NativeAdDisplayState.Loading, is NativeAdDisplayState.Success -> return
            else -> {
                job?.cancel()
                _state.value = NativeAdDisplayState.Idle
                job = restartBlock?.invoke()
            }
        }
    }

    fun cancelLoad() {
        if (currentState is NativeAdDisplayState.Loading) {
            job?.cancel()
            _state.value = NativeAdDisplayState.Cancelled(adOptionVisibility)
        }
    }

    fun cancel() {
        job?.cancel()
        _state.value = NativeAdDisplayState.Cancelled(adOptionVisibility)
    }

    fun reload() {
        job?.cancel()
        (currentState as? NativeAdDisplayState.Success)?.let {
            it.eventRelay?.clearListeners()
            runCatching { it.ad.admobNativeAd?.destroy() }
        }
        _state.value = NativeAdDisplayState.Idle
        job = restartBlock?.invoke()
    }

    internal fun dispose() {
        job?.cancel()
        (currentState as? NativeAdDisplayState.Success)?.let {
            it.eventRelay?.clearListeners()
            runCatching { it.ad.admobNativeAd?.destroy() }
        }
        onDispose()
    }

    companion object {
        @VisibleForTesting
        fun testInstance(
            tag: String = "test",
            state: MutableStateFlow<NativeAdDisplayState> = MutableStateFlow(NativeAdDisplayState.Idle),
            adOptionVisibility: AdOptionVisibility = AdOptionVisibility.GONE
        ): NativeAdPreloadHolder = NativeAdPreloadHolder(
            tag = tag,
            _state = state,
            job = null,
            restartBlock = null,
            adOptionVisibility = adOptionVisibility,
            onDispose = {}
        )
    }
}

@Composable
fun rememberNativeAdPreload(
    tag: String,
    fallbackAdUnitIds: List<String> = emptyList(),
    timeoutPerIdMs: Long = 10_000L,
    adOptionVisibility: AdOptionVisibility = AdOptionVisibility.GONE,
    autoRequestOnStart: Boolean = true,
    autoReloadOnResume: Boolean = true,
    cancelOnPause: Boolean = false,
    destroyOnDispose: Boolean = true,
    adCallback: AzAdCallback? = null
): NativeAdPreloadHolder {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val holder = remember(tag, fallbackAdUnitIds.hashCode()) {
        createPreloadHolder(
            tag = tag,
            context = context.applicationContext,
            fallbackAdUnitIds = fallbackAdUnitIds,
            timeoutPerIdMs = timeoutPerIdMs,
            adOptionVisibility = adOptionVisibility,
            scope = scope,
            destroyOnDispose = destroyOnDispose
        )
    }

    val currentCallback by rememberUpdatedState(adCallback)

    LaunchedEffect(holder) {
        var lastRelay: NativeAdEventRelay? = null
        val relayForwarder = object : AzAdCallback() {
            override fun onAdImpression() { currentCallback?.onAdImpression() }
            override fun onAdClicked() { currentCallback?.onAdClicked() }
            override fun onAdClosed() { currentCallback?.onAdClosed() }
        }
        holder.state.collect { state ->
            lastRelay?.removeListener(relayForwarder)
            lastRelay = null
            when (state) {
                is NativeAdDisplayState.Success -> {
                    currentCallback?.onAdLoaded()
                    currentCallback?.onNativeAdLoaded(state.ad)
                    state.eventRelay?.addListener(relayForwarder)
                    lastRelay = state.eventRelay
                }
                is NativeAdDisplayState.Error -> currentCallback?.onAdFailedToLoad(null)
                else -> Unit
            }
        }
    }

    var resumeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumeCount++
                    if (resumeCount == 1) {
                        if (autoRequestOnStart) holder.request()
                    } else if (autoReloadOnResume) {
                        if (!holder.isLoading) holder.reload()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    if (cancelOnPause) holder.cancelLoad()
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
fun NativeAdCard(
    holder: NativeAdPreloadHolder,
    modifier: Modifier = Modifier,
    loading: @Composable (Boolean) -> Unit = {},
    error: @Composable (LoadAdError?) -> Unit = {},
    nativeView: @Composable (ApNativeAd) -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        val nativeAd by remember { mutableStateOf(ApNativeAd()) }
        CompositionLocalProvider(
            LocalApNativeAd provides nativeAd,
            LocalNativeAdView provides null,
        ) {
            Box(modifier = modifier) { loading(true) }
        }
        return
    }

    val state by holder.state.collectAsStateWithLifecycle(NativeAdDisplayState.Idle)
    var loadStartTime by remember { mutableLongStateOf(0L) }
    var loadTimeMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state) {
        when (state) {
            is NativeAdDisplayState.Loading -> loadStartTime = System.currentTimeMillis()
            is NativeAdDisplayState.Success -> {
                if (loadStartTime > 0) {
                    loadTimeMs = System.currentTimeMillis() - loadStartTime
                    loadStartTime = 0L
                }
            }
            else -> Unit
        }
    }

    val modifierAds = remember(state) {
        when (state) {
            is NativeAdDisplayState.Cancelled -> {
                if ((state as? NativeAdDisplayState.Cancelled)?.adOptionVisibility == AdOptionVisibility.INVISIBLE) {
                    Modifier.wrapContentHeight()
                } else {
                    Modifier.height(0.dp)
                }
            }
            is NativeAdDisplayState.Error -> Modifier.height(0.dp)
            else -> Modifier.wrapContentHeight()
        }
    }

    Box(modifier.fillMaxWidth().wrapContentHeight()) {
        Box(modifierAds) {
            when (val currentState = state) {
                is NativeAdDisplayState.Loading, NativeAdDisplayState.Idle -> loading(true)
                is NativeAdDisplayState.Error -> error(currentState.error)
                is NativeAdDisplayState.Success -> {
                    androidx.compose.runtime.key(currentState.ad, currentState.adUnitId) {
                        nativeView(currentState.ad)
                    }
                }
                is NativeAdDisplayState.Cancelled -> {
                    if (currentState.adOptionVisibility == AdOptionVisibility.INVISIBLE) {
                        loading(false)
                    } else {
                        Box(modifier = Modifier.height(0.dp))
                    }
                }
            }
        }

        if (AzAds.getInstance().isShowMessageTester) {
            val debugInfo = remember(state, loadTimeMs) {
                AdDebugInfo(
                    adType = "Native",
                    state = state.debugLabel(),
                    adUnitId = (state as? NativeAdDisplayState.Success)?.adUnitId,
                    fromPreload = (state as? NativeAdDisplayState.Success)?.fromPreload,
                    cacheCount = NativeAdPreload.getInstance()
                        .getNativeAdBuffer((state as? NativeAdDisplayState.Success)?.adUnitId ?: "", 0).size,
                    loadTimeMs = loadTimeMs,
                    tag = holder.tag
                )
            }
            AdDebugOverlayCompose(info = debugInfo, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

private fun NativeAdDisplayState.debugLabel(): String = when (this) {
    is NativeAdDisplayState.Idle -> "Idle"
    is NativeAdDisplayState.Loading -> "Loading"
    is NativeAdDisplayState.Success -> "Loaded"
    is NativeAdDisplayState.Error -> "Error"
    is NativeAdDisplayState.Cancelled -> "Cancelled"
}

@Composable
fun NativeAdWithPreload(
    tag: String,
    modifier: Modifier = Modifier,
    fallbackAdUnitIds: List<String> = emptyList(),
    timeoutPerIdMs: Long = 10_000L,
    autoRequestOnStart: Boolean = true,
    autoReloadOnResume: Boolean = true,
    cancelOnPause: Boolean = false,
    adCallback: AzAdCallback? = null,
    loading: @Composable (Boolean) -> Unit = {},
    error: @Composable (LoadAdError?) -> Unit = {},
    nativeView: @Composable (ApNativeAd) -> Unit = {}
) {
    val holder = rememberNativeAdPreload(
        tag = tag,
        fallbackAdUnitIds = fallbackAdUnitIds,
        timeoutPerIdMs = timeoutPerIdMs,
        autoRequestOnStart = autoRequestOnStart,
        autoReloadOnResume = autoReloadOnResume,
        cancelOnPause = cancelOnPause,
        adCallback = adCallback
    )
    NativeAdCard(holder = holder, modifier = modifier, loading = loading, error = error, nativeView = nativeView)
}

@Composable
fun NativeAdWithPreload(
    config: NativeAdTagConfig,
    modifier: Modifier = Modifier,
    timeoutPerIdMs: Long = 10_000L,
    autoRequestOnStart: Boolean = true,
    autoReloadOnResume: Boolean = true,
    cancelOnPause: Boolean = false,
    adCallback: AzAdCallback? = null,
    loading: @Composable (Boolean) -> Unit = {},
    error: @Composable (LoadAdError?) -> Unit = {},
    nativeView: @Composable (ApNativeAd) -> Unit = {}
) {
    val holder = rememberNativeAdPreload(
        tag = config.tag,
        fallbackAdUnitIds = config.getAllAdUnitIds(),
        timeoutPerIdMs = timeoutPerIdMs,
        autoRequestOnStart = autoRequestOnStart,
        autoReloadOnResume = autoReloadOnResume,
        cancelOnPause = cancelOnPause,
        adCallback = adCallback
    )
    NativeAdCard(holder = holder, modifier = modifier, loading = loading, error = error, nativeView = nativeView)
}

private fun createPreloadHolder(
    tag: String,
    context: android.content.Context,
    fallbackAdUnitIds: List<String>,
    timeoutPerIdMs: Long,
    adOptionVisibility: AdOptionVisibility,
    scope: CoroutineScope,
    destroyOnDispose: Boolean
): NativeAdPreloadHolder {
    val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Idle)
    val ids = fallbackAdUnitIds.filter { it.isNotBlank() }.distinct()

    val launchOnce: () -> Job = {
        scope.launch(Dispatchers.Main.immediate) {
            state.value = NativeAdDisplayState.Loading

            // Try preloaded pool first (uses NativeAdPreload with layoutId=0 for Compose)
            var preloadedAd: ApNativeAd? = null
            var preloadedAdUnitId = ""
            for (id in ids) {
                val ad = NativeAdPreload.getInstance().pollAdNative(id, 0)
                if (ad != null) {
                    preloadedAd = ad
                    preloadedAdUnitId = id
                    break
                }
            }
            if (preloadedAd != null) {
                state.value = NativeAdDisplayState.Success(ad = preloadedAd, adUnitId = preloadedAdUnitId, fromPreload = true)
                return@launch
            }

            if (ids.isNotEmpty()) {
                runColdRequest(context, ids, timeoutPerIdMs, state)
            } else {
                state.value = NativeAdDisplayState.Error(null)
            }
        }
    }

    return NativeAdPreloadHolder(
        adOptionVisibility = adOptionVisibility,
        tag = tag,
        _state = state,
        job = null,
        restartBlock = launchOnce,
        onDispose = {
            if (destroyOnDispose) {
                (state.value as? NativeAdDisplayState.Success)?.let {
                    it.eventRelay?.clearListeners()
                    runCatching { it.ad.admobNativeAd?.destroy() }
                }
            }
        }
    )
}

private suspend fun runColdRequest(
    context: android.content.Context,
    adUnitIds: List<String>,
    timeoutPerIdMs: Long,
    state: MutableStateFlow<NativeAdDisplayState>
) {
    for (id in adUnitIds) {
        val ad = try {
            withTimeout(timeoutPerIdMs) { loadOneAd(context, id) }
        } catch (e: Exception) {
            null
        }
        if (ad != null) {
            state.value = NativeAdDisplayState.Success(ad = ad, adUnitId = id, fromPreload = false)
            return
        }
    }
    state.value = NativeAdDisplayState.Error(null)
}

private suspend fun loadOneAd(
    context: android.content.Context,
    adUnitId: String
): ApNativeAd? = suspendCancellableCoroutine { cont ->
    AzAds.getInstance().loadNativeAdResultCallback(
        context = context,
        id = adUnitId,
        layoutCustomNative = 0,
        callback = object : AzAdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                if (cont.isActive) cont.resume(nativeAd)
            }
            override fun onAdFailedToLoad(adError: ApAdError?) {
                if (cont.isActive) cont.resume(null)
            }
        }
    )
}
