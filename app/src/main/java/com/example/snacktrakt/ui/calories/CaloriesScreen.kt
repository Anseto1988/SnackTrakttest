package com.example.snacktrakt.ui.calories

// Imports unverändert zur letzten Version, stelle sicher, dass alle da sind
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Wird benötigt? Falls nicht, entfernen.
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.ui.dogs.DogViewModel
import com.example.snacktrakt.ui.components.BarcodeScanner
import com.example.snacktrakt.ui.futter.EANSearchState
import com.example.snacktrakt.ui.futter.EinreichungScreen
import com.example.snacktrakt.ui.futter.FutterDBViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.isGranted
import android.Manifest
import com.example.snacktrakt.ui.calories.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CaloriesScreen(
    onNavigateBack: () -> Unit,
    dogViewModel: DogViewModel,
    futterDBViewModel: FutterDBViewModel
) {
    // States unverändert zur letzten Version
    val selectedDog by dogViewModel.selectedDog.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var calories by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var gramm by remember { mutableStateOf("") }
    var saveCompleted by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showScanner by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var showEinreichungScreen by remember { mutableStateOf(false) }
    val eanSearchState by futterDBViewModel.eanSearchState.collectAsState()
    val currentFutter by futterDBViewModel.currentFutter.collectAsState()

    // LaunchedEffects für Reset, EAN-Suche, Snackbar (unverändert)
    // ...

    if (showEinreichungScreen && scannedBarcode != null) {
        // EinreichungScreen (unverändert)
        // ...
    } else {
        if (showNotFoundDialog) {
            // ProductNotFoundDialog (unverändert)
            // ...
        }

        Scaffold(
            topBar = { /* Unverändert */ },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            if (showScanner) {
                // BarcodeSection (unverändert)
                // ...
            } else {
                Column( /* ... Modifier ... */ ) {
                    // Anzeige des Hundes (unverändert)
                    // ...

                    // Input Section mit allen erforderlichen Parametern
                    CaloriesInputSection(
                        description = description,
                        onDescriptionChange = { description = it },
                        calories = calories,
                        onCaloriesChange = { calories = it },
                        gramm = gramm,
                        onGrammChange = { gramm = it },
                        scannedBarcode = scannedBarcode,
                        currentFutter = currentFutter,
                        isLoading = isLoading,
                        isEnabled = selectedDog != null,
                        onScanRequest = { showScanner = true }
                    )

                    // Futter Info Card (unverändert)
                    currentFutter?.let { futter -> FutterInfoCard(futter = futter) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button zum Speichern
                    SaveButton(
                        isLoading = isLoading,
                        enabled = !isLoading && selectedDog != null &&
                                calories.isNotEmpty() && description.isNotEmpty(),
                        onClick = {
                            if (selectedDog != null && calories.isNotEmpty() && description.isNotEmpty()) {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        val caloriesValue = calories.toIntOrNull() ?: 0
                                        // == KORREKTUR / PRÜFUNG ==
                                        // Stelle sicher, dass dogViewModel.addFutter existiert und korrekt aufgerufen wird.
                                        // Wenn 'Unresolved reference' hier weiterhin auftritt: Clean & Rebuild!
                                        dogViewModel.addFutter(
                                            description = description,
                                            calories = caloriesValue
                                        )
                                        saveCompleted = true
                                    } catch (e: Exception) {
                                        // Fehlerbehandlung (unverändert)
                                        // ...
                                        isLoading = false
                                    }
                                }
                            } else {
                                // Snackbar für fehlende Felder (unverändert)
                                // ...
                            }
                        }
                    )
                }
            }
        }
    }
}