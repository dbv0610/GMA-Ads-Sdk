package com.ads.app.gmasdk.control.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.ads.app.gmasdk.control.admob.Admob
import com.ads.app.gmasdk.control.admob.AppOpenManager
import com.ads.app.gmasdk.control.config.AzAdConfig
import com.ads.app.gmasdk.control.config.TaichiConfig
import com.ads.app.gmasdk.control.event.AzAdjust
import com.ads.app.gmasdk.control.event.AzTaichi
import com.ads.app.gmasdk.control.util.AppLogger
import com.ads.app.gmasdk.control.util.AppUtil
import com.adjust.sdk.Adjust
import com.adjust.sdk.LogLevel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import com.adjust.sdk.AdjustConfig as AdjustSdkConfig

class AzAds private constructor() {
    lateinit var adConfig: AzAdConfig
        private set
    private lateinit var application: Application
    private lateinit var appAdId: String

    var isShowMessageTester: Boolean = false

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    @Volatile private var isMobileAdsReady = false
    private val initLock = Any()
    private val pendingInitCallbacks = CopyOnWriteArrayList<() -> Unit>()

    fun isConfigured() = ::adConfig.isInitialized && ::application.isInitialized && ::appAdId.isInitialized

    fun isMobileAdsReady() = isMobileAdsReady

    fun runWhenReady(callback: () -> Unit) {
        if (isMobileAdsReady) { callback(); return }
        synchronized(initLock) {
            if (isMobileAdsReady) { callback(); return }
            pendingInitCallbacks.add(callback)
        }
    }

    fun init(application: Application, adConfig: AzAdConfig) {
        this.application = application
        this.adConfig = adConfig
        val appAdId = adConfig.appAdId ?: throw IllegalArgumentException("app ad id can be not null")
        this.appAdId = appAdId
        AppUtil.VARIANT_DEV = adConfig.isVariantDev
        AppLogger.i(TAG, "Config variant dev: ${AppUtil.VARIANT_DEV}")
        adConfig.adjustConfig?.let { if (it.enableAdjust) setupAdjust(adConfig.isVariantDev, it.adjustToken, it.fbAppId) }
        val taichiConfig = adConfig.taichiConfig
        if (taichiConfig.isEnableTaichi) setupTaichi(taichiConfig)
    }

    private fun setupTaichi(taichiConfig: TaichiConfig) {
        AzTaichi.configure(taichiConfig.isEnableTaichi, taichiConfig.day2ImpressionThreshold, taichiConfig.revenueThresholdUsd)
        AppLogger.i(TAG, "setupTaichi: enabled=${taichiConfig.isEnableTaichi}, day2Threshold=${taichiConfig.day2ImpressionThreshold}")
    }

    fun initAdsNetwork(initFinished: () -> Unit) {
        runWhenReady(initFinished)
        if (!isConfigured()) {
            AppLogger.w(TAG, "initAdsNetwork called before AzAds.init(); callback queued")
            return
        }
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            AppLogger.d(TAG, "initAdsNetwork already in-flight, callback queued")
            return
        }
        AppLogger.d(TAG, "initAdsNetwork")
        Admob.getInstance().init(application, appAdId, adConfig.listDeviceTest) {
            val callbacks = synchronized(initLock) {
                isMobileAdsReady = true
                pendingInitCallbacks.toList().also { pendingInitCallbacks.clear() }
            }
            callbacks.forEach { runCatching { it() } }
            if (adConfig.isEnableAdResume()) {
                val listIdAdResume = adConfig.listIdAdResume
                val idAdResume = adConfig.idAdResume
                if (!listIdAdResume.isNullOrEmpty()) {
                    AppOpenManager.getInstance().init(adConfig.application, listIdAdResume)
                } else if (!idAdResume.isNullOrEmpty()) {
                    AppOpenManager.getInstance().init(adConfig.application, idAdResume)
                }
            }
        }
    }

    private fun setupAdjust(buildDebug: Boolean, adjustToken: String, fbAppId: String) {
        AzAdjust.enableAdjust = true
        val environment = if (buildDebug) AdjustSdkConfig.ENVIRONMENT_SANDBOX else AdjustSdkConfig.ENVIRONMENT_PRODUCTION
        AppLogger.i(TAG_ADJUST, "setupAdjust: $environment")
        val config = AdjustSdkConfig(application, adjustToken, environment)
        config.setLogLevel( LogLevel.VERBOSE)
        config.setOnAttributionChangedListener { attribution ->
            AppLogger.d(TAG_ADJUST, "Attribution callback called!")
            AppLogger.d(TAG_ADJUST, "Attribution: $attribution")
        }
        config.setOnEventTrackingSucceededListener { success ->
            AppLogger.d(TAG_ADJUST, "Event success callback called!")
            AppLogger.d(TAG_ADJUST, "Event success data: $success")
        }
        config.setOnEventTrackingFailedListener { failure ->
            AppLogger.d(TAG_ADJUST, "Event failure callback called!")
            AppLogger.d(TAG_ADJUST, "Event failure data: $failure")
        }
        config.setOnSessionTrackingSucceededListener { success ->
            AppLogger.d(TAG_ADJUST, "Session success callback called!")
            AppLogger.d(TAG_ADJUST, "Session success data: $success")
        }
        config.setOnSessionTrackingFailedListener { failure ->
            AppLogger.d(TAG_ADJUST, "Session failure callback called!")
            AppLogger.d(TAG_ADJUST, "Session failure data: $failure")
        }
        config.enableSendingInBackground()
        config.fbAppId = fbAppId
        Adjust.initSdk(config)
        application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
    }

    private inner class AdjustLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = Adjust.onResume()
        override fun onActivityPaused(activity: Activity) = Adjust.onPause()
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
    }

    companion object {
        const val TAG_ADJUST = "AzAdjust"
        const val TAG = "AzAds"

        internal val MAIN = Handler(Looper.getMainLooper())

        @Volatile private var INSTANCE: AzAds? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): AzAds = INSTANCE ?: AzAds().also { INSTANCE = it }
    }
}

inline fun AzAds.whenAdsReady(crossinline block: AzAds.() -> Unit) {
    val self = this
    runWhenReady { self.block() }
}
