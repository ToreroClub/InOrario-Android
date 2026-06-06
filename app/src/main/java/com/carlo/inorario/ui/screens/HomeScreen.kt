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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    onTrainClick: (SavedTrain) -> Unit
) {
    val sectionOrder by trainViewModel.sectionOrder.collectAsState()
    val myStations by trainViewModel.myStations.collectAsState()
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()
    val nearbyStation by locationViewModel.nearbyStation.collectAsState()
    val selectedSuburbanLines by trainViewModel.selectedSuburbanLines.collectAsState()
    val collapsedSections by trainViewModel.collapsedSections.collectAsState()

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
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = Color(0xFFFF9500)
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Cerca")
                    }
                    IconButton(onClick = onNewsClick) {
                        Icon(painter = painterResource(id = android.R.drawable.ic_dialog_info), contentDescription = "News")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Profilo")
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STAZIONE VICINA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "Espandi" else "Comprimi",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onRefreshClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rileva",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (!isCollapsed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (station == null) {
                        Text(
                            text = "Nessuna stazione rilevata entro 15km.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStationClick(station) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = station.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tocca per aprire il tabellone completo",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LE MIE STAZIONI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Espandi" else "Comprimi",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (!isCollapsed) {
            if (stations.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Non hai stazioni salvate. Usa la barra di ricerca in alto per trovarne una e salvarla con la stella.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        stations.forEachIndexed { index, station ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStationClick(station) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Train,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = station.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            
                            if (index < stations.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.5.dp
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFCC00),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "I MIEI TRENI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Espandi" else "Comprimi",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (!isCollapsed) {
            if (favorites.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Non ci sono treni monitorati. Cerca un treno ed aggiungilo ai preferiti per vederlo qui.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        favorites.forEachIndexed { index, favTrain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTrainClick(favTrain) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFCC00),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Treno ${favTrain.number}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Direzione ${favTrain.description}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            
                            if (index < favorites.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 0.5.dp
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = Color(0xFF34C759),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PASSANTE SUBURBANO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Espandi" else "Comprimi",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (!isCollapsed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
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
                            Text(
                                text = "Tocca per vedere tutti i treni in transito",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    val combinedOvest = remember(passanteTrains) {
                        val viaBovisa = passanteViewModel.getPassanteTrainsViaBovisa(passanteTrains)
                        val viaRho = passanteViewModel.getPassanteTrainsViaRho(passanteTrains)
                        (viaBovisa + viaRho).sortedBy { it.time }
                    }
                    val combinedEst = remember(passanteTrains) {
                        val viaRogoredo = passanteViewModel.getPassanteTrainsViaRogoredo(passanteTrains)
                        val viaForlanini = passanteViewModel.getPassanteTrainsViaForlanini(passanteTrains)
                        (viaRogoredo + viaForlanini).sortedBy { it.time }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PassanteBranchView(
                            label = "← Ovest (Bovisa / Rho)",
                            color = Color(0xFF34C759),
                            trains = combinedOvest,
                            modifier = Modifier.weight(1f),
                            onTrainClick = onTrainClick
                        )

                        PassanteBranchView(
                            label = "Est (Rogoredo / Treviglio) →",
                            color = Color(0xFFFF9500),
                            trains = combinedEst,
                            modifier = Modifier.weight(1f),
                            onTrainClick = onTrainClick
                        )
                    }
                }
            }
        }
    }
}
