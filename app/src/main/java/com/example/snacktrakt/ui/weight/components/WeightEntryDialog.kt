package com.example.snacktrakt.ui.weight.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog zur Eingabe eines neuen Gewichts
 *
 * @param initialWeight Vorausgefüllter Gewichtswert (kann leer sein)
 * @param onWeightChange Callback bei Änderung des Gewichts
 * @param note Vorausgefüllte Notiz (kann leer sein)
 * @param onNoteChange Callback bei Änderung der Notiz
 * @param onDismiss Callback beim Schließen des Dialogs
 * @param onConfirm Callback bei Bestätigung der Eingabe
 * @param isLoading Wird angezeigt, während die Daten gespeichert werden
 */
@Composable
fun WeightEntryDialog(
    initialWeight: String,
    onWeightChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Gewicht eingeben") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Gewichtseingabefeld
                OutlinedTextField(
                    value = initialWeight,
                    onValueChange = onWeightChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Gewicht (kg)") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )
                
                // Notizenfeld
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notiz (optional)") },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLoading
                )
                
                // Anzeige von Ladevorgang
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Abbrechen")
            }
        }
    )
}
