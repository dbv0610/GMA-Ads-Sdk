package com.ads.app.gmasdk

import com.ads.app.gmasdk.compose.NativeAdTagConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdTagConfigTest {

    @Test
    fun simple_getAllAdUnitIds_returnsSingleId() {
        val config = NativeAdTagConfig.simple(tag = "home", adUnitId = "ca-app-pub-test/123")
        assertEquals(listOf("ca-app-pub-test/123"), config.getAllAdUnitIds())
    }

    @Test
    fun simple_idAds_matchesProvidedId() {
        val config = NativeAdTagConfig.simple(tag = "home", adUnitId = "ca-app-pub-test/123")
        assertEquals("ca-app-pub-test/123", config.idAds)
    }

    @Test
    fun simple_tag_matchesProvidedTag() {
        val config = NativeAdTagConfig.simple(tag = "settings", adUnitId = "id")
        assertEquals("settings", config.tag)
    }

    @Test
    fun simple_canShowAds_defaultsTrue() {
        val config = NativeAdTagConfig.simple(tag = "home", adUnitId = "id")
        assertTrue(config.canShowAds)
    }

    @Test
    fun simple_canShowAds_canBeSetFalse() {
        val config = NativeAdTagConfig.simple(tag = "home", adUnitId = "id", canShowAds = false)
        assertEquals(false, config.canShowAds)
    }

    @Test
    fun waterfall_getAllAdUnitIds_returnsAllIds() {
        val ids = listOf("id-high", "id-med", "id-low")
        val config = NativeAdTagConfig.waterfall(tag = "home", adUnitIds = ids)
        assertEquals(ids, config.getAllAdUnitIds())
    }

    @Test
    fun waterfall_idAds_isFirstId() {
        val config = NativeAdTagConfig.waterfall(tag = "home", adUnitIds = listOf("id-high", "id-low"))
        assertEquals("id-high", config.idAds)
    }

    @Test
    fun waterfall_emptyList_getAllAdUnitIds_returnsEmpty() {
        val config = NativeAdTagConfig.waterfall(tag = "home", adUnitIds = emptyList())
        assertTrue(config.getAllAdUnitIds().isEmpty())
    }

    @Test
    fun setListId_overridesIdAds() {
        val config = NativeAdTagConfig.simple(tag = "home", adUnitId = "original")
            .setListId(listOf("override-1", "override-2"))
        assertEquals(listOf("override-1", "override-2"), config.getAllAdUnitIds())
    }

    @Test
    fun getAllAdUnitIds_withNoListId_fallsBackToIdAds() {
        val config = NativeAdTagConfig(tag = "home", idAds = "single-id")
        assertEquals(listOf("single-id"), config.getAllAdUnitIds())
    }
}
