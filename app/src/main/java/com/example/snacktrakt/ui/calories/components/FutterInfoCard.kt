package com.example.snacktrakt.ui.calories.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.model.FutterDB

/**
 * Karte zur Anzeige von Futterinformationen
 *
 * @param futter Das anzuzeigende Futter
 */
@Composable
fun FutterInfoCard(futter: FutterDB) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Produktinformationen",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Name: ${futter.produktname}")
            Text("Hersteller: ${futter.hersteller}")
            Text("Kategorie: ${futter.kategorie}")
            futter.kcalPro100g?.let {
                Text("Energie: $it kcal/100g")
            }
        }
    }
}
