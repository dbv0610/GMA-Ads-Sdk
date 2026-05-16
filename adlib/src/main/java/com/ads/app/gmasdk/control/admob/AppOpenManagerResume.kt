package com.ads.app.gmasdk.control.admob

import ads_mobile_sdk.nu
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

private const val TAG = "AppOpenManager"

fun loadAppOpenResume(manager: AppOpenManager) {
    AzAds.getInstance().runWhenReady {
        if (manager.isEnableList) {
            loadAppOpenResumeList(manager)
            return@runWhenReady
        }
        Log.d(TAG, "loadAppOpenResume: ")
        if (manager.isAdAvailable(false)) return@runWhenReady
        if (AppOpenManager.isShowingAd) {
            Log.d(TAG, "isShowingAd: ")
            return@runWhenReady
        }
        if (manager.isLoadingAppResume) {
            Log.d(TAG, "isLoadingAppResume: ")
            return@runWhenReady
        }
        val adId = manager.appResumeAdId
        if (adId.isNullOrEmpty()) {
            Log.d(TAG, "appResumeAdId: null or empty")
            return@runWhenReady
        }
        manager.isLoadingAppResume = true
        AppOpenAd.load(manager.getAdRequest(adId), object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                manager.appResumeAd = ad
                manager.appResumeLoadTime = System.currentTimeMillis()
                manager.isLoadingAppResume = false
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                manager.isLoadingAppResume = false
            }
        })
    }
}

internal fun loadAppOpenResumeList(manager: AppOpenManager) {
    Log.d(TAG, "loadAppOpenResumeList :")
    if (manager.isAdAvailable(false)) return
    if (AppOpenManager.isShowingAd) {
        Log.d(TAG, "isShowingAd: ")
        return
    }
    if (manager.isLoadingAppResume) {
        Log.d(TAG, "isLoadingAppResume: ")
        return
    }
    val adIdList = manager.appResumeAdIdList ?: return
    val index = intArrayOf(0)
    manager.isLoadingAppResume = true
    AppLogger.d(TAG, "loadAppOpenResumeList: ${index[0]} id: ${adIdList[index[0]]}")
    var adCallback: AdLoadCallback<AppOpenAd>? = null
    adCallback = object : AdLoadCallback<AppOpenAd> {
        override fun onAdLoaded(ad: AppOpenAd) {
            super.onAdLoaded(ad)
            manager.appResumeAd = ad
            manager.appResumeLoadTime = System.currentTimeMillis()
            manager.isLoadingAppResume = false
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            super.onAdFailedToLoad(adError)
            if (index[0] < adIdList.size - 1) {
                index[0]++
                AppLogger.d(TAG, "loadAppOpenResumeList: ${index[0]} id: ${adIdList[index[0]]}")
                AppOpenAd.load(manager.getAdRequest(adIdList[index[0]]), adCallback!!)
            } else {
                manager.isLoadingAppResume = false
                Log.d(TAG, "loadAppOpenResumeList: all ad IDs failed")
            }
        }
    }
    AppOpenAd.load(manager.getAdRequest(adIdList[index[0]]), adCallback)
}

internal fun showResumeAds(manager: AppOpenManager) {
    val currentActivity = manager.getCurrentActivity() ?: return
    val ad = manager.appResumeAd ?: return
    if (AppPurchase.getInstance().isPurchased()) return

    if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        try {
            manager.hideDialogLoading()
            manager.showDialogLoading()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        manager.appResumeAd?.adEventCallback =(object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                manager.appResumeAd = null
                AppOpenManager.isShowingAd = false
                manager.hideDialogLoading()
                loadAppOpenResume(manager)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                AppOpenManager.isShowingAd = false
                manager.hideDialogLoading()
            }

            override fun onAdShowedFullScreenContent() {
                AppOpenManager.isShowingAd = true
            }

            override fun onAdClicked() {}

            override fun onAdImpression() {}
        })
        manager.appResumeAd?.show(currentActivity)
    }
}
