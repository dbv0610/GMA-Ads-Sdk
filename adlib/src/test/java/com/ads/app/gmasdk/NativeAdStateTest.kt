package com.ads.app.gmasdk

import com.ads.app.gmasdk.compose.NativeAdDisplayState
import com.ads.app.gmasdk.compose.NativeAdEventRelay
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.helper.AdOptionVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdStateTest {

    // ── Idle ────────────────────────────────────────────────────────────────

    @Test
    fun idle_isNotLoaded() {
        val state = NativeAdDisplayState.Idle
        assertFalse(state is NativeAdDisplayState.Success)
    }

    @Test
    fun idle_isNotLoading() {
        val state = NativeAdDisplayState.Idle
        assertFalse(state is NativeAdDisplayState.Loading)
    }

    // ── Loading ─────────────────────────────────────────────────────────────

    @Test
    fun loading_isLoading() {
        val state = NativeAdDisplayState.Loading
        assertTrue(state is NativeAdDisplayState.Loading)
    }

    @Test
    fun loading_isNotLoaded() {
        val state = NativeAdDisplayState.Loading
        assertFalse(state is NativeAdDisplayState.Success)
    }

    // ── Success ─────────────────────────────────────────────────────────────

    @Test
    fun success_isLoaded() {
        val ad = ApNativeAd()
        val state = NativeAdDisplayState.Success(ad = ad, adUnitId = "test-id", fromPreload = false)
        assertTrue(state is NativeAdDisplayState.Success)
    }

    @Test
    fun success_fromPreload_true() {
        val ad = ApNativeAd()
        val state = NativeAdDisplayState.Success(ad = ad, adUnitId = "test-id", fromPreload = true)
        assertTrue((state as NativeAdDisplayState.Success).fromPreload)
    }

    @Test
    fun success_fromPreload_false() {
        val ad = ApNativeAd()
        val state = NativeAdDisplayState.Success(ad = ad, adUnitId = "test-id", fromPreload = false)
        assertFalse((state as NativeAdDisplayState.Success).fromPreload)
    }

    @Test
    fun success_adUnitId_matches() {
        val ad = ApNativeAd()
        val state = NativeAdDisplayState.Success(ad = ad, adUnitId = "ca-app-pub-test/456", fromPreload = false)
        assertEquals("ca-app-pub-test/456", (state as NativeAdDisplayState.Success).adUnitId)
    }

    @Test
    fun success_eventRelay_defaultsNull() {
        val state = NativeAdDisplayState.Success(ad = ApNativeAd(), adUnitId = "id", fromPreload = false)
        assertNull((state as NativeAdDisplayState.Success).eventRelay)
    }

    @Test
    fun success_eventRelay_canBeSet() {
        val relay = NativeAdEventRelay()
        val state = NativeAdDisplayState.Success(ad = ApNativeAd(), adUnitId = "id", fromPreload = false, eventRelay = relay)
        assertEquals(relay, (state as NativeAdDisplayState.Success).eventRelay)
    }

    // ── Error ───────────────────────────────────────────────────────────────

    @Test
    fun error_isError() {
        val state = NativeAdDisplayState.Error(null)
        assertTrue(state is NativeAdDisplayState.Error)
    }

    @Test
    fun error_nullError_isAllowed() {
        val state = NativeAdDisplayState.Error(null)
        assertNull((state as NativeAdDisplayState.Error).error)
    }

    // ── Cancelled ───────────────────────────────────────────────────────────

    @Test
    fun cancelled_isCancelled() {
        val state = NativeAdDisplayState.Cancelled()
        assertTrue(state is NativeAdDisplayState.Cancelled)
    }

    @Test
    fun cancelled_defaultAdVisibility_isGone() {
        val state = NativeAdDisplayState.Cancelled()
        assertEquals(AdOptionVisibility.GONE, state.adOptionVisibility)
    }

    @Test
    fun cancelled_invisibleVisibility_preserved() {
        val state = NativeAdDisplayState.Cancelled(AdOptionVisibility.INVISIBLE)
        assertEquals(AdOptionVisibility.INVISIBLE, state.adOptionVisibility)
    }

    // ── Holder state helpers ─────────────────────────────────────────────────

    @Test
    fun nativeAdEventRelay_addAndRemove_doesNotThrow() {
        val relay = NativeAdEventRelay()
        val callback = object : com.ads.app.gmasdk.control.ads.AzAdCallback() {}
        relay.addListener(callback)
        relay.removeListener(callback)
        relay.clearListeners()
    }

    @Test
    fun nativeAdEventRelay_clearListeners_noCallbacks() {
        val relay = NativeAdEventRelay()
        var clicked = false
        relay.addListener(object : com.ads.app.gmasdk.control.ads.AzAdCallback() {
            override fun onAdClicked() { clicked = true }
        })
        relay.clearListeners()
        // after clear, internal events should not fire
        relay.onAdClicked()
        assertFalse(clicked)
    }
}
