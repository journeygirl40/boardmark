package com.boardmark.app.billing

import android.app.Activity
import android.content.Context
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
import com.boardmark.app.ads.AdFreeAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Play Consoleにこのプロダクト ID(管理対象商品・買い切り)を作成し、価格を500円に設定しておくこと。 */
const val AD_FREE_PRODUCT_ID = "ad_free_upgrade"

/**
 * 広告非表示アップグレード(買い切り)のPlay Billing連携。アプリ全体で1つだけ接続を張り、
 * 起動時の所有権確認・購入フロー起動・購入承認(acknowledge)をここに集約する。
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : PurchasesUpdatedListener {

    // Billing接続前でもすぐ判定できるよう、ローカルに保存済みの状態でまず初期化しておく。
    private val _isAdFree = MutableStateFlow(AdFreeAccess.isAdFree(appContext))
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(AD_FREE_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList.firstOrNull()
            }
        }
    }

    /** 起動のたびにPlay側の実際の所有権で照合し直す(端末の再インストールや返金にも追従するため)。 */
    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases, reconcileIfMissing = true)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = _productDetails.value ?: return
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases, reconcileIfMissing = false)
        }
    }

    private fun handlePurchases(purchases: List<Purchase>, reconcileIfMissing: Boolean) {
        val owned = purchases.any { purchase ->
            AD_FREE_PRODUCT_ID in purchase.products && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (owned) {
            AdFreeAccess.setAdFree(appContext, true)
            _isAdFree.value = true
            purchases
                .filter {
                    AD_FREE_PRODUCT_ID in it.products &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        !it.isAcknowledged
                }
                .forEach { purchase ->
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) {}
                }
        } else if (reconcileIfMissing) {
            // 端末に保存していた「購入済み」がPlay側の実際の所有権と食い違う場合(返金など)は解除する。
            AdFreeAccess.setAdFree(appContext, false)
            _isAdFree.value = false
        }
    }
}
