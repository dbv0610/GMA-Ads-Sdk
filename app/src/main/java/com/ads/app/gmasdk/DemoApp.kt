package com.ads.app.gmasdk

import com.ads.app.gmasdk.control.ads.AzAds
import com.ads.app.gmasdk.control.application.AdsMultiDexApplication

private const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

class DemoApp : AdsMultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        azAdConfig.apply {
            appAdId = TEST_APP_ID
            isVariantDev = true
        }
        AzAds.getInstance().init(this, azAdConfig)
        AzAds.getInstance().isShowMessageTester = true
        AzAds.getInstance().initAdsNetwork {}
    }
}
