package com.example.snacktrakt.data.repository

import android.util.Log
import com.example.snacktrakt.data.model.Dog
import io.appwrite.Client
import io.appwrite.Query // Import hinzugefügt, falls benötigt (war nicht im Original)
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Repository für die Statistiken von Hunden
 *
 * Diese Klasse kapselt Zugriffe auf Hundestatistiken wie:
 * - Abrufen von Kalorieninformationen
 * - Aktualisieren von konsumierten Kalorien
 *
 * @param client Appwrite Client für API-Zugriffe
 */
class DogStatsRepository(private val client: Client) {
    private val databases = Databases(client)

    companion object {
        const val DATABASE_ID = "67d175de002e0cb4c394"  // Hunde DB ID (korrekt)

        // ++ KORRIGIERTE COLLECTION ID ++
        // Verwende die gleiche ID wie in DogDataRepository
        const val DOG_COLLECTION_ID = "67d1761c00166b4b2b85"  // Collection Name: "dogs" (Annahme)
        // const val DOG_COLLECTION_ID = "67d176af002cb83b6a88"  // << ALTE, WAHRSCHEINLICH FALSCHE ID

        private const val TAG = "DogStatsRepository"
    }

    /**
     * Ruft einen Hund anhand seiner ID ab
     *
     * @param dogId ID des Hundes
     * @return Dog-Objekt oder wirft eine Exception, wenn nicht gefunden oder Fehler auftritt
     * @throws io.appwrite.exceptions.AppwriteException wenn der Hund oder die Collection nicht gefunden wird
     */
    suspend fun getDogById(dogId: String): Dog = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting dog with ID: $dogId from DB $DATABASE_ID, Collection $DOG_COLLECTION_ID")

