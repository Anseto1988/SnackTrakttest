package com.example.snacktrakt.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.repository.WeightRepository
import com.example.snacktrakt.ui.weight.components.TimeRangeSelector
import com.example.snacktrakt.ui.weight.components.WeightChart
import com.example.snacktrakt.ui.weight.components.WeightEntryDialog
import com.example.snacktrakt.ui.weight.components.WeightHistoryList

/**
 * Hauptbildschirm für die Gewichtsverfolgung
 *
 * Zeigt einen Verlaufsdiagramm und eine Liste der Gewichtseinträge an.
 * Ermöglicht das Hinzufügen neuer Gewichtsmessungen und das Filtern nach Zeiträumen.
 *
 * @param viewModel ViewModel für die Gewichtsdaten
 * @param dogId ID des Hundes, dessen Gewichtsdaten angezeigt werden sollen
 * @param onNavigateBack Callback für die Navigation zurück
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    viewModel: WeightViewModel,
    dogId: String,
    onNavigateBack: () -> Unit
) {
    // Daten aus dem ViewModel laden
    val dog by viewModel.currentDog.collectAsState()
    val weightEntries by viewModel.filteredEntries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Daten beim ersten Anzeigen laden
    LaunchedEffect(dogId) {
        viewModel.loadDog(dogId)
    }
    
    // Fehler anzeigen, wenn vorhanden
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }
    
    // Bildschirm-Layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Gewichtsverlauf: ${dog?.name ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showWeightInput(dog?.gewicht) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Hinzufügen") },
                text = { Text("Neues Gewicht") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Anzeige während des Ladens
            if (viewModel.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Daten werden geladen...",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else if (dog == null) {
                // Anzeige, wenn kein Hund geladen werden konnte
                Text(
                    text = "Hund konnte nicht geladen werden",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // Angezeigte Informationen, wenn ein Hund geladen wurde
                
                // Aktuelle Gewichtsinformation
                dog?.gewicht?.let { currentWeight ->
                    Text(
                        text = "Aktuelles Gewicht: $currentWeight kg",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                } ?: Text(
                    text = "Kein Gewicht eingetragen",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Zeitraumauswahl
                TimeRangeSelector(
                    selectedRange = viewModel.selectedTimeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )
                
                // Gewichtsdiagramm
                WeightChart(
                    entries = weightEntries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                
                // Liste der Gewichtseinträge
                WeightHistoryList(
                    entries = weightEntries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
    
    // Dialog zur Gewichtseingabe
    if (viewModel.showWeightInputDialog) {
        WeightEntryDialog(
            initialWeight = viewModel.weightInput,
            onWeightChange = { viewModel.updateWeightInput(it) },
            note = viewModel.noteInput,
            onNoteChange = { viewModel.updateNoteInput(it) },
            onDismiss = { viewModel.dismissWeightInput() },
            onConfirm = { viewModel.saveWeight() },
            isLoading = viewModel.isLoading
        )
    }
}
