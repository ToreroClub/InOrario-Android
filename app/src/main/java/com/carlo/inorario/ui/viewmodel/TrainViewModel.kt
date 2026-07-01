package com.carlo.inorario.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.model.AppSection
import com.carlo.inorario.data.model.FavoriteRoute
import com.carlo.inorario.data.model.RFIStation
import com.carlo.inorario.data.model.SavedTrain
import com.carlo.inorario.data.model.SavedTrip
import com.carlo.inorario.data.model.SavedTripSegment
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.Stop
import com.carlo.inorario.data.model.StopsResult
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.data.model.SuburbanRoute
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.data.model.TrainStatus
import com.carlo.inorario.data.model.TravelSolution
import com.carlo.inorario.data.model.TravelSegment
import com.carlo.inorario.data.model.TrenitaliaLocation
import com.carlo.inorario.data.model.VTSearchStation
import com.carlo.inorario.data.network.NetworkClient
import com.carlo.inorario.data.network.RfiScraper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TrainViewModel(
    private val context: Context,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val gson = Gson()

    // --- Core states ---
    private val _trains = MutableStateFlow<List<Train>>(emptyList())
    val trains = _trains.asStateFlow()

    private val _selectedTrainStops = MutableStateFlow<List<Stop>>(emptyList())
    val selectedTrainStops = _selectedTrainStops.asStateFlow()

    private val _favoriteTrainsStops = MutableStateFlow<Map<String, List<Stop>>>(emptyMap())
    val favoriteTrainsStops = _favoriteTrainsStops.asStateFlow()

    private val _currentTrainStatus = MutableStateFlow(TrainStatus())
    val currentTrainStatus = _currentTrainStatus.asStateFlow()

    private val _currentTrainReports = MutableStateFlow<Map<String, Int>>(emptyMap())
    val currentTrainReports = _currentTrainReports.asStateFlow()

    private val _currentTrainBlockedLocations = MutableStateFlow<List<String>>(emptyList())
    val currentTrainBlockedLocations = _currentTrainBlockedLocations.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SavedTrain>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchStationResults = MutableStateFlow<List<VTSearchStation>>(emptyList())
    val searchStationResults = _searchStationResults.asStateFlow()

    private val _searchTrenitaliaLocations = MutableStateFlow<List<TrenitaliaLocation>>(emptyList())
    val searchTrenitaliaLocations = _searchTrenitaliaLocations.asStateFlow()

    private val _searchRFIStationResults = MutableStateFlow<List<RFIStation>>(emptyList())
    val searchRFIStationResults = _searchRFIStationResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isStopsLoading = MutableStateFlow(false)
    val isStopsLoading = _isStopsLoading.asStateFlow()

    private val _stopErrorMessage = MutableStateFlow<String?>(null)
    val stopErrorMessage = _stopErrorMessage.asStateFlow()

    private val _notificationLimitError = MutableStateFlow<String?>(null)
    val notificationLimitError = _notificationLimitError.asStateFlow()

    fun clearNotificationLimitError() {
        _notificationLimitError.value = null
    }

    private val _stationAlerts = MutableStateFlow<String?>(null)
    val stationAlerts = _stationAlerts.asStateFlow()

    private val _travelSolutions = MutableStateFlow<List<TravelSolution>>(emptyList())
    val travelSolutions = _travelSolutions.asStateFlow()

    private val _isSearchingSolutions = MutableStateFlow(false)
    val isSearchingSolutions = _isSearchingSolutions.asStateFlow()

    private val _isHomeFilterActive = MutableStateFlow(false)
    val isHomeFilterActive = _isHomeFilterActive.asStateFlow()

    // --- RFI stations mappings ---
    var allRFIStations = emptyList<RFIStation>()
        private set
    private var rfiStationDictionary = emptyMap<String, String>()
    private var rfiStationNormalizedDict = emptyMap<String, String>()

    // --- State flows inherited from DataStore ---
    val favoriteTrains = dataStoreManager.favoriteTrainsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val myStations = dataStoreManager.myStationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sectionOrder = dataStoreManager.sectionOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSection.entries.toList())

    val favoriteRoutes = dataStoreManager.favoriteRoutesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedTrips = dataStoreManager.savedTripsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedSuburbanLines = dataStoreManager.selectedSuburbanLinesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf("S5", "S6"))

    val hiddenSuburbanStations = dataStoreManager.hiddenSuburbanStationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val homeDestinationStationName = dataStoreManager.homeDestinationStationNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val recentTrains = dataStoreManager.recentTrainsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentStations = dataStoreManager.recentStationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTravelLocations = dataStoreManager.recentTravelLocationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val viewedRecentTrains = dataStoreManager.viewedRecentTrainsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val viewedRecentStations = dataStoreManager.viewedRecentStationsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val smartRoutes = dataStoreManager.smartRoutesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(SuburbanRoute("Magenta", "Milano Bovisa")))

    val collapsedSections = dataStoreManager.collapsedSectionsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hasSupport = dataStoreManager.hasSupportFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val rememberSectionState = dataStoreManager.rememberSectionStateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // --- Temporary state for passing search date from UI ---
    var tempSearchDate: Date? = null

    // --- Auto Refresh Job ---
    private var refreshJob: Job? = null

    init {
        loadRFIStations()
        checkAndCleanOneShotNotifications()
        
        // Restore sections state based on remember toggles
        viewModelScope.launch {
            val rememberStates = dataStoreManager.rememberSectionStateFlow.first()
            val currentCollapsed = dataStoreManager.collapsedSectionsFlow.first().toMutableSet()
            var changed = false
            
            for (section in AppSection.entries) {
                // If not in map, fallback to defaults: PASSANTE false, others true
                val defaultRemember = section != AppSection.PASSANTE
                val remember = rememberStates[section.name] ?: defaultRemember
                
                if (!remember) {
                    if (section == AppSection.PASSANTE) {
                        if (!currentCollapsed.contains(section.name)) {
                            currentCollapsed.add(section.name)
                            changed = true
                        }
                    } else {
                        if (currentCollapsed.contains(section.name)) {
                            currentCollapsed.remove(section.name)
                            changed = true
                        }
                    }
                }
            }
            if (changed) {
                dataStoreManager.saveCollapsedSections(currentCollapsed)
            }
        }
    }

    private fun loadRFIStations() {
        try {
            val inputStream = context.assets.open("rfi_stations.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<RFIStation>>() {}.type
            allRFIStations = gson.fromJson(reader, type) ?: emptyList()
            reader.close()

            val dict = mutableMapOf<String, String>()
            val normDict = mutableMapOf<String, String>()
            for (station in allRFIStations) {
                val rfi = station.rfiID
                if (rfi.isNullOrEmpty()) continue
                val nameLower = station.name.lowercase().trim()
                dict[nameLower] = rfi

                val normName = normalizeStationName(station.name)
                normDict[normName] = rfi
            }
            rfiStationDictionary = dict
            rfiStationNormalizedDict = normDict
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun normalizeStationName(name: String): String {
        val normalized = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace("p.", "porta")
            .replace("s.", "san")
            .replace(" ", "")
            .replace("-", "")
            .replace("'", "")
    }

    fun getRfiID(vtName: String): String? {
        val lower = vtName.lowercase().trim()
        val exact = rfiStationDictionary[lower]
        if (exact != null) return exact
        val norm = normalizeStationName(vtName)
        return rfiStationNormalizedDict[norm]
    }

    // --- Favorites Actions ---

    fun toggleFavorite(trainNumber: String, description: String) {
        viewModelScope.launch {
            val list = favoriteTrains.value.toMutableList()
            val index = list.indexOfFirst { it.number == trainNumber }
            val added = if (index != -1) {
                list.removeAt(index)
                false
            } else {
                val cleanDesc = description.replace("$trainNumber - ", "")
                // Fetch origin and times in background before saving
                val stopsResult = withContext(Dispatchers.IO) { fetchLiveStops(trainNumber) }
                val origin = stopsResult.stops.firstOrNull()?.stationName ?: ""
                val departureTime = stopsResult.stops.firstOrNull()?.time ?: ""
                val arrivalTime = stopsResult.stops.lastOrNull()?.time ?: ""
                list.add(SavedTrain(trainNumber, cleanDesc, origin = origin, departureTime = departureTime, arrivalTime = arrivalTime, notifyDelay = false))
                true
            }
            dataStoreManager.saveFavoriteTrains(list)
            
            // Sync with remote server if notifications are enabled
            val isEnabled = dataStoreManager.remoteNotificationsEnabledFlow.first()
            if (isEnabled) {
                val token = getOrFetchFcmToken()
                if (token.isNotEmpty()) {
                    if (added) {
                        // Do not register automatically for push notifications; notifications are manual per-train
                    } else {
                        // Always unregister from push notifications if removed from favorites
                        unregisterTrainForPush(trainNumber, token)
                    }
                } else {
                    android.util.Log.w("TrainViewModel", "Impossibile sincronizzare la rimozione push: token vuoto.")
                }
            }
        }
    }

    private suspend fun getOrFetchFcmToken(): String {
        val stored = dataStoreManager.fcmTokenFlow.first()
        if (stored.isNotEmpty()) return stored
        return try {
            val token = com.google.android.gms.tasks.Tasks.await(
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            )
            if (token.isNotEmpty()) {
                dataStoreManager.saveFcmToken(token)
                android.util.Log.d("TrainViewModel", "Token FCM recuperato on-demand: $token")
            }
            token
        } catch (e: Exception) {
            android.util.Log.e("TrainViewModel", "Errore recupero token FCM on-demand", e)
            ""
        }
    }

    fun registerDeviceForStrikes(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isPremium = dataStoreManager.hasSupportFlow.first()
                val region = if (isPremium) dataStoreManager.strikeRegionFlow.first() else "Tutte"
                val strikeEnabled = dataStoreManager.strikeNotificationsEnabledFlow.first()
                val payload = JSONObject().apply {
                    put("token", token)
                    put("platform", "android")
                    put("strike_region", region)
                    put("strike_enabled", strikeEnabled)
                }
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = okhttp3.Request.Builder()
                    .url("https://gestioneinorario.toreroclub.com/notifications/register")
                    .post(body)
                    .build()
                okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        android.util.Log.d("TrainViewModel", "Dispositivo registrato per scioperi con regione: $region, enabled: $strikeEnabled")
                    } else {
                        android.util.Log.e("TrainViewModel", "Errore registrazione dispositivo scioperi: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TrainViewModel", "Errore durante la registrazione del dispositivo per gli scioperi", e)
            }
        }
    }

    fun registerTrainForPush(trainNumber: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trainPref = favoriteTrains.value.firstOrNull { it.number == trainNumber }
                val notifyDelay = trainPref?.notifyDelay ?: true
                val notifyDeparture = trainPref?.notifyDeparture ?: false
                val notifyStationPass = trainPref?.notifyStationPass ?: false
                val stationPassName = trainPref?.stationPassName.orEmpty()

                val isPremium = dataStoreManager.hasSupportFlow.first()
                val region = if (isPremium) dataStoreManager.strikeRegionFlow.first() else "Tutte"
                val strikeEnabled = dataStoreManager.strikeNotificationsEnabledFlow.first()
                val limit = if (isPremium) 10 else 1
                val payload = JSONObject().apply {
                    put("token", token)
                    put("platform", "android")
                    put("train_number", trainNumber)
                    put("notify_delay", notifyDelay)
                    put("notify_departure", notifyDeparture)
                    put("notify_station_pass", notifyStationPass)
                    put("station_pass_name", stationPassName ?: JSONObject.NULL)
                    
                    val activeDaysArr = JSONArray()
                    trainPref?.activeDays?.forEach { activeDaysArr.put(it) }
                    put("active_days", activeDaysArr)
                    put("notify_platform_change", trainPref?.notifyPlatformChange ?: false)
                    put("platform_change_station_name", trainPref?.platformChangeStationName ?: JSONObject.NULL)

                    put("limit", limit)
                    put("strike_region", region)
                    put("strike_enabled", strikeEnabled)
                    put("departure_time", trainPref?.departureTime.orEmpty())
                    put("arrival_time", trainPref?.arrivalTime.orEmpty())
                }
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = okhttp3.Request.Builder()
                    .url("https://gestioneinorario.toreroclub.com/notifications/register")
                    .post(body)
                    .build()
                okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        android.util.Log.d("TrainViewModel", "Registrato treno $trainNumber per push")
                    } else if (response.code == 403) {
                        android.util.Log.e("TrainViewModel", "Errore registrazione push: limite raggiunto (403)")
                        _notificationLimitError.value = "Limite massimo di treni monitorati raggiunto. Riprova più tardi."
                    } else {
                        android.util.Log.e("TrainViewModel", "Errore registrazione push: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TrainViewModel", "Errore durante la registrazione del treno per push", e)
            }
        }
    }

    fun unregisterTrainForPush(trainNumber: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("token", token)
                    put("train_number", trainNumber)
                }
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = okhttp3.Request.Builder()
                    .url("https://gestioneinorario.toreroclub.com/notifications/unregister")
                    .post(body)
                    .build()
                okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        android.util.Log.d("TrainViewModel", "Rimosso treno $trainNumber da push")
                    } else {
                        android.util.Log.e("TrainViewModel", "Errore rimozione push: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TrainViewModel", "Errore durante la rimozione del treno da push", e)
            }
        }
    }

    fun syncRemoteNotifications(enabled: Boolean, token: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeToken = if (token != null && token.isNotEmpty()) token else getOrFetchFcmToken()
            if (activeToken.isEmpty()) {
                android.util.Log.w("TrainViewModel", "Impossibile sincronizzare le notifiche: token FCM vuoto.")
                return@launch
            }
            val trains = favoriteTrains.value
            if (enabled) {
                registerDeviceForStrikes(activeToken)
                for (train in trains) {
                    if (train.notifyDelay) {
                        registerTrainForPush(train.number, activeToken)
                    } else {
                        unregisterTrainForPush(train.number, activeToken)
                    }
                }
            } else {
                for (train in trains) {
                    unregisterTrainForPush(train.number, activeToken)
                }
            }
        }
    }

    fun updateFavoriteTrainNotifications(
        trainNumber: String,
        notifyDelay: Boolean,
        notifyDeparture: Boolean,
        notifyStationPass: Boolean,
        stationPassName: String?,
        activeDays: List<Int>? = null,
        notifyPlatformChange: Boolean = false,
        platformChangeStationName: String? = null
    ) {
        viewModelScope.launch {
            var list = favoriteTrains.value.toMutableList()

            if (notifyDelay) {
                val isPremium = dataStoreManager.hasSupportFlow.first()
                if (!isPremium) {
                    val token = getOrFetchFcmToken()
                    list = list.map {
                        if (it.number != trainNumber && it.notifyDelay) {
                            if (token.isNotEmpty()) {
                                unregisterTrainForPush(it.number, token)
                            }
                            it.copy(
                                notifyDelay = false,
                                notifyDeparture = false,
                                notifyStationPass = false,
                                stationPassName = null
                            )
                        } else {
                            it
                        }
                    }.toMutableList()
                }
            }

            val index = list.indexOfFirst { it.number == trainNumber }
            if (index != -1) {
                val updated = list[index].copy(
                    notifyDelay = notifyDelay,
                    notifyDeparture = if (notifyDelay) notifyDeparture else false,
                    notifyStationPass = if (notifyDelay) notifyStationPass else false,
                    stationPassName = if (notifyDelay && notifyStationPass) stationPassName else null,
                    activeDays = activeDays,
                    notifyPlatformChange = notifyPlatformChange,
                    platformChangeStationName = platformChangeStationName
                )
                list[index] = updated
                dataStoreManager.saveFavoriteTrains(list)

                // Sync with remote server if notifications are enabled
                val isEnabled = dataStoreManager.remoteNotificationsEnabledFlow.first()
                if (isEnabled) {
                    val token = getOrFetchFcmToken()
                    if (token.isNotEmpty()) {
                        if (notifyDelay) {
                            registerTrainForPush(trainNumber, token)
                        } else {
                            unregisterTrainForPush(trainNumber, token)
                        }
                    } else {
                        android.util.Log.w("TrainViewModel", "Impossibile sincronizzare la notifica del treno: token vuoto.")
                    }
                }
            }
        }
    }

    fun fetchStopsForFavorite(trainNumber: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { fetchLiveStops(trainNumber) }
            if (result.stops.isNotEmpty()) {
                _favoriteTrainsStops.update { map ->
                    map.toMutableMap().apply { put(trainNumber, result.stops) }
                }
            }
        }
    }

    /**
     * Fetches live stops for a favorited train that is missing origin/departure/arrival data
     * and updates it in the persisted list. Safe to call even if data already exists.
     */
    fun enrichFavoriteTrainData(trainNumber: String) {
        viewModelScope.launch {
            val current = favoriteTrains.value.firstOrNull { it.number == trainNumber } ?: return@launch
            // Only enrich if data is actually missing
            if (current.origin.isNotEmpty() && current.departureTime.isNotEmpty() && current.arrivalTime.isNotEmpty()) return@launch

            val result = withContext(Dispatchers.IO) { fetchLiveStops(trainNumber) }
            if (result.stops.isEmpty()) return@launch

            val firstStop = result.stops.first()
            val lastStop = result.stops.last()

            val list = favoriteTrains.value.toMutableList()
            val index = list.indexOfFirst { it.number == trainNumber }
            if (index != -1) {
                val enriched = list[index].copy(
                    origin = if (list[index].origin.isEmpty()) firstStop.stationName else list[index].origin,
                    departureTime = if (list[index].departureTime.isEmpty()) firstStop.time else list[index].departureTime,
                    arrivalTime = if (list[index].arrivalTime.isEmpty()) lastStop.time else list[index].arrivalTime,
                    description = if (list[index].description.isEmpty() || !list[index].description.contains(" - "))
                        "${firstStop.stationName} - ${lastStop.stationName}"
                    else list[index].description
                )
                list[index] = enriched
                dataStoreManager.saveFavoriteTrains(list)

                // Also cache the stops for the notification station picker
                _favoriteTrainsStops.update { map ->
                    map.toMutableMap().apply { put(trainNumber, result.stops) }
                }
            }
        }
    }


    fun isFavorite(trainNumber: String): Boolean {
        return favoriteTrains.value.any { it.number == trainNumber }
    }

    fun toggleFavoriteRoute(originName: String, originID: String, destName: String, destID: String) {
        viewModelScope.launch {
            val list = favoriteRoutes.value.toMutableList()
            val index = list.indexOfFirst { (it.originID == originID) && (it.destinationID == destID) }
            if (index != -1) {
                list.removeAt(index)
            } else {
                list.add(FavoriteRoute(originName, originID, destName, destID))
            }
            dataStoreManager.saveFavoriteRoutes(list)
        }
    }

    fun isFavoriteRoute(originID: String, destID: String): Boolean {
        return favoriteRoutes.value.any { (it.originID == originID) && (it.destinationID == destID) }
    }

    fun toggleSavedTrip(solution: TravelSolution) {
        viewModelScope.launch {
            val tripId = "${solution.origin}-${solution.destination}-${solution.departureTime}"
            val list = savedTrips.value.toMutableList()
            val index = list.indexOfFirst { it.id == tripId }
            if (index != -1) {
                list.removeAt(index)
            } else {
                val segments = solution.segments.map {
                    SavedTripSegment(
                        origin = it.origin,
                        destination = it.destination,
                        departureTime = it.departureTime,
                        arrivalTime = it.arrivalTime,
                        trainNumber = it.trainNumber,
                        trainCategory = it.trainCategory
                    )
                }
                list.add(
                    SavedTrip(
                        id = tripId,
                        origin = solution.origin,
                        destination = solution.destination,
                        departureTime = solution.departureTime,
                        arrivalTime = solution.arrivalTime,
                        duration = solution.duration,
                        segments = segments
                    )
                )
            }
            dataStoreManager.saveSavedTrips(list)
        }
    }

    fun isTripSaved(solution: TravelSolution): Boolean {
        val tripId = "${solution.origin}-${solution.destination}-${solution.departureTime}"
        return savedTrips.value.any { it.id == tripId }
    }

    fun addMyStation(name: String, vtID: String) {
        viewModelScope.launch {
            val list = myStations.value.toMutableList()
            if (!list.any { it.vtID == vtID }) {
                val possibleRfi = getRfiID(name)
                list.add(
                    Station(
                        name = name.lowercase().replaceFirstChar { it.titlecase() },
                        rfiID = possibleRfi,
                        vtID = vtID
                    )
                )
                dataStoreManager.saveMyStations(list)
            }
        }
    }

    fun removeMyStation(vtID: String) {
        viewModelScope.launch {
            val list = myStations.value.toMutableList()
            list.removeAll { it.vtID == vtID }
            dataStoreManager.saveMyStations(list)
        }
    }

    fun addToRecentTrains(train: SavedTrain) {
        viewModelScope.launch {
            val list = recentTrains.value.toMutableList()
            list.removeAll { it.number == train.number }
            list.add(0, train)
            if (list.size > 10) {
                list.removeAt(list.lastIndex)
            }
            dataStoreManager.saveRecentTrains(list)
        }
    }

    fun addToRecentStations(station: Station) {
        viewModelScope.launch {
            val list = recentStations.value.toMutableList()
            list.removeAll { it.vtID == station.vtID || (it.rfiID != null && it.rfiID == station.rfiID) }
            list.add(0, station)
            if (list.size > 10) {
                list.removeAt(list.lastIndex)
            }
            dataStoreManager.saveRecentStations(list)
        }
    }

    fun clearRecentTrains() {
        viewModelScope.launch {
            dataStoreManager.saveRecentTrains(emptyList())
        }
    }

    fun clearRecentStations() {
        viewModelScope.launch {
            dataStoreManager.saveRecentStations(emptyList())
        }
    }

    fun addToRecentTravelLocations(location: TrenitaliaLocation) {
        viewModelScope.launch {
            val list = recentTravelLocations.value.toMutableList()
            list.removeAll { it.id == location.id }
            list.add(0, location)
            if (list.size > 10) {
                list.removeAt(list.lastIndex)
            }
            dataStoreManager.saveRecentTravelLocations(list)
        }
    }

    fun clearRecentTravelLocations() {
        viewModelScope.launch {
            dataStoreManager.saveRecentTravelLocations(emptyList())
        }
    }

    fun addToViewedRecentTrains(train: SavedTrain) {
        viewModelScope.launch {
            val list = viewedRecentTrains.value.toMutableList()
            list.removeAll { it.number == train.number }
            list.add(0, train)
            if (list.size > 10) {
                list.removeAt(list.lastIndex)
            }
            dataStoreManager.saveViewedRecentTrains(list)
        }
    }

    fun addToViewedRecentStations(station: Station) {
        viewModelScope.launch {
            val list = viewedRecentStations.value.toMutableList()
            list.removeAll { it.vtID == station.vtID || (it.rfiID != null && it.rfiID == station.rfiID) }
            list.add(0, station)
            if (list.size > 10) {
                list.removeAt(list.lastIndex)
            }
            dataStoreManager.saveViewedRecentStations(list)
        }
    }

    fun clearViewedRecentTrains() {
        viewModelScope.launch {
            dataStoreManager.saveViewedRecentTrains(emptyList())
        }
    }

    fun clearViewedRecentStations() {
        viewModelScope.launch {
            dataStoreManager.saveViewedRecentStations(emptyList())
        }
    }

    fun isMyStation(vtID: String): Boolean {
        return myStations.value.any { it.vtID == vtID }
    }

    fun toggleSuburbanLine(id: String) {
        viewModelScope.launch {
            val list = selectedSuburbanLines.value.toMutableList()
            if (list.contains(id)) {
                list.remove(id)
            } else {
                list.add(id)
            }
            dataStoreManager.saveSelectedSuburbanLines(list)
        }
    }

    fun toggleHiddenStation(lineId: String, stationName: String) {
        viewModelScope.launch {
            val map = hiddenSuburbanStations.value.toMutableMap()
            val list = map[lineId]?.toMutableList() ?: mutableListOf()
            if (list.contains(stationName)) {
                list.remove(stationName)
            } else {
                list.add(stationName)
            }
            map[lineId] = list
            dataStoreManager.saveHiddenSuburbanStations(map)
        }
    }

    fun saveHomeDestinationStationName(name: String) {
        viewModelScope.launch {
            dataStoreManager.saveHomeDestinationStationName(name)
        }
    }

    fun saveSectionOrder(order: List<AppSection>) {
        viewModelScope.launch {
            dataStoreManager.saveSectionOrder(order)
        }
    }

    fun toggleSectionCollapsed(sectionId: String) {
        viewModelScope.launch {
            val set = collapsedSections.value.toMutableSet()
            if (set.contains(sectionId)) {
                set.remove(sectionId)
            } else {
                set.add(sectionId)
            }
            dataStoreManager.saveCollapsedSections(set)
        }
    }

    fun toggleRememberSectionState(sectionId: String) {
        viewModelScope.launch {
            val states = rememberSectionState.value.toMutableMap()
            val defaultRemember = sectionId != AppSection.PASSANTE.name
            val current = states[sectionId] ?: defaultRemember
            states[sectionId] = !current
            dataStoreManager.saveRememberSectionState(states)
        }
    }

    fun toggleHomeFilter() {
        _isHomeFilterActive.update { !it }
    }

    // --- Search APIs ---

    fun searchTrains(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    NetworkClient.viaggiatrenoService.searchTrainAutocomplete(query)
                }
                if (response.isSuccessful) {
                    val raw = withContext(Dispatchers.IO) { response.body()?.string().orEmpty() }
                    val lines = raw.split("\n").filter { it.isNotEmpty() }
                    _searchResults.value = lines.mapNotNull { line ->
                        val parts = line.split("|")
                        if (parts.isEmpty()) return@mapNotNull null
                        val desc = parts[0].split(" - ")
                        val num = desc[0].trim()
                        val destination = if (desc.size > 1) desc[1].trim() else desc[0]
                        SavedTrain(num, destination)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchStations(query: String) {
        if (query.length < 2) {
            _searchStationResults.value = emptyList()
            return
        }
        _isSearching.value = true
        val queryLower = query.lowercase().trim()
        val results = allRFIStations.filter { it.name.lowercase().contains(queryLower) }
            .map { VTSearchStation(it.name, it.name, it.vtID ?: it.rfiID.orEmpty()) }
            .sortedBy { it.nomeLungo }

        _searchStationResults.value = results
        _isSearching.value = false
    }

    fun searchRFIStationsLocally(query: String) {
        if (query.length < 2) {
            _searchRFIStationResults.value = emptyList()
            return
        }
        val queryLower = query.lowercase().trim()
        _searchRFIStationResults.value = allRFIStations.filter { it.name.lowercase().contains(queryLower) }
    }

    fun searchTravelLocations(query: String) {
        if (query.length < 2) {
            _searchTrenitaliaLocations.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val requestObj = okhttp3.Request.Builder()
                        .url("https://www.lefrecce.it/Channels.Website.BFF.WEB/website/locations/search?name=${java.net.URLEncoder.encode(query, "UTF-8")}")
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)")
                        .build()

                    client.newCall(requestObj).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string().orEmpty()
                            val type = object : TypeToken<List<TrenitaliaLocation>>() {}.type
                            gson.fromJson<List<TrenitaliaLocation>>(body, type) ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
                _searchTrenitaliaLocations.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchTravelSolutions(originID: String, destID: String, date: Date) {
        _isSearchingSolutions.value = true
        _travelSolutions.value = emptyList()

        val depId = originID.toIntOrNull()
        val arrId = destID.toIntOrNull()
        if (depId == null || arrId == null) {
            _isSearchingSolutions.value = false
            return
        }

        viewModelScope.launch {
            try {
                val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000ZZZZZ", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("Europe/Rome")
                }
                val dateStr = f.format(date)

                val payload = JSONObject().apply {
                    put("departureLocationId", depId)
                    put("arrivalLocationId", arrId)
                    put("departureTime", dateStr)
                    put("adults", 1)
                    put("children", 0)
                    put("criteria", JSONObject().apply {
                        put("frecceOnly", false)
                        put("regionalOnly", false)
                        put("noChanges", false)
                        put("order", "DEPARTURE_DATE")
                        put("offset", 0)
                        put("limit", 15)
                    })
                    put("advancedSearchRequest", JSONObject().apply {
                        put("bestFare", false)
                    })
                }

                val list = withContext(Dispatchers.IO) {
                    val client = okhttp3.OkHttpClient()
                    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                    val requestObj = okhttp3.Request.Builder()
                        .url("https://www.lefrecce.it/Channels.Website.BFF.WEB/website/ticket/solutions")
                        .post(body)
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)")
                        .build()

                    client.newCall(requestObj).execute().use { response ->
                        if (response.isSuccessful) {
                            val respBody = response.body?.string().orEmpty()
                            val root = JSONObject(respBody)
                            val solutionsArr = root.optJSONArray("solutions")
                            val resultList = mutableListOf<TravelSolution>()

                            if (solutionsArr != null) {
                                for (i in 0 until solutionsArr.length()) {
                                    val item = solutionsArr.getJSONObject(i)
                                    val sol = item.optJSONObject("solution") ?: continue

                                    val origin = sol.optString("origin")
                                    val destination = sol.optString("destination")
                                    val duration = sol.optString("duration")

                                    var depTimeStr = "--:--"
                                    var arrTimeStr = "--:--"
                                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getTimeZone("Europe/Rome")
                                    }

                                    val rawDep = sol.optString("departureTime")
                                    val rawArr = sol.optString("arrivalTime")
                                    try {
                                        f.parse(rawDep)?.let { depTimeStr = timeFormatter.format(it) }
                                        f.parse(rawArr)?.let { arrTimeStr = timeFormatter.format(it) }
                                    } catch (e: Exception) {
                                    }

                                    var category = "Treno"
                                    var num = ""

                                    val trainsArr = sol.optJSONArray("trains")
                                    if (trainsArr != null && trainsArr.length() > 0) {
                                        val firstTrain = trainsArr.getJSONObject(0)
                                        category = firstTrain.optString("trainCategory", firstTrain.optString("acronym", "Treno"))
                                        num = firstTrain.optString("name", firstTrain.optString("description", ""))

                                        if (trainsArr.length() > 1) {
                                            num += " (+${trainsArr.length() - 1} cambi)"
                                        }
                                    }

                                    val segments = mutableListOf<TravelSegment>()
                                    val nodesArr = sol.optJSONArray("nodes")
                                    if (nodesArr != null) {
                                        for (j in 0 until nodesArr.length()) {
                                            val node = nodesArr.getJSONObject(j)
                                            val nOrigin = node.optString("origin")
                                            val nDest = node.optString("destination")
                                            var nDep = "--:--"
                                            var nArr = "--:--"
                                            try {
                                                f.parse(node.optString("departureTime"))?.let { nDep = timeFormatter.format(it) }
                                                f.parse(node.optString("arrivalTime"))?.let { nArr = timeFormatter.format(it) }
                                            } catch (e: Exception) {
                                            }

                                            var nCat = "Treno"
                                            var nNum = ""
                                            val nTrain = node.optJSONObject("train")
                                            if (nTrain != null) {
                                                nCat = nTrain.optString("trainCategory", nTrain.optString("acronym", "Treno"))
                                                nNum = nTrain.optString("name", nTrain.optString("description", ""))
                                            }

                                            if (nOrigin.lowercase().startsWith("milano") &&
                                                nDest.lowercase().startsWith("milano") &&
                                                nOrigin != nDest
                                            ) {
                                                nCat = "Trasporto Urbano"
                                                nNum = "(Metro / Mezzi)"
                                            }

                                            segments.add(TravelSegment(nOrigin, nDest, nDep, nArr, nNum, nCat))
                                        }
                                    }

                                    resultList.add(
                                        TravelSolution(
                                            trainNumber = num,
                                            category = category,
                                            departureTime = depTimeStr,
                                            arrivalTime = arrTimeStr,
                                            origin = origin.lowercase().replaceFirstChar { it.titlecase() },
                                            destination = destination.lowercase().replaceFirstChar { it.titlecase() },
                                            duration = duration,
                                            segments = segments
                                        )
                                    )
                                }
                            }
                            resultList
                        } else {
                            emptyList<TravelSolution>()
                        }
                    }
                }
                _travelSolutions.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSearchingSolutions.value = false
            }
        }
    }

    // --- Station & Scraping Fetch Actions ---

    fun fetchTrains(station: Station, isDepartures: Boolean = true) {
        _isLoading.value = true
        _stationAlerts.value = null

        viewModelScope.launch {
            try {
                // rfiID = numeric ID for iechub.rfi.it scraper (e.g. "1714")
                // vtID  = S0XXXX code for ViaggiaTreno API (e.g. "S01647")
                val rfiScraperId = station.rfiID   // numeric, used by iechub
                val vtApiId = station.vtID         // S0XXXX, used by ViaggiaTreno JSON API

                var scraperSuccess = false
                if (!rfiScraperId.isNullOrEmpty()) {
                    val result = RfiScraper.performRfiScraping(rfiScraperId, isDepartures = isDepartures)
                    if (result.second != null) {
                        _stationAlerts.value = result.second
                    }
                    if (result.first.isNotEmpty()) {
                        _trains.value = result.first
                        scraperSuccess = true
                    }
                }

                if (!scraperSuccess && !vtApiId.isNullOrEmpty()) {
                    val f = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'ZZZ", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("Europe/Rome")
                    }
                    val dateStr = f.format(Date()).replace(" ", "%20")
                    val endpoint = if (isDepartures) "partenze" else "arrivi"
                    val response = withContext(Dispatchers.IO) {
                        NetworkClient.viaggiatrenoService.getStationBoard(
                            endpoint = endpoint,
                            vtID = vtApiId,
                            dateStr = dateStr
                        )
                    }
                    if (response.isSuccessful) {
                        val body = withContext(Dispatchers.IO) { response.body()?.string().orEmpty() }
                        val array = JSONArray(body)
                        val list = mutableListOf<Train>()
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val num = item.optInt("numeroTreno").toString()
                            var cat = item.optString("categoriaDescrizione").trim()
                            val dest = if (isDepartures) {
                                item.optString("destinazione")
                            } else {
                                item.optString("origine")
                            }.lowercase().replaceFirstChar { it.titlecase() }
                            
                            val timeVal = if (isDepartures) {
                                item.optLong("orarioPartenza")
                            } else {
                                item.optLong("orarioArrivo")
                            }
                            
                            val ritardo = item.optInt("ritardo")

                            val binEffKey = if (isDepartures) "binarioEffettivoPartenzaDescrizione" else "binarioEffettivoArrivoDescrizione"
                            val binProgKey = if (isDepartures) "binarioProgrammatoPartenzaDescrizione" else "binarioProgrammatoArrivoDescrizione"
                            val binEff = item.optString(binEffKey, "").takeIf { it.trim().lowercase() != "null" && it.isNotBlank() }
                            val binProg = item.optString(binProgKey, "").takeIf { it.trim().lowercase() != "null" && it.isNotBlank() }
                            val platform = (binEff ?: binProg)?.trim()?.takeIf { it.isNotEmpty() } ?: "--"

                            val catUpper = cat.uppercase()
                            when {
                                catUpper.contains("ALTA VELOCIT") -> cat = "AV"
                                catUpper.contains("INTERCITY") -> cat = "IC"
                                catUpper.contains("EUROCITY") -> cat = "EC"
                                catUpper == "REGIONALE VELOCE" -> cat = "RV"
                                catUpper == "REGIONALE" -> cat = "REG"
                                catUpper == "SUBURBANO" -> cat = "S"
                            }

                            if (cat == "S" || cat == "REG") {
                                cat = when {
                                    num.startsWith("240") || num.startsWith("230") || num.startsWith("241") || num.startsWith("231") -> "S1"
                                    num.startsWith("242") || num.startsWith("232") -> {
                                        if (dest.lowercase().contains("melegnano") || dest.lowercase().contains("cormano")) "S12" else "S2"
                                    }
                                    num.startsWith("243") || num.startsWith("233") || num.startsWith("328") || num.startsWith("329") -> "S13"
                                    num.startsWith("245") || num.startsWith("235") -> "S5"
                                    num.startsWith("246") || num.startsWith("236") -> "S6"
                                    num.startsWith("256") || num.startsWith("257") || num.startsWith("247") || num.startsWith("237") -> "S12"
                                    num.startsWith("248") || num.startsWith("238") -> "S8"
                                    num.startsWith("249") || num.startsWith("239") -> "S9"
                                    num.startsWith("250") || num.startsWith("251") || num.startsWith("252") -> "S11"
                                    else -> {
                                        val d = dest.lowercase()
                                        when {
                                            d.contains("saronno") || d.contains("lodi") -> "S1"
                                            d.contains("mariano") || d.contains("seveso") || d.contains("camnago") -> "S2"
                                            d.contains("varese") || d.contains("treviglio") || d.contains("gallarate") -> "S5"
                                            d.contains("novara") || d.contains("nov ") || d.contains("pioltello") || d.contains("piolt") || d.contains("magenta") -> "S6"
                                            d.contains("melegnano") || d.contains("cormano") -> "S12"
                                            d.contains("pavia") || d.contains("garbagnate") -> "S13"
                                            else -> cat
                                        }
                                    }
                                }
                            }

                            if (timeVal > 0) {
                                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                                    timeZone = TimeZone.getTimeZone("Europe/Rome")
                                }.format(Date(timeVal))
                                list.add(
                                    Train(
                                        category = cat,
                                        number = num,
                                        destination = dest,
                                        time = formattedTime,
                                        delay = if (ritardo > 0) "+$ritardo'" else "In orario",
                                        platform = platform
                                    )
                                )
                            }
                        }
                        _trains.value = list
                    }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Train Stops details ---

    suspend fun fetchLiveStops(trainNumber: String): StopsResult = withContext(Dispatchers.IO) {
        val clean = trainNumber.trim()
        try {
            val searchResponse = NetworkClient.viaggiatrenoService.searchTrainAutocomplete(clean)
            if (searchResponse.isSuccessful) {
                val raw = searchResponse.body()?.string().orEmpty()
                val lines = raw.split("\n").filter { it.trim().isNotEmpty() }
                val targets = lines.filter { it.contains("|$clean-") }.ifEmpty { if (lines.isNotEmpty()) listOf(lines[0]) else emptyList() }
                if (targets.isEmpty()) return@withContext StopsResult(emptyList(), TrainStatus(), "Dettagli del treno non trovati.")

                val resultsList = coroutineScope {
                    targets.map { targetLine ->
                        async {
                            val pipes = targetLine.split("|")
                            if (pipes.size < 2) return@async null
                            val subParts = pipes[1].split("-")
                            if (subParts.size < 2) return@async null
                            val originID = subParts[1]
                            val timestamp = if (subParts.size >= 3) subParts[2] else ""

                            val progressResponse = if (timestamp.isNotEmpty()) {
                                NetworkClient.viaggiatrenoService.getTrainProgressWithTimestamp(originID, clean, timestamp)
                            } else {
                                NetworkClient.viaggiatrenoService.getTrainProgress(originID, clean)
                            }

                            if (progressResponse.isSuccessful) {
                                val rawBody = progressResponse.body()?.string().orEmpty()
                                if (rawBody.isNotEmpty()) Pair(targetLine, org.json.JSONObject(rawBody)) else null
                            } else null
                        }
                    }.awaitAll()
                }.filterNotNull()

                if (resultsList.isEmpty()) return@withContext StopsResult(emptyList(), TrainStatus(), "Risposta server vuota.")

                val nowTs = System.currentTimeMillis()
                var bestJson: org.json.JSONObject? = null
                var bestScore = -1.0

                for ((targetLine, jsonObj) in resultsList) {
                    val pipes = targetLine.split("|")
                    val subParts = if (pipes.size >= 2) pipes[1].split("-") else emptyList()
                    val tsStr = if (subParts.size >= 3) subParts[2] else ""
                    val trainTs = tsStr.toLongOrNull() ?: nowTs

                    val deltaDays = Math.abs(nowTs - trainTs) / (1000.0 * 60 * 60 * 24)
                    val isDeparted = !(jsonObj.optBoolean("nonPartito", true))
                    val isArrived = jsonObj.optBoolean("arrivato", false)

                    val baseScore = if (isDeparted && !isArrived) 10000.0 else 1000.0
                    val score = baseScore - (deltaDays * 100.0)

                    if (score > bestScore) {
                        bestScore = score
                        bestJson = jsonObj
                    }
                }

                val json = bestJson ?: resultsList.first().second
                val status = TrainStatus().apply {
                    isDeparted = !(json.optBoolean("nonPartito", true))
                    isArrived = json.optBoolean("arrivato", false)
                    lastStation = json.optString("stazioneUltimoRilevamento", "--")
                    lastTime = json.optString("compOraUltimoRilevamento", json.optString("oraUltimoRilevamento", "--:--"))

                    val ritardi = json.optJSONArray("compRitardo")
                    if (ritardi != null && ritardi.length() > 0) {
                        statusMessage = ritardi.getString(0)
                    } else {
                        statusMessage = if (isDeparted) "In viaggio" else "In attesa di partenza"
                    }

                    val provv = json.optInt("provvedimento", 0)
                    if (provv != 0) {
                        cancellationNote = "TRENO CANCELLATO O DEVIATO"
                        statusMessage = "Soppresso"
                    }
                }

                val globalDelay = json.optInt("ritardo", 0)
                val fermate = json.optJSONArray("fermate")
                val stops = mutableListOf<Stop>()

                if (fermate != null) {
                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Europe/Rome")
                    }

                    for (i in 0 until fermate.length()) {
                        val f = fermate.getJSONObject(i)
                        val name = f.optString("stazione", "Sconosciuta").lowercase().replaceFirstChar { it.titlecase() }
                        val tProg = f.optLong("programmata")
                        val tEff = f.optLong("effettiva")

                        val stopSpecificDelay = f.optInt("ritardo", 0)
                        val effectiveDelay = if (stopSpecificDelay > 0) stopSpecificDelay else globalDelay

                        val pTime = timeFormatter.format(Date(tProg))
                        val actTime = if (tEff > 0) timeFormatter.format(Date(tEff)) else null

                        var estTime: String? = null
                        if (actTime == null && effectiveDelay >= 4) {
                            val cal = java.util.Calendar.getInstance().apply {
                                time = Date(tProg)
                                add(java.util.Calendar.MINUTE, effectiveDelay)
                            }
                            estTime = timeFormatter.format(cal.time)
                        }

                        stops.add(
                            Stop(
                                stationName = name,
                                time = pTime,
                                actualTime = actTime,
                                delay = effectiveDelay,
                                estimatedTime = estTime
                            )
                        )
                    }
                }
                return@withContext StopsResult(stops, status, null)
            }
            return@withContext StopsResult(emptyList(), TrainStatus(), "Dati in aggiornamento o non disponibili.")
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext StopsResult(emptyList(), TrainStatus(), "Errore di rete o dati illeggibili.")
        }
    }

    fun fetchStops(train: Train, isRefresh: Boolean = false) {
        if (!isRefresh) {
            _selectedTrainStops.value = emptyList()
            _currentTrainStatus.value = TrainStatus()
            _isStopsLoading.value = true
            _stopErrorMessage.value = null
        }

        viewModelScope.launch {
            val result = fetchLiveStops(train.number)
            if (!isRefresh || result.errorMessage == null) {
                _selectedTrainStops.value = result.stops
                _currentTrainStatus.value = result.status
                _stopErrorMessage.value = result.errorMessage
            }
            if (!isRefresh) {
                _isStopsLoading.value = false
            }
        }
    }

    // --- Dashboard & Filtering ---

    fun filterTrainsForHome(trainList: List<Train>, currentStationName: String): List<Train> {
        val home = homeDestinationStationName.value.lowercase().trim()
        val current = currentStationName.lowercase().trim()
        if (home.isEmpty() || home == current) return trainList

        return trainList.filter { train ->
            val dest = train.destination.lowercase()

            when {
                home.contains("magenta") -> {
                    val eastOfMagenta = listOf("milano", "garibaldi", "repubblica", "venezia", "dateo", "vittoria", "forlanini", "certosa", "villapizzone", "lancetti", "rho", "pregnana", "vittuone", "arluno")
                    val isEast = eastOfMagenta.any { current.contains(it) }
                    if (isEast) {
                        val cat = train.category.lowercase()
                        val isHighSpeed = cat.contains("fr") || cat.contains("freccia") || cat.contains("italo") || cat.contains("av") || cat.contains("ec") || cat.contains("ic")
                        if (current.contains("garibaldi")) {
                            dest.contains("novara") || dest.contains("magenta") || dest.contains("trecate")
                        } else {
                            val validDest = dest.contains("novara") || dest.contains("torino") || dest.contains("magenta") || dest.contains("trecate") || dest.contains("lingotto")
                            validDest && !isHighSpeed
                        }
                    } else {
                        val westOfMagenta = listOf("novara", "trecate")
                        val isWest = westOfMagenta.any { current.contains(it) }
                        if (isWest) {
                            dest.contains("milano") || dest.contains("pioltello") || dest.contains("treviglio") || dest.contains("passante")
                        } else {
                            dest.contains(home) || home.contains(dest)
                        }
                    }
                }
                home.contains("bovisa") -> {
                    val northWestOfBovisa = listOf("saronno", "mariano", "camnago", "meda", "seveso", "cesano", "bovisio", "varedo", "paderno", "cormano", "cusano", "caronno", "cesate", "garbagnate", "bollate", "novate")
                    val isNorthWest = northWestOfBovisa.any { current.contains(it) }
                    if (isNorthWest) {
                        dest.contains("cadorna") || dest.contains("milano") || dest.contains("pavia") || dest.contains("lodi") || dest.contains("rogoredo")
                    } else {
                        val southEastOfBovisa = listOf("cadorna", "domodossola", "lancetti", "garibaldi", "repubblica", "venezia", "dateo", "vittoria", "rogoredo", "lodi", "pavia")
                        val isSouthEast = southEastOfBovisa.any { current.contains(it) }
                        if (isSouthEast) {
                            dest.contains("saronno") || dest.contains("mariano") || dest.contains("camnago") || dest.contains("bovisa")
                        } else {
                            dest.contains(home) || home.contains(dest)
                        }
                    }
                }
                home.contains("rogoredo") -> {
                    val northWestOfRogoredo = listOf("bovisa", "lancetti", "garibaldi", "repubblica", "venezia", "dateo", "vittoria", "forlanini", "certosa", "villapizzone", "rho", "greco", "lambrate")
                    val isNorthWest = northWestOfRogoredo.any { current.contains(it) }
                    if (isNorthWest) {
                        dest.contains("rogoredo") || dest.contains("lodi") || dest.contains("pavia") || dest.contains("piacenza") || dest.contains("mantova") || dest.contains("genova") || dest.contains("bologna") || dest.contains("parma") || dest.contains("melegnano")
                    } else {
                        val southEastOfRogoredo = listOf("pavia", "lodi", "melegnano", "piacenza")
                        val isSouthEast = southEastOfRogoredo.any { current.contains(it) }
                        if (isSouthEast) {
                            dest.contains("milano") || dest.contains("bovisa") || dest.contains("saronno") || dest.contains("mariano") || dest.contains("cadorna") || dest.contains("torino")
                        } else {
                            dest.contains(home) || home.contains(dest)
                        }
                    }
                }
                home.contains("monza") -> {
                    val southOfMonza = listOf("milano", "greco", "garibaldi", "lambrate", "forlanini", "rogoredo", "albairate", "cristoforo", "romolo", "romana", "tibaldi", "sesto")
                    val isSouth = southOfMonza.any { current.contains(it) }
                    if (isSouth) {
                        dest.contains("chiasso") || dest.contains("como") || dest.contains("seregno") || dest.contains("lecco") || dest.contains("monza") || dest.contains("bergamo") || dest.contains("carnate") || dest.contains("molteno") || dest.contains("colico") || dest.contains("sondrio")
                    } else {
                        val northOfMonza = listOf("como", "chiasso", "lecco", "seregno", "desio", "lissone", "carnate", "arcore")
                        val isNorth = northOfMonza.any { current.contains(it) }
                        if (isNorth) {
                            dest.contains("milano") || dest.contains("greco") || dest.contains("albairate") || dest.contains("saronno") || dest.contains("rho")
                        } else {
                            dest.contains(home) || home.contains(dest)
                        }
                    }
                }
                home.contains("saronno") -> {
                    val southEastOfSaronno = listOf("milano", "cadorna", "bovisa", "domodossola", "greco", "monza", "lodi", "albairate", "romolo", "cristoforo", "lambrate", "garibaldi")
                    val isSouthEast = southEastOfSaronno.any { current.contains(it) }
                    if (isSouthEast) {
                        dest.contains("saronno") || dest.contains("laveno") || dest.contains("como") || dest.contains("novara") || dest.contains("varese")
                    } else {
                        val northWestOfSaronno = listOf("laveno", "como", "varese", "gerenzano", "turate", "lomazzo", "fino", "grandate")
                        val isNorthWest = northWestOfSaronno.any { current.contains(it) }
                        if (isNorthWest) {
                            dest.contains("cadorna") || dest.contains("milano") || dest.contains("lodi") || dest.contains("albairate")
                        } else {
                            dest.contains(home) || home.contains(dest)
                        }
                    }
                }
                else -> dest.contains(home) || home.contains(dest)
            }
        }
    }

    fun startAutoRefresh(station: Station, isDepartures: Boolean = true) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(45000)
                fetchTrains(station, isDepartures)
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun createDummyTrain(saved: SavedTrain): Train {
        val cat = when {
            saved.number.startsWith("20") || saved.number.startsWith("21") -> "RV"
            saved.number.startsWith("24") || saved.number.startsWith("10") -> "S"
            saved.number.startsWith("9") -> "FR"
            else -> "REG"
        }
        return Train(
            category = cat,
            number = saved.number,
            destination = saved.description.lowercase().replaceFirstChar { it.titlecase() },
            time = "--:--",
            delay = "In orario",
            platform = "--"
        )
    }

    fun addSmartRoute(origin: String, destination: String) {
        viewModelScope.launch {
            val list = smartRoutes.value.toMutableList()
            val route = SuburbanRoute(origin, destination)
            if (!list.any { it.id == route.id }) {
                list.add(route)
                dataStoreManager.saveSmartRoutes(list)
            }
        }
    }

    fun removeSmartRoute(id: String) {
        viewModelScope.launch {
            val list = smartRoutes.value.toMutableList()
            list.removeAll { it.id == id }
            dataStoreManager.saveSmartRoutes(list)
        }
    }

    private fun checkAndCleanOneShotNotifications() {
        viewModelScope.launch {
            val lastCleanDate = dataStoreManager.lastOneShotCleanDateFlow.first()
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            
            if (lastCleanDate != today) {
                var list = favoriteTrains.value.toMutableList()
                var changed = false
                
                list = list.map {
                    if (it.notifyDelay && (it.activeDays == null || it.activeDays.isEmpty())) {
                        changed = true
                        it.copy(
                            notifyDelay = false,
                            notifyDeparture = false,
                            notifyStationPass = false,
                            notifyPlatformChange = false
                        )
                    } else {
                        it
                    }
                }.toMutableList()
                
                if (changed) {
                    dataStoreManager.saveFavoriteTrains(list)
                }
                dataStoreManager.saveLastOneShotCleanDate(today)
            }
        }
    }
    fun fetchComfortReports(trainNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.backendService.getComfortReports(trainNumber)
                if (response.isSuccessful) {
                    val rawBody = response.body()?.string().orEmpty()
                    if (rawBody.isNotEmpty()) {
                        val root = JSONObject(rawBody)
                        val reportsMap = mutableMapOf<String, Int>()
                        
                        reportsMap["crowded"] = root.optInt("crowded", 0)
                        reportsMap["hot"] = root.optInt("hot", 0)
                        reportsMap["cold"] = root.optInt("cold", 0)
                        reportsMap["stopped"] = root.optInt("stopped", 0)

                        val blockedList = mutableListOf<String>()
                        val arr = root.optJSONArray("blocked_locations")
                        if (arr != null) {
                            for (i in 0 until arr.length()) {
                                blockedList.add(arr.getString(i))
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            _currentTrainReports.value = reportsMap
                            _currentTrainBlockedLocations.value = blockedList
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun postComfortReport(
        trainNumber: String,
        type: String,
        locality: String? = null,
        lastStation: String? = null,
        lastTime: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                val prefKey = "reported_train_${trainNumber}_${todayStr}_$type"
                val sharedPref = context.getSharedPreferences("comfort_reports", Context.MODE_PRIVATE)
                if (sharedPref.getBoolean(prefKey, false)) return@launch

                val payload = JSONObject().apply {
                    put("train_number", trainNumber)
                    put("report_type", type)
                    if (locality != null) put("locality", locality)
                    if (lastStation != null) put("last_station", lastStation)
                    if (lastTime != null) put("last_time", lastTime)
                }
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                
                val response = NetworkClient.backendService.postComfortReport(body)
                if (response.isSuccessful) {
                    sharedPref.edit().apply {
                        putBoolean(prefKey, true)
                        if (type == "moving") {
                            putBoolean("reported_train_${trainNumber}_${todayStr}_stopped", false)
                        } else if (type == "stopped") {
                            putBoolean("reported_train_${trainNumber}_${todayStr}_moving", false)
                        }
                    }.apply()
                    fetchComfortReports(trainNumber) // refresh
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
