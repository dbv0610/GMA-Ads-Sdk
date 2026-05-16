package com.ads.app.gmasdk.control.ads.wrapper

import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd

class ApRewardAd : ApAdBase {
    @JvmField
    var admobReward: RewardedAd? = null

    @JvmField
    var admobRewardInter: RewardedInterstitialAd? = null

    constructor() : super()

    constructor(status: StatusAd) : super(status)

    constructor(admobReward: RewardedAd) : super(StatusAd.AD_LOADED) {
        this.admobReward = admobReward
    }

    constructor(admobRewardInter: RewardedInterstitialAd) : super(StatusAd.AD_LOADED) {
        this.admobRewardInter = admobRewardInter
    }

    fun setAdmobReward(admobReward: RewardedAd) {
        this.admobReward = admobReward
        status = StatusAd.AD_LOADED
    }

    fun setAdmobReward(admobRewardInter: RewardedInterstitialAd) {
        this.admobRewardInter = admobRewardInter
    }

    fun clean() {
        admobReward = null
        admobRewardInter = null
    }

    override fun isReady() = admobReward != null || admobRewardInter != null

    fun isRewardInterstitial() = admobRewardInter != null
}
