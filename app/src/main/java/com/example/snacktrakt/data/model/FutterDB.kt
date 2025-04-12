package com.example.snacktrakt.data.model

import io.appwrite.models.Document

/**
 * Datenmodell für einen Futter-Eintrag aus der Futter-Datenbank
 */
data class FutterDB(
    val id: String = "",
    val hersteller: String = "",
    val kategorie: String = "",
    val unterkategorie: String = "",
    val produktname: String = "",
    val ean: String = "",
    val lebensphase: String = "",
    val kcalPro100g: Int? = null,
    val protein: Int? = null,
    val fett: Int? = null,
    val rohasche: Int? = null,
    val rohfaser: Int? = null,
    val feuchtigkeit: Int? = null,
    val nfe: Int? = null
)

/**
 * Datenmodell für eine Lebensphase
 */
data class Lebensphase(
    val id: String = "",
    val name: String = ""
)

/**
 * Datenmodell für eine Kategorie
 */
data class Kategorie(
    val id: String = "",
    val name: String = ""
)

/**
 * Datenmodell für eine Unterkategorie
 */
data class Unterkategorie(
    val id: String = "",
    val name: String = ""
)

/**
 * Datenmodell für einen Hersteller
 */
data class Hersteller(
    val id: String = "",
    val name: String = ""
) {
    companion object {
        fun fromDocument(document: Document<Map<String, Any>>): Hersteller {
            return Hersteller(
                id = document.id,
                name = document.data["Name"] as String
            )
        }
    }
}

/**
 * Datenmodell für eine Einreichung eines neuen Futterprodukts
 */
data class Einreichung(
    val id: String = "",
    val hersteller: String,
    val kategorie: String,
    val unterkategorie: String,
    val produktname: String,
    val ean: String,
    val lebensphase: String,
    val kcalPro100g: Int,
    val protein: Int,
    val fett: Int,
    val rohasche: Int,
    val rohfaser: Int,
    val feuchtigkeit: Int,
    val nfe: Int
)
