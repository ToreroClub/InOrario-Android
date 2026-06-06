package com.carlo.inorario.data.network

import com.carlo.inorario.data.model.Train
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RfiScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun stripHTML(str: String): String {
        var text = "<td$str"
        // Replace html tags with spaces
        text = text.replace(Regex("<[^>]+>"), " ")
        text = text.replace(Regex("<td", RegexOption.IGNORE_CASE), " ")
        text = text.replace("&amp;", "&")
        text = text.replace("&nbsp;", " ")
        text = text.replace("&#39;", "'")
        text = text.replace("&#39", "'")
        text = text.replace("&apos;", "'")
        text = text.replace("&quot;", "\"")
        return text.trim()
    }

    suspend fun performRfiScraping(rfiID: String, isDepartures: Boolean): Pair<List<Train>, String?> = withContext(Dispatchers.IO) {
        val urlString = "https://iechub.rfi.it/ArriviPartenze/ArrivalsDepartures/Monitor?placeId=$rfiID&arrivals=${!isDepartures}"
        val request = Request.Builder()
            .url(urlString)
            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Pair(emptyList(), null)
                val html = response.body?.string() ?: return@withContext Pair(emptyList(), null)

                var stationAlerts: String? = null
                val avvisiIndex = html.indexOf("Avvisi")
                if (avvisiIndex != -1) {
                    val subHtml = html.substring(avvisiIndex)
                    val endDivIndex = subHtml.indexOf("</div>")
                    if (endDivIndex != -1) {
                        val alertRaw = subHtml.substring(0, endDivIndex)
                        var cleanAlert = stripHTML(alertRaw)
                            .replace("Avvisi", "")
                            .replace("<", "")
                            .trim()

                        if (cleanAlert.uppercase().contains("VIETATO APRIRE LE PORTE")) {
                            cleanAlert = ""
                        }

                        if (cleanAlert.isNotEmpty()) {
                            stationAlerts = cleanAlert
                        }
                    }
                }

                // Parse rows
                val cleanHtml = html.replace("<TR", "<tr").replace("<TD", "<td")
                val rows = cleanHtml.split("<tr")
                val scrapedTrains = mutableListOf<Train>()

                // Drop first row as it contains header
                for (row in rows.drop(1)) {
                    val cols = row.split("<td")
                    if (cols.size >= 8) {
                        var cat = stripHTML(cols[2])
                            .replace("Categoria", "", ignoreCase = true)
                            .trim()

                        if (cat.isEmpty()) {
                            val altPattern = Pattern.compile("alt=\"([^\"]+)\"")
                            val matcher = altPattern.matcher(cols[2])
                            if (matcher.find()) {
                                val match = matcher.group(1) ?: ""
                                val rawAlt = match.replace("Categoria", "", ignoreCase = true)
                                cat = stripHTML(rawAlt)
                            }
                        }

                        val num = stripHTML(cols[3])
                        val dest = stripHTML(cols[4])
                        val time = stripHTML(cols[5])
                        val delayRaw = stripHTML(cols[6])
                        val plat = stripHTML(cols[7])

                        var mappedCat = cat
                        val catUpper = cat.uppercase()
                        if (catUpper.contains("ALTA VELOCIT")) mappedCat = "AV"
                        else if (catUpper.contains("INTERCITY")) mappedCat = "IC"
                        else if (catUpper.contains("EUROCITY")) mappedCat = "EC"
                        else if (catUpper == "REGIONALE VELOCE") mappedCat = "RV"
                        else if (catUpper == "REGIONALE") mappedCat = "REG"

                        if (mappedCat.isEmpty()) {
                            mappedCat = if (num.startsWith("20") || num.startsWith("21")) {
                                "RV"
                            } else if (num.startsWith("24") || num.startsWith("10")) {
                                "S"
                            } else {
                                "REG"
                            }
                        }

                        if ((mappedCat == "S") || (mappedCat == "REG")) {
                            mappedCat = when {
                                num.startsWith("240") || num.startsWith("230") || num.startsWith("241") || num.startsWith("231") -> "S1"
                                num.startsWith("242") || num.startsWith("232") -> {
                                    val d = dest.lowercase()
                                    if (d.contains("melegnano") || d.contains("cormano")) "S12" else "S2"
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
                                        else -> mappedCat
                                    }
                                }
                            }
                        }

                        val delayFormatted = if (delayRaw.isEmpty()) "In orario" else "+$delayRaw'"
                        val platformFormatted = if (plat.isEmpty() || plat.trim().lowercase() == "null") "--" else plat

                        if (num.isNotEmpty() && time.contains(":")) {
                            scrapedTrains.add(
                                Train(
                                    category = mappedCat,
                                    number = num,
                                    destination = dest.lowercase().replaceFirstChar { it.titlecase() },
                                    time = time,
                                    delay = delayFormatted,
                                    platform = platformFormatted,
                                )
                            )
                        }
                    }
                }
                Pair(scrapedTrains, stationAlerts)
            }
        } catch (_: Exception) {
            Pair(emptyList(), null)
        }
    }
}
