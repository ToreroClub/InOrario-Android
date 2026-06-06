package com.carlo.inorario.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carlo.inorario.data.model.AppSection
import com.carlo.inorario.data.model.FavoriteRoute
import com.carlo.inorario.data.model.FullSchedule
import com.carlo.inorario.data.model.SavedTrain
import com.carlo.inorario.data.model.SavedTrip
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.SuburbanRoute
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "in_orario_preferences")

class DataStoreManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val REMOTE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("remoteNotificationsEnabled_v1")
        private val NOTIFY_ON_STATION_PASS = booleanPreferencesKey("notifyOnStationPass_v1")
        private val FCM_TOKEN = stringPreferencesKey("fcmToken_v1")
        private val HAS_CAPPUCCINO = booleanPreferencesKey("hasCappuccino_v1")
        private val HAS_COLAZIONE = booleanPreferencesKey("hasColazione_v1")
        private val DEVELOPER_MOCK_PURCHASES = booleanPreferencesKey("developerMockPurchases_v1")
        private val FAVORITE_TRAINS = stringPreferencesKey("savedFavoriteTrains_v3")
        private val MY_STATIONS = stringPreferencesKey("savedMyStations_v3")
        private val SECTION_ORDER = stringPreferencesKey("savedSectionOrder_v3")
        private val FAVORITE_ROUTES = stringPreferencesKey("savedFavoriteRoutes_v1")
        private val SAVED_TRIPS = stringPreferencesKey("savedTrips_v1")
        private val SELECTED_SUBURBAN_LINES = stringPreferencesKey("selectedSuburbanLines_v1")
        private val HIDDEN_SUBURBAN_STATIONS = stringPreferencesKey("hiddenSuburbanStations_v1")
        private val SELECTED_PASSANTE_STATION = stringPreferencesKey("selectedPassanteStation_v1")
        private val SMART_ROUTES = stringPreferencesKey("savedSmartRoutes_v1")
        private val HOME_DESTINATION_STATION_NAME = stringPreferencesKey("homeDestinationStationName_v1")
        private val USER_NAME = stringPreferencesKey("userName_v1")
        private val USE_SPECIAL_PASSANTE_VIEW = booleanPreferencesKey("useSpecialPassanteView_v1")
        private val METRO_CACHE = stringPreferencesKey("com.magenta.metro.cache")
        private val COLLAPSED_SECTIONS = stringPreferencesKey("collapsedSections_v1")
        private val STRIKE_REGION = stringPreferencesKey("strikeRegion_v1")
    }

    // --- Getters with default values ---

    val favoriteTrainsFlow: Flow<List<SavedTrain>> = context.dataStore.data.map { preferences ->
        val json = preferences[FAVORITE_TRAINS] ?: return@map emptyList()
        val type = object : TypeToken<List<SavedTrain>>() {}.type
        gson.fromJson<List<SavedTrain>>(json, type) ?: emptyList()
    }

    val myStationsFlow: Flow<List<Station>> = context.dataStore.data.map { preferences ->
        val json = preferences[MY_STATIONS] ?: return@map emptyList()
        val type = object : TypeToken<List<Station>>() {}.type
        val rawList: List<Station> = gson.fromJson(json, type) ?: emptyList()
        // Fix corrupted stations that might have vtID saved as rfiID (starts with S or N)
        rawList.map { st ->
            if (st.rfiID != null && ((st.rfiID.startsWith("S")) || (st.rfiID.startsWith("N")))) {
                Station(st.name, null, st.vtID, st.lat, st.lon)
            } else {
                st
            }
        }
    }

    val sectionOrderFlow: Flow<List<AppSection>> = context.dataStore.data.map { preferences ->
        val json = preferences[SECTION_ORDER]
        if (json == null) {
            AppSection.entries.toList()
        } else {
            val type = object : TypeToken<List<AppSection>>() {}.type
            val loaded: MutableList<AppSection> = gson.fromJson(json, type) ?: AppSection.entries.toMutableList()
            for (section in AppSection.entries) {
                if (!loaded.contains(section)) {
                    loaded.add(section)
                }
            }
            loaded
        }
    }

    val favoriteRoutesFlow: Flow<List<FavoriteRoute>> = context.dataStore.data.map { preferences ->
        val json = preferences[FAVORITE_ROUTES] ?: return@map emptyList()
        val type = object : TypeToken<List<FavoriteRoute>>() {}.type
        gson.fromJson<List<FavoriteRoute>>(json, type) ?: emptyList()
    }

    val savedTripsFlow: Flow<List<SavedTrip>> = context.dataStore.data.map { preferences ->
        val json = preferences[SAVED_TRIPS] ?: return@map emptyList()
        val type = object : TypeToken<List<SavedTrip>>() {}.type
        gson.fromJson<List<SavedTrip>>(json, type) ?: emptyList()
    }

    val selectedSuburbanLinesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[SELECTED_SUBURBAN_LINES]
        if (json == null) {
            listOf("S5", "S6")
        } else {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: listOf("S5", "S6")
        }
    }

    val hiddenSuburbanStationsFlow: Flow<Map<String, List<String>>> = context.dataStore.data.map { preferences ->
        val json = preferences[HIDDEN_SUBURBAN_STATIONS] ?: return@map emptyMap()
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        gson.fromJson<Map<String, List<String>>>(json, type) ?: emptyMap()
    }

    val selectedPassanteStationFlow: Flow<Station> = context.dataStore.data.map { preferences ->
        val json = preferences[SELECTED_PASSANTE_STATION]
        if (json == null) {
            Station("Porta Venezia", "1723", "S01061", 45.4746, 9.2052)
        } else {
            gson.fromJson(json, Station::class.java) ?: Station("Porta Venezia", "1723", "S01061", 45.4746, 9.2052)
        }
    }

    val smartRoutesFlow: Flow<List<SuburbanRoute>> = context.dataStore.data.map { preferences ->
        val json = preferences[SMART_ROUTES]
        if (json == null) {
            listOf(SuburbanRoute("Magenta", "Milano Bovisa"))
        } else {
            val type = object : TypeToken<List<SuburbanRoute>>() {}.type
            gson.fromJson<List<SuburbanRoute>>(json, type) ?: listOf(SuburbanRoute("Magenta", "Milano Bovisa"))
        }
    }

    val homeDestinationStationNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_DESTINATION_STATION_NAME] ?: ""
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val useSpecialPassanteViewFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SPECIAL_PASSANTE_VIEW] ?: true
    }
    val metroCacheFlow: Flow<Map<String, FullSchedule>> = context.dataStore.data.map { preferences ->
        val json = preferences[METRO_CACHE] ?: return@map emptyMap()
        val type = object : TypeToken<Map<String, FullSchedule>>() {}.type
        gson.fromJson<Map<String, FullSchedule>>(json, type) ?: emptyMap()
    }

    val collapsedSectionsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[COLLAPSED_SECTIONS] ?: return@map emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        gson.fromJson<Set<String>>(json, type) ?: emptySet()
    }

    // --- Setters ---

    
    val remoteNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMOTE_NOTIFICATIONS_ENABLED] ?: false
    }

    val notifyOnStationPassFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFY_ON_STATION_PASS] ?: false
    }

    val fcmTokenFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN] ?: ""
    }

    val hasCappuccinoFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MOCK_PURCHASES] == true || (preferences[HAS_CAPPUCCINO] ?: false)
    }

    val hasColazioneFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MOCK_PURCHASES] == true || (preferences[HAS_COLAZIONE] ?: false)
    }

    val developerMockPurchasesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MOCK_PURCHASES] ?: false
    }

    val strikeRegionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STRIKE_REGION] ?: "Tutte"
    }

