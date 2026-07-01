package com.carlo.inorario.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.ui.theme.getSuburbanColor
import com.carlo.inorario.ui.viewmodel.TrainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    trainViewModel: TrainViewModel,
    onOnboardingCompleted: () -> Unit,
    onRequestLocationPermission: () -> Unit,
) {
    val pageCount = 6
    val pagerState = rememberPagerState { pageCount }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip Button (only if not on the last page)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < (pageCount - 1)) {
                Text(
                    text = "Salta",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable {
                            onOnboardingCompleted()
                        }
                        .padding(8.dp),
                    fontSize = 14.sp
                )
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Horizontal Pager for pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = true
        ) { pageIndex ->
            when (pageIndex) {
                0 -> OnboardingWelcomeView()
                1 -> OnboardingHomeStationPickerView(trainViewModel)
                2 -> OnboardingFavoriteRoutesView(trainViewModel)
                3 -> OnboardingPassanteLinePickerView(trainViewModel)
                4 -> OnboardingFeaturesView()
                5 -> OnboardingGPSView(onRequestLocationPermission)
            }
        }

        // Indicator and Action Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pageCount) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            val isLastPage = pagerState.currentPage == pageCount - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onOnboardingCompleted()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isLastPage) "Inizia ora" else "Avanti",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun OnboardingWelcomeView() {
    val infiniteTransition = rememberInfiniteTransition(label = "iconScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Benvenuto su In Orario",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Il tuo compagno ideale per viaggiare in treno. Vedi sul tuo smartphone esattamente ciò che mostrano i tabelloni fisici delle stazioni con dati ufficiali RFI aggiornati all'istante.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingHomeStationPickerView(trainViewModel: TrainViewModel) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var homeDestInput by remember { mutableStateOf("") }
    val homeStationName by trainViewModel.homeDestinationStationName.collectAsState()
    val myStations by trainViewModel.myStations.collectAsState()

    var showFavSearch by remember { mutableStateOf(false) }

    LaunchedEffect(homeStationName) {
        homeDestInput = homeStationName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Stazione di Casa & Preferite",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configura la stazione di casa per il filtro rapido 🏠 e aggiungi le stazioni che frequenti più spesso per averle sempre in primo piano.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Casa / Lavoro Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stazione di Casa / Lavoro (Filtro 🏠)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )

                    val allStations = remember(trainViewModel.allRFIStations) {
                        (SuburbanData.allLines.flatMap { it.stations.map { s -> s.name } } +
                                trainViewModel.allRFIStations.map { s -> s.name })
                            .map { name ->
                                name.trim().lowercase().split(" ").joinToString(" ") { word ->
                                    word.replaceFirstChar { it.uppercaseChar() }
                                }
                            }
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                            .sorted()
                    }

                    AutocompleteTextField(
                        label = "Seleziona Stazione di Casa",
                        placeholder = "Es. Magenta, Rho, Milano Centrale...",
                        value = homeDestInput,
                        onValueChange = { homeDestInput = it },
                        suggestions = allStations
                    )

                    if (homeStationName.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Stazione salvata: $homeStationName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (homeDestInput.isNotEmpty()) {
                                    trainViewModel.saveHomeDestinationStationName(homeDestInput)
                                    focusManager.clearFocus()
                                }
                            },
                            enabled = homeDestInput.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9500)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (homeStationName.isEmpty()) "Salva Stazione" else "Aggiorna", color = Color.White)
                        }

                        if (homeStationName.isNotEmpty()) {
                            Button(
                                onClick = {
                                    homeDestInput = ""
                                    trainViewModel.saveHomeDestinationStationName("")
                                    focusManager.clearFocus()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f),
                                    contentColor = Color.Red
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Rimuovi")
                            }
                        }
                    }
                }
            }

            // General favorites Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Le Mie Stazioni Preferite",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFavSearch = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Cerca e aggiungi stazione preferita...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                tint = Color(0xFF007AFF)
                            )
                        }
                    }
                }
            }

            // List of added stations
            if (myStations.isEmpty()) {
                item {
                    Text(
                        text = "Nessuna stazione preferita aggiunta.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            } else {
                items(myStations) { station ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = station.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                station.vtID?.let { trainViewModel.removeMyStation(it) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Rimuovi",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFavSearch) {
        StationSelectionDialog(
            title = "Aggiungi Preferita",
            onDismiss = { showFavSearch = false },
            onStationSelected = { name, id ->
                trainViewModel.addMyStation(name, id)
                showFavSearch = false
            },
            trainViewModel = trainViewModel
        )
    }
}

@Composable
fun OnboardingFavoriteRoutesView(trainViewModel: TrainViewModel) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var originName by remember { mutableStateOf("") }
    var originID by remember { mutableStateOf("") }
    var destName by remember { mutableStateOf("") }
    var destID by remember { mutableStateOf("") }

    var showOriginSearch by remember { mutableStateOf(false) }
    var showDestSearch by remember { mutableStateOf(false) }

    val favoriteRoutes by trainViewModel.favoriteRoutes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Le Tue Tratte Preferite",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Salva le tratte generiche (es. Magenta ➔ Milano Porta Garibaldi). Non contengono orari fissi e mostreranno tutti i treni regionali e suburbani in tempo reale.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Input Fields (simulate selectors)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOriginSearch = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF9500))
                        )
                        Text(
                            text = originName.ifEmpty { "Seleziona Stazione di Partenza" },
                            fontWeight = if (originName.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                            color = if (originName.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDestSearch = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = destName.ifEmpty { "Seleziona Stazione di Arrivo" },
                            fontWeight = if (destName.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                            color = if (destName.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isAddEnabled = originID.isNotEmpty() && destID.isNotEmpty() && originID != destID
            Button(
                onClick = {
                    if (isAddEnabled) {
                        trainViewModel.toggleFavoriteRoute(originName, originID, destName, destID)
                        originName = ""
                        originID = ""
                        destName = ""
                        destID = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = isAddEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Aggiungi ai Preferiti", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // List of routes
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (favoriteRoutes.isEmpty()) {
                item {
                    Text(
                        text = "Nessuna tratta preferita ancora aggiunta.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            } else {
                items(favoriteRoutes) { route ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFCC00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${route.originName} ➔ ${route.destinationName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = {
                                trainViewModel.toggleFavoriteRoute(route.originName, route.originID, route.destinationName, route.destinationID)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Rimuovi",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOriginSearch) {
        StationSelectionDialog(
            title = "Stazione di Partenza",
            onDismiss = { showOriginSearch = false },
            onStationSelected = { name, id ->
                originName = name
                originID = id
                showOriginSearch = false
            },
            trainViewModel = trainViewModel
        )
    }

    if (showDestSearch) {
        StationSelectionDialog(
            title = "Stazione di Arrivo",
            onDismiss = { showDestSearch = false },
            onStationSelected = { name, id ->
                destName = name
                destID = id
                showDestSearch = false
            },
            trainViewModel = trainViewModel
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingPassanteLinePickerView(trainViewModel: TrainViewModel) {
    val selectedLines by trainViewModel.selectedSuburbanLines.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Passante & Tunnel",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Monitora lo stato del Passante di Milano e del relativo Tunnel sotterraneo in un'unica schermata. Seleziona qui sotto le tue linee suburbane preferite da tenere sott'occhio:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Grid of Lines (using FlowRow)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SuburbanData.allLines.forEach { line ->
                val isSelected = selectedLines.contains(line.id)
                val color = getSuburbanColor(line.hexColor)

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { trainViewModel.toggleSuburbanLine(line.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.id,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Puoi configurare dalle impostazioni quali stazioni mostrare per ogni singola linea suburbana.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun OnboardingFeaturesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Funzioni Smart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tutto ciò di cui hai bisogno per viaggiare senza stress:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FeatureRow(
                icon = Icons.Default.Star,
                color = Color(0xFFFFCC00),
                title = "Smart Routes",
                desc = "Trova le migliori coincidenze tra treni regionali e linee metropolitane."
            )

            FeatureRow(
                icon = Icons.Default.Train,
                color = Color(0xFF007AFF),
                title = "Treni Salvati",
                desc = "Tieni d'occhio i tuoi treni più frequenti direttamente dalla dashboard principale."
            )

            FeatureRow(
                icon = Icons.Default.Info,
                color = Color(0xFFFF9500),
                title = "Scioperi e News",
                desc = "Notizie, aggiornamenti e avvisi in tempo reale per viaggiare informato."
            )

            FeatureRow(
                icon = Icons.Default.Notifications,
                color = Color(0xFFAF52DE),
                title = "Notifiche",
                desc = "Monitora i tuoi treni preferiti con avvisi push di ritardo e infomobilità."
            )
        }
    }
}

@Composable
fun FeatureRow(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}


@Composable
fun OnboardingGPSView(onRequestLocationPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Scioperi e GPS",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Resta aggiornato su scioperi o disservizi con notizie ed elaborazioni intelligenti.\n\nConsenti l'accesso alla posizione: il GPS serve per rilevare le stazioni a te più vicine per una navigazione immediata!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestLocationPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9500)
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("Consenti Posizione GPS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AutocompleteTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.length < 2) emptyList()
        else suggestions.filter { it.contains(value, ignoreCase = true) }.take(5)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Cancella")
                    }
                }
            }
        )

        if (expanded && filteredSuggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StationSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onStationSelected: (String, String) -> Unit,
    trainViewModel: TrainViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val results by trainViewModel.searchStationResults.collectAsState()
    val isSearching by trainViewModel.isSearching.collectAsState()

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            trainViewModel.searchStations(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cerca stazione...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (searchQuery.length < 2) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Digita almeno 2 caratteri", color = Color.Gray, fontSize = 13.sp)
                    }
                } else if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nessuna stazione trovata", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(results) { station ->
                            Text(
                                text = station.nomeLungo.lowercase().replaceFirstChar { it.titlecase() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onStationSelected(station.nomeLungo, station.vtID)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                fontSize = 14.sp
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}
