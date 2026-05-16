package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ads.app.gmasdk.control.ads.AzAdCallback
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.helper.extension.extractAdUnitIdOrNull
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

private const val TAG = "AzAdmob"

fun Admob.loadSplashInterstitialAds(
    context: Context,
    id: String,
    timeOut: Long,
    timeDelay: Long,
    adListener: AdCallback
) {
    Log.i(TAG, "loadSplashInterstitialAds: loading")
    isTimeDelay = false
    isTimeout = false

    if (AppPurchase.getInstance().isPurchased()) {
        adListener.onNextAction()
        return
    }

    handlerTimeDelay = Handler(Looper.getMainLooper())
    val rdDelay = Runnable {
        if (interstitialSplash != null) {
            Log.i(TAG, "loadSplashInterstitialAds: onAdSplashReady after delay")
            adListener.onAdSplashReady()
        } else {
            Log.i(TAG, "loadSplashInterstitialAds: delay validate")
            isTimeDelay = true
        }
    }
    rdTimeDelay = rdDelay
    handlerTimeDelay?.postDelayed(rdDelay, timeDelay)

    if (timeOut > 0L) {
        handlerTimeout = Handler(Looper.getMainLooper())
        val rdOut = Runnable {
            Log.e(TAG, "loadSplashInterstitialAds: on timeout")
            isTimeout = true
            if (interstitialSplash != null) {
                Log.i(TAG, "loadSplashInterstitialAds: onAdSplashReady after timeout ")
                adListener.onAdSplashReady()
            } else {
                adListener.onNextAction()
            }
        }
        rdTimeout = rdOut
        handlerTimeout?.postDelayed(rdOut, timeOut)
    }

    getInterstitialAds(context, id, object : AdCallback() {
        override fun onInterstitialLoad(interstitialAd: InterstitialAd) {
            interstitialSplash = interstitialAd
            if (isTimeout || isTimeDelay) {
                adListener.onAdSplashReady()
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            if (!isTimeout) {
                adListener.onNextAction()
            }
        }
    })
}

fun Admob.loadSplashListAds(
    context: Context,
    listId: List<String>,
    timeOut: Long,
    timeDelay: Long,
    adCallback: AzAdCallback
) {
    isTimeDelay = false
    isTimeout = false
    Log.i(TAG, "loadSplashListAds loading: ")

    if (AppPurchase.getInstance().isPurchased()) {
        adCallback.onNextAction()
        return
    }

    Handler(Looper.getMainLooper()).postDelayed({
        if (interstitialSplash != null) {
            Log.i(TAG, "loadSplashListAds: onAdSplashReady after delay")
            adCallback.onAdSplashReady()
        } else {
            Log.i(TAG, "loadSplashListAds: delay validate")
            isTimeDelay = true
        }
    }, timeDelay)

    if (timeOut > 0L) {
        handlerTimeout = Handler(Looper.getMainLooper())
        val rdOut = Runnable {
            Log.e(TAG, "loadSplashListAds: on timeout")
            isTimeout = true
            if (interstitialSplash != null) {
                Log.i(TAG, "loadSplashListAds: onAdSplashReady after timeout ")
                adCallback.onAdSplashReady()
            } else {
                adCallback.onNextAction()
            }
        }
        rdTimeout = rdOut
        handlerTimeout?.postDelayed(rdOut, timeOut)
    }

    loadNextAdSplashList(context, listId, 0, adCallback)
}

internal fun Admob.loadNextAdSplashList(
    context: Context,
    listId: List<String>,
    index: Int,
    adCallback: AzAdCallback
) {
    if (index >= listId.size) {
        Log.e(TAG, "loadNextAdSplashList: All ad IDs failed.")
        val h = handlerTimeout
        val r = rdTimeout
        if (h != null && r != null) {
            h.removeCallbacks(r)
        }
        adCallback.onNextAction()
        adCallback.onAdFailedToLoad(ApAdError("All ad IDs failed."))
        return
    }
    Log.i(TAG, "loadNextAdSplashList: Trying ad ID at index $index")
    getInterstitialAds(context, listId[index], object : AdCallback() {
        override fun onInterstitialLoad(interstitialAd: InterstitialAd) {
            interstitialSplash = interstitialAd
            if (isTimeout || isTimeDelay) {
                adCallback.onAdSplashReady()
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            loadNextAdSplashList(context, listId, index + 1, adCallback)
        }
    })
}

fun Admob.onShowSplash(activity: Activity, adListener: AdCallback) {
    val ad = interstitialSplash
    Log.d(TAG, "onShowSplash: ")
    if (ad == null) {
        Log.d(TAG, "onShowSplash: ad null")
        adListener.onNextAction()
        return
    }
    val responseInfo = ad.getResponseInfo()
    val adUnitId = responseInfo.extractAdUnitIdOrNull()
    val appCtx = activity.applicationContext
    ad.adEventCallback =(object : InterstitialAdEventCallback {
        override fun onAdDismissedFullScreenContent() {
            interstitialSplash = null
            safeDismissDialog(activity)
            adListener.onAdClosed()
            adListener.onNextAction()
        }

        override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
            safeDismissDialog(activity)
            adListener.onAdFailedToShow(error)
            adListener.onNextAction()
        }

        override fun onAdShowedFullScreenContent() {}

        override fun onAdClicked() {
            adListener.onAdClicked()
        }

        override fun onAdImpression() {
            adListener.onAdImpression()
        }
    })
    showLoadingDialogSafely(activity)
    Handler(Looper.getMainLooper()).postDelayed({
        if (openActivityAfterShowInterAds) {
            adListener.onNextAction()
            Handler(Looper.getMainLooper()).postDelayed({
                safeDismissDialog(activity)
            }, 1500L)
        }
        ad.show(activity)
    }, 800L)
}

fun Admob.onCheckShowSplashWhenFail(activity: Activity, callback: AdCallback, timeDelay: Int) {
    Handler(activity.mainLooper).postDelayed({
        if (interstitialSplashLoaded() && !isShowLoadingSplash) {
            Log.i(TAG, "show ad splash when show fail in background")
            onShowSplash(activity, callback)
        }
    }, timeDelay.toLong())
}
