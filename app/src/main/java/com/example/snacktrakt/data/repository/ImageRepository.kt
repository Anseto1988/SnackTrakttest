package com.example.snacktrakt.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository für die Verwaltung von Bildern mit Appwrite Storage
 * 
 * Diese Klasse kapselt alle Datenbankzugriffe für:
 * - Upload von Bildern (z.B. Hundebilder)
 * - Abruf von Bild-URLs
 * 
 * @param client Appwrite Client für API-Zugriffe
 */
class ImageRepository(private val client: Client) {
    private val storage = Storage(client)
    
    companion object {
        const val DOG_IMAGES_BUCKET_ID = "67d177e3002409c21b59"  // dog_images
        private const val TAG = "ImageRepository"
    }
    
    /**
     * Generiert eine URL für ein Vorschaubild mit den angegebenen Dimensionen
     *
     * @param fileId ID des Bildes im Storage
     * @param width Breite des Vorschaubildes
     * @param height Höhe des Vorschaubildes
     * @return URL zum Bild mit den angegebenen Dimensionen
     */
    suspend fun getImagePreviewUrl(fileId: String, width: Int, height: Int): String {
        // Appwrite benötigt Long für width und height
        val previewUrl = storage.getFilePreview(
            bucketId = DOG_IMAGES_BUCKET_ID,
            fileId = fileId,
            width = width.toLong(),
            height = height.toLong()
        )
        // URL für das Vorschaubild zurückgeben
        return previewUrl.toString()
    }
    
    /**
     * Lädt ein Bild in den Appwrite Storage hoch
     * 
     * @param imageUri URI des hochzuladenden Bildes
     * @param bucketId ID des Buckets, in den das Bild hochgeladen werden soll
     * @param context Context für den Dateizugriff
     * @return ID des hochgeladenen Bildes
     */
    suspend fun uploadImage(
        imageUri: Uri, 
        bucketId: String = DOG_IMAGES_BUCKET_ID,
        context: Context
    ): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        
        // Kopiere Eingabe-Stream in Datei
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        
        // Lade Datei in Storage hoch
        try {
            val response = storage.createFile(
                bucketId = bucketId,
                fileId = ID.unique(),
                file = InputFile.fromFile(file)
            )
            
            // Lösche temporäre Datei
            file.delete()
            
            Log.d(TAG, "Bild hochgeladen mit ID: ${response.id}")
            return@withContext response.id
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Hochladen des Bildes", e)
            // Lösche temporäre Datei im Fehlerfall
            file.delete()
            throw e
        }
    }
    
    /**
     * Ruft die Vorschau-URL für ein Bild ab
     * 
     * @param fileId ID des Bildes
     * @param bucketId ID des Buckets, in dem das Bild gespeichert ist
     * @param width Breite der Vorschau
     * @param height Höhe der Vorschau
     * @return Vorschau-URL des Bildes
     */
    fun getImagePreviewUrl(
        fileId: String, 
        bucketId: String = DOG_IMAGES_BUCKET_ID, 
        width: Int = 400, 
        height: Int = 400
    ): String {
        // Bericht, wenn fileId leer ist
        if (fileId.isBlank()) {
            Log.w(TAG, "getImagePreviewUrl: Leere fileId übergeben")
            return ""
        }

        val projectId = client.config["project"] ?: throw IllegalStateException("Project ID not found in client config")
        return "${client.endpoint}/storage/buckets/$bucketId/files/$fileId/preview?project=$projectId&width=$width&height=$height&gravity=center"
    }
    
    /**
     * Löscht ein Bild aus dem Storage
     * 
     * @param fileId ID des zu löschenden Bildes
     * @param bucketId ID des Buckets, in dem das Bild gespeichert ist
     */
    suspend fun deleteImage(fileId: String, bucketId: String = DOG_IMAGES_BUCKET_ID): Boolean = withContext(Dispatchers.IO) {
        if (fileId.isBlank()) {
            Log.w(TAG, "deleteImage: Leere fileId übergeben")
            return@withContext false
        }
        
        try {
            storage.deleteFile(bucketId = bucketId, fileId = fileId)
            Log.d(TAG, "Bild mit ID $fileId gelöscht")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Löschen des Bildes mit ID $fileId", e)
            return@withContext false
        }
    }
}
