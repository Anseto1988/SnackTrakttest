package com.example.snacktrakt.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.snacktrakt.data.AppwriteConfig
import com.example.snacktrakt.data.AuthRepository
import com.example.snacktrakt.data.DogRepository
import com.example.snacktrakt.data.model.Dog // ++ Import Dog Model ++
import io.appwrite.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch // Import bleibt hier, auch wenn nicht direkt genutzt wird
import kotlinx.coroutines.withContext

class DailyCalorieResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object { private const val TAG = "DailyCalorieResetWorker" }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Worker starting...")
        try {
            val client = AppwriteConfig.createClient(applicationContext)
            val authRepository = AuthRepository(client)
            // ++ Korrekte Instanziierung von DogRepository mit 3 Argumenten ++
            val dogRepository = DogRepository(client, applicationContext, authRepository)

            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                Log.d(TAG, "User ${currentUser.id} found. Loading dogs...")
                // ++ Lade NUR die eigenen Hunde des Users über getAccessibleDogs und filtere ++
                //    ODER implementiere eine getDogsByOwnerId in DogRepository/DogDataRepository
                //    Wir filtern hier die Ergebnisse von getAccessibleDogs
                val allAccessibleDogs = dogRepository.getAccessibleDogs() // Holt alle (eigene + geteilte)
                val ownedDogs = allAccessibleDogs.filter { dog -> dog.ownerId == currentUser.id } // Filtere nur die eigenen

                Log.d(TAG, "${ownedDogs.size} owned dogs found.")
                if (ownedDogs.isNotEmpty()) {
                    Log.d(TAG, "Resetting calories for ${ownedDogs.size} dogs...")
                    var successCount = 0; var failureCount = 0
                    ownedDogs.forEach { dog: Dog -> // ++ Typ explizit angeben ++
                        try {
                            // Aufruf ist bereits in Coroutine (doWork)
                            val success = dogRepository.resetConsumedCalories(dog.id) // ++ dog.id ist korrekt ++
                            if (success) {
                                Log.d(TAG, "Calories reset for dog ${dog.id} (${dog.name})") // ++ dog.name ist korrekt ++
                                successCount++
                            } else {
                                Log.w(TAG, "Failed to reset calories for dog ${dog.id} (${dog.name})")
                                failureCount++
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resetting calories for dog ${dog.id}", e)
                            failureCount++
                        }
                    }
                    Log.d(TAG, "Calorie reset finished: $successCount succeeded, $failureCount failed.")
                    // Entscheide, ob Fehler zum Retry führen sollen
                    if (failureCount > 0) Result.retry() else Result.success()
                } else {
                    Log.d(TAG, "No owned dogs found for user ${currentUser.id}.")
                    Result.success()
                }
            } else {
                Log.d(TAG, "No logged in user. Worker finished.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyCalorieResetWorker", e)
            Result.retry() // Bei unerwarteten Fehlern erneut versuchen
        }
    }
}