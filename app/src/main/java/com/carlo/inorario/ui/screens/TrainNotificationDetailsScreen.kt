package com.carlo.inorario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.Stop
import com.carlo.inorario.ui.viewmodel.TrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainNotificationDetailsScreen(
    trainNumber: String,
    trainViewModel: TrainViewModel,
    onBackClick: () -> Unit,
) {
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()
    val favoriteTrainsStops by trainViewModel.favoriteTrainsStops.collectAsState()

    val train = favoriteTrains.firstOrNull { it.number == trainNumber }
    val stops: List<Stop> = favoriteTrainsStops[trainNumber] ?: emptyList()

    // Load stops when station-pass toggle is on or user opens this screen
    LaunchedEffect(trainNumber) {
        if (stops.isEmpty()) {
            trainViewModel.fetchStopsForFavorite(trainNumber)
        }
    }

    if (train == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Treno non trovato", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }

    val hasOrigin = train.origin.isNotEmpty()
    val hasTimes = train.departureTime.isNotEmpty()

    // Compute display text for origin → destination
    val descParts = train.description.split(" - ", limit = 2)
    val destinationDisplay = if (descParts.size >= 2) descParts[1] else train.description
    val routeDisplay = when {
        hasOrigin && destinationDisplay.isNotEmpty() -> "${train.origin} → $destinationDisplay"
        hasOrigin -> train.origin
        train.description.isNotEmpty() -> train.description
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Treno ${train.number}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (routeDisplay.isNotEmpty()) {
                            Text(
                                text = routeDisplay,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // --- Train header info ---
            if (hasTimes) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Corsa: ${train.departureTime}" + if (train.arrivalTime.isNotEmpty()) " → ${train.arrivalTime}" else "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // --- Notification section ---
            item {
                Text(
                    text = "NOTIFICHE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    // --- 1. Notify Delay ---
                    NotifToggleRow(
                        label = "Variazione Ritardo",
                        description = "Ti avvisa quando il ritardo del treno cambia.",
                        iconColor = Color(0xFFFF9500),
                        checked = train.notifyDelay,
                        onCheckedChange = { newVal ->
                            trainViewModel.updateFavoriteTrainNotifications(
                                trainNumber = train.number,
                                notifyDelay = newVal,
                                notifyDeparture = if (newVal) train.notifyDeparture else false,
                                notifyStationPass = if (newVal) train.notifyStationPass else false,
                                stationPassName = train.stationPassName,
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // --- 2. Notify Departure ---
                    NotifToggleRow(
                        label = "Partenza dall'Origine",
                        description = "Ti avvisa quando il treno parte dalla stazione di origine.",
                        iconColor = Color(0xFF34C759),
                        checked = train.notifyDeparture,
                        enabled = train.notifyDelay,
                        onCheckedChange = { newVal ->
                            trainViewModel.updateFavoriteTrainNotifications(
                                trainNumber = train.number,
                                notifyDelay = train.notifyDelay,
                                notifyDeparture = newVal,
                                notifyStationPass = train.notifyStationPass,
                                stationPassName = train.stationPassName
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // --- 3. Notify Station Pass ---
                    NotifToggleRow(
                        label = "Passaggio Stazione",
                        description = "Ti avvisa quando il treno transita dalla stazione selezionata.",
                        iconColor = Color(0xFF007AFF),
                        checked = train.notifyStationPass,
                        enabled = train.notifyDelay,
                        onCheckedChange = { newVal ->
                            trainViewModel.updateFavoriteTrainNotifications(
                                trainNumber = train.number,
                                notifyDelay = train.notifyDelay,
                                notifyDeparture = train.notifyDeparture,
                                notifyStationPass = newVal,
                                stationPassName = train.stationPassName
                            )
                            // Fetch stops if enabling
                            if (newVal && stops.isEmpty()) {
                                trainViewModel.fetchStopsForFavorite(train.number)
                            }
                        }
                    )
                }
            }

            // --- Station selector when station pass is on ---
            if (train.notifyDelay && train.notifyStationPass) {
                item {
                    Text(
                        text = "SELEZIONA STAZIONE DI PASSAGGIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                if (stops.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF007AFF)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Caricamento stazioni...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                } else {
                    items(stops) { stop ->
                        val isSelected = (stop.stationName == train.stationPassName) ||
                            ((train.stationPassName == null) && (stops.firstOrNull() == stop))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF007AFF).copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    trainViewModel.updateFavoriteTrainNotifications(
                                        trainNumber = train.number,
                                        notifyDelay = train.notifyDelay,
                                        notifyDeparture = train.notifyDeparture,
                                        notifyStationPass = true,
                                        stationPassName = stop.stationName
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                        .background(
                                            if (isSelected) Color(0xFF007AFF)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                )
                                Column {
                                    Text(
                                        text = stop.stationName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ore ${stop.time}" + if (stop.delay > 0) " (+${stop.delay}')" else "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selezionata",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String,
    description: String,
    iconColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = if (enabled) 0.12f else 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationAdd,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = alpha),
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = if (!enabled) "Abilita prima la notifica ritardo" else description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.55f)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else { _ -> },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconColor
            )
        )
    }
}
