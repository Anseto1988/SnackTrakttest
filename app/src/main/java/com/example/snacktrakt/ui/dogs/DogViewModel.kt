package com.example.snacktrakt.ui.dogs

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snacktrakt.data.AuthRepository // Import AuthRepository
import com.example.snacktrakt.data.model.Dog
import com.example.snacktrakt.data.model.Futter
import com.example.snacktrakt.data.repository.DogDataRepository
import com.example.snacktrakt.data.repository.FutterRepository
import com.example.snacktrakt.data.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DogViewModel @Inject constructor(
    private val dogRepository: DogDataRepository,
    private val imageRepository: ImageRepository,
    private val futterRepository: FutterRepository,
    private val authRepository: AuthRepository // AuthRepository injizieren
) : ViewModel() {

    // Zustand für die Liste der Hunde
    private val _dogs = MutableStateFlow<List<Dog>>(emptyList())
    val dogs: StateFlow<List<Dog>> = _dogs
    
    // Zustand für den ausgewählten Hund
    private val _selectedDog = MutableStateFlow<Dog?>(null)
    val selectedDog: StateFlow<Dog?> = _selectedDog
    
    // Zustand für die Futtereinträge
    private val _futterState = MutableStateFlow<FutterState>(FutterState.Initial)
    val futterState: StateFlow<FutterState> = _futterState

    // Zustände für die Eingabefelder im CreateDogScreen
    var dogName by mutableStateOf("")
    var dogBreed by mutableStateOf("")
    var dogAge by mutableStateOf("")
    var dogBirthDate by mutableStateOf("") // Als String für TextField
    var dogWeight by mutableStateOf("")
    var dogCalorieNeeds by mutableStateOf("")
    var dogImageBitmap by mutableStateOf<Bitmap?>(null) // Für die Bildvorschau

    // Zustand für Ladeanzeige und Fehler
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var addDogSuccess by mutableStateOf(false) // Zustand für Erfolgsanzeige

    init {
        loadDogs() // Hunde beim Initialisieren laden
    }

    // Funktion zum Laden der Hunde aus dem Repository
    fun loadDogs() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Aktuelle User-ID holen (wichtig für Filterung, falls implementiert)
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    _dogs.value = dogRepository.getDogs(userId)
                } else {
                    // Fehler: Benutzer nicht angemeldet
                    errorMessage = "Benutzer nicht angemeldet."
                    _dogs.value = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "Fehler beim Laden der Hunde: ${e.message}"
                Log.e("DogViewModel", "Fehler in loadDogs: ${e.message}")
                _dogs.value = emptyList() // Leere Liste bei Fehler
            } finally {
                isLoading = false
            }
        }
    }

    // Funktion zum Hinzufügen eines neuen Hundes
    fun addDog() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            addDogSuccess = false // Erfolgsstatus zurücksetzen

            // 1. Eingaben validieren
            val ageInt = dogAge.toIntOrNull()
            val weightDouble = dogWeight.toDoubleOrNull()
            val calorieInt = dogCalorieNeeds.toIntOrNull()
            val ownerId = authRepository.getCurrentUserId() // User ID holen

            // NEU: Team ID holen
            val teamId = authRepository.getCurrentUserTeamId() // Kann null sein!

            if (dogName.isBlank() || dogBreed.isBlank() || ageInt == null || dogBirthDate.isBlank() || weightDouble == null || calorieInt == null) {
                errorMessage = "Bitte alle Felder korrekt ausfüllen."
                isLoading = false
                return@launch
            }

            if (ownerId == null) {
                errorMessage = "Fehler: Benutzer nicht angemeldet."
                isLoading = false
                return@launch
            }

            // NEU: Prüfen ob Team ID vorhanden ist (je nach Logik erforderlich oder nicht)
            if (teamId == null) {
                errorMessage = "Fehler: Team-ID konnte nicht ermittelt werden." // Oder andere Logik
                isLoading = false
                // return@launch // Entscheiden, ob Abbruch nötig ist, wenn keine Team-ID da ist
                // Wenn teamId optional ist, kann man hier weitermachen oder einen Default setzen
                // Für dieses Beispiel brechen wir ab, da die Berechtigung teamId braucht
                Log.e("DogViewModel", "Abbruch beim Hund hinzufügen: Keine Team-ID gefunden.")
                return@launch
            }


            // Datum validieren oder umwandeln (Annahme: Eingabe ist bereits im korrekten Format)
            // TODO: Füge hier eine robustere Datumsvalidierung/-konvertierung hinzu
            val birthDateString = dogBirthDate // Hier annehmen, dass es schon ISO 8601 ist oder passend formatiert wird

            try {
                var uploadedImageId: String? = null

                // 2. Bild hochladen, falls vorhanden
                dogImageBitmap?.let { bitmap ->
                    try {
                        // Bitmap in ByteArray umwandeln
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream) // Qualität anpassen
                        val byteArray = stream.toByteArray()
                        val fileName = "dog_${System.currentTimeMillis()}.jpg" // Eindeutiger Dateiname

                        uploadedImageId = imageRepository.uploadImage(byteArray, fileName, ownerId) // ownerId für Permissions
                        if (uploadedImageId == null) {
                            throw Exception("Bild-Upload fehlgeschlagen.") // Fehler auslösen, wenn Upload scheitert
                        }
                        Log.d("DogViewModel", "Bild hochgeladen mit ID: $uploadedImageId")
                    } catch (e: Exception) {
                        Log.e("DogViewModel", "Fehler beim Bild-Upload: ${e.message}")
                        errorMessage = "Fehler beim Bild-Upload: ${e.message}"
                        // Entscheiden, ob der Hund trotzdem erstellt werden soll
                        // isLoading = false
                        // return@launch // Hier vielleicht abbrechen?
                        // Wir machen hier weiter, aber ohne Bild
                    }
                }


                // 3. Hund im Repository hinzufügen (mit Permissions)
                val success = dogRepository.addDog(
                    name = dogName,
                    rasse = dogBreed,
                    alter = ageInt,
                    geburtsdatum = birthDateString, // ISO 8601 Format erwartet
                    gewicht = weightDouble,
                    kalorienbedarf = calorieInt,
                    ownerId = ownerId,
                    teamId = teamId, // Team-ID übergeben (NEU)
                    imageFileId = uploadedImageId
                )

                if (success) {
                    addDogSuccess = true // Erfolg signalisieren
                    Log.i("DogViewModel", "Hund erfolgreich hinzugefügt.")
                    // Felder leeren nach Erfolg
                    clearInputFields()
                    // Hundeliste neu laden, um den neuen Hund anzuzeigen
                    loadDogs()
                } else {
                    errorMessage = "Hund konnte nicht hinzugefügt werden. Überprüfe die Logs."
                    // Hier könnte spezifischeres Feedback basierend auf dem Repository-Fehler erfolgen
                }

            } catch (e: Exception) {
                errorMessage = "Ein Fehler ist aufgetreten: ${e.message}"
                Log.e("DogViewModel", "Fehler in addDog: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Funktion zum Aktualisieren eines bestehenden Hundes (Beispiel)
    fun updateDog(dog: Dog) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Hier könnten Validierungen für das 'dog'-Objekt erfolgen
                val success = dogRepository.updateDog(dog)
                if (success) {
                    Log.i("DogViewModel", "Hund ${dog.id} erfolgreich aktualisiert.")
                    loadDogs() // Liste neu laden
                } else {
                    errorMessage = "Fehler beim Aktualisieren des Hundes ${dog.id}."
                }
            } catch (e: Exception) {
                errorMessage = "Fehler beim Aktualisieren: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Funktion zum Löschen eines Hundes
    fun deleteDog(dogId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val success = dogRepository.deleteDog(dogId)
                if (success) {
                    Log.i("DogViewModel", "Hund $dogId erfolgreich gelöscht.")
                    loadDogs() // Liste neu laden
                } else {
                    errorMessage = "Fehler beim Löschen des Hundes $dogId."
                }
            } catch (e: Exception) {
                errorMessage = "Fehler beim Löschen: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    // Hilfsfunktion zum Leeren der Eingabefelder
    fun clearInputFields() {
        dogName = ""
        dogBreed = ""
        dogAge = ""
        dogBirthDate = ""
        dogWeight = ""
        dogCalorieNeeds = ""
        dogImageBitmap = null
        addDogSuccess = false // Erfolgsstatus zurücksetzen
        errorMessage = null // Fehlermeldung löschen
    }

    // Hilfsfunktion zum Formatieren von Datum (Beispiel)
    fun formatDate(date: Date): String {
        // Verwende das ISO 8601 Format, das Appwrite erwartet
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.GERMANY)
        return sdf.format(date)
    }

    // Funktion zum Setzen des Geburtsdatums (wird vom Date Picker aufgerufen)
    fun setBirthDate(year: Int, month: Int, day: Int) {
        // Erstelle einen ISO 8601 String (oder das Format, das du brauchst)
        // Hier als Beispiel nur Datum, Appwrite 'datetime' braucht auch Zeit.
        // Passe das an, wie du es in der DB speichern willst (Date oder DateTime)
        // Beispiel für reines Datum:
        // dogBirthDate = String.format("%d-%02d-%02d", year, month + 1, day)

        // Beispiel für DateTime (mit 00:00:00 Uhr):
        // ACHTUNG: Appwrite erwartet oft UTC Zeit! Ggf. Zeitzone berücksichtigen.
        val dateString = String.format("%d-%02d-%02dT00:00:00.000+00:00", year, month + 1, day)
        dogBirthDate = dateString // Setzt den String im ViewModel
    }
    
    /**
     * Setzt den aktuell ausgewählten Hund
     */
    fun selectDog(dog: Dog) {
        _selectedDog.value = dog
    }
    
    /**
     * Lädt Futtereinträge für einen bestimmten Hund
     */
    fun loadFutterForDog(dogId: String) {
        viewModelScope.launch {
            _futterState.value = FutterState.Loading
            try {
                val futterList = futterRepository.getFutterByDogId(dogId)
                _futterState.value = FutterState.Success(futterList)
            } catch (e: Exception) {
                Log.e("DogViewModel", "Fehler beim Laden der Futtereinträge: ${e.message}")
                _futterState.value = FutterState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    /**
     * Fügt einen neuen Futtereintrag für den ausgewählten Hund hinzu
     */
    fun addFutter(description: String, calories: Int) {
        val dog = _selectedDog.value ?: return
        viewModelScope.launch {
            try {
                // Aktuelle Benutzer-ID abrufen
                val ownerId = authRepository.getCurrentUserId() ?: return@launch
                val teamId = dog.teamId ?: return@launch
                
                // Futtereintrag erstellen
                val futter = futterRepository.addFutterEntry(
                    name = description,
                    calories = calories,
                    dogId = dog.id,
                    teamId = teamId,
                    entryOwnerId = ownerId
                )
                
                // Kalorien zum Hund hinzufügen
                val updatedCalories = (dog.consumedCalories ?: 0) + calories
                dogRepository.updateConsumedCalories(dog.id, updatedCalories)
                
                // Aktualisierte Futtereinträge laden
                loadFutterForDog(dog.id)
                
            } catch (e: Exception) {
                Log.e("DogViewModel", "Fehler beim Hinzufügen von Futter: ${e.message}")
                errorMessage = "Fehler beim Speichern der Mahlzeit: ${e.message}"
            }
        }
    }
    
    /**
     * Lädt alle Hunde des aktuellen Benutzers neu
     */
    fun loadUserDogs() {
        loadDogs()
    }

    /**
     * Status der Futtereinträge (Mahlzeiten)
     */
    sealed class FutterState {
        object Initial : FutterState()
        object Loading : FutterState()
        data class Success(val futterList: List<Futter>) : FutterState()
        data class Error(val message: String) : FutterState()
    }
}