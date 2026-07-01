package com.carlo.inorario.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class NewsItem(
    val title: String,
    val content: String,
    val isUrgent: Boolean,
    val category: String? = null,
    val date: String? = null
)

enum class DayType {
    @SerializedName("feriali") FERIALI,
    @SerializedName("sabato") SABATO,
    @SerializedName("festivo") FESTIVO;

    companion object {
        val current: DayType
            get() {
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                return when (dayOfWeek) {
                    Calendar.SUNDAY -> FESTIVO
                    Calendar.SATURDAY -> SABATO
                    else -> FERIALI
                }
            }
    }
}

data class MetroDepartureItem(
    val time: String,
    val destination: String
)

data class MetroDeparturesResponse(
    val departures: List<MetroDepartureItem>
)

data class FormattedDeparture(
    val timeString: String,
    val destinationName: String?
)

sealed class MetroDisplayMode {
    data class Exact(val departures: List<FormattedDeparture>) : MetroDisplayMode()
    data class Frequency(val text: String) : MetroDisplayMode()
    object Closed : MetroDisplayMode()
}

data class MetroLine(
    val name: String,
    val colorName: String,
    val pdfID: String?,
    val direction: Int = 0,
    val customFrequencies: Map<DayType, String>? = null,
    val destinations: Map<String, String>? = null
)

data class SavedTrain(
    val number: String,
    val description: String,
    val origin: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val notifyDelay: Boolean = false,
    val notifyDeparture: Boolean = false,
    val notifyStationPass: Boolean = false,
    val stationPassName: String? = null,
    val activeDays: List<Int>? = null,
    val lastNotifiedPlatform: String? = null,
    val notifyPlatformChange: Boolean = false,
    val platformChangeStationName: String? = null
)

data class VTSearchStation(
    val nomeLungo: String,
    val nomeBreve: String,
    @SerializedName("id") val vtID: String
)

data class TrenitaliaLocation(
    val id: Int,
    val name: String,
    val displayName: String
)

data class RFIStation(
    val name: String,
    @SerializedName("id") val rfiID: String?,
    val vtID: String?,
    val lat: Double? = null,
    val lon: Double? = null
)

data class Train(
    val category: String,
    val number: String,
    val destination: String,
    val time: String,
    val delay: String,
    val platform: String
) {
    val id: String = "${category}_${number}_${time}_${cleanStationName(destination)}"

    val estimatedArrivalTime: String
        get() {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("Europe/Rome")
            return try {
                val baseDate = format.parse(time) ?: return time
                val delayMinutes = delay.replace("+", "").replace("'", "").toIntOrNull() ?: 0
                val calendar = Calendar.getInstance()
                calendar.time = baseDate
                calendar.add(Calendar.MINUTE, delayMinutes)
                format.format(calendar.time)
            } catch (e: Exception) {
                time
            }
        }

    companion object {
        fun cleanStationName(name: String): String {
            var clean = name
            val replacements = listOf(
                "Milano Bovisa Politecnico" to "Bovisa",
                "Milano Bovisa" to "Bovisa",
                "Milano Porta Garibaldi" to "Porta Garibaldi",
                "Milano Lancetti" to "Lancetti",
                "Milano Rogoredo" to "Rogoredo",
                "Milano Forlanini" to "Forlanini",
                "Milano Porta Venezia" to "Porta Venezia",
                "Milano Repubblica" to "Repubblica",
                "Milano Dateo" to "Dateo",
                "Milano Porta Vittoria" to "Porta Vittoria",
                "Milano Villapizzone" to "Villapizzone",
                "Milano Cadorna" to "Cadorna",
                "Milano Centrale" to "Centrale",
                "Milano Greco Pirelli" to "Greco Pirelli",
                "Milano Scalo Romana" to "Scalo Romana",
                "Milano Porta Romana" to "Scalo Romana",
                "Milano San Cristoforo" to "San Cristoforo",
                "Milano Lambrate" to "Lambrate",
                "Milano Certosa" to "Certosa",
                "Milano Lodi T.i.b.b." to "Lodi T.I.B.B."
            )

            for ((target, replacement) in replacements) {
                if (clean.contains(target, ignoreCase = true)) {
                    clean = clean.replace(target, replacement, ignoreCase = true)
                }
            }

            if (clean.startsWith("Milano ", ignoreCase = true)) {
                clean = clean.substring(7)
            } else if (clean.startsWith("Milano", ignoreCase = true)) {
                clean = clean.substring(6)
            }

            return clean.trim()
        }
    }
}