        try {
            val response = databases.getDocument(
                databaseId = DATABASE_ID,
                collectionId = DOG_COLLECTION_ID, // ++ Verwendet jetzt die korrigierte ID ++
                documentId = dogId
            )

            val dogName = response.data["name"] as? String ?: "Unbekannt"
            Log.d(TAG, "Retrieved dog: $dogName")

            // Parse das Geburtsdatum (falls vorhanden) - Verwende Hilfsfunktion
            val geburtsdatum = parseDateString(response.data["geburtsdatum"] as? String)

            // Helper function to safely get Int/Double values, handling various possible types
            fun getNumberValue(key: String): Number? = response.data[key] as? Number
            fun getIntValue(key: String): Int? = getNumberValue(key)?.toInt()
            fun getDoubleValue(key: String): Double? = getNumberValue(key)?.toDouble()

            // Erstelle das Dog-Objekt mit sichereren Zugriffen
            return@withContext Dog(
                id = response.id,
                name = response.data["name"] as? String ?: "",
                rasse = response.data["rasse"] as? String ?: "",
                alter = getIntValue("alter"), // Kann null sein
                geburtsdatum = geburtsdatum, // Kann null sein
                gewicht = getDoubleValue("gewicht"), // Kann null sein
                kalorienbedarf = getIntValue("kalorienbedarf"), // Kann null sein
                imageFileId = response.data["imageFileId"] as? String, // Kann null sein
                ownerId = response.data["ownerId"] as? String ?: "", // Sollte nicht null sein
                // Stelle sicher, dass consumedCalories existiert und ein numerischer Typ ist
                consumedCalories = getIntValue("consumedCalories") ?: 0 // Standard 0, falls fehlt/null
            )
        } catch (e: Exception) {
            // Logge den spezifischen Fehler und werfe ihn weiter
            Log.e(TAG, "Error getting dog with ID $dogId", e)
            throw e // Wichtig, damit aufrufende Funktion den Fehler bemerkt
        }
    }

    /**
     * Aktualisiert die konsumierten Kalorien eines Hundes
     *
     * @param dogId ID des Hundes
     * @param calories Neue Gesamtkalorienmenge
     * @return true bei Erfolg, false bei Misserfolg
     */
    suspend fun updateDogCalories(dogId: String, calories: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating consumedCalories for dog $dogId to $calories in collection $DOG_COLLECTION_ID")

        try {
            // Stelle sicher, dass das Attribut in Appwrite 'consumedCalories' heißt
            val updateData = mapOf("consumedCalories" to calories)

            databases.updateDocument(
                databaseId = DATABASE_ID,
                collectionId = DOG_COLLECTION_ID, // ++ Verwendet jetzt die korrigierte ID ++
                documentId = dogId,
                data = updateData
            )

            Log.d(TAG, "Successfully updated consumedCalories for dog $dogId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating consumedCalories for dog $dogId", e)
            return@withContext false // Gib false zurück, um Fehler anzuzeigen
        }
    }

    /**
     * Berechnet die verbleibenden Kalorien für einen Hund
     *
     * @param dogId ID des Hundes
     * @return Verbleibende Kalorien oder null wenn kein Kalorienbedarf definiert ist.
     * @throws Exception wenn Hund nicht geladen werden kann.
     */
    suspend fun getRemainingCalories(dogId: String): Int? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Calculating remaining calories for dog: $dogId")

        try {
            // Hole die Hundedaten (wirft Exception bei Fehler)
            val dog = getDogById(dogId)

            // Sichere Verarbeitung des Kalorienbedarfs
            val kalorienbedarf = dog.kalorienbedarf ?: return@withContext null // Wenn Bedarf null, gib null zurück

            if (kalorienbedarf <= 0) {
                Log.w(TAG, "Dog ${dog.name} has no valid calorie requirement defined ($kalorienbedarf)")
                return@withContext 0 // Kein Bedarf definiert oder ungültig -> 0 verbleibend
            }

            // Berechne verbleibende Kalorien
            val remaining = kalorienbedarf - dog.consumedCalories
            Log.d(TAG, "Dog ${dog.name}: Required=$kalorienbedarf, Consumed=${dog.consumedCalories}, Remaining=$remaining")

            // Verbleibend kann nicht negativ sein
            return@withContext maxOf(0, remaining)

        } catch (e: Exception) {
            // Fehler beim Laden des Hundes weitergeben
            Log.e(TAG, "Error calculating remaining calories because dog couldn't be loaded", e)
            throw e
        }
    }

    /**
     * Setzt die konsumierten Kalorien eines Hundes auf 0 zurück.
     * Nützlich für tägliche Zurücksetzungen.
     *
     * @param dogId ID des Hundes
     * @return Erfolg oder Misserfolg
     */
    suspend fun resetCaloriesForDog(dogId: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Resetting consumedCalories for dog: $dogId")
        // Ruft die bereits existierende Update-Funktion auf
        return@withContext updateDogCalories(dogId, 0)
    }

    /**
     * Ruft alle Hunde eines Besitzers ab.
     *
     * @param ownerId ID des Besitzers
     * @return Liste von Dog-Objekten. Gibt eine leere Liste bei Fehlern zurück.
     */
    suspend fun getDogsByOwnerId(ownerId: String): List<Dog> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting dogs for owner: $ownerId from collection $DOG_COLLECTION_ID")

        try {
            val dogsList = mutableListOf<Dog>()
            var offset = 0
            val limit = 100 // Paginierung

            while(true){
                Log.d(TAG, "Fetching dogs for owner $ownerId with limit $limit, offset $offset")
                val response = databases.listDocuments(
                    databaseId = DATABASE_ID,
                    collectionId = DOG_COLLECTION_ID, // ++ Verwendet jetzt die korrigierte ID ++
                    queries = listOf(
                        Query.equal("ownerId", ownerId),
                        Query.limit(limit),
                        Query.offset(offset)
                        // Optional: Sortierung Query.orderAsc("name")
                    )
                )
                val documents = response.documents
                Log.d(TAG, "Retrieved ${documents.size} dog documents in this batch.")

                if (documents.isEmpty()) break

                documents.forEach { document ->
                    try {
                        val geburtsdatum = parseDateString(document.data["geburtsdatum"] as? String)
                        fun getNumberValue(key: String): Number? = document.data[key] as? Number
                        fun getIntValue(key: String): Int? = getNumberValue(key)?.toInt()
                        fun getDoubleValue(key: String): Double? = getNumberValue(key)?.toDouble()

                        dogsList.add(
                            Dog(
                                id = document.id,
                                name = document.data["name"] as? String ?: "",
                                rasse = document.data["rasse"] as? String ?: "",
                                alter = getIntValue("alter"),
                                geburtsdatum = geburtsdatum,
                                gewicht = getDoubleValue("gewicht"),
                                kalorienbedarf = getIntValue("kalorienbedarf"),
                                imageFileId = document.data["imageFileId"] as? String,
                                ownerId = document.data["ownerId"] as? String ?: "",
                                consumedCalories = getIntValue("consumedCalories") ?: 0
                            )
                        )
                    } catch (parseError: Exception){
                        Log.e(TAG, "Error parsing dog document ${document.id}", parseError)
                    }
                }

                if (documents.size < limit) break
                offset += limit
            }

            Log.d(TAG, "Total dogs retrieved for owner $ownerId: ${dogsList.size}")
            return@withContext dogsList

        } catch (e: Exception) {
            Log.e(TAG, "Error getting dogs for owner $ownerId", e)
            return@withContext emptyList<Dog>() // Leere Liste bei Fehler
        }
    }

    /**
     * Hilfsfunktion zum Parsen verschiedener Datumsformate von Appwrite.
     */
    private fun parseDateString(dateString: String?): Date? {
        if (dateString == null) return null

        // Liste der zu prüfenden Formate
        val possibleFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US), // Primäres ISO 8601 mit Zone
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),     // Ohne Millis, mit Zone
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US), // Mit Millis, Z
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)      // Ohne Millis, Z
            // Füge bei Bedarf weitere Formate hinzu
        )

        for (format in possibleFormats) {
            format.timeZone = TimeZone.getTimeZone("UTC") // Wichtig: Appwrite speichert meist in UTC
            try {
                return format.parse(dateString) // Gib das erste erfolgreiche Ergebnis zurück
            } catch (e: ParseException) {
                // Ignoriere und versuche das nächste Format
            }
        }

        Log.w(TAG, "Could not parse date string: $dateString with any known ISO format.")
        return null // Konnte mit keinem Format geparst werden
    }
}