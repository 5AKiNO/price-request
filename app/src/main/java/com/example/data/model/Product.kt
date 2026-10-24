package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.MathUtils
import java.util.UUID

@Immutable
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["codigo"]),
        Index(value = ["descripcion"])
    ]
)
data class Product(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val codigo: String? = null,
    val codigoAlternativo: String? = null,
    val descripcion: String,
    val precioCosto: Long? = null,
    val precioVenta: Long? = null,
    val porcentajePersonalizado: Int? = null,
    val porcentajeRealRedondeado: Int? = null,
    val estadoRegistro: String = "COMPLETO",
    val notaRedondeo: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    // Pre-computed search index for 0-allocation instant search on low-end phones
    val searchIndex: String = buildString {
        if (!codigo.isNullOrBlank()) append(MathUtils.normalizarConEnie(codigo)).append(' ')
        if (!codigoAlternativo.isNullOrBlank()) append(MathUtils.normalizarConEnie(codigoAlternativo)).append(' ')
        append(MathUtils.normalizarConEnie(descripcion)).append(' ')
        if (precioCosto != null) append(precioCosto).append(' ')
        if (precioVenta != null) append(precioVenta)
    }
)
