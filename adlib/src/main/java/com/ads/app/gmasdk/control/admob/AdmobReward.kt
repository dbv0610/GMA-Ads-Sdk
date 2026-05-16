package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardAd
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardItem
import com.ads.app.gmasdk.control.ads.wrapper.RewardAdEvent
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.funtion.RewardCallback
import com.ads.app.gmasdk.control.helper.extension.extractAdUnitIdOrNull
import com.ads.app.gmasdk.control.util.runOnMain
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.MediationAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader

private const val TAG = "AzAdmob"

fun Admob.preloadRewardAds(id: String) {
    val request = getAdRequest(id)
    val preloadConfig = PreloadConfiguration(request)
    RewardedAdPreloader.start(id, preloadConfig)
}

fun Admob.getRewardAdsPreload(id: String): ApRewardAd? {
    val ad = RewardedAdPreloader.pollAd(id)
    return ad?.let { ApRewardAd(it) }
}

fun Admob.preloadRewardInterstitialAds(id: String) {
    val request = getAdRequest(id)
    val preloadConfig = PreloadConfiguration(request)
    RewardedInterstitialAdPreloader.start(id, preloadConfig)
}

fun Admob.getRewardInterstitialAdsPreload(id: String): ApRewardAd? {
    val ad = RewardedInterstitialAdPreloader.pollAd(id)
    return ad?.let { ApRewardAd(it) }
}

fun Admob.initRewardAds(context: Context, id: String, callback: AdCallback) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.REWARD_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        return
    }
    val appCtx = context.applicationContext
    RewardedAd.load(getAdRequest(id), object : AdLoadCallback<RewardedAd> {
        override fun onAdLoaded(ad: RewardedAd) {
            callback.onRewardAdLoaded(ad)
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            super.onAdFailedToLoad(adError)
            callback.onAdFailedToLoad(adError)
        }
    })
}

fun Admob.getRewardInterstitial(context: Context, id: String, callback: AdCallback) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.REWARD_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        return
    }
    RewardedInterstitialAd.load(getAdRequest(id), object : AdLoadCallback<RewardedInterstitialAd> {
        override fun onAdLoaded(ad: RewardedInterstitialAd) {
            callback.onRewardAdLoaded(ad)
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            callback.onAdFailedToLoad(adError)
        }
    })
}

fun Admob.showRewardInterstitial(
    activity: Activity,
    rewardedInterstitialAd: RewardedInterstitialAd,
    adCallback: RewardCallback
) {
    if (AppPurchase.getInstance().isPurchased()) {
        adCallback.onRewardedAdFailedToShow(
            FullScreenContentError(FullScreenContentError.ErrorCode.INTERNAL_ERROR, "App is purchased", null as MediationAdError?)
        )
        return
    }
    val appCtx = activity.applicationContext
    val responseInfo = rewardedInterstitialAd.getResponseInfo()
    val id = responseInfo.extractAdUnitIdOrNull()
    rewardedInterstitialAd.adEventCallback =(object : RewardedInterstitialAdEventCallback {
        override fun onAdDismissedFullScreenContent() {
            adCallback.onRewardedAdClosed()
        }

        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
            adCallback.onRewardedAdFailedToShow(error)
        }

        override fun onAdShowedFullScreenContent() {}

        override fun onAdClicked() {
            adCallback.onAdClicked()
        }

        override fun onAdImpression() {
            adCallback.onAdImpression()
        }
    })
    rewardedInterstitialAd.show(activity) { rewardItem ->
        runOnMain { adCallback.onUserEarnedReward(rewardItem) }
    }
}

fun Admob.showRewardAds(
    activity: Activity,
    rewardedAd: RewardedAd,
    adCallback: RewardCallback
) {
    if (AppPurchase.getInstance().isPurchased()) {
        adCallback.onRewardedAdFailedToShow(
            FullScreenContentError(FullScreenContentError.ErrorCode.INTERNAL_ERROR, "App is purchased", null as MediationAdError?)
        )
        return
    }
    val appCtx = activity.applicationContext
    val responseInfo = rewardedAd.getResponseInfo()
    val id = responseInfo.extractAdUnitIdOrNull()
    rewardedAd.adEventCallback =(object : RewardedAdEventCallback {
        override fun onAdDismissedFullScreenContent() {
            adCallback.onRewardedAdClosed()
        }

        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
            adCallback.onRewardedAdFailedToShow(error)
        }

        override fun onAdShowedFullScreenContent() {}

        override fun onAdClicked() {
            adCallback.onAdClicked()
        }

        override fun onAdImpression() {
            adCallback.onAdImpression()
        }
    })
    rewardedAd.show(activity) { rewardItem ->
        runOnMain { adCallback.onUserEarnedReward(rewardItem) }
    }
}

fun Admob.loadRewardAd(context: Context, id: String, onResult: (RewardAdEvent) -> Unit) {
    initRewardAds(context, id, object : AdCallback() {
        override fun onRewardAdLoaded(rewardedAd: RewardedAd) {
            val apAd = ApRewardAd(rewardedAd)
            onResult(RewardAdEvent.Loaded(apAd))
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            onResult(RewardAdEvent.Failed(ApAdError(loadAdError.message)))
        }
    })
}

fun Admob.showRewardAd(activity: Activity, apAd: ApRewardAd, onResult: (RewardAdEvent) -> Unit) {
    if (!apAd.isReady()) {
        onResult(RewardAdEvent.NotReady)
        return
    }
    if (AppPurchase.getInstance().isPurchased()) {
        onResult(RewardAdEvent.FailedToShow(ApAdError("App is purchased")))
        return
    }
    val appCtx = activity.applicationContext
    if (apAd.isRewardInterstitial()) {
        val ad = apAd.admobRewardInter!!
        val responseInfo = ad.getResponseInfo()
        val adUnitId = responseInfo.extractAdUnitIdOrNull()
        ad.adEventCallback =(object : RewardedInterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                onResult(RewardAdEvent.Dismissed(apAd))
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                onResult(RewardAdEvent.FailedToShow(ApAdError(error.message)))
            }

            override fun onAdShowedFullScreenContent() {
                onResult(RewardAdEvent.Shown(apAd))
            }

            override fun onAdClicked() {
                onResult(RewardAdEvent.Clicked(apAd))
            }

            override fun onAdImpression() {}
        })
        ad.show(activity) { rewardItem ->
            runOnMain { onResult(RewardAdEvent.Rewarded(apAd, ApRewardItem(rewardItem))) }
        }
    } else {
        val ad = apAd.admobReward!!
        val responseInfo = ad.getResponseInfo()
        val adUnitId = responseInfo.extractAdUnitIdOrNull()
        ad.adEventCallback =(object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                onResult(RewardAdEvent.Dismissed(apAd))
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                onResult(RewardAdEvent.FailedToShow(ApAdError(error.message)))
            }

            override fun onAdShowedFullScreenContent() {
                onResult(RewardAdEvent.Shown(apAd))
            }

            override fun onAdClicked() {
                onResult(RewardAdEvent.Clicked(apAd))
            }

            override fun onAdImpression() {}
        })
        ad.show(activity) { rewardItem ->
            runOnMain { onResult(RewardAdEvent.Rewarded(apAd, ApRewardItem(rewardItem))) }
        }
    }
}
