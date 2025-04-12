package com.example.snacktrakt.data

import android.util.Log
import com.example.snacktrakt.data.model.Einreichung
import com.example.snacktrakt.data.model.FutterDB
import com.example.snacktrakt.data.model.Hersteller
import com.example.snacktrakt.data.model.Kategorie
import com.example.snacktrakt.data.model.Lebensphase
import com.example.snacktrakt.data.model.Unterkategorie
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository für die Verwaltung der Futter-Datenbank mit Appwrite
 * 
 * Diese Klasse kapselt alle Datenbankzugriffe für die neue Futterinformationsdatenbank:
 * - Abfrage von Futterinformationen nach EAN
 * - Abfrage aller Kategorien, Unterkategorien, Hersteller und Lebensphasen
 * - Berechnung der Kalorien basierend auf Nährwerten
 * 
 * @param client Appwrite Client für API-Zugriffe
 */
class FutterDBRepository(private val client: Client) {
    private val databases = Databases(client)
    
    /**
     * Konstanten für Datenbank- und Sammlungs-IDs
     */
    companion object {
        const val DATABASE_ID = "67d5665000332c9ebb54"  // Futter-DB
        const val FUTTER_COLLECTION_ID = "67e6f65700170b2f787f"  // futter
        const val LEBENSPHASE_COLLECTION_ID = "67e6f8e50036071b9247"  // lebensphase
        const val KATEGORIE_COLLECTION_ID = "67e42323003e7690101c"  // Kategorie
        const val UNTERKATEGORIE_COLLECTION_ID = "67e6f5ad00012aa9d26e"  // Unterkategorie
        const val HERSTELLER_COLLECTION_ID = "67d566620031143ec185"  // Hersteller
        const val EINREICHUNG_COLLECTION_ID = "67e7e85d003a070d176b"  // Einreichung
        private const val TAG = "FutterDBRepository"
        
        // Maximale Länge für String-Felder in Appwrite
        private const val MAX_STRING_LENGTH = 50
    }
    
