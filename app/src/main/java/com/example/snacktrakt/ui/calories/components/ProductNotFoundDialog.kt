package com.example.snacktrakt.ui.calories.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog für "Produkt nicht gefunden"
 *
 * @param barcode Der gescannte Barcode
 * @param onDismiss Callback zum Schließen des Dialogs
 * @param onAddProduct Callback zum Hinzufügen des Produkts
 */
@Composable
fun ProductNotFoundDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onAddProduct: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Produkt nicht gefunden") },
        text = {
            Text("Das gescannte Produkt (EAN: $barcode) wurde nicht in der Datenbank gefunden. " +
                 "Möchten Sie es zur Datenbank hinzufügen?")
        },
        confirmButton = {
            Button(onClick = onAddProduct) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
