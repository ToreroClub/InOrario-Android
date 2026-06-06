package com.carlo.inorario.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.ui.viewmodel.TrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainStopsScreen(
    train: Train,
    trainViewModel: TrainViewModel,
    showCloseButton: Boolean = true,
    onBackClick: () -> Unit,
    onStationClick: (Station) -> Unit,
) {
    val context = LocalContext.current
    val stops by trainViewModel.selectedTrainStops.collectAsState()
    val status by trainViewModel.currentTrainStatus.collectAsState()
    val isStopsLoading by trainViewModel.isStopsLoading.collectAsState()
    val stopErrorMessage by trainViewModel.stopErrorMessage.collectAsState()
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()

    val isFavorite = favoriteTrains.any { it.number == train.number }

    LaunchedEffect(key1 = train.number) {
        trainViewModel.fetchStops(train, isRefresh = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Treno ${train.number}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = if (showCloseButton) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (showCloseButton) "Chiudi" else "Indietro"
                        )
                    }
                },
                actions = {
                    // Share Button
                    IconButton(
                        onClick = {
                            val delayMinutes = train.delay.replace("+", "").replace("'", "")
                            val isDelayed = !train.delay.contains("In orario")
                            val shareText = if (isDelayed) {
                                "Il treno ${train.number} è in ritardo di $delayMinutes minuti, ci vediamo dopo! 🐌"
                            } else {
                                "Il treno ${train.number} è in perfetto orario, a tra poco! 🚄"
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Condividi stato treno"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Condividi", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Favorite Star
                    IconButton(
                        onClick = {
                            trainViewModel.toggleFavorite(train.number, train.destination)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favoriti",
                            tint = if (isFavorite) Color(0xFFFFCC00) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { trainViewModel.fetchStops(train, isRefresh = true) },
                        enabled = !isStopsLoading
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Ricarica")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (isStopsLoading && stops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (stopErrorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stopErrorMessage.orEmpty(),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Train status header block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isOntime = status.statusMessage.contains("In orario")
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isOntime) Color(0xFF34C759) else (if (status.isDeparted) Color.Red else Color.Gray))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status.statusMessage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    status.cancellationNote?.let { note ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red.copy(alpha = 0.2f))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = note,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }

                    if (status.isDeparted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ultimo rilevamento: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = status.lastStation,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " alle ${status.lastTime}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Il treno non ha ancora lasciato la stazione di partenza.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Stops list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(stops) { stop ->
                        val isMagenta = stop.stationName.contains("Magenta", ignoreCase = true)
                        val rowBg = if (isMagenta) Color(0xFFFF9500).copy(alpha = 0.1f) else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .clickable {
                                    val station = resolveStationFromStopName(stop.stationName, trainViewModel)
                                    onStationClick(station)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stop.stationName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (stop.actualTime != null) {
                                    val delayColor = when {
                                        stop.delay <= 2 -> Color(0xFF34C759)
                                        stop.delay <= 6 -> Color(0xFFFF9500)
                                        else -> Color(0xFFFF3B30)
                                    }
                                    Text(
                                        text = "Effettivo: ${stop.actualTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = delayColor
                                    )
                                } else if (stop.estimatedTime != null) {
                                    Text(
                                        text = "Previsto: ${stop.estimatedTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF3B30)
                                    )
                                }
                            }

                            val isStrikethrough = (stop.actualTime == null) && (stop.estimatedTime != null)
                            Text(
                                text = stop.time,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

private fun resolveStationFromStopName(name: String, trainViewModel: TrainViewModel): Station {
    val clean = name.trim()

    // 1. Corrispondenza esatta (case-insensitive) in allRFIStations
    val exactRfi = trainViewModel.allRFIStations.firstOrNull { it.name.equals(clean, ignoreCase = true) }
    if (exactRfi != null) {
        return Station(
            name = exactRfi.name.lowercase().replaceFirstChar { it.titlecase() },
            rfiID = exactRfi.rfiID,
            vtID = exactRfi.vtID
        )
    }

    // 2. Corrispondenza parziale in allRFIStations
    val partialRfi = trainViewModel.allRFIStations.firstOrNull {
        it.name.contains(clean, ignoreCase = true) || clean.contains(it.name, ignoreCase = true)
    }
    if (partialRfi != null) {
        return Station(
            name = partialRfi.name.lowercase().replaceFirstChar { it.titlecase() },
            rfiID = partialRfi.rfiID,
            vtID = partialRfi.vtID
        )
    }

    // 3. Fallback su stazioni del Passante / Suburbane (SuburbanData)
    for (line in com.carlo.inorario.data.model.SuburbanData.allLines) {
        val subStation = line.stations.firstOrNull {
            it.name.equals(clean, ignoreCase = true) ||
            it.name.contains(clean, ignoreCase = true) ||
            clean.contains(it.name, ignoreCase = true)
        }
        if (subStation != null) {
            return subStation
        }
    }

    // 4. Se non troviamo nulla, restituiamo un oggetto Station generico
    val possibleRfi = trainViewModel.getRfiID(clean)
    return Station(
        name = clean.lowercase().replaceFirstChar { it.titlecase() },
        rfiID = possibleRfi,
        vtID = null
    )
}
