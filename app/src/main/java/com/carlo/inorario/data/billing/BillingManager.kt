package com.carlo.inorario.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.carlo.inorario.data.local.DataStoreManager
import kotlinx.coroutines.*


class BillingManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val onPurchaseSuccess: () -> Unit,
    private val onPurchaseError: (String) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val TAG = "BillingManager"
        const val PRODUCT_ID_CAPPUCCINO = "tip.cappuccino"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var productDetails: ProductDetails? = null

    // --- Connection ---

    fun startConnection() {
        if (billingClient.isReady) {
            scope.launch { queryOwnedPurchases() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient connesso con successo.")
                    scope.launch {
                        queryProductDetails()
                        queryOwnedPurchases()
                    }
                } else {
                    Log.e(TAG, "Errore connessione BillingClient: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient disconnesso. Prossima chiamata riconnetterà.")
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
        scope.cancel()
    }

    // --- Product Query ---

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_CAPPUCCINO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            productDetails = result.productDetailsList?.firstOrNull()
            Log.d(TAG, "Prodotti ottenuti: ${result.productDetailsList?.size}")
        } else {
            Log.e(TAG, "Errore query prodotti: ${result.billingResult.debugMessage}")
        }
    }

    // --- Purchase Launch ---

    fun launchBillingFlow(activity: Activity) {
        scope.launch {
            // Reconnect if needed
            if (!billingClient.isReady) {
                Log.w(TAG, "BillingClient non pronto, riprovo la connessione...")
                startConnection()
                delay(1500)
            }

            val details = productDetails ?: run {
                queryProductDetails()
                productDetails
            } ?: run {
                Log.e(TAG, "Dettagli prodotto non disponibili.")
                withContext(Dispatchers.Main) {
                    onPurchaseError("Prodotto non disponibile. Verifica la connessione e riprova.")
                }
                return@launch
            }

            val offerToken = details.oneTimePurchaseOfferDetails?.formattedPrice?.let { _ ->
                ""
            } ?: ""

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build()
                    )
                )
                .build()

            withContext(Dispatchers.Main) {
                val result = billingClient.launchBillingFlow(activity, billingFlowParams)
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Errore avvio BillingFlow: ${result.debugMessage}")
                    onPurchaseError("Impossibile avviare il pagamento: ${result.debugMessage}")
                }
            }
        }
    }

    // --- Purchases Updated (callback) ---

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Acquisto annullato dall'utente.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Prodotto già acquistato.")
                scope.launch {
                    dataStoreManager.saveHasSupport(true)
                    withContext(Dispatchers.Main) { onPurchaseSuccess() }
                }
            }
            else -> {
                Log.e(TAG, "Errore acquisto: ${result.debugMessage}")
                onPurchaseError("Errore durante l'acquisto: ${result.debugMessage}")
            }
        }
    }

    // --- Handle & Acknowledge Purchase ---

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val result = billingClient.acknowledgePurchase(params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Acquisto confermato (acknowledged).")
            } else {
                Log.e(TAG, "Errore acknowledgePurchase: ${result.debugMessage}")
            }
        }

        if (purchase.products.contains(PRODUCT_ID_CAPPUCCINO)) {
            dataStoreManager.saveHasSupport(true)
            withContext(Dispatchers.Main) { onPurchaseSuccess() }
            Log.d(TAG, "Premium sbloccato con successo!")
        }
    }

    // --- Restore existing purchases ---

    suspend fun queryOwnedPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val owned = result.purchasesList.any { p ->
                p.products.contains(PRODUCT_ID_CAPPUCCINO) && p.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            dataStoreManager.saveHasSupport(owned)
            Log.d(TAG, "Ripristino acquisti: hasSupport=$owned")
        } else {
            Log.e(TAG, "Errore query acquisti posseduti: ${result.billingResult.debugMessage}")
        }
    }
}
