package com.ads.app.gmasdk.control.ads

import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApInterstitialAd
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardAd
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardItem

open class AzAdCallback {
    open fun onNextAction() {}
    open fun onAdClosed() {}
    open fun onAdFailedToLoad(adError: ApAdError?) {}
    open fun onAdFailedToShow(adError: ApAdError?) {}
    open fun onAdLoaded() {}
    open fun onRewardAdLoaded(apRewardAd: ApRewardAd) {}
    open fun onAdSplashReady() {}
    open fun onInterstitialLoad(interstitialAd: ApInterstitialAd?) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    open fun onNativeAdLoaded(nativeAd: ApNativeAd) {}
    open fun onUserEarnedReward(rewardItem: ApRewardItem) {}
    open fun onInterstitialShow() {}
}
