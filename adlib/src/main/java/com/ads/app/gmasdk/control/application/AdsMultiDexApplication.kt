package com.ads.app.gmasdk.control.application

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.ads.app.gmasdk.control.config.AzAdConfig
import com.ads.app.gmasdk.control.event.FirebaseAnalyticsUtil
import com.ads.app.gmasdk.control.util.AppUtil
import com.ads.app.gmasdk.control.util.SharePreferenceUtils

abstract class AdsMultiDexApplication : MultiDexApplication() {
    protected lateinit var azAdConfig: AzAdConfig
    protected var listTestDevice: List<String> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        azAdConfig = AzAdConfig(this as Application)
        if (SharePreferenceUtils.getInstallTime(this) == 0L) {
            SharePreferenceUtils.setInstallTime(this)
        }
        FirebaseAnalyticsUtil.init(this)
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this)
    }
}
