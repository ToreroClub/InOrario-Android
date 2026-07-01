package com.carlo.inorario.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.carlo.inorario.data.model.RFIStation
import com.carlo.inorario.data.model.Station
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStreamReader

class LocationTracker(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation

    private val _nearbyStation = MutableStateFlow<Station?>(null)
    val nearbyStation: StateFlow<Station?> = _nearbyStation

    private val referenceStations: List<Station> = loadStations(context)

    companion object {
        private fun loadStations(context: Context): List<Station> {
            return try {
                val inputStream = context.assets.open("rfi_stations.json")
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<RFIStation>>() {}.type
                val rawList: List<RFIStation> = Gson().fromJson(reader, type) ?: emptyList()
                reader.close()

                rawList.mapNotNull { rfi ->
                    val lat = rfi.lat
                    val lon = rfi.lon
                    if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                        Station(
                            name = rfi.name,
                            rfiID = rfi.rfiID,
                            vtID = rfi.vtID,
                            lat = lat,
                            lon = lon
                        )
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("LocationTracker", "Error loading reference stations", e)
                emptyList()
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("LocationTracker", "onLocationResult received: ${locationResult.lastLocation}")
            locationResult.lastLocation?.let { location ->
                _userLocation.value = location
                updateNearbyStation(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        Log.d("LocationTracker", "requestLocation called")
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                Log.d("LocationTracker", "lastLocation success: $location")
                if (location != null && (System.currentTimeMillis() - location.time) < 60000) {
                    _userLocation.value = location
                    updateNearbyStation(location)
                } else {
                    Log.d("LocationTracker", "lastLocation was null or too old (>60s), requesting location updates...")
                    // If last location is null, request dynamic updates once
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        10000
                    ).setMaxUpdates(1).build()

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    ).addOnFailureListener { e ->
                        Log.e("LocationTracker", "requestLocationUpdates failed", e)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("LocationTracker", "lastLocation failed", e)
            }
        } catch (e: Exception) {
            Log.e("LocationTracker", "Exception in requestLocation", e)
        }
    }

    private fun updateNearbyStation(userLoc: Location) {
        Log.d("LocationTracker", "updateNearbyStation called with user coords: Lat=${userLoc.latitude}, Lon=${userLoc.longitude}")
        val candidate = referenceStations.asSequence().mapNotNull { station ->
            val lat = station.lat ?: return@mapNotNull null
            val lon = station.lon ?: return@mapNotNull null
            val stationLoc = Location("").apply {
                latitude = lat
                longitude = lon
            }
            val distance = userLoc.distanceTo(stationLoc)
            Pair(station, distance)
        }.minByOrNull { it.second }

        if (candidate != null) {
            Log.d("LocationTracker", "Closest station: ${candidate.first.name}, distance: ${candidate.second} meters")
        } else {
            Log.d("LocationTracker", "No candidate stations found")
        }

        // Max range: 5 km (5000 meters)
        if ((candidate != null) && (candidate.second < 5000)) {
            Log.d("LocationTracker", "Setting nearbyStation to: ${candidate.first.name}")
            _nearbyStation.value = candidate.first
        } else {
            Log.d("LocationTracker", "Candidate station is too far (> 5km) or null. Setting nearbyStation to null")
            _nearbyStation.value = null
        }
    }
}
