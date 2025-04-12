package com.example.snacktrakt.data.model

import java.util.Date

/**
 * Datenmodell für einen Hund
 *
 * Enthält jetzt auch eine teamId für die Rechteverwaltung mit Appwrite Teams.
 */
data class Dog(
    val id: String = "",
    val name: String = "",
    val rasse: String = "",
    val alter: Int? = null,
    val geburtsdatum: Date? = null,
    val gewicht: Double? = null,
    val kalorienbedarf: Int? = null,
    val imageFileId: String? = null,
    val ownerId: String = "", // ID des ursprünglichen Besitzers
    val consumedCalories: Int = 0,
    val teamId: String? = null // ++ NEU: ID des Appwrite Teams für diesen Hund ++
)