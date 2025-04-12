package com.example.snacktrakt.ui.dogs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog zum Hinzufügen eines neuen Hundes
 * 
 * Diese Composable-Funktion stellt einen Dialog dar, der es Benutzern ermöglicht,
 * einen neuen Hund schnell hinzuzufügen, ohne den vollständigen CreateDogScreen zu öffnen.
 * Der Dialog enthält:
 * - Ein Bild-Upload-Feld (optional)
 * - Pflichtfelder für Name und Rasse
 * - Optionale Felder für Alter, Geburtsdatum, Gewicht und Kalorienbedarf
 * - Einen "Speichern"-Button, der nur aktiv ist, wenn alle Pflichtfelder ausgefüllt sind
 * 
 * Im Vergleich zum vollständigen CreateDogScreen bietet dieser Dialog einen
 * schnelleren und einfacheren Weg, um einen neuen Hund mit grundlegenden Informationen hinzuzufügen.
 * 
 * @param onDismiss Callback, der aufgerufen wird, wenn der Dialog geschlossen werden soll
 * @param onConfirm Callback, der aufgerufen wird, wenn der Benutzer auf "Speichern" klickt,
 *                  mit allen eingegebenen Daten als Parameter
 * @param context Der Anwendungskontext, standardmäßig LocalContext.current
 */
@Composable
fun AddDogDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, rasse: String, alter: Int?, geburtsdatum: Date?, 
                gewicht: Double?, kalorienbedarf: Int?, imageUri: Uri?) -> Unit,
    context: Context = LocalContext.current // Use LocalContext if not provided
) {
    // States für alle Formularfelder
    var name by remember { mutableStateOf("") }
    var rasse by remember { mutableStateOf("") }
    var alterText by remember { mutableStateOf("") }
    var geburtsdatumText by remember { mutableStateOf("") }
    var gewichtText by remember { mutableStateOf("") }
    var kalorienbedarfText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Datums-Formatter für das Geburtsdatum
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    // Activity-Launcher für die Bildauswahl
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Dialogtitel
                Text(
                    text = "Neuen Hund hinzufügen",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Bildauswahl-Bereich
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePicker.launch("image/*") }
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        // Zeige das ausgewählte Bild an
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Hund Bild",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Zeige ein Plus-Symbol an, wenn kein Bild ausgewählt ist
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Bild hinzufügen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pflichtfelder
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = rasse,
                    onValueChange = { rasse = it },
                    label = { Text("Rasse *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Optionale Felder
                OutlinedTextField(
                    value = alterText,
                    onValueChange = { alterText = it },
                    label = { Text("Alter") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = geburtsdatumText,
                    onValueChange = { geburtsdatumText = it },
                    label = { Text("Geburtsdatum (DD.MM.YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = gewichtText,
                    onValueChange = { gewichtText = it },
                    label = { Text("Gewicht (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = kalorienbedarfText,
                    onValueChange = { kalorienbedarfText = it },
                    label = { Text("Kalorienbedarf (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speichern-Button (nur aktiv, wenn Pflichtfelder ausgefüllt sind)
                Button(
                    onClick = {
                        // Parse optionale Werte mit Fehlerbehandlung
                        val alter = alterText.toIntOrNull()
                        val geburtsdatum = try {
                            if (geburtsdatumText.isNotEmpty()) dateFormat.parse(geburtsdatumText) else null
                        } catch (e: Exception) {
                            null
                        }
                        val gewicht = gewichtText.toDoubleOrNull()
                        val kalorienbedarf = kalorienbedarfText.toIntOrNull()
                        
                        onConfirm(name, rasse, alter, geburtsdatum, gewicht, kalorienbedarf, selectedImageUri)
                    },
                    enabled = name.isNotEmpty() && rasse.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Speichern")
                }
            }
        }
    }
}
