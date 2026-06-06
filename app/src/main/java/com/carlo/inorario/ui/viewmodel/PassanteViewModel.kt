package com.carlo.inorario.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlo.inorario.data.local.DataStoreManager
import com.carlo.inorario.data.model.SmartRouteDetails
import com.carlo.inorario.data.model.Station
import com.carlo.inorario.data.model.SuburbanData
import com.carlo.inorario.data.model.SuburbanRoute
import com.carlo.inorario.data.model.Train
import com.carlo.inorario.data.model.TrainStatus
import com.carlo.inorario.data.network.RfiScraper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PassanteViewModel(
    private val dataStoreManager: DataStoreManager,
    private val trainViewModel: TrainViewModel,
) : ViewModel() {

    // --- State variables ---
    private val _passanteTrains = MutableStateFlow<List<Train>>(emptyList())
    val passanteTrains = _passanteTrains.asStateFlow()

    private val _isLoadingPassanteBoard = MutableStateFlow(value = false)
    val isLoadingPassanteBoard = _isLoadingPassanteBoard.asStateFlow()

    private val _passanteTunnelHealthMessage = MutableStateFlow("Circolazione Regolare nel Tunnel")
    val passanteTunnelHealthMessage = _passanteTunnelHealthMessage.asStateFlow()

    private val _passanteTunnelHealthColor = MutableStateFlow("#009640") // Green
    val passanteTunnelHealthColor = _passanteTunnelHealthColor.asStateFlow()

    private val _passanteTunnelAverageDelay = MutableStateFlow(0)
    val passanteTunnelAverageDelay = _passanteTunnelAverageDelay.asStateFlow()

    private val _passanteTunnelTrains = MutableStateFlow<List<Train>>(emptyList())
    val passanteTunnelTrains = _passanteTunnelTrains.asStateFlow()

    private val _passanteLiveStatuses = MutableStateFlow<Map<String, TrainStatus>>(emptyMap())
    val passanteLiveStatuses = _passanteLiveStatuses.asStateFlow()

    private val _loadedSmartRouteDetails = MutableStateFlow<Map<String, SmartRouteDetails>>(emptyMap())
    val loadedSmartRouteDetails = _loadedSmartRouteDetails.asStateFlow()

    private val _isLoadingSmartRoutes = MutableStateFlow(false)
    val isLoadingSmartRoutes = _isLoadingSmartRoutes.asStateFlow()

    // --- References Flow ---
    val selectedPassanteStation: StateFlow<Station> = dataStoreManager.selectedPassanteStationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Station("Porta Venezia", "1723", "S01061", 45.4746, 9.2052)
        )

    fun selectPassanteStation(station: Station) {
        viewModelScope.launch {
            dataStoreManager.saveSelectedPassanteStation(station)
            fetchPassanteLive()
        }
    }

    fun fetchPassanteLive() {
        _isLoadingPassanteBoard.value = true
        viewModelScope.launch {
            val station = selectedPassanteStation.value
            val trainsFetched = fetchTrainsForStation(station)
            _passanteTrains.value = trainsFetched
            _isLoadingPassanteBoard.value = false

            fetchTunnelHealth()
        }
    }

    private suspend fun fetchTunnelHealth() {
        val repubblica = Station("Repubblica", "1719", "S01060", 45.4795, 9.1963)
        val trainsFetched = fetchTrainsForStation(repubblica)

        if (trainsFetched.isEmpty()) {
            return
        }

        _passanteTunnelTrains.value = trainsFetched

        val romeZone = java.util.TimeZone.getTimeZone("Europe/Rome")
        val now = java.util.Calendar.getInstance(romeZone)
        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val currentTotalMinutes = (currentHour * 60) + currentMinute

        val delays = trainsFetched.mapNotNull { t ->
            val timeParts = t.time.split(":")
            if (timeParts.size < 2) return@mapNotNull null
            val trainHour = timeParts[0].toIntOrNull() ?: return@mapNotNull null
            val trainMinute = timeParts[1].toIntOrNull() ?: return@mapNotNull null
            val trainTotalMinutes = trainHour * 60 + trainMinute
            
            var diff = trainTotalMinutes - currentTotalMinutes
            if (diff < -12 * 60) diff += 24 * 60
            if (diff > 12 * 60) diff -= 24 * 60
            
            if (diff in -15..30) {
                val delayStr = t.delay.replace("+", "").replace("'", "")
                if (delayStr.lowercase().contains("orario")) 0 else delayStr.toIntOrNull()
            } else {
                null
            }
        }

        val avgDelay = if (delays.isEmpty()) {
            0
        } else {
            kotlin.math.round(delays.sum().toDouble() / delays.size).toInt()
        }
        _passanteTunnelAverageDelay.value = avgDelay

        val targetLines = trainViewModel.selectedSuburbanLines.value.ifEmpty {
            listOf("S1", "S2", "S5", "S6", "S12", "S13")
        }

        val trainsToQuery = trainsFetched.filter { train ->
            val line = resolveLine(train)
            targetLines.contains(line)
        }

        // Fetch live statuses in parallel
        viewModelScope.launch {
            val jobs = trainsToQuery.map { train ->
                async {
                    val result = trainViewModel.fetchLiveStops(train.number)
                    train.number to result.status
                }
            }
            val results = jobs.awaitAll()
            val newStatuses = results.toMap()
            _passanteLiveStatuses.value = newStatuses
        }

        // Detailed S-line analysis
        val lineCancellations = mutableMapOf<String, Int>()
        val lineDelays = mutableMapOf<String, MutableList<Int>>()

        for (train in trainsFetched) {
            val line = resolveLine(train)
            if (!line.startsWith("S")) continue

            val isCancelled = train.delay.lowercase().contains("soppresso") || train.delay.lowercase().contains("cancellato")
            if (isCancelled) {
                lineCancellations[line] = (lineCancellations[line] ?: 0) + 1
            } else {
                val delayStr = train.delay.replace("+", "").replace("'", "")
                val delayVal = if (delayStr.lowercase().contains("orario")) 0 else (delayStr.toIntOrNull() ?: 0)
                lineDelays.getOrPut(line) { mutableListOf() }.add(delayVal)
            }
        }

        val criticalLines = mutableListOf<String>()
        val delayedLines = mutableListOf<String>()

        for (line in listOf("S1", "S2", "S5", "S6", "S12", "S13")) {
            val cancellations = lineCancellations[line] ?: 0
            val delaysForLine = lineDelays[line] ?: emptyList()
            val avgDelayForLine = if (delaysForLine.isEmpty()) 0 else (delaysForLine.sum() / delaysForLine.size)

            if (cancellations > 0 || avgDelayForLine >= 8) {
                criticalLines.add(line)
            } else if (avgDelayForLine >= 3) {
                delayedLines.add(line)
            }
        }

        if (criticalLines.isNotEmpty()) {
            criticalLines.sortBy { it.replace("S", "").toIntOrNull() ?: 0 }
            _passanteTunnelHealthMessage.value = "Criticità su ${criticalLines.joinToString(", ")}"
            _passanteTunnelHealthColor.value = "#e30613" // Red
        } else if (delayedLines.isNotEmpty()) {
            delayedLines.sortBy { it.replace("S", "").toIntOrNull() ?: 0 }
            _passanteTunnelHealthMessage.value = "Rallentamenti su ${delayedLines.joinToString(", ")}"
            _passanteTunnelHealthColor.value = "#f39200" // Orange
        } else {
            _passanteTunnelHealthMessage.value = "Circolazione Regolare"
            _passanteTunnelHealthColor.value = "#009640" // Green
        }
    }

    private suspend fun fetchTrainsForStation(station: Station): List<Train> {
        // rfiID = numeric ID for iechub.rfi.it scraper (e.g. "1714")
        // vtID  = S0XXXX code for ViaggiaTreno API (e.g. "S01647")
        val rfiScraperId = station.rfiID
        val vtApiId = station.vtID

        if (!rfiScraperId.isNullOrEmpty()) {
            val trains = RfiScraper.performRfiScraping(rfiScraperId, isDepartures = true).first
            if (trains.isNotEmpty()) return trains
        }

        if (!vtApiId.isNullOrEmpty()) {
            try {
                val f = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'ZZZ", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Europe/Rome")
                }
                val dateStr = f.format(java.util.Date()).replace(" ", "%20")
                val response = com.carlo.inorario.data.network.NetworkClient.viaggiatrenoService.getStationBoard(
                    endpoint = "partenze",
                    vtID = vtApiId,
                    dateStr = dateStr
                )
                if (response.isSuccessful) {
                    val body = response.body()?.string().orEmpty()
                    val array = org.json.JSONArray(body)
                    val list = mutableListOf<Train>()
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val num = item.optInt("numeroTreno").toString()
                        var cat = item.optString("categoriaDescrizione").trim()
                        val dest = item.optString("destinazione").lowercase().replaceFirstChar { it.titlecase() }
                        val timeVal = item.optLong("orarioPartenza")
                        val ritardo = item.optInt("ritardo")

                        val binEff = item.optString("binarioEffettivoPartenzaDescrizione", "").takeIf { it.trim().lowercase() != "null" && it.isNotBlank() }
                        val binProg = item.optString("binarioProgrammatoPartenzaDescrizione", "").takeIf { it.trim().lowercase() != "null" && it.isNotBlank() }
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
                            val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getTimeZone("Europe/Rome")
                            }.format(java.util.Date(timeVal))
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
                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return emptyList()
    }

    private fun resolveLine(train: Train): String {
        val cat = train.category.uppercase()
        if (cat != "S" && cat.startsWith("S")) {
            return cat
        }
        val dest = train.destination.lowercase()
        return when {
            dest.contains("saronno") || dest.contains("lodi") -> "S1"
            dest.contains("mariano") || dest.contains("seveso") || dest.contains("camnago") -> "S2"
            dest.contains("varese") || dest.contains("treviglio") || dest.contains("gallarate") -> "S5"
            dest.contains("novara") || dest.contains("pioltello") -> "S6"
            dest.contains("melegnano") || dest.contains("cormano") -> "S12"
            dest.contains("pavia") || dest.contains("bovisa") -> "S13"
            else -> cat
        }
    }

    // --- Directions and Platforms filtering helpers ---

    private fun isRogoredoDestination(destName: String): Boolean {
        val d = destName.lowercase()
        return d.contains("rogoredo") || d.contains("lodi") ||
                d.contains("pavia") || d.contains("melegnano") ||
                d.contains("locate") || d.contains("borgolombardo") ||
                d.contains("s.donato") || d.contains("san donato") ||
                d.contains("cremona") || d.contains("piacenza") ||
                d.contains("mantova") || d.contains("s.giuliano") ||
                d.contains("san giuliano")
    }

    private fun isBovisaDestination(destName: String): Boolean {
        val d = destName.lowercase()
        return d.contains("bovisa") || d.contains("saronno") ||
                d.contains("mariano") || d.contains("como") ||
                d.contains("camnago") || d.contains("chiasso") ||
                d.contains("cormano") || d.contains("domodossola") ||
                d.contains("garbagnate") || d.contains("seveso") ||
                d.contains("cesano") || d.contains("cogliate") ||
                d.contains("meda") || d.contains("cabiate") ||
                d.contains("seregno") || d.contains("canzo") ||
                d.contains("asso") || d.contains("calolziocorte") ||
                d.contains("molteno") || d.contains("lecco")
    }

    private fun isForlaniniDestination(destName: String): Boolean {
        val d = destName.lowercase()
        return d.contains("treviglio") || d.contains("pioltello") ||
                d.contains("segrate") || d.contains("melzo") ||
                d.contains("vignate") || d.contains("pozzuolo") ||
                (d.contains("forlanini") && !d.contains("rogoredo"))
    }

    private fun isRhoDestination(destName: String): Boolean {
        val d = destName.lowercase()
        return d.contains("novara") || d.contains("varese") ||
                d.contains("gallarate") || d.contains("malpensa") ||
                d.contains("rho") || d.contains("certosa") ||
                d.contains("busto") || d.contains("casale")
    }

    fun getPassanteBranch(train: Train): String? {
        val cat = train.category.uppercase()
        val dest = train.destination
        return when {
            cat == "S1" || cat == "S2" || cat == "S12" || cat == "S13" -> {
                if (isRogoredoDestination(dest)) "Rogoredo" else "Bovisa"
            }
            cat == "S5" || cat == "S6" -> {
                if (isForlaniniDestination(dest)) "Forlanini" else "Rho"
            }
            else -> null
        }
    }

    fun getPassanteDirection(train: Train): String? {
        val branch = getPassanteBranch(train) ?: return null
        return if (branch == "Bovisa" || branch == "Rho") "Ovest" else "Est"
    }

    fun resolvedPlatform(stationName: String, train: Train): String {
        val name = stationName.lowercase()
        val direction = getPassanteDirection(train) ?: "Est"
        val cat = train.category.uppercase()

        return when {
            name.contains("rho fiera") -> if (direction == "Ovest") "1" else "2"
            name.contains("certosa") -> if (direction == "Est") "5" else "6"
            name.contains("villapizzone") -> if (direction == "Ovest") "1" else "2"
            name.contains("lancetti") -> {
                if (direction == "Est") {
                    if (cat == "S5" || cat == "S6") "1" else "2"
                } else {
                    if (cat == "S5" || cat == "S6") "3" else "4"
                }
            }
            name.contains("garibaldi") -> if (direction == "Est") "1" else "2"
            name.contains("repubblica") -> if (direction == "Est") "1" else "2"
            name.contains("venezia") || name.contains("porta venezia") -> if (direction == "Est") "1" else "2"
            name.contains("dateo") -> if (direction == "Est") "1" else "2"
            name.contains("vittoria") || name.contains("porta vittoria") -> {
                if (direction == "Est") {
                    if (cat == "S5" || cat == "S6") "3" else "4"
                } else {
                    if (cat == "S5" || cat == "S6") "1" else "2"
                }
            }
            name.contains("forlanini") -> {
                if (cat == "S9") {
                    if (direction == "Est") "3" else "4"
                } else {
                    if (direction == "Est") "1" else "2"
                }
            }
            else -> train.platform
        }
    }

    // --- Filtered lists for rendering branches ---

    fun getPassanteTrainsViaBovisa(trainsList: List<Train>): List<Train> {
        return trainsList.filter { train ->
            val cat = train.category.uppercase()
            val dest = train.destination
            if (cat == "S1" || cat == "S2" || cat == "S12" || cat == "S13") {
                !isRogoredoDestination(dest)
            } else {
                isBovisaDestination(dest)
            }
        }
    }

    fun getPassanteTrainsViaRho(trainsList: List<Train>): List<Train> {
        return trainsList.filter { train ->
            val cat = train.category.uppercase()
            val dest = train.destination
            if (cat == "S5" || cat == "S6") {
                !isForlaniniDestination(dest)
            } else {
                isRhoDestination(dest)
            }
        }
    }

    fun getPassanteTrainsViaForlanini(trainsList: List<Train>): List<Train> {
        return trainsList.filter { train ->
            val cat = train.category.uppercase()
            val dest = train.destination
            if (cat == "S5" || cat == "S6") {
                isForlaniniDestination(dest)
            } else {
                isForlaniniDestination(dest)
            }
        }
    }

    fun getPassanteTrainsViaRogoredo(trainsList: List<Train>): List<Train> {
        return trainsList.filter { train ->
            val cat = train.category.uppercase()
            val dest = train.destination
            if (cat == "S1" || cat == "S2" || cat == "S12" || cat == "S13") {
                isRogoredoDestination(dest)
            } else {
                isRogoredoDestination(dest)
            }
        }
    }

    // --- Smart connection routes calculations ---

    fun fetchSmartRoutesLive() {
        _isLoadingSmartRoutes.value = true
        viewModelScope.launch {
            val list = trainViewModel.smartRoutes.value
            val detailsMap = mutableMapOf<String, SmartRouteDetails>()
            for (route in list) {
                val details = findSuburbanRouteDetails(route.originName, route.destinationName)
                if (details != null) {
                    detailsMap[route.id] = details
                }
            }
            _loadedSmartRouteDetails.value = detailsMap
            _isLoadingSmartRoutes.value = false
        }
    }

    suspend fun refreshSmartRoute(route: SuburbanRoute) {
        val details = findSuburbanRouteDetails(route.originName, route.destinationName)
        if (details != null) {
            _loadedSmartRouteDetails.update { it.toMutableMap().apply { put(route.id, details) } }
        }
    }

    private suspend fun findSuburbanRouteDetails(origin: String, destination: String): SmartRouteDetails? {
        val allStations = SuburbanData.allLines.flatMap { it.stations }
        val origStation = allStations.firstOrNull { it.name.equals(origin, ignoreCase = true) } ?: return null
        val destStation = allStations.firstOrNull { it.name.equals(destination, ignoreCase = true) } ?: return null

        val origLines = SuburbanData.allLines.filter { line ->
            line.stations.any { it.name.equals(origin, ignoreCase = true) }
        }
        val destLines = SuburbanData.allLines.filter { line ->
            line.stations.any { it.name.equals(destination, ignoreCase = true) }
        }

        val directLines = origLines.filter { ol -> destLines.any { dl -> dl.id == ol.id } }

        return if (directLines.isNotEmpty()) {
            val scraped = fetchTrainsForStation(origStation)
            val directTrains = scraped.filter { t ->
                val cat = t.category.uppercase()
                directLines.any { it.id == cat || t.number.startsWith(it.id) }
            }
            SmartRouteDetails(
                isDirect = true,
                exchangeStation = null,
                originStation = origStation,
                destinationStation = destStation,
                originTrains = directTrains.take(3),
                exchangeTrains = emptyList()
            )
        } else {
            val tunnelStations = listOf("Lancetti", "P. Garibaldi Passante", "Repubblica", "Porta Venezia", "Dateo", "Porta Vittoria")
            var bestExchange: Station? = null
            for (ts in tunnelStations) {
                if (origLines.any { ol -> ol.stations.any { it.name == ts } } &&
                    destLines.any { dl -> dl.stations.any { it.name == ts } }
                ) {
                    bestExchange = allStations.firstOrNull { it.name == ts }
                    break
                }
            }

            if (bestExchange == null) {
                for (line in origLines) {
                    for (s in line.stations) {
                        if (destLines.any { dl -> dl.stations.any { it.name == s.name } }) {
                            bestExchange = s
                            break
                        }
                    }
                    if (bestExchange != null) break
                }
            }

            val exchange = bestExchange ?: return null

            // Parallel fetches
            val origJob = viewModelScope.async { fetchTrainsForStation(origStation) }
            val exJob = viewModelScope.async { fetchTrainsForStation(exchange) }
            val origTrains = origJob.await()
            val exTrains = exJob.await()

            val toExchangeTrains = origTrains.filter { t ->
                val cat = t.category.uppercase()
                origLines.any { it.id == cat }
            }

            val toDestTrains = exTrains.filter { t ->
                val cat = t.category.uppercase()
                destLines.any { it.id == cat }
            }

            SmartRouteDetails(
                isDirect = false,
                exchangeStation = exchange,
                originStation = origStation,
                destinationStation = destStation,
                originTrains = toExchangeTrains.take(2),
                exchangeTrains = toDestTrains.take(2)
            )
        }
    }
}
