package com.ads.app.gmasdk.control.config

class AdjustConfig(
    var enableAdjust: Boolean = false,
    var adjustToken: String = ""
) {
    var eventNamePurchase: String = "purchase_item"
    var eventAdImpressionValue: String = ""
    var eventAdImpression: String = ""
    var eventAdClick: String = ""
    var fbAppId: String = ""
}
