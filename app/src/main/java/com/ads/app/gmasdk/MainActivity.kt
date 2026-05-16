package com.ads.app.gmasdk

import android.annotation.SuppressLint
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ads.app.gmasdk.compose.BannerAd
import com.ads.app.gmasdk.compose.NativeAd
import com.ads.app.gmasdk.compose.NativeAdView
import com.ads.app.gmasdk.compose.NativeBodyText
import com.ads.app.gmasdk.compose.NativeCtaButton
import com.ads.app.gmasdk.compose.NativeHeadlineRow
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.ads.loadInterstitial
import com.ads.app.gmasdk.control.ads.loadReward
import com.ads.app.gmasdk.control.ads.showInterstitial
import com.ads.app.gmasdk.control.ads.showReward
import com.ads.app.gmasdk.control.ads.wrapper.ApInterstitialAd
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardAd
import com.ads.app.gmasdk.control.ads.wrapper.InterstitialAdEvent
import com.ads.app.gmasdk.control.ads.wrapper.RewardAdEvent
import com.ads.app.gmasdk.control.helper.adnative.NativeAdConfig
import com.ads.app.gmasdk.control.helper.adnative.NativeAdHelper
import com.ads.app.gmasdk.control.helper.adnative.params.NativeAdParam
import com.ads.app.gmasdk.control.helper.banner.BannerAdConfig
import com.ads.app.gmasdk.control.helper.banner.BannerAdHelper
import com.ads.app.gmasdk.control.helper.banner.params.BannerAdParam
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import com.ads.app.gmasdk.compose.NativeAdAdvertiserView
import com.ads.app.gmasdk.compose.NativeAdIconView
import com.ads.app.gmasdk.compose.NativeAdMediaView
import com.ads.app.gmasdk.compose.NativeIconContent

private const val AD_BANNER = "ca-app-pub-3940256099942544/6300978111"
private const val AD_NATIVE = "ca-app-pub-3940256099942544/2247696110"
private const val AD_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
private const val AD_REWARD = "ca-app-pub-3940256099942544/5224354917"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoScreen()
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
private fun DemoScreen() {
    val activity = LocalContext.current as ComponentActivity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Ad Demo", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        AdSection("Compose — Banner") { ComposeBannerSection() }
        AdSection("Compose — Native") { ComposeNativeSection() }
        AdSection("XML — Banner") { XmlBannerSection() }
        AdSection("XML — Native") { XmlNativeSection() }
        AdSection("Interstitial") { InterstitialSection(activity) }
        AdSection("Reward") { RewardSection(activity) }
    }
}

@Composable
private fun AdSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        HorizontalDivider()
        content()
    }
}

@Composable
private fun ComposeBannerSection() {
    val config = remember {
        BannerAdConfig(idAds = AD_BANNER, canShowAds = true, canReloadAds = true)
    }
    BannerAd(config = config)
}

@Composable
private fun ComposeNativeSection() {
    NativeAd(tag = "demo_native", adUnitId = AD_NATIVE) { ad ->
        NativeAdView(nativeAd = ad) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth()) {
                    NativeIconContent(Modifier.size(48.dp))
                    Column(Modifier.weight(1f)) {
                        NativeHeadlineRow()
                        NativeBodyText()
                    }

                }

                NativeAdMediaView(modifier = Modifier.height(120.dp))
                NativeAdAdvertiserView(modifier = Modifier.fillMaxWidth().height(18.dp).background(
                    Color.LightGray)) {
                    Text(it, modifier = Modifier.fillMaxWidth())
                }
                NativeCtaButton()
            }

        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
private fun XmlBannerSection() {
    val activity = LocalContext.current as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            FrameLayout(ctx).also { container ->
                BannerAdHelper(
                    activity = activity,
                    lifecycleOwner = lifecycleOwner,
                    config = BannerAdConfig(
                        idAds = AD_BANNER,
                        canShowAds = true,
                        canReloadAds = true
                    )
                ).setBannerContentView(container)
                    .requestAds(BannerAdParam.Request)
            }
        }
    )
}

@SuppressLint("ContextCastToActivity")
@Composable
private fun XmlNativeSection() {
    val activity = LocalContext.current as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            FrameLayout(ctx).also { container ->
                NativeAdHelper(
                    activity = activity,
                    lifecycleOwner = lifecycleOwner,
                    config = NativeAdConfig(
                        idAds = AD_NATIVE,
                        canShowAds = true,
                        canReloadAds = true,
                        layoutId = R.layout.custom_native_admod_medium
                    )
                ).setNativeContentView(container)
                    .requestAds(NativeAdParam.Request.CreateRequest)
            }
        }
    )
}

@Composable
private fun InterstitialSection(activity: ComponentActivity) {
    var loadedAd by remember { mutableStateOf<ApInterstitialAd?>(null) }
    var status by remember { mutableStateOf("Idle") }

    Text("Status: $status")
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                status = "Loading..."
                loadedAd = null
                AzAds.getInstance().loadInterstitial(activity, AD_INTERSTITIAL) { event ->
                    when (event) {
                        is InterstitialAdEvent.Loaded -> {
                            loadedAd = event.ad
                            status = "Loaded — tap Show"
                        }

                        is InterstitialAdEvent.Failed -> status =
                            "Failed: ${event.error.getMessage()}"

                        else -> {}
                    }
                }
            }
        ) { Text("Load") }

        Button(
            enabled = loadedAd != null,
            onClick = {
                val ad = loadedAd ?: return@Button
                AzAds.getInstance().showInterstitial(activity, ad, null) { event ->
                    when (event) {
                        is InterstitialAdEvent.Shown -> status = "Showing"
                        is InterstitialAdEvent.Dismissed -> {
                            loadedAd = null
                            status = "Dismissed"
                        }

                        is InterstitialAdEvent.FailedToShow -> {
                            loadedAd = null
                            status = "Failed to show"
                        }

                        else -> {}
                    }
                }
            }
        ) { Text("Show") }
    }
}

@Composable
private fun RewardSection(activity: ComponentActivity) {
    var loadedAd by remember { mutableStateOf<ApRewardAd?>(null) }
    var status by remember { mutableStateOf("Idle") }

    Text("Status: $status")
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                status = "Loading..."
                loadedAd = null
                AzAds.getInstance().loadReward(activity, AD_REWARD) { event ->
                    when (event) {
                        is RewardAdEvent.Loaded -> {
                            loadedAd = event.ad
                            status = "Loaded — tap Show"
                        }

                        is RewardAdEvent.Failed -> status = "Failed: ${event.error.getMessage()}"
                        else -> {}
                    }
                }
            }
        ) { Text("Load") }

        Button(
            enabled = loadedAd != null,
            onClick = {
                val ad = loadedAd ?: return@Button
                AzAds.getInstance().showReward(activity, ad, null) { event ->
                    when (event) {
                        is RewardAdEvent.Shown -> status = "Showing"
                        is RewardAdEvent.Rewarded -> status =
                            "Rewarded: ${event.item.admobRewardItem.type} x${event.item.admobRewardItem.amount}"

                        is RewardAdEvent.Dismissed -> {
                            loadedAd = null
                            status = "Dismissed"
                        }

                        is RewardAdEvent.FailedToShow -> {
                            loadedAd = null
                            status = "Failed to show"
                        }

                        else -> {}
                    }
                }
            }
        ) { Text("Show") }
    }
}
