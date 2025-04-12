package com.example.snacktrakt.data.repository

import android.content.Context
import android.util.Log
import com.example.snacktrakt.data.AppwriteConfig
import com.example.snacktrakt.data.model.WeightEntry
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Repository für die Verwaltung von Gewichtseintragungen.
 * Berücksichtigt jetzt teamId und ownerId (des Eintrag-Erstellers).
 */
class WeightRepository(
    private val client: Client,
    private val context: Context
) {
    private val databases = Databases(client)

    companion object {
        private const val TAG = "WeightRepository"
        const val WEIGHT_COLLECTION_ID = "weight_history"
    }

    /**
     * Fügt einen neuen Gewichtseintrag hinzu und setzt Berechtigungen.
     *
     * @param dogId ID des Hundes
     * @param weight Neues Gewicht in kg
     * @param teamId ID des Teams, zu dem der Hund gehört
     * @param entryOwnerId ID des Benutzers, der diesen Eintrag erstellt
     * @param date Datum der Messung (standardmäßig aktuelles Datum)
     * @param note Optionale Notiz zum Gewichtseintrag
     * @return Der erstellte Gewichtseintrag
     * @throws Exception wenn das Erstellen fehlschlägt
     */
    suspend fun addWeightEntry(
        dogId: String,
        weight: Double,
        teamId: String,
        entryOwnerId: String, // ++ NEU: ID des Eintrag-Erstellers ++
        date: Date = Date(),
        note: String? = null
    ): WeightEntry = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding weight entry for dog $dogId (Team: $teamId) by user $entryOwnerId: $weight kg, Date: $date, Note: $note")

        try {
            // Datum in ISO String konvertieren
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val dateString = isoFormatter.format(date)

            // Daten für Appwrite
            val weightData = mapOf(
                "dogId" to dogId,
                "weight" to weight,
                "date" to dateString, // Sicherstellen, dass Attribut 'date' heißt
                "note" to (note ?: ""),
                "teamId" to teamId,
                "ownerId" to entryOwnerId // ++ NEU: ownerId des Eintrags ++
            )

            // Berechtigungen
            val permissions = listOf(
                Permission.read(Role.team(teamId)),
                Permission.update(Role.user(entryOwnerId)), // Nur Ersteller darf ändern
                Permission.update(Role.team(teamId, "owner")), // ODER Team-Owner darf ändern
                Permission.delete(Role.user(entryOwnerId)), // Nur Ersteller darf löschen
                Permission.delete(Role.team(teamId, "owner"))  // ODER Team-Owner darf löschen
            )
            Log.d(TAG,"Creating weight document in collection ${AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID} with data: $weightData and permissions: $permissions")

            val response = databases.createDocument(
                databaseId = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID,
                documentId = ID.unique(),
                data = weightData,
                permissions = permissions
            )

            Log.d(TAG, "Added weight entry document with ID: ${response.id}")

            // Lokales Objekt zurückgeben
            return@withContext WeightEntry(
                id = response.id,
                dogId = dogId,
                weight = weight,
                date = date,
                note = note
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding weight entry for dog $dogId by user $entryOwnerId", e)
            throw e
        }
    }

    // --- getWeightEntriesForDog ---
    // (Code bleibt gleich wie in der vorherigen Antwort, mit Paginierung und robustem Parsing)
    suspend fun getWeightEntriesForDog(dogId: String): List<WeightEntry> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting ALL weight entries for dog: $dogId from collection $WEIGHT_COLLECTION_ID")
        try {
            val allEntries = mutableListOf<WeightEntry>()
            var offset = 0
            val limit = 100

            while(true) {
                Log.d(TAG, "Fetching weight documents for dog $dogId with limit $limit, offset $offset")
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("dogId", dogId),
                        Query.limit(limit),
                        Query.offset(offset)
                    )
                )
                val documents = response.documents
                Log.d(TAG, "Retrieved ${documents.size} weight documents in batch.")
                if (documents.isEmpty()) break

                val parsedEntries = documents.mapNotNull { document ->
                    parseWeightDocument(document.id, document.data)
                }
                allEntries.addAll(parsedEntries)

                if (documents.size < limit) break
                offset += limit
            }
            Log.d(TAG, "Total weight entries retrieved for dog $dogId: ${allEntries.size}")
            return@withContext allEntries.sortedBy { it.date } // Aufsteigend nach Datum

        } catch (e: Exception) {
            Log.e(TAG, "Error getting weight entries for dog $dogId", e)
            return@withContext emptyList<WeightEntry>()
        }
    }


    // --- getWeightEntriesForTimeRange ---
    // (Code bleibt gleich wie in der vorherigen Antwort, mit DB-Filterung und Paginierung)
    suspend fun getWeightEntriesForTimeRange(
        dogId: String,
        timeRange: TimeRange
    ): List<WeightEntry> = withContext(Dispatchers.IO) {
        if (timeRange == TimeRange.ALL) {
            return@withContext getWeightEntriesForDog(dogId)
        }

        Log.d(TAG, "Getting weight entries for dog $dogId in time range: $timeRange using DB query.")
        try {
            // Daten für Zeitbereich berechnen
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_YEAR, -timeRange.days)
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startDate = calendar.time

            // ISO Strings für Query
            val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val startDateString = isoFormatter.format(startDate)
            val endDateString = isoFormatter.format(endDate)
            Log.d(TAG, "Filtering using ISO dates: $startDateString to $endDateString")

            // Abfrage mit Paginierung
            val allFilteredEntries = mutableListOf<WeightEntry>()
            var offset = 0
            val limit = 100
            while(true){
                Log.d(TAG, "Fetching filtered weight documents for $dogId, offset $offset")
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("dogId", dogId),
                        Query.greaterThanEqual("date", startDateString),
                        Query.lessThanEqual("date", endDateString),
                        Query.limit(limit),
                        Query.offset(offset)
                    )
                )
                val documents = response.documents
                Log.d(TAG, "Retrieved ${documents.size} filtered documents in batch.")
                if(documents.isEmpty()) break

                val parsedEntries = documents.mapNotNull { document ->
                    parseWeightDocument(document.id, document.data)
                }
                allFilteredEntries.addAll(parsedEntries)

                if (documents.size < limit) break
                offset += limit
            }

            Log.d(TAG, "Total filtered weight entries retrieved for dog $dogId: ${allFilteredEntries.size}")
            return@withContext allFilteredEntries.sortedBy { it.date } // Sortiere aufsteigend

        } catch (e: Exception) {
            Log.e(TAG, "Error filtering weight entries for dog $dogId, time range $timeRange", e)
            return@withContext emptyList<WeightEntry>()
        }
    }

    // --- parseWeightDocument ---
    // (Code bleibt gleich wie in der vorherigen Antwort)
    private fun parseWeightDocument(docId: String, data: Map<String, Any?>): WeightEntry? {
        try {
            val date = parseDateString(data["date"] as? String)
            val weightValue = data["weight"]
            val weight: Double? = when (weightValue) {
                is Number -> weightValue.toDouble()
                is String -> weightValue.toDoubleOrNull()
                else -> null
            }
            val dogIdValue = data["dogId"] as? String

            if (date != null && weight != null && dogIdValue != null) {
                return WeightEntry(
                    id = docId,
                    dogId = dogIdValue,
                    weight = weight,
                    date = date,
                    note = data["note"] as? String
                )
            } else {
                Log.e(TAG, "Failed to parse required fields for weight document $docId.")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight document $docId", e)
            return null
        }
    }

    // --- parseDateString ---
    // (Code bleibt gleich wie in der vorherigen Antwort)
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

    // --- deleteWeightEntriesForDog ---
    // (Code bleibt gleich wie in der vorherigen Antwort, mit Paginierung)
    suspend fun deleteWeightEntriesForDog(dogId: String): Int = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting ALL weight entries for dog: $dogId")
        var deletedCount = 0
        try {
            var offset = 0
            val limit = 100
            while (true) {
                Log.d(TAG, "Fetching weight documents to delete for dog $dogId, offset $offset")
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID,
                    queries = listOf(
                        Query.equal("dogId", dogId),
                        Query.limit(limit),
                        Query.offset(offset),
                        Query.select(listOf("\$id"))
                    )
                )
                val documents = response.documents
                if (documents.isEmpty()) break

                Log.d(TAG, "Found ${documents.size} weight document IDs to delete.")
                documents.forEach { doc ->
                    try {
                        databases.deleteDocument(
                            databaseId = AppwriteConfig.DATABASE_ID,
                            collectionId = AppwriteConfig.WEIGHT_HISTORY_COLLECTION_ID,
                            documentId = doc.id
                        )
                        deletedCount++
                        Log.v(TAG, "Deleted weight entry: ${doc.id}")
                    } catch (delEx: Exception) {
                        Log.e(TAG, "Error deleting weight entry: ${doc.id}", delEx)
                    }
                }
                if (documents.size < limit) break
                offset += limit
            }
            Log.d(TAG, "Deleted $deletedCount weight entries for dog $dogId")
            return@withContext deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching/deleting weight for dog $dogId", e)
            return@withContext deletedCount
        }
    }


    /**
     * Zeitintervalle für die Filterung von Gewichtsdaten.
     */
    enum class TimeRange(val days: Int, val displayName: String) {
        WEEK(7, "1 Woche"),
        MONTH(30, "1 Monat"),
        THREE_MONTHS(90, "3 Monate"),
        YEAR(365, "1 Jahr"),
        ALL(-1, "Alle Zeiten")
    }
}