package com.example.snacktrakt.data.repository

import android.util.Log
import com.example.snacktrakt.data.AppwriteConfig
import com.example.snacktrakt.data.model.Futter
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Repository für die Verwaltung von Futtereinträgen (Mahlzeiten).
 * Berücksichtigt jetzt teamId und ownerId (des Eintrag-Erstellers).
 */
class FutterRepository(private val client: Client) {
    private val databases = Databases(client)

    companion object {
        private const val TAG = "FutterRepository"
        const val FUTTER_COLLECTION_ID = "futter"
    }

    /**
     * Fügt einen neuen Futtereintrag hinzu und setzt Berechtigungen.
     *
     * @param name Name des Futtereintrags
     * @param calories Kalorien des Futtereintrags
     * @param dogId ID des Hundes
     * @param teamId ID des Teams, zu dem der Hund gehört
     * @param entryOwnerId ID des Benutzers, der diesen Eintrag erstellt
     * @return Der neue Futtereintrag
     * @throws Exception wenn das Erstellen fehlschlägt
     */
    suspend fun addFutterEntry(
        name: String,
        calories: Int,
        dogId: String,
        teamId: String,
        entryOwnerId: String // ++ NEU: ID des Eintrag-Erstellers ++
    ): Futter = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding futter for dog $dogId (Team: $teamId) by user $entryOwnerId: $name, $calories kcal")

        try {
            // Datum als ISO String
            val currentDate = Date()
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val dateString = isoFormatter.format(currentDate)

            // Daten für Appwrite
            val futterData = mapOf(
                "name" to name,
                "calories" to calories,
                "Date" to dateString, // Stelle sicher, dass Attribut "Date" heißt (Großschreibung!)
                "dogId" to dogId,
                "teamId" to teamId,
                "ownerId" to entryOwnerId // ++ NEU: ownerId des Eintrags setzen ++
            )

            // Berechtigungen für das Dokument
            val permissions = listOf(
                Permission.read(Role.team(teamId)),    // Teammitglieder dürfen lesen
                Permission.update(Role.user(entryOwnerId)), // Nur Ersteller darf ändern
                Permission.update(Role.team(teamId, "owner")), // ODER Team-Owner darf ändern
                Permission.delete(Role.user(entryOwnerId)), // Nur Ersteller darf löschen
                Permission.delete(Role.team(teamId, "owner"))  // ODER Team-Owner darf löschen
            )
            Log.d(TAG,"Creating futter document with data: $futterData and permissions: $permissions")

            val response = databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.FUTTER_COLLECTION_ID,
                documentId = ID.unique(),
                data = futterData,
                permissions = permissions
            )
            Log.d(TAG, "Added futter document with ID: ${response.id}")

            // Lokales Futter-Objekt zurückgeben
            return@withContext Futter(
                id = response.id,
                name = response.data["name"] as String,
                calories = (response.data["calories"] as Number).toInt(),
                date = currentDate,
                dogId = response.data["dogId"] as String
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding futter for dog $dogId by user $entryOwnerId", e)
            throw e
        }
    }

    /**
     * Ruft alle Futtereinträge für einen bestimmten Hund ab.
     * (Keine Änderung nötig, Berechtigungen regeln den Zugriff).
     */
    suspend fun getFutterByDogId(dogId: String): List<Futter> = withContext(Dispatchers.IO) {
        // Code bleibt gleich wie in der vorherigen Antwort (mit Paginierung etc.)
        Log.d(TAG, "Getting futter list for dog: $dogId from collection $FUTTER_COLLECTION_ID")
        try {
            val futterList = mutableListOf<Futter>()
            var offset = 0
            val limit = 100 // Paginierung

            while(true) {
                Log.d(TAG, "Fetching futter documents for dog $dogId with limit $limit, offset $offset")
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.FUTTER_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("dogId", dogId),
                        Query.limit(limit),
                        Query.offset(offset)
                        // Query.orderDesc("Date") // Optional serverseitig sortieren
                    )
                )
                val documents = response.documents
                Log.d(TAG, "Retrieved ${documents.size} futter documents in batch.")
                if (documents.isEmpty()) break

                documents.forEach { document ->
                    try {
                        // Verwende die robustere Parsing-Logik
                        val date = parseDateString(document.data["Date"] as? String)
                        val caloriesValue = document.data["calories"]
                        val calories : Int? = when(caloriesValue) {
                            is Number -> caloriesValue.toInt()
                            is String -> caloriesValue.toIntOrNull()
                            else -> null
                        }
                        if (date != null && calories != null) {
                            futterList.add(Futter(
                                id = document.id,
                                name = document.data["name"] as? String ?: "",
                                calories = calories,
                                date = date,
                                dogId = document.data["dogId"] as? String ?: ""
                            ))
                        } else {
                            Log.w(TAG, "Skipping futter document ${document.id} due to invalid date or calories.")
                        }
                    } catch (parseEx: Exception){
                        Log.e(TAG, "Error parsing futter document ${document.id}", parseEx)
                    }
                }
                if (documents.size < limit) break
                offset += limit
            }

            Log.d(TAG, "Total futter entries retrieved for dog $dogId: ${futterList.size}")
            // Sortiere absteigend nach Datum (neueste zuerst)
            return@withContext futterList.sortedByDescending { it.date }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting futter list for dog $dogId", e)
            return@withContext emptyList<Futter>()
        }
    }

    /**
     * Löscht Futtereinträge für einen bestimmten Hund (mit Paginierung).
     * (Keine Änderung nötig)
     */
    suspend fun deleteFutterForDog(dogId: String): Int = withContext(Dispatchers.IO) {
        // Code bleibt gleich wie in der vorherigen Antwort
        Log.d(TAG, "Deleting all futter entries for dog: $dogId")
        var deletedCount = 0
        try {
            var offset = 0
            val limit = 100
            while (true) {
                Log.d(TAG, "Fetching futter documents to delete for dog $dogId, offset $offset")
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.FUTTER_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("dogId", dogId),
                        Query.limit(limit),
                        Query.offset(offset),
                        Query.select(listOf("\$id")) // Nur IDs holen
                    )
                )
                val documents = response.documents
                if (documents.isEmpty()) break

                Log.d(TAG, "Found ${documents.size} futter document IDs to delete.")
                documents.forEach { doc ->
                    try {
                        databases.deleteDocument(
                            databaseId = AppwriteConfig.DATABASE_ID,
                            collectionId = AppwriteConfig.FUTTER_COLLECTION_ID,
                            documentId = doc.id
                        )
                        deletedCount++
                        Log.v(TAG, "Deleted futter entry: ${doc.id}")
                    } catch (delEx: Exception) {
                        Log.e(TAG, "Error deleting futter entry: ${doc.id}", delEx)
                    }
                }
                if (documents.size < limit) break
                offset += limit
            }
            Log.d(TAG, "Deleted $deletedCount futter entries for dog $dogId")
            return@withContext deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/deleting futter for dog $dogId", e)
            return@withContext deletedCount
        }
    }

    /**
     * Hilfsfunktion zum Parsen von Datums-Strings.
     */
    private fun parseDateString(dateString: String?): Date? {
        if (dateString == null) return null
        val possibleFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        )
        for (format in possibleFormats) {
            format.timeZone = TimeZone.getTimeZone("UTC")
            try { return format.parse(dateString) } catch (e: ParseException) {}
        }
        Log.w(TAG, "Could not parse date string: $dateString")
        return null
    }
}