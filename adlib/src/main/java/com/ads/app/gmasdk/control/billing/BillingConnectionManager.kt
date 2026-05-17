package com.ads.app.gmasdk.control.billing

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class BillingConnectionManager {

    private var billingClient: BillingClient? = null

    @Volatile var isAvailable: Boolean = false
        private set

    @Volatile var isInitBillingFinish: Boolean = false
        private set

    private val _state: MutableStateFlow<BillingState> = MutableStateFlow(BillingState.Disconnected)
    val state: StateFlow<BillingState> = _state.asStateFlow()

    private var reconnectAttempts: Int = 0
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var stateListener: BillingClientStateListener? = null

    fun connect(
        application: Application,
        purchasesUpdatedListener: PurchasesUpdatedListener,
        onSetupFinished: (BillingResult, Boolean) -> Unit
    ) {
        _state.value = BillingState.Connecting
        billingClient = BillingClient.newBuilder(application as Context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()

        var isFirstSetup = true
        stateListener = object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: code=${billingResult.responseCode}")
                reconnectAttempts = 0
                isInitBillingFinish = true
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isAvailable = true
                    _state.value = BillingState.Connected
                } else {
                    isAvailable = false
                    _state.value = BillingState.Error(billingResult.responseCode, billingResult.debugMessage)
                }
                onSetupFinished(billingResult, isFirstSetup)
                isFirstSetup = false
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected: attempt=$reconnectAttempts")
                isAvailable = false
                _state.value = BillingState.Disconnected
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++
                    val delayMs = (reconnectAttempts * 2_000L).coerceAtMost(30_000L)
                    reconnectHandler.postDelayed({
                        val sl = stateListener
                        if (sl != null) {
                            billingClient?.startConnection(sl)
                        }
                    }, delayMs)
                }
            }
        }
        val sl = stateListener
        if (sl != null) {
            billingClient?.startConnection(sl)
        }
    }

    internal suspend fun awaitConnection(): BillingClient? {
        _state.first { it is BillingState.Connected }
        return billingClient
    }

    fun getBillingClient(): BillingClient? = billingClient

    companion object {
        private const val TAG = "BillingConnectionMgr"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
}
