package com.ads.app.gmasdk.control.admob

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ads.app.gmasdk.R
import com.ads.app.gmasdk.control.dialog.PrepareLoadingAdsDialog
import com.ads.app.gmasdk.control.util.AppUtil
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

class Admob private constructor() {

    internal var handlerTimeout: Handler? = null
    internal var handlerTimeDelay: Handler? = null
    internal var rdTimeout: Runnable? = null
    internal var rdTimeDelay: Runnable? = null
    internal var dialog: PrepareLoadingAdsDialog? = null
    internal var isTimeout: Boolean = false
    internal var disableAdResumeWhenClickAds: Boolean = false
    var isShowLoadingSplash: Boolean = false
        internal set
    internal var isTimeDelay: Boolean = false
    internal var openActivityAfterShowInterAds: Boolean = false
    internal var interstitialSplash: InterstitialAd? = null

    companion object {
        private const val TAG = "AzAdmob"

        @JvmField
        val SPLASH_ADS = 0

        @JvmField
        val RESUME_ADS = 1
        const val BANNER_ADS = 2
        const val INTERS_ADS = 3
        const val REWARD_ADS = 4
        const val NATIVE_ADS = 5

        @Volatile
        private var instance: Admob? = null

        @JvmStatic
        fun getInstance(): Admob =
            instance ?: synchronized(this) {
                instance ?: Admob().also { instance = it }
            }
    }

    fun init(
        application: Application,
        appAdId: String,
        testDeviceList: List<String>? = null,
        onInitialized: (() -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT >= 28) {
            val processName = Application.getProcessName()
            val packageName = application.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            val config = InitializationConfig.Builder(appAdId).build()
            MobileAds.initialize(application, config, object : OnAdapterInitializationCompleteListener {
                override fun onAdapterInitializationComplete(status: InitializationStatus) {
                    onInitialized?.invoke()
                }
            })
        }
    }

    fun setDisableAdResumeWhenClickAds(disableAdResumeWhenClickAds: Boolean) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds
    }

    fun setOpenActivityAfterShowInterAds(openActivityAfterShowInterAds: Boolean) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds
    }

    fun getAdRequest(adUnitId: String): AdRequest {
        return AdRequest.Builder(adUnitId).build()
    }

    fun interstitialSplashLoaded(): Boolean = interstitialSplash != null

    fun getInterstitialSplash(): InterstitialAd? = interstitialSplash

    internal fun showLoadingDialogSafely(activity: Activity) {
        isShowLoadingSplash = true
        Handler(Looper.getMainLooper()).post {
            try {
                if (dialog != null) {
                    if (dialog!!.isShowing) {
                        dialog?.dismiss()
                        dialog = null
                    }
                }
                dialog = PrepareLoadingAdsDialog(activity)
                dialog?.show()
            } catch (_: Exception) {}
        }
    }

    internal fun safeDismissDialog(activity: Activity) {
        isShowLoadingSplash = false
        Handler(Looper.getMainLooper()).post {
            if (dialog != null && dialog!!.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                try {
                    dialog?.dismiss()
                    dialog = null
                } catch (_: Exception) {}
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(activity: Activity): String {
        val androidId = Settings.Secure.getString(activity.contentResolver, "android_id")!!
        return md5(androidId).uppercase(Locale.ROOT)
    }

    private fun md5(s: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray(Charsets.UTF_8))
            val messageDigest = digest.digest()
            val hexString = StringBuilder()
            for (b in messageDigest) {
                val h = StringBuilder(Integer.toHexString(0xFF and b.toInt()))
                while (h.length < 2) h.insert(0, "0")
                hexString.append(h)
            }
            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        }
    }

    @SuppressLint("MissingPermission", "NotificationPermission")
    internal fun showTestIdAlert(context: Context, typeAds: Int, id: String) {
        val content = when (typeAds) {
            2 -> "Banner Ads: "
            3 -> "Interstitial Ads: "
            4 -> "Rewarded Ads: "
            5 -> "Native Ads: "
            else -> ""
        } + id

        val notification = NotificationCompat.Builder(context, "warning_ads")
            .setContentTitle("Found test ad id")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_warning)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notification.flags = notification.flags or 0x10

        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("warning_ads", "Warning Ads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(typeAds, notification)
        }

        Log.e(TAG, "Found test ad id on debug : ${AppUtil.VARIANT_DEV}")
        if (!AppUtil.VARIANT_DEV) {
            Log.e(TAG, "Found test ad id on environment production. use test id only for develop environment ")
            throw RuntimeException("Found test ad id on environment production. Id found: $id")
        }
    }
}
