package com.example.snacktrakt.ui.dogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.snacktrakt.ui.dogs.DogViewModel.ActivityLevel
import kotlinx.coroutines.launch
import java.util.*

/**
 * Bildschirm zur Erstellung eines neuen Hundes
 * 
 * Diese Composable-Funktion bietet ein Formular mit Eingabefeldern für alle relevanten
 * Hundedaten wie Name, Rasse, Alter, Gewicht und Kalorienbedarf.
 * Zusätzlich ermöglicht sie:
 * - Hochladen eines Hundebildes
 * - Automatische Berechnung des Kalorienbedarfs basierend auf Gewicht und Aktivitätslevel
 * - Speichern des neuen Hundes in der Datenbank
 * 
 * @param onNavigateBack Callback für die Navigation zurück zum vorherigen Bildschirm
 * @param dogViewModel ViewModel für die Interaktion mit den Hundedaten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDogScreen(
    onNavigateBack: () -> Unit,
    dogViewModel: DogViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Zustand für das Formular
    var name by remember { mutableStateOf("") }
    var rasse by remember { mutableStateOf("") }
    var alter by remember { mutableStateOf("") }
    var gewicht by remember { mutableStateOf("") }
    var kalorienbedarf by remember { mutableStateOf("") }
    
    // Zustände für die Kalorienberechnung
    var isAutoCalc by remember { mutableStateOf(true) } // Standardmäßig aktiviert bei neuen Hunden
    var activityLevel by remember { mutableStateOf(ActivityLevel.NORMAL) }
    var expanded by remember { mutableStateOf(false) }
    
    // Kalorienberechnung aktualisieren, wenn sich Gewicht oder Aktivitätslevel ändert
    LaunchedEffect(gewicht, activityLevel) {
        if (isAutoCalc) {
            val weightValue = gewicht.toDoubleOrNull()
            if (weightValue != null && weightValue > 0) {
                kalorienbedarf = dogViewModel.calculateCalorieRequirement(weightValue, activityLevel).toString()
            }
        }
    }
    
    // Zustand für die Bildauswahl
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Snackbar-Zustände
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    
    // Snackbar-Host-State
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Bildauswahl-Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }
    
    // Effect für Snackbar
    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuen Hund erstellen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        try {
                            if (name.isBlank()) {
                                snackbarMessage = "Bitte geben Sie einen Namen ein"
                                showSnackbar = true
                                return@launch
                            }
                            
                            dogViewModel.createDog(
                                name = name,
                                rasse = rasse,
                                alter = alter.toIntOrNull(),
                                gewicht = gewicht.toDoubleOrNull(),
                                kalorienbedarf = kalorienbedarf.toIntOrNull(),
                                imageUri = imageUri,
                                context = context
                            )
                            snackbarMessage = "Hund erfolgreich erstellt"
                            showSnackbar = true
                            onNavigateBack()
                        } catch (e: Exception) {
                            snackbarMessage = "Fehler beim Speichern: ${e.message}"
                            showSnackbar = true
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = "Speichern")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bild
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    // Ausgewähltes Bild
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Hundebild",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    // Placeholder
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (name.isNotEmpty()) name.take(1).uppercase() else "?",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }
            }
            
            // Button zum Hinzufügen eines Bildes
            OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Bild hinzufügen")
            }
            
            // Formularfelder
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = rasse,
                onValueChange = { rasse = it },
                label = { Text("Rasse") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = alter,
                onValueChange = { alter = it },
                label = { Text("Alter (Jahre)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            OutlinedTextField(
                value = gewicht,
                onValueChange = { gewicht = it },
                label = { Text("Gewicht (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            // Kalorienbedarf-Bereich
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kalorienbedarf",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Toggle für automatische Berechnung
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kalorienbedarf automatisch berechnen")
                        Switch(
                            checked = isAutoCalc,
                            onCheckedChange = { isAutoCalc = it }
                        )
                    }
                    
                    if (isAutoCalc) {
                        // Aktivitätslevel-Auswahl
                        Column {
                            Text("Aktivitätslevel des Hundes:")
                            
                            Box {
                                OutlinedTextField(
                                    value = activityLevel.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(onClick = { expanded = !expanded }) {
                                            if (expanded) 
                                                Icon(Icons.Default.KeyboardArrowUp, "Schließen")
                                            else 
                                                Icon(Icons.Default.KeyboardArrowDown, "Öffnen")
                                        }
                                    }
                                )
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    ActivityLevel.values().forEach { level ->
                                        DropdownMenuItem(
                                            text = { Text(level.label) },
                                            onClick = {
                                                activityLevel = level
                                                expanded = false
                                                // Kalorienbedarf automatisch aktualisieren
                                                val weightValue = gewicht.toDoubleOrNull()
                                                if (weightValue != null && weightValue > 0) {
                                                    kalorienbedarf = dogViewModel.calculateCalorieRequirement(
                                                        weightValue, level
                                                    ).toString()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Info-Text zur Berechnung
                        Text(
                            text = "Basierend auf FEDIAF-Richtlinien 2018: RER = 70 × (${gewicht.toDoubleOrNull() ?: 0} kg)^0.75, " +
                                  "multipliziert mit dem Aktivitätsfaktor ${activityLevel.factor}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // Kalorienbedarf-Anzeige/Eingabe
                    if (isAutoCalc) {
                        // Nur Anzeige
                        Text(
                            text = "Berechneter Kalorienbedarf: ${kalorienbedarf} kcal",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        // Manuelle Eingabe
                        OutlinedTextField(
                            value = kalorienbedarf,
                            onValueChange = { kalorienbedarf = it },
                            label = { Text("Kalorienbedarf (kcal)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(72.dp)) // Platz für FAB
        }
    }
}
