package com.ads.app.gmasdk.control.billing

class PurchaseItem @JvmOverloads constructor(
    var itemId: String,
    var type: Int,
    var trialId: String? = null,
    var consume: Boolean = false
) {
    constructor(itemId: String, trialId: String, type: Int) : this(
        itemId = itemId,
        type = type,
        trialId = trialId,
        consume = false
    )
}
