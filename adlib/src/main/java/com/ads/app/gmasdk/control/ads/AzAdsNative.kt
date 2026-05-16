package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.loadNativeAd
import com.ads.app.gmasdk.control.admob.populateUnifiedNativeAdView
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.util.AppLogger
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import java.util.concurrent.atomic.AtomicBoolean

fun AzAds.loadNativeList(context: Context, listId: List<String>?, layoutCustomNative: Int, adCallback: AzAdCallback) {
    whenAdsReady {
        Log.e("AzAds", "loadNativeList size: ${listId?.size}")
        if (listId.isNullOrEmpty()) {
            adCallback.onAdFailedToLoad(ApAdError("list id is null or empty"))
        } else {
            val appCtx = context.applicationContext
            val done = AtomicBoolean(false)
            AppLogger.d("AzAds", "loadNativeList start id=${listId[0]}")
            loadNativeNextAd(appCtx, listId, 0, layoutCustomNative, adCallback, done)
        }
    }
}

fun AzAds.loadNativeNextAd(context: Context, listId: List<String>, pos: Int, layoutCustomNative: Int, finalCallback: AzAdCallback, done: AtomicBoolean) {
    val adUnit = listId[pos]
    loadNativeAdResultCallback(context, adUnit, layoutCustomNative, object : AzAdCallback() {
        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
            if (!done.get()) {
                done.set(true)
                AppLogger.d("AzAds", "loadNativeList success id=$adUnit")
                finalCallback.onNativeAdLoaded(nativeAd)
            }
        }
        override fun onAdFailedToLoad(adError: ApAdError?) {
            AppLogger.e("AzAds", "loadNativeList onAdFailedToLoad id=$adUnit")
            if (pos < listId.size - 1) {
                loadNativeNextAd(context, listId, pos + 1, layoutCustomNative, finalCallback, done)
            } else {
                finalCallback.onAdFailedToLoad(adError)
            }
        }
    })
}

fun AzAds.loadNativeListTimeOut(context: Context, listId: List<String>?, timeOutPerId: List<Long>?, layoutCustomNative: Int, adCallback: AzAdCallback) {
    whenAdsReady {
        if (listId.isNullOrEmpty()) {
            adCallback.onAdFailedToLoad(ApAdError("list id is null or empty"))
        } else {
            val safeIds = ArrayList(listId)
            val appCtx = context.applicationContext
            val done = AtomicBoolean(false)
            if (timeOutPerId != null && timeOutPerId.size != safeIds.size) {
                AppLogger.w("AzAds", "loadNativeListTimeOut timeOut size != listId size. Will apply timeOut[0] for overflow indices.")
            }
            AppLogger.d("AzAds", "loadNativeListTimeOut start id=${safeIds[0]}")
            loadNativeNextAdWithTimeout(appCtx, safeIds, timeOutPerId, 0, layoutCustomNative, adCallback, done)
        }
    }
}

fun AzAds.loadNativeNextAdWithTimeout(context: Context, listId: List<String>, timeOutPerId: List<Long>?, pos: Int, layoutCustomNative: Int, finalCallback: AzAdCallback, done: AtomicBoolean) {
    if (done.get()) return
    val adUnit = listId[pos]
    val perAttemptTimeout = resolveTimeoutMs(timeOutPerId, pos)
    val startMs = SystemClock.uptimeMillis()
    val abandoned = AtomicBoolean(false)
    val timeoutTask = Runnable {
        if (!done.get() && !abandoned.get()) {
            abandoned.set(true)
            val elapsed = SystemClock.uptimeMillis() - startMs
            AppLogger.e("AzAds", "loadNativeListTimeOut TIMEOUT id=$adUnit after ${elapsed}ms (limit=${perAttemptTimeout}ms)")
            val next = pos + 1
            if (next < listId.size) {
                AppLogger.d("AzAds", "loadNativeListTimeOut try next (timeout) id=${listId[next]}")
                loadNativeNextAdWithTimeout(context, listId, timeOutPerId, next, layoutCustomNative, finalCallback, done)
            } else {
                if (!done.getAndSet(true)) {
                    finalCallback.onAdFailedToLoad(ApAdError("timeout on last id after ${elapsed}ms"))
                }
            }
        }
    }
    AzAds.MAIN.postDelayed(timeoutTask, perAttemptTimeout)
    loadNativeAdResultCallback(context, adUnit, layoutCustomNative, object : AzAdCallback() {
        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
            if (done.get() || abandoned.get()) return
            AzAds.MAIN.removeCallbacks(timeoutTask)
            if (done.getAndSet(true)) return
            AppLogger.d("AzAds", "loadNativeListTimeOut success id=$adUnit after ${SystemClock.uptimeMillis() - startMs}ms")
            finalCallback.onNativeAdLoaded(nativeAd)
        }
        override fun onAdFailedToLoad(adError: ApAdError?) {
            if (done.get() || abandoned.get()) return
            AzAds.MAIN.removeCallbacks(timeoutTask)
            AppLogger.e("AzAds", "loadNativeListTimeOut FAIL id=$adUnit")
            val next = pos + 1
            if (next < listId.size) {
                loadNativeNextAdWithTimeout(context, listId, timeOutPerId, next, layoutCustomNative, finalCallback, done)
            } else {
                if (!done.getAndSet(true)) {
                    finalCallback.onAdFailedToLoad(adError)
                }
            }
        }
    })
}

fun resolveTimeoutMs(timeOutPerId: List<Long>?, pos: Int): Long {
    if (timeOutPerId.isNullOrEmpty()) return 5000L
    val t = if (pos < timeOutPerId.size) timeOutPerId[pos] else timeOutPerId[0]
    return if (t > 0L) t else 5000L
}

fun AzAds.loadNativeAd(activity: Activity, id: String, layoutCustomNative: Int, adPlaceHolder: FrameLayout, containerShimmerLoading: ShimmerFrameLayout, callback: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd)
                callback.onNativeAdLoaded(apNativeAd)
                populateNativeAdView(activity, apNativeAd, adPlaceHolder, containerShimmerLoading)
            }
            override fun onAdImpression() { super.onAdImpression(); callback.onAdImpression() }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                callback.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                super.onAdFailedToShow(fullScreenContentError)
                callback.onAdFailedToShow(ApAdError(fullScreenContentError))
            }
            override fun onAdClicked() { super.onAdClicked(); callback.onAdClicked() }
        })
    }
}

fun AzAds.loadNativeAdResultCallback(context: Context, id: String, layoutCustomNative: Int, callback: AzAdCallback) {
    whenAdsReady {
        val appCtx = context.applicationContext
        Admob.getInstance().loadNativeAd(appCtx, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                callback.onNativeAdLoaded(ApNativeAd(layoutCustomNative, unifiedNativeAd))
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                callback.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                super.onAdFailedToShow(fullScreenContentError)
                callback.onAdFailedToShow(ApAdError(fullScreenContentError))
            }
            override fun onAdClicked() { super.onAdClicked(); callback.onAdClicked() }
            override fun onAdImpression() { super.onAdImpression(); callback.onAdImpression() }
        })
    }
}

fun AzAds.loadNativeAdResultCallback(activity: Activity, id: String, layoutCustomNative: Int, callback: AzAdCallback, maxNumberOfAds: Int) {
    whenAdsReady {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                callback.onNativeAdLoaded(ApNativeAd(layoutCustomNative, unifiedNativeAd))
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                callback.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                super.onAdFailedToShow(fullScreenContentError)
                callback.onAdFailedToShow(ApAdError(fullScreenContentError))
            }
            override fun onAdClicked() { super.onAdClicked(); callback.onAdClicked() }
            override fun onAdImpression() { super.onAdImpression(); callback.onAdImpression() }
        }, maxNumberOfAds)
    }
}

fun AzAds.populateNativeAdView(activity: Activity, apNativeAd: ApNativeAd, adPlaceHolder: FrameLayout, containerShimmerLoading: ShimmerFrameLayout?) {
    if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
        containerShimmerLoading?.visibility = android.view.View.GONE
        Log.e("AzAds", "populateNativeAdView failed : native is not loaded ")
        return
    }
    val adView = LayoutInflater.from(activity).inflate(apNativeAd.layoutCustomNative, null) as NativeAdView
    containerShimmerLoading?.let {
        it.stopShimmer()
        it.visibility = android.view.View.GONE
    }
    adPlaceHolder.visibility = android.view.View.VISIBLE
    apNativeAd.admobNativeAd?.let { Admob.getInstance().populateUnifiedNativeAdView(it, adView) }
    adPlaceHolder.removeAllViews()
    adPlaceHolder.addView(adView)
}
