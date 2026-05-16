package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApInterstitialAd
import com.ads.app.gmasdk.control.ads.wrapper.InterstitialAdEvent
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.forceShowInterstitial
import com.ads.app.gmasdk.control.admob.getInterstitialAds
import com.ads.app.gmasdk.control.admob.getInterstitialAdsPreload
import com.ads.app.gmasdk.control.admob.loadInterstitialAd
import com.ads.app.gmasdk.control.admob.preloadInterstitialAds
import com.ads.app.gmasdk.control.admob.showInterstitialAd
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

fun AzAds.preloadInterstitialAds(id: String) {
    whenAdsReady { Admob.getInstance().preloadInterstitialAds(id) }
}

fun AzAds.getInterstitialAdsPreload(id: String): ApInterstitialAd? =
    Admob.getInstance().getInterstitialAdsPreload(id)

@Deprecated(
    "Use loadInterstitial() with InterstitialAdEvent callback",
    ReplaceWith("loadInterstitial(context, id, onResult)")
)
fun AzAds.getInterstitialAds(context: Context, id: String, adListener: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd) {
                super.onInterstitialLoad(interstitialAd)
                Log.d("AzAds", "Admob onInterstitialLoad")
                adListener.onInterstitialLoad(ApInterstitialAd(interstitialAd))
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                adListener.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                super.onAdFailedToShow(fullScreenContentError)
                adListener.onAdFailedToShow(ApAdError(fullScreenContentError))
            }
        })
    }
}

fun AzAds.getInterstitialAdsList(context: Context, listId: List<String>?, adListener: AzAdCallback) {
    whenAdsReady {
        if (listId.isNullOrEmpty()) {
            adListener.onAdFailedToLoad(ApAdError("list id is null or empty"))
        } else {
            val index = intArrayOf(0)
            val adCallback = object : AdCallback() {
                override fun onInterstitialLoad(interstitialAd: InterstitialAd) {
                    super.onInterstitialLoad(interstitialAd)
                    AppLogger.d("AzAds", "loadInterstitialList onInterstitialLoad: ")
                    adListener.onInterstitialLoad(ApInterstitialAd(interstitialAd))
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AppLogger.e("AzAds", "loadInterstitialList onAdFailedToLoad: ${loadAdError.message}")
                    if (index[0] < listId.size - 1) {
                        index[0]++
                        AppLogger.d("AzAds", "loadInterstitialList: ${index[0]} id: ${listId[index[0]]}")
                        Admob.getInstance().getInterstitialAds(context, listId[index[0]], this)
                    } else {
                        adListener.onAdFailedToLoad(ApAdError(loadAdError))
                    }
                }
            }
            AppLogger.d("AzAds", "loadInterstitialList: ${index[0]} id: ${listId[index[0]]}")
            Admob.getInstance().getInterstitialAds(context, listId[index[0]], adCallback)
        }
    }
}

@Deprecated(
    "Use showInterstitial() with InterstitialAdEvent callback",
    ReplaceWith("showInterstitial(activity, mInterstitialAd, onResult)")
)
fun AzAds.forceShowInterstitial(activity: Activity, mInterstitialAd: ApInterstitialAd?, callback: AzAdCallback) {
    if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
        Log.e("AzAds", "forceShowInterstitial: ApInterstitialAd is not ready")
        callback.onNextAction()
        return
    }
    val adCallback = object : AdCallback() {
        override fun onAdClosed() {
            super.onAdClosed()
            callback.onAdClosed()
            callback.onNextAction()
        }
        override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
            super.onAdFailedToShow(fullScreenContentError)
            callback.onAdFailedToShow(ApAdError(fullScreenContentError))
            callback.onNextAction()
        }
        override fun onAdClicked() { super.onAdClicked(); callback.onAdClicked() }
        override fun onAdImpression() { super.onAdImpression(); callback.onAdImpression() }
        override fun onInterstitialShow() { super.onInterstitialShow(); callback.onInterstitialShow() }
        override fun onNextAction() { super.onNextAction(); callback.onNextAction() }
    }
    val interstitialAd = mInterstitialAd.interstitialAd
    if (interstitialAd != null) {
        Admob.getInstance().forceShowInterstitial(activity, interstitialAd, adCallback)
    } else {
        Log.e("AzAds", "forceShowInterstitial: ApInterstitialAd is not ready")
        callback.onNextAction()
    }
}

fun AzAds.loadInterstitial(context: Context, id: String, onResult: (InterstitialAdEvent) -> Unit) {
    whenAdsReady { Admob.getInstance().loadInterstitialAd(context, id, onResult) }
}

fun AzAds.showInterstitial(activity: Activity, ad: ApInterstitialAd, onNextAction: (() -> Unit)?, onResult: (InterstitialAdEvent) -> Unit) {
    Admob.getInstance().showInterstitialAd(activity, ad) { event ->
        onResult(event)
        if (event is InterstitialAdEvent.Dismissed || event is InterstitialAdEvent.FailedToShow) {
            onNextAction?.invoke()
        }
    }
}
