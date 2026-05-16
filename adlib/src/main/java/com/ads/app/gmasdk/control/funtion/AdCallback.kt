package com.ads.app.gmasdk.control.funtion

import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd

open class AdCallback {
    open fun onNextAction() {}
    open fun onAdClosed() {}
    open fun onAdFailedToLoad(loadAdError: LoadAdError) {}
    open fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {}
    open fun onAdLoaded() {}
    open fun onAdSplashReady() {}
    open fun onInterstitialLoad(interstitialAd: InterstitialAd) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    open fun onRewardAdLoaded(rewardedAd: RewardedAd) {}
    open fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd) {}
    open fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {}
    open fun onInterstitialShow() {}
    open fun onBannerLoaded(adView: AdView) {}
}