data class TrainStatus(
    var lastStation: String = "--",
    var lastTime: String = "--",
    var statusMessage: String = "In attesa di dati...",
    var isDeparted: Boolean = false,
    var cancellationNote: String? = null,
    var isArrived: Boolean = false
)

data class Stop(
    val stationName: String,
    val time: String,
    val actualTime: String?,
    val delay: Int,
    val estimatedTime: String?
)

data class Station(
    val name: String,
    val rfiID: String?,
    val vtID: String?,
    val lat: Double? = null,
    val lon: Double? = null
) {
    val id: String = rfiID ?: vtID ?: name

    val metroLines: List<MetroLine>
        get() {
            val upperName = name.uppercase()
            return when {
                upperName.contains("RHO FIERA") -> listOf(
                    MetroLine("M1 Sesto", "red", "RHO FIERAMILANO", 0)
                )
                upperName.contains("GARIBALDI") -> listOf(
                    MetroLine("M2 Nord", "green", "GARIBALDI FS", 0),
                    MetroLine("M2 Sud", "green", "GARIBALDI FS", 1),
                    MetroLine("M5 Bignami", "purple", "GARIBALDI FS", 0),
                    MetroLine("M5 San Siro", "purple", "GARIBALDI FS", 1)
                )
                upperName.contains("CENTRALE") -> listOf(
                    MetroLine("M2 Nord", "green", "CENTRALE FS", 0),
                    MetroLine("M2 Sud", "green", "CENTRALE FS", 1),
                    MetroLine("M3 S. Donato", "yellow", "CENTRALE FS", 0),
                    MetroLine("M3 Comasina", "yellow", "CENTRALE FS", 1)
                )
                upperName.contains("REPUBBLICA") -> listOf(
                    MetroLine("M3 S. Donato", "yellow", "REPUBBLICA", 0),
                    MetroLine("M3 Comasina", "yellow", "REPUBBLICA", 1)
                )
                upperName.contains("VENEZIA") -> listOf(
                    MetroLine("M1 Sesto", "red", "P.TA VENEZIA", 0),
                    MetroLine("M1 Rho/Bisc.", "red", "P.TA VENEZIA", 1)
                )
                upperName.contains("DATEO") -> listOf(
                    MetroLine("M4 S. Cristoforo", "blue", "DATEO", 0),
                    MetroLine("M4 Linate", "blue", "DATEO", 1)
                )
                upperName.contains("FORLANINI") -> listOf(
                    MetroLine("M4 S. Cristoforo", "blue", "STAZIONE FORLANINI", 0),
                    MetroLine("M4 Linate", "blue", "STAZIONE FORLANINI", 1)
                )
                upperName.contains("SESTO S") || upperName.contains("SESTO SAN GIOVANNI") -> listOf(
                    MetroLine("M1 Rho/Bisc.", "red", "SESTO 1 MAGGIO FS", 1)
                )
                upperName.contains("CADORNA") -> listOf(
                    MetroLine("M1 Sesto", "red", "CADORNA FN M1", 0),
                    MetroLine("M1 Rho/Bisc.", "red", "CADORNA FN M1", 1),
                    MetroLine("M2 Nord", "green", "CADORNA FN M2", 0),
                    MetroLine("M2 Sud", "green", "CADORNA FN M2", 1)
                )
                upperName.contains("LAMBRATE") -> listOf(
                    MetroLine("M2 Nord", "green", "LAMBRATE FS", 0),
                    MetroLine("M2 Sud", "green", "LAMBRATE FS", 1)
                )
                upperName.contains("GENOVA") -> listOf(
                    MetroLine("M2 Nord", "green", "PORTA GENOVA FS", 0),
                    MetroLine("M2 Sud", "green", "PORTA GENOVA FS", 1)
                )
                upperName.contains("ROMOLO") -> listOf(
                    MetroLine("M2 Nord", "green", "ROMOLO", 0),
                    MetroLine("M2 Sud", "green", "ROMOLO", 1)
                )
                upperName.contains("AFFORI") -> listOf(
                    MetroLine("M3 S. Donato", "yellow", "AFFORI FN", 0),
                    MetroLine("M3 Comasina", "yellow", "AFFORI FN", 1)
                )
                upperName.contains("ROMANA") -> listOf(
                    MetroLine("M3 S. Donato", "yellow", "PORTA ROMANA", 0),
                    MetroLine("M3 Comasina", "yellow", "PORTA ROMANA", 1)
                )
                upperName.contains("ROGOREDO") -> listOf(
                    MetroLine("M3 S. Donato", "yellow", "ROGOREDO FS", 0),
                    MetroLine("M3 Comasina", "yellow", "ROGOREDO FS", 1)
                )
                upperName.contains("CRISTOFORO") -> listOf(
                    MetroLine("M4 S. Cristoforo", "blue", "SAN CRISTOFORO FS", 0),
                    MetroLine("M4 Linate", "blue", "SAN CRISTOFORO FS", 1)
                )
                upperName.contains("DOMODOSSOLA") -> listOf(
                    MetroLine("M5 Bignami", "purple", "DOMODOSSOLA FN", 0),
                    MetroLine("M5 San Siro", "purple", "DOMODOSSOLA FN", 1)
                )
                else -> emptyList()
            }
        }
}

enum class AppSection(val displayName: String) {
    @SerializedName("nearby") NEARBY("Stazione Vicina"),
    @SerializedName("myStations") MY_STATIONS("Le Mie Stazioni"),
    @SerializedName("favoriteTrains") FAVORITE_TRAINS("I miei Treni"),
    @SerializedName("passante") PASSANTE("Linee Suburbane"),
}

data class SuburbanLine(
    val id: String,
    val name: String,
    val hexColor: String,
    val stations: List<Station>
)

data class SuburbanRoute(
    val originName: String,
    val destinationName: String
) {
    val id: String = "$originName-$destinationName"
}

data class TravelSegment(
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val trainNumber: String,
    val trainCategory: String
)

data class TravelSolution(
    val trainNumber: String,
    val category: String,
    val departureTime: String,
    val arrivalTime: String,
    val origin: String,
    val destination: String,
    val duration: String,
    val segments: List<TravelSegment>
)

data class FavoriteRoute(
    val originName: String,
    val originID: String,
    val destinationName: String,
    val destinationID: String
) {
    val id: String = "$originID-$destinationID"
}

data class SavedTripSegment(
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val trainNumber: String,
    val trainCategory: String
)

data class SavedTrip(
    val id: String, // composite of origin, dest, departureTime
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val segments: List<SavedTripSegment>
) {
    val asTravelSolution: TravelSolution
        get() = TravelSolution(
            trainNumber = "",
            category = "Viaggio",
            departureTime = departureTime,
            arrivalTime = arrivalTime,
            origin = origin,
            destination = destination,
            duration = duration,
            segments = segments.map {
                TravelSegment(
                    origin = it.origin,
                    destination = it.destination,
                    departureTime = it.departureTime,
                    arrivalTime = it.arrivalTime,
                    trainNumber = it.trainNumber,
                    trainCategory = it.trainCategory
                )
            }
        )
}

data class StopsResult(
    val stops: List<Stop>,
    val status: TrainStatus,
    val errorMessage: String?
)

data class SmartRouteDetails(
    val isDirect: Boolean,
    val exchangeStation: Station?,
    val originStation: Station,
    val destinationStation: Station,
    val originTrains: List<Train>,
    val exchangeTrains: List<Train>
)
