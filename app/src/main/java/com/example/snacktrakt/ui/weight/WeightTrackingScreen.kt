package com.example.snacktrakt.ui.weight

// --- Standard Imports ---
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import für remember, mutableStateOf, LaunchedEffect, getValue, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch // Import für Coroutines
import com.example.snacktrakt.data.model.Dog // Optional, wenn Hund direkt angezeigt wird
import com.example.snacktrakt.data.model.WeightEntry
import com.example.snacktrakt.data.repository.WeightRepository // Für TimeRange Enum
import com.example.snacktrakt.ui.weight.components.WeightChart
import com.example.snacktrakt.ui.weight.components.WeightHistoryList
import com.example.snacktrakt.ui.weight.components.TimeRangeSelector
// Importiere den KORREKTEN ViewModel
import com.example.snacktrakt.ui.weight.WeightViewModel // ++ WICHTIG: WeightViewModel importieren ++
// Importiere NICHT DogViewModel für gewichtsspezifische Dinge!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackingScreen(
    dogId: String,
    onNavigateBack: () -> Unit,
    weightViewModel: WeightViewModel // ++ KORREKTEN ViewModel als Parameter ++
) {
    // --- States vom WeightViewModel abrufen ---
    // ++ Imports für getValue und collectAsState oben hinzufügen! ++
    val selectedDog by weightViewModel.currentDog.collectAsState()
    val weightEntries by weightViewModel.filteredEntries.collectAsState()
    // These are already Compose state variables, no need to collect
    val isLoading = weightViewModel.isLoading
    val selectedTimeRange = weightViewModel.selectedTimeRange
    val errorMessage = weightViewModel.errorMessage
    val showAddDialog = weightViewModel.showWeightInputDialog

    // --- Lokale States für den Dialog ---
    // Diese bleiben lokal, da sie nur während der Dialoganzeige relevant sind
    var newWeightInput by remember { mutableStateOf("") }
    var newNoteInput by remember { mutableStateOf("") }
    var dialogIsError by remember { mutableStateOf(false) }
    var dialogErrorMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Lade Daten beim Start / Wechsel der dogId
    LaunchedEffect(dogId) {
        weightViewModel.loadDog(dogId)
    }

    // Lade initialen Dialog-Wert, wenn Dialog geöffnet wird
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            newWeightInput = weightViewModel.weightInput // Hole aktuellen Wert vom VM
            newNoteInput = weightViewModel.noteInput
            dialogIsError = false // Fehler zurücksetzen
            dialogErrorMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gewicht ${selectedDog?.name ?: "..."}") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { /* ... Icon ... */ } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { weightViewModel.showWeightInput(selectedDog?.gewicht) }) {
                Icon(Icons.Default.Add, contentDescription = "Gewicht hinzufügen")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && selectedDog == null -> { // Initiales Laden des Hundes
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    // Fehleranzeige (zentriert)
                    Text(
                        "Fehler: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                selectedDog != null -> {
                    // Hauptinhalt anzeigen
                    WeightTrackingContent(
                        dog = selectedDog,
                        isLoadingHistory = isLoading, // Zeigt Ladeindikator im Chart/Liste
                        entries = weightEntries, // Gefilterte Einträge übergeben
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { weightViewModel.setTimeRange(it) }, // VM-Funktion
                        errorMessage = null // Fehler wird oben behandelt
                    )
                }
                else -> {
                    // Sollte nicht passieren, aber als Fallback
                    Text("Lade Daten...", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    // Dialog zum Hinzufügen (gesteuert durch VM-State)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { weightViewModel.dismissWeightInput() },
            title = { Text("Neues Gewicht") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWeightInput, // Lokaler State
                        onValueChange = {
                            newWeightInput = it
                            dialogIsError = false // Fehler zurücksetzen bei Eingabe
                        },
                        label = { Text("Gewicht (kg)") },
                        isError = dialogIsError,
                        supportingText = { if (dialogIsError) Text(dialogErrorMessage, color = MaterialTheme.colorScheme.error) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newNoteInput, // Lokaler State
                        onValueChange = { newNoteInput = it },
                        label = { Text("Notiz (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val weightDouble = newWeightInput.replace(',', '.').toDoubleOrNull()
                    if (weightDouble != null && weightDouble > 0) {
                        // VM Inputs aktualisieren *bevor* save aufgerufen wird
                        weightViewModel.updateWeightInput(newWeightInput)
                        weightViewModel.updateNoteInput(newNoteInput)
                        weightViewModel.saveWeight() // VM übernimmt Speichern & Schließen
                    } else {
                        dialogIsError = true // Lokalen Fehler im Dialog setzen
                        dialogErrorMessage = "Ungültiges Gewicht."
                    }
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { weightViewModel.dismissWeightInput() }) { Text("Abbrechen") }
            }
        )
    }
}

// --- WeightTrackingContent Composable (angepasst) ---
@Composable
private fun WeightTrackingContent(
    dog: Dog?, // Bleibt nullable, da er kurz null sein kann
    isLoadingHistory: Boolean, // Zeigt Ladezustand für Chart/List
    entries: List<WeightEntry>,
    selectedTimeRange: WeightRepository.TimeRange,
    onTimeRangeSelected: (WeightRepository.TimeRange) -> Unit,
    errorMessage: String? // Wird übergeben, um Fehler anzuzeigen
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Hund Info Card (unverändert)
        dog?.let { dog ->
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = dog.name, style = MaterialTheme.typography.headlineMedium)
                    Text("Aktuelles Gewicht: ${dog.gewicht} kg", style = MaterialTheme.typography.bodyLarge)
                    Text("Rasse: ${dog.rasse}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        TimeRangeSelector(
            selectedRange = selectedTimeRange,
            onRangeSelected = onTimeRangeSelected,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Chart/Liste oder Ladeanzeige/Fehler
        when {
            isLoadingHistory -> {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // Fehler wird jetzt außerhalb dieser Funktion behandelt
            // errorMessage != null -> { /* Error Card */ }
            else -> {
                // Chart nur anzeigen, wenn Einträge da sind
                if (entries.isNotEmpty()) {
                    WeightChart(entries = entries, modifier = Modifier.padding(bottom = 12.dp))
                } else {
                    // Optional: Meldung anzeigen, wenn keine Daten im Zeitraum
                    Text("Keine Gewichtsdaten für diesen Zeitraum.", modifier = Modifier.padding(vertical=32.dp).align(Alignment.CenterHorizontally))
                }
            }
        }

        WeightHistoryList(entries = entries, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(72.dp)) // Platz für FAB
    }
}