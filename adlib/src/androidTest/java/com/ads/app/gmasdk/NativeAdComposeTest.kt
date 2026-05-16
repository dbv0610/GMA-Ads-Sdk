package com.ads.app.gmasdk

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ads.app.gmasdk.compose.AdBadge
import com.ads.app.gmasdk.compose.NativeAdAttribution
import com.ads.app.gmasdk.compose.NativeAdButton
import com.ads.app.gmasdk.compose.NativeAdCard
import com.ads.app.gmasdk.compose.NativeAdDisplayState
import com.ads.app.gmasdk.compose.NativeAdPreloadHolder
import com.ads.app.gmasdk.compose.NativeAdView
import com.ads.app.gmasdk.compose.ShimmerHeadlineRow
import com.ads.app.gmasdk.compose.ShimmerIconCircle
import com.ads.app.gmasdk.control.ads.wrapper.ApNativeAd
import com.ads.app.gmasdk.control.helper.AdOptionVisibility
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeAdComposeTest {

    @get:Rule
    val rule = createComposeRule()

    // ── NativeAdCard – state-driven rendering ────────────────────────────────

    @Test
    fun nativeAdCard_idleState_showsLoadingContent() {
        val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Idle)
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) }
            )
        }

        rule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun nativeAdCard_loadingState_showsLoadingContent() {
        val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Loading)
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) }
            )
        }

        rule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun nativeAdCard_cancelledStateGone_contentIsHidden() {
        val state = MutableStateFlow<NativeAdDisplayState>(
            NativeAdDisplayState.Cancelled(AdOptionVisibility.GONE)
        )
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) }
            )
        }

        // GONE → NativeAdCard renders a plain height(0.dp) Box, loading lambda is not called
        rule.onNodeWithTag("loading").assertDoesNotExist()
    }

    @Test
    fun nativeAdCard_cancelledStateInvisible_showsLoadingFalse() {
        val state = MutableStateFlow<NativeAdDisplayState>(
            NativeAdDisplayState.Cancelled(AdOptionVisibility.INVISIBLE)
        )
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { isActive ->
                    if (!isActive) Box(Modifier.testTag("placeholder"))
                }
            )
        }

        rule.onNodeWithTag("placeholder").assertIsDisplayed()
    }

    @Test
    fun nativeAdCard_errorState_errorContentRenderedButHidden() {
        val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Error(null))
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) },
                error = { Box(Modifier.testTag("error")) }
            )
        }

        // Error → loading lambda not called; error lambda called but inside height(0.dp) container
        rule.onNodeWithTag("loading").assertDoesNotExist()
        rule.onNodeWithTag("error").assertIsNotDisplayed()
    }

    // ── NativeAdCard inspection mode ─────────────────────────────────────────

    @Test
    fun nativeAdCard_inspectionMode_showsLoadingComposable() {
        val holder = NativeAdPreloadHolder.testInstance()

        rule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                NativeAdCard(
                    holder = holder,
                    loading = { Box(Modifier.testTag("preview_loading")) }
                )
            }
        }

        rule.onNodeWithTag("preview_loading").assertIsDisplayed()
    }

    // ── NativeAdView – inspection mode ───────────────────────────────────────

    @Test
    fun nativeAdView_inspectionMode_rendersContent() {
        val ad = ApNativeAd()

        rule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                NativeAdView(nativeAd = ad) {
                    Box(Modifier.testTag("ad_content"))
                }
            }
        }

        rule.onNodeWithTag("ad_content").assertIsDisplayed()
    }

    // ── Utility composables ──────────────────────────────────────────────────

    @Test
    fun adBadge_rendersDefaultText() {
        rule.setContent { AdBadge() }
        rule.onNodeWithText("Ad").assertIsDisplayed()
    }

    @Test
    fun adBadge_rendersCustomText() {
        rule.setContent { AdBadge(text = "Sponsored") }
        rule.onNodeWithText("Sponsored").assertIsDisplayed()
    }

    @Test
    fun nativeAdAttribution_rendersText() {
        rule.setContent { NativeAdAttribution(text = "Ad") }
        rule.onNodeWithText("Ad").assertIsDisplayed()
    }

    @Test
    fun nativeAdButton_rendersText() {
        rule.setContent { NativeAdButton(text = "Install") }
        rule.onNodeWithText("Install").assertIsDisplayed()
    }

    // ── Shimmer skeleton components ───────────────────────────────────────────

    @Test
    fun shimmerIconCircle_renders() {
        rule.setContent {
            Box(Modifier.testTag("shimmer_icon")) { ShimmerIconCircle() }
        }
        rule.onNodeWithTag("shimmer_icon").assertIsDisplayed()
    }

    @Test
    fun shimmerHeadlineRow_renders() {
        rule.setContent {
            Box(Modifier.testTag("shimmer_headline")) { ShimmerHeadlineRow() }
        }
        rule.onNodeWithTag("shimmer_headline").assertIsDisplayed()
    }

    // ── NativeAdCard state transitions ────────────────────────────────────────

    @Test
    fun nativeAdCard_stateTransition_idleToLoading_updatesContent() {
        val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Idle)
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) }
            )
        }

        rule.onNodeWithTag("loading").assertIsDisplayed()

        state.value = NativeAdDisplayState.Loading
        rule.waitForIdle()

        rule.onNodeWithTag("loading").assertIsDisplayed()
    }

    @Test
    fun nativeAdCard_stateTransition_idleToCancelled_hidesContent() {
        val state = MutableStateFlow<NativeAdDisplayState>(NativeAdDisplayState.Idle)
        val holder = NativeAdPreloadHolder.testInstance(state = state)

        rule.setContent {
            NativeAdCard(
                holder = holder,
                loading = { Box(Modifier.testTag("loading")) }
            )
        }

        state.value = NativeAdDisplayState.Cancelled(AdOptionVisibility.GONE)
        rule.waitForIdle()

        // GONE → loading lambda not called, node is removed from composition
        rule.onNodeWithTag("loading").assertDoesNotExist()
    }
}
