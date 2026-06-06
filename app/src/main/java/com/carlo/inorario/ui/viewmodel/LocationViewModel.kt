package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.location.LocationTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LocationViewModel(private val locationTracker: LocationTracker) : ViewModel() {

    val nearbyStation: StateFlow<Station?> = locationTracker.nearbyStation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    fun requestLocation() {
        locationTracker.requestLocation()
    }
}
