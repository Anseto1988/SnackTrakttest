package com.example.snacktrakt.ui.calories.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import com.example.snacktrakt.ui.components.BarcodeScanner

private const val TAG = "BarcodeSection"

/**
 * Komponente für den Barcode-Scanner im Vollbildmodus
 *
 * @param onBarcodeDetected Callback, wenn ein Barcode erkannt wurde
 * @param onClose Callback zum Schließen des Scanners
 */
@Composable
fun BarcodeSection(
    onBarcodeDetected: (String) -> Unit,
    onClose: () -> Unit
) {
    // Box zum Anzeigen des Scanners mit einem Schließen-Button oben
    Box(modifier = Modifier.fillMaxSize()) {
        // Der Scanner selbst
        BarcodeScanner(
            onBarcodeDetected = { barcode ->
                Log.d(TAG, "Barcode erkannt: $barcode")
                onBarcodeDetected(barcode)
            }
        )
        
        // Schließen-Button am oberen Rand
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Schließen",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
