package com.example.snacktrakt.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.snacktrakt.data.AppwriteConfig
import com.example.snacktrakt.data.AuthRepository
import com.example.snacktrakt.data.model.Dog
import io.appwrite.Client
import io.appwrite.ID // Import für ID
import io.appwrite.Permission // Import für Permission
import io.appwrite.Role // Import für Role
import io.appwrite.exceptions.AppwriteException
// Queries Import (wird für Datenbank-Anfragen benötigt)
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DogDataRepository @Inject constructor(
    private val client: Client,
    private val imageRepository: ImageRepository,
    private val authRepository: AuthRepository
) {

    private val databases = Databases(client)
    // ISO 8601 Format für Appwrite 'datetime'
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    // Funktion zum Erstellen eines neuen Hundes mit optionalem Bild
    suspend fun createDog(
        name: String,
        rasse: String,
        alter: Int? = null,
        geburtsdatum: Date? = null,
        gewicht: Double? = null,
        kalorienbedarf: Int? = null,
        ownerId: String,
        imageUri: Uri? = null,
        context: Context
    ): Dog {
        var imageFileId: String? = null
        
        // Wenn ein Bild vorhanden ist, lade es hoch
        if (imageUri != null) {
            try {
                // Verwende den ImageRepository um das Bild hochzuladen
                imageFileId = imageRepository.uploadImage(imageUri, context = context)
                Log.d("DogDataRepository", "Bild erfolgreich hochgeladen mit ID: $imageFileId")
            } catch (e: Exception) {
                Log.e("DogDataRepository", "Fehler beim Hochladen des Bildes: ${e.message}")
                // Kein Fehler werfen, nur loggen und ohne Bild fortfahren
            }
        }
        
        // Erstelle ein Team für den Hund, falls der Besitzer noch keins hat
        val teamId = createOrGetTeam(ownerId)
        
        // Erstelle den Hund in der Datenbank
        val dog = addDog(
            name = name,
            rasse = rasse,
            alter = alter,
            geburtsdatum = geburtsdatum,
            gewicht = gewicht,
            kalorienbedarf = kalorienbedarf,
            ownerId = ownerId,
            teamId = teamId,
            imageFileId = imageFileId
        )
        
        return dog ?: throw RuntimeException("Failed to create dog")
    }
    
    // Hilfsfunktion zum Erstellen oder Abrufen eines Teams für den Benutzer
    private suspend fun createOrGetTeam(ownerId: String): String {
        return try {
            withContext(Dispatchers.IO) {
                // Versuche, ein vorhandenes Team für den Benutzer zu finden
                val teams = io.appwrite.services.Teams(client)
                val userTeams = teams.list()
                
                if (userTeams.teams.isNotEmpty()) {
                    // Verwende das erste Team des Benutzers, falls vorhanden
                    val teamId = userTeams.teams.first().id
                    Log.d("DogDataRepository", "Vorhandenes Team gefunden: $teamId")
                    return@withContext teamId
                } else {
                    // Erstelle ein neues Team für den Benutzer
                    val newTeam = teams.create(
                        teamId = io.appwrite.ID.unique(),
                        name = "Hunde Team von $ownerId",
                        roles = listOf("owner")
                    )
                    Log.d("DogDataRepository", "Neues Team erstellt: ${newTeam.id}")
                    return@withContext newTeam.id
                }
            }
        } catch (e: Exception) {
            // Im Fehlerfall erstellen wir einen eindeutigen Team-ID, damit der Rest der App weiter funktioniert
            Log.e("DogDataRepository", "Fehler beim Abrufen/Erstellen des Teams: ${e.message}")
            "team-${ownerId}-${System.currentTimeMillis()}"
        }
    }

    // Funktion zum Hinzufügen eines neuen Hundes zur Datenbank
    suspend fun addDog(
        name: String,
        rasse: String,
        alter: Int? = null,
        geburtsdatum: Date? = null, // Jetzt Date?
        gewicht: Double? = null,
        kalorienbedarf: Int? = null,
        ownerId: String, // Wichtig für Daten und Berechtigungen
        teamId: String, // Hinzugefügt: Wird für Daten und Berechtigungen benötigt
        imageFileId: String? = null // Optional
    ): Dog? { // Gibt das erstellte Dog-Objekt oder null bei Fehler zurück
        return try {
            withContext(Dispatchers.IO) {
                // Datenobjekt für Appwrite erstellen
                val dogData = mutableMapOf<String, Any?>(
                    "name" to name,
                    "rasse" to rasse,
                    "alter" to alter,
                    // Datum in ISO 8601 String umwandeln, falls vorhanden
                    "geburtsdatum" to geburtsdatum?.let { isoDateFormat.format(it) },
                    "gewicht" to gewicht,
                    "kalorienbedarf" to kalorienbedarf,
                    "ownerId" to ownerId, // ID des Besitzers speichern
                    "teamId" to teamId, // Team-ID speichern (NEU)
                    "consumedCalories" to 0 // Initialwert für verbrauchte Kalorien (NEU)
                )
                // Bild-ID nur hinzufügen, wenn vorhanden
                imageFileId?.let { dogData["imageFileId"] = it }

                // Berechtigungen definieren basierend auf dem DB-Schema
                val permissions = listOf(
                    // Der Besitzer (ownerId) darf alles
                    Permission.read(Role.user(ownerId)),
                    Permission.update(Role.user(ownerId)),
                    Permission.delete(Role.user(ownerId)),
                    // Das Team (teamId) darf lesen (gemäß der Schema-Beschreibung)
                    Permission.read(Role.team(teamId)),
                    Permission.update(Role.team(teamId)) // Team darf auch updaten (z.B. Gewicht)
                )

                Log.d("DogDataRepository", "Versuche Hund zu erstellen mit Daten: $dogData und Berechtigungen: $permissions")
                val documentId = ID.unique()
                // Dokument in Appwrite erstellen
                val document = databases.createDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = documentId, // Eindeutige ID verwenden
                    data = dogData,
                    permissions = permissions // Berechtigungen übergeben (NEU)
                )
                Log.i("DogDataRepository", "Hund erfolgreich erstellt mit ID: ${document.id}")
                // Erstelltes Dog-Objekt zurückgeben
                mapDocumentToDog(document)
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Hinzufügen des Hundes: ${e.message} (Code: ${e.code}, Type: ${e.type})")
            Log.e("DogDataRepository", "Request Details: ${e.response}") // Zeigt mehr Details zum Fehler
            null // Fehler
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Allgemeiner Fehler beim Hinzufügen des Hundes: ${e.message}")
            null // Allgemeiner Fehler
        }
    }


    // Funktion zum Abrufen aller Hunde, auf die der Benutzer Zugriff hat (Owner oder Teammitglied)
    suspend fun getAccessibleDogs(userId: String): List<Dog> {
        return try {
            withContext(Dispatchers.IO) {
                // Da Berechtigungen auf Dokument-Ebene geprüft werden, reicht listDocuments()
                // Appwrite filtert automatisch basierend auf den Leseberechtigungen des Benutzers.
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID
                    // Keine expliziten Queries für ownerId/teamId nötig, da Permissions greifen
                )
                Log.d("DogDataRepository", "Found ${response.total} dogs accessible by user $userId")
                response.documents.mapNotNull { document -> mapDocumentToDog(document) }
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Abrufen der zugreifbaren Hunde für User $userId: ${e.message}")
            emptyList() // Leere Liste bei Fehler zurückgeben
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Allgemeiner Fehler beim Abrufen der zugreifbaren Hunde: ${e.message}")
            emptyList()
        }
    }

    // Funktion zum Abrufen eines spezifischen Hundes anhand seiner ID
    suspend fun getDogById(dogId: String): Dog {
        return try {
            withContext(Dispatchers.IO) {
                Log.d("DogDataRepository", "Fetching dog with ID: $dogId")
                val document = databases.getDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = dogId
                )
                mapDocumentToDog(document) ?: throw AppwriteException("Failed to map document to Dog", 500, "mapping_error", null)
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Abrufen des Hundes (ID: $dogId): ${e.message}")
            // Wirf die Exception weiter, damit der Aufrufer darauf reagieren kann
            throw e
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Allgemeiner Fehler beim Abrufen des Hundes (ID: $dogId): ${e.message}")
            throw RuntimeException("Failed to get dog $dogId due to an unexpected error", e)
        }
    }


    // Funktion zum Abrufen aller Hunde für den aktuellen Benutzer (oder Team)
    suspend fun getDogs(userId: String): List<Dog> {
        return try {
            withContext(Dispatchers.IO) {
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID
                    // Hier könnten Queries hinzugefügt werden, z.B. um nur Hunde des Benutzers zu holen
                    // queries = listOf(Query.equal("ownerId", userId))
                )
                response.documents.mapNotNull { document ->
                    try {
                        val data = document.data
                        val birthDateString = data["geburtsdatum"] as? String
                        val birthDate = birthDateString?.let {
                            try { isoDateFormat.parse(it) } catch (e: Exception) { null }
                        }

                        Dog(
                            id = document.id,
                            name = data["name"] as? String ?: "",
                            rasse = data["rasse"] as? String ?: "",
                            alter = (data["alter"] as? Number)?.toInt(),
                            geburtsdatum = birthDate, // Gemapptes Date-Objekt
                            gewicht = (data["gewicht"] as? Number)?.toDouble(),
                            kalorienbedarf = (data["kalorienbedarf"] as? Number)?.toInt(),
                            imageFileId = data["imageFileId"] as? String,
                            ownerId = data["ownerId"] as? String ?: "",
                            // Default auf 0, falls das Feld fehlt (für ältere Einträge)
                            consumedCalories = (data["consumedCalories"] as? Number)?.toInt() ?: 0,
                            // TeamId auch auslesen, Default auf null, falls fehlt
                            teamId = data["teamId"] as? String
                        )
                    } catch (e: Exception) {
                        Log.e("DogDataRepository", "Fehler beim Parsen eines Dog-Dokuments (ID: ${document.id}): ${e.message}")
                        null // Überspringt fehlerhafte Dokumente
                    }
                }
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Abrufen der Hunde: ${e.message}")
            emptyList() // Leere Liste bei Fehler zurückgeben
        }
    }

    // Funktion zum Aktualisieren eines Hundes (nimmt jetzt ein Dog-Objekt)
    suspend fun updateDog(dog: Dog): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val dogData = mapOf(
                    "name" to dog.name,
                    "rasse" to dog.rasse,
                    "alter" to dog.alter,
                    "geburtsdatum" to dog.geburtsdatum?.let { isoDateFormat.format(it) },
                    "gewicht" to dog.gewicht,
                    "kalorienbedarf" to dog.kalorienbedarf,
                    "imageFileId" to dog.imageFileId,
                    "ownerId" to dog.ownerId,
                    "consumedCalories" to dog.consumedCalories,
                    "teamId" to dog.teamId // teamId auch beim Update mitsenden
                ).filterValues { it != null } // Entferne Null-Werte, um Felder nicht zu löschen

                Log.d("DogDataRepository", "Updating dog ${dog.id} with data: $dogData")

                // Wichtig: Beim Update werden die Berechtigungen normalerweise *nicht* erneut gesetzt,
                // es sei denn, man möchte sie ändern. Die vorhandenen Berechtigungen bleiben bestehen.
                databases.updateDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = dog.id,
                    data = dogData
                    // permissions = neuePermissionsListe // Nur angeben, wenn Berechtigungen geändert werden sollen!
                )
                Log.i("DogDataRepository", "Dog ${dog.id} successfully updated.")
                true // Erfolg
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Aktualisieren des Hundes (ID: ${dog.id}): ${e.message}")
            false // Fehler
        } catch (e: Exception) {
             Log.e("DogDataRepository", "Allgemeiner Fehler beim Aktualisieren des Hundes (ID: ${dog.id}): ${e.message}")
            false // Allgemeiner Fehler
        }
    }
    
    // Überladene Funktion zum Aktualisieren eines Hundes mit einzelnen Parametern
    suspend fun updateDog(
        dogId: String,
        name: String,
        rasse: String,
        alter: Int?,
        geburtsdatum: Date?,
        gewicht: Double?,
        kalorienbedarf: Int?,
        imageFileId: String?,
        ownerId: String,
        teamId: String?,
        imageUri: Uri? = null,
        context: Context? = null
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Verarbeite das Bild, falls eines vorhanden ist
                var updatedImageFileId = imageFileId
                if (imageUri != null && context != null) {
                    try {
                        // Laden Sie das Bild hoch und erhalten Sie die neue Bild-ID
                        updatedImageFileId = imageRepository.uploadImage(imageUri, context = context)
                        Log.d("DogDataRepository", "Bild erfolgreich aktualisiert mit ID: $updatedImageFileId")
                    } catch (e: Exception) {
                        Log.e("DogDataRepository", "Fehler beim Aktualisieren des Bildes: ${e.message}")
                        // Wir verwenden einfach die bestehende imageFileId weiter
                    }
                }
                
                // Sammle alle nötigen Daten
                val dogData = mutableMapOf<String, Any?>(
                    "name" to name,
                    "rasse" to rasse,
                    "alter" to alter,
                    "geburtsdatum" to geburtsdatum?.let { isoDateFormat.format(it) },
                    "gewicht" to gewicht,
                    "kalorienbedarf" to kalorienbedarf,
                    "ownerId" to ownerId,
                    "teamId" to teamId
                )
                
                // Bild-Datei-ID aktualisieren
                if (updatedImageFileId != null) {
                    dogData["imageFileId"] = updatedImageFileId
                }
                
                // Entferne null-Werte
                val filteredData = dogData.filterValues { it != null }
                
                Log.d("DogDataRepository", "Updating dog $dogId with data: $filteredData")
                
                databases.updateDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = dogId,
                    data = filteredData
                )
                
                Log.i("DogDataRepository", "Dog $dogId successfully updated.")
                true // Erfolg
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Aktualisieren des Hundes (ID: $dogId): ${e.message}")
            false // Fehler
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Allgemeiner Fehler beim Aktualisieren des Hundes (ID: $dogId): ${e.message}")
            false // Allgemeiner Fehler
        }
    }


    // Funktion zum Löschen eines Hundes
    suspend fun deleteDog(dogId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                databases.deleteDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = dogId
                )
                true // Erfolg
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Löschen des Hundes (ID: $dogId): ${e.message}")
            false // Fehler
        }
    }

    // Funktion zum Aktualisieren der verbrauchten Kalorien für einen Hund
    suspend fun updateConsumedCalories(dogId: String, consumed: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                databases.updateDocument(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                    documentId = dogId,
                    data = mapOf("consumedCalories" to consumed)
                )
                true // Erfolg
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Aktualisieren der verbrauchten Kalorien (ID: $dogId): ${e.message}")
            false // Fehler
        }
    }

    // Funktion zum Zurücksetzen der verbrauchten Kalorien für alle Hunde (z.B. täglich)
    suspend fun resetAllConsumedCalories(): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // 1. Alle Hunde-Dokumente abrufen (ggf. mit Query, falls nötig)
                // Vorsicht bei sehr vielen Dokumenten - Paginierung verwenden!
                val response = databases.listDocuments(
                    databaseId = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.DOGS_COLLECTION_ID
                    // queries = listOf(Query.limit(100)) // Beispiel für Limit
                )

                // 2. Jedes Dokument aktualisieren
                var allSuccessful = true
                for (document in response.documents) {
                    try {
                        databases.updateDocument(
                            databaseId = AppwriteConfig.DATABASE_ID,
                            collectionId = AppwriteConfig.DOGS_COLLECTION_ID,
                            documentId = document.id,
                            data = mapOf("consumedCalories" to 0)
                        )
                    } catch (updateError: AppwriteException) {
                        Log.e("DogDataRepository", "Fehler beim Zurücksetzen der Kalorien für Hund ${document.id}: ${updateError.message}")
                        allSuccessful = false
                        // Hier entscheiden, ob bei einem Fehler abgebrochen oder weitergemacht werden soll
                    }
                }
                allSuccessful
            }
        } catch (e: AppwriteException) {
            Log.e("DogDataRepository", "Fehler beim Abrufen der Hunde zum Kalorien-Reset: ${e.message}")
            false
        }
    }

    // Hilfsfunktion zum Mappen eines Appwrite-Dokuments auf ein Dog-Objekt
    private fun mapDocumentToDog(document: io.appwrite.models.Document<Map<String, Any>>): Dog? {
        return try {
            val data = document.data
            val birthDateString = data["geburtsdatum"] as? String
            val birthDate = birthDateString?.let {
                try { isoDateFormat.parse(it) } catch (e: Exception) { null }
            }

            Dog(
                id = document.id,
                name = data["name"] as? String ?: "",
                rasse = data["rasse"] as? String ?: "",
                alter = (data["alter"] as? Number)?.toInt(),
                geburtsdatum = birthDate, // Gemapptes Date-Objekt
                gewicht = (data["gewicht"] as? Number)?.toDouble(),
                kalorienbedarf = (data["kalorienbedarf"] as? Number)?.toInt(),
                imageFileId = data["imageFileId"] as? String,
                ownerId = data["ownerId"] as? String ?: "",
                // Default auf 0, falls das Feld fehlt (für ältere Einträge)
                consumedCalories = (data["consumedCalories"] as? Number)?.toInt() ?: 0,
                // TeamId auch auslesen, Default auf null, falls fehlt
                teamId = data["teamId"] as? String
            )
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Fehler beim Parsen eines Dog-Dokuments (ID: ${document.id}): ${e.message}")
            null // Überspringt fehlerhafte Dokumente
        }
    }
    
    // Fügt einen Benutzer zu einem Team hinzu
    suspend fun addTeamMemberByUserId(teamId: String, userIdToAdd: String, roles: List<String>): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val teams = io.appwrite.services.Teams(client)
                teams.createMembership(
                    teamId = teamId,
                    email = "", // Wir verwenden userId statt E-Mail
                    roles = roles,
                    url = "https://example.com", // Dummy URL für Einladung
                    userId = userIdToAdd
                )
                true
            }
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Fehler beim Hinzufügen des Benutzers $userIdToAdd zum Team $teamId: ${e.message}")
            false
        }
    }
    
    // Listet alle Mitglieder eines Teams auf
    suspend fun listTeamMembers(teamId: String): List<io.appwrite.models.Membership> {
        return try {
            withContext(Dispatchers.IO) {
                val teams = io.appwrite.services.Teams(client)
                val memberships = teams.listMemberships(teamId)
                memberships.memberships
            }
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Fehler beim Abrufen der Teammitglieder für Team $teamId: ${e.message}")
            emptyList()
        }
    }
    
    // Entfernt ein Mitglied aus einem Team
    suspend fun removeTeamMember(teamId: String, membershipId: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val teams = io.appwrite.services.Teams(client)
                teams.deleteMembership(teamId, membershipId)
                true
            }
        } catch (e: Exception) {
            Log.e("DogDataRepository", "Fehler beim Entfernen des Mitglieds $membershipId aus Team $teamId: ${e.message}")
            false
        }
    }
}