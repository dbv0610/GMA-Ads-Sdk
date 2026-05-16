# GMASDK — How to Use

## Table of Contents

1. [Setup](#setup)
2. [Initialization](#initialization)
3. [Banner Ads — Compose](#banner-ads--compose)
4. [Banner Ads — XML](#banner-ads--xml)
5. [Native Ads — Compose](#native-ads--compose)
6. [Native Ads — XML](#native-ads--xml)
7. [Interstitial Ads](#interstitial-ads)
8. [Reward Ads](#reward-ads)
9. [App Open Ads](#app-open-ads)
10. [Native Ad Preloading](#native-ad-preloading)
11. [Running Tests](#running-tests)

---

## Project Structure

```
GMASDK/
├── app/                  — thin demo application (MainActivity only)
│   └── build.gradle.kts  — depends on :adlib
└── adlib/                — Android library module (all ads code + Compose UI)
    ├── build.gradle.kts
    └── src/
        ├── main/java/com/ads/app/gmasdk/
        │   ├── compose/  — Jetpack Compose ad components
        │   └── control/  — core SDK wrappers, helpers, billing, events
        ├── test/         — unit tests (no device)
        └── androidTest/  — instrumented tests (device/emulator)
```

## Setup

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":adlib"))
    // or, when published as an AAR:
    // implementation("com.ads.app:adlib:1.0.0")
}
```

---

## Initialization

Call once in `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AzAdConfig(this).apply {
            appAdId = BuildConfig.ADMOB_APP_ID
            isVariantDev = BuildConfig.DEBUG
        }
        AzAds.getInstance().init(this, config)
    }
}
```

Enable the debug overlay (shows ad state on every ad unit):

```kotlin
AzAds.getInstance().isShowMessageTester = BuildConfig.DEBUG
```

---

## Banner Ads — Compose

### Simple banner (single ad unit)

```kotlin
@Composable
fun MyScreen() {
    val config = remember {
        BannerAdConfig(
            idAds = BuildConfig.AD_BANNER,
            canShowAds = true,
            canReloadAds = true
        )
    }

    Column {
        // ... your content ...
        BannerAd(config = config)
    }
}
```

### Waterfall banner (tries each unit in order)

```kotlin
val config = remember {
    BannerAdConfig(idAds = BuildConfig.AD_BANNER_HIGH, canShowAds = true, canReloadAds = true)
        .setListId(listOf(BuildConfig.AD_BANNER_HIGH, BuildConfig.AD_BANNER_LOW))
}
BannerAd(config = config)
```

### Collapsible banner

```kotlin
val config = remember {
    BannerAdConfig(idAds = BuildConfig.AD_BANNER, canShowAds = true, canReloadAds = true).apply {
        collapsibleGravity = "bottom"  // or "top"
    }
}
BannerAd(config = config)
```

### Inline adaptive banner

```kotlin
val config = remember {
    BannerAdConfig(idAds = BuildConfig.AD_BANNER, canShowAds = true, canReloadAds = true)
        .asInlineBanner(maxHeightDp = 100)
}
BannerAd(config = config)
```

### Manual holder (advanced — control reload yourself)

```kotlin
@Composable
fun MyScreen() {
    val config = remember { BannerAdConfig(...) }
    val holder = rememberBannerAd(config = config)

    BannerAdCard(
        holder = holder,
        loading = { MyBannerShimmer() },
        error = { /* optional error UI */ }
    )

    // force a reload:
    Button(onClick = { holder.reload() }) { Text("Reload") }
}
```

---

## Banner Ads — XML

### Layout

```xml
<!-- layout_banner_control.xml already included, or inline: -->
<FrameLayout
    android:id="@+id/banner_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Activity/Fragment code

```kotlin
class MyActivity : AppCompatActivity() {
    private val bannerHelper by lazy {
        BannerAdHelper(
            activity = this,
            lifecycleOwner = this,
            config = BannerAdConfig(
                idAds = BuildConfig.AD_BANNER,
                canShowAds = true,
                canReloadAds = true
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        bannerHelper
            .setBannerContentView(findViewById(R.id.banner_container))
            .setShimmerLayoutView(findViewById(R.id.fl_shimmer))
            .requestAds(BannerAdParam.Request.CreateRequest)
    }
}
```

---

## Native Ads — Compose

### Quickstart (single line)

```kotlin
NativeAd(tag = "home", adUnitId = BuildConfig.AD_NATIVE) { ad ->
    NativeAdView(nativeAd = ad) {
        NativeHeadlineRow()
        NativeBodyText()
        NativeCtaButton()
    }
}
```

### Waterfall (tries each unit in order)

```kotlin
NativeAd(
    tag = "home",
    adUnitIds = listOf(BuildConfig.AD_NATIVE_HIGH, BuildConfig.AD_NATIVE_LOW)
) { ad ->
    NativeAdView(nativeAd = ad) {
        NativeHeadlineRow()
        NativeBodyText()
        NativeCtaButton()
    }
}
```

### Full custom layout with all components

```kotlin
NativeAd(tag = "home", adUnitId = BuildConfig.AD_NATIVE) { ad ->
    NativeAdView(
        nativeAd = ad,
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NativeAdIconView(Modifier.size(48.dp)) { drawable ->
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
                    update = { it.setImageDrawable(drawable) }
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                NativeAdHeadlineView { headline ->
                    Text(headline, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NativeAdBodyView { body ->
                    Text(body, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        NativeAdMediaView(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        NativeAdCallToActionView(Modifier.fillMaxWidth()) { cta ->
            Button(onClick = {}) { Text(cta) }
        }
    }
}
```

### Loading and error placeholders

```kotlin
NativeAd(
    tag = "home",
    adUnitId = BuildConfig.AD_NATIVE,
    loading = { isActive ->
        if (isActive) {
            // Full skeleton shown while loading
            Column {
                ShimmerHeadlineRow()
                ShimmerBodyBox()
                ShimmerCtaRoundedButton()
            }
        }
        // isActive=false → Cancelled/INVISIBLE state (reserve space silently)
    },
    error = { /* hide or show nothing */ }
) { ad ->
    NativeAdView(nativeAd = ad) { /* ... */ }
}
```

### Manual holder (for screen-level preload control)

```kotlin
@Composable
fun HomeScreen() {
    val holder = rememberNativeAdPreload(
        tag = "home",
        fallbackAdUnitIds = listOf(BuildConfig.AD_NATIVE_HIGH, BuildConfig.AD_NATIVE_LOW),
        autoRequestOnStart = true,
        autoReloadOnResume = true
    )

    // pass to multiple cards, or check state manually:
    if (holder.isLoaded) {
        NativeAdCard(holder = holder) { ad ->
            NativeAdView(nativeAd = ad) { /* ... */ }
        }
    }

    // force reload on user action:
    Button(onClick = { holder.reload() }) { Text("Refresh Ad") }
}
```

### Compose native ad — available components

| Composable | Purpose |
|---|---|
| `NativeAdView` | Root container — wraps `NativeAdView` (SDK) |
| `NativeAdHeadlineView` | Ad title |
| `NativeAdBodyView` | Ad description |
| `NativeAdCallToActionView` | CTA button area |
| `NativeAdIconView` | App icon |
| `NativeAdMediaView` | Video / image media |
| `NativeAdAdvertiserView` | Advertiser name |
| `NativeAdPriceView` | Price string |
| `NativeAdStarRatingView` | Star rating (Double) |
| `NativeAdAttribution` | "Ad" badge (pre-styled Box) |
| `AdBadge` | Plain text "Ad" label |
| `NativeAdButton` | Pre-styled CTA box |

> All components are no-ops or show preview content when `LocalInspectionMode = true`.

---

## Native Ads — XML

### Layout files provided

| Layout | Description |
|---|---|
| `custom_native_admod_medium.xml` | Medium card (icon + headline + body + CTA) |
| `custom_native_admob_small.xml` | Small horizontal card |
| `custom_native_admob_free_size.xml` | Full card with MediaView |
| `custom_native_admod_medium2.xml` | Medium variant |
| `custom_native_admod_medium_rate.xml` | Medium + star rating |

### Activity/Fragment code

```kotlin
val nativeHelper = NativeAdHelper(
    activity = this,
    lifecycleOwner = this,
    config = NativeAdConfig(
        idAds = BuildConfig.AD_NATIVE,
        canShowAds = true,
        canReloadAds = true,
        layoutId = R.layout.custom_native_admod_medium
    )
).apply {
    setNativeContentView(findViewById(R.id.native_content))
    setShimmerLayoutView(findViewById(R.id.shimmer))
}

nativeHelper.requestAds(NativeAdParam.Request.CreateRequest)
```

### Waterfall + timeout

```kotlin
NativeAdConfig(
    idAds = BuildConfig.AD_NATIVE_HIGH,
    canShowAds = true,
    canReloadAds = true,
    layoutId = R.layout.custom_native_admod_medium
).setListId(listOf(BuildConfig.AD_NATIVE_HIGH, BuildConfig.AD_NATIVE_LOW))
 .setListTimeout(listOf(8_000L, 8_000L))
```

---

## Interstitial Ads

### Load and show

```kotlin
// Load
AzAds.getInstance().loadInterstitial(context, BuildConfig.AD_INTERSTITIAL) { event ->
    when (event) {
        is InterstitialAdEvent.Loaded -> { /* store ApInterstitialAd */ }
        is InterstitialAdEvent.Failed -> { /* handle */ }
    }
}

// Show
AzAds.getInstance().showInterstitialAd(
    activity = this,
    interstitialAd = myAd,
    onNextAction = { /* called after dismiss */ }
)
```

### Preload (background prefetch)

```kotlin
// In Application.onCreate after init:
AzAds.getInstance().preloadInterstitialAds(BuildConfig.AD_INTERSTITIAL)

// Retrieve preloaded ad:
val ad = AzAds.getInstance().getInterstitialAdsPreload(BuildConfig.AD_INTERSTITIAL)
```

---

## Reward Ads

```kotlin
AzAds.getInstance().loadRewardAd(context, BuildConfig.AD_REWARD) { event ->
    when (event) {
        is RewardAdEvent.Loaded -> myRewardAd = event.ad
        is RewardAdEvent.Failed -> { /* fallback */ }
    }
}

AzAds.getInstance().showRewardAd(
    activity = this,
    rewardAd = myRewardAd,
    onRewarded = { item -> grantReward(item) },
    onDismissed = { /* resume flow */ }
)
```

---

## App Open Ads

Configure in `AzAdConfig`:

```kotlin
val config = AzAdConfig(this).apply {
    appAdId = BuildConfig.ADMOB_APP_ID
    idAdResume = BuildConfig.AD_APP_OPEN
    // or waterfall:
    // listIdAdResume = listOf(BuildConfig.AD_APP_OPEN_HIGH, BuildConfig.AD_APP_OPEN_LOW)
}
AzAds.getInstance().init(this, config)
```

App Open ads load automatically on cold start and show when the app resumes from background.

---

## Native Ad Preloading

Fill a buffer before the user reaches the ad screen so display is instant.

### XML path (NativeAdHelper)

```kotlin
// In Application.onCreate, after AzAds.init():
NativeAdPreload.getInstance().preload(
    context = this,
    adId = BuildConfig.AD_NATIVE,
    layoutId = R.layout.custom_native_admod_medium,
    bufferSize = 2
)
```

### Compose path

Use `layoutId = 0` as the Compose slot key:

```kotlin
// Application.onCreate:
NativeAdPreload.getInstance().preload(
    context = this,
    adId = BuildConfig.AD_NATIVE,
    layoutId = 0,     // Compose slot
    bufferSize = 2
)

// In Composable — rememberNativeAdPreload checks this pool first:
NativeAd(tag = "home", adUnitId = BuildConfig.AD_NATIVE) { ad -> ... }
```

### Check / inspect buffer

```kotlin
val count = NativeAdPreload.getInstance()
    .getNativeAdBuffer(BuildConfig.AD_NATIVE, 0).size
```

---

## Running Tests

### Unit tests (no device required)

```bash
./gradlew :app:test
```

Covers:
- `NativeAdTagConfigTest` — config factory methods and `getAllAdUnitIds()`
- `NativeAdStateTest` — `NativeAdDisplayState` sealed class properties
- `BannerAdStateTest` — `BannerAdDisplayState` and `BannerAdConfig` properties

### Instrumented tests (requires device or emulator)

```bash
./gradlew :app:connectedAndroidTest
```

Covers:
- `NativeAdComposeTest` — `NativeAdCard` state rendering, inspection mode, shimmer components
- `BannerAdComposeTest` — `BannerAdCard` state rendering, `DefaultBannerAdLoading`, state transitions
- `NativeAdXmlLayoutTest` — XML layout inflation, required view IDs present

### Run a specific test class

```bash
./gradlew :app:connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.ads.app.gmasdk.NativeAdComposeTest
```
