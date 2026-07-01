package com.carlo.inorario.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.carlo.inorario.data.model.FormattedDeparture

import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.ui.viewmodel.MetroViewModel

enum class FeasibilityStatus(val label: String, val icon: String, val color: Color, val bg: Color) {
    OK("Ce la fai", "✓", Color(0xFF34C759), Color(0xFF34C759).copy(alpha = 0.1f)),
    HURRY("Sbrigati", "🚶", Color(0xFFFF9500), Color(0xFFFF9500).copy(alpha = 0.08f)),
    RUN("Corri!", "⚡", Color(0xFFFF9500), Color(0xFFFF9500).copy(alpha = 0.12f)),
    MISS("Non ce la fai", "✗", Color(0xFFFF3B30), Color(0xFFFF3B30).copy(alpha = 0.1f)),
    UNKNOWN("--", "-", Color.Gray, Color.Gray.copy(alpha = 0.1f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroQuickViewBottomSheet(
    station: Station,
    timeContext: String,
    metroViewModel: MetroViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val hasMetro = station.metroLines.isNotEmpty()
    
    val liveDepartures by metroViewModel.liveDepartures.collectAsState()
    val isOfflineMode by metroViewModel.isOfflineMode.collectAsState()

    // Sync live departures on launch
    LaunchedEffect(station, timeContext) {
        station.metroLines.forEach { metro ->
            metro.pdfID?.let { pdfID ->
                metroViewModel.syncLiveDepartures(
                    metroName = metro.name,
                    pdfID = pdfID,
                    direction = metro.direction,
                    time = timeContext
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = station.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Arrivo treno previsto: $timeContext",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            if (!hasMetro) {
                Text(
                    text = "Nessuna metropolitana disponibile in questa stazione.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(station.metroLines) { metro ->
                        val lineCode = metro.name.take(2)
                        val cacheKey = "${lineCode}_${metro.pdfID.orEmpty()}_${metro.direction}_$timeContext"
                        val isOffline = isOfflineMode[cacheKey] ?: false
                        
                        val liveResponse = liveDepartures[cacheKey]

                        // 1. Get raw departures (server response)
                        val rawDeps = liveResponse?.departures?.map { FormattedDeparture(it.time, it.destination) } ?: emptyList()

                        // 2. Filter departures with margin >= 0 (only future ones)
                        val usableDeps = rawDeps.filter { dep ->
                            val margin = getMarginMinutes(dep.timeString, timeContext)
                            margin != null && margin >= 0
                        }

                        val displayDeps = usableDeps.take(2)
                        val metroColor = getMetroColor(metro.colorName)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge Line (e.g. circle M1, M2, etc.)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(metroColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = metro.name.take(2),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DIREZIONE " + metro.name.substringAfter(" ").uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (displayDeps.isEmpty()) {
                                    Text(
                                        text = if (liveResponse == null) "Caricamento in corso..." else "Nessun transito utile",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        displayDeps.forEach { dep ->
                                            val margin = getMarginMinutes(dep.timeString, timeContext) ?: 0.0
                                            val feasibility = getFeasibility(station.name, margin)
                                            val cleanDest = cleanDestinationName(dep.destinationName ?: "")

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Chip for departure
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(feasibility.bg)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = dep.timeString,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (feasibility == FeasibilityStatus.UNKNOWN) MaterialTheme.colorScheme.onSurface else feasibility.color
                                                    )
                                                    Text(
                                                        text = feasibility.icon,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp,
                                                        color = feasibility.color
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = cleanDest,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

// Feasibility logic helper
fun getFeasibility(stationName: String, margin: Double): FeasibilityStatus {
    val name = stationName.lowercase()
    
    // M1
    if (name.contains("sesto")) {
        return when {
            margin >= 3.0 -> FeasibilityStatus.OK
            margin >= 2.0 -> FeasibilityStatus.HURRY
            margin >= 1.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("cadorna")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 3.0 -> FeasibilityStatus.HURRY
            margin >= 2.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("venezia")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 3.0 -> FeasibilityStatus.HURRY
            margin >= 2.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("rho fiera")) {
        return when {
            margin >= 6.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    
    // M2
    if (name.contains("centrale")) {
        return when {
            margin >= 6.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("garibaldi")) {
        return when {
            margin >= 7.0 -> FeasibilityStatus.OK
            margin >= 5.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("lambrate")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("genova")) {
        return when {
            margin >= 6.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("romolo")) {
        return when {
            margin >= 4.0 -> FeasibilityStatus.OK
            margin >= 3.0 -> FeasibilityStatus.HURRY
            margin >= 2.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    
    // M3
    if (name.contains("affori")) {
        return when {
            margin >= 2.0 -> FeasibilityStatus.OK
            margin >= 1.5 -> FeasibilityStatus.HURRY
            margin >= 1.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("repubblica")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 3.0 -> FeasibilityStatus.HURRY
            margin >= 2.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("lodi") || name.contains("romana") || name.contains("tibb")) {
        return when {
            margin >= 10.0 -> FeasibilityStatus.OK
            margin >= 8.0 -> FeasibilityStatus.HURRY
            margin >= 6.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("rogoredo")) {
        return when {
            margin >= 6.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    
    // M4
    if (name.contains("cristoforo")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 3.0 -> FeasibilityStatus.HURRY
            margin >= 2.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("dateo")) {
        return when {
            margin >= 5.0 -> FeasibilityStatus.OK
            margin >= 4.0 -> FeasibilityStatus.HURRY
            margin >= 3.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    if (name.contains("forlanini")) {
        return when {
            margin >= 3.0 -> FeasibilityStatus.OK
            margin >= 2.0 -> FeasibilityStatus.HURRY
            margin >= 1.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    
    // M5
    if (name.contains("domodossola")) {
        return when {
            margin >= 3.0 -> FeasibilityStatus.OK
            margin >= 2.0 -> FeasibilityStatus.HURRY
            margin >= 1.0 -> FeasibilityStatus.RUN
            else -> FeasibilityStatus.MISS
        }
    }
    
    // Default fallback
    return when {
        margin >= 4.0 -> FeasibilityStatus.OK
        margin >= 3.0 -> FeasibilityStatus.HURRY
        margin >= 2.0 -> FeasibilityStatus.RUN
        else -> FeasibilityStatus.MISS
    }
}

// Compute margin minutes helper
fun getMarginMinutes(timeString: String, timeContext: String): Double? {
    val contextParts = timeContext.split(":")
    if (contextParts.size < 2) return null
    val cHour = contextParts[0].toIntOrNull() ?: return null
    val cMin = contextParts[1].toIntOrNull() ?: return null
    
    val timeParts = timeString.split(":")
    if (timeParts.size < 2) return null
    val tHour = timeParts[0].toIntOrNull() ?: return null
    val tMin = timeParts[1].toIntOrNull() ?: return null
    
    val currentTotal = cHour * 60 + cMin
    val trainTotal = tHour * 60 + tMin
    
    var diff = (trainTotal - currentTotal).toDouble()
    if (diff < -720.0) {
        diff += 1440.0
    } else if (diff > 720.0) {
        diff -= 1440.0
    }
    return diff
}


// Helper to get Color object from string colorName
fun getMetroColor(colorName: String?): Color {
    return when (colorName) {
        "red" -> Color.Red
        "green" -> Color(0xFF009640)
        "purple" -> Color(0xFF8E44AD)
        "yellow" -> Color(0xFFFFCC00)
        "blue" -> Color(0xFF007AFF)
        "orange" -> Color(0xFFFF9500)
        else -> Color.Gray
    }
}

// Clean destination name helper (similar to iOS)
fun cleanDestinationName(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("cologno nord") || lower.contains("cologno n") -> "Cologno N."
        lower.contains("cascina gobba") || lower.contains("c.gobba") || lower.contains("c. gobba") -> "C. Gobba"
        lower.contains("assago") -> "Assago"
        lower.contains("abbiategrasso") -> "Abbiategrasso"
        lower.contains("rho fiera") -> "Rho Fiera"
        lower.contains("bisceglie") -> "Bisceglie"
        lower.contains("sesto") -> "Sesto FS"
        lower.contains("comasina") -> "Comasina"
        lower.contains("donato") -> "San Donato"
        lower.contains("linate") -> "Linate"
        lower.contains("cristoforo") -> "S. Cristoforo"
        lower.contains("bignami") -> "Bignami"
        lower.contains("siro") -> "San Siro"
        else -> name
    }
}
