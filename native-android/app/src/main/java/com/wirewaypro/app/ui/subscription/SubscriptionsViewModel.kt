package com.wirewaypro.app.ui.subscription

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.wirewaypro.app.data.entitlements.PlayEntitlements
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubProductUi(
    val id: String,
    val title: String,
    val price: String,
    val tier: String, // "Pro" | "Teams" | "Elite"
)

data class SubsUiState(
    val connecting: Boolean = true,
    val available: Boolean = false,
    val products: List<SubProductUi> = emptyList(),
    val status: String? = null, // graceful "unavailable"/"active" message
)

/**
 * Play Billing subscriptions. Fully fault-tolerant: if billing is unavailable
 * (no Play, not signed in) or no products are configured in Play Console, it shows
 * a clear "unavailable" state instead of crashing. Product IDs must be created in
 * the Play Console; needs on-device testing with a license-tester account.
 */
@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val entitlements: PlayEntitlements,
) : ViewModel() {

    // Three tiers (Pro / Teams / Elite), each with a monthly + yearly product. These
    // IDs must be created as subscriptions in the Play Console (a merchant account is
    // required first). Order here also drives display order. Elite unlocks AI Takeoff.
    private val productIds = listOf(
        "wireway_pro_monthly", "wireway_pro_yearly",
        "wireway_teams_monthly", "wireway_teams_yearly",
        "wireway_elite_monthly", "wireway_elite_yearly",
    )

    /** Maps a product ID to its tier label for grouping + (future) entitlement sync. */
    private fun tierOf(productId: String): String = when {
        productId.startsWith("wireway_elite") -> "Elite"
        productId.startsWith("wireway_teams") -> "Teams"
        else -> "Pro"
    }

    private val detailsById = mutableMapOf<String, ProductDetails>()

    private val _state = MutableStateFlow(SubsUiState())
    val state: StateFlow<SubsUiState> = _state.asStateFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> {
                // CRITICAL: acknowledge within 3 days or Google auto-refunds the purchase.
                viewModelScope.launch { purchases.forEach { acknowledgeIfNeeded(it) } }
                // Unlock gated features app-wide right away — no waiting on backend sync.
                entitlements.refresh()
                _state.update { it.copy(status = "Subscription active — thank you!") }
            }
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                _state.update { it.copy(status = null) }
            else -> _state.update { it.copy(status = "Purchase didn't complete.") }
        }
    }

    /** Acknowledge a completed SUBS purchase (idempotent) so it isn't auto-refunded. */
    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        runCatching { billing.acknowledgePurchase(params) }
    }

    /** On (re)connect, acknowledge any purchase the listener may have missed (app killed mid-flow). */
    private fun reconcilePurchases() {
        viewModelScope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = runCatching { billing.queryPurchasesAsync(params) }.getOrNull() ?: return@launch
            result.purchasesList.forEach { acknowledgeIfNeeded(it) }
            if (result.purchasesList.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                entitlements.refresh()
                _state.update { it.copy(status = "Subscription active") }
            }
        }
    }

    private val billing: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener(purchasesListener)
        .build()

    init {
        connect()
    }

    private fun connect() {
        runCatching {
            billing.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProducts()
                        reconcilePurchases()
                    } else {
                        _state.update { it.copy(connecting = false, available = false, status = "Subscriptions are unavailable on this device.") }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _state.update { it.copy(connecting = false, available = false, status = "Subscriptions are unavailable right now.") }
                }
            })
        }.onFailure {
            _state.update { it.copy(connecting = false, available = false, status = "Subscriptions are unavailable on this device.") }
        }
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()
        viewModelScope.launch {
            val result = runCatching { billing.queryProductDetails(params) }.getOrNull()
            val list = result?.productDetailsList.orEmpty()
            if (list.isEmpty()) {
                _state.update { it.copy(connecting = false, available = false, status = "No subscription plans are configured yet.") }
                return@launch
            }
            detailsById.clear()
            val ui = list.map { pd ->
                detailsById[pd.productId] = pd
                val phase = pd.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.firstOrNull()
                SubProductUi(
                    id = pd.productId,
                    title = pd.title.ifBlank { pd.productId },
                    price = phase?.formattedPrice.orEmpty(),
                    tier = tierOf(pd.productId),
                )
            }
                // Keep a stable Pro → Teams → Elite (monthly before yearly) order.
                .sortedBy { productIds.indexOf(it.id) }
            _state.update { it.copy(connecting = false, available = true, products = ui, status = null) }
        }
    }

    fun purchase(activity: Activity, productId: String) {
        val pd = detailsById[productId] ?: return
        val offerToken = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        runCatching { billing.launchBillingFlow(activity, params) }
    }

    override fun onCleared() {
        runCatching { billing.endConnection() }
    }
}
