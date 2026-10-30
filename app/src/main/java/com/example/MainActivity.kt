package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.catalog.CatalogScreen
import com.example.ui.catalog.CatalogViewModel
import com.example.ui.components.FastNavIconItem
import com.example.ui.request.RequestHistoryScreen
import com.example.ui.request.RequestScreen
import com.example.ui.request.RequestViewModel
import com.example.ui.theme.AppPricingTheme
import com.example.ui.theme.BrandBlueContainer
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.getContrastingTextColor

enum class AppScreen {
    CATALOGO,
    NUEVA_SOLICITUD,
    HISTORIAL
}

class MainActivity : ComponentActivity() {

    private val catalogViewModel: CatalogViewModel by viewModels()
    private val requestViewModel: RequestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppPricingTheme {
                MainAppRoot(
                    catalogViewModel = catalogViewModel,
                    requestViewModel = requestViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppRoot(
    catalogViewModel: CatalogViewModel,
    requestViewModel: RequestViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.CATALOGO) }
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = currentScreen != AppScreen.CATALOGO) {
        currentScreen = AppScreen.CATALOGO
    }

    val topBarBg = BrandBluePrimary
    val topBarTextColor = getContrastingTextColor(topBarBg)

    val navBarBg = MaterialTheme.colorScheme.surface
    val navIndicatorBg = BrandBlueContainer

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBg,
            contentColor = screenTextColor,
            topBar = {
                // Barra superior plana y compacta de una sola línea
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarBg)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                currentScreen = AppScreen.CATALOGO
                                catalogViewModel.openWorkplaceDialog()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = topBarTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = catalogState.workplaceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = topBarTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("top_bar_workplace_name")
                        )
                    }

                    IconButton(
                        onClick = {
                            currentScreen = AppScreen.CATALOGO
                            catalogViewModel.openWorkplaceDialog()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_bar_edit_workplace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Cambiar nombre del local",
                            tint = topBarTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    // Barra de navegación inferior plana y ligera de solo iconos (sin textos)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(navBarBg)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        HorizontalDivider(thickness = 1.dp, color = BrandBluePrimary.copy(alpha = 0.25f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FastNavIconItem(
                                selected = currentScreen == AppScreen.CATALOGO,
                                onClick = { currentScreen = AppScreen.CATALOGO },
                                icon = Icons.Default.Inventory2,
                                contentDescription = "Catálogo",
                                selectedContainerColor = navIndicatorBg,
                                unselectedContainerColor = navBarBg,
                                borderColor = BrandBluePrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_item_catalogo")
                            )

                            FastNavIconItem(
                                selected = currentScreen == AppScreen.NUEVA_SOLICITUD,
                                onClick = { currentScreen = AppScreen.NUEVA_SOLICITUD },
                                icon = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Solicitud",
                                selectedContainerColor = navIndicatorBg,
                                unselectedContainerColor = navBarBg,
                                borderColor = BrandBluePrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_item_solicitud")
                            )

                            FastNavIconItem(
                                selected = currentScreen == AppScreen.HISTORIAL,
                                onClick = { currentScreen = AppScreen.HISTORIAL },
                                icon = Icons.Default.History,
                                contentDescription = "Historial",
                                selectedContainerColor = navIndicatorBg,
                                unselectedContainerColor = navBarBg,
                                borderColor = BrandBluePrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_item_historial")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    // Barra lateral plana y ligera de solo iconos (sin textos)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(60.dp)
                            .background(navBarBg)
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FastNavIconItem(
                            selected = currentScreen == AppScreen.CATALOGO,
                            onClick = { currentScreen = AppScreen.CATALOGO },
                            icon = Icons.Default.Inventory2,
                            contentDescription = "Catálogo",
                            selectedContainerColor = navIndicatorBg,
                            unselectedContainerColor = navBarBg,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nav_item_catalogo")
                        )

                        FastNavIconItem(
                            selected = currentScreen == AppScreen.NUEVA_SOLICITUD,
                            onClick = { currentScreen = AppScreen.NUEVA_SOLICITUD },
                            icon = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Solicitud",
                            selectedContainerColor = navIndicatorBg,
                            unselectedContainerColor = navBarBg,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nav_item_solicitud")
                        )

                        FastNavIconItem(
                            selected = currentScreen == AppScreen.HISTORIAL,
                            onClick = { currentScreen = AppScreen.HISTORIAL },
                            icon = Icons.Default.History,
                            contentDescription = "Historial",
                            selectedContainerColor = navIndicatorBg,
                            unselectedContainerColor = navBarBg,
                            borderColor = BrandBluePrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("nav_item_historial")
                        )
                    }
                    VerticalDivider(thickness = 1.dp, color = BrandBluePrimary.copy(alpha = 0.25f))
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        AppScreen.CATALOGO -> {
                            CatalogScreen(
                                viewModel = catalogViewModel,
                                onNavigateToNewRequestWithProduct = { selectedProduct ->
                                    requestViewModel.selectProduct(selectedProduct)
                                    currentScreen = AppScreen.NUEVA_SOLICITUD
                                }
                            )
                        }

                        AppScreen.NUEVA_SOLICITUD -> {
                            RequestScreen(
                                viewModel = requestViewModel,
                                onEditSavedToHistory = {
                                    currentScreen = AppScreen.HISTORIAL
                                }
                            )
                        }

                        AppScreen.HISTORIAL -> {
                            RequestHistoryScreen(
                                viewModel = requestViewModel,
                                onEditRequest = { requestToEdit ->
                                    requestViewModel.loadRequestForEditing(requestToEdit)
                                    currentScreen = AppScreen.NUEVA_SOLICITUD
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
