package com.ads.app.gmasdk.control.util

object AppConstant {
    @Retention(AnnotationRetention.SOURCE)
    annotation class CollapsibleGravity {
        companion object {
            const val TOP = "top"
            const val BOTTOM = "bottom"
        }
    }
}
