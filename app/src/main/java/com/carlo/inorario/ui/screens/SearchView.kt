package com.carlo.inorario.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.FavoriteRoute
import com.carlo.inorario.data.model.SavedTrain
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.TrenitaliaLocation
import com.carlo.inorario.ui.viewmodel.TrainViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ViaggiSearchMode {
    MAIN,
    DEPARTURE,
    ARRIVAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(
    trainViewModel: TrainViewModel,
    onBackClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onTrainClick: (SavedTrain) -> Unit,
    onRouteSearchClick: (FavoriteRoute) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0=Viaggi, 1=Treni, 2=Stazioni
    val tabs = listOf("Viaggi", "Treni", "Stazioni")
    var viaggiSearchMode by remember { mutableStateOf(ViaggiSearchMode.MAIN) }

    Scaffold(
        topBar = {
            if (selectedTabIndex != 0 || viaggiSearchMode == ViaggiSearchMode.MAIN) {
                Column {
                    TopAppBar(
                        title = {
                            Text("Cerca", fontWeight = FontWeight.Bold)
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = Color(0xFFFF9500),
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Color(0xFFFF9500)
                                )
                            }
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTabIndex == index) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(if (selectedTabIndex == 0 && viaggiSearchMode != ViaggiSearchMode.MAIN) PaddingValues(0.dp) else innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> ViaggiSearchTab(
                    trainViewModel = trainViewModel,
                    searchMode = viaggiSearchMode,
                    onSearchModeChange = { viaggiSearchMode = it },
                    onRouteSearchClick = onRouteSearchClick
                )
                1 -> TreniSearchTab(trainViewModel, onTrainClick)
                2 -> StazioniSearchTab(trainViewModel, onStationClick)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViaggiSearchTab(
    trainViewModel: TrainViewModel,
    searchMode: ViaggiSearchMode,
    onSearchModeChange: (ViaggiSearchMode) -> Unit,
    onRouteSearchClick: (FavoriteRoute) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isSearching by trainViewModel.isSearching.collectAsState()
    val trenitaliaLocations by trainViewModel.searchTrenitaliaLocations.collectAsState()
    val savedRoutes by trainViewModel.favoriteRoutes.collectAsState()
    val recentTravelLocations by trainViewModel.recentTravelLocations.collectAsState()

    var selectedDep by remember { mutableStateOf<TrenitaliaLocation?>(null) }
    var selectedArr by remember { mutableStateOf<TrenitaliaLocation?>(null) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALY) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }

    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchMode) {
        if (searchMode != ViaggiSearchMode.MAIN) {
            searchQuery = ""
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchMode != ViaggiSearchMode.MAIN && searchQuery.length >= 2) {
            trainViewModel.searchTravelLocations(searchQuery)
        }
    }

    if (showDatePicker) {
        CompactDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        CompactTimePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showTimePicker = false },
            onConfirm = { newDate ->
                selectedDate = newDate
                showTimePicker = false
            }
        )
    }

    when (searchMode) {
        ViaggiSearchMode.MAIN -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dettagli Viaggio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Partenza row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchModeChange(ViaggiSearchMode.DEPARTURE) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Partenza", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                            Text(
                                text = selectedDep?.displayName ?: "Seleziona",
                                color = if (selectedDep != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (selectedDep != null) FontWeight.Medium else FontWeight.Normal
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Arrivo row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchModeChange(ViaggiSearchMode.ARRIVAL) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Arrivo", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                            Text(
                                text = selectedArr?.displayName ?: "Seleziona",
                                color = if (selectedArr != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (selectedArr != null) FontWeight.Medium else FontWeight.Normal
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Data e Ora row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Data e Ora", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Date Chip
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF007AFF).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                        .clickable { showDatePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dateFormatter.format(selectedDate),
                                        color = Color(0xFF007AFF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }

                                // Time Chip
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                                        .clickable { showTimePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = timeFormatter.format(selectedDate),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (selectedDep != null && selectedArr != null) {
                            trainViewModel.tempSearchDate = selectedDate
                            val route = FavoriteRoute(
                                originID = selectedDep!!.id.toString(),
                                originName = selectedDep!!.displayName,
                                destinationID = selectedArr!!.id.toString(),
                                destinationName = selectedArr!!.displayName
                            )
                            onRouteSearchClick(route)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    enabled = selectedDep != null && selectedArr != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9500),
                        disabledContainerColor = Color(0xFFFF9500).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerca Soluzioni", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }

                if (savedRoutes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "I TUOI VIAGGI SALVATI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                    savedRoutes.forEach { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onRouteSearchClick(route) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bookmark, null, tint = Color(0xFFFF9500))
                                Spacer(Modifier.width(12.dp))
                                Text("${route.originName} ➔ ${route.destinationName}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        else -> {
            val isDepartureSearch = searchMode == ViaggiSearchMode.DEPARTURE
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isDepartureSearch) "Stazione di partenza" else "Stazione di arrivo",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { onSearchModeChange(ViaggiSearchMode.MAIN) }) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Chiudi")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            ) { searchPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(searchPadding)
                        .imePadding()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Nome stazione...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF9500),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (searchQuery.length < 2) {
                            if (recentTravelLocations.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "STAZIONI RECENTI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Cancella",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable {
                                                trainViewModel.clearRecentTravelLocations()
                                            }
                                        )
                                    }
                                }
                                items(recentTravelLocations) { location ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isDepartureSearch) {
                                                    selectedDep = location
                                                } else {
                                                    selectedArr = location
                                                }
                                                trainViewModel.addToRecentTravelLocations(location)
                                                onSearchModeChange(ViaggiSearchMode.MAIN)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(12.dp))
                                            Text(location.displayName, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Digita almeno 2 caratteri per cercare",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            if (isSearching) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color(0xFFFF9500))
                                    }
                                }
                            }
                            items(trenitaliaLocations) { location ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isDepartureSearch) {
                                                selectedDep = location
                                            } else {
                                                selectedArr = location
                                            }
                                            trainViewModel.addToRecentTravelLocations(location)
                                            onSearchModeChange(ViaggiSearchMode.MAIN)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF007AFF))
                                        Spacer(Modifier.width(12.dp))
                                        Text(location.displayName, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreniSearchTab(
    trainViewModel: TrainViewModel,
    onTrainClick: (SavedTrain) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    val isSearching by trainViewModel.isSearching.collectAsState()
    val trainResults by trainViewModel.searchResults.collectAsState()
    val recentTrains by trainViewModel.recentTrains.collectAsState()
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()

    LaunchedEffect(query) {
        if (query.length >= 2) {
            trainViewModel.searchTrains(query)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Numero treno...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { 
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF9500),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.length < 2) {
                if (recentTrains.isNotEmpty()) {
                    item {
                        Text(
                            text = "TRENI RECENTI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(recentTrains) { savedTrain ->
                        val isFav = favoriteTrains.any { it.number == savedTrain.number }
                        TrainResultRow(savedTrain, isFav, { 
                            trainViewModel.addToRecentTrains(savedTrain)
                            focusManager.clearFocus()
                            onTrainClick(savedTrain)
                        }, {
                            trainViewModel.toggleFavorite(savedTrain.number, savedTrain.description)
                        })
                    }
                }
            } else {
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFF9500))
                        }
                    }
                }
                items(trainResults) { savedTrain ->
                    val isFav = favoriteTrains.any { it.number == savedTrain.number }
                    TrainResultRow(savedTrain, isFav, {
                        trainViewModel.addToRecentTrains(savedTrain)
                        focusManager.clearFocus()
                        onTrainClick(savedTrain)
                    }, {
                        trainViewModel.toggleFavorite(savedTrain.number, savedTrain.description)
                    })
                }
            }
        }
    }
}

