package com.example.snacktrakt.data

import io.appwrite.Client
import android.content.Context

/**
 * Konfigurationsklasse für Appwrite-Backend
 * 
 * Diese Singleton-Klasse stellt die Verbindung zur Appwrite-Backend-Infrastruktur her.
 * Sie enthält die erforderlichen Konfigurationsparameter (Endpoint und Projekt-ID)
 * und stellt eine Methode zum Erstellen des Client-Objekts bereit.
 * 
 * Der Appwrite-Client wird von allen Repository-Klassen verwendet, um mit dem Backend zu kommunizieren.
 */
object AppwriteConfig {
    /**
     * Endpoint-URL des Appwrite-Servers
     */
    private const val ENDPOINT = "https://parse.nordburglarp.de/v1"
    
    /**
     * Projekt-ID des Appwrite-Projekts
     * Diese ID wird in verschiedenen Teilen der Anwendung verwendet, 
     * z.B. für die Generierung von Bild-URLs
     */
    const val PROJECT_ID = "67cde635000efa2e5081"
    
    /**
     * Datenbank-ID der Hauptdatenbank
     */
    const val DATABASE_ID = "67d175de002e0cb4c394"
    
    /**
     * Collection-IDs für die verschiedenen Datensammlungen
     */
    const val DOGS_COLLECTION_ID = "67d1761c00166b4b2b85"
    const val FUTTER_COLLECTION_ID = "67e394570024164067d7"
    const val WEIGHT_HISTORY_COLLECTION_ID = "67e94c6a002909bd9379" // Korrigierte ID
    
    /**
     * Bucket-ID für Hundebilder im Storage
     */
    const val DOG_IMAGES_BUCKET_ID = "67d177e3002409c21b59"

    /**
     * Erstellt einen konfigurierten Appwrite-Client
     * 
     * @param context Android-Kontext, der für die Client-Erstellung benötigt wird
     * @return Ein vollständig konfigurierter Appwrite-Client, bereit für API-Aufrufe
     */
    fun createClient(context: Context): Client {
        return Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)
    }
}
