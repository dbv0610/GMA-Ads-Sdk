package com.ads.app.gmasdk.control.util

import android.os.Handler
import android.os.Looper

private val mainHandler = Handler(Looper.getMainLooper())

fun runOnMain(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action()
    } else {
        mainHandler.post(action)
    }
}
