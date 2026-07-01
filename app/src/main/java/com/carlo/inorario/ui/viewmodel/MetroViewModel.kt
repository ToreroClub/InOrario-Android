package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.model.FormattedDeparture
import com.carlo.inorario.data.model.MetroDeparturesResponse
import com.carlo.inorario.data.model.MetroDisplayMode
import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MetroViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _isOfflineMode = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isOfflineMode = _isOfflineMode.asStateFlow()

    // Departures cache: key is line_pdfID_direction_time
    private val _allSchedules = MutableStateFlow<Map<String, MetroDeparturesResponse>>(emptyMap())
    val allSchedules: StateFlow<Map<String, MetroDeparturesResponse>> = _allSchedules.asStateFlow()

    // For backwards compatibility with QuickView BottomSheet
    val liveDepartures: StateFlow<Map<String, MetroDeparturesResponse>> = allSchedules

    private val activeSyncs = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val lastFetchTime = java.util.Collections.synchronizedMap(mutableMapOf<String, Long>())
    private val cacheTTL = 60000L // 1 minute cache TTL

    // Keep syncMetroSchedule signature as a fallback/stub for compatibility, mapping to syncLiveDepartures
    fun syncMetroSchedule(metroName: String, pdfID: String, direction: Int) {
        syncLiveDepartures(metroName, pdfID, direction)
    }

    // Sync live departures
    fun syncLiveDepartures(
        metroName: String,
        pdfID: String,
        direction: Int,
        time: String? = null,
        force: Boolean = false
    ) {
        val lineCode = metroName.take(2)
        val cacheKey = "${lineCode}_${pdfID}_${direction}_${time.orEmpty()}"

        // Fresh data in cache? Skip call (unless forced)
        if (!force) {
            val lastFetch = lastFetchTime[cacheKey]
            if (lastFetch != null && (System.currentTimeMillis() - lastFetch) < cacheTTL && _allSchedules.value[cacheKey] != null) {
                return
            }
        }

        if (activeSyncs.contains(cacheKey)) return
        activeSyncs.add(cacheKey)

        viewModelScope.launch {
            try {
                val response = NetworkClient.backendService.getMetroDepartures(
                    line = lineCode,
                    pdfID = pdfID,
                    direction = direction,
                    time = time
                )
                _allSchedules.update { it.toMutableMap().apply { put(cacheKey, response) } }
                _isOfflineMode.update { it.toMutableMap().apply { put(cacheKey, false) } }
                lastFetchTime[cacheKey] = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                if (_allSchedules.value[cacheKey] == null) {
                    _isOfflineMode.update { it.toMutableMap().apply { put(cacheKey, true) } }
                }
            } finally {
                activeSyncs.remove(cacheKey)
            }
        }
    }

    // Overload for current time logic in StationBoardScreen
    fun getNextDepartures(metro: MetroLine, now: Date = Date()): MetroDisplayMode {
        return getNextDepartures(metro, null, now)
    }

    // Main implementation mapping to iOS getNextDepartures
    fun getNextDepartures(metro: MetroLine, time: String? = null, now: Date = Date()): MetroDisplayMode {
        val calendar = Calendar.getInstance()
        calendar.time = now
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (hour in 2..4) {
            return MetroDisplayMode.Closed
        }

        val line = metro.name.take(2)
        val cacheKey = "${line}_${metro.pdfID.orEmpty()}_${metro.direction}_${time.orEmpty()}"
        val response = _allSchedules.value[cacheKey]
            ?: return MetroDisplayMode.Frequency("In aggiornamento...")

        if (response.departures.isEmpty()) {
            return MetroDisplayMode.Frequency("Nessuna partenza programmata")
        }

        // Prefix 4 departures, mapping to FormattedDeparture
        val departures = response.departures.take(4).map { dep ->
            FormattedDeparture(dep.time, dep.destination)
        }

        return MetroDisplayMode.Exact(departures)
    }
}
