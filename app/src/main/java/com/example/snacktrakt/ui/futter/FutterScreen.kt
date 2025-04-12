package com.example.snacktrakt.ui.futter

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.model.Futter
import com.example.snacktrakt.ui.dogs.DogViewModel
import com.example.snacktrakt.ui.futter.FutterDBViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutterScreen(
    onNavigateBack: () -> Unit,
    dogViewModel: DogViewModel,
    futterDBViewModel: FutterDBViewModel // Parameter beibehalten für zukünftige Funktionalität
) {
    val selectedDog by dogViewModel.selectedDog.collectAsState()
    val futterState by dogViewModel.futterState.collectAsState()

    var refreshTrigger by remember { mutableStateOf(0) }

    // Initialisierung und Daten laden
    LaunchedEffect(Unit) {
        Log.d("FutterScreen", "Initialisiert")
    }

    LaunchedEffect(refreshTrigger, selectedDog) {
        selectedDog?.let { dog ->
            dogViewModel.loadFutterForDog(dog.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedDog?.let { "Mahlzeiten für ${it.name}" } ?: "Mahlzeiten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedDog?.let { dog ->
                                dogViewModel.loadUserDogs()
                                dogViewModel.loadFutterForDog(dog.id)
                                refreshTrigger++
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            selectedDog?.let { dog ->
                // Hund Info Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = dog.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rasse: ${dog.rasse}")
                        dog.gewicht?.let { Text("Gewicht: $it kg") }
                        dog.kalorienbedarf?.let { Text("Kalorienbedarf: $it kcal") }
                    }
                }

                // Container für Futterliste
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (val state = futterState) {
                        is DogViewModel.FutterState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        is DogViewModel.FutterState.Success -> {
                            val futterList = state.futterList
                            if (futterList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Keine Mahlzeiten gefunden",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                dogViewModel.loadFutterForDog(dog.id)
                                                refreshTrigger++
                                            }
                                        ) {
                                            Text("Neu laden")
                                        }
                                    }
                                }
                            } else {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Letzte Mahlzeiten",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        TextButton(
                                            onClick = {
                                                dogViewModel.loadFutterForDog(dog.id)
                                                refreshTrigger++
                                            }
                                        ) {
                                            Text("Aktualisieren")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyColumn {
                                        items(
                                            items = futterList,
                                            key = { it.id }
                                        ) { futterItem ->
                                            FutterListItem(futter = futterItem)
                                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                                        }
                                    }
                                }
                            }
                        }

                        is DogViewModel.FutterState.Error -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Fehler: ${state.message}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            dogViewModel.loadFutterForDog(dog.id)
                                            refreshTrigger++
                                        }
                                    ) {
                                        Text("Neu versuchen")
                                    }
                                }
                            }
                        }

                        is DogViewModel.FutterState.Initial -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            } ?: run {
                // Meldung wenn kein Hund ausgewählt
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Bitte wählen Sie einen Hund aus",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/**
 * Zeigt einen Futtereintrag mit Namen, Datum und Kalorien an
 */
@Composable
fun FutterListItem(futter: Futter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = futter.name,
            style = MaterialTheme.typography.titleMedium
        )

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        Text(
            text = "Datum: ${dateFormat.format(futter.date)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Kalorien: ${futter.calories} kcal",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}