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
    val category: String? = null
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

data class MetroDeparture(
    val min: Int,
    val color: String
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

data class FullSchedule(
    val feriali: Map<Int, List<MetroDeparture>> = emptyMap(),
    val sabato: Map<Int, List<MetroDeparture>> = emptyMap(),
    val festivo: Map<Int, List<MetroDeparture>> = emptyMap(),
    val frequenze: Map<String, String> = emptyMap(),
    val lastSyncDate: Date? = null
)

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
    val stationPassName: String? = null
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
    val vtID: String?
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
        get() = when (name) {
            "Rho Fiera" -> listOf(
                MetroLine(
                    name = "M1 Sesto",
                    colorName = "red",
                    pdfID = "504",
                    direction = 0,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Ogni 4' - 9'",
                        DayType.SABATO to "Ogni 7' - 9'",
                    )
                )
            )
            "Porta Garibaldi", "P. Garibaldi Passante" -> listOf(
                MetroLine(
                    name = "M2 Nord",
                    colorName = "green",
                    pdfID = "682",
                    direction = 0,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Gobba: 2'-4'   Gessate: 5'-12'   Cologno: 5'-12'",
                        DayType.SABATO to "Gobba: 5'   Gessate: 10'-12'   Cologno: 10'",
                    ),
                    destinations = mapOf("orange" to "Gessate", "blue" to "Cologno N.", "black" to "C. Gobba")
                ),
                MetroLine(
                    name = "M2 Sud",
                    colorName = "green",
                    pdfID = "682",
                    direction = 1,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Famagosta: 2'-4'   Abbiategrasso: 5'-7'   Assago: 5'-12'",
                        DayType.SABATO to "Famagosta: 4'-5'   Abbiategrasso: 9'-10'   Assago: 10'-11'",
                    ),
                    destinations = mapOf("orange" to "Assago", "blue" to "Abbiategrasso", "black" to "Famagosta")
                ),
                MetroLine("M5 Bignami", "purple", "308", 0),
                MetroLine("M5 San Siro", "purple", "308", 1)
            )
            "Milano Centrale" -> listOf(
                MetroLine(
                    name = "M2 Nord",
                    colorName = "green",
                    pdfID = "680",
                    direction = 0,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Gobba: 2'-4'   Gessate: 5'-12'   Cologno: 5'-12'",
                        DayType.SABATO to "Gobba: 5'   Gessate: 10'-12'   Cologno: 10'",
                    ),
                    destinations = mapOf("orange" to "Gessate", "blue" to "Cologno N.", "black" to "C. Gobba")
                ),
                MetroLine(
                    name = "M2 Sud",
                    colorName = "green",
                    pdfID = "680",
                    direction = 1,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Famagosta: 2'-4'   Abbiategrasso: 5'-7'   Assago: 5'-12'",
                        DayType.SABATO to "Famagosta: 4'-5'   Abbiategrasso: 9'-10'   Assago: 10'-11'",
                    ),
                    destinations = mapOf("orange" to "Assago", "blue" to "Abbiategrasso", "black" to "Famagosta")
                ),
                MetroLine("M3 S. Donato", "yellow", "731", 0),
                MetroLine("M3 Comasina", "yellow", "731", 1)
            )
            "Repubblica" -> listOf(
                MetroLine("M3 S. Donato", "yellow", "732", 0),
                MetroLine("M3 Comasina", "yellow", "732", 1)
            )
            "Porta Venezia" -> listOf(
                MetroLine(
                    name = "M1 Rho/Bisc.",
                    colorName = "red",
                    pdfID = "536",
                    direction = 1,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Pagano: 2'-4'   Rho: 4'-11'   Bisceglie: 4'-8'",
                        DayType.SABATO to "Pagano: 3'-5'   Rho: 7'-9'   Bisceglie: 7'-11'",
                    ),
                    destinations = mapOf("orange" to "Rho Fiera", "blue" to "Bisceglie", "black" to "Pagano")
                ),
                MetroLine(
                    name = "M1 Sesto",
                    colorName = "red",
                    pdfID = "536",
                    direction = 0,
                    customFrequencies = mapOf(
                        DayType.FERIALI to "Ogni 2' - 3'",
                        DayType.SABATO to "Ogni 3' - 4'",
                        DayType.FESTIVO to "Ogni 5' - 8'",
                    )
                )
            )
            "Dateo" -> listOf(
                MetroLine("M4 S. Cristoforo", "blue", "336", 0),
                MetroLine("M4 Linate", "blue", "336", 1)
            )
            "Forlanini" -> listOf(
                MetroLine("M4 S. Cristoforo", "blue", "339", 0),
                MetroLine("M4 Linate", "blue", "339", 1)
            )
            else -> emptyList()
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
