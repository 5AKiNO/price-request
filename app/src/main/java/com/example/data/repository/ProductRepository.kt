package com.example.data.repository

import android.content.Context
import com.example.data.local.FastBinaryStore
import com.example.data.local.PriceRequestDao
import com.example.data.local.ProductDao
import com.example.data.model.PriceRequest
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val binaryStore: FastBinaryStore
) {

    // Constructor secundario de compatibilidad
    constructor(
        context: Context,
        productDao: ProductDao? = null,
        priceRequestDao: PriceRequestDao? = null
    ) : this(FastBinaryStore.getInstance(context))

    val allProducts: Flow<List<Product>> = binaryStore.productsFlow
    val allRequests: Flow<List<PriceRequest>> = binaryStore.requestsFlow
    val workplaceName: Flow<String> = binaryStore.workplaceNameFlow

    suspend fun saveWorkplaceName(name: String) {
        binaryStore.saveWorkplaceName(name)
    }

    suspend fun saveProduct(product: Product) {
        binaryStore.saveProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        binaryStore.deleteProduct(product.id)
    }

    suspend fun clearAllProducts() {
        binaryStore.clearAllProducts()
    }

    fun getProductById(id: String): Product? {
        return binaryStore.getProductById(id)
    }

    suspend fun savePriceRequest(request: PriceRequest) {
        binaryStore.savePriceRequest(request)
    }

    suspend fun deletePriceRequest(request: PriceRequest) {
        binaryStore.deletePriceRequest(request.id)
    }
}
