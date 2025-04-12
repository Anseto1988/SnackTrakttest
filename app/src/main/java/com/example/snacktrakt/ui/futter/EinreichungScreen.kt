package com.example.snacktrakt.ui.futter

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAG = "EinreichungScreen"

/**
 * Bildschirm zur Einreichung eines neuen Futterprodukts in die Datenbank
 *
 * Ermöglicht dem Benutzer die Eingabe aller relevanten Informationen zu einem
 * Futterprodukt, das noch nicht in der Datenbank existiert.
 *
 * @param ean Die EAN-Nummer des Produkts (wird vom Barcode-Scanner übergeben)
 * @param onNavigateBack Callback-Funktion für die Navigation zurück
 * @param viewModel ViewModel für die Interaktion mit der Futterdatenbank
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EinreichungScreen(
    ean: String,
    onNavigateBack: () -> Unit,
    viewModel: FutterDBViewModel
) {
    // Keyboard-Controller für automatisches Ausblenden der Tastatur
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Scrollstate für den Bildschirminhalt
    val scrollState = rememberScrollState()
    
    // Status der Einreichung aus dem ViewModel beobachten
    val einreichungsState by viewModel.einreichungsState.collectAsState()
    
    // Referenzdaten laden
    val kategorien by viewModel.kategorien.collectAsState()
    val unterkategorien by viewModel.unterkategorien.collectAsState()
    val lebensphasen by viewModel.lebensphasen.collectAsState()
    val herstellerVorschlaege by viewModel.herstellerVorschlaege.collectAsState()
    
    // Lokale Zustandsvariablen für die Formularwerte
    var hersteller by remember { mutableStateOf("") }
    var showHerstellerSuggestions by remember { mutableStateOf(false) }
    
    var selectedKategorie by remember { mutableStateOf("") }
    var expandedKategorie by remember { mutableStateOf(false) }
    
    var selectedUnterkategorie by remember { mutableStateOf("") }
    var expandedUnterkategorie by remember { mutableStateOf(false) }
    
    var produktname by remember { mutableStateOf("") }
    
    var selectedLebensphase by remember { mutableStateOf("") }
    var expandedLebensphase by remember { mutableStateOf(false) }
    
    var protein by remember { mutableStateOf("") }
    var fett by remember { mutableStateOf("") }
    var rohasche by remember { mutableStateOf("") }
    var rohfaser by remember { mutableStateOf("") }
    var feuchtigkeit by remember { mutableStateOf("") }
    
    // Berechnete Werte
    var kcalPro100g by remember { mutableStateOf(0) }
    var nfe by remember { mutableStateOf(0) }
    
    // Status-Variablen für die UI
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // FocusRequester für das Hersteller-Eingabefeld
    val herstellerFocusRequester = remember { FocusRequester() }
    
    // SnackbarHostState für Benachrichtigungen
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Überwache den Einreichungsstatus
    LaunchedEffect(einreichungsState) {
        when (einreichungsState) {
            is EinreichungsState.Success -> {
                isLoading = false
                showSuccessDialog = true
                
                // Zeige Erfolgsmeldung für 3 Sekunden, dann gehe zurück
                delay(3000)
                viewModel.resetEinreichungsState()
                onNavigateBack()
            }
            
            is EinreichungsState.Error -> {
                isLoading = false
                errorMessage = (einreichungsState as EinreichungsState.Error).message
                showError = true
                
                snackbarHostState.showSnackbar(
                    message = "Fehler: $errorMessage",
                    actionLabel = "OK",
                    duration = SnackbarDuration.Long
                )
                
                viewModel.resetEinreichungsState()
            }
            
            is EinreichungsState.Submitting -> {
                isLoading = true
            }
            
            else -> { /* Keine Aktion für Initial */ }
        }
    }
    
    // Aktualisiere die berechneten Werte (kcalPro100g und NFE), wenn sich Nährwertangaben ändern
    LaunchedEffect(protein, fett, rohasche, rohfaser, feuchtigkeit) {
        val proteinValue = protein.toIntOrNull() ?: 0
        val fettValue = fett.toIntOrNull() ?: 0
        val rohascheValue = rohasche.toIntOrNull() ?: 0
        val rohfaserValue = rohfaser.toIntOrNull() ?: 0
        val feuchtigkeitValue = feuchtigkeit.toIntOrNull() ?: 0
        
        // Prüfe, ob alle Werte gültig sind
        if (protein.isNotEmpty() && fett.isNotEmpty() && rohasche.isNotEmpty() &&
            rohfaser.isNotEmpty() && feuchtigkeit.isNotEmpty()) {
            
            // Berechne NFE
            nfe = viewModel.calculateNFE(
                proteinValue, fettValue, rohascheValue, rohfaserValue, feuchtigkeitValue
            )
            
            // Berechne kcalPro100g
            kcalPro100g = viewModel.calculateKcal(
                proteinValue, fettValue, rohascheValue, rohfaserValue, feuchtigkeitValue
            )
        }
    }
    
    // Aktualisiere Herstellervorschläge, wenn sich die Eingabe ändert
    LaunchedEffect(hersteller) {
        if (hersteller.isNotEmpty()) {
            viewModel.updateHerstellerQuery(hersteller)
            showHerstellerSuggestions = true
        } else {
            showHerstellerSuggestions = false
        }
    }
    
    // Dialog für die Vorschau vor dem Einreichen
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Einreichung überprüfen") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Bitte überprüfen Sie die folgenden Angaben:")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("EAN: $ean")
                    Text("Hersteller: $hersteller")
                    Text("Kategorie: $selectedKategorie")
                    Text("Unterkategorie: $selectedUnterkategorie")
                    Text("Produktname: $produktname")
                    Text("Lebensphase: $selectedLebensphase")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Nährwerte pro 100g:")
                    Text("Protein: $protein%")
                    Text("Fett: $fett%")
                    Text("Rohasche: $rohasche%")
                    Text("Rohfaser: $rohfaser%")
                    Text("Feuchtigkeit: $feuchtigkeit%")
                    Text("NFE (berechnet): $nfe%")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Kalorien pro 100g: $kcalPro100g kcal")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreviewDialog = false
                        isLoading = true
                        
                        // Führe die Einreichung durch
                        viewModel.submitEinreichung(
                            hersteller = hersteller,
                            kategorie = selectedKategorie,
                            unterkategorie = selectedUnterkategorie,
                            produktname = produktname,
                            ean = ean,
                            lebensphase = selectedLebensphase,
                            protein = protein.toIntOrNull() ?: 0,
                            fett = fett.toIntOrNull() ?: 0,
                            rohasche = rohasche.toIntOrNull() ?: 0,
                            rohfaser = rohfaser.toIntOrNull() ?: 0,
                            feuchtigkeit = feuchtigkeit.toIntOrNull() ?: 0
                        )
                    }
                ) {
                    Text("Einreichen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPreviewDialog = false }
                ) {
                    Text("Ändern")
                }
            }
        )
    }
    
    // Dialog für die Erfolgsmeldung
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Nicht schließbar */ },
            title = { Text("Vielen Dank!") },
            text = { 
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Vielen Dank für Ihre Mithilfe! Ihre Einreichung wird so schnell wie möglich bearbeitet."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Hauptinhalt des Bildschirms
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Futter einreichen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hauptinhalt (Eingabeformular)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // EAN-Nummer (nicht editierbar)
                OutlinedTextField(
                    value = ean,
                    onValueChange = { /* Nicht editierbar */ },
                    label = { Text("EAN-Nummer") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hersteller mit Autovervollständigung
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hersteller,
                        onValueChange = { hersteller = it },
                        label = { Text("Hersteller") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(herstellerFocusRequester),
                        enabled = !isLoading
                    )
                    
                    // Autovervollständigungs-Dropdown
                    DropdownMenu(
                        expanded = showHerstellerSuggestions && herstellerVorschlaege.isNotEmpty(),
                        onDismissRequest = { showHerstellerSuggestions = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        herstellerVorschlaege.forEach { herstellerOption ->
                            DropdownMenuItem(
                                text = { Text(herstellerOption.name) },
                                onClick = {
                                    hersteller = herstellerOption.name
                                    showHerstellerSuggestions = false
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Kategorie-Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedKategorie,
                    onExpandedChange = { expandedKategorie = !expandedKategorie }
                ) {
                    OutlinedTextField(
                        value = selectedKategorie,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKategorie) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedKategorie,
                        onDismissRequest = { expandedKategorie = false }
                    ) {
                        kategorien.forEach { kategorie ->
                            DropdownMenuItem(
                                text = { Text(kategorie.name) },
                                onClick = {
                                    selectedKategorie = kategorie.name
                                    expandedKategorie = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Unterkategorie-Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedUnterkategorie,
                    onExpandedChange = { expandedUnterkategorie = !expandedUnterkategorie }
                ) {
                    OutlinedTextField(
                        value = selectedUnterkategorie,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unterkategorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnterkategorie) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedUnterkategorie,
                        onDismissRequest = { expandedUnterkategorie = false }
                    ) {
                        unterkategorien.forEach { unterkategorie ->
                            DropdownMenuItem(
                                text = { Text(unterkategorie.name) },
                                onClick = {
                                    selectedUnterkategorie = unterkategorie.name
                                    expandedUnterkategorie = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Produktname
                OutlinedTextField(
                    value = produktname,
                    onValueChange = { produktname = it },
                    label = { Text("Produktname") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lebensphase-Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedLebensphase,
                    onExpandedChange = { expandedLebensphase = !expandedLebensphase }
                ) {
                    OutlinedTextField(
                        value = selectedLebensphase,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lebensphase") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLebensphase) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedLebensphase,
                        onDismissRequest = { expandedLebensphase = false }
                    ) {
                        lebensphasen.forEach { lebensphase ->
                            DropdownMenuItem(
                                text = { Text(lebensphase.name) },
                                onClick = {
                                    selectedLebensphase = lebensphase.name
                                    expandedLebensphase = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Überschrift für Nährwerte
                Text(
                    "Nährwerte (in % bzw. g/100g)",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nährwerte-Eingabefelder in einem Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Protein-Eingabe
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        enabled = !isLoading
                    )
                    
                    // Fett-Eingabe
                    OutlinedTextField(
                        value = fett,
                        onValueChange = { fett = it },
                        label = { Text("Fett %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Rohasche-Eingabe
                    OutlinedTextField(
                        value = rohasche,
                        onValueChange = { rohasche = it },
                        label = { Text("Rohasche %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        enabled = !isLoading
                    )
                    
                    // Rohfaser-Eingabe
                    OutlinedTextField(
                        value = rohfaser,
                        onValueChange = { rohfaser = it },
                        label = { Text("Rohfaser %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Feuchtigkeit-Eingabe
                OutlinedTextField(
                    value = feuchtigkeit,
                    onValueChange = { feuchtigkeit = it },
                    label = { Text("Feuchtigkeit %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Berechnete Werte anzeigen
                if (protein.isNotEmpty() && fett.isNotEmpty() && rohasche.isNotEmpty() &&
                    rohfaser.isNotEmpty() && feuchtigkeit.isNotEmpty()) {
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Berechnete Werte:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("NFE (berechnet): $nfe %")
                            Text("Kalorien pro 100g: $kcalPro100g kcal")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Einreichen-Button
                Button(
                    onClick = { showPreviewDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && 
                             hersteller.isNotEmpty() && 
                             selectedKategorie.isNotEmpty() && 
                             selectedUnterkategorie.isNotEmpty() && 
                             produktname.isNotEmpty() && 
                             selectedLebensphase.isNotEmpty() && 
                             protein.isNotEmpty() && fett.isNotEmpty() && 
                             rohasche.isNotEmpty() && rohfaser.isNotEmpty() && 
                             feuchtigkeit.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Einreichen")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Loading-Overlay
            if (isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
