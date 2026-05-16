package com.ads.app.gmasdk.control.ads.wrapper

sealed class InterstitialAdEvent {
    data class Loaded(val ad: ApInterstitialAd) : InterstitialAdEvent()
    data class Failed(val error: ApAdError) : InterstitialAdEvent()
    data class Shown(val ad: ApInterstitialAd) : InterstitialAdEvent()
    data class Clicked(val ad: ApInterstitialAd) : InterstitialAdEvent()
    data class Dismissed(val ad: ApInterstitialAd) : InterstitialAdEvent()
    data class FailedToShow(val error: ApAdError) : InterstitialAdEvent()
}
