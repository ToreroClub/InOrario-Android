package com.carlo.inorario.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.DayType
import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.ui.components.MetroRowView
import com.carlo.inorario.ui.components.TrainRowView
import com.carlo.inorario.ui.viewmodel.MetroViewModel
import com.carlo.inorario.ui.viewmodel.TrainViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationBoardScreen(
    station: Station,
    trainViewModel: TrainViewModel,
    metroViewModel: MetroViewModel,
    onBackClick: () -> Unit,
    onTrainClick: (Train) -> Unit,
    onMetroClick: (MetroLine) -> Unit
) {
    val isFerrovienord = station.vtID?.startsWith("N") == true
    val useRfi = !isFerrovienord && !station.rfiID.isNullOrEmpty()

    var showingDepartures by remember { mutableStateOf(true) }
    var selectedPassanteDirection by remember { mutableStateOf("Ovest") }
    var isMetroExpanded by remember { mutableStateOf(false) }
    var isAlertExpanded by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(Date()) }

    val trains by trainViewModel.trains.collectAsState()
    val isLoading by trainViewModel.isLoading.collectAsState()
    val stationAlerts by trainViewModel.stationAlerts.collectAsState()
    val myStations by trainViewModel.myStations.collectAsState()
    val isHomeFilterActive by trainViewModel.isHomeFilterActive.collectAsState()
    val homeDestinationName by trainViewModel.homeDestinationStationName.collectAsState()

    val isMyStation = myStations.any { it.vtID == station.vtID }
    val isPassanteDirectional = false

    // Auto-refresh timer simulated
    LaunchedEffect(key1 = station.id, key2 = showingDepartures) {
        trainViewModel.addToViewedRecentStations(station)
        trainViewModel.fetchTrains(station, isDepartures = showingDepartures)
        trainViewModel.startAutoRefresh(station, isDepartures = showingDepartures)
    }

    // Handle timer for metro countdown updates
    LaunchedEffect(key1 = Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            currentTime = Date()
        }
    }

    val filteredTrains: List<Train> = remember(trains, selectedPassanteDirection, isHomeFilterActive, homeDestinationName) {
        var list = trains
        if (isPassanteDirectional) {
            list = list.filter { train ->
                val dir = getPassanteDirection(train)
                dir == selectedPassanteDirection
            }
        }
        if (isHomeFilterActive && homeDestinationName.isNotEmpty()) {
            list = trainViewModel.filterTrainsForHome(list, station.name)
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Large iOS-style station name
                    Column {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isMyStation) {
                                station.vtID?.let { trainViewModel.removeMyStation(it) }
                            } else {
                                station.vtID?.let { trainViewModel.addMyStation(station.name, it) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isMyStation) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Preferito",
                            tint = if (isMyStation) Color(0xFFFFCC00) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { trainViewModel.fetchTrains(station, isDepartures = showingDepartures) },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Aggiorna",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                expandedHeight = 104.dp
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // ── FISSO: source label + segmented buttons (non scorrono) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (useRfi) "Treni RFI" else "ViaggiaTreno",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isPassanteDirectional) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = showingDepartures,
                            onClick = { showingDepartures = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("Partenze", style = MaterialTheme.typography.labelMedium) }
                        )
                        SegmentedButton(
                            selected = !showingDepartures,
                            onClick = { showingDepartures = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("Arrivi", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DirectionButton(
                            text = "← Bovisa/Rho",
                            isSelected = selectedPassanteDirection == "Ovest"
                        ) { selectedPassanteDirection = "Ovest" }
                        DirectionButton(
                            text = "Rogoredo/Forlanini →",
                            isSelected = selectedPassanteDirection == "Est"
                        ) { selectedPassanteDirection = "Est" }
                    }
                }
            }

            // ── SCROLL UNICO: avvisi + metro + treni scrollano insieme ──
            val allSchedules by metroViewModel.allSchedules.collectAsState()
            val isOfflineMode by metroViewModel.isOfflineMode.collectAsState()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Card Avvisi / Salute linea
                item(key = "alerts") {
                    val healthColor = getLineHealthColor(filteredTrains)
                    val healthMsg = getLineHealthMessage(filteredTrains)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .animateContentSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(healthColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = healthMsg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = healthColor
                                )
                            }

                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if ((stationAlerts != null) && !isAlertExpanded) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        stationAlerts?.let { alerts ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAlertExpanded = !isAlertExpanded }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Avvisi della Stazione",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Icon(
                                    imageVector = if (isAlertExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isAlertExpanded) {
                                Text(
                                    text = alerts,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Card Metropolitana (solo se presenti linee)
                if (station.metroLines.isNotEmpty()) {
                    item(key = "metro") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .animateContentSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMetroExpanded = !isMetroExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Subway,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Metropolitana",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Icon(
                                    imageVector = if (isMetroExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isMetroExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    station.metroLines.forEach { metro ->
                                        val lineCode = metro.name.take(2)
                                        val cacheKey = "${lineCode}_${metro.pdfID.orEmpty()}_${metro.direction}_"
                                        val isOffline = isOfflineMode[cacheKey] ?: false
                                        val loaded = allSchedules[cacheKey] != null
                                        val mode = metroViewModel.getNextDepartures(metro, currentTime)

                                        MetroRowView(
                                            metro = metro,
                                            isOffline = isOffline,
                                            scheduleLoaded = loaded,
                                            displayMode = mode,
                                            onSyncRequested = {
                                                metro.pdfID?.let {
                                                    metroViewModel.syncLiveDepartures(
                                                        metroName = metro.name,
                                                        pdfID = it,
                                                        direction = metro.direction
                                                    )
                                                }
                                            },
                                            onClick = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Intestazione lista treni
                item(key = "trains_header") {
                    Text(
                        text = if (showingDepartures) "TRENI IN PARTENZA" else "TRENI IN ARRIVO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Treni
                if (isLoading && filteredTrains.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (filteredTrains.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nessun treno trovato.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredTrains, key = { it.number + it.time }) { train ->
                        val isCentralPassante = trainViewModel.normalizeStationName(station.name).let { name ->
                            name.contains("villapizzone") || name.contains("garibaldi") ||
                                    name.contains("repubblica") || name.contains("venezia") ||
                                    name.contains("dateo") || name.contains("vittoria")
                        }
                        val resolvedPlat = getResolvedPlatform(station.name, train, trainViewModel)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrainClick(train) }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrainRowView(
                                train = train,
                                displayPlatform = resolvedPlat,
                                showPassanteTag = isCentralPassante,
                                passanteBranch = getPassanteBranch(train),
                                isFilterActive = isHomeFilterActive,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            thickness = 0.5.dp
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DirectionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Color(0xFFFF9500) else Color.Gray.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CircularTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFFFF9500) else Color.Gray.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Helpers

private fun getPassanteBranch(train: Train): String? {
    val dest = train.destination.lowercase()
    return when (train.category.uppercase()) {
        "S1", "S2", "S12", "S13" -> {
            if (isRogoredoDestination(dest)) "Rogoredo" else "Bovisa"
        }
        "S5", "S6" -> {
            if (isForlaniniDestination(dest)) "Forlanini" else "Rho"
        }
        else -> null
    }
}

private fun getPassanteDirection(train: Train): String? {
    val branch = getPassanteBranch(train) ?: return null
    return if (branch == "Bovisa" || branch == "Rho") "Ovest" else "Est"
}

private fun getResolvedPlatform(stationName: String, train: Train, trainViewModel: TrainViewModel): String {
    val name = trainViewModel.normalizeStationName(stationName)
    val direction = getPassanteDirection(train) ?: "Est"
    val cat = train.category.uppercase()

    return when {
        name.contains("rhofiera") -> if (direction == "Ovest") "1" else "2"
        name.contains("certosa") -> if (direction == "Est") "5" else "6"
        name.contains("villapizzone") -> if (direction == "Ovest") "1" else "2"
        name.contains("lancetti") -> {
            if (direction == "Est") {
                if (cat == "S5" || cat == "S6") "1" else "2"
            } else {
                if (cat == "S5" || cat == "S6") "3" else "4"
            }
        }
        name.contains("garibaldi") -> if (direction == "Est") "1" else "2"
        name.contains("repubblica") -> if (direction == "Est") "1" else "2"
        name.contains("venezia") || name.contains("portavenezia") -> if (direction == "Est") "1" else "2"
        name.contains("dateo") -> if (direction == "Est") "1" else "2"
        name.contains("vittoria") || name.contains("portavittoria") -> {
            if (direction == "Est") {
                if (cat == "S5" || cat == "S6") "3" else "4"
            } else {
                if (cat == "S5" || cat == "S6") "1" else "2"
            }
        }
        name.contains("forlanini") -> {
            if (cat == "S9") {
                if (direction == "Est") "3" else "4"
            } else {
                if (direction == "Est") "1" else "2"
            }
        }
        else -> train.platform
    }
}

private fun isRogoredoDestination(destName: String): Boolean {
    val d = destName.lowercase()
    return d.contains("rogoredo") || d.contains("lodi") ||
            d.contains("pavia") || d.contains("melegnano") ||
            d.contains("locate") || d.contains("borgolombardo") ||
            d.contains("s.donato") || d.contains("san donato") ||
            d.contains("cremona") || d.contains("piacenza") ||
            d.contains("mantova") || d.contains("s.giuliano") ||
            d.contains("san giuliano")
}

private fun isForlaniniDestination(destName: String): Boolean {
    val d = destName.lowercase()
    return d.contains("treviglio") || d.contains("pioltello") ||
            d.contains("segrate") || d.contains("melzo") ||
            d.contains("vignate") || d.contains("pozzuolo") ||
            (d.contains("forlanini") && !d.contains("rogoredo"))
}

private fun getLineHealthColor(trains: List<Train>): Color {
    val regTrains = trains.filter { !isAVOrLongDistance(it) }
    val regCritical = regTrains.filter { train ->
        val isCancelled = train.delay.lowercase().contains("soppresso") || train.delay.lowercase().contains("cancellato")
        if (isCancelled) return@filter true
        val delayMin = train.delay.replace("+", "").replace("'", "").toIntOrNull() ?: 0
        delayMin >= 20
    }
    if (regCritical.isNotEmpty()) return Color(0xFFFF3B30) // Red

    val regDelayed = regTrains.filter { train ->
        val delayMin = train.delay.replace("+", "").replace("'", "").toIntOrNull() ?: 0
        delayMin in 10..19
    }
    if (regDelayed.isNotEmpty()) return Color(0xFFFF9500) // Orange

    return Color(0xFF34C759) // Green
}

private fun getLineHealthMessage(trains: List<Train>): String {
    if (trains.isEmpty()) return "Dati non disponibili"
    val regTrains = trains.filter { !isAVOrLongDistance(it) }
    val regCritical = regTrains.filter { train ->
        val isCancelled = train.delay.lowercase().contains("soppresso") || train.delay.lowercase().contains("cancellato")
        if (isCancelled) return@filter true
        val delayMin = train.delay.replace("+", "").replace("'", "").toIntOrNull() ?: 0
        delayMin >= 20
    }
    if (regCritical.isNotEmpty()) {
        val hasCancellations = regCritical.any { it.delay.lowercase().contains("soppresso") || it.delay.lowercase().contains("cancellato") }
        return if (hasCancellations) "Soppressioni in corso" else "Forti ritardi"
    }

    val regDelayed = regTrains.filter { train ->
        val delayMin = train.delay.replace("+", "").replace("'", "").toIntOrNull() ?: 0
        delayMin in 10..19
    }
    if (regDelayed.isNotEmpty()) return "Rallentamenti"

    return "Circolazione Regolare"
}

private fun isAVOrLongDistance(train: Train): Boolean {
    val cat = train.category.uppercase()
    val dest = train.destination.uppercase()
    return cat.contains("FR") || cat.contains("FA") || cat.contains("FB") ||
            cat.contains("AV") || cat.contains("EC") || cat.contains("IC") ||
            cat.contains("ITALO") || cat.contains("FRECCIA") ||
            cat == "NTV" || cat == "EXP" || cat == "ES" ||
            dest.contains("ITALO") || dest.contains("FRECCIAROSSA")
}
