package com.carlo.inorario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.location.LocationTracker
import com.carlo.inorario.ui.screens.*
import com.carlo.inorario.ui.theme.InOrarioTheme
import com.carlo.inorario.ui.viewmodel.*
import com.google.gson.Gson
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.carlo.inorario.data.billing.BillingManager
import androidx.compose.material3.SnackbarHostState
import com.carlo.inorario.ui.components.MetroQuickViewBottomSheet
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val gson = Gson()

    private fun <T> T.toJsonUri(): String {
        val json = gson.toJson(this)
        return URLEncoder.encode(json, "UTF-8")
    }

    private fun <T> String.fromJsonUri(clazz: Class<T>): T {
        val decoded = URLDecoder.decode(this, "UTF-8")
        return gson.fromJson(decoded, clazz)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataStoreManager = DataStoreManager(applicationContext)
        val trainViewModel = TrainViewModel(applicationContext, dataStoreManager)
        val passanteViewModel = PassanteViewModel(dataStoreManager, trainViewModel)
        val locationTracker = LocationTracker(applicationContext)
        val locationViewModel = LocationViewModel(locationTracker)
        val profileViewModel = ProfileViewModel(dataStoreManager)
        val metroViewModel = MetroViewModel(dataStoreManager)
        val newsViewModel = NewsViewModel(dataStoreManager)

        val billingManager = BillingManager(
            context = applicationContext,
            dataStoreManager = dataStoreManager,
            onPurchaseSuccess = {
                // We can let ProfileScreen show thank you or just rely on the UI update
            },
            onPurchaseError = { errorMsg ->
                // Basic error logging, UI could be extended to show it
                android.util.Log.e("Billing", "Errore acquisto: $errorMsg")
            }
        )
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onCreate(owner: androidx.lifecycle.LifecycleOwner) {
                billingManager.startConnection()
            }
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                billingManager.endConnection()
            }
        })

        // Retrieve Firebase Messaging token on launch and update preferences
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result ?: ""
                    android.util.Log.d("MainActivity", "Token FCM recuperato con successo: $token")
                    if (token.isNotEmpty()) {
                        profileViewModel.saveFcmToken(token)
                        lifecycleScope.launch {
                            val isEnabled = dataStoreManager.remoteNotificationsEnabledFlow.first()
                            if (isEnabled) {
                                trainViewModel.syncRemoteNotifications(enabled = true, token = token)
                            }
                        }
                    }
                } else {
                    android.util.Log.e(
                        "MainActivity",
                        "Errore recupero token Firebase: ${task.exception?.message}",
                        task.exception
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Errore inizializzazione Firebase Messaging: ${e.message}", e)
        }

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if ((permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            ) {
                locationTracker.requestLocation()
            }
        }

        val requestLocationPermission = {
            locationPermissionRequest.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }

        setContent {
            InOrarioTheme {
                val navController = rememberNavController()
                val userName by profileViewModel.userName.collectAsState()
                val notificationLimitError by trainViewModel.notificationLimitError.collectAsState()

                var showMetroBottomSheet by remember { mutableStateOf<Station?>(null) }
                var metroTimeContext by remember { mutableStateOf("") }

                if (notificationLimitError != null) {
                    AlertDialog(
                        onDismissRequest = { trainViewModel.clearNotificationLimitError() },
                        title = { Text(text = "Limite Raggiunto") },
                        text = { Text(text = notificationLimitError ?: "") },
                        confirmButton = {
                            TextButton(
                                onClick = { trainViewModel.clearNotificationLimitError() }
                            ) {
                                Text("OK", color = androidx.compose.ui.graphics.Color(0xFFFF9500))
                            }
                        }
                    )
                }

                if (userName == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1C1E))
                    )
                } else {
                    // Check if user has completed onboarding by seeing if name is configured
                    val startDest = if (userName!!.isEmpty()) "onboarding" else "home"

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                    composable("onboarding") {
                        OnboardingScreen(
                            trainViewModel = trainViewModel,
                            onOnboardingCompleted = {
                                profileViewModel.saveUserName("Pendolare")
                                navController.navigate("home") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        ) {
                            requestLocationPermission()
                        }
                    }

                    composable("home") {
                        HomeScreen(
                            trainViewModel = trainViewModel,
                            passanteViewModel = passanteViewModel,
                            locationViewModel = locationViewModel,
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            onNewsClick = {
                                navController.navigate("newsCenter")
                            },
                            onProfileClick = {
                                navController.navigate("profile")
                            },
                            onStationClick = { station ->
                                navController.navigate("stationBoard/${station.toJsonUri()}")
                            },
                            onTrainClick = { savedTrain ->
                                val dummy = trainViewModel.createDummyTrain(savedTrain)
                                navController.navigate("trainStops/${dummy.toJsonUri()}")
                            },
                            onSavedTripsClick = {
                                navController.navigate("savedTrips")
                            }
                        )
                    }

                    composable("savedTrips") {
                        SavedTripsScreen(
                            trainViewModel = trainViewModel,
                            onBackClick = { navController.popBackStack() },
                            onRouteSolutionClick = { route ->
                                navController.navigate("favoriteRoute/${route.toJsonUri()}")
                            },
                            onTripDetailsClick = { solution ->
                                navController.navigate("travelSolution/${solution.toJsonUri()}")
                            }
                        )
                    }

                    composable(
                        route = "favoriteRoute/{routeJson}",
                        arguments = listOf(navArgument("routeJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val routeJson = backStackEntry.arguments?.getString("routeJson").orEmpty()
                        val route = routeJson.fromJsonUri(com.carlo.inorario.data.model.FavoriteRoute::class.java)

                        FavoriteRouteSolutionScreen(
                            route = route,
                            trainViewModel = trainViewModel,
                            onBackClick = { navController.popBackStack() },
                            onSolutionClick = { solution ->
                                navController.navigate("travelSolution/${solution.toJsonUri()}")
                            }
                        )
                    }

                    composable(
                        route = "travelSolution/{solutionJson}",
                        arguments = listOf(navArgument("solutionJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val solutionJson = backStackEntry.arguments?.getString("solutionJson").orEmpty()
                        val solution = solutionJson.fromJsonUri(com.carlo.inorario.data.model.TravelSolution::class.java)

                        TravelSolutionDetailsScreen(
                            solution = solution,
                            trainViewModel = trainViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("newsCenter") {
                        NewsCenterScreen(
                            newsViewModel = newsViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("search") {
                        SearchView(
                            trainViewModel = trainViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onStationClick = { station ->
                                navController.navigate("stationBoard/${station.toJsonUri()}")
                            },
                            onTrainClick = { savedTrain ->
                                val dummy = trainViewModel.createDummyTrain(savedTrain)
                                navController.navigate("trainStops/${dummy.toJsonUri()}")
                            },
                            onRouteSearchClick = { route ->
                                navController.navigate("favoriteRoute/${route.toJsonUri()}")
                            }
                        )
                    }

                    composable(
                        route = "stationBoard/{stationJson}",
                        arguments = listOf(navArgument("stationJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val stationJson = backStackEntry.arguments?.getString("stationJson").orEmpty()
                        val station = stationJson.fromJsonUri(Station::class.java)

                        StationBoardScreen(
                            station = station,
                            trainViewModel = trainViewModel,
                            metroViewModel = metroViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onTrainClick = { train ->
                                navController.navigate("trainStops/${train.toJsonUri()}")
                            },
                            onMetroClick = {}
                        )
                    }

                    composable(
                        route = "trainStops/{trainJson}",
                        arguments = listOf(navArgument("trainJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val trainJson = backStackEntry.arguments?.getString("trainJson").orEmpty()
                        val train = trainJson.fromJsonUri(Train::class.java)

                        TrainStopsScreen(
                            train = train,
                            trainViewModel = trainViewModel,
                            showCloseButton = false,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onStationClick = { station ->
                                navController.navigate("stationBoard/${station.toJsonUri()}")
                            },
                            onStopLongClick = { station, time ->
                                showMetroBottomSheet = station
                                metroTimeContext = time
                            }
                        )
                    }

                    composable("profile") {
                        ProfileScreen(
                            profileViewModel = profileViewModel,
                            trainViewModel = trainViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onCustomizeDashboardClick = {
                                navController.navigate("customizeDashboard")
                            },
                            onCustomizePassanteClick = {
                                navController.navigate("customizePassante")
                            },
                            onRerunTutorialClick = {
                                profileViewModel.saveUserName("")
                                navController.navigate("onboarding") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            onNotificationCenterClick = {
                                navController.navigate("notificationCenter")
                            },
                            onPurchaseClick = {
                                billingManager.launchBillingFlow(this@MainActivity)
                            }
                        )
                    }

                    composable("notificationCenter") {
                        NotificationCenterScreen(
                            profileViewModel = profileViewModel,
                            trainViewModel = trainViewModel,
                            onBackClick = { navController.popBackStack() },
                            onTrainClick = { trainNumber ->
                                navController.navigate("trainNotifDetails/$trainNumber")
                            }
                        )
                    }

                    composable(
                        route = "trainNotifDetails/{trainNumber}",
                        arguments = listOf(navArgument("trainNumber") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val trainNumber = backStackEntry.arguments?.getString("trainNumber").orEmpty()
                        TrainNotificationDetailsScreen(
                            trainNumber = trainNumber,
                            trainViewModel = trainViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("customizeDashboard") {
                        CustomizeDashboardScreen(
                            trainViewModel = trainViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("customizePassante") {
                        CustomizePassanteScreen(
                            trainViewModel = trainViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                val currentStation = showMetroBottomSheet
                if (currentStation != null) {
                    MetroQuickViewBottomSheet(
                        station = currentStation,
                        timeContext = metroTimeContext,
                        metroViewModel = metroViewModel,
                        onDismiss = { showMetroBottomSheet = null }
                    )
                }
            }
        }
    }
}
}