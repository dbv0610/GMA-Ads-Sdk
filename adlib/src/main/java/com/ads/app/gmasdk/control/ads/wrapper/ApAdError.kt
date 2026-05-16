package com.ads.app.gmasdk.control.ads.wrapper

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

class ApAdError {
    private var loadAdError: LoadAdError? = null
    private var fullScreenContentError: FullScreenContentError? = null
    private var errorMessage: String = ""

    constructor(loadAdError: LoadAdError) {
        this.loadAdError = loadAdError
    }

    constructor(fullScreenContentError: FullScreenContentError) {
        this.fullScreenContentError = fullScreenContentError
    }

    constructor(message: String) {
        this.errorMessage = message
    }

    fun getMessage(): String {
        loadAdError?.let { return it.message }
        fullScreenContentError?.let { return it.message }
        if (errorMessage.isNotEmpty()) return errorMessage
        return "unknown error"
    }
}
