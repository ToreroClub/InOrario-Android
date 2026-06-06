package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.model.DayType
import com.carlo.inorario.data.model.FormattedDeparture
import com.carlo.inorario.data.model.FullSchedule
import com.carlo.inorario.data.model.MetroDisplayMode
import com.carlo.inorario.data.model.MetroLine
import com.carlo.inorario.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MetroViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _isOfflineMode = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isOfflineMode = _isOfflineMode.asStateFlow()

    val allSchedules: StateFlow<Map<String, FullSchedule>> = dataStoreManager.metroCacheFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun syncMetroSchedule(metroName: String, pdfID: String, direction: Int) {
        val cacheKey = "${pdfID}_$direction"
        if (_isOfflineMode.value[cacheKey] == true) return

        viewModelScope.launch {
            try {
                // e.g. name prefix is M1, M2, M3, M4, M5
                val lineCode = metroName.take(2)
                val schedule = NetworkClient.backendService.getMetroSchedule(
                    line = lineCode,
                    pdfID = pdfID,
                    direction = direction
                )
                
                val updatedSchedule = schedule.copy(lastSyncDate = Date())
                val newCache = allSchedules.value.toMutableMap().apply {
                    put(cacheKey, updatedSchedule)
                }
                
                dataStoreManager.saveMetroCache(newCache)
                _isOfflineMode.update { it.toMutableMap().apply { put(cacheKey, false) } }
            } catch (e: Exception) {
                e.printStackTrace()
                _isOfflineMode.update { it.toMutableMap().apply { put(cacheKey, true) } }
            }
        }
    }

    fun getNextDepartures(metro: MetroLine, now: Date = Date()): MetroDisplayMode {
        val calendar = Calendar.getInstance()
        calendar.time = now
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Between 2:00 AM and 4:00 AM the metro is closed
        if (hour in 2..4) {
            return MetroDisplayMode.Closed
        }

        val cacheKey = "${metro.pdfID.orEmpty()}_${metro.direction}"
        val schedule = allSchedules.value[cacheKey] 
            ?: return MetroDisplayMode.Frequency("Sincronizza per i dati...")

        val dayType = DayType.current
        val todayData = when (dayType) {
            DayType.FERIALI -> schedule.feriali
            DayType.SABATO -> schedule.sabato
            DayType.FESTIVO -> schedule.festivo
        }

        val currentFreq = schedule.frequenze[dayType.name].orEmpty()
        val minute = calendar.get(Calendar.MINUTE)
        val found = todayData[hour]?.filter { it.min > minute } ?: emptyList()

        if (found.isEmpty()) {
            val customFreq = metro.customFrequencies?.get(dayType)
            if (customFreq != null) {
                return MetroDisplayMode.Frequency(customFreq)
            }
            if (currentFreq.isNotEmpty()) {
                return MetroDisplayMode.Frequency(currentFreq)
            }
            return MetroDisplayMode.Frequency("Servizio frequente")
        }

        val departures = found.take(3).map { dep ->
            val timeStr = String.format(java.util.Locale.US, "%02d:%02d", hour, dep.min)
            val destName = metro.destinations?.get(dep.color)
            FormattedDeparture(timeStr, destName)
        }

        return MetroDisplayMode.Exact(departures)
    }
}
