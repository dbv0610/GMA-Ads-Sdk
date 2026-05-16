package com.ads.app.gmasdk.control.ads.wrapper

sealed class RewardAdEvent {
    data class Loaded(val ad: ApRewardAd) : RewardAdEvent()
    data class Failed(val error: ApAdError) : RewardAdEvent()
    data class Shown(val ad: ApRewardAd) : RewardAdEvent()
    data class Rewarded(val ad: ApRewardAd, val item: ApRewardItem) : RewardAdEvent()
    data class Clicked(val ad: ApRewardAd) : RewardAdEvent()
    data class Dismissed(val ad: ApRewardAd) : RewardAdEvent()
    data class FailedToShow(val error: ApAdError) : RewardAdEvent()
    data object NotReady : RewardAdEvent()
}
