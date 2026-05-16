package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions

private const val TAG = "AzAdmob"

fun Admob.loadNative(mActivity: Activity, id: String) {
    val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)!!
    val containerShimmer = mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)!!
    loadNative(mActivity as Context, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size)
}

fun Admob.loadNativeFragment(mActivity: Activity, id: String, parent: View) {
    val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)!!
    val containerShimmer = parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)!!
    loadNative(mActivity as Context, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size)
}

fun Admob.loadSmallNative(mActivity: Activity, adUnitId: String) {
    val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)!!
    val containerShimmer = mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)!!
    loadNative(mActivity as Context, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admod_medium)
}

fun Admob.loadSmallNativeFragment(mActivity: Activity, adUnitId: String, parent: View) {
    val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)!!
    val containerShimmer = parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)!!
    loadNative(mActivity as Context, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admod_medium)
}

fun Admob.loadNativeAd(context: Context, id: String, callback: AdCallback) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.NATIVE_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        return
    }
    val appCtx = context.applicationContext
    val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
    val adRequest = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
        .setVideoOptions(videoOptions)
        .build()
    NativeAdLoader.load(adRequest, object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            callback.onUnifiedNativeAdLoaded(nativeAd)
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            callback.onAdFailedToLoad(loadAdError)
        }
    })
}

fun Admob.loadNativeAd(context: Context, id: String, callback: AdCallback, maxNumberOfAds: Int) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.NATIVE_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        return
    }
    val appCtx = context.applicationContext
    val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
    val adRequest = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
        .setVideoOptions(videoOptions)
        .build()
    NativeAdLoader.load(adRequest, maxNumberOfAds, object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            callback.onUnifiedNativeAdLoaded(nativeAd)
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            callback.onAdFailedToLoad(loadAdError)
        }
    })
}

fun Admob.loadNativeFullScreen(context: Context, id: String, callback: AdCallback) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.NATIVE_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        return
    }
    val appCtx = context.applicationContext
    val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
    val adRequest = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
        .setMediaAspectRatio(NativeAd.NativeMediaAspectRatio.PORTRAIT)
        .setVideoOptions(videoOptions)
        .build()
    NativeAdLoader.load(adRequest, object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            callback.onUnifiedNativeAdLoaded(nativeAd)
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            callback.onAdFailedToLoad(loadAdError)
        }
    })
}

internal fun Admob.loadNative(
    context: Context,
    containerShimmer: ShimmerFrameLayout,
    frameLayout: FrameLayout,
    id: String,
    layout: Int
) {
    val testIds = context.resources.getStringArray(R.array.list_id_test).toList()
    if (testIds.contains(id)) {
        showTestIdAlert(context, Admob.NATIVE_ADS, id)
    }
    if (AppPurchase.getInstance().isPurchased()) {
        containerShimmer.visibility = View.GONE
        return
    }
    val appCtx = context.applicationContext
    frameLayout.removeAllViews()
    frameLayout.visibility = View.GONE
    containerShimmer.visibility = View.VISIBLE
    containerShimmer.startShimmer()
    val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
    val adRequest = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
        .setVideoOptions(videoOptions)
        .build()
    NativeAdLoader.load(adRequest, object : NativeAdLoaderCallback {
        override fun onNativeAdLoaded(nativeAd: NativeAd) {
            containerShimmer.stopShimmer()
            containerShimmer.visibility = View.GONE
            frameLayout.visibility = View.VISIBLE
            try {
                val adView = LayoutInflater.from(context).inflate(layout, null) as NativeAdView
                frameLayout.removeAllViews()
                frameLayout.addView(adView)
                populateUnifiedNativeAdView(nativeAd, adView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            containerShimmer.stopShimmer()
            containerShimmer.visibility = View.GONE
            frameLayout.visibility = View.GONE
        }
    })
}
