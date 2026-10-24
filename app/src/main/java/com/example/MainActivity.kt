package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.catalog.CatalogScreen
import com.example.ui.catalog.CatalogViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppRoot(
    catalogViewModel: CatalogViewModel,
    requestViewModel: RequestViewModel
) {
    var currentScreen by remember { mutableStateOf(AppScreen.CATALOGO) }
    val catalogState by catalogViewModel.uiState.collectAsState()

    // Handle back button to return to CATALOGO if on secondary screen
    BackHandler(enabled = currentScreen != AppScreen.CATALOGO) {
        currentScreen = AppScreen.CATALOGO
    }

    val topBarBg = BrandBluePrimary
    val topBarTextColor = getContrastingTextColor(topBarBg)

    val navBarBg = MaterialTheme.colorScheme.surface
    val navBarTextColor = getContrastingTextColor(navBarBg)
    val navIndicatorBg = BrandBlueContainer
    val navSelectedIconColor = getContrastingTextColor(navIndicatorBg)

    val screenBg = MaterialTheme.colorScheme.background
    val screenTextColor = getContrastingTextColor(screenBg)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = screenBg,
        contentColor = screenTextColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            currentScreen = AppScreen.CATALOGO
                            catalogViewModel.openWorkplaceDialog()
                        }
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = topBarTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = catalogState.workplaceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = topBarTextColor,
                                modifier = Modifier.testTag("top_bar_workplace_name")
                            )
                            Text(
                                text = "Catálogo y Gestor de Precios Gs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = topBarTextColor
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            currentScreen = AppScreen.CATALOGO
                            catalogViewModel.openWorkplaceDialog()
                        },
                        modifier = Modifier.testTag("top_bar_edit_workplace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Cambiar nombre del local",
                            tint = topBarTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarBg,
                    titleContentColor = topBarTextColor
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = navBarBg,
                contentColor = navBarTextColor,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = navSelectedIconColor,
                    selectedTextColor = navBarTextColor,
                    indicatorColor = navIndicatorBg,
                    unselectedIconColor = navBarTextColor,
                    unselectedTextColor = navBarTextColor
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.CATALOGO,
                    onClick = { currentScreen = AppScreen.CATALOGO },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Catálogo") },
                    label = { Text("Catálogo", color = navBarTextColor, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_item_catalogo")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.NUEVA_SOLICITUD,
                    onClick = { currentScreen = AppScreen.NUEVA_SOLICITUD },
                    icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Nueva Solicitud") },
                    label = { Text("Solicitud 16:9", color = navBarTextColor, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_item_solicitud")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORIAL,
                    onClick = { currentScreen = AppScreen.HISTORIAL },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial", color = navBarTextColor, fontWeight = FontWeight.Bold) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_item_historial")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                        viewModel = requestViewModel
                    )
                }

                AppScreen.HISTORIAL -> {
                    RequestHistoryScreen(
                        viewModel = requestViewModel
                    )
                }
            }
        }
    }
}