    /**
     * Sucht ein Futter anhand der EAN-Nummer
     * 
     * @param ean EAN-Nummer des zu suchenden Futters
     * @return Das gefundene Futter oder null, wenn nicht gefunden
     */
    suspend fun getFutterByEAN(ean: String): FutterDB? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Suche Futter mit EAN: $ean")
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = FUTTER_COLLECTION_ID,
                queries = listOf(Query.equal("EAN", ean))
            )
            
            return@withContext if (response.documents.isNotEmpty()) {
                val document = response.documents[0]
                Log.d(TAG, "Futter gefunden: ${document.id}")
                parseFutterFromDocument(document.id, document.data)
            } else {
                Log.d(TAG, "Kein Futter mit EAN $ean gefunden")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Suchen von Futter mit EAN $ean", e)
            null
        }
    }
    
    /**
     * Ruft alle Lebensphasen ab
     */
    suspend fun getAllLebensphasen(): List<Lebensphase> = withContext(Dispatchers.IO) {
        try {
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = LEBENSPHASE_COLLECTION_ID
            )
            
            return@withContext response.documents.map { document ->
                Lebensphase(
                    id = document.id,
                    name = document.data["name"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen aller Lebensphasen", e)
            emptyList()
        }
    }
    
    /**
     * Ruft alle Kategorien ab
     */
    suspend fun getAllKategorien(): List<Kategorie> = withContext(Dispatchers.IO) {
        try {
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = KATEGORIE_COLLECTION_ID
            )
            
            return@withContext response.documents.map { document ->
                Kategorie(
                    id = document.id,
                    name = document.data["name"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen aller Kategorien", e)
            emptyList()
        }
    }
    
    /**
     * Ruft alle Unterkategorien ab
     */
    suspend fun getAllUnterkategorien(): List<Unterkategorie> = withContext(Dispatchers.IO) {
        try {
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = UNTERKATEGORIE_COLLECTION_ID
            )
            
            return@withContext response.documents.map { document ->
                Unterkategorie(
                    id = document.id,
                    name = document.data["name"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen aller Unterkategorien", e)
            emptyList()
        }
    }
    
    /**
     * Ruft alle Hersteller ab
     */
    suspend fun getAllHersteller(): List<Hersteller> = withContext(Dispatchers.IO) {
        try {
            val response = databases.listDocuments(
                databaseId = DATABASE_ID,
                collectionId = HERSTELLER_COLLECTION_ID
            )
            
            return@withContext response.documents.map { document ->
                Hersteller(
                    id = document.id,
                    name = document.data["Hersteller"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen aller Hersteller", e)
            emptyList()
        }
    }
    
    /**
     * Sucht Hersteller, die mit dem gegebenen Suchbegriff beginnen
     *
     * @param query Der Suchbegriff
     * @return Eine gefilterte Liste von Herstellern, die mit dem Suchbegriff beginnen
     */
    suspend fun searchHersteller(query: String): List<Hersteller> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext emptyList()
            }
            
            val allHersteller = getAllHersteller()
            return@withContext allHersteller.filter { hersteller ->
                hersteller.name.startsWith(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Suchen von Herstellern", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Begrenzt die Länge eines Strings auf die maximale Länge für Appwrite
     */
    private fun limitStringLength(value: String, maxLength: Int = MAX_STRING_LENGTH): String {
        return value.take(maxLength)
    }
    
    /**
     * Erstellt eine neue Futtereinreichung in der Datenbank
     *
     * @param einreichung Die zu erstellende Einreichung
     * @return Die erstellte Einreichung mit der generierten ID oder null bei einem Fehler
     */
    suspend fun createEinreichung(einreichung: Einreichung): Einreichung? = withContext(Dispatchers.IO) {
        try {
            // Limitiere die Länge aller String-Felder
            val document = databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = EINREICHUNG_COLLECTION_ID,
                documentId = ID.unique(),
                data = mapOf(
                    "Hersteller" to limitStringLength(einreichung.hersteller),
                    "Kategorie" to limitStringLength(einreichung.kategorie),
                    "Unterkategorie" to limitStringLength(einreichung.unterkategorie),
                    "Produktname" to limitStringLength(einreichung.produktname, 100), // Produktname kann länger sein
                    "EAN" to limitStringLength(einreichung.ean),
                    "Lebensphase" to limitStringLength(einreichung.lebensphase),
                    "Kcalpro100g" to einreichung.kcalPro100g,
                    "protein" to einreichung.protein,
                    "fett" to einreichung.fett,
                    "rohasche" to einreichung.rohasche,
                    "rohfaser" to einreichung.rohfaser,
                    "feuchtigkeit" to einreichung.feuchtigkeit,
                    "nfe" to einreichung.nfe
                )
            )
            
            return@withContext Einreichung(
                id = document.id,
                hersteller = document.data["Hersteller"] as String,
                kategorie = document.data["Kategorie"] as String,
                unterkategorie = document.data["Unterkategorie"] as String,
                produktname = document.data["Produktname"] as String,
                ean = document.data["EAN"] as String,
                lebensphase = document.data["Lebensphase"] as String,
                kcalPro100g = (document.data["Kcalpro100g"] as Number).toInt(),
                protein = (document.data["protein"] as Number).toInt(),
                fett = (document.data["fett"] as Number).toInt(),
                rohasche = (document.data["rohasche"] as Number).toInt(),
                rohfaser = (document.data["rohfaser"] as Number).toInt(),
                feuchtigkeit = (document.data["feuchtigkeit"] as Number).toInt(),
                nfe = (document.data["nfe"] as Number).toInt()
            )
        } catch (e: AppwriteException) {
            Log.e(TAG, "Fehler beim Erstellen einer Einreichung", e)
            return@withContext null
        }
    }
    
    /**
     * Berechnet den NFE-Wert (Stickstofffreie Extraktstoffe) aus den gegebenen Nährwerten
     *
     * @param protein Protein in % (g/100g)
     * @param fett Fett in % (g/100g)
     * @param rohasche Rohasche in % (g/100g)
     * @param rohfaser Rohfaser in % (g/100g)
     * @param feuchtigkeit Feuchtigkeit in % (g/100g)
     * @return Berechneter NFE-Wert in % (g/100g)
     */
    fun calculateNFE(
        protein: Int, 
        fett: Int, 
        rohasche: Int, 
        rohfaser: Int, 
        feuchtigkeit: Int
    ): Int {
        // Formel: NFE = 100 - (Protein + Fett + Rohasche + Rohfaser + Feuchtigkeit)
        val nfeValue = 100 - (protein + fett + rohasche + rohfaser + feuchtigkeit)
        return maxOf(0, nfeValue) // NFE kann nicht negativ sein
    }
    
    /**
     * Berechnet die Kalorien pro 100g basierend auf den Nährwerten nach der Atwater-Methode
     * 
     * Nach wissenschaftlichen Studien zur Berechnung von Kalorien in Hundefutter:
     * - Protein: 3,5 kcal/g
     * - Fett: 8,5 kcal/g
     * - NFE (Kohlenhydrate): 3,5 kcal/g
     * 
     * Diese modifizierten Atwater-Faktoren berücksichtigen die geringere Verdaulichkeit 
     * von Tiernahrung im Vergleich zu menschlicher Nahrung.
     *
     * @param protein Protein in % (g/100g)
     * @param fett Fett in % (g/100g)
     * @param rohasche Rohasche in % (g/100g)
     * @param rohfaser Rohfaser in % (g/100g)
     * @param feuchtigkeit Feuchtigkeit in % (g/100g)
     * @return Berechnete Kalorien in kcal/100g
     */
    fun calculateKcalPro100g(
        protein: Int, 
        fett: Int, 
        rohasche: Int, 
        rohfaser: Int, 
        feuchtigkeit: Int
    ): Int {
        // Berechne NFE aus den anderen Werten
        val nfe = calculateNFE(protein, fett, rohasche, rohfaser, feuchtigkeit)
        
        // Angepasste Atwater-Faktoren für Hundefutter
        val proteinFaktor = 3.5 // kcal/g
        val fettFaktor = 8.5 // kcal/g
        val nfeFaktor = 3.5 // kcal/g
        
        // Berechne Kalorien pro 100g
        val kcal = (protein * proteinFaktor) + (fett * fettFaktor) + (nfe * nfeFaktor)
        
        return kcal.toInt()
    }
    
    /**
     * Parsierung eines Dokuments in ein FutterDB-Objekt
     */
    private fun parseFutterFromDocument(id: String, data: Map<String, Any?>): FutterDB {
        // Helper Funktion zum sicheren Extrahieren von Integer-Werten
        fun getIntValue(key: String): Int? {
            val value = data[key] ?: return null
            return when (value) {
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        }
        
        return FutterDB(
            id = id,
            hersteller = data["Hersteller"] as? String ?: "",
            kategorie = data["Kategorie"] as? String ?: "",
            unterkategorie = data["Unterkategorie"] as? String ?: "",
            produktname = data["Produktname"] as? String ?: "",
            ean = data["EAN"] as? String ?: "",
            lebensphase = data["Lebensphase"] as? String ?: "",
            kcalPro100g = getIntValue("Kcalpro100g"),
            protein = getIntValue("protein"),
            fett = getIntValue("fett"),
            rohasche = getIntValue("rohasche"),
            rohfaser = getIntValue("rohfaser"),
            feuchtigkeit = getIntValue("feuchtigkeit"),
            nfe = getIntValue("nfe")
        )
    }
}
