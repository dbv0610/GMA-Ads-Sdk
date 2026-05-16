package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.app.gmasdk.control.billing.AppPurchase
import com.ads.app.gmasdk.control.dialog.PrepareLoadingAdsDialog
import com.ads.app.gmasdk.control.dialog.ResumeLoadingDialog
import com.ads.app.gmasdk.control.listener.AdResumePreShowListener
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import java.lang.ref.WeakReference
import java.util.Date

class AppOpenManager private constructor() : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    internal var appResumeAd: AppOpenAd? = null
    internal var splashAd: AppOpenAd? = null

    internal var appOpenAdEventCallback: AppOpenAdEventCallback? = null
    internal var handlerTimeout: Handler? = null
    internal var isTimeout: Boolean = false
    internal var rdTimeout: Runnable? = null
    var isTimeDelay: Boolean = false
    internal var appResumeAdId: String? = null
    internal var appResumeAdIdList: List<String>? = null
    internal var splashAdId: String? = null
    internal var splashAdIdList: List<String>? = null
    private var currentActivityRef: WeakReference<Activity>? = null
    private var myApplication: Application? = null
    internal var appResumeLoadTime: Long = 0L
    private var isInitialized: Boolean = false
    var isAppResumeEnabled: Boolean = true
        private set
    var isInterstitialShowing: Boolean = false
    internal var enableScreenContentCallback: Boolean = false
    private var disableAdResumeByClickAction: Boolean = false
    private val disabledAppOpenList: MutableList<Class<*>> = ArrayList()
    internal var isLoadingAppResume: Boolean = false
    var dialog: Dialog? = null
    var dialogSplash: Dialog? = null
    internal var isEnableList: Boolean = false
    private var adResumePreShowListener: AdResumePreShowListener? = null

    companion object {
        private const val TAG = "AppOpenManager"

        @Volatile
        private var INSTANCE: AppOpenManager? = null

        @JvmStatic
        var isShowingAd: Boolean = false
            internal set

        @JvmStatic
        fun getInstance(): AppOpenManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppOpenManager().also { INSTANCE = it }
            }
    }

    fun init(application: Application, appOpenAdId: String) {
        isInitialized = true
        disableAdResumeByClickAction = false
        myApplication = application
        myApplication!!.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this as LifecycleObserver)
        appResumeAdId = appOpenAdId
        isEnableList = false
    }

    fun init(application: Application, appOpenAdIdList: List<String>) {
        isInitialized = true
        disableAdResumeByClickAction = false
        myApplication = application
        myApplication!!.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this as LifecycleObserver)
        this.appResumeAdIdList = appOpenAdIdList
        isEnableList = true
    }

    fun isInitialized(): Boolean = isInitialized

    fun setInitialized(initialized: Boolean) {
        isInitialized = initialized
    }

    fun setEnableScreenContentCallback(enableScreenContentCallback: Boolean) {
        this.enableScreenContentCallback = enableScreenContentCallback
    }

    fun disableAdResumeByClickAction() {
        disableAdResumeByClickAction = true
    }

    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "disableAppResumeWithActivity: ${activityClass.name}")
        disabledAppOpenList.add(activityClass)
    }

    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "enableAppResumeWithActivity: ${activityClass.name}")
        disabledAppOpenList.remove(activityClass)
    }

    fun disableAppResume() {
        isAppResumeEnabled = false
    }

    fun enableAppResume() {
        isAppResumeEnabled = true
    }

    fun setAppResumeAdId(appResumeAdId: String) {
        this.appResumeAdId = appResumeAdId
    }

    fun getAppResumeAdId(): String? = appResumeAdId

    fun setAppResumeAdIdList(appResumeAdIdList: List<String>) {
        this.appResumeAdIdList = appResumeAdIdList
    }

    fun getAppResumeAdIdList(): List<String>? = appResumeAdIdList

    fun setSplashAdId(id: String) {
        splashAdId = id
    }

    fun setSplashAdIdList(ids: List<String>) {
        splashAdIdList = ids
    }

    fun setFullScreenContentCallback(callback: AppOpenAdEventCallback) {
        appOpenAdEventCallback = callback
    }

    fun removeFullScreenContentCallback() {
        appOpenAdEventCallback = null
    }

    internal fun getAdRequest(id: String): AdRequest = AdRequest.Builder(id).build()

    internal fun wasLoadTimeLessThanNHoursAgo(loadTime: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour = 3_600_000L
        return dateDifference < numMilliSecondsPerHour * 4
    }

    fun isAdAvailable(isSplash: Boolean): Boolean {
        val loadTime = if (isSplash) 0L else appResumeLoadTime
        val isFresh = wasLoadTimeLessThanNHoursAgo(loadTime)
        val hasAd = if (isSplash) splashAd != null else appResumeAd != null
        val available = hasAd && isFresh
        AppLogger.d(TAG, "isAdAvailable [hasAd=$hasAd, isFresh=$isFresh] => $available")
        return available
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
        // no-op
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        Log.d(TAG, "onActivityStarted: $activity")
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        Log.d(TAG, "onActivityResumed: $activity")
        if (activity.javaClass.name != AdActivity::class.java.name) {
            loadAppOpenResume(this)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // no-op
    }

    override fun onActivityPaused(activity: Activity) {
        // no-op
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: android.os.Bundle) {
        // no-op
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
            Log.d(TAG, "onActivityDestroyed: cleared ref for ${activity.javaClass.simpleName}")
        }
    }

    fun showAdIfAvailable() {
        val currentActivity = getCurrentActivity()
        if (currentActivity == null || AppPurchase.getInstance().isPurchased()) {
            if (appOpenAdEventCallback != null && enableScreenContentCallback) {
                appOpenAdEventCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        Log.d(TAG, "showAdIfAvailable: ${ProcessLifecycleOwner.get().lifecycle.currentState}")
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            Log.d(TAG, "showAdIfAvailable: return")
            if (appOpenAdEventCallback != null && enableScreenContentCallback) {
                appOpenAdEventCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        if (!isShowingAd && isAdAvailable(false)) {
            showResumeAds(this)
        }
    }

    fun setAdResumePreShowListener(listener: AdResumePreShowListener) {
        adResumePreShowListener = listener
    }

    internal fun hideDialogLoading() {
        val d = dialog ?: return
        dialog = null
        Handler(Looper.getMainLooper()).post {
            try {
                if (!d.isShowing) return@post
                val window: Window? = d.window
                if (window != null) {
                    val decorView = window.decorView
                    if (!decorView.isAttachedToWindow) return@post
                    val ctx = d.context
                    if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) return@post
                    d.dismiss()
                }
            } catch (_: IllegalArgumentException) {
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal fun showDialogLoading() {
        val currentActivity = getCurrentActivity()
        try {
            if (dialog == null) {
                dialog = currentActivity?.let { ResumeLoadingDialog(it as Context) }
            }
            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun showDialogLoadingSplash(context: Context) {
        try {
            if (dialogSplash == null) {
                dialogSplash = PrepareLoadingAdsDialog(context)
            }
            val d = dialogSplash!!
            d.setCancelable(false)
            d.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun hideDialogLoadingSplash() {
        Handler(Looper.getMainLooper()).postDelayed({
            val d = dialogSplash ?: return@postDelayed
            dialogSplash = null
            try {
                if (!d.isShowing) return@postDelayed
                val window: Window? = d.window
                if (window != null) {
                    val decorView = window.decorView
                    if (!decorView.isAttachedToWindow) return@postDelayed
                    val ctx = d.context
                    if (ctx is Activity && (ctx.isFinishing || ctx.isDestroyed)) return@postDelayed
                    d.dismiss()
                }
            } catch (_: IllegalArgumentException) {
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 300L)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val currentActivity = getCurrentActivity()
        if (!isInitialized) {
            Log.d(TAG, "onResume: app not initialized")
            return
        }
        if (currentActivity == null) {
            Log.d(TAG, "onResume: currentActivity is null")
            return
        }
        if (!isAppResumeEnabled) {
            Log.d(TAG, "onResume: app resume is disabled")
            return
        }
        if (isInterstitialShowing) {
            Log.d(TAG, "onResume: interstitial is showing")
            return
        }
        if (disableAdResumeByClickAction) {
            Log.d(TAG, "onResume:ad resume disable ad by action")
            disableAdResumeByClickAction = false
            return
        }
        for (activity in disabledAppOpenList) {
            if (activity.name == currentActivity.javaClass.name) {
                Log.d(TAG, "onResume: activity is disabled")
                return
            }
        }
        Log.d(TAG, "onResume: show resume ads :${currentActivity.javaClass.simpleName}")
        if (adResumePreShowListener != null) {
            adResumePreShowListener!!.onPreShowAd()
        } else {
            showAdIfAvailable()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "onStop: app stop")
    }

    internal fun getCurrentActivity(): Activity? =
        try {
            currentActivityRef?.get()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
}
