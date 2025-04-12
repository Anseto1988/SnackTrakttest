package com.example.snacktrakt.ui.weight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// LazyColumn und items werden NICHT benötigt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.model.WeightEntry
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Liste der Gewichtseintragungen
 *
 * Zeigt alle Gewichtseintragungen mit Datum und optionaler Notiz an.
 * VERWENDET EINE NORMALE COLUMN, um verschachteltes Scrollen zu vermeiden.
 *
 * @param entries Liste der anzuzeigenden Gewichtseintragungen (sollte aufsteigend nach Datum sortiert sein)
 * @param modifier Modifier für das Layout
 */
@Composable
fun WeightHistoryList(
    entries: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(), // Card nimmt volle Breite ein
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // Column füllt die Card
                .padding(16.dp)
        ) {
            // Überschrift
            Text(
                text = "Gewichtsverlauf",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (entries.isEmpty()) {
                // Anzeige wenn keine Daten vorhanden sind
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp), // Etwas Padding
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Gewichtsdaten für diesen Zeitraum vorhanden",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Kopfzeile der Tabelle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp), // Padding angepasst
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Datum",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Gewicht",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Notiz",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.5f), // Kleinere Spalte
                        textAlign = TextAlign.Center
                    )
                }

                // ++ NORMALE COLUMN STATT LAZYCOLUMN ++
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Einträge absteigend anzeigen (neueste zuerst)
                    entries.sortedByDescending { it.date }.forEach { entry ->
                        WeightEntryItem(entry = entry)
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) // Dünnere Divider
                    }
                }
            }
        }
    }
}

/**
 * Einzelner Gewichtseintrag in der Liste
 *
 * @param entry Daten des Eintrags
 */
@Composable
private fun WeightEntryItem(entry: WeightEntry) {
    // Sicherstellen, dass ein gültiges Datum vorhanden ist
    val formattedDate = try {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateFormat.format(entry.date)
    } catch (e: Exception) {
        "Ungültiges Datum"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp) // Gleiches Horizontal-Padding wie Header
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // Zentriert Elemente vertikal
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format(Locale.GERMANY, "%.1f kg", entry.weight), // Locale für Formatierung
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            // Zeige "Ja/Nein" oder ein Icon für Notiz
            Text(
                text = if (!entry.note.isNullOrBlank()) "Ja" else " ", // Zeige Ja oder nichts
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.5f),
                textAlign = TextAlign.Center
            )
        }

        // Notiz anzeigen, falls vorhanden und nicht leer
        if (!entry.note.isNullOrBlank()) {
            Text(
                text = "Notiz: ${entry.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp) // Etwas Abstand nach oben
            )
        }
    }
}