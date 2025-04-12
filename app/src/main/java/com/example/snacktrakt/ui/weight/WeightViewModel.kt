package com.example.snacktrakt.ui.weight

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snacktrakt.data.DogRepository
import com.example.snacktrakt.data.model.Dog
import com.example.snacktrakt.data.model.WeightEntry
import com.example.snacktrakt.data.repository.WeightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel für die Gewichtsverfolgung eines Hundes
 *
 * Dieses ViewModel verwaltet die Anzeige und Eingabe von Gewichtsdaten,
 * inklusive der Gewichtshistorie und Diagrammdarstellung.
 *
 * @param repository DogRepository für Datenbankzugriffe
 */
class WeightViewModel(private val repository: DogRepository) : ViewModel() {
    
    // UI-Zustände
    var isLoading by mutableStateOf(false)
        private set
    
    var showWeightInputDialog by mutableStateOf(false)
        private set
    
    var selectedTimeRange by mutableStateOf(WeightRepository.TimeRange.MONTH)
        private set
    
    var weightInput by mutableStateOf("")
        private set
    
    var noteInput by mutableStateOf("")
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Daten
    private val _currentDog = MutableStateFlow<Dog?>(null)
    val currentDog = _currentDog.asStateFlow()
    
    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()
    
    private val _filteredEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val filteredEntries: StateFlow<List<WeightEntry>> = _filteredEntries.asStateFlow()
    
    /**
     * Lädt die Daten eines Hundes und dessen Gewichtshistorie
     *
     * @param dogId ID des Hundes
     */
    fun loadDog(dogId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // Lade Hundedaten
                val dog = repository.getDogById(dogId)
                _currentDog.value = dog
                
                // Lade Gewichtseinträge
                loadWeightEntries(dogId)
                
                // Filtere nach ausgewähltem Zeitraum
                filterEntriesByTimeRange()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dog data", e)
                errorMessage = "Fehler beim Laden der Daten: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Lädt die Gewichtseinträge eines Hundes
     *
     * @param dogId ID des Hundes
     */
    private suspend fun loadWeightEntries(dogId: String) {
        try {
            val entries = repository.getWeightHistory(dogId)
            _weightEntries.value = entries
            Log.d(TAG, "Loaded ${entries.size} weight entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading weight entries", e)
            errorMessage = "Fehler beim Laden der Gewichtsdaten: ${e.message}"
        }
    }
    
    /**
     * Filtert die Gewichtseinträge nach ausgewähltem Zeitraum
     */
    private suspend fun filterEntriesByTimeRange() {
        val dogId = _currentDog.value?.id ?: return
        
        try {
            val filtered = if (selectedTimeRange == WeightRepository.TimeRange.ALL) {
                _weightEntries.value
            } else {
                // TimeRange in Start- und Enddate umwandeln für Repository-Aufruf
                val now = Date()
                val startDate = when(selectedTimeRange) {
                    WeightRepository.TimeRange.WEEK -> Date(now.time - 7L * 24L * 60L * 60L * 1000L)
                    WeightRepository.TimeRange.MONTH -> Date(now.time - 30L * 24L * 60L * 60L * 1000L)
                    WeightRepository.TimeRange.THREE_MONTHS -> Date(now.time - 90L * 24L * 60L * 60L * 1000L)
                    WeightRepository.TimeRange.YEAR -> Date(now.time - 365L * 24L * 60L * 60L * 1000L)
                    WeightRepository.TimeRange.ALL -> Date(0) // Anfang der Zeit
                }
                repository.getWeightHistoryForTimeRange(dogId, startDate, now)
            }
            
            _filteredEntries.value = filtered
            Log.d(TAG, "Filtered to ${filtered.size} entries for time range: $selectedTimeRange")
        } catch (e: Exception) {
            Log.e(TAG, "Error filtering entries", e)
            errorMessage = "Fehler beim Filtern der Daten: ${e.message}"
        }
    }
    
    /**
     * Ändert den ausgewählten Zeitraum und aktualisiert die gefilterten Einträge
     *
     * @param range Der neue Zeitraum
     */
    fun setTimeRange(range: WeightRepository.TimeRange) {
        if (selectedTimeRange != range) {
            selectedTimeRange = range
            
            viewModelScope.launch {
                filterEntriesByTimeRange()
            }
        }
    }
    
    /**
     * Öffnet den Dialog zur Gewichtseingabe
     *
     * @param currentWeight Das aktuelle Gewicht als Vorgabewert
     */
    fun showWeightInput(currentWeight: Double? = null) {
        weightInput = currentWeight?.toString() ?: ""
        noteInput = ""
        showWeightInputDialog = true
    }
    
    /**
     * Schließt den Dialog zur Gewichtseingabe
     */
    fun dismissWeightInput() {
        showWeightInputDialog = false
        weightInput = ""
        noteInput = ""
    }
    
    /**
     * Aktualisiert den Gewichtseingabewert
     *
     * @param value Der neue Wert
     */
    fun updateWeightInput(value: String) {
        weightInput = value
    }
    
    /**
     * Aktualisiert die Notizeingabe
     *
     * @param value Der neue Wert
     */
    fun updateNoteInput(value: String) {
        noteInput = value
    }
    
    /**
     * Speichert das neue Gewicht
     */
    fun saveWeight() {
        val dogId = _currentDog.value?.id ?: return
        val weightString = weightInput.trim()
        
        if (weightString.isEmpty()) {
            errorMessage = "Bitte gib ein Gewicht ein"
            return
        }
        
        val weight = try {
            weightString.toDouble()
        } catch (e: NumberFormatException) {
            errorMessage = "Bitte gib eine gültige Zahl ein"
            return
        }
        
        if (weight <= 0) {
            errorMessage = "Das Gewicht muss größer als 0 sein"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                // Gewicht speichern
                val note = noteInput.takeIf { it.isNotBlank() }
                repository.updateDogWeight(dogId, weight, note)
                
                // Dialog schließen
                dismissWeightInput()
                
                // Daten neu laden
                loadDog(dogId)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving weight", e)
                errorMessage = "Fehler beim Speichern: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    companion object {
        private const val TAG = "WeightViewModel"
    }
}
