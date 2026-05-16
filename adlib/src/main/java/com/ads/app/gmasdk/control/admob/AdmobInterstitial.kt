package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApInterstitialAd
import com.ads.app.gmasdk.control.ads.wrapper.InterstitialAdEvent
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.dialog.PrepareLoadingAdsDialog
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader

private const val TAG = "AzAdmob"

fun Admob.preloadInterstitialAds(id: String) {
    val request = getAdRequest(id)
    val preloadConfig = PreloadConfiguration(request)
    InterstitialAdPreloader.start(id, preloadConfig)
}

fun Admob.getInterstitialAdsPreload(id: String): ApInterstitialAd? {
    val ad = InterstitialAdPreloader.pollAd(id)
    return ad?.let { ApInterstitialAd(it) }
}

fun Admob.getInterstitialAds(context: Context, id: String, adCallback: AdCallback) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.INTERS_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        adCallback.onAdFailedToLoad(LoadAdError(LoadAdError.ErrorCode.CANCELLED, "App is purchased", null as ResponseInfo?))
        return
    }
    InterstitialAd.load(getAdRequest(id), object : AdLoadCallback<InterstitialAd> {
        override fun onAdLoaded(ad: InterstitialAd) {
            super.onAdLoaded(ad)
            adCallback.onInterstitialLoad(ad)
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            super.onAdFailedToLoad(adError)
            adCallback.onAdFailedToLoad(adError)
        }
    })
}

fun Admob.forceShowInterstitial(activity: Activity, mInterstitialAd: InterstitialAd, callback: AdCallback) {
    if (AppPurchase.getInstance().isPurchased()) {
        callback.onNextAction()
        return
    }
    val responseInfo = mInterstitialAd.getResponseInfo()
    mInterstitialAd.adEventCallback =object : InterstitialAdEventCallback {
        override fun onAdDismissedFullScreenContent() {
            disableAdResumeWhenClickAds = false
            interstitialSplash = null
            safeDismissDialog(activity)
            callback.onAdClosed()
            callback.onNextAction()
        }

        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
            safeDismissDialog(activity)
            callback.onAdFailedToShow(error)
            callback.onNextAction()
        }

        override fun onAdShowedFullScreenContent() {
            disableAdResumeWhenClickAds = true
        }

        override fun onAdClicked() {
            callback.onAdClicked()
        }

        override fun onAdImpression() {
            callback.onAdImpression()
        }
    }
    showInterstitialAdInternal(activity, mInterstitialAd, callback)
}

fun Admob.loadInterstitialAd(context: Context, id: String, onResult: (InterstitialAdEvent) -> Unit) {
    getInterstitialAds(context, id, object : AdCallback() {
        override fun onInterstitialLoad(interstitialAd: InterstitialAd) {
            val apAd = ApInterstitialAd(interstitialAd)
            onResult(InterstitialAdEvent.Loaded(apAd))
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            onResult(InterstitialAdEvent.Failed(ApAdError(loadAdError.message)))
        }
    })
}

fun Admob.showInterstitialAd(activity: Activity, apAd: ApInterstitialAd, onResult: (InterstitialAdEvent) -> Unit) {
    val interstitialAd = apAd.interstitialAd
    if (interstitialAd == null) {
        onResult(InterstitialAdEvent.FailedToShow(ApAdError("Interstitial not ready")))
        return
    }
    forceShowInterstitial(activity, interstitialAd, object : AdCallback() {
        override fun onAdClosed() {
            onResult(InterstitialAdEvent.Dismissed(apAd))
        }

        override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
            onResult(InterstitialAdEvent.FailedToShow(ApAdError(fullScreenContentError.message)))
        }

        override fun onAdClicked() {
            onResult(InterstitialAdEvent.Clicked(apAd))
        }

        override fun onAdImpression() {
            onResult(InterstitialAdEvent.Shown(apAd))
        }

        override fun onNextAction() {}
    })
}

internal fun Admob.showInterstitialAdInternal(activity: Activity, mInterstitialAd: InterstitialAd, callback: AdCallback) {
    try {
        if (dialog != null && dialog!!.isShowing) {
            try {
                dialog?.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dialog = PrepareLoadingAdsDialog(activity)
        dialog?.setCancelable(false)
        try {
            callback.onInterstitialShow()
            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onNextAction()
            return
        }
    } catch (e: Exception) {
        dialog = null
        e.printStackTrace()
    }

    Handler(Looper.getMainLooper()).postDelayed({
        if (openActivityAfterShowInterAds) {
            callback.onNextAction()
            Handler(Looper.getMainLooper()).postDelayed({
                if (dialog != null && dialog!!.isShowing && !activity.isDestroyed) {
                    try {
                        dialog?.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 1500L)
        }
        mInterstitialAd.show(activity)
    }, 800L)
}
