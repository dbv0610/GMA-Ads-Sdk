package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardAd
import com.ads.app.gmasdk.control.ads.wrapper.ApRewardItem
import com.ads.app.gmasdk.control.ads.wrapper.RewardAdEvent
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.getRewardAdsPreload
import com.ads.app.gmasdk.control.admob.getRewardInterstitial
import com.ads.app.gmasdk.control.admob.getRewardInterstitialAdsPreload
import com.ads.app.gmasdk.control.admob.initRewardAds
import com.ads.app.gmasdk.control.admob.loadRewardAd
import com.ads.app.gmasdk.control.admob.preloadRewardAds
import com.ads.app.gmasdk.control.admob.preloadRewardInterstitialAds
import com.ads.app.gmasdk.control.admob.showRewardAd
import com.ads.app.gmasdk.control.admob.showRewardAds
import com.ads.app.gmasdk.control.admob.showRewardInterstitial
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.funtion.RewardCallback
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd

fun AzAds.preloadRewardAd(id: String) {
    whenAdsReady { Admob.getInstance().preloadRewardAds(id) }
}

fun AzAds.getRewardAdPreload(id: String): ApRewardAd? =
    Admob.getInstance().getRewardAdsPreload(id)

fun AzAds.preloadRewardInterstitialAd(id: String) {
    whenAdsReady { Admob.getInstance().preloadRewardInterstitialAds(id) }
}

fun AzAds.getRewardInterstitialAdPreload(id: String): ApRewardAd? =
    Admob.getInstance().getRewardInterstitialAdsPreload(id)

@Deprecated(
    "Use loadReward() with RewardAdEvent callback",
    ReplaceWith("loadReward(activity, id, onResult)")
)
fun AzAds.loadRewardAd(activity: Activity, id: String, callback: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().initRewardAds(activity as Context, id, object : AdCallback() {
            override fun onRewardAdLoaded(rewardedAd: RewardedAd) {
                super.onRewardAdLoaded(rewardedAd)
                callback.onRewardAdLoaded(ApRewardAd(rewardedAd))
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                callback.onAdFailedToLoad(ApAdError(loadAdError))
            }
        })
    }
}

fun AzAds.loadRewardAdList(activity: Activity, listId: List<String>?, azAdCallback: AzAdCallback) {
    whenAdsReady {
        if (listId.isNullOrEmpty()) {
            azAdCallback.onAdFailedToLoad(ApAdError("list id is null or empty"))
        } else {
            val index = intArrayOf(0)
            val adCallback = object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd) {
                    super.onRewardAdLoaded(rewardedAd)
                    AppLogger.d("AzAds", "getRewardAdList onRewardAdLoaded: ")
                    azAdCallback.onRewardAdLoaded(ApRewardAd(rewardedAd))
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AppLogger.e("AzAds", "onAdFailedToLoad: ${loadAdError.message}")
                    if (index[0] < listId.size - 1) {
                        index[0]++
                        AppLogger.d("AzAds", "getRewardAdList: ${index[0]} id: ${listId[index[0]]}")
                        Admob.getInstance().initRewardAds(activity as Context, listId[index[0]], this)
                    } else {
                        azAdCallback.onAdFailedToLoad(ApAdError(loadAdError))
                    }
                }
            }
            AppLogger.d("AzAds", "getRewardAdList: ${index[0]} id: ${listId[index[0]]}")
            Admob.getInstance().initRewardAds(activity as Context, listId[index[0]], adCallback)
        }
    }
}

fun AzAds.loadRewardInterstitialAd(activity: Activity, id: String, callback: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().getRewardInterstitial(activity as Context, id, object : AdCallback() {
            override fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd) {
                super.onRewardAdLoaded(rewardedAd)
                callback.onAdLoaded()
                callback.onRewardAdLoaded(ApRewardAd(rewardedAd))
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                callback.onAdFailedToLoad(ApAdError(loadAdError))
            }
        })
    }
}

@Deprecated(
    "Use showReward() with RewardAdEvent callback",
    ReplaceWith("showReward(activity, apRewardAd, onResult)")
)
fun AzAds.forceShowRewardAd(activity: Activity, apRewardAd: ApRewardAd, callback: AzAdCallback) {
    if (!apRewardAd.isReady()) {
        Log.e("AzAds", "forceShowRewardAd fail: reward ad not ready")
        callback.onNextAction()
        return
    }
    if (apRewardAd.isRewardInterstitial()) {
        val admobRewardInter = apRewardAd.admobRewardInter
        if (admobRewardInter == null) {
            Log.e("AzAds", "forceShowRewardAd fail: reward ad not ready")
            callback.onNextAction()
            return
        }
        Admob.getInstance().showRewardInterstitial(activity, admobRewardInter, object : RewardCallback {
            override fun onUserEarnedReward(rewardItem: RewardItem) {
                callback.onUserEarnedReward(ApRewardItem(rewardItem))
            }
            override fun onRewardedAdClosed() {
                callback.onAdClosed()
                callback.onNextAction()
            }
            override fun onRewardedAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                callback.onAdFailedToShow(ApAdError(fullScreenContentError))
                callback.onNextAction()
            }
            override fun onAdClicked() { callback.onAdClicked() }
            override fun onAdImpression() { callback.onAdImpression() }
        })
    } else {
        val admobReward = apRewardAd.admobReward
        if (admobReward == null) {
            Log.e("AzAds", "forceShowRewardAd fail: reward ad not ready")
            callback.onNextAction()
            return
        }
        Admob.getInstance().showRewardAds(activity, admobReward, object : RewardCallback {
            override fun onUserEarnedReward(rewardItem: RewardItem) {
                callback.onUserEarnedReward(ApRewardItem(rewardItem))
            }
            override fun onRewardedAdClosed() {
                callback.onAdClosed()
                callback.onNextAction()
            }
            override fun onRewardedAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                callback.onAdFailedToShow(ApAdError(fullScreenContentError))
                callback.onNextAction()
            }
            override fun onAdClicked() { callback.onAdClicked() }
            override fun onAdImpression() { callback.onAdImpression() }
        })
    }
}

fun AzAds.loadReward(activity: Activity, id: String, onResult: (RewardAdEvent) -> Unit) {
    whenAdsReady { Admob.getInstance().loadRewardAd(activity as Context, id, onResult) }
}

fun AzAds.showReward(activity: Activity, ad: ApRewardAd, onNextAction: (() -> Unit)?, onResult: (RewardAdEvent) -> Unit) {
    Admob.getInstance().showRewardAd(activity, ad) { event ->
        onResult(event)
        if (event is RewardAdEvent.Dismissed || event is RewardAdEvent.FailedToShow || event is RewardAdEvent.NotReady) {
            onNextAction?.invoke()
        }
    }
}
