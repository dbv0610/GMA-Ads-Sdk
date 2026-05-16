package com.ads.app.gmasdk.control.helper.adnative.preload

import java.util.Collections
import java.util.WeakHashMap

object AdUnitTagger {
    private val map: MutableMap<Any, String> = Collections.synchronizedMap(WeakHashMap())

    fun tag(ad: Any, adUnitId: String) { map[ad] = adUnitId }

    fun idOf(ad: Any): String? = map[ad]
}
