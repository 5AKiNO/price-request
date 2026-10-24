package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Immutable
@Entity(tableName = "price_requests")
data class PriceRequest(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val tipoId: Int,
    val titulo: String,
    val imagenRecortadaUri: String? = null,
    val imagenCompuestaUri: String? = null,
    val productoYaRegistrado: Boolean = false,
    val codigo: String? = null,
    val codigoProducto: String? = null,
    val descripcion: String,
    val porcentajeDeseado: Int? = null,
    val precioCostoAnterior: Long? = null,
    val precioCostoNuevo: Long = 0L,
    val precioVentaAnterior: Long? = null,
    val porcentajeAnteriorRedondeado: Int? = null,
    val precioVentaNuevo: Long = 0L,
    val porcentajeNuevoRedondeado: Int = 0,
    val stockAnterior: String? = null,
    val recargaStock: String? = null,
    val variedadesMismoCosto: String? = null
)