@Composable
fun StazioniSearchTab(
    trainViewModel: TrainViewModel,
    onStationClick: (Station) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    val isSearching by trainViewModel.isSearching.collectAsState()
    val stationResults by trainViewModel.searchStationResults.collectAsState()
    val recentStations by trainViewModel.recentStations.collectAsState()

    LaunchedEffect(query) {
        if (query.length >= 2) {
            trainViewModel.searchStations(query)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Nome stazione...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { 
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF9500),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(10.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.length < 2) {
                if (recentStations.isNotEmpty()) {
                    item {
                        Text(
                            text = "STAZIONI RECENTI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(recentStations) { station ->
                        StationResultRow(station, {
                            trainViewModel.addToRecentStations(station)
                            focusManager.clearFocus()
                            onStationClick(station)
                        })
                    }
                }
            } else {
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFF9500))
                        }
                    }
                }
                items(stationResults) { vtStation ->
                    val cleanName = vtStation.nomeLungo.lowercase().replaceFirstChar { it.titlecase() }
                    val possibleRfi = trainViewModel.getRfiID(vtStation.nomeLungo)
                    val station = Station(name = cleanName, rfiID = possibleRfi, vtID = vtStation.vtID)
                    
                    StationResultRow(station, {
                        trainViewModel.addToRecentStations(station)
                        focusManager.clearFocus()
                        onStationClick(station)
                    })
                }
            }
        }
    }
}

