package com.wirewaypro.app.data.entitlements

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide, read-only view of the Play subscriptions this device owns, kept as a
 * [StateFlow] of product ids for [TierService] to fold into the effective tier.
 *
 * Fault-tolerant by design: with no Play services / not signed in / billing
 * errors it simply stays empty, and the tier falls back to the server profile —
 * billing problems must never take features away from a paying user, only fail
 * to add the instant-unlock path.
 */
@Singleton
class PlayEntitlements @Inject constructor(@ApplicationContext context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _ownedProducts = MutableStateFlow<Set<String>>(emptySet())

    /** Product ids of active (PURCHASED) Play subscriptions on this device. */
    val ownedProducts: StateFlow<Set<String>> = _ownedProducts.asStateFlow()

    // Purchases made through OTHER BillingClient instances (the Subscription
    // screen's purchase flow) don't hit this listener — that screen calls
    // refresh() on completion instead. This listener only covers flows Play
    // routes to every registered client.
    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            publish(purchases)
        }
    }

    private val billing: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener(listener)
        .build()

    init {
        refresh()
    }

    /** Re-query owned subscriptions (connects on demand; safe to call anytime). */
    fun refresh() {
        if (billing.isReady) {
            query()
            return
        }
        runCatching {
            billing.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) query()
                }

                override fun onBillingServiceDisconnected() {
                    // Next refresh() reconnects; owned set keeps its last value.
                }
            })
        }
    }

    private fun query() {
        scope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = runCatching { billing.queryPurchasesAsync(params) }.getOrNull() ?: return@launch
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                publish(result.purchasesList)
            }
        }
    }

    private fun publish(purchases: List<Purchase>) {
        _ownedProducts.value = purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { it.products }
            .toSet()
    }
}
