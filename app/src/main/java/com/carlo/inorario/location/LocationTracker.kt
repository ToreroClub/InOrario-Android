package com.carlo.inorario.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.carlo.inorario.data.model.Station
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationTracker(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation

    private val _nearbyStation = MutableStateFlow<Station?>(null)
    val nearbyStation: StateFlow<Station?> = _nearbyStation

    private val referenceStations = listOf(
        Station("Rho Fiera", "3098", "S01039", 45.5215, 9.0883),
        Station("Certosa", "1708", "S01640", 45.5085, 9.1272),
        Station("Villapizzone", "3099", "S01639", 45.4998, 9.1465),
        Station("Lancetti", "1713", "S01643", 45.4925, 9.1751),
        Station("P. Garibaldi Passante", "1714", "S01647", 45.4844, 9.1887),
        Station("Repubblica", "1719", "S01648", 45.4795, 9.1963),
        Station("Porta Venezia", "1723", "S01649", 45.4746, 9.2052),
        Station("Dateo", "3468", "S01650", 45.4682, 9.2158),
        Station("Porta Vittoria", "1718", "S01633", 45.4613, 9.2227),
        Station("Forlanini", "3169", "S01492", 45.4625, 9.2368),
        Station("Magenta", "1618", "S01040", 45.4641, 8.8845),
        Station("Porta Garibaldi", "1715", "S01645", 45.4844, 9.1887),
        Station("Milano Centrale", "1728", "S01700", 45.4849, 9.2033),
        Station("Vittuone-Arluno", "3119", "S01042", 45.4921, 8.9568),
        Station("Pregnana Milanese", "381", "S01058", 45.5036, 9.0069),
        Station("Novara", "1917", "S00248", 45.4524, 8.6253),
        Station("Trecate", "2909", "S00252", 45.4374, 8.7428),
        Station("Rho", "2345", "S01037", 45.5262, 9.0402),
    )

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
                if (location != null) {
                    _userLocation.value = location
                    updateNearbyStation(location)
                } else {
                    Log.d("LocationTracker", "lastLocation was null, requesting location updates...")
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

        // Max range: 15 km (15000 meters)
        if ((candidate != null) && (candidate.second < 15000)) {
            Log.d("LocationTracker", "Setting nearbyStation to: ${candidate.first.name}")
            _nearbyStation.value = candidate.first
        } else {
            Log.d("LocationTracker", "Candidate station is too far (> 15km) or null. Setting nearbyStation to null")
            _nearbyStation.value = null
        }
    }
}
