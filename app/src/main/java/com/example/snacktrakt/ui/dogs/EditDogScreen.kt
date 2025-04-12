package com.example.snacktrakt.ui.dogs

// --- Standard Imports ---
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility // ++ IMPORT HINZUGEFÜGT ++
import androidx.compose.foundation.background // ++ IMPORT HINZUGEFÜGT ++
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Importiere alle Standard-Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector // Import für Icon Parameter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.snacktrakt.R
import com.example.snacktrakt.data.model.Dog
import com.example.snacktrakt.ui.dogs.DogViewModel.ActivityLevel
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDogScreen(
    dog: Dog,
    onNavigateBack: () -> Unit,
    dogViewModel: DogViewModel
) {
    // States und Launcher (unverändert zur letzten Version)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf(dog.name) }
    var rasse by remember { mutableStateOf(dog.rasse) }
    var alter by remember { mutableStateOf(dog.alter?.toString() ?: "") }
    var gewicht by remember { mutableStateOf(dog.gewicht?.toString() ?: "") }
    var kalorienbedarf by remember { mutableStateOf(dog.kalorienbedarf?.toString() ?: "") }
    var geburtsdatum by remember { mutableStateOf(dog.geburtsdatum) } // Optional
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val currentImageUrl by remember(dog.imageFileId) {
        derivedStateOf { dog.imageFileId?.let { dogViewModel.getDogImageUrl(it, 400, 400) } }
    }
    var isAutoCalc by remember { mutableStateOf(false) }
    var activityLevel by remember { mutableStateOf(ActivityLevel.NORMAL) }
    var activityDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    // LaunchedEffect für Kalorienberechnung (unverändert)
    LaunchedEffect(gewicht, activityLevel, isAutoCalc) { /* ... wie vorher ... */ }

    // Dialog für Löschen
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hund löschen") },
            text = { Text("Möchtest du ${dog.name} wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dogViewModel.deleteDog(dog.id)
                        onNavigateBack()
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${dog.name} bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hund löschen")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    // Speichern ohne Bildänderung
                                    dogViewModel.updateDog(
                                        dogId = dog.id, name = name.trim(), rasse = rasse.trim(),
                                        alter = alter.toIntOrNull(), gewicht = gewicht.replace(',', '.').toDoubleOrNull(),
                                        kalorienbedarf = kalorienbedarf.toIntOrNull(),
                                        geburtsdatum = geburtsdatum
                                    )
                                    snackbarHostState.showSnackbar("Speichern erfolgreich")
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Fehler: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Speichern")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // FAB zum Speichern (Optional, da auch oben)
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    try {
                        // WICHTIG: Context wird hier NICHT mehr übergeben, da ViewModel ihn nicht braucht!
                        dogViewModel.updateDog(
                            dogId = dog.id, name = name.trim(), rasse = rasse.trim(),
                            alter = alter.toIntOrNull(), gewicht = gewicht.replace(',', '.').toDoubleOrNull(),
                            kalorienbedarf = kalorienbedarf.toIntOrNull(), imageUri = imageUri,
                            geburtsdatum = geburtsdatum
                        )
                        snackbarHostState.showSnackbar("Änderungen gespeichert.")
                        onNavigateBack()
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Fehler: ${e.message}")
                    }
                }
            }) { Icon(Icons.Default.Save, "Speichern") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Bildanzeige und -auswahl ---
            Box(contentAlignment = Alignment.BottomEnd) {
                // Fallback zu einem einfachen Platzhalter ohne Bildressource
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Pets, contentDescription = "Hundebild", 
                         modifier = Modifier.size(64.dp))
                }
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        // ++ KORREKTUR: Import für background wurde oben hinzugefügt ++
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Bild ändern",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // --- Formularfelder ---
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = rasse,
                onValueChange = { rasse = it },
                label = { Text("Rasse") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = alter,
                    onValueChange = { alter = it },
                    label = { Text("Alter") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = gewicht,
                    onValueChange = { gewicht = it },
                    label = { Text("Gewicht (kg)") },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // --- Kalorienbedarf ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Täglicher Kalorienbedarf", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("Automatisch berechnen", modifier = Modifier.weight(1f))
                        Switch(
                            checked = isAutoCalc,
                            onCheckedChange = { isAutoCalc = it }
                        )
                    }

                    // ++ KORREKTUR: Import für AnimatedVisibility wurde oben hinzugefügt ++
                    // Stelle sicher, dass ALLE Composables *innerhalb* von AnimatedVisibility importiert sind!
                    // (Column, ExposedDropdownMenuBox, OutlinedTextField, Text, DropdownMenuItem)
                    // Führe Clean & Rebuild durch, falls der @Composable Fehler hier weiterhin auftritt.
                    AnimatedVisibility(visible = isAutoCalc) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = activityDropdownExpanded,
                                onExpandedChange = { activityDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = activityLevel.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Aktivitätslevel") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = activityDropdownExpanded,
                                    onDismissRequest = { activityDropdownExpanded = false }
                                ) {
                                    ActivityLevel.values().forEach { level ->
                                        DropdownMenuItem(
                                            text = { Text(level.label) },
                                            onClick = {
                                                activityLevel = level
                                                activityDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Text("Das Aktivitätslevel beeinflusst den täglichen Kalorienbedarf.", 
                                 style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Kalorienbedarf-Eingabe/Anzeige
                    OutlinedTextField(
                        value = kalorienbedarf,
                        onValueChange = { /* ... */ },
                        label = { Text("Kalorienbedarf (kcal/Tag)") },
                        // ...
                        readOnly = isAutoCalc,
                        trailingIcon = {
                            // ++ KORREKTUR: Korrektes Icon verwenden und importieren ++
                            if (isAutoCalc) Icon(Icons.Filled.Lock, contentDescription = "Automatisch berechnet")
                        }
                    )
                }
            }

            // --- Infos zu verbrauchten Kalorien ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bereits verbraucht (heute)", /* ... */ )
                    Spacer(modifier = Modifier.height(8.dp))
                    val consumed = dog.consumedCalories
                    val target = kalorienbedarf.toIntOrNull() ?: dog.kalorienbedarf ?: 0 // Nimm Ziel vom Hund, wenn Eingabe ungültig
                    Text("$consumed kcal ${if (target > 0) "/ $target kcal" else ""}", /* ... */ )
                    if (target > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // ++ KORREKTUR: Lambda {} entfernt ++
                        LinearProgressIndicator(
                            progress = (consumed.toFloat() / target).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(72.dp)) // Platz für FAB
        }
    }
}

// Entferne die Dummy-Ressourcen, wenn du echte Placeholder hast
/*
object R {
    object drawable {
        const val dog_placeholder = android.R.drawable.sym_def_app_icon
        const val dog_placeholder_error = android.R.drawable.ic_dialog_alert
    }
}
*/