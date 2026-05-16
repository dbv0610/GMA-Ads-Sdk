package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout

fun Admob.loadBannerFragment(mActivity: Activity, id: String, rootView: View) {
    val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(mActivity, id, adContainer, containerShimmer, null, false)
}

fun Admob.loadBannerFragment(mActivity: Activity, id: String, rootView: View, callback: AdCallback?) {
    val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(mActivity, id, adContainer, containerShimmer, callback, false)
}

fun Admob.loadInlineBannerFragment(activity: Activity, id: String, rootView: View) {
    val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(activity, id, adContainer, containerShimmer, null, true)
}

fun Admob.loadInlineBannerFragment(activity: Activity, id: String, rootView: View, callback: AdCallback?) {
    val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadBanner(activity, id, adContainer, containerShimmer, callback, true)
}

fun Admob.loadCollapsibleBannerFragment(mActivity: Activity, id: String, rootView: View, gravity: String, callback: AdCallback?) {
    val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)!!
    val containerShimmer = rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)!!
    loadCollapsibleBanner(mActivity, id, gravity, adContainer, containerShimmer, callback)
}
