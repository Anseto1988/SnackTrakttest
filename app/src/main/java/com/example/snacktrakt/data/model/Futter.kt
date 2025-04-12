package com.example.snacktrakt.data.model

import java.util.Date

/**
 * Datenmodell für einen Futtereintrag
 * 
 * Diese Klasse repräsentiert einen einzelnen Futtereintrag (Mahlzeit) für einen Hund.
 * Sie speichert Informationen über die Art des Futters, den Kaloriengehalt 
 * und den Zeitpunkt der Fütterung.
 * 
 * @property id Eindeutige ID des Futtereintrags in der Datenbank
 * @property name Bezeichnung des Futters (z.B. "Trockenfutter", "Leckerli")
 * @property calories Kalorien dieses Futtereintrags in kcal
 * @property date Datum und Uhrzeit der Fütterung
 * @property dogId ID des Hundes, dem dieses Futter gegeben wurde
 * @property teamId ID des Teams für Berechtigungen
 * @property ownerId ID des Erstellers des Eintrags für Berechtigungen
 */
data class Futter(
    val id: String = "",
    val name: String = "",
    val calories: Int = 0,
    val date: Date = Date(),
    val dogId: String = "",
    val teamId: String? = null,
    val ownerId: String? = null
)
