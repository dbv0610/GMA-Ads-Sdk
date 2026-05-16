package com.ads.app.gmasdk.control.admob

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.ads.app.gmasdk.R
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

fun Admob.populateUnifiedNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    adView.advertiserView =(adView.findViewById(R.id.ad_advertiser))
    adView.bodyView =(adView.findViewById(R.id.ad_body))
    adView.callToActionView =(adView.findViewById(R.id.ad_call_to_action))
    adView.headlineView =(adView.findViewById(R.id.ad_headline))
    adView.iconView =(adView.findViewById(R.id.ad_app_icon))
    adView.priceView =(adView.findViewById(R.id.ad_price))
    adView.starRatingView =(adView.findViewById(R.id.ad_stars))

    (adView.headlineView as? TextView)?.text = nativeAd.headline
    (adView.bodyView as? TextView)?.text = nativeAd.body
    (adView.callToActionView as? TextView)?.text = nativeAd.callToAction
    (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
    (adView.priceView as? TextView)?.text = nativeAd.price
    (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)

    val starRating = nativeAd.starRating
    if (starRating != null) {
        (adView.starRatingView as? RatingBar)?.rating = starRating.toFloat()
    }

    adView.advertiserView?.visibility = getAssetViewVisibility(nativeAd.advertiser)
    adView.bodyView?.visibility = getAssetViewVisibility(nativeAd.body)
    adView.callToActionView?.visibility = getAssetViewVisibility(nativeAd.callToAction)
    adView.headlineView?.visibility = getAssetViewVisibility(nativeAd.headline)
    adView.iconView?.visibility = getAssetViewVisibility(nativeAd.icon)
    adView.priceView?.visibility = getAssetViewVisibility(nativeAd.price)
    adView.starRatingView?.visibility = getAssetViewVisibility(nativeAd.starRating)

    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    adView.registerNativeAd(nativeAd, mediaView)
}

private fun Admob.getAssetViewVisibility(asset: Any?): Int =
    if (asset == null) View.INVISIBLE else View.VISIBLE
