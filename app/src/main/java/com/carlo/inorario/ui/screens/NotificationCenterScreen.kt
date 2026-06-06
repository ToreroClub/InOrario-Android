package com.carlo.inorario.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlo.inorario.ui.viewmodel.ProfileViewModel
import com.carlo.inorario.ui.viewmodel.TrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    profileViewModel: ProfileViewModel,
    trainViewModel: TrainViewModel,
    onBackClick: () -> Unit,
    onTrainClick: (trainNumber: String) -> Unit
) {
    val notificationsEnabled by profileViewModel.remoteNotificationsEnabled.collectAsState()
    val fcmToken by profileViewModel.fcmToken.collectAsState()
    val favoriteTrains by trainViewModel.favoriteTrains.collectAsState()

    // Enrich any existing favorite trains that are missing origin/departure/arrival data
    LaunchedEffect(favoriteTrains.size) {
        favoriteTrains.forEach { train ->
            if (train.origin.isEmpty() || train.departureTime.isEmpty() || train.arrivalTime.isEmpty()) {
                trainViewModel.enrichFavoriteTrainData(train.number)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            profileViewModel.saveRemoteNotificationsEnabled(enabled = true)
            trainViewModel.syncRemoteNotifications(true, fcmToken)
        } else {
            profileViewModel.saveRemoteNotificationsEnabled(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Centro Notifiche",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Global toggle card ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "IMPOSTAZIONE PRINCIPALE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newValue = !notificationsEnabled
                                if (newValue) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        profileViewModel.saveRemoteNotificationsEnabled(enabled = true)
                                        trainViewModel.syncRemoteNotifications(true, fcmToken)
                                    }
                                } else {
                                    profileViewModel.saveRemoteNotificationsEnabled(false)
                                    trainViewModel.syncRemoteNotifications(false, fcmToken)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (notificationsEnabled)
                                            Brush.radialGradient(listOf(Color(0xFFFF9500), Color(0xFFFF6B00)))
                                        else
                                            Brush.radialGradient(listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.2f)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (notificationsEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Notifiche Stato Treno",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (notificationsEnabled) "Attive per i tuoi treni preferiti" else "Disabilitate",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        profileViewModel.saveRemoteNotificationsEnabled(enabled = true)
                                        trainViewModel.syncRemoteNotifications(true, fcmToken)
                                    }
                                } else {
                                    profileViewModel.saveRemoteNotificationsEnabled(false)
                                    trainViewModel.syncRemoteNotifications(false, fcmToken)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF9500)
                            )
                        )
                    }
                }
            }

            // --- Train list ---
            if (favoriteTrains.isNotEmpty()) {
                item {
                    Text(
                        text = "I TUOI TRENI PREFERITI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                items(favoriteTrains) { train ->
                    val isActive = notificationsEnabled && train.notifyDelay
                    val hasOrigin = train.origin.isNotEmpty()
                    val hasDestination = train.description.isNotEmpty()
                    val hasTimes = train.departureTime.isNotEmpty()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(enabled = notificationsEnabled) { onTrainClick(train.number) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isActive) Color(0xFF007AFF).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = if (isActive) Color(0xFF007AFF) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Text info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Treno ${train.number}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Origin → Destination line
                            if (hasOrigin || hasDestination) {
                                // description is stored as "Origin - Destination"
                                val parts = train.description.split(" - ", limit = 2)
                                val displayText = if (hasOrigin && parts.size >= 2) {
                                    "${train.origin} → ${parts[1]}"
                                } else if (hasOrigin) {
                                    "${train.origin} → ${train.description}"
                                } else {
                                    train.description
                                }
                                Text(
                                    text = displayText,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            if (hasTimes) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = train.departureTime + if (train.arrivalTime.isNotEmpty()) " → ${train.arrivalTime}" else "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }

                            // Active badge
                            if (notificationsEnabled) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val notifLabels = buildList {
                                    if (train.notifyDelay) add("Ritardo")
                                    if (train.notifyDeparture) add("Partenza")
                                    if (train.notifyStationPass && (train.stationPassName != null)) add("▶ ${train.stationPassName}")
                                }
                                if (notifLabels.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        notifLabels.forEach { label ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color(0xFF34C759).copy(alpha = 0.12f))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF34C759)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Nessuna notifica configurata",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }

                        if (notificationsEnabled) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nessun treno preferito",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Aggiungi treni ai preferiti dalla ricerca per configurare le notifiche.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
