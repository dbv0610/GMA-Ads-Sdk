package com.ads.app.gmasdk.control.helper.adnative.preload

import androidx.annotation.IntRange

data class NativeAdPreloadClientOption(
    val preloadAfterShow: Boolean = false,
    @IntRange(from = 0, to = 10) val preloadBuffer: Int = 0,
    val preloadOnResume: Boolean = false
) {
    companion object {
        @JvmStatic fun builder() = Builder()
    }

    class Builder {
        private var client = NativeAdPreloadClientOption()

        fun setPreloadAfterShow(preloadAfterShow: Boolean): Builder {
            client = client.copy(preloadAfterShow = preloadAfterShow)
            return this
        }

        fun setPreloadBuffer(@IntRange(from = 1, to = 10) preloadBuffer: Int): Builder {
            client = client.copy(preloadBuffer = preloadBuffer)
            return this
        }

        fun setPreloadOnResume(preloadOnResume: Boolean): Builder {
            client = client.copy(preloadOnResume = preloadOnResume)
            return this
        }

        fun build(): NativeAdPreloadClientOption = client
    }
}
