package com.example.snacktrakt.ui.auth

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.snacktrakt.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sealed Class für die verschiedenen Authentifizierungszustände
 */
sealed class AuthState {
    /** Initialer Zustand beim Start der App */
    object Initial : AuthState()
    
    /** Ladezustand während Authentifizierungsvorgängen */
    object Loading : AuthState()
    
    /** Benutzer ist authentifiziert */
    object Authenticated : AuthState()
    
    /** Benutzer ist nicht authentifiziert */
    object Unauthenticated : AuthState()
    
    /** Fehler bei einem Authentifizierungsvorgang */
    data class Error(val message: String) : AuthState()
}

/**
 * ViewModel für die Authentifizierungsfunktionen der App
 * 
 * Diese Klasse verwaltet:
 * - Die Benutzeranmeldung (E-Mail/Passwort und Google OAuth)
 * - Die Benutzerregistrierung
 * - Den aktuellen Authentifizierungsstatus
 * - Abmeldung des Benutzers
 * 
 * Der Authentifizierungszustand wird über einen StateFlow bereitgestellt,
 * sodass UI-Komponenten auf Änderungen reagieren können.
 * 
 * @param authRepository Repository für Authentifizierungsvorgänge
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // Zustand der Authentifizierung
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Boolean Flow für einfachen Login-Status Check
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Überprüft den aktuellen Authentifizierungsstatus des Benutzers
     * 
     * Diese Methode wird beim Start der App aufgerufen und setzt den
     * AuthState auf Authenticated oder Unauthenticated, je nachdem
     * ob ein gültiger Benutzer vorhanden ist.
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    _authState.value = AuthState.Authenticated
                    _isLoggedIn.value = true
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to check auth status")
                _isLoggedIn.value = false
            }
        }
    }

    /**
     * Meldet einen Benutzer mit E-Mail und Passwort an
     * 
     * @param email E-Mail-Adresse des Benutzers
     * @param password Passwort des Benutzers
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.login(email, password)
                _authState.value = AuthState.Authenticated
                _isLoggedIn.value = true // Ensure isLoggedIn is updated on successful login
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
                _isLoggedIn.value = false
            }
        }
    }
    
    /**
     * Startet den Google OAuth-Anmeldeprozess
     * 
     * Diese Methode leitet den Benutzer zur Google-Anmeldeseite weiter.
     * Nach erfolgreicher Anmeldung wird der Benutzer zurück zur App geleitet
     * und der Authentifizierungsstatus wird aktualisiert.
     * 
     * @param activity Die aktuelle Activity für den OAuth-Redirect
     */
    fun loginWithGoogle(activity: ComponentActivity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.createGoogleOAuthSession(activity)
                // Der Authentifizierungsstatus wird automatisch aktualisiert, wenn
                // der Benutzer von der Google-Anmeldung zurückkehrt
                checkAuthStatus()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google login failed")
                _isLoggedIn.value = false
            }
        }
    }

    /**
     * Registriert einen neuen Benutzer mit E-Mail, Passwort und Namen
     * 
     * @param email E-Mail-Adresse des neuen Benutzers
     * @param password Passwort des neuen Benutzers
     * @param name Anzeigename des neuen Benutzers
     */
    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.createAccount(email, password, name)
                authRepository.login(email, password)
                _authState.value = AuthState.Authenticated
                _isLoggedIn.value = true // Ensure isLoggedIn is updated on successful registration
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
                _isLoggedIn.value = false
            }
        }
    }

    /**
     * Meldet den aktuellen Benutzer ab
     */
    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.logout()
                _authState.value = AuthState.Unauthenticated
                _isLoggedIn.value = false
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Logout failed")
            }
        }
    }
}
