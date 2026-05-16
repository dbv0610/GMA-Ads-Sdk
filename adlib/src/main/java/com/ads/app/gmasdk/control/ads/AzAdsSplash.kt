package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.content.Context
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.loadSplashInterstitialAds
import com.ads.app.gmasdk.control.admob.loadSplashListAds
import com.ads.app.gmasdk.control.admob.onCheckShowSplashWhenFail
import com.ads.app.gmasdk.control.admob.onShowSplash
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

fun AzAds.loadSplashInterstitialAds(context: Context, id: String, timeOut: Long, timeDelay: Long, adListener: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, object : AdCallback() {
            override fun onAdClosed() { super.onAdClosed(); adListener.onAdClosed() }
            override fun onNextAction() { super.onNextAction(); adListener.onNextAction() }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                adListener.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
                super.onAdFailedToShow(fullScreenContentError)
                adListener.onAdFailedToShow(ApAdError(fullScreenContentError))
            }
            override fun onAdLoaded() { super.onAdLoaded(); adListener.onAdLoaded() }
            override fun onAdSplashReady() { super.onAdSplashReady(); adListener.onAdSplashReady() }
            override fun onAdClicked() { super.onAdClicked(); adListener.onAdClicked() }
        })
    }
}

fun AzAds.loadSplashListAds(context: Context, listId: List<String>, timeOut: Long, timeDelay: Long, adListener: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().loadSplashListAds(context, listId, timeOut, timeDelay, adListener)
    }
}

fun AzAds.onShowSplash(activity: Activity, adListener: AzAdCallback) {
    Admob.getInstance().onShowSplash(activity, object : AdCallback() {
        override fun onAdClosed() { super.onAdClosed(); adListener.onAdClosed() }
        override fun onNextAction() { super.onNextAction(); adListener.onNextAction() }
        override fun onAdFailedToShow(fullScreenContentError: FullScreenContentError) {
            super.onAdFailedToShow(fullScreenContentError)
            adListener.onAdFailedToShow(ApAdError(fullScreenContentError))
        }
        override fun onAdClicked() { super.onAdClicked(); adListener.onAdClicked() }
        override fun onAdLoaded() { super.onAdLoaded(); adListener.onAdLoaded() }
        override fun onAdSplashReady() { super.onAdSplashReady(); adListener.onAdSplashReady() }
        override fun onAdImpression() { super.onAdImpression(); adListener.onAdImpression() }
    })
}

fun AzAds.onCheckShowSplashWhenFail(activity: Activity, callback: AzAdCallback, timeDelay: Int) {
    Admob.getInstance().onCheckShowSplashWhenFail(activity, object : AdCallback() {
        override fun onNextAction() { super.onNextAction(); callback.onNextAction() }
        override fun onAdClosed() { super.onAdClosed(); callback.onAdClosed() }
        override fun onAdLoaded() { super.onAdLoaded(); callback.onAdLoaded() }
        override fun onAdSplashReady() { super.onAdSplashReady(); callback.onAdSplashReady() }
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            super.onAdFailedToLoad(loadAdError)
            callback.onAdFailedToLoad(ApAdError(loadAdError))
        }
    }, timeDelay)
}
