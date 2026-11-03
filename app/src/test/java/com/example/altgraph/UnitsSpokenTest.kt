package com.example.altgraph

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsSpokenTest {
    private fun en(p: String) = Units.convertSpoken(p, listOf("kilometers", "kilometer"), listOf("meters", "meter"),
        "mile", "miles", "foot", "feet", "miles per hour", Locale.US)
    private fun es(p: String) = Units.convertSpoken(p, listOf("kilómetros", "kilómetro"), listOf("metros", "metro"),
        "milla", "millas", "pie", "pies", "millas por hora", Locale("es"))
    private fun de(p: String) = Units.convertSpoken(p, listOf("Kilometer"), listOf("Meter"),
        "Meile", "Meilen", "Fuß", "Fuß", "Meilen pro Stunde", Locale.GERMANY)
    private fun fr(p: String) = Units.convertSpoken(p, listOf("kilomètres", "kilomètre"), listOf("mètres", "mètre"),
        "mile", "miles", "pied", "pieds", "miles par heure", Locale.FRANCE)

    @Test fun englishDistancesAndElevation() {
        assertEquals("Summit in 1650 feet.", en("Summit in 500 meters."))
        assertEquals("Summit in 330 feet.", en("Summit in 100 meters."))
        assertEquals("Approaching climb X. 3.1 miles at 7 percent average.", en("Approaching climb X. 5 kilometers at 7 percent average."))
        assertEquals("Elevation gained: 820 feet. Current altitude: 1650 feet.", en("Elevation gained: 250 meters. Current altitude: 500 meters."))
    }

    @Test fun englishSpeedAndVam() {
        assertEquals("Average speed 15.5 miles per hour.", en("Average speed 25 km/h."))
        assertEquals("VAM 1650 feet per hour.", en("VAM 500 meters per hour."))
    }

    @Test fun spanishKeepsTheTailOfTheUnit() {
        assertEquals("Velocidad media 15,5 millas por hora.", es("Velocidad media 25 kilómetros por hora."))
        assertEquals("Cima a 330 pies.", es("Cima a 100 metros."))
        assertEquals("Distancia 1 milla.", es("Distancia 1,6 kilómetros."))
    }

    @Test fun germanAndFrench() {
        assertEquals("Durchschnittsgeschwindigkeit 15,5 Meilen pro Stunde.", de("Durchschnittsgeschwindigkeit 25 km/h."))
        assertEquals("VAM 1650 Fuß pro Stunde.", de("VAM 500 Höhenmeter pro Stunde."))
        assertEquals("Vitesse moyenne 15,5 miles par heure.", fr("Vitesse moyenne 25 km/h."))
        assertEquals("Sommet à 330 pieds.", fr("Sommet à 100 mètres."))
    }

    @Test fun panelTextsAreConverted() {
        assertEquals("- 160 ft/h", Units.localizeImperial("- 50 m/h"))
        assertEquals("+ 160 ft/h", Units.localizeImperial("+ 50 m/h"))
        assertEquals("Target VAM (ft/h)", Units.localizeImperial("Target VAM (m/h)"))
        assertEquals("Flat roads are not exaggerated below 100 ft.", Units.localizeImperial("Flat roads are not exaggerated below 30 m."))
        assertEquals("a climb is about to start (1310 ft before)", Units.localizeImperial("a climb is about to start (400 m before)"))
        assertEquals("Announces every 820ft or 1640ft of gained elevation", Units.localizeImperial("Announces every 250m or 500m of gained elevation"))
        assertEquals("Block Resolution (ft / mi)", Units.localizeImperial("Block Resolution (Meters)"))
        assertEquals("Avisa cada 5 mi", Units.localizeImperial("Avisa cada 8 km"))
    }

    @Test fun numbersWithoutUnitAreUntouched() {
        assertEquals("Climb kilometer 2. Current grade 8 percent.", en("Climb kilometer 2. Current grade 8 percent."))
        assertEquals("Wall of 12 percent in 490 feet.", en("Wall of 12 percent in 150 meters."))
    }
}
