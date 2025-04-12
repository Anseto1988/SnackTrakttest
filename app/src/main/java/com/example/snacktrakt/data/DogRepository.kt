package com.example.snacktrakt.data

// --- IMPORTS ---
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.snacktrakt.data.model.Dog
import com.example.snacktrakt.data.model.Futter
import com.example.snacktrakt.data.model.WeightEntry
import com.example.snacktrakt.data.repository.DogDataRepository
import com.example.snacktrakt.data.repository.DogStatsRepository
import com.example.snacktrakt.data.repository.FutterRepository
import com.example.snacktrakt.data.repository.ImageRepository
import com.example.snacktrakt.data.repository.WeightRepository
import io.appwrite.Client
import io.appwrite.models.Membership
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
// --- ENDE IMPORTS ---

class DogRepository @Inject constructor(
    private val client: Client,
    private val context: Context, // Haupt-Context der Anwendung
    private val authRepository: AuthRepository
) {

    // Spezialisierte Repositories initialisieren (unverändert)
    private val imageRepository = ImageRepository(client)
    private val dogDataRepository = DogDataRepository(client, imageRepository, authRepository)
    private val dogStatsRepository = DogStatsRepository(client)
    private val futterRepository = FutterRepository(client)
    private val weightRepository = WeightRepository(client, context)

    companion object {
        // Konstanten (unverändert)
        // ...
        private const val TAG = "DogRepository"
    }

    // createDog, addFutter (aktualisiert für die neue Methoden-Signatur)
    suspend fun createDog(name: String, rasse: String, alter: Int? = null, geburtsdatum: Date? = null,
        gewicht: Double? = null, kalorienbedarf: Int? = null, imageUri: Uri? = null, context: Context? = null): Dog {
        val currentUser = authRepository.getCurrentUser() ?: throw IllegalStateException("No logged in user")
        // We need the non-null context for createDog
        val effectiveContext = context ?: this.context
        return dogDataRepository.createDog(
            name = name,
            rasse = rasse,
            alter = alter,
            geburtsdatum = geburtsdatum,
            gewicht = gewicht,
            kalorienbedarf = kalorienbedarf,
            ownerId = currentUser.id,
            imageUri = imageUri,
            context = effectiveContext
        )
    }
    suspend fun addFutter(description: String, calories: Int, gramm: Int, dogId: String): Futter = withContext(Dispatchers.IO) {
        // Log the action
        Log.d(TAG, "Adding futter for dog $dogId: $description, $calories calories, $gramm gram")
        
        try {
            val currentUser = authRepository.getCurrentUser() ?: throw IllegalStateException("No logged in user")
            val dog = getDogById(dogId) // Get dog to check team membership
            val teamId = dog.teamId ?: throw IllegalStateException("Dog $dogId has no teamId")
            
            // Create the food entry via repository
            return@withContext futterRepository.addFutterEntry(
                name = description,
                calories = calories,
                dogId = dogId,
                teamId = teamId,
                entryOwnerId = currentUser.id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding futter", e)
            throw e
        }
    }


    suspend fun updateDogWeight(
        dogId: String,
        weight: Double,
        note: String? = null
    ): WeightEntry = withContext(Dispatchers.IO) {
        Log.d(TAG, "updateDogWeight called for dog $dogId")
        val currentUser = authRepository.getCurrentUser() ?: throw IllegalStateException("No logged in user")
        val dog = dogDataRepository.getDogById(dogId) // Holen, um Owner zu prüfen & Daten zu haben
        val teamId = dog.teamId ?: throw IllegalStateException("Dog $dogId has no teamId.")

        // 1. Haupt-Dokument aktualisieren (nur wenn Owner)
        if(currentUser.id == dog.ownerId) {
            Log.d(TAG, "User is owner, updating weight in main dog document for $dogId.")
            try{
                // Kopiere das aktuelle Dog-Objekt und ändere nur das Gewicht
                val updatedDog = dog.copy(gewicht = weight)
                dogDataRepository.updateDog(updatedDog)
            } catch (e: Exception){
                Log.e(TAG,"Failed to update weight in main dog document for $dogId", e)
            }
        } else {
            Log.w(TAG, "User ${currentUser.id} is not owner of dog $dogId (${dog.ownerId}). Skip update main weight.")
        }

        // 2. Gewichtseintrag erstellen (immer möglich für Teammitglieder)
        return@withContext weightRepository.addWeightEntry(
            dogId = dogId, weight = weight, teamId = teamId, entryOwnerId = currentUser.id,
            date = Date(), note = note
        )
    }

    // getWeightHistory, getWeightHistoryForTimeRange, deleteDog, getDogById, getAccessibleDogs (unverändert)
    suspend fun getWeightHistory(dogId: String): List<WeightEntry> {
        return weightRepository.getWeightEntriesForDog(dogId)
    }
    suspend fun getWeightHistoryForTimeRange(dogId: String, startDate: Date, endDate: Date): List<WeightEntry> = withContext(Dispatchers.IO) {
        return@withContext weightRepository.getWeightEntriesForDog(dogId).filter { entry ->
            entry.date.time >= startDate.time && entry.date.time <= endDate.time
        }
    }
    suspend fun deleteDog(dogId: String): Boolean = withContext(Dispatchers.IO) {
        // Log the action
        Log.d(TAG, "Deleting dog $dogId")
        return@withContext dogDataRepository.deleteDog(dogId)
    }
    suspend fun getDogById(dogId: String): Dog {
        return dogDataRepository.getDogById(dogId)
    }
    suspend fun getAccessibleDogs(): List<Dog> = withContext(Dispatchers.IO) {
        try {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.e(TAG, "getAccessibleDogs: No logged in user")
                return@withContext emptyList() // Sicherere Fehlerbehandlung: Leere Liste statt Exception
            }
            return@withContext dogDataRepository.getAccessibleDogs(currentUser.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAccessibleDogs: ${e.message}")
            return@withContext emptyList() // Bei allen Fehlern leere Liste zurückgeben
        }
    }
    
    /**
     * Ruft alle Futtereinträge für einen bestimmten Hund ab.
     */
    suspend fun getFutterByDogId(dogId: String): List<Futter> = withContext(Dispatchers.IO) {
        return@withContext futterRepository.getFutterByDogId(dogId)
    }
    
    /**
     * Generiert eine URL für ein Hundebild mit der angegebenen Größe
     */
    suspend fun getImageUrl(fileId: String, width: Int = 400, height: Int = 400): String = withContext(Dispatchers.IO) {
        // Sicherstellen, dass bei der Übergabe die richtigen Typen verwendet werden
        return@withContext imageRepository.getImagePreviewUrl(fileId, width, height)
    }
    
    /**
     * Setzt die verbrauchten Kalorien eines Hundes zurück (täglicher Reset).
     * Dies geschieht über einen Update-Call, der alle vorhandenen Daten beibehält
     * und nur die consumedCalories auf 0 setzt.
     * 
     * @param dogId ID des Hundes
     * @return true, wenn erfolgreich zurückgesetzt wurde
     */
    suspend fun resetConsumedCalories(dogId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Hole zuerst den aktuellen Zustand des Hundes
            val dog = dogDataRepository.getDogById(dogId)
            // Die getDogById-Methode wirft eine Exception, wenn der Hund nicht gefunden wird,
            // daher ist keine Null-Prüfung notwendig
            
            // Erstelle einen neuen Hund mit den gleichen Daten, aber consumedCalories = 0
            val updatedDog = dog.copy(consumedCalories = 0)
            
            // Rufe die updateDog-Methode des DogDataRepository auf mit dem aktualisierten Dog-Objekt
            dogDataRepository.updateDog(updatedDog)
            
            Log.d(TAG, "Reset consumed calories for dog $dogId (${dog.name}) to 0")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting consumed calories for dog $dogId", e)
            return@withContext false
        }
    }

    /**
     * Aktualisiert Hundedaten. Benötigt Context nur, wenn ein neues Bild hochgeladen wird.
     */
    suspend fun updateDogData(
        dogId: String, name: String? = null, rasse: String? = null, alter: Int? = null,
        geburtsdatum: Date? = null, gewicht: Double? = null, kalorienbedarf: Int? = null,
        imageUri: Uri? = null,
        context: Context? = null // Wird nur für Bild-Upload benötigt
    ): Dog? {
        Log.d(TAG, "updateDogData called for dog $dogId")
        val effectiveContext = if (imageUri != null) context else null
        if (imageUri != null && effectiveContext == null) {
            throw IllegalArgumentException("Context is required to update the dog's image.")
        }

        try {
            val currentDog = getDogById(dogId) // Aktuelle Daten holen

            // == BUILD CACHE PROBLEM? ==
            // Wenn hier "No parameter..." Fehler auftreten -> Invalidate Caches / Restart!
            // Stelle sicher, dass die Signatur von updateDog in DogDataRepository passt!
            return dogDataRepository.updateDog(
                dogId = dogId,
                name = name ?: currentDog.name,
                rasse = rasse ?: currentDog.rasse,
                alter = alter ?: currentDog.alter,
                geburtsdatum = geburtsdatum ?: currentDog.geburtsdatum,
                gewicht = gewicht ?: currentDog.gewicht,
                kalorienbedarf = kalorienbedarf ?: currentDog.kalorienbedarf,
                imageFileId = currentDog.imageFileId, // Aktuelle ID übergeben
                ownerId = currentDog.ownerId,         // Unverändert lassen
                teamId = currentDog.teamId,           // Unverändert lassen
                imageUri = imageUri,                  // Neues Bild oder null
                context = effectiveContext            // Context oder null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateDogData facade for dog $dogId", e)
            return null
        }
    }



    /**
     * NEU: Fügt einen Benutzer anhand seiner User ID zum Team des Hundes hinzu.
     * (Wrapper um die neue Funktion im DogDataRepository)
     *
     * @param dogId ID des Hundes, dessen Team der Benutzer beitreten soll.
     * @param userIdToAdd Die Appwrite User ID des hinzuzufügenden Benutzers.
     * @return True bei Erfolg, false bei Fehler oder wenn der Hund kein Team hat.
     */
    suspend fun addSharedUserById(dogId: String, userIdToAdd: String): Boolean {
        Log.d(TAG, "addSharedUserById called for dog $dogId, user $userIdToAdd")
        val currentUser = authRepository.getCurrentUser() ?: run {
            Log.e(TAG, "Cannot add shared user: Current user not logged in.")
            return false
        }
        try {
            val dog = getDogById(dogId)
            // Prüfen, ob der aktuelle Benutzer der Besitzer ist (nur Besitzer dürfen hinzufügen)
            if (dog.ownerId != currentUser.id) {
                Log.w(TAG, "User ${currentUser.id} is not the owner of dog $dogId (${dog.ownerId}). Cannot add member.")
                return false
            }
            if (dog.teamId == null) {
                Log.e(TAG, "Cannot add user $userIdToAdd: Dog $dogId has no associated teamId.")
                return false
            }
            // Delegiere an DataRepository, füge als EDITOR hinzu
            return dogDataRepository.addTeamMemberByUserId(
                teamId = dog.teamId,
                userIdToAdd = userIdToAdd,
                roles = listOf("editor") // Standardrolle EDITOR
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding shared user $userIdToAdd for dog $dogId", e)
            return false
        }
    }


    /** Listet geteilte Nutzer */
    suspend fun getSharedUsersForDog(dogId: String): List<Membership> {
        // Unverändert
        Log.d(TAG, "getSharedUsersForDog called for $dogId")
        val currentUser = authRepository.getCurrentUser() ?: return emptyList()
        try {
            val dog = getDogById(dogId)
            if (dog.teamId == null) return emptyList()
            val memberships = dogDataRepository.listTeamMembers(dog.teamId)
            return memberships.filter { it.userId != currentUser.id }
        } catch (e: Exception) { Log.e(TAG, "Error getting shared users", e); return emptyList() }
    }

    /** Entfernt geteilten Nutzer */
    suspend fun removeSharedUser(dogId: String, membershipId: String): Boolean {
        // Unverändert
        Log.d(TAG, "removeSharedUser called for dog $dogId, membership $membershipId")
        val currentUser = authRepository.getCurrentUser() ?: return false
        try {
            val dog = getDogById(dogId)
            if (dog.ownerId != currentUser.id) { Log.w(TAG, "Not owner"); return false }
            if (dog.teamId == null) return false
            return dogDataRepository.removeTeamMember(dog.teamId, membershipId)
        } catch (e: Exception) { Log.e(TAG, "Error removing shared user", e); return false }
    }
}