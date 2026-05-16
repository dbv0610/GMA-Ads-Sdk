package com.ads.app.gmasdk.control.util

object AppUtil {
    @JvmField
    var VARIANT_DEV: Boolean = true

    @JvmField
    var currentTotalRevenue001Ad: Float = 0f

    @JvmStatic
    fun countCharInStr(str: String, targetChar: Char): Int = str.count { it == targetChar }
}
