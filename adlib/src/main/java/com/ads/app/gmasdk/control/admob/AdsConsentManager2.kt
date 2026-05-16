package com.ads.app.gmasdk.control.admob

import android.app.Activity
import android.content.Context
import com.ads.app.gmasdk.control.util.AppLogger
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AdsConsentManager2 private constructor(context: Context) {

    private val TAG = "AdsConsentManager2"
    private val appContext: Context = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    companion object {
        @Volatile
        private var instance: AdsConsentManager2? = null

        fun getInstance(context: Context): AdsConsentManager2 =
            instance ?: synchronized(this) {
                instance ?: AdsConsentManager2(context).also { instance = it }
            }
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    val hasUserConsented: Boolean
        get() {
            val prefs = appContext.getSharedPreferences(
                "${appContext.packageName}_preferences", Context.MODE_PRIVATE
            )
            if (prefs.getInt("IABTCF_gdprApplies", -1) == 0) return true
            val purposeConsents = prefs.getString("IABTCF_PurposeConsents", null)
                ?: return false
            AppLogger.d(TAG, "[ConsentManager] IABTCF_PurposeConsents=\"$purposeConsents")
            return purposeConsents.isNotEmpty() && purposeConsents[0] == '1'
        }

    val wasConsentFormPreviouslyShown: Boolean
        get() = appContext.getSharedPreferences(
            "${appContext.packageName}_preferences", Context.MODE_PRIVATE
        ).contains("IABTCF_PurposeConsents")

    fun resetConsentState() {
        AppLogger.d(TAG, "[ConsentManager] resetConsentState() called — form will show again")
        consentInformation.reset()
    }

    suspend fun requestConsent(
        activity: Activity,
        isDebug: Boolean = false,
        idTestDevice: String = ""
    ) {
        if (hasUserConsented) {
            AppLogger.d(TAG, "[ConsentManager] requestConsent: already consented, skipping")
            return
        }
        if (wasConsentFormPreviouslyShown) {
            resetConsentState()
        }
        suspendCancellableCoroutine { continuation ->
            gatherConsent(activity, isDebug, idTestDevice) { error ->
                continuation.resume(Unit)
            }
        }
    }

    private fun gatherConsent(
        activity: Activity,
        isDebug: Boolean,
        idTestDevice: String,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        val params = if (isDebug) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(idTestDevice)
                .build()
            ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build()
        } else {
            ConsentRequestParameters.Builder().build()
        }
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { loadAndShowConsentFormIfRequired(activity, onConsentGatheringCompleteListener) },
            { requestConsentError -> onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError) }
        )
    }

    private fun loadAndShowConsentFormIfRequired(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            onConsentGatheringCompleteListener.consentGatheringComplete(formError)
        }
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }
}
