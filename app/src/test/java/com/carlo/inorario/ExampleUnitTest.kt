package com.carlo.inorario

import org.junit.Test
import java.net.URL
import java.util.regex.Pattern

class ExampleUnitTest {
    @Test
    fun testScraping() {
        val html = URL("https://iechub.rfi.it/ArriviPartenze/ArrivalsDepartures/Monitor?placeId=1708&arrivals=false").readText()

        fun stripHTML(str: String): String {
            var text = "<td$str"
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

        var cleanHtml = html.replace("<TR", "<tr").replace("<TD", "<td")
        val rows = cleanHtml.split("<tr")
        
        var count = 0

        for (row in rows.drop(1)) {
            val cols = row.split("<td")
            if (cols.size >= 8) {
                val num = stripHTML(cols[3])
                val time = stripHTML(cols[5])
                if (num.isNotEmpty() && time.contains(":")) {
                    count++
                    println("Found train: $num at $time")
                }
            }
        }
        println("Total trains: $count")
        assert(count > 0)
    }
}