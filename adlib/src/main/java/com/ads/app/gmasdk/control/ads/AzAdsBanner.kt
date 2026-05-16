package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.ads.app.gmasdk.control.ads.wrapper.ApAdError
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.loadBanner
import com.ads.app.gmasdk.control.admob.loadBannerFragment
import com.ads.app.gmasdk.control.admob.loadCollapsibleBanner
import com.ads.app.gmasdk.control.admob.loadCollapsibleBannerFragment
import com.ads.app.gmasdk.control.admob.populateUnifiedBannerAdView
import com.ads.app.gmasdk.control.admob.requestLoadBanner
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo

fun AzAds.loadBanner(mActivity: Activity, id: String) {
    whenAdsReady { Admob.getInstance().loadBanner(mActivity, id) }
}

fun AzAds.loadBanner(mActivity: Activity, id: String, adCallback: AzAdCallback) {
    whenAdsReady {
        Admob.getInstance().loadBanner(mActivity, id, object : AdCallback() {
            override fun onAdLoaded() { super.onAdLoaded(); adCallback.onAdLoaded() }
            override fun onAdClicked() { super.onAdClicked(); adCallback.onAdClicked() }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                adCallback.onAdFailedToLoad(ApAdError(loadAdError))
            }
            override fun onAdImpression() { super.onAdImpression(); adCallback.onAdImpression() }
        })
    }
}

fun AzAds.loadCollapsibleBanner(activity: Activity, id: String, gravity: String, adCallback: AdCallback) {
    whenAdsReady { Admob.getInstance().loadCollapsibleBanner(activity, id, gravity, adCallback) }
}

fun AzAds.loadBannerFragment(mActivity: Activity, id: String, rootView: View) {
    whenAdsReady { Admob.getInstance().loadBannerFragment(mActivity, id, rootView) }
}

fun AzAds.loadBannerFragment(mActivity: Activity, id: String, rootView: View, adCallback: AdCallback) {
    whenAdsReady { Admob.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback) }
}

fun AzAds.requestLoadBanner(activity: Activity, idBannerAd: String, collapsibleGravity: String?, adCallback: AdCallback) {
    whenAdsReady { Admob.getInstance().requestLoadBanner(activity, idBannerAd, collapsibleGravity, adCallback, false) }
}

fun AzAds.requestLoadBanner(activity: Activity, idBannerAd: String, collapsibleGravity: String?, useInlineAdaptive: Boolean, maxHeight: Int, adCallback: AdCallback) {
    whenAdsReady { Admob.getInstance().requestLoadBanner(activity, idBannerAd, collapsibleGravity, useInlineAdaptive, maxHeight, adCallback) }
}

fun AzAds.loadBannerList(activity: Activity, listId: List<String>?, collapsibleGravity: String?, adCallback: AdCallback) {
    whenAdsReady {
        if (listId.isNullOrEmpty()) {
            adCallback.onAdFailedToLoad(LoadAdError(LoadAdError.ErrorCode.CANCELLED, "list id is null or empty", null as ResponseInfo?))
        } else {
            val index = intArrayOf(0)
            val callback = object : AdCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AppLogger.e("AzAds", "onAdFailedToLoad id=${listId[index[0]]} msg=${loadAdError.message}")
                    if (activity.isFinishing || activity.isDestroyed) {
                        adCallback.onAdFailedToLoad(loadAdError); return
                    }
                    if (index[0] < listId.size - 1) {
                        index[0]++
                        AppLogger.d("AzAds", "loadBannerList index=${index[0]} id=${listId[index[0]]}")
                        Admob.getInstance().requestLoadBanner(activity, listId[index[0]], collapsibleGravity, this, false)
                    } else {
                        adCallback.onAdFailedToLoad(loadAdError)
                    }
                }
                override fun onBannerLoaded(adView: AdView) { super.onBannerLoaded(adView); adCallback.onBannerLoaded(adView) }
                override fun onAdClicked() { super.onAdClicked(); adCallback.onAdClicked() }
                override fun onAdImpression() { super.onAdImpression(); adCallback.onAdImpression() }
            }
            AppLogger.d("AzAds", "loadBannerList index=0 id=${listId[0]}")
            Admob.getInstance().requestLoadBanner(activity, listId[index[0]], collapsibleGravity, callback, false)
        }
    }
}

fun AzAds.loadBannerList(activity: Activity, listId: List<String>?, collapsibleGravity: String?, useInlineAdaptive: Boolean, maxHeight: Int, adCallback: AdCallback) {
    whenAdsReady {
        if (listId.isNullOrEmpty()) {
            adCallback.onAdFailedToLoad(LoadAdError(LoadAdError.ErrorCode.CANCELLED, "list id is null or empty", null as ResponseInfo?))
        } else {
            val index = intArrayOf(0)
            val callback = object : AdCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    AppLogger.e("AzAds", "onAdFailedToLoad id=${listId[index[0]]} msg=${loadAdError.message}")
                    if (activity.isFinishing || activity.isDestroyed) {
                        adCallback.onAdFailedToLoad(loadAdError); return
                    }
                    if (index[0] < listId.size - 1) {
                        index[0]++
                        AppLogger.d("AzAds", "loadBannerList index=${index[0]} id=${listId[index[0]]}")
                        Admob.getInstance().requestLoadBanner(activity, listId[index[0]], collapsibleGravity, useInlineAdaptive, maxHeight, this)
                    } else {
                        adCallback.onAdFailedToLoad(loadAdError)
                    }
                }
                override fun onBannerLoaded(adView: AdView) { super.onBannerLoaded(adView); adCallback.onBannerLoaded(adView) }
                override fun onAdClicked() { super.onAdClicked(); adCallback.onAdClicked() }
                override fun onAdImpression() { super.onAdImpression(); adCallback.onAdImpression() }
            }
            AppLogger.d("AzAds", "loadBannerList index=0 id=${listId[0]}")
            Admob.getInstance().requestLoadBanner(activity, listId[index[0]], collapsibleGravity, useInlineAdaptive, maxHeight, callback)
        }
    }
}

fun AzAds.populateUnifiedBannerAdView(adView: AdView, adContainer: FrameLayout) {
    Admob.getInstance().populateUnifiedBannerAdView(adView, adContainer)
}

fun AzAds.loadCollapsibleBannerFragment(mActivity: Activity, id: String, rootView: View, gravity: String, adCallback: AdCallback) {
    whenAdsReady { Admob.getInstance().loadCollapsibleBannerFragment(mActivity, id, rootView, gravity, adCallback) }
}
