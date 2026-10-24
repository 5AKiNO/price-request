package com.example.ui.catalog

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.ui.theme.ClaudiaBluePrimary
import com.example.ui.theme.Tipo1Verde
import com.example.ui.theme.getContrastingTextColor

@Composable
fun WhatsAppExportDialog(
    selectedProducts: List<Product>,
    onDismiss: () -> Unit,
    buildTextFunction: (products: List<Product>, codigo: Boolean, costo: Boolean, venta: Boolean, margen: Boolean) -> String
) {
    val context = LocalContext.current

    var includeCodigo by remember { mutableStateOf(true) }
    var includeCosto by remember { mutableStateOf(true) }
    var includeVenta by remember { mutableStateOf(true) }
    var includeMargen by remember { mutableStateOf(true) }

    val previewText = remember(selectedProducts, includeCodigo, includeCosto, includeVenta, includeMargen) {
        buildTextFunction(selectedProducts, includeCodigo, includeCosto, includeVenta, includeMargen)
    }

    val dialogBg = MaterialTheme.colorScheme.surface
    val dialogTextColor = getContrastingTextColor(dialogBg)
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = ClaudiaBluePrimary,
        checkmarkColor = getContrastingTextColor(ClaudiaBluePrimary),
        uncheckedColor = dialogTextColor
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogTextColor,
        textContentColor = dialogTextColor,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exportar a WhatsApp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = dialogTextColor)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Campos a incluir en el mensaje (${selectedProducts.size} seleccionados):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = dialogTextColor
                )

                // Selectores de campos visibles
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeCodigo, onCheckedChange = { includeCodigo = it }, colors = checkboxColors)
                    Text("Incluir Código", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeCosto, onCheckedChange = { includeCosto = it }, colors = checkboxColors)
                    Text("Incluir Precio de Costo", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeVenta, onCheckedChange = { includeVenta = it }, colors = checkboxColors)
                    Text("Incluir Precio de Venta", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMargen, onCheckedChange = { includeMargen = it }, colors = checkboxColors)
                    Text("Incluir % Margen", style = MaterialTheme.typography.bodyMedium, color = dialogTextColor, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vista previa del mensaje:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextColor
                )

                // Preview box (light background -> black text)
                val previewBg = Color(0xFFECE5DD)
                val previewTextColor = getContrastingTextColor(previewBg)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = previewText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = previewTextColor
                    )
                }
            }
        },
        confirmButton = {
            val sendBtnBg = Tipo1Verde
            val sendBtnText = getContrastingTextColor(sendBtnBg)
            Button(
                onClick = {
                    shareTextToWhatsApp(context, previewText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = sendBtnBg,
                    contentColor = sendBtnText
                ),
                modifier = Modifier.testTag("whatsapp_button_enviar")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = sendBtnText)
                Text(" Enviar por WhatsApp", color = sendBtnText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.5.dp, ClaudiaBluePrimary)
            ) {
                Text("Cancelar", color = dialogTextColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun shareTextToWhatsApp(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    val whatsappIntent = Intent(intent).apply {
        `package` = "com.whatsapp"
    }

    val launchIntent = if (whatsappIntent.resolveActivity(context.packageManager) != null) {
        whatsappIntent
    } else {
        Intent.createChooser(intent, "Compartir productos por WhatsApp")
    }

    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launchIntent)
}
