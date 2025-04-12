package com.example.snacktrakt // << Hauptpaket

import android.app.Application
import android.util.Log
import androidx.work.*
import com.example.snacktrakt.worker.DailyCalorieResetWorker // ++ Importiere den Worker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Haupt-Application Klasse.
 * Hier wird der tägliche Worker für den Kalorien-Reset geplant.
 */
class MainApplication : Application() {

    companion object {
        private const val TAG = "MainApplication"
        private const val UNIQUE_WORK_NAME = "DailyCalorieResetWork"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate()")
        scheduleDailyResetWorker()
    }

    private fun scheduleDailyResetWorker() {
        Log.d(TAG, "Scheduling DailyCalorieResetWorker...")

        val workManager = WorkManager.getInstance(this)

        // Berechne die Verzögerung bis zum nächsten Mitternacht
        val currentTime = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1) // Zum nächsten Tag wechseln
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Sicherstellen, dass die Verzögerung nicht negativ ist, falls die App genau um Mitternacht startet
        var initialDelayMillis = nextMidnight.timeInMillis - currentTime.timeInMillis
        if (initialDelayMillis < 0) {
            initialDelayMillis += TimeUnit.DAYS.toMillis(1) // Füge 24 Stunden hinzu, wenn die Zeit schon vorbei ist
            Log.d(TAG, "Calculated initial delay was negative, adding 24 hours.")
        }


        Log.d(TAG, "Current time: ${currentTime.time}")
        Log.d(TAG, "Next midnight: ${nextMidnight.time}")
        Log.d(TAG, "Initial delay: $initialDelayMillis ms (${TimeUnit.MILLISECONDS.toMinutes(initialDelayMillis)} min)")

        // Erstelle die periodische Arbeitsanforderung
        val dailyResetWorkRequest = PeriodicWorkRequestBuilder<DailyCalorieResetWorker>(
            repeatInterval = 1, // Wiederhole jeden Tag
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            // Optional: Constraints hinzufügen (z.B. Netzwerk erforderlich, Akku nicht niedrig)
            .setConstraints(Constraints.Builder()
                // .setRequiredNetworkType(NetworkType.CONNECTED) // Optional: Nur wenn Netzwerk da ist
                .build())
            .addTag(UNIQUE_WORK_NAME) // Tag zur Identifizierung
            .build()

        // Worker in die Warteschlange einreihen (oder bestehenden beibehalten)
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // KEEP: Wenn Arbeit existiert, nichts tun. REPLACE: Ersetzen.
            dailyResetWorkRequest
        )

        // Status überprüfen (optional für Debugging)
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME)
            .observeForever { workInfos ->
                if (workInfos != null && workInfos.isNotEmpty()) {
                    val workInfo = workInfos[0]
                    // Vermeide das Loggen von Observer-Details direkt, um Memory Leaks zu verhindern
                    // wenn der Observer nicht entfernt wird (was hier mit observeForever der Fall ist)
                    Log.d(TAG, "Work State: ${workInfo.state}")
                } else {
                    Log.d(TAG, "Work '$UNIQUE_WORK_NAME' not found or list empty.")
                }
            }


        Log.d(TAG, "DailyCalorieResetWorker enqueued with initial delay of ${initialDelayMillis / (1000 * 60)} minutes.")
    }
}