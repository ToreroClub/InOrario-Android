package com.carlo.inorario.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.ui.theme.getSuburbanColor
import com.carlo.inorario.ui.viewmodel.TrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizePassanteScreen(
    trainViewModel: TrainViewModel,
    onBackClick: () -> Unit,
) {
    val selectedLines by trainViewModel.selectedSuburbanLines.collectAsState()
    val hiddenStations by trainViewModel.hiddenSuburbanStations.collectAsState()

    var selectedLineTab by remember { mutableStateOf("S5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Personalizza Passante",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "LINEE SUBURBANE ATTIVE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Horizontal grid of lines (checkboxes/chips)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuburbanData.allLines.forEach { line ->
                    val isChecked = selectedLines.contains(line.id)
                    val color = getSuburbanColor(line.hexColor)
                    
                    FilterChip(
                        selected = isChecked,
                        onClick = { trainViewModel.toggleSuburbanLine(line.id) },
                        label = {
                            Text(
                                text = line.id,
                                fontWeight = FontWeight.Bold,
                                color = if (isChecked) Color.White else color
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isChecked,
                            selectedBorderColor = Color.Transparent,
                            borderColor = color.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FILTRA STAZIONI DELLA LINEA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Tabs for line selection to configure stations
            ScrollableTabRow(
                selectedTabIndex = SuburbanData.allLines.indexOfFirst { it.id == selectedLineTab }.coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                SuburbanData.allLines.forEach { line ->
                    Tab(
                        selected = selectedLineTab == line.id,
                        onClick = { selectedLineTab = line.id },
                        text = {
                            Text(
                                text = line.id,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedLineTab == line.id) getSuburbanColor(line.hexColor) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stations list for the selected tab line
            val activeLine = SuburbanData.allLines.find { it.id == selectedLineTab }
            if (activeLine != null) {
                val stations = activeLine.stations
                val lineHiddenStations = hiddenStations[activeLine.id] ?: emptyList()

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stations) { station ->
                        val isHidden = lineHiddenStations.contains(station.name)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { trainViewModel.toggleHiddenStation(activeLine.id, station.name) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = station.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHidden) "Stazione nascosta sulla linea ${activeLine.id}" else "Attiva e monitorata",
                                        fontSize = 11.sp,
                                        color = if (isHidden) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isHidden) "Mostra" else "Nascondi",
                                    tint = if (isHidden) Color.Gray else getSuburbanColor(activeLine.hexColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
