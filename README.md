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
11. [In-App Billing](#in-app-billing)
12. [Running Tests](#running-tests)

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

## In-App Billing

`AppPurchase` wraps the Google Play Billing Library. It splits concerns across:

| Class | Role |
|---|---|
| `AppPurchase` | Singleton facade — the only class your app touches |
| `BillingConnectionManager` | Manages the `BillingClient` connection with exponential-backoff reconnect |
| `ProductDetailsRepository` | Caches `ProductDetails` from Play Store; price/period helpers |
| `PurchaseVerifier` | Queries owned purchases on init and exposes `isPurchased` as a `StateFlow` |
| `PurchaseProcessor` | Launches billing flows; handles `PurchasesUpdatedListener` |

---

### Setup — declare products

```kotlin
val items = mutableListOf(
    PurchaseItem(
        itemId  = BuildConfig.IAP_PRODUCT_ID,   // one-time purchase
        type    = AppPurchase.TYPE_IAP.PURCHASE,
        consume = false                          // set true for consumables
    ),
    PurchaseItem(
        itemId  = BuildConfig.SUB_MONTHLY,      // subscription
        type    = AppPurchase.TYPE_IAP.SUBSCRIPTION,
        trialId = "promo-trial-3day"            // optional: offer ID for trial
    ),
)

AppPurchase.getInstance().initBilling(application, items)
```

Call once in `Application.onCreate()`.

---

### Wait for billing to be ready

**Callback (recommended):**

```kotlin
// With timeout — fires onInitBillingFinished(ERROR) if billing stalls
AppPurchase.getInstance().setBillingListener(object : BillingListener {
    override fun onInitBillingFinished(resultCode: Int) {
        val isPurchased = AppPurchase.getInstance().isPurchased()
        // update UI, unlock features, etc.
    }
}, timeout = 10_000)
```

**StateFlow (Compose / coroutine):**

```kotlin
lifecycleScope.launch {
    AppPurchase.getInstance().billingState?.collect { state ->
        when (state) {
            is BillingState.Connected    -> { /* ready */ }
            is BillingState.Disconnected -> { /* retry / hide paywall */ }
            is BillingState.Error        -> { /* show error */ }
            else -> Unit
        }
    }
}
```

**Observe purchase state as Flow:**

```kotlin
lifecycleScope.launch {
    AppPurchase.getInstance().isPurchasedFlow()?.collect { purchased ->
        updatePremiumUI(purchased)
    }
}
```

---

### Make a purchase

```kotlin
AppPurchase.getInstance().purchase(activity, BuildConfig.IAP_PRODUCT_ID) { event ->
    when (event) {
        is PurchaseEvent.Success  -> { /* unlock feature */ }
        is PurchaseEvent.Pending  -> { /* show "pending payment" UI */ }
        is PurchaseEvent.Cancelled -> { /* user closed the sheet */ }
        is PurchaseEvent.Error    -> showError(event.message)
    }
}
```

### Subscribe

```kotlin
AppPurchase.getInstance().subscribe(activity, BuildConfig.SUB_MONTHLY) { event ->
    when (event) {
        is PurchaseEvent.Success -> { /* grant premium */ }
        else -> { /* handle */ }
    }
}
```

### Upgrade / change plan

```kotlin
val oldToken = AppPurchase.getInstance().getSubscriptionPurchaseToken(BuildConfig.SUB_MONTHLY)
AppPurchase.getInstance().upgradeSubscription(
    activity        = this,
    newSubsId       = BuildConfig.SUB_YEARLY,
    oldPurchaseToken = oldToken ?: "",
    replacementMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
) { event -> /* handle */ }
```

---

### Check purchase status

```kotlin
// Simple boolean
if (AppPurchase.getInstance().isPurchased()) {
    showPremiumContent()
}

// Who owns what
val ownedSubs   = AppPurchase.getInstance().getOwnerIdSubs()   // List<PurchaseResult>
val ownedInApps = AppPurchase.getInstance().getOwnerIdInApp()  // List<PurchaseResult>

// Force a re-query (e.g. after returning from another screen)
AppPurchase.getInstance().updatePurchaseStatus()
AppPurchase.getInstance().setUpdatePurchaseListener(UpdatePurchaseListener {
    // called when the re-query completes
    refreshUI()
})
```

---

### Get product info for your paywall UI

```kotlin
val products = AppPurchase.getInstance().getProductInfoList()

products.forEach { info ->
    when (info.type) {
        AppPurchase.TYPE_IAP.PURCHASE -> {
            // info.price, info.priceMicros, info.currency
        }
        AppPurchase.TYPE_IAP.SUBSCRIPTION -> {
            // info.regularPrice, info.billingPeriod
            // info.introPrice / info.introCycles  (discounted intro period)
            // info.trialPeriod                    (free trial, e.g. "P3D")
            // info.hasPromo, info.hasTrial, info.hasIntroPrice
        }
    }
}
```

Price helpers (available after `onInitBillingFinished`):

```kotlin
AppPurchase.getInstance().getPrice(productId)                        // INAP formatted price
AppPurchase.getInstance().getPriceSub(productId)                     // sub regular price
AppPurchase.getInstance().getIntroductorySubPrice(productId)         // first paid intro phase
AppPurchase.getInstance().getIntroductorySubPrice(productId, offerId) // target a specific offer
AppPurchase.getInstance().getTrialPeriod(productId)                  // e.g. "P3D"
AppPurchase.getInstance().getPeriod(productId)                       // billing period "P1M"
AppPurchase.getInstance().getPriceWithCurrency(productId, type)      // formatted with symbol
AppPurchase.getInstance().getPriceWithCurrency(productId, type, 0.5) // at 50 % of regular price
```

---

### Consumable products

Mark `consume = true` in `PurchaseItem` to auto-consume on purchase. To manually consume (e.g. after granting coins server-side):

```kotlin
AppPurchase.getInstance().consumePurchase(BuildConfig.IAP_COINS)
```

---

### Dev / test mode

When `AppUtil.VARIANT_DEV = true`, `purchase()` and `subscribe()` open `PurchaseDevBottomSheet` instead of launching the real billing flow. Tapping **Purchase / Subscribe** fires a `PurchaseEvent.Success` with a synthetic order ID, so the rest of your flow runs end-to-end without a real transaction.

A test product (`android.test.purchased`) is automatically appended to the product list in dev mode.

---

### Revenue tracking helpers

```kotlin
AppPurchase.getInstance().setEnableTrackingRevenue(true)  // opt-in flag read by your analytics layer
AppPurchase.getInstance().setDiscount(0.5)                 // store a sale factor for price display
val discount = AppPurchase.getInstance().getDiscount()
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
