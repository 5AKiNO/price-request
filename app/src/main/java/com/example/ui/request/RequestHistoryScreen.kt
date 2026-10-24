package com.example.ui.request

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PriceRequest
import com.example.ui.theme.getContrastingTextColor
import com.example.ui.theme.getRequestTypeColor
import com.example.ui.theme.getRequestTypeContainerColor
import com.example.util.ImageUtils
import com.example.util.MathUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RequestHistoryScreen(
    viewModel: RequestViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = screenTextColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Historial de Solicitudes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = screenTextColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.allRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Sin solicitudes registradas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = screenTextColor
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = uiState.allRequests,
                    key = { it.id },
                    contentType = { "history_card" }
                ) { request ->
                    RequestHistoryCard(
                        request = request,
                        onShareAgain = {
                            val uri = request.imagenCompuestaUri?.let { Uri.parse(it) }
                            if (uri != null) {
                                ImageUtils.shareImageOnlyToWhatsApp(context, uri)
                            }
                        },
                        onDelete = { viewModel.deleteRequest(request) }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestHistoryCard(
    request: PriceRequest,
    onShareAgain: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = getRequestTypeColor(request.tipoId)
    val cardBg = getRequestTypeContainerColor(request.tipoId)
    val textOnCardColor = getContrastingTextColor(cardColor)
    val textOnCardBg = getContrastingTextColor(cardBg)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${request.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, cardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = cardBg,
                    border = BorderStroke(1.5.dp, cardColor),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (request.tipoId == 2) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = textOnCardBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (request.tipoId == 3) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = textOnCardBg, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = request.titulo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                    }
                }

                val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(request.timestamp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nombre Producto:",
                style = MaterialTheme.typography.labelSmall,
                color = textOnCardBg,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "· ${request.descripcion}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textOnCardBg
            )

            // Variedades adicionales con puntos igual que en la imagen
            if (!request.variedadesMismoCosto.isNullOrBlank()) {
                val lines = request.variedadesMismoCosto.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                lines.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textOnCardBg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val code = request.codigoProducto ?: request.codigo
            if (!code.isNullOrBlank()) {
                Text(
                    text = "Código: $code",
                    style = MaterialTheme.typography.bodySmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!request.stockAnterior.isNullOrBlank() || !request.recargaStock.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val ant = request.stockAnterior ?: "---"
                val rec = request.recargaStock ?: "---"
                Text(
                    text = "Stock Anterior: $ant  |  Recarga: $rec",
                    style = MaterialTheme.typography.bodySmall,
                    color = textOnCardBg,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar precios en el orden exacto según el tipo de solicitud con contraste según el fondo
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (request.tipoId) {
                    2, 3 -> {
                        val cAnt = request.precioCostoAnterior ?: request.precioCostoNuevo
                        val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                        val pAnt = request.porcentajeAnteriorRedondeado
                            ?: MathUtils.calcularPorcentajeRedondeado(cAnt, vAnt)

                        Text(
                            text = "Precio de Costo Anterior Gs.: ${MathUtils.formatearGuaranies(cAnt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Costo Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                    4, 5 -> {
                        val vAnt = request.precioVentaAnterior ?: request.precioVentaNuevo
                        val pAnt = request.porcentajeAnteriorRedondeado
                            ?: MathUtils.calcularPorcentajeRedondeado(request.precioCostoNuevo, vAnt)

                        Text(
                            text = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Anterior Gs.: ${MathUtils.formatearGuaranies(vAnt)} ($pAnt%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Nuevo Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                    else -> {
                        Text(
                            text = "Precio de Costo Gs.: ${MathUtils.formatearGuaranies(request.precioCostoNuevo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textOnCardBg
                        )
                        Text(
                            text = "Precio de Venta Gs.: ${MathUtils.formatearGuaranies(request.precioVentaNuevo)} (${request.porcentajeNuevoRedondeado}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textOnCardBg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = textOnCardBg)
                }
                Button(
                    onClick = onShareAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardColor,
                        contentColor = textOnCardColor
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = textOnCardColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reenviar Ficha a WhatsApp", color = textOnCardColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
