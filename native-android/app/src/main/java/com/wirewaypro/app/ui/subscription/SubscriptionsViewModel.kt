package com.wirewaypro.app.ui.subscription

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubProductUi(val id: String, val title: String, val price: String)

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
) : ViewModel() {

    private val productIds = listOf("wireway_pro_monthly", "wireway_pro_yearly")
    private val detailsById = mutableMapOf<String, ProductDetails>()

    private val _state = MutableStateFlow(SubsUiState())
    val state: StateFlow<SubsUiState> = _state.asStateFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null ->
                _state.update { it.copy(status = "Subscription active — thank you!") }
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                _state.update { it.copy(status = null) }
            else -> _state.update { it.copy(status = "Purchase didn't complete.") }
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
                )
            }
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
