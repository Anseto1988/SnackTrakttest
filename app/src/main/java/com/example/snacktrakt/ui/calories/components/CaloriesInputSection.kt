package com.example.snacktrakt.ui.calories.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.model.FutterDB

/**
 * Eingabebereich für die Kalorieneingabe
 *
 * @param description Beschreibungstext
 * @param onDescriptionChange Callback für Änderungen der Beschreibung
 * @param calories Kalorientext
 * @param onCaloriesChange Callback für Änderungen der Kalorien
 * @param gramm Grammangabe
 * @param onGrammChange Callback für Änderungen der Grammangabe
 * @param scannedBarcode Gescannter Barcode (kann null sein)
 * @param currentFutter Aktuell ausgewähltes Futter (kann null sein)
 * @param isLoading Gibt an, ob gerade ein Ladevorgang stattfindet
 * @param isEnabled Gibt an, ob die Eingabefelder aktiviert sind
 * @param onScanRequest Callback für Anfrage zum Scannen
 */
@Composable
fun CaloriesInputSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    gramm: String,
    onGrammChange: (String) -> Unit,
    scannedBarcode: String?,
    currentFutter: FutterDB?,
    isLoading: Boolean,
    isEnabled: Boolean,
    onScanRequest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Beschreibungsfeld (automatisch ausgefüllt, wenn ein Barcode gescannt wurde)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Beschreibung") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && isEnabled,
            trailingIcon = {
                // Scanner-Button neben dem Eingabefeld
                IconButton(
                    onClick = onScanRequest,
                    enabled = !isLoading && isEnabled
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode scannen")
                }
            }
        )
        
        // Tipp zur Barcode-Nutzung
        if (scannedBarcode == null) {
            Text(
                text = "Tipp: Scannen Sie den Barcode auf der Futterverpackung",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Barcode: $scannedBarcode",
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mengenangabe in Gramm (nur relevant, wenn ein Futter ausgewählt wurde)
        if (currentFutter != null) {
            OutlinedTextField(
                value = gramm,
                onValueChange = onGrammChange,
                label = { Text("Menge in Gramm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isEnabled
            )
            
            // Futter-Info-Karte wird separat gerendert durch FutterInfoCard
            
        } else {
            // Standard-Kalorienfeld, wenn kein Futter ausgewählt wurde
            OutlinedTextField(
                value = calories,
                onValueChange = onCaloriesChange,
                label = { Text("Kalorien") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isEnabled
            )
        }
    }
}
