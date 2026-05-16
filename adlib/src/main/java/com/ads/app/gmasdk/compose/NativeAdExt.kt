package com.ads.app.gmasdk.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Display a native ad with minimal setup.
 *
 * Usage:
 * ```kotlin
 * NativeAd(tag = "home", adUnitId = BuildConfig.AD_NATIVE) { ad ->
 *     NativeAdView(ad) {
 *         NativeHeadlineRow()
 *         NativeBodyText()
 *         NativeCtaButton()
 *     }
 * }
 * ```
 */
@Composable
fun NativeAd(
    tag: String,
    adUnitId: String,
    modifier: Modifier = Modifier,
    timeoutPerIdMs: Long = 10_000L,
    autoReloadOnResume: Boolean = true,
    adCallback: AzAdCallback? = null,
    loading: @Composable (Boolean) -> Unit = {},
    error: @Composable (LoadAdError?) -> Unit = {},
    nativeView: @Composable (ApNativeAd) -> Unit = {}
) {
    val config = remember(tag, adUnitId) {
        NativeAdTagConfig.simple(tag = tag, adUnitId = adUnitId)
    }
    NativeAdWithPreload(
        config = config,
        modifier = modifier,
        timeoutPerIdMs = timeoutPerIdMs,
        autoReloadOnResume = autoReloadOnResume,
        adCallback = adCallback,
        loading = loading,
        error = error,
        nativeView = nativeView
    )
}

/**
 * Display a native ad with waterfall ad unit IDs.
 *
 * Usage:
 * ```kotlin
 * NativeAd(
 *     tag = "home",
 *     adUnitIds = listOf(BuildConfig.AD_NATIVE_HIGH, BuildConfig.AD_NATIVE_LOW),
 * ) { ad -> ... }
 * ```
 */
@Composable
fun NativeAd(
    tag: String,
    adUnitIds: List<String>,
    modifier: Modifier = Modifier,
    timeoutPerIdMs: Long = 10_000L,
    autoReloadOnResume: Boolean = true,
    adCallback: AzAdCallback? = null,
    loading: @Composable (Boolean) -> Unit = {},
    error: @Composable (LoadAdError?) -> Unit = {},
    nativeView: @Composable (ApNativeAd) -> Unit = {}
) {
    val config = remember(tag, adUnitIds.hashCode()) {
        NativeAdTagConfig.waterfall(tag = tag, adUnitIds = adUnitIds)
    }
    NativeAdWithPreload(
        config = config,
        modifier = modifier,
        timeoutPerIdMs = timeoutPerIdMs,
        autoReloadOnResume = autoReloadOnResume,
        adCallback = adCallback,
        loading = loading,
        error = error,
        nativeView = nativeView
    )
}
