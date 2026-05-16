package com.ads.app.gmasdk.control.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ads.app.gmasdk.control.funtion.AdType
import com.ads.app.gmasdk.control.util.SharePreferenceUtils
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import java.util.Calendar
import java.util.TimeZone

object AzTaichi {
    private const val TAG = "AzTaichi"
    private const val EVENT_DAILY_ADS_REVENUE = "Daily_Ads_Revenue"
    private const val DEFAULT_REVENUE_THRESHOLD_USD = 0.01
    private const val DEFAULT_DAY2_IMPRESSION_THRESHOLD = 10

    private var enableTaichi = true
    private var revenueThresholdUsd = DEFAULT_REVENUE_THRESHOLD_USD
    private var day2ImpressionThreshold = DEFAULT_DAY2_IMPRESSION_THRESHOLD

    @JvmStatic
    @JvmOverloads
    fun configure(
        enableTaichi: Boolean,
        day2ImpressionThreshold: Int = DEFAULT_DAY2_IMPRESSION_THRESHOLD,
        revenueThresholdUsd: Double = DEFAULT_REVENUE_THRESHOLD_USD
    ) {
        AzTaichi.enableTaichi = enableTaichi
        AzTaichi.day2ImpressionThreshold = day2ImpressionThreshold
        AzTaichi.revenueThresholdUsd = revenueThresholdUsd
    }

    @JvmStatic
    fun isEnabled() = enableTaichi

    @JvmStatic
    fun onTrackingPaidImpression(context: Context, adValue: AdValue, adFormat: AdType) {
        if (!enableTaichi) return
        val revenueUsd = adValue.valueMicros / 1_000_000.0
        FirebaseAnalyticsUtil.logEventTracking(context, "azura_paid_ad_impression", Bundle().apply {
            putString("ad_platform", "Admob")
            putString("ad_format", adFormat.toString())
            putString("currency", "USD")
            putDouble("value", revenueUsd)
        })
    }

    @JvmStatic
    fun onTrackingDailyRevenue(context: Context, adValue: AdValue, adFormat: AdType) {
        if (!enableTaichi) return
        if (SharePreferenceUtils.isDailyAdsRevenueLogged(context)) {
            Log.d(TAG, "Daily_Ads_Revenue event already logged, skipping")
            return
        }
        val revenueUsd = adValue.valueMicros / 1_000_000.0
        val currentDayTimestamp = getLocalDayTimestamp()
        var firstImpressionDay = SharePreferenceUtils.getFirstImpressionDay(context)
        if (firstImpressionDay == 0L) {
            SharePreferenceUtils.setFirstImpressionDay(context, currentDayTimestamp)
            firstImpressionDay = currentDayTimestamp
            Log.d(TAG, "First impression day set: $firstImpressionDay")
        }
        val dayNumber = getDayNumber(firstImpressionDay, currentDayTimestamp)
        Log.d(TAG, "Current day number: $dayNumber, revenue: $revenueUsd USD, format: $adFormat")
        when (dayNumber) {
            1 -> handleDay1(context, revenueUsd, adFormat)
            2 -> handleDay2(context, revenueUsd, adFormat)
        }
    }

    private fun handleDay1(context: Context, revenueUsd: Double, adFormat: AdType) {
        SharePreferenceUtils.addDay1Revenue(context, revenueUsd)
        val totalRevenue = SharePreferenceUtils.getDay1Revenue(context)
        Log.d(TAG, "Day 1 - Total accumulated revenue: $totalRevenue USD")
        if (totalRevenue > revenueThresholdUsd) logDailyAdsRevenueEvent(context, totalRevenue, adFormat)
    }

    private fun handleDay2(context: Context, revenueUsd: Double, adFormat: AdType) {
        val impressionCount = SharePreferenceUtils.incrementDay2Impressions(context)
        val day1Revenue = SharePreferenceUtils.getDay1Revenue(context)
        Log.d(TAG, "Day 2+ - Impression count: $impressionCount, Day 1 revenue: $day1Revenue USD")
        if (impressionCount >= day2ImpressionThreshold) {
            logDailyAdsRevenueEvent(context, day1Revenue + revenueUsd, adFormat)
        }
    }

    private fun logDailyAdsRevenueEvent(context: Context, totalRevenueUsd: Double, adFormat: AdType) {
        if (SharePreferenceUtils.isDailyAdsRevenueLogged(context)) {
            Log.d(TAG, "Event already logged (race condition prevented)")
            return
        }
        SharePreferenceUtils.setDailyAdsRevenueLogged(context)
        FirebaseAnalyticsUtil.logEventTracking(context, EVENT_DAILY_ADS_REVENUE, Bundle().apply {
            putString("ad_platform", "Admob")
            putString("ad_format", adFormat.toString())
            putString("currency", "USD")
            putDouble("value", totalRevenueUsd)
        })
        Log.d(TAG, "Daily_Ads_Revenue event logged, value: $totalRevenueUsd USD")
    }

    private fun getLocalDayTimestamp(): Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getDayNumber(firstDayTimestamp: Long, currentDayTimestamp: Long): Int =
        ((currentDayTimestamp - firstDayTimestamp) / 86_400_000L).toInt() + 1
}