suspend fun saveFavoriteTrains(trains: List<SavedTrain>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_TRAINS] = gson.toJson(trains)
        }
    }

    suspend fun saveMyStations(stations: List<Station>) {
        context.dataStore.edit { preferences ->
            preferences[MY_STATIONS] = gson.toJson(stations)
        }
    }

    suspend fun saveSectionOrder(order: List<AppSection>) {
        context.dataStore.edit { preferences ->
            preferences[SECTION_ORDER] = gson.toJson(order)
        }
    }

    suspend fun saveFavoriteRoutes(routes: List<FavoriteRoute>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_ROUTES] = gson.toJson(routes)
        }
    }

    suspend fun saveSavedTrips(trips: List<SavedTrip>) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_TRIPS] = gson.toJson(trips)
        }
    }

    suspend fun saveSelectedSuburbanLines(lines: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_SUBURBAN_LINES] = gson.toJson(lines)
        }
    }

    suspend fun saveHiddenSuburbanStations(hidden: Map<String, List<String>>) {
        context.dataStore.edit { preferences ->
            preferences[HIDDEN_SUBURBAN_STATIONS] = gson.toJson(hidden)
        }
    }

    suspend fun saveSelectedPassanteStation(station: Station) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PASSANTE_STATION] = gson.toJson(station)
        }
    }

    suspend fun saveSmartRoutes(routes: List<SuburbanRoute>) {
        context.dataStore.edit { preferences ->
            preferences[SMART_ROUTES] = gson.toJson(routes)
        }
    }

    suspend fun saveHomeDestinationStationName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_DESTINATION_STATION_NAME] = name
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun saveUseSpecialPassanteView(use: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SPECIAL_PASSANTE_VIEW] = use
        }
    }
    suspend fun saveMetroCache(cache: Map<String, FullSchedule>) {
        context.dataStore.edit { preferences ->
            preferences[METRO_CACHE] = gson.toJson(cache)
        }
    }

    suspend fun saveCollapsedSections(collapsed: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[COLLAPSED_SECTIONS] = gson.toJson(collapsed)
        }
    }

    suspend fun saveRemoteNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMOTE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveNotifyOnStationPass(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFY_ON_STATION_PASS] = enabled
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN] = token
        }
    }

    suspend fun saveHasCappuccino(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_CAPPUCCINO] = value
        }
    }

    suspend fun saveHasColazione(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_COLAZIONE] = value
        }
    }

    suspend fun saveDeveloperMockPurchases(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MOCK_PURCHASES] = value
        }
    }

    suspend fun saveStrikeRegion(region: String) {
        context.dataStore.edit { preferences ->
            preferences[STRIKE_REGION] = region
        }
    }
}