@Composable
fun TrainResultRow(savedTrain: SavedTrain, isFav: Boolean, onClick: () -> Unit, onFavClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Train, null, tint = Color(0xFFFF9500))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Treno ${savedTrain.number}", fontWeight = FontWeight.Bold)
                    Text("Direzione ${savedTrain.description}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onFavClick) {
                Icon(
                    if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                    null,
                    tint = if (isFav) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StationResultRow(station: Station, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFFF3B30))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(station.name, fontWeight = FontWeight.Bold)
                Text(if (station.rfiID != null) "Tabellone RFI attivo" else "Dati ViaggiaTreno", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WheelNumberPicker(
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    val list = range.toList()
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = list.indexOf(selectedValue))
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedValue) {
        val targetIndex = list.indexOf(selectedValue)
        if (targetIndex >= 0 && lazyListState.firstVisibleItemIndex != targetIndex) {
            lazyListState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val containerCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = visibleItems.minByOrNull {
                    val itemCenter = it.offset + it.size / 2
                    kotlin.math.abs(itemCenter - containerCenter)
                }
                if (closest != null && closest.index in list.indices) {
                    val newValue = list[closest.index]
                    if (newValue != selectedValue) {
                        onValueChange(newValue)
                    }
                    lazyListState.animateScrollToItem(closest.index)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .height(150.dp)
            .width(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 55.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(list.size) { index ->
                val value = list[index]
                val isSelected = value == selectedValue
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                onValueChange(value)
                                lazyListState.animateScrollToItem(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", value),
                        fontSize = if (isSelected) 22.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFFF9500) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun WheelTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelNumberPicker(
            range = 0..23,
            selectedValue = selectedHour,
            onValueChange = { onTimeSelected(it, selectedMinute) }
        )
        Text(
            text = ":",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF9500),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        WheelNumberPicker(
            range = 0..59,
            selectedValue = selectedMinute,
            onValueChange = { onTimeSelected(selectedHour, it) }
        )
    }
}

@Composable
fun CustomCalendarPicker(
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    var currentMonthCalendar by remember(selectedDate) {
        mutableStateOf(Calendar.getInstance(Locale.ITALY).apply { time = selectedDate })
    }

    val monthName = currentMonthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ITALY)
        ?.replaceFirstChar { it.titlecase(Locale.ITALY) } ?: ""
    val year = currentMonthCalendar.get(Calendar.YEAR)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$monthName $year",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val newCal = (currentMonthCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                        currentMonthCalendar = newCal
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mese precedente")
                    }
                    IconButton(onClick = {
                        val newCal = (currentMonthCalendar.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                        currentMonthCalendar = newCal
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mese successivo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val daysOfWeekLabels = listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM")
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeekLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val firstDayCal = (currentMonthCalendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
            val offset = when (firstDayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            val daysInMonth = currentMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

            val totalCells = offset + daysInMonth
            val rowsCount = (totalCells + 6) / 7
            val selectedCal = Calendar.getInstance().apply { time = selectedDate }

            for (r in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNumber = cellIndex - offset + 1
                        if (dayNumber in 1..daysInMonth) {
                            val isSelected = selectedCal.get(Calendar.YEAR) == currentMonthCalendar.get(Calendar.YEAR) &&
                                    selectedCal.get(Calendar.MONTH) == currentMonthCalendar.get(Calendar.MONTH) &&
                                    selectedCal.get(Calendar.DAY_OF_MONTH) == dayNumber

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(
                                        color = if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        val newDateCal = (currentMonthCalendar.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, dayNumber)
                                        }
                                        onDateSelected(newDateCal.time)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactDatePickerDialog(
    initialDate: Date,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDate) }) {
                Text("Conferma", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Text("Seleziona data", fontWeight = FontWeight.Bold)
        },
        text = {
            CustomCalendarPicker(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it }
            )
        }
    )
}

@Composable
fun CompactTimePickerDialog(
    initialDate: Date,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    val calendar = remember { Calendar.getInstance().apply { time = initialDate } }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val newCal = Calendar.getInstance().apply {
                    time = initialDate
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }
                onConfirm(newCal.time)
            }) {
                Text("Conferma", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Text("Seleziona ora", fontWeight = FontWeight.Bold)
        },
        text = {
            WheelTimePicker(
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                onTimeSelected = { h, m ->
                    selectedHour = h
                    selectedMinute = m
                }
            )
        }
    )
}
