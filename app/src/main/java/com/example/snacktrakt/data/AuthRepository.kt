package com.example.snacktrakt.data

import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Teams // Import hinzugefügt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.User // Import hinzugefügt
import io.appwrite.models.Team // Import hinzugefügt
import android.util.Log

@Singleton
class AuthRepository @Inject constructor(
    private val client: Client
) {
    private val account = Account(client)
    private val teams = Teams(client) // Teams Service hinzugefügt

    suspend fun createUser(email: String, pass: String, name: String): User<Map<String, Any>>? {
        return try {
            withContext(Dispatchers.IO) {
                // ID.unique() verwenden, um sicherzustellen, dass die Benutzer-ID eindeutig ist
                account.create(io.appwrite.ID.unique(), email, pass, name)
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler beim Erstellen des Benutzers: ${e.message}")
            null
        }
    }

    suspend fun createSession(email: String, pass: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                account.createEmailPasswordSession(email, pass)
                true // Session erfolgreich erstellt
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler beim Erstellen der Session: ${e.message}")
            false // Fehler beim Erstellen der Session
        }
    }
    
    // Alias für createSession für die Kompatibilität mit AuthViewModel
    suspend fun login(email: String, password: String): Boolean {
        return createSession(email, password)
    }


    suspend fun getCurrentUser(): User<Map<String, Any>>? {
        return try {
            withContext(Dispatchers.IO) {
                account.get()
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler beim Abrufen des Benutzers: ${e.message}")
            null // Kein Benutzer angemeldet oder Fehler
        }
    }

    fun getCurrentUserId(): String? {
        // Diese Methode sollte synchron bleiben, wenn sie im ViewModel direkt verwendet wird,
        // aber idealerweise holt man den User asynchron und speichert die ID.
        // Für die *sichere* Variante, nutze eine suspend-Funktion oder StateFlow.
        // **Vorsicht:** Dies kann blockieren oder null zurückgeben, wenn nicht im richtigen Kontext aufgerufen.
        // Besser wäre es, den User im ViewModel zu halten und dessen ID zu verwenden.
        // Hier *provisorisch* für die bestehende Struktur:
        return try {
            // ACHTUNG: Dies ist ein synchroner Aufruf und kann in Hauptthread Probleme verursachen.
            // Sicherer wäre es, `getCurrentUser()` (suspend) zu verwenden und die ID im ViewModel zu speichern.
            // Da der Rest des Codes es synchron erwartet, lassen wir es vorerst, aber markieren es als TODO.
            kotlinx.coroutines.runBlocking { account.get().id } // Provisorisch synchron, nicht empfohlen!
        } catch (e: Exception) {
            Log.e("AuthRepository", "Konnte User-ID nicht synchron holen: ${e.message}")
            null
        }
    }


    // NEUE FUNKTION zum Abrufen der Team-ID des Benutzers
    // Passe die Implementierung an, wie du die Team-ID speicherst!
    suspend fun getCurrentUserTeamId(): String? {
        return try {
            withContext(Dispatchers.IO) {
                // Beispiel: Annahme, die Team-ID ist in den Benutzer-Prefs gespeichert
                // val userPrefs = account.getPrefs()
                // userPrefs.data["teamId"] as? String

                // Alternative: Hole die Team-Mitgliedschaften des Benutzers
                // Nimm die ID des ersten Teams (oder implementiere eine Logik zur Auswahl des "primären" Teams)
                val teamList = teams.list() // Liste *aller* Teams, zu denen der User gehört
                if (teamList.teams.isNotEmpty()) {
                    // Hier nehmen wir einfach das erste Team. Passe das an deine Logik an!
                    // Wichtig: Der Benutzer muss Mitglied des Teams sein!
                    teamList.teams.first().id
                } else {
                    Log.w("AuthRepository", "Benutzer ist in keinem Team.")
                    null // Benutzer ist in keinem Team
                }

                // Andere Möglichkeit: Wenn die Team-ID direkt im Benutzerobjekt (prefs) gespeichert ist:
                // val user = account.get()
                // user.prefs.data["teamId"] as? String // Wenn du ein Pref namens "teamId" hast
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler beim Abrufen der Team-ID: ${e.message}")
            null
        }
    }


    suspend fun logout() {
        try {
            withContext(Dispatchers.IO) {
                // Löscht die aktuelle Session
                account.deleteSession("current")
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler beim Logout: ${e.message}")
            // Optional: Fehlerbehandlung für den Benutzer
        }
    }

    // Funktion zum Überprüfen, ob ein Benutzer angemeldet ist
    suspend fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
    
    // Google OAuth Session erstellen
    suspend fun createGoogleOAuthSession(activity: androidx.activity.ComponentActivity): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // OAuth2 Session mit Google erstellen
                // Hier werden die Scopes für E-Mail und Profilinformationen angefordert
                account.createOAuth2Session(
                    activity = activity,
                    provider = io.appwrite.enums.OAuthProvider.GOOGLE,
                    success = "appwrite-callback-${AppwriteConfig.PROJECT_ID}://",
                    failure = "appwrite-callback-${AppwriteConfig.PROJECT_ID}://",
                    scopes = listOf("email", "profile")
                )
                true
            }
        } catch (e: AppwriteException) {
            Log.e("AuthRepository", "Fehler bei Google OAuth: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("AuthRepository", "Allgemeiner Fehler bei Google OAuth: ${e.message}")
            false
        }
    }
    
    // Konto erstellen - Alias für createUser für Kompatibilität mit AuthViewModel
    suspend fun createAccount(email: String, password: String, name: String): Boolean {
        return try {
            val user = createUser(email, password, name)
            user != null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Fehler beim Erstellen des Kontos: ${e.message}")
            false
        }
    }
}