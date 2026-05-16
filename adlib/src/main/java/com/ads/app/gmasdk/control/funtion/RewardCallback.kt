package com.ads.app.gmasdk.control.funtion

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

interface RewardCallback {
    fun onUserEarnedReward(rewardItem: RewardItem)
    fun onRewardedAdClosed()
    fun onRewardedAdFailedToShow(fullScreenContentError: FullScreenContentError)
    fun onAdClicked()
    fun onAdImpression()
}
