package com.example.data.local

import android.content.Context
import com.example.data.model.PriceRequest
import com.example.data.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Motor de almacenamiento binario ultraligero en memoria y archivo binario directo (.bin).
 * Diseñado específicamente para funcionar con respuesta instantánea (0ms en UI)
 * en teléfonos de gama muy baja sin sobrecarga de SQLite ni reflexión JSON.
 */
class FastBinaryStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val productsMutex = Mutex()
    private val requestsMutex = Mutex()

    private val productsFile = File(appContext.filesDir, "claudia_products_v1.bin")
    private val requestsFile = File(appContext.filesDir, "claudia_requests_v1.bin")

    private val _productsFlow = MutableStateFlow<List<Product>>(emptyList())
    val productsFlow: StateFlow<List<Product>> = _productsFlow.asStateFlow()

    private val _requestsFlow = MutableStateFlow<List<PriceRequest>>(emptyList())
    val requestsFlow: StateFlow<List<PriceRequest>> = _requestsFlow.asStateFlow()

    init {
        ioScope.launch {
            _productsFlow.value = readProductsFromDisk()
            _requestsFlow.value = readRequestsFromDisk()
        }
    }

    suspend fun saveProduct(product: Product) {
        val updated = productsMutex.withLock {
            val current = _productsFlow.value.toMutableList()
            val existingIdx = current.indexOfFirst { it.id == product.id }
            if (existingIdx >= 0) {
                current[existingIdx] = product
            } else {
                current.add(product)
            }
            current.sortBy { it.descripcion.lowercase() }
            val snapshot = current.toList()
            _productsFlow.value = snapshot
            snapshot
        }
        ioScope.launch {
            productsMutex.withLock {
                writeProductsToDisk(updated)
            }
        }
    }

    suspend fun deleteProduct(productId: String) {
        val updated = productsMutex.withLock {
            val snapshot = _productsFlow.value.filterNot { it.id == productId }
            _productsFlow.value = snapshot
            snapshot
        }
        ioScope.launch {
            productsMutex.withLock {
                writeProductsToDisk(updated)
            }
        }
    }

    suspend fun clearAllProducts() {
        productsMutex.withLock {
            _productsFlow.value = emptyList()
            withContext(Dispatchers.IO) {
                if (productsFile.exists()) productsFile.delete()
            }
        }
    }

    fun getProductById(id: String): Product? {
        return _productsFlow.value.firstOrNull { it.id == id }
    }

    suspend fun savePriceRequest(request: PriceRequest) {
        val updated = requestsMutex.withLock {
            val current = _requestsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == request.id }
            if (idx >= 0) {
                current[idx] = request
            } else {
                current.add(0, request)
            }
            val snapshot = current.toList()
            _requestsFlow.value = snapshot
            snapshot
        }
        ioScope.launch {
            requestsMutex.withLock {
                writeRequestsToDisk(updated)
            }
        }
    }

    suspend fun deletePriceRequest(requestId: String) {
        val updated = requestsMutex.withLock {
            val snapshot = _requestsFlow.value.filterNot { it.id == requestId }
            _requestsFlow.value = snapshot
            snapshot
        }
        ioScope.launch {
            requestsMutex.withLock {
                writeRequestsToDisk(updated)
            }
        }
    }

    private fun readProductsFromDisk(): List<Product> {
        if (!productsFile.exists()) return emptyList()
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(productsFile))).use { dis ->
                val magic = dis.readInt()
                if (magic != MAGIC_PRODUCTS) return emptyList()
                val count = dis.readInt()
                val list = ArrayList<Product>(count)
                for (i in 0 until count) {
                    val id = dis.readUTF()
                    val codigo = dis.readNullableUTF()
                    val codigoAlt = dis.readNullableUTF()
                    val descripcion = dis.readUTF()
                    val precioCosto = dis.readNullableLong()
                    val precioVenta = dis.readNullableLong()
                    val pctPersonalizado = dis.readNullableInt()
                    val pctReal = dis.readNullableInt()
                    val estado = dis.readUTF()
                    val nota = dis.readNullableUTF()
                    val updatedAt = dis.readLong()
                    list.add(
                        Product(
                            id = id,
                            codigo = codigo,
                            codigoAlternativo = codigoAlt,
                            descripcion = descripcion,
                            precioCosto = precioCosto,
                            precioVenta = precioVenta,
                            porcentajePersonalizado = pctPersonalizado,
                            porcentajeRealRedondeado = pctReal,
                            estadoRegistro = estado,
                            notaRedondeo = nota,
                            updatedAt = updatedAt
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeProductsToDisk(products: List<Product>) {
        try {
            val tempFile = File(appContext.filesDir, "claudia_products_v1.tmp")
            DataOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { dos ->
                dos.writeInt(MAGIC_PRODUCTS)
                dos.writeInt(products.size)
                for (p in products) {
                    dos.writeUTF(p.id)
                    dos.writeNullableUTF(p.codigo)
                    dos.writeNullableUTF(p.codigoAlternativo)
                    dos.writeUTF(p.descripcion)
                    dos.writeNullableLong(p.precioCosto)
                    dos.writeNullableLong(p.precioVenta)
                    dos.writeNullableInt(p.porcentajePersonalizado)
                    dos.writeNullableInt(p.porcentajeRealRedondeado)
                    dos.writeUTF(p.estadoRegistro)
                    dos.writeNullableUTF(p.notaRedondeo)
                    dos.writeLong(p.updatedAt)
                }
                dos.flush()
            }
            tempFile.renameTo(productsFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readRequestsFromDisk(): List<PriceRequest> {
        if (!requestsFile.exists()) return emptyList()
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(requestsFile))).use { dis ->
                val magic = dis.readInt()
                if (magic != MAGIC_REQUESTS) return emptyList()
                val count = dis.readInt()
                val list = ArrayList<PriceRequest>(count)
                for (i in 0 until count) {
                    val id = dis.readUTF()
                    val timestamp = dis.readLong()
                    val tipoId = dis.readInt()
                    val titulo = dis.readUTF()
                    val imgRecortada = dis.readNullableUTF()
                    val imgCompuesta = dis.readNullableUTF()
                    val yaReg = dis.readBoolean()
                    val codigo = dis.readNullableUTF()
                    val codigoProd = dis.readNullableUTF()
                    val descripcion = dis.readUTF()
                    val pctDeseado = dis.readNullableInt()
                    val costoAnt = dis.readNullableLong()
                    val costoNuevo = dis.readLong()
                    val ventaAnt = dis.readNullableLong()
                    val pctAnt = dis.readNullableInt()
                    val ventaNueva = dis.readLong()
                    val pctNuevo = dis.readInt()
                    val stockAnt = dis.readNullableUTF()
                    val recargaStock = dis.readNullableUTF()
                    val variedades = dis.readNullableUTF()
                    list.add(
                        PriceRequest(
                            id = id,
                            timestamp = timestamp,
                            tipoId = tipoId,
                            titulo = titulo,
                            imagenRecortadaUri = imgRecortada,
                            imagenCompuestaUri = imgCompuesta,
                            productoYaRegistrado = yaReg,
                            codigo = codigo,
                            codigoProducto = codigoProd,
                            descripcion = descripcion,
                            porcentajeDeseado = pctDeseado,
                            precioCostoAnterior = costoAnt,
                            precioCostoNuevo = costoNuevo,
                            precioVentaAnterior = ventaAnt,
                            porcentajeAnteriorRedondeado = pctAnt,
                            precioVentaNuevo = ventaNueva,
                            porcentajeNuevoRedondeado = pctNuevo,
                            stockAnterior = stockAnt,
                            recargaStock = recargaStock,
                            variedadesMismoCosto = variedades
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun writeRequestsToDisk(requests: List<PriceRequest>) {
        try {
            val tempFile = File(appContext.filesDir, "claudia_requests_v1.tmp")
            DataOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { dos ->
                dos.writeInt(MAGIC_REQUESTS)
                dos.writeInt(requests.size)
                for (r in requests) {
                    dos.writeUTF(r.id)
                    dos.writeLong(r.timestamp)
                    dos.writeInt(r.tipoId)
                    dos.writeUTF(r.titulo)
                    dos.writeNullableUTF(r.imagenRecortadaUri)
                    dos.writeNullableUTF(r.imagenCompuestaUri)
                    dos.writeBoolean(r.productoYaRegistrado)
                    dos.writeNullableUTF(r.codigo)
                    dos.writeNullableUTF(r.codigoProducto)
                    dos.writeUTF(r.descripcion)
                    dos.writeNullableInt(r.porcentajeDeseado)
                    dos.writeNullableLong(r.precioCostoAnterior)
                    dos.writeLong(r.precioCostoNuevo)
                    dos.writeNullableLong(r.precioVentaAnterior)
                    dos.writeNullableInt(r.porcentajeAnteriorRedondeado)
                    dos.writeLong(r.precioVentaNuevo)
                    dos.writeInt(r.porcentajeNuevoRedondeado)
                    dos.writeNullableUTF(r.stockAnterior)
                    dos.writeNullableUTF(r.recargaStock)
                    dos.writeNullableUTF(r.variedadesMismoCosto)
                }
                dos.flush()
            }
            tempFile.renameTo(requestsFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun DataOutputStream.writeNullableUTF(value: String?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeUTF(value)
        }
    }

    private fun DataInputStream.readNullableUTF(): String? {
        return if (readBoolean()) readUTF() else null
    }

    private fun DataOutputStream.writeNullableLong(value: Long?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeLong(value)
        }
    }

    private fun DataInputStream.readNullableLong(): Long? {
        return if (readBoolean()) readLong() else null
    }

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        if (value == null) {
            writeBoolean(false)
        } else {
            writeBoolean(true)
            writeInt(value)
        }
    }

    private fun DataInputStream.readNullableInt(): Int? {
        return if (readBoolean()) readInt() else null
    }

    companion object {
        private const val MAGIC_PRODUCTS = 0x434C5031 // "CLP1"
        private const val MAGIC_REQUESTS = 0x434C5231 // "CLR1"

        @Volatile
        private var INSTANCE: FastBinaryStore? = null

        fun getInstance(context: Context): FastBinaryStore {
            return INSTANCE ?: synchronized(this) {
                val instance = FastBinaryStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
