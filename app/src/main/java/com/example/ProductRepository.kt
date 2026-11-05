package com.example

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val binaryStore: FastBinaryStore
) {

    constructor(context: Context) : this(FastBinaryStore.getInstance(context))

    val allProducts: Flow<List<Product>> = binaryStore.productsFlow
    val allRequests: Flow<List<PriceRequest>> = binaryStore.requestsFlow
    val workplaceName: Flow<String> = binaryStore.workplaceNameFlow
    val allDistributors: Flow<List<DistributorInvoice>> = binaryStore.distributorsFlow
    val activeDistributorId: Flow<String?> = binaryStore.activeDistributorIdFlow

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

    suspend fun setActiveDistributorId(distributorId: String?) {
        binaryStore.setActiveDistributorId(distributorId)
    }

    suspend fun saveDistributor(distributor: DistributorInvoice) {
        binaryStore.saveDistributor(distributor)
    }

    suspend fun deleteDistributor(distributorId: String) {
        binaryStore.deleteDistributor(distributorId)
    }

    suspend fun upsertDistributorItem(distributorId: String, item: DistributorItem) {
        binaryStore.upsertDistributorItem(distributorId, item)
    }

    suspend fun deleteDistributorItem(distributorId: String, itemId: String) {
        binaryStore.deleteDistributorItem(distributorId, itemId)
    }

    suspend fun shiftDistributorItemOrder(distributorId: String, itemId: String, delta: Int) {
        binaryStore.shiftDistributorItemOrder(distributorId, itemId, delta)
    }

    suspend fun syncDistributorItemsToCatalog(items: List<DistributorItem>): Int {
        return binaryStore.syncDistributorItemsToCatalog(items)
    }

    suspend fun exportCatalogBinary(): ByteArray {
        return binaryStore.exportCatalogBinary()
    }

    suspend fun importCatalogBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> {
        return binaryStore.importCatalogBinary(bytes, replaceAll)
    }

    suspend fun exportRequestsBinary(): ByteArray {
        return binaryStore.exportRequestsBinary()
    }

    suspend fun importRequestsBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> {
        return binaryStore.importRequestsBinary(bytes, replaceAll)
    }

    suspend fun exportDistributorsBinary(): ByteArray {
        return binaryStore.exportDistributorsBinary()
    }

    suspend fun importDistributorsBinary(bytes: ByteArray, replaceAll: Boolean): Result<Int> {
        return binaryStore.importDistributorsBinary(bytes, replaceAll)
    }
}
