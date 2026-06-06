package com.carlo.inorario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.carlo.inorario.ui.viewmodel.MetroViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroFrequenciesScreen(
    metroLine: MetroLine,
    metroViewModel: MetroViewModel,
    onBackClick: () -> Unit
) {
    val allSchedules by metroViewModel.allSchedules.collectAsState()
    val isOfflineMode by metroViewModel.isOfflineMode.collectAsState()
    
    val cacheKey = "${metroLine.pdfID.orEmpty()}_${metroLine.direction}"
    val schedule = allSchedules[cacheKey]
    val isOffline = isOfflineMode[cacheKey] ?: false

    var selectedDayType by remember { mutableStateOf(DayType.current) }

    // Sync schedule when screen opens
    LaunchedEffect(key1 = metroLine.pdfID) {
        metroLine.pdfID?.let {
            metroViewModel.syncMetroSchedule(
                metroName = metroLine.name,
                pdfID = it,
                direction = metroLine.direction
            )
        }
    }

    val metroColor = when (metroLine.colorName) {
        "red" -> Color.Red
        "green" -> Color(0xFF009640)
        "purple" -> Color(0xFF8E44AD)
        "yellow" -> Color(0xFFFFCC00)
        "blue" -> Color(0xFF007AFF)
        "orange" -> Color(0xFFFF9500)
        else -> Color.Gray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(metroColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = metroLine.name.take(2),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = metroLine.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Indietro")
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
                .padding(horizontal = 16.dp)
        ) {
            // Day selection tab row
            TabRow(
                selectedTabIndex = DayType.values().indexOf(selectedDayType),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                DayType.values().forEach { type ->
                    Tab(
                        selected = selectedDayType == type,
                        onClick = { selectedDayType = type },
                        text = {
                            Text(
                                text = when (type) {
                                    DayType.FERIALI -> "Feriali"
                                    DayType.SABATO -> "Sabato"
                                    DayType.FESTIVO -> "Festivi"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (schedule == null) {
                // Loading or missing data state
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = metroColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scaricamento orari in corso...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val todayData = when (selectedDayType) {
                    DayType.FERIALI -> schedule.feriali
                    DayType.SABATO -> schedule.sabato
                    DayType.FESTIVO -> schedule.festivo
                }

                if (todayData.isEmpty()) {
                    // No data found or static frequency mode fallback
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Orario esatto non disponibile per questo giorno.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = metroLine.customFrequencies?.get(selectedDayType)
                                ?: schedule.frequenze[selectedDayType.name]
                                ?: "Servizio frequente in base ai passaggi reali in stazione.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Timetable scrollable table
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(metroColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nota: Gli orari indicano i passaggi programmati. Eventuali interruzioni o ritardi straordinari non sono calcolati offline.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Hours list sorted
                        val hours = todayData.keys.toList().sorted()
                        items(hours) { hour ->
                            val departures = todayData[hour] ?: emptyList()
                            val minutesSorted = departures.sortedBy { it.min }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hour block
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(metroColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%02d", hour),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = metroColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Minutes flow
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        minutesSorted.forEach { departure ->
                                            val destName = metroLine.destinations?.get(departure.color)
                                            val formattedMin = String.format(java.util.Locale.US, "%02d", departure.min)
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = formattedMin,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (destName != null) {
                                                    Text(
                                                        text = destName.take(3).uppercase(),
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
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
        }
    }
}
