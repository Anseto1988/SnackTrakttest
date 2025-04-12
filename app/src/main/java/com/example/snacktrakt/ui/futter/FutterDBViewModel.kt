package com.example.snacktrakt.ui.futter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snacktrakt.data.FutterDBRepository
import com.example.snacktrakt.data.model.Einreichung
import com.example.snacktrakt.data.model.FutterDB
import com.example.snacktrakt.data.model.Hersteller
import com.example.snacktrakt.data.model.Kategorie
import com.example.snacktrakt.data.model.Lebensphase
import com.example.snacktrakt.data.model.Unterkategorie
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

private const val TAG = "FutterDBViewModel"

/**
 * ViewModel für die Futter-Datenbank
 *
 * Diese Klasse ist verantwortlich für:
 * - Suchen von Futterinformationen nach EAN
 * - Abrufen von Referenzdaten (Kategorien, Hersteller, usw.)
 * - Berechnen von Kalorien basierend auf Nährwerten
 * - Einreichen neuer Futterprodukte
 */
class FutterDBViewModel(
    private val futterDBRepository: FutterDBRepository
) : ViewModel() {
    
    // State für die Suche nach Futter per EAN
    private val _eanSearchState = MutableStateFlow<EANSearchState>(EANSearchState.Initial)
    val eanSearchState: StateFlow<EANSearchState> = _eanSearchState.asStateFlow()
    
    // Zuletzt gefundenes Futter
    private val _currentFutter = MutableStateFlow<FutterDB?>(null)
    val currentFutter: StateFlow<FutterDB?> = _currentFutter.asStateFlow()
    
    // Referenzdaten
    private val _lebensphasen = MutableStateFlow<List<Lebensphase>>(emptyList())
    val lebensphasen: StateFlow<List<Lebensphase>> = _lebensphasen.asStateFlow()
    
    private val _kategorien = MutableStateFlow<List<Kategorie>>(emptyList())
    val kategorien: StateFlow<List<Kategorie>> = _kategorien.asStateFlow()
    
    private val _unterkategorien = MutableStateFlow<List<Unterkategorie>>(emptyList())
    val unterkategorien: StateFlow<List<Unterkategorie>> = _unterkategorien.asStateFlow()
    
    private val _hersteller = MutableStateFlow<List<Hersteller>>(emptyList())
    val hersteller: StateFlow<List<Hersteller>> = _hersteller.asStateFlow()
    
    // Status der Einreichung
    private val _einreichungsState = MutableStateFlow<EinreichungsState>(EinreichungsState.Initial)
    val einreichungsState: StateFlow<EinreichungsState> = _einreichungsState.asStateFlow()
    
    // Herstellervorschläge für die Autovervollständigung
    private val _herstellerQuery = MutableStateFlow("")
    private val _herstellerVorschlaege = MutableStateFlow<List<Hersteller>>(emptyList())
    val herstellerVorschlaege: StateFlow<List<Hersteller>> = _herstellerVorschlaege.asStateFlow()
    
    // Job für die verzögerte Suche
    private var searchJob: Job? = null
    
    init {
        loadReferenceData()
        setupHerstellerSearch()
    }
    
    /**
     * Sucht ein Futter anhand der EAN-Nummer
     *
     * @param ean EAN-Nummer des zu suchenden Futters
     */
    fun searchFutterByEAN(ean: String) {
        _eanSearchState.value = EANSearchState.Loading
        
        viewModelScope.launch {
            try {
                val result = futterDBRepository.getFutterByEAN(ean)
                
                if (result != null) {
                    Log.d(TAG, "Futter gefunden: ${result.produktname}")
                    _currentFutter.value = result
                    _eanSearchState.value = EANSearchState.Success(result)
                } else {
                    Log.d(TAG, "Kein Futter mit EAN $ean gefunden")
                    _currentFutter.value = null
                    _eanSearchState.value = EANSearchState.NotFound
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei der Suche nach Futter mit EAN $ean", e)
                _eanSearchState.value = EANSearchState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    /**
     * Berechnet die Kalorien für eine bestimmte Menge Futter
     *
     * @param futterDB Das Futter, für das die Kalorien berechnet werden sollen
     * @param gramm Die Menge in Gramm
     * @return Die berechnete Kalorienmenge oder null, wenn nicht berechenbar
     */
    fun calculateCaloriesForAmount(futterDB: FutterDB, gramm: Double): Int? {
        // Prüfen, ob kcalPro100g direkt verfügbar ist
        return if (futterDB.kcalPro100g != null) {
            // Einfache Berechnung basierend auf dem vorhandenen kcalPro100g-Wert
            ((futterDB.kcalPro100g * gramm) / 100.0).toInt()
        } else if (futterDB.protein != null && futterDB.fett != null && 
                 futterDB.rohasche != null && futterDB.rohfaser != null && 
                 futterDB.feuchtigkeit != null) {
            // Berechnung über Nährwerte, wenn kcalPro100g nicht direkt verfügbar ist
            val kcalPro100g = futterDBRepository.calculateKcalPro100g(
                protein = futterDB.protein,
                fett = futterDB.fett,
                rohasche = futterDB.rohasche,
                rohfaser = futterDB.rohfaser,
                feuchtigkeit = futterDB.feuchtigkeit
            )
            ((kcalPro100g * gramm) / 100.0).toInt()
        } else {
            // Nicht genügend Daten für die Berechnung
            null
        }
    }
    
    /**
     * Berechnet die Nährwertangaben für ein Futter
     *
     * @param protein Protein in % (g/100g)
     * @param fett Fett in % (g/100g)
     * @param rohasche Rohasche in % (g/100g)
     * @param rohfaser Rohfaser in % (g/100g)
     * @param feuchtigkeit Feuchtigkeit in % (g/100g)
     * @return Berechneter Kaloriengehalt in kcal/100g
     */
    fun calculateKcal(
        protein: Int,
        fett: Int,
        rohasche: Int,
        rohfaser: Int,
        feuchtigkeit: Int
    ): Int {
        return futterDBRepository.calculateKcalPro100g(
            protein, fett, rohasche, rohfaser, feuchtigkeit
        )
    }
    
    /**
     * Berechnet den NFE-Wert basierend auf den Nährwerten
     */
    fun calculateNFE(
        protein: Int,
        fett: Int,
        rohasche: Int,
        rohfaser: Int,
        feuchtigkeit: Int
    ): Int {
        return futterDBRepository.calculateNFE(
            protein, fett, rohasche, rohfaser, feuchtigkeit
        )
    }
    
    /**
     * Reicht ein neues Futterprodukt ein
     *
     * @param hersteller Der Hersteller des Produkts
     * @param kategorie Die Kategorie des Produkts
     * @param unterkategorie Die Unterkategorie des Produkts
     * @param produktname Der Name des Produkts
     * @param ean Die EAN des Produkts
     * @param lebensphase Die Lebensphase für das Produkt
     * @param protein Protein in % (g/100g)
     * @param fett Fett in % (g/100g)
     * @param rohasche Rohasche in % (g/100g)
     * @param rohfaser Rohfaser in % (g/100g)
     * @param feuchtigkeit Feuchtigkeit in % (g/100g)
     */
    fun submitEinreichung(
        hersteller: String,
        kategorie: String,
        unterkategorie: String,
        produktname: String,
        ean: String,
        lebensphase: String,
        protein: Int,
        fett: Int,
        rohasche: Int,
        rohfaser: Int,
        feuchtigkeit: Int
    ) {
        _einreichungsState.value = EinreichungsState.Submitting
        
        viewModelScope.launch {
            try {
                // Berechne NFE und kcalPro100g
                val nfe = calculateNFE(protein, fett, rohasche, rohfaser, feuchtigkeit)
                val kcalPro100g = calculateKcal(protein, fett, rohasche, rohfaser, feuchtigkeit)
                
                // Erstelle Einreichungs-Objekt
                val einreichung = Einreichung(
                    hersteller = hersteller,
                    kategorie = kategorie,
                    unterkategorie = unterkategorie,
                    produktname = produktname,
                    ean = ean,
                    lebensphase = lebensphase,
                    kcalPro100g = kcalPro100g,
                    protein = protein,
                    fett = fett,
                    rohasche = rohasche,
                    rohfaser = rohfaser,
                    feuchtigkeit = feuchtigkeit,
                    nfe = nfe
                )
                
                // Speichere die Einreichung
                val result = futterDBRepository.createEinreichung(einreichung)
                
                if (result != null) {
                    Log.d(TAG, "Einreichung erfolgreich: ${result.produktname}")
                    _einreichungsState.value = EinreichungsState.Success
                } else {
                    Log.e(TAG, "Fehler bei der Einreichung")
                    _einreichungsState.value = EinreichungsState.Error("Einreichung konnte nicht gespeichert werden")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei der Einreichung", e)
                _einreichungsState.value = EinreichungsState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
    
    /**
     * Aktualisiert die Herstellersuche
     */
    fun updateHerstellerQuery(query: String) {
        _herstellerQuery.value = query
    }
    
    /**
     * Richtet die verzögerte Suche für Herstellervorschläge ein
     */
    @OptIn(FlowPreview::class)
    private fun setupHerstellerSearch() {
        viewModelScope.launch {
            _herstellerQuery
                .debounce(300) // Warte 300ms, bevor die Suche ausgeführt wird
                .collect { query ->
                    if (query.length >= 2) {
                        searchHersteller(query)
                    } else {
                        _herstellerVorschlaege.value = emptyList()
                    }
                }
        }
    }
    
    /**
     * Sucht nach Herstellern, die mit der Anfrage beginnen
     */
    private fun searchHersteller(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val results = futterDBRepository.searchHersteller(query)
                _herstellerVorschlaege.value = results
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei der Herstellersuche", e)
                _herstellerVorschlaege.value = emptyList()
            }
        }
    }
    
    /**
     * Lädt alle Referenzdaten (Lebensphasen, Kategorien, etc.)
     */
    private fun loadReferenceData() {
        viewModelScope.launch {
            try {
                // Lade alle Referenzdaten parallel
                launch { _lebensphasen.value = futterDBRepository.getAllLebensphasen() }
                launch { _kategorien.value = futterDBRepository.getAllKategorien() }
                launch { _unterkategorien.value = futterDBRepository.getAllUnterkategorien() }
                launch { _hersteller.value = futterDBRepository.getAllHersteller() }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Laden der Referenzdaten", e)
            }
        }
    }
    
    /**
     * Zurücksetzen des EAN-Suchstatus
     */
    fun resetEANSearch() {
        _eanSearchState.value = EANSearchState.Initial
        _currentFutter.value = null
    }
    
    /**
     * Zurücksetzen des Einreichungsstatus
     */
    fun resetEinreichungsState() {
        _einreichungsState.value = EinreichungsState.Initial
    }
}

/**
 * Status der EAN-Suche
 */
sealed class EANSearchState {
    object Initial : EANSearchState()
    object Loading : EANSearchState()
    data class Success(val futter: FutterDB) : EANSearchState()
    object NotFound : EANSearchState()
    data class Error(val message: String) : EANSearchState()
}

/**
 * Status der Einreichung
 */
sealed class EinreichungsState {
    object Initial : EinreichungsState()
    object Submitting : EinreichungsState()
    object Success : EinreichungsState()
    data class Error(val message: String) : EinreichungsState()
}
