package com.example.snacktrakt.ui.weight.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.snacktrakt.data.repository.WeightRepository

/**
 * Komponente zur Auswahl des Zeitraums für die Gewichtsanzeige
 *
 * @param selectedRange Aktuell ausgewählter Zeitraum
 * @param onRangeSelected Callback bei Auswahl eines neuen Zeitraums
 * @param modifier Modifier für das Layout
 */
@Composable
fun TimeRangeSelector(
    selectedRange: WeightRepository.TimeRange,
    onRangeSelected: (WeightRepository.TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Erstelle für jeden Zeitraum einen Button
        WeightRepository.TimeRange.values().forEach { range ->
            val isSelected = range == selectedRange
            
            if (isSelected) {
                Button(
                    onClick = { onRangeSelected(range) },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(range.displayName)
                }
            } else {
                OutlinedButton(
                    onClick = { onRangeSelected(range) },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(range.displayName)
                }
            }
        }
    }
}
