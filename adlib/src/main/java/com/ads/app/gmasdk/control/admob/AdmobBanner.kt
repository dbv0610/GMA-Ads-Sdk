package com.ads.app.gmasdk.control.admob

import ads_mobile_sdk.ad
import android.app.Activity
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo

private const val TAG = "AzAdmob"

fun Admob.loadBanner(mActivity: Activity, id: String) {
    val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(mActivity, id, adContainer, containerShimmer, null, false)
}

fun Admob.loadBanner(mActivity: Activity, id: String, callback: AdCallback?) {
    val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(mActivity, id, adContainer, containerShimmer, callback, false)
}

fun Admob.loadInlineBanner(activity: Activity, id: String) {
    val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(activity, id, adContainer, containerShimmer, null, true)
}

fun Admob.loadInlineBanner(activity: Activity, id: String, callback: AdCallback?) {
    val adContainer = activity.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(activity, id, adContainer, containerShimmer, callback, true)
}

fun Admob.loadCollapsibleBanner(mActivity: Activity, id: String, gravity: String, callback: AdCallback?) {
    val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback)
}

fun Admob.loadBanner(
    mActivity: Activity,
    id: String,
    adContainer: FrameLayout,
    containerShimmer: ShimmerFrameLayout,
    callback: AdCallback?,
    useInlineAdaptive: Boolean
) {
    val testIds = mActivity.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(mActivity, Admob.BANNER_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        containerShimmer.visibility = View.GONE
        return
    }
    containerShimmer.visibility = View.VISIBLE
    containerShimmer.startShimmer()
    try {
        val adView = AdView(mActivity)
        adContainer.addView(adView)
        val adSize = getAdSize(mActivity, useInlineAdaptive)
        val adHeight = adSize.height
        containerShimmer.layoutParams.height =
            (adHeight * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()
        val adRequest = BannerAdRequest.Builder(id, adSize).build()
        adView.setLayerType(View.LAYER_TYPE_HARDWARE, null as Paint?)
        adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                super.onAdLoaded(ad)
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                adContainer.visibility = View.VISIBLE
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                adContainer.visibility = View.GONE
                callback?.onAdFailedToLoad(adError)
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Admob.populateUnifiedBannerAdView(adView: AdView, adContainer: FrameLayout) {
    try {
        adContainer.addView(adView)
        adContainer.visibility = View.VISIBLE
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Admob.requestLoadBanner(
    mActivity: Activity,
    id: String,
    collapsibleGravity: String?,
    callback: AdCallback,
    useInlineAdaptive: Boolean
) {
    val testIds = mActivity.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(mActivity, Admob.BANNER_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        callback.onAdFailedToLoad(LoadAdError(LoadAdError.ErrorCode.CANCELLED, "App isPurchased", null as ResponseInfo?))
        return
    }
    try {
        val adView = AdView(mActivity)
        val adSize = getAdSize(mActivity, useInlineAdaptive)
        adView.setLayerType(View.LAYER_TYPE_HARDWARE, null as Paint?)
        val adRequest = getAdRequestForCollapsibleBanner(id, adSize, collapsibleGravity)
        adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                super.onAdLoaded(ad)
                callback.onAdLoaded()
                callback.onBannerLoaded(adView)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                callback.onAdFailedToLoad(adError)
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Admob.requestLoadBanner(
    mActivity: Activity,
    id: String,
    collapsibleGravity: String?,
    useInlineAdaptive: Boolean,
    maxHeight: Int,
    callback: AdCallback
) {
    val testIds = mActivity.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(mActivity, Admob.BANNER_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        callback.onAdFailedToLoad(LoadAdError(LoadAdError.ErrorCode.CANCELLED, "App isPurchased", null as ResponseInfo?))
        return
    }
    try {
        val adView = AdView(mActivity)
        val adSize = getAdSize(mActivity, useInlineAdaptive, maxHeight)
        adView.setLayerType(View.LAYER_TYPE_HARDWARE, null as Paint?)
        val adRequest = getAdRequestForCollapsibleBanner(id, adSize, collapsibleGravity)
        adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                callback.onAdLoaded()
                callback.onBannerLoaded(adView)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                callback.onAdFailedToLoad(adError)
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Admob.loadCollapsibleBanner(
    mActivity: Activity,
    id: String,
    gravity: String,
    adContainer: FrameLayout,
    containerShimmer: ShimmerFrameLayout,
    callback: AdCallback?
) {
    val testIds = mActivity.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(mActivity, Admob.BANNER_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        containerShimmer.visibility = View.GONE
        return
    }
    containerShimmer.visibility = View.VISIBLE
    containerShimmer.startShimmer()
    try {
        val adView = AdView(mActivity)
        adContainer.addView(adView)
        val adSize = getAdSize(mActivity, false)
        containerShimmer.layoutParams.height =
            (adSize.height * android.content.res.Resources.getSystem().displayMetrics.density + 0.5f).toInt()
        adView.setLayerType(View.LAYER_TYPE_HARDWARE, null as Paint?)
        val adRequest = getAdRequestForCollapsibleBanner(id, adSize, gravity)
        adView.loadAd(adRequest, object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                adContainer.visibility = View.VISIBLE
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                containerShimmer.stopShimmer()
                containerShimmer.visibility = View.GONE
                adContainer.visibility = View.GONE
                callback?.onAdFailedToLoad(adError)
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getAdSize(mActivity: Activity, useInlineAdaptive: Boolean, maxHeight: Int): AdSize {
    val adWidth = getAdWidthDp(mActivity)
    Log.e(TAG, "getAdSize: $useInlineAdaptive : $maxHeight")
    return if (useInlineAdaptive) {
        AdSize.getInlineAdaptiveBannerAdSize(adWidth, maxHeight)
    } else {
        AdSize.getPortraitInlineAdaptiveBannerAdSize(mActivity, adWidth)
    }
}

fun getAdSize(mActivity: Activity, useInlineAdaptive: Boolean): AdSize {
    val adWidth = getAdWidthDp(mActivity)
    return if (useInlineAdaptive) {
        AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, adWidth)
    } else {
        AdSize.getLargePortraitAnchoredAdaptiveBannerAdSize(mActivity, adWidth)
    }

}

internal fun getAdWidthDp(activity: Activity): Int {
    val widthPixels: Float = if (Build.VERSION.SDK_INT >= 30) {
        activity.windowManager.currentWindowMetrics.bounds.width().toFloat()
    } else {
        val outMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(outMetrics)
        outMetrics.widthPixels.toFloat()
    }
    val density = activity.resources.displayMetrics.density
    return (widthPixels / density).toInt()
}

internal fun getAdRequestForCollapsibleBanner(
    adUnitId: String,
    adSize: AdSize,
    gravity: String?
): BannerAdRequest {
    val request = BannerAdRequest.Builder(adUnitId, adSize)
    if (!gravity.isNullOrEmpty()) {
        val extras = Bundle()
        extras.putString("collapsible", gravity)
        request.setGoogleExtrasBundle(extras)
    }
    return request.build()
}
