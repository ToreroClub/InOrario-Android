package com.carlo.inorario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.AppSection
import com.carlo.inorario.data.model.SavedTrain
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.ui.components.PassanteBranchView
import com.carlo.inorario.ui.components.PassanteNodeView
import com.carlo.inorario.ui.components.PassanteTrainRowView
import com.carlo.inorario.ui.components.PassanteTunnelStatusHeaderView
import com.carlo.inorario.ui.components.SuburbanLineBadge
import com.carlo.inorario.ui.components.TrainRowView
import com.carlo.inorario.ui.theme.getSuburbanColor
import com.carlo.inorario.ui.viewmodel.LocationViewModel
import com.carlo.inorario.ui.viewmodel.PassanteViewModel
import com.carlo.inorario.ui.viewmodel.TrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    trainViewModel: TrainViewModel,
    passanteViewModel: PassanteViewModel,
    locationViewModel: LocationViewModel,
    onSearchClick: () -> Unit,
    onNewsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onTrainClick: (SavedTrain) -> Unit,
    onSavedTripsClick: () -> Unit
) {
    val sectionOrder by trainViewModel.sectionOrder.collectAsState()
    val myStations by trainViewModel.myStations.collectAsState()
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()
    val nearbyStation by locationViewModel.nearbyStation.collectAsState()
    val selectedSuburbanLines by trainViewModel.selectedSuburbanLines.collectAsState()
    val collapsedSections by trainViewModel.collapsedSections.collectAsState()
    val savedTrips by trainViewModel.savedTrips.collectAsState()
    // Passante states
    val selectedPassanteStation by passanteViewModel.selectedPassanteStation.collectAsState()
    val passanteTrains by passanteViewModel.passanteTrains.collectAsState()
    val isPassanteLoading by passanteViewModel.isLoadingPassanteBoard.collectAsState()
    val passanteHealthMsg by passanteViewModel.passanteTunnelHealthMessage.collectAsState()
    val passanteHealthColor by passanteViewModel.passanteTunnelHealthColor.collectAsState()

    // Passante Dialog Info State
    var showPassanteInfoDialog by remember { mutableStateOf(false) }

    // Location request
    LaunchedEffect(Unit) {
        locationViewModel.requestLocation()
        passanteViewModel.fetchPassanteLive()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "In Orario",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFFFF9500)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profilo",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNewsClick) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "News",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        BadgedBox(
                            badge = {
                                if (savedTrips.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFFF3B30),
                                        contentColor = Color.White
                                    ) {
                                        Text(text = savedTrips.size.toString())
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            IconButton(onClick = onSavedTripsClick) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Viaggi Salvati",
                                    tint = Color(0xFFFF9500)
                                )
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cerca",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Render sections in custom order
            items(sectionOrder, key = { it.name }) { section ->
                val isCollapsed = collapsedSections.contains(section.name)
                when (section) {
                    AppSection.NEARBY -> {
                        NearbyStationSection(
                            station = nearbyStation,
                            isCollapsed = isCollapsed,
                            onHeaderClick = { trainViewModel.toggleSectionCollapsed(section.name) },
                            onStationClick = onStationClick,
                            onRefreshClick = { locationViewModel.requestLocation() }
                        )
                    }
                    AppSection.MY_STATIONS -> {
                        MyStationsSection(
                            stations = myStations,
                            isCollapsed = isCollapsed,
                            onHeaderClick = { trainViewModel.toggleSectionCollapsed(section.name) },
                            onStationClick = onStationClick
                        )
                    }
                    AppSection.FAVORITE_TRAINS -> {
                        FavoriteTrainsSection(
                            favorites = favoriteTrains,
                            isCollapsed = isCollapsed,
                            onHeaderClick = { trainViewModel.toggleSectionCollapsed(section.name) },
                            onTrainClick = onTrainClick
                        )
                    }
                    AppSection.PASSANTE -> {
                        if (selectedSuburbanLines.isNotEmpty()) {
                            PassanteSection(
                                selectedStation = selectedPassanteStation,
                                passanteTrains = passanteTrains,
                                isLoading = isPassanteLoading,
                                healthMsg = passanteHealthMsg,
                                healthColor = passanteHealthColor,
                                isCollapsed = isCollapsed,
                                onHeaderClick = { trainViewModel.toggleSectionCollapsed(section.name) },
                                passanteViewModel = passanteViewModel,
                                selectedSuburbanLines = selectedSuburbanLines,
                                onStationClick = onStationClick,
                                onTrainClick = { train ->
                                    onTrainClick(SavedTrain(train.number, train.destination))
                                },
                                onInfoClick = { showPassanteInfoDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPassanteInfoDialog) {
        val avgDelay by passanteViewModel.passanteTunnelAverageDelay.collectAsState()
        AlertDialog(
            onDismissRequest = { showPassanteInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = try { Color(android.graphics.Color.parseColor(passanteHealthColor)) } catch (e: Exception) { Color.Gray },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stato del Passante",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(passanteHealthColor)) } catch (e: Exception) { Color.Gray })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = passanteHealthMsg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = try { Color(android.graphics.Color.parseColor(passanteHealthColor)) } catch (e: Exception) { Color.Gray }
                        )
                    }

                    Text(
                        text = "Ritardo medio stimato: $avgDelay min",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Il Passante Ferroviario è la tratta sotterranea di Milano in cui convergono le linee suburbane S1, S2, S5, S6, S12 e S13.\n\nLo stato di salute viene calcolato analizzando i ritardi e le soppressioni dei treni in transito in tempo reale nella stazione di Milano Repubblica.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPassanteInfoDialog = false }) {
                    Text(text = "Chiudi", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Shared section header composable — iOS style
@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isCollapsed: Boolean,
    onHeaderClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingContent?.invoke()
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Espandi" else "Comprimi",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// --- 1. Nearby Station Component ---
@Composable
fun NearbyStationSection(
    station: Station?,
    isCollapsed: Boolean,
    onHeaderClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Stazione Vicina",
            icon = Icons.Default.LocationOn,
            iconTint = Color(0xFFFF3B30),
            isCollapsed = isCollapsed,
            onHeaderClick = onHeaderClick,
            trailingContent = {
                IconButton(onClick = onRefreshClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rileva",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        )

        if (!isCollapsed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (station == null) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "Nessuna stazione rilevata entro 5km.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationClick(station) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tocca per aprire il tabellone",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

// --- 2. My Stations Section ---
@Composable
fun MyStationsSection(
    stations: List<Station>,
    isCollapsed: Boolean,
    onHeaderClick: () -> Unit,
    onStationClick: (Station) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Le mie Stazioni",
            icon = Icons.Default.Train,
            iconTint = Color(0xFFFF9500),
            isCollapsed = isCollapsed,
            onHeaderClick = onHeaderClick
        )

        if (!isCollapsed) {
            if (stations.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "Non hai stazioni salvate. Cerca una stazione e salvala con la stella ★",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        stations.forEachIndexed { index, station ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationClick(station) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            if (index < stations.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.4.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. Favorite Trains Section ---
@Composable
fun FavoriteTrainsSection(
    favorites: List<SavedTrain>,
    isCollapsed: Boolean,
    onHeaderClick: () -> Unit,
    onTrainClick: (SavedTrain) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "I miei Treni",
            icon = Icons.Default.Star,
            iconTint = Color(0xFFFFCC00),
            isCollapsed = isCollapsed,
            onHeaderClick = onHeaderClick
        )

        if (!isCollapsed) {
            if (favorites.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "Cerca un treno e aggiungilo ai preferiti per vederlo qui.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        favorites.forEachIndexed { index, favTrain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTrainClick(favTrain) }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Treno ${favTrain.number}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "dir. ${favTrain.description}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            if (index < favorites.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.4.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Passante Suburbano Section ---
@Composable
fun PassanteSection(
    selectedStation: Station,
    passanteTrains: List<Train>,
    isLoading: Boolean,
    healthMsg: String,
    healthColor: String,
    isCollapsed: Boolean,
    onHeaderClick: () -> Unit,
    passanteViewModel: PassanteViewModel,
    selectedSuburbanLines: List<String>,
    onStationClick: (Station) -> Unit,
    onTrainClick: (Train) -> Unit,
    onInfoClick: () -> Unit
) {
    val passanteArrivals by passanteViewModel.passanteLineArrivals.collectAsState()
    
    val tunnelStations = remember(selectedSuburbanLines) {
        val hasRhoLines = selectedSuburbanLines.contains("S5") || selectedSuburbanLines.contains("S6") || selectedSuburbanLines.contains("S11")
        val hasBovisaLines = selectedSuburbanLines.contains("S1") || selectedSuburbanLines.contains("S2") || selectedSuburbanLines.contains("S12") || selectedSuburbanLines.contains("S13") || selectedSuburbanLines.contains("S3") || selectedSuburbanLines.contains("S4")
        
        if (hasRhoLines && !hasBovisaLines) {
            listOf(
                Station("Rho Fiera", "3098", "S01026", 45.5215, 9.0883),
                Station("Certosa", "1708", "S01640", 45.5085, 9.1272),
                Station("Villapizzone", "3099", "S01639", 45.4998, 9.1465),
                Station("Lancetti", "1713", "S01643", 45.4925, 9.1751),
                Station("P. Garibaldi Passante", "1714", "S01647", 45.4844, 9.1887),
                Station("Repubblica", "1719", "S01648", 45.4795, 9.1963),
                Station("Porta Venezia", "1723", "S01649", 45.4746, 9.2052),
                Station("Dateo", "3468", "S01650", 45.4682, 9.2158),
                Station("Porta Vittoria", "1718", "S01633", 45.4613, 9.2227),
                Station("Forlanini", "3169", "S01492", 45.4625, 9.2368)
            )
        } else {
            listOf(
                Station("Milano Bovisa", null, "S01201", 45.5025, 9.1592),
                Station("Lancetti", "1713", "S01643", 45.4925, 9.1751),
                Station("P. Garibaldi Passante", "1714", "S01647", 45.4844, 9.1887),
                Station("Repubblica", "1719", "S01648", 45.4795, 9.1963),
                Station("Porta Venezia", "1723", "S01649", 45.4746, 9.2052),
                Station("Dateo", "3468", "S01650", 45.4682, 9.2158),
                Station("Porta Vittoria", "1718", "S01633", 45.4613, 9.2227)
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Passante Suburbano",
            icon = Icons.Default.Train,
            iconTint = Color(0xFF34C759),
            isCollapsed = isCollapsed,
            onHeaderClick = onHeaderClick
        )

        if (!isCollapsed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Health header
                    PassanteTunnelStatusHeaderView(
                        healthMessage = healthMsg,
                        healthColorHex = healthColor,
                        onInfoClick = onInfoClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable horizontal map
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(tunnelStations) { station ->
                            val isSelected = station.name == selectedStation.name
                            val isFirst = station.name == tunnelStations.first().name
                            val isLast = station.name == tunnelStations.last().name
                            
                            PassanteNodeView(
                                station = station,
                                isFirst = isFirst,
                                isLast = isLast,
                                isNearby = isSelected,
                                lineColor = Color(0xFF34C759),
                                onClick = { passanteViewModel.selectPassanteStation(station) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Heading for the selected station
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationClick(selectedStation) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tabellone " + selectedStation.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (passanteArrivals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = passanteArrivals.joinToString("   "),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF34C759)
                                )
                            } else {
                                Text(
                                    text = "Tocca per vedere tutti i treni in transito",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct S-branches split view
                    val activeLines = remember(selectedSuburbanLines) {
                        selectedSuburbanLines.filter { listOf("S1", "S2", "S5", "S6", "S12", "S13").contains(it) }
                    }
                    val onlyCertosa = remember(activeLines) {
                        activeLines.isNotEmpty() && activeLines.all { listOf("S5", "S6").contains(it) }
                    }
                    val onlyBovisa = remember(activeLines) {
                        activeLines.isNotEmpty() && activeLines.all { listOf("S1", "S2", "S12", "S13").contains(it) }
                    }

                    if (onlyCertosa) {
                        val ovestTrains = remember(passanteTrains, activeLines) {
                            passanteViewModel.getPassanteTrainsViaRho(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                activeLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }
                        val estTrains = remember(passanteTrains, activeLines) {
                            passanteViewModel.getPassanteTrainsViaForlanini(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                activeLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PassanteBranchView(
                                label = "← Direzione Ovest (Rho / Varese)",
                                color = Color(0xFFFF9500), // Orange
                                trains = ovestTrains,
                                isLarge = true,
                                onTrainClick = onTrainClick
                            )
                            PassanteBranchView(
                                label = "Direzione Est (Forlanini / Treviglio) →",
                                color = Color(0xFFFF9500), // Orange
                                trains = estTrains,
                                isLarge = true,
                                onTrainClick = onTrainClick
                            )
                        }
                    } else if (onlyBovisa) {
                        val ovestTrains = remember(passanteTrains, activeLines) {
                            passanteViewModel.getPassanteTrainsViaBovisa(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                activeLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }
                        val estTrains = remember(passanteTrains, activeLines) {
                            passanteViewModel.getPassanteTrainsViaRogoredo(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                activeLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PassanteBranchView(
                                label = "← Direzione Ovest (Bovisa / Saronno)",
                                color = Color(0xFFFF3B30), // Red
                                trains = ovestTrains,
                                isLarge = true,
                                onTrainClick = onTrainClick
                            )
                            PassanteBranchView(
                                label = "Direzione Est (Rogoredo / Pavia / Lodi) →",
                                color = Color(0xFFFF3B30), // Red
                                trains = estTrains,
                                isLarge = true,
                                onTrainClick = onTrainClick
                            )
                        }
                    } else {
                        val bovisaTrains = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaBovisa(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }
                        val forlaniniTrains = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaForlanini(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }
                        val rhoTrains = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaRho(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }
                        val rogoredoTrains = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaRogoredo(passanteTrains).filter { t ->
                                val cat = t.category.uppercase()
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(cat) || cat == "S" || cat == "REG" || cat == "RV"
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PassanteBranchView(
                                    label = "← Bovisa",
                                    color = Color(0xFFFF3B30), // Red
                                    trains = bovisaTrains,
                                    modifier = Modifier.weight(1f),
                                    onTrainClick = onTrainClick
                                )
                                PassanteBranchView(
                                    label = "Forlanini →",
                                    color = Color(0xFFFF9500), // Orange
                                    trains = forlaniniTrains,
                                    modifier = Modifier.weight(1f),
                                    onTrainClick = onTrainClick
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PassanteBranchView(
                                    label = "← Rho",
                                    color = Color(0xFFFF9500), // Orange
                                    trains = rhoTrains,
                                    modifier = Modifier.weight(1f),
                                    onTrainClick = onTrainClick
                                )
                                PassanteBranchView(
                                    label = "Rogoredo →",
                                    color = Color(0xFFFF3B30), // Red
                                    trains = rogoredoTrains,
                                    modifier = Modifier.weight(1f),
                                    onTrainClick = onTrainClick
                                )
                            }
                        }

                    // Altre Partenze
                        val filteredBovisa = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaBovisa(passanteTrains).filter { t ->
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(t.category.uppercase())
                            }
                        }
                        val filteredRho = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaRho(passanteTrains).filter { t ->
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(t.category.uppercase())
                            }
                        }
                        val filteredForlanini = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaForlanini(passanteTrains).filter { t ->
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(t.category.uppercase())
                            }
                        }
                        val filteredRogoredo = remember(passanteTrains, selectedSuburbanLines) {
                            passanteViewModel.getPassanteTrainsViaRogoredo(passanteTrains).filter { t ->
                                selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(t.category.uppercase())
                            }
                        }

                        val classified = remember(filteredBovisa, filteredRho, filteredForlanini, filteredRogoredo) {
                            filteredBovisa + filteredRho + filteredForlanini + filteredRogoredo
                        }

                        val unclassified = remember(passanteTrains, selectedSuburbanLines, classified) {
                            passanteTrains.filter { t ->
                                val isPreferred = selectedSuburbanLines.isEmpty() || selectedSuburbanLines.contains(t.category.uppercase())
                                isPreferred && !classified.any { it.number == t.number }
                            }
                        }

                        if (unclassified.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ALTRE PARTENZE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                unclassified.take(3).forEach { train ->
                                    PassanteTrainRowView(train = train) { onTrainClick(train) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 5. Viewed Recent Stations ---
@Composable
fun ViewedRecentStationsSection(
    stations: List<Station>,
    onStationClick: (Station) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF007AFF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Stazioni visitate",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                stations.forEachIndexed { index, station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationClick(station) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    if (index < stations.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.4.dp
                        )
                    }
                }
            }
        }
    }
}

// --- 6. Viewed Recent Trains ---
@Composable
fun ViewedRecentTrainsSection(
    trains: List<SavedTrain>,
    onTrainClick: (SavedTrain) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFF007AFF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Treni visti di recente",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                trains.forEachIndexed { index, train ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrainClick(train) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Treno ${train.number}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "dir. ${train.description}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    if (index < trains.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 0.4.dp
                        )
                    }
                }
            }
        }
    }
}
