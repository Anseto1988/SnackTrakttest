package com.example.snacktrakt.ui.dashboard

import android.content.Context // Unbenutzt, kann entfernt werden
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Logout // Import für Logout-Icon
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Unbenutzt, kann entfernt werden
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.model.Dog // Import Dog
import com.example.snacktrakt.ui.dogs.DogCard
import com.example.snacktrakt.ui.dogs.DogViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // Für Logout-Scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCalories: () -> Unit,
    onNavigateToFutter: () -> Unit,
    onNavigateToEditDog: (String) -> Unit,
    onNavigateToCreateDog: () -> Unit,
    onNavigateToWeightTracking: (String) -> Unit,
    onLogout: () -> Unit, // Callback bleibt bestehen
    dogViewModel: DogViewModel
) {
    val dogState by dogViewModel.dogState.collectAsState()
    val selectedDog by dogViewModel.selectedDog.collectAsState()
    val coroutineScope = rememberCoroutineScope() // Für manuelle Refreshs

    // Diese Variable wird verwendet, um die Aktualisierung manuell anzustoßen
    var refreshTrigger by remember { mutableIntStateOf(0) } // Besser mutableIntStateOf für Trigger

    // Automatisch Hunde laden, wenn der Screen angezeigt wird oder refreshTrigger sich ändert
    LaunchedEffect(key1 = Unit, key2 = refreshTrigger) {
        // Verwende die Funktion im ViewModel zum Laden
        coroutineScope.launch { // Start in Coroutine, falls ViewModel suspend verwendet
            dogViewModel.loadUserDogs()
        }
    }

    // Periodische Aktualisierung (optional, überdenken ob wirklich nötig/effizient)
    // LaunchedEffect(key1 = Unit) {
    //     while (true) { // Endlosschleife für periodisches Update
    //         delay(60000) // 1 Minute warten
    //         Log.d("DashboardScreen", "Periodically refreshing dogs...")
    //         coroutineScope.launch { dogViewModel.loadUserDogs() }
    //         // Vorsicht: Dies kann Batterie verbrauchen und unnötige API-Aufrufe verursachen.
    //         // Besser ist oft ein Pull-to-Refresh oder Aktualisierung bei Fokus.
    //     }
    // }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meine Hunde") }, // Angepasster Titel
                actions = {
                    // Aktualisierungsbutton
                    IconButton(onClick = {
                        refreshTrigger++ // Löst LaunchedEffect aus
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }

                    // Abmelde-Button (ruft den Callback auf)
                    IconButton(onClick = onLogout) { // Verwende IconButton für Konsistenz
                        Icon(Icons.Default.Logout, contentDescription = "Abmelden")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB zum Hinzufügen eines neuen Hundes
            FloatingActionButton(onClick = onNavigateToCreateDog) {
                Icon(Icons.Filled.Add, contentDescription = "Neuen Hund hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // Padding vom Scaffold anwenden
                .padding(horizontal = 16.dp), // Zusätzliches horizontales Padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status der Hundedaten anzeigen
            when (val currentState = dogState) {
                is DogViewModel.DogState.Loading -> {
                    // Ladeanzeige zentriert
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                    }
                }
                is DogViewModel.DogState.Success -> {
                    val dogsList = currentState.dogs
                    if (dogsList.isEmpty()) {
                        // Keine Hunde vorhanden: Freundliche Nachricht und Button
                        Box(
                            modifier = Modifier.fillMaxSize(), // Füllt den verfügbaren Platz
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 60.dp) // Platz über FAB
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Noch keine Hunde",
                                    style = MaterialTheme.typography.headlineSmall // Etwas kleiner
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Füge deinen ersten Hund hinzu!",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                // Button zum Hinzufügen, alternativ zum FAB
                                FilledTonalButton(onClick = onNavigateToCreateDog) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Hund hinzufügen")
                                }
                            }
                        }
                    } else {
                        // Hunde vorhanden: Liste anzeigen
                        // Button zum Hinzufügen von Kalorien (optional hier, oft besser im Detail)
                        /* Button(
                            onClick = onNavigateToCalories,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp), // Oben und unten Padding
                            enabled = selectedDog != null
                        ) {
                            Icon(Icons.Default.Fastfood, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDog?.let { "Mahlzeit für ${it.name} hinzufügen" }
                                      ?: "Wähle einen Hund aus"
                            )
                        }
                        */

                        // Liste der Hunde
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(), // Füllt den Rest des Screens
                            contentPadding = PaddingValues(vertical = 16.dp), // Padding für die Liste
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Abstand zwischen Karten
                        ) {
                            items(dogsList, key = { dog -> dog.id }) { dog ->
                                // ++ Korrekter Aufruf von getDogImageUrl aus ViewModel ++
                                val imageUrl = dog.imageFileId?.let { fileId ->
                                    // Verwende die Funktion im ViewModel
                                    dogViewModel.getDogImageUrl(fileId, width=100, height=100) // Kleinere Größe für Liste
                                }
                                DogCard(
                                    dog = dog,
                                    imageUrl = imageUrl,
                                    isSelected = selectedDog?.id == dog.id,
                                    onClick = {
                                        dogViewModel.selectDog(dog)
                                        // Optional: Direkt zur Detailansicht navigieren statt nur auswählen?
                                        // onNavigateToEditDog(dog.id)
                                    },
                                    onMahlzeitenClick = {
                                        dogViewModel.selectDog(dog) // Sicherstellen, dass Hund ausgewählt ist
                                        onNavigateToFutter() // Navigiere zur Futterübersicht für diesen Hund
                                    },
                                    onEditClick = {
                                        onNavigateToEditDog(dog.id) // Navigiere zum Bearbeiten
                                    },
                                    onWeightTrackingClick = {
                                        onNavigateToWeightTracking(dog.id) // Navigiere zur Gewichtserfassung
                                    }
                                )
                            }
                        }
                    }
                }
                is DogViewModel.DogState.Error -> {
                    // Fehlermeldung anzeigen
                    val errorMessage = currentState.message
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Fehler beim Laden:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { refreshTrigger++ }) { // Erneut versuchen über Trigger
                                Text("Erneut versuchen")
                            }
                        }
                    }
                }
                else -> {
                    // Initialzustand oder anderer unerwarteter Zustand
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Initialisiere...") // Oder eine andere passende Meldung
                    }
                }
            }
        }
    }
}