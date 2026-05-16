import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ads.app.gmasdk"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

configurations.all {
    exclude(group = "com.google.android.gms", module = "play-services-ads-api")
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    api("androidx.multidex:multidex:2.0.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Google Mobile Ads Next-Gen SDK
    api("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.0.1")
    api("com.google.android.gms:play-services-ads-identifier:18.3.0")
    api("com.google.android.gms:play-services-appset:16.1.0")
    api("com.google.android.gms:play-services-basement:18.10.0")
    api("com.google.android.ump:user-messaging-platform:4.0.0")
    api("com.google.android.play:review-ktx:2.0.2")

    // Firebase
    api("com.google.firebase:firebase-analytics:23.2.0")

    // Billing
    api("com.android.billingclient:billing:8.3.0")

    // Analytics SDKs
    api("com.adjust.sdk:adjust-android:5.6.1")
    api("com.appsflyer:af-android-sdk:6.17.5")
    api("com.appsflyer:adrevenue:6.9.1")
    api("com.facebook.android:facebook-android-sdk:18.2.3")

    // Mediation Adapters
    api("com.google.ads.mediation:facebook:6.21.0.2")
    api("com.facebook.android:audience-network-sdk:6.21.0")
    api("com.google.ads.mediation:mintegral:17.1.51.0")
    api("com.google.ads.mediation:pangle:8.0.0.4.0")
    api("com.google.ads.mediation:unity:4.17.0.0")

    // UI Libraries
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")

    // Utilities
    implementation("com.google.guava:guava:27.1-android")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.3.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Arrow-kt
    api("io.arrow-kt:arrow-core:2.2.2.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
