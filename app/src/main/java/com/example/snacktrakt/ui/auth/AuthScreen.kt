package com.example.snacktrakt.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity

/**
 * Authentifizierungsbildschirm für Anmeldung und Registrierung
 * 
 * Diese Compose-Komponente bietet:
 * - Ein Formular zur Anmeldung mit E-Mail und Passwort
 * - Ein Formular zur Registrierung mit Name, E-Mail und Passwort
 * - Einen Google-Login-Button für die OAuth-Authentifizierung
 * - Ein Umschalter zwischen Anmeldung und Registrierung
 * - Statusanzeigen für Lade- und Fehlerzustände
 * 
 * @param authViewModel ViewModel für die Authentifizierungsfunktionen
 * @param onNavigateToDashboard Callback für die Navigation zum Dashboard nach erfolgreicher Anmeldung
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    // Zustandsvariablen für die Eingabefelder
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }

    // Authentifizierungsstatus aus dem ViewModel
    val authState by authViewModel.authState.collectAsState()

    // Navigiere zum Dashboard, wenn der Benutzer authentifiziert ist
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    // Hauptlayout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Überschrift: Login oder Register
        Text(
            text = if (isLogin) "Login" else "Register",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // E-Mail-Eingabefeld
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Name-Eingabefeld (nur bei Registrierung)
        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Passwort-Eingabefeld
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Login- oder Register-Button
        Button(
            onClick = {
                if (isLogin) {
                    authViewModel.login(email, password)
                } else {
                    authViewModel.register(email, password, name)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLogin) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // Google Login Button
        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                if (context is ComponentActivity) {
                    authViewModel.loginWithGoogle(context)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mit Google anmelden")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Umschalter zwischen Anmeldung und Registrierung
        TextButton(
            onClick = { isLogin = !isLogin }
        ) {
            Text(if (isLogin) "Need an account? Register" else "Have an account? Login")
        }

        // Ladeindikator während der Authentifizierung
        if (authState is AuthState.Loading) {
            CircularProgressIndicator()
        }

        // Fehleranzeige
        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
