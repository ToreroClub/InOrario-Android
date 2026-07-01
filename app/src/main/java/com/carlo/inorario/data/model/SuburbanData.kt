package com.carlo.inorario.data.model

object SuburbanData {
    val allLines: List<SuburbanLine>

    init {
        // --- Stations ---
        val bovisa = Station("Milano Bovisa", null, "S01642", 45.5025, 9.1592)
        val certosa = Station("Certosa", "1708", "S01640", 45.5085, 9.1272)
        val villapizzone = Station("Villapizzone", "3099", "S01639", 45.4998, 9.1465)
        val lancetti = Station("Lancetti", "1713", "S01643", 45.4925, 9.1751)
        val garibaldiPassante = Station("P. Garibaldi Passante", "1714", "S01647", 45.4844, 9.1887)
        val repubblica = Station("Repubblica", "1719", "S01648", 45.4795, 9.1963)
        val venezia = Station("Porta Venezia", "1723", "S01649", 45.4746, 9.2052)
        val dateo = Station("Dateo", "3468", "S01650", 45.4682, 9.2158)
        val vittoria = Station("Porta Vittoria", "1718", "S01633", 45.4613, 9.2227)
        val rogoredo = Station("Milano Rogoredo", "1720", "S01820", 45.4333, 9.2389)
        val forlanini = Station("Forlanini", "3169", "S01492", 45.4625, 9.2368)

        val domodossola = Station("Milano Domodossola", null, "S01067", 45.4811, 9.1619)
        val cadorna = Station("Milano Cadorna", null, "S01066", 45.4686, 9.1752)

        val saronno = Station("Saronno", null, "S01933", 45.6264, 9.0336)
        val greco = Station("Milano Greco Pirelli", "1711", "S01326", 45.5129, 9.2141)
        val lambrate = Station("Milano Lambrate", "1712", "S01701", 45.4849, 9.2373)
        val romana = Station("Milano Scalo Romana", "1717", "S01632", 45.4458, 9.2131)
        val tibaldi = Station("Milano Tibaldi", "3251", "S01022", 45.4436, 9.1840)
        val romolo = Station("Milano Romolo", "58", "S01032", 45.4432, 9.1678)
        val cristoforo = Station("Milano S. Cristoforo", "1721", "S01630", 45.4425, 9.1302)
        val albairate = Station("Albairate-Vermezzo", "1734", "S01035", 45.4044, 8.9575)

        val garibaldiSup = Station("Milano P. Garibaldi", "1715", "S01645", 45.4844, 9.1887)
        val rhoFiera = Station("Rho Fiera", "3098", "S01039", 45.5215, 9.0883)

        // S6 and S5 West / East
        val novara = Station("Novara", "1917", "S00248", 45.4524, 8.6253)
        val trecate = Station("Trecate", "2909", "S00252", 45.4374, 8.7428)
        val magenta = Station("Magenta", "1618", "S01040", 45.4641, 8.8845)
        val corbetta = Station("Corbetta-S.Stefano Ticino", "1174", "S01041", 45.4716, 8.9189)
        val vittuone = Station("Vittuone-Arluno", "3119", "S01042", 45.4921, 8.9568)
        val pregnana = Station("Pregnana Milanese", "381", "S01058", 45.5036, 9.0069)
        val rho = Station("Rho", "2345", "S01037", 45.5262, 9.0402)
        val segrate = Station("Segrate", "3507", "S01715", 45.4712, 9.2974)
        val pioltello = Station("Pioltello-Limito", "2147", "S01703", 45.4801, 9.3245)

        val varese = Station("Varese", "2994", "S01205", 45.8176, 8.8329)
        val gazzada = Station("Gazzada-Schianno-Morazzone", "1413", "S01207", 45.7821, 8.8251)
        val castronno = Station("Castronno", "1029", "S01208", 45.7483, 8.8105)
        val albizzate = Station("Albizzate-Solbiate Arno", "405", "S01209", 45.7196, 8.8021)
        val cavaria = Station("Cavaria-Oggiona-Jerago", "1046", "S01210", 45.6985, 8.8183)
        val gallarate = Station("Gallarate", "1393", "S01030", 45.6599, 8.7963)
        val busto = Station("Busto Arsizio", "766", "S01031", 45.6062, 8.8612)
        val legnano = Station("Legnano", "1554", "S01033", 45.5925, 8.9189)
        val canegrate = Station("Canegrate", "858", "S01034", 45.5684, 8.9321)
        val parabiago = Station("Parabiago", "2033", "S01035", 45.5562, 8.9483)
        val vanzago = Station("Vanzago-Pogliano", "2987", "S01036", 45.5262, 8.9951)
        val melzo = Station("Melzo", "1690", "S01705", 45.4983, 9.4212)
        val pozzuolo = Station("Pozzuolo Martesana", "380", "S01722", 45.5065, 9.4583)
        val trecella = Station("Trecella", "2910", "S01706", 45.5121, 9.4896)
        val cassano = Station("Cassano d'Adda", "951", "S01707", 45.5242, 9.5165)
        val treviglio = Station("Treviglio", "2919", "S01708", 45.5201, 9.5932)

        // S1 West / East
        val caronno = Station("Caronno Pertusella", null, "S01076", 45.5983, 9.0432)
        val cesate = Station("Cesate", null, "S01075", 45.5812, 9.0621)
        val garbagnateM = Station("Garbagnate Milanese", null, "S01074", 45.5684, 9.0763)
        val garbagnateP = Station("Garbagnate Parco delle Groane", null, "S01073", 45.5562, 9.0883)
        val bollateN = Station("Bollate Nord", null, "S01072", 45.5451, 9.1021)
        val bollateC = Station("Bollate Centro", null, "S01071", 45.5342, 9.1162)
        val novate = Station("Novate Milanese", null, "S01070", 45.5262, 9.1301)
        val quartoOggiaro = Station("Milano Quarto Oggiaro", null, "S01069", 45.5121, 9.1412)
        val sanDonato = Station("San Donato Milanese", "2487", "S01624", 45.4183, 9.2562)
        val borgolombardo = Station("Borgolombardo", "710", "S01830", 45.4062, 9.2683)
        val sanGiuliano = Station("San Giuliano Milanese", "2520", "S01821", 45.3983, 9.2812)
        val melegnano = Station("Melegnano", "1688", "S01822", 45.3592, 9.3235)
        val tavazzano = Station("Tavazzano", "2820", "S01824", 45.3262, 9.3783)
        val lodi = Station("Lodi", "1584", "S01825", 45.2796, 9.4795)

        // S2 West
        val mariano = Station("Mariano Comense", null, "S01089", 45.6983, 9.1832)
        val cabiate = Station("Cabiate", null, "S01088", 45.6812, 9.1721)
        val meda = Station("Meda", null, "S01087", 45.6684, 9.1563)
        val seveso = Station("Seveso", null, "S01925", 45.6421, 9.1412)
        val cesano = Station("Cesano Maderno", null, "S01086", 45.6262, 9.1501)
        val bovisio = Station("Bovisio Masciago-Mombello", null, "S01085", 45.6062, 9.1521)
        val varedo = Station("Varedo", null, "S01084", 45.5983, 9.1583)
        val palazzolo = Station("Palazzolo Milanese", null, "S01083", 45.5862, 9.1621)
        val paderno = Station("Paderno Dugnano", null, "S01082", 45.5712, 9.1683)
        val cormano = Station("Cormano-Cusano Milanino", null, "S01109", 45.5451, 9.1783)
        val bruzzano = Station("Milano Bruzzano", null, "S01079", 45.5262, 9.1762)

        // S13 East
        val locate = Station("Locate Triulzi", "1583", "S01801", 45.3583, 9.2182)
        val pieve = Station("Pieve Emanuele", "1749", "S01104", 45.3421, 9.2062)
        val villamaggiore = Station("Villamaggiore", "3092", "S01802", 45.3212, 9.2021)
        val certosaPavia = Station("Certosa di Pavia", "1069", "S01803", 45.2562, 9.1583)
        val pavia = Station("Pavia", "2046", "S01860", 45.1868, 9.1625)

        // --- Flows ---
        val tunnelOvestBovisa = listOf(bovisa, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, rogoredo)
        val tunnelOvestCertosa = listOf(rhoFiera, certosa, villapizzone, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, forlanini)
        val ramoCadorna = listOf(bovisa, domodossola, cadorna)
        val cinturaS9 = listOf(saronno, greco, lambrate, forlanini, romana, tibaldi, romolo, cristoforo, albairate)
        val superficieS11 = listOf(greco, garibaldiSup, villapizzone, certosa, rhoFiera)

        val lineS1Stations = listOf(saronno, caronno, cesate, garbagnateM, garbagnateP, bollateN, bollateC, novate, quartoOggiaro, bovisa, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, rogoredo, sanDonato, borgolombardo, sanGiuliano, melegnano, tavazzano, lodi)
        val lineS2Stations = listOf(mariano, cabiate, meda, seveso, cesano, bovisio, varedo, palazzolo, paderno, cormano, bruzzano, bovisa, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, rogoredo)
        val lineS5Stations = listOf(varese, gazzada, castronno, albizzate, cavaria, gallarate, busto, legnano, canegrate, parabiago, vanzago, rho, rhoFiera, certosa, villapizzone, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, forlanini, segrate, pioltello, melzo, pozzuolo, trecella, cassano, treviglio)
        val lineS6Stations = listOf(novara, trecate, magenta, corbetta, vittuone, pregnana, rho, rhoFiera, certosa, villapizzone, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, forlanini, segrate, pioltello)
        val lineS12Stations = listOf(cormano, bruzzano, bovisa, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, rogoredo, sanDonato, borgolombardo, sanGiuliano, melegnano)
        val lineS13Stations = listOf(bovisa, lancetti, garibaldiPassante, repubblica, venezia, dateo, vittoria, rogoredo, locate, pieve, villamaggiore, certosaPavia, pavia)

        // --- Lines list ---
        allLines = listOf(
            SuburbanLine("S1", "S1 Saronno - Lodi", "#e30613", lineS1Stations),
            SuburbanLine("S2", "S2 Mariano - Rogoredo", "#009640", lineS2Stations),
            SuburbanLine("S5", "S5 Varese - Treviglio", "#f39200", lineS5Stations),
            SuburbanLine("S6", "S6 Novara - Pioltello", "#ffd60a", lineS6Stations),
            SuburbanLine("S11", "S11 Chiasso - Rho", "#8a8bbf", superficieS11),
            SuburbanLine("S12", "S12 Cormano - Melegnano", "#005a2b", lineS12Stations),
            SuburbanLine("S13", "S13 Bovisa - Pavia", "#a37a3e", lineS13Stations),
        )
    }
}
