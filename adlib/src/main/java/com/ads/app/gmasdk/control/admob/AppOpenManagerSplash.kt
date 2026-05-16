package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

private const val TAG = "AppOpenManager"

fun AppOpenManager.loadOpenAppAdSplash(
    activity: Activity,
    timeDelay: Long,
    timeOut: Long,
    adCallback: AdCallback
) {
    AzAds.getInstance().runWhenReady {
        val adId = splashAdId
        if (adId.isNullOrEmpty()) {
            Log.d(TAG, "loadOpenAppAdSplash: splashAdId null or empty")
            adCallback.onNextAction()
            return@runWhenReady
        }
        if (AppPurchase.getInstance().isPurchased()) {
            adCallback.onNextAction()
            return@runWhenReady
        }
        isTimeout = false
        val startLoadAd = System.currentTimeMillis()
        val handleTimeOut = Handler(Looper.getMainLooper())
        val actionTimeOut = Runnable {
            isTimeout = true
            Log.d(TAG, "getAdSplash time out")
            adCallback.onNextAction()
            AppOpenManager.isShowingAd = false
        }
        handleTimeOut.postDelayed(actionTimeOut, timeOut)

        AppOpenAd.load(getAdRequest(adId), object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                handleTimeOut.removeCallbacks(actionTimeOut)
                splashAd = ad
                if (isTimeout) return
                val elapsed = System.currentTimeMillis() - startLoadAd
                val remaining = timeDelay - elapsed
                if (remaining > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        adCallback.onAdSplashReady()
                    }, remaining)
                } else {
                    adCallback.onAdSplashReady()
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                handleTimeOut.removeCallbacks(actionTimeOut)
                if (!isTimeout) {
                    adCallback.onNextAction()
                }
            }
        })
    }
}

fun AppOpenManager.loadOpenAppAdSplashList(
    activity: Activity,
    timeDelay: Long,
    timeOut: Long,
    adCallback: AdCallback
) {
    AzAds.getInstance().runWhenReady {
        val list = splashAdIdList
        if (list.isNullOrEmpty()) {
            Log.d(TAG, "loadOpenAppAdSplashList: input list is null or empty")
            adCallback.onNextAction()
            return@runWhenReady
        }
        if (AppPurchase.getInstance().isPurchased()) {
            Log.d(TAG, "loadOpenAppAdSplashList: has been purchased")
            adCallback.onNextAction()
            return@runWhenReady
        }
        Log.d(TAG, "loadOpenAppAdSplashList: list_size = ${list.size}")

        Handler(Looper.getMainLooper()).postDelayed({
            if (splashAd != null) {
                adCallback.onAdSplashReady()
            } else {
                isTimeDelay = true
                Log.i(TAG, "loadOpenAppAdSplashList: time delay has been reached")
            }
        }, timeDelay)

        isTimeout = false
        handlerTimeout = Handler(Looper.getMainLooper())
        val rdOut = Runnable {
            adCallback.onNextAction()
            AppOpenManager.isShowingAd = false
            isTimeout = true
            Log.i(TAG, "loadOpenAppAdSplashList: time out has been reached")
        }
        rdTimeout = rdOut
        handlerTimeout?.postDelayed(rdOut, timeOut)

        loadNextOpenSplash(activity, list, 0, adCallback)
    }
}

internal fun AppOpenManager.loadNextOpenSplash(
    activity: Activity,
    listId: List<String>,
    index: Int,
    adCallback: AdCallback
) {
    if (index >= listId.size) {
        Log.e(TAG, "loadOpenAppAdSplashList: All ad IDs failed, list_size: ${listId.size}")
        val h = handlerTimeout
        val r = rdTimeout
        if (h != null && r != null) {
            h.removeCallbacks(r)
        }
        adCallback.onNextAction()
        return
    }
    AppOpenAd.load(getAdRequest(listId[index]), object : AdLoadCallback<AppOpenAd> {
        override fun onAdLoaded(ad: AppOpenAd) {
            splashAd = ad
            if (isTimeout || isTimeDelay) {
                adCallback.onAdSplashReady()
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            loadNextOpenSplash(activity, listId, index + 1, adCallback)
        }
    })
}

fun AppOpenManager.showAppOpenSplash(activity: Activity, adCallback: AdCallback) {
    val ad = splashAd
    if (ad == null) {
        Log.d(TAG, "showAppOpenSplash: App Open Splash wasn't ready yet")
        adCallback.onNextAction()
        return
    }
    showDialogLoadingSplash(activity as Context)
    val responseInfo = ad.getResponseInfo()
    Handler(Looper.getMainLooper()).postDelayed({
        ad.adEventCallback =(object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                splashAd = null
                AppOpenManager.isShowingAd = false
                hideDialogLoadingSplash()
                adCallback.onAdClosed()
                adCallback.onNextAction()
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                AppOpenManager.isShowingAd = false
                hideDialogLoadingSplash()
                adCallback.onAdFailedToShow(error)
                adCallback.onNextAction()
            }

            override fun onAdShowedFullScreenContent() {
                AppOpenManager.isShowingAd = true
            }

            override fun onAdClicked() {
                adCallback.onAdClicked()
            }

            override fun onAdImpression() {
                adCallback.onAdImpression()
            }
        })
        ad.show(activity)
    }, 800L)
}

fun AppOpenManager.onCheckShowAppOpenSplashWhenFail(activity: Activity, callback: AdCallback, timeDelay: Int) {
    Handler(activity.mainLooper).postDelayed({
        if (splashAd != null && !AppOpenManager.isShowingAd) {
            showAppOpenSplash(activity, object : AdCallback() {
                override fun onNextAction() {
                    callback.onNextAction()
                }

                override fun onAdClosed() {
                    callback.onAdClosed()
                }

                override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                    callback.onAdFailedToShow(fullScreenContentError)
                }
            })
        }
    }, timeDelay.toLong())
}
