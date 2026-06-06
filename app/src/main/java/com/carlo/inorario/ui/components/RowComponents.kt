package com.carlo.inorario.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.DayType
import com.carlo.inorario.data.model.FormattedDeparture
import com.carlo.inorario.data.model.MetroDisplayMode
import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.ui.theme.getSuburbanColor

// 1. SuburbanLineBadge Component
@Composable
fun SuburbanLineBadge(
    id: String,
    modifier: Modifier = Modifier
) {
    val line = SuburbanData.allLines.firstOrNull { it.id == id }
    val backgroundColor = line?.let { getSuburbanColor(it.hexColor) } ?: Color(0xFFFF9500)
    val isLight = listOf("S4", "S5", "S6", "S8").contains(id.uppercase())
    val textColor = if (isLight) Color.Black else Color.White

    Box(
        modifier = modifier
            .size(width = 32.dp, height = 20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = id,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
    }
}

// 2. PassanteNodeView Component (Horizontal scrollbar node)
@Composable
fun PassanteNodeView(
    station: Station,
    isFirst: Boolean,
    isLast: Boolean,
    isNearby: Boolean,
    lineColor: Color,
    onClick: () -> Unit
) {
    val cleanName = station.name
        .replace("Milano ", "")
        .replace(" Passante", "")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(70.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rotated Text Label
        Box(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = cleanName,
                fontSize = 11.sp,
                fontWeight = if (isNearby) FontWeight.Bold else FontWeight.Medium,
                color = if (isNearby) lineColor else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .rotate(-45f)
                    .padding(bottom = 8.dp)
            )
        }

        // Metro dots
        Row(
            modifier = Modifier
                .height(10.dp)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val metroColors = station.metroLines.asSequence().map {
                when (it.colorName) {
                    "red" -> Color.Red
                    "green" -> Color(0xFF4CD964)
                    "purple" -> Color(0xFF8E44AD)
                    "yellow" -> Color(0xFFFFCC00)
                    "blue" -> Color(0xFF007AFF)
                    "orange" -> Color(0xFFFF9500)
                    else -> Color.Gray
                }
            }.distinct()

            metroColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Connection Line & Dot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (isFirst) Color.Transparent else lineColor.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(if (isLast) Color.Transparent else lineColor.copy(alpha = 0.6f))
                )
            }

            // Dot
            val finalScale = if (isNearby) pulseScale else 1.0f
            Box(
                modifier = Modifier
                    .size(if (isNearby) 16.dp else 12.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(16.dp * finalScale)
                ) {
                    drawCircle(
                        color = if (isNearby) lineColor else Color.White,
                        radius = (size.minDimension / 2)
                    )
                    drawCircle(
                        color = if (isNearby) lineColor else Color.Gray.copy(alpha = 0.5f),
                        radius = (size.minDimension / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = if (isNearby) 3.dp.toPx() else 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

// 3. MetroRowView Component
@Composable
fun MetroRowView(
    metro: MetroLine,
    isOffline: Boolean,
    scheduleLoaded: Boolean,
    displayMode: MetroDisplayMode,
    onSyncRequested: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val metroColor = when (metro.colorName) {
        "red" -> Color.Red
        "green" -> Color(0xFF009640)
        "purple" -> Color(0xFF8E44AD)
        "yellow" -> Color(0xFFFFCC00)
        "blue" -> Color(0xFF007AFF)
        "orange" -> Color(0xFFFF9500)
        else -> Color.Gray
    }

    LaunchedEffect(key1 = metro.pdfID) {
        onSyncRequested()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (onClick != null) modifier.clickable { onClick() } else modifier
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line code badge (circle)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(metroColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = metro.name.take(2),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name, Offline status, and timings
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metro.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOffline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFF9500).copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFLINE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            when (displayMode) {
                is MetroDisplayMode.Closed -> {
                    Text(
                        text = "Servizio terminato",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                is MetroDisplayMode.Frequency -> {
                    Text(
                        text = displayMode.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is MetroDisplayMode.Exact -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        displayMode.departures.forEach { departure ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = departure.timeString,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                departure.destinationName?.let { dest ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dest.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Network cache Sync State indicator (Green/Red)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (scheduleLoaded) Color(0xFF34C759) else Color(0xFFFF3B30))
        )
    }
}

// 4. TrainRowView Component
@Composable
fun TrainRowView(
    train: Train,
    displayPlatform: String,
    showPassanteTag: Boolean,
    passanteBranch: String?,
    isFilterActive: Boolean,
    modifier: Modifier = Modifier
) {
    val isDelayed = !train.delay.contains("In orario") && !train.delay.contains("0'") && train.delay.isNotEmpty()
    val isCancelled = train.delay.lowercase().contains("cancellato") || train.delay.lowercase().contains("soppresso")

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val rowBg = when {
        isFilterActive && isCancelled -> Color(0xFFFF3B30).copy(alpha = 0.15f)
        isFilterActive && isDelayed -> Color(0xFFFF9500).copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = if (isFilterActive && (isDelayed || isCancelled)) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Train specs & category
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getTrainCategoryColor(train.category))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = train.category,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Full Category name (readable name e.g. Regionale)
                Text(
                    text = getFullCategoryName(train.category, train.destination),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Train number
                Text(
                    text = train.number,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Train Destination Name + optional Passante tag
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = train.destination,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (showPassanteTag && (passanteBranch != null)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    val branchColor = if (passanteBranch == "Bovisa" || passanteBranch == "Rogoredo") Color(0xFFFF3B30) else Color(0xFFFF9500)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(branchColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = passanteBranch.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = branchColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Time and delay / platform
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = train.time,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delay text
                val isOntime = train.delay.contains("In orario")
                Text(
                    text = train.delay,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOntime) Color(0xFF34C759) else Color(0xFFFF3B30),
                    modifier = Modifier.alpha(if (isOntime) pulseAlpha else 1.0f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Platform tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Bin. $displayPlatform",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFCC00),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// 5. PassanteTrainRowView Component
@Composable
fun PassanteTrainRowView(
    train: Train,
    onTrainClick: () -> Unit
) {
    val isCancelled = train.delay.lowercase().contains("soppresso") || train.delay.lowercase().contains("cancellato")
    val delayStr = train.delay.replace("+", "").replace("'", "")
    val delayMinutes = if (delayStr.lowercase().contains("orario")) 0 else (delayStr.toIntOrNull() ?: 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrainClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Line Badge
        SuburbanLineBadge(id = train.category.ifEmpty { "S" })

        Spacer(modifier = Modifier.width(10.dp))

        // Destination + time
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formatDestination(train.destination),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Part. ${train.time}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Delay state
        if (isCancelled) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFF3B30))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Soppresso",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else if (delayMinutes > 0) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "+$delayMinutes'",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF3B30)
                )
                Text(
                    text = "ritardo",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3B30).copy(alpha = 0.8f)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "In orario",
                tint = Color(0xFF34C759),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// 6. PassanteBranchTrainRow Component
@Composable
fun PassanteBranchTrainRow(
    train: Train,
    isLarge: Boolean,
    onTrainClick: () -> Unit
) {
    val isCancelled = train.delay.lowercase().contains("soppresso") || train.delay.lowercase().contains("cancellato")
    val delayStr = train.delay.replace("+", "").replace("'", "")
    val delayMinutes = if (delayStr.lowercase().contains("orario")) 0 else (delayStr.toIntOrNull() ?: 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrainClick() }
            .padding(vertical = if (isLarge) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuburbanLineBadge(
            id = if (train.category.isEmpty()) "S" else train.category,
            modifier = Modifier.size(width = if (isLarge) 36.dp else 28.dp, height = if (isLarge) 22.dp else 18.dp)
        )

        Spacer(modifier = Modifier.width(if (isLarge) 8.dp else 6.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = formatDestination(train.destination),
                fontSize = if (isLarge) 13.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Part. ${train.time}",
                fontSize = if (isLarge) 10.sp else 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        if (isCancelled) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "CANCELLATO",
                    fontSize = if (isLarge) 11.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3B30)
                )
            }
        } else if (delayMinutes > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+$delayMinutes'",
                    fontSize = if (isLarge) 14.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF3B30)
                )
                if (isLarge) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "rit.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF3B30).copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLarge) {
                    Text(
                        text = "IN ORARIO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ok",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(if (isLarge) 16.dp else 12.dp)
                )
            }
        }
    }
}

// Private Helpers
private fun getTrainCategoryColor(category: String): Color {
    val cat = category.uppercase()
    return when {
        cat.contains("FR") || cat.contains("ITA") || cat == "AV" || cat == "NTV" -> Color.Red
        cat == "IC" || cat == "EC" -> Color.Gray
        cat.contains("S") -> Color(0xFF009640)
        cat.contains("RV") || cat.contains("RE") -> Color(0xFF007AFF)
        else -> Color.Gray
    }
}

private fun getFullCategoryName(c: String, dest: String): String {
    val cat = c.uppercase()
    return when {
        cat.contains("FR") -> "Frecciarossa"
        cat.contains("RV") -> "Regionale Veloce"
        cat.contains("AV") || cat.contains("ALTA VELOCIT") -> "Alta Velocità"
        cat.contains("IC") -> "Intercity"
        cat.contains("EC") -> "Eurocity"
        cat.contains("S6") || (cat == "S" && (dest.lowercase().contains("novara") || dest.lowercase().contains("treviglio"))) -> "Suburbano"
        cat.contains("NTV") || cat.contains("ITA") -> "Italo"
        else -> "Treno"
    }
}

fun formatDestination(name: String): String {
    if (name == "Milano Centrale") return name

    var dest = name
    dest = dest.replace("Milano Porta Garibaldi Passante", "Milano P. Garibaldi")
    dest = dest.replace("Milano Porta Garibaldi", "Milano P. Garibaldi")
    dest = dest.replace("Porta Garibaldi Passante", "Milano P. Garibaldi")
    dest = dest.replace("Porta Garibaldi", "Milano P. Garibaldi")

    if (dest == "Milano Porta Venezia" || dest == "Porta Venezia" || dest == "Venezia") {
        dest = "P. Venezia"
    } else {
        dest = dest.replace("Milano Porta Venezia", "P. Venezia")
        dest = dest.replace("Porta Venezia", "P. Venezia")
    }

    if (dest == "Milano Porta Vittoria" || dest == "Porta Vittoria" || dest == "Vittoria") {
        dest = "P. Vittoria"
    } else {
        dest = dest.replace("Milano Porta Vittoria", "P. Vittoria")
        dest = dest.replace("Porta Vittoria", "P. Vittoria")
    }

    dest = dest.replace("Milano Repubblica", "Repubblica")
    dest = dest.replace("Milano Dateo", "Dateo")
    dest = dest.replace("Milano Lancetti", "Lancetti")

    if (dest.contains("Porta ") && !dest.contains("Milano P. ") && !dest.contains("P. ")) {
        dest = dest.replace("Porta ", "P. ")
    }

    return dest
}
