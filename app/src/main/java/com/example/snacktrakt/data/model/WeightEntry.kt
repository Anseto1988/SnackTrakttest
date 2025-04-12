package com.example.snacktrakt.data.model

import java.util.Date

/**
 * Datenmodell für einen Gewichtseintrag eines Hundes
 *
 * Diese Klasse repräsentiert einen Gewichtseintrag zu einem bestimmten Zeitpunkt.
 * Sie wird für die Anzeige der Gewichtshistorie und Diagramme verwendet.
 *
 * @property id Eindeutige ID des Eintrags
 * @property dogId ID des Hundes, zu dem dieser Eintrag gehört
 * @property weight Gewicht in kg
 * @property date Datum der Eintragung
 * @property note Optionale Notiz zum Gewichtseintrag (z.B. "Nach Diät", "Tierarztbesuch")
 * @property teamId ID des Teams für Berechtigungen
 * @property ownerId ID des Erstellers des Gewichtseintrags für Berechtigungen
 */
data class WeightEntry(
    val id: String = "",
    val dogId: String,
    val weight: Double,
    val date: Date = Date(),
    val note: String? = null,
    val teamId: String? = null,
    val ownerId: String? = null
)
