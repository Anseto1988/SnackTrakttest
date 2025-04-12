package com.example.snacktrakt.ui.weight.components

import android.graphics.Color // Android Graphics Color
import android.util.Log // ++ Logging hinzufügen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb // Compose Color zu Android Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.snacktrakt.data.model.WeightEntry
// MPAndroidChart Imports
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
// Java / Kotlin Utils
import java.text.SimpleDateFormat
import java.util.Calendar // ++ Für die Gruppierung nach Tag
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Gewichtsverlaufsdiagramm mit MPAndroidChart
 *
 * Zeigt den Verlauf des Gewichts über die Zeit als Liniendiagramm an.
 * NEU: Zeigt nur den höchsten Gewichtseintrag pro Tag an.
 *
 * @param entries Gewichtseinträge für das Diagramm (sollten aufsteigend nach Datum sortiert sein)
 * @param modifier Modifier für das Layout
 */
@Composable
fun WeightChart(
    entries: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val TAG = "WeightChart" // Tag für Logging

    // Farben aus dem Compose Theme holen
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    // State für die gefilterten Daten, die tatsächlich im Chart angezeigt werden
    // Verwende derivedStateOf, damit es nur neu berechnet wird, wenn entries sich ändern
    val dailyMaxEntries by remember(entries) {
        derivedStateOf {
            if (entries.isEmpty()) {
                emptyList()
            } else {
                Log.d(TAG, "Filtering ${entries.size} entries to daily max...")
                val calendar = Calendar.getInstance()
                val filtered = entries
                    // Gruppiere nach Jahr und Tag des Jahres
                    .groupBy { entry ->
                        calendar.time = entry.date
                        // Eindeutiger Schlüssel pro Tag
                        Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR))
                    }
                    // Wähle den Eintrag mit dem höchsten Gewicht pro Tag aus
                    .mapNotNull { (_, entriesOnDay) ->
                        entriesOnDay.maxByOrNull { it.weight }
                    }
                    // Sortiere das Ergebnis nach Datum (wichtig für die Chart-Linie)
                    .sortedBy { it.date }

                Log.d(TAG, "Filtered down to ${filtered.size} daily max entries.")
                filtered
            }
        }
    }

    // Berechne Y-Achsen-Grenzen basierend auf den *gefilterten* Einträgen
    val axisBounds by remember(dailyMaxEntries) {
        derivedStateOf {
            if (dailyMaxEntries.size >= 2) {
                val weights = dailyMaxEntries.map { it.weight }
                val dataMinY = weights.minOrNull() ?: 0.0
                val dataMaxY = weights.maxOrNull() ?: (dataMinY + 1.0)
                val range = max(dataMaxY - dataMinY, 0.1)
                val padding = (range * 0.1).coerceIn(0.5, 2.0)
                val finalMinY = max(0.0, dataMinY - padding)
                val finalMaxY = dataMaxY + padding
                Pair(finalMinY.toFloat(), finalMaxY.toFloat())
            } else {
                Pair(null, null) // Automatische Skalierung bei < 2 Punkten
            }
        }
    }


    // Formatter bleiben gleich
    val bottomAxisValueFormatter = DateAxisValueFormatter()
    val startAxisValueFormatter = WeightAxisValueFormatter()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp), // Feste Höhe für die Card
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // AndroidView hostet die native MPAndroidChart-View
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(260.dp), // Höhe innerhalb der Card
            factory = { context ->
                // Diagramm nur einmal erstellen
                Log.d(TAG, "Creating LineChart view")
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    setDrawGridBackground(false)
                    setNoDataText("Keine Daten verfügbar.")
                    setNoDataTextColor(onSurfaceColor)

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        granularity = 1f
                        setDrawGridLines(false)
                        textColor = onSurfaceColor
                        valueFormatter = bottomAxisValueFormatter // Eigener Formatter
                        labelRotationAngle = -45f
                        // Optional: Feinere Steuerung der Label-Anzahl
                        // setLabelCount(6, true) // Erzwinge ca. 6 Labels
                    }

                    axisLeft.apply {
                        setDrawGridLines(true)
                        textColor = onSurfaceColor
                        valueFormatter = startAxisValueFormatter
                        axisMinimum = 0f // Gewicht fängt bei 0 an
                        // Grenzen werden in 'update' gesetzt
                    }
                    axisRight.isEnabled = false

                    setScaleEnabled(true)
                    setPinchZoom(true)
                    isDragEnabled = true
                }
            },
            update = { lineChart ->
                // Update-Block wird bei Änderung von dailyMaxEntries und axisBounds ausgeführt

                Log.d(TAG, "Updating LineChart view with ${dailyMaxEntries.size} points.")

                // Setze Achsengrenzen (falls berechnet)
                // Wichtig: resetAxisMinimum/Maximum vor setAxisMinimum/Maximum aufrufen,
                //          falls die Werte null sein könnten (um Auto-Scaling wieder zu aktivieren)
                if (axisBounds.first != null && axisBounds.second != null) {
                    lineChart.axisLeft.axisMinimum = axisBounds.first!!
                    lineChart.axisLeft.axisMaximum = axisBounds.second!!
                } else {
                    lineChart.axisLeft.resetAxisMinimum()
                    lineChart.axisLeft.resetAxisMaximum()
                    // Setze einen Standard-Mindestwert, wenn automatisch skaliert wird
                    lineChart.axisLeft.axisMinimum = 0f
                }


                if (dailyMaxEntries.size >= 2) {
                    // Datenpunkte für MPAndroidChart erstellen (nur aus gefilterter Liste)
                    val chartEntries = dailyMaxEntries.map {
                        Entry(it.date.time.toFloat(), it.weight.toFloat())
                    }

                    // DatenSet erstellen und formatieren
                    val dataSet = LineDataSet(chartEntries, "Gewicht").apply {
                        color = primaryColor
                        valueTextColor = onSurfaceColor
                        setDrawValues(false)
                        lineWidth = 1.8f
                        setCircleColor(primaryColor)
                        circleRadius = 3f
                        setDrawCircleHole(false)
                        setDrawFilled(true)
                        fillColor = primaryColor
                        fillAlpha = 70
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }

                    // LineData erstellen und dem Chart zuweisen
                    val lineData = LineData(dataSet)
                    lineChart.data = lineData

                } else {
                    // Weniger als 2 Punkte, Diagramm leeren
                    lineChart.clear()
                    lineChart.data = null
                    Log.d(TAG, "Not enough data points (need >= 2), clearing chart.")
                }

                // Chart neu zeichnen
                lineChart.notifyDataSetChanged() // Sicherstellen, dass Datenänderungen erkannt werden
                lineChart.invalidate()
                Log.d(TAG, "Chart invalidated.")
            }
        )
    }
}

// Eigener Formatter für die X-Achse (zeigt Datum an)
private class DateAxisValueFormatter : ValueFormatter() {
    // Format nur einmal erstellen
    private val sdf = SimpleDateFormat("dd.MM", Locale.GERMANY)

    override fun getFormattedValue(value: Float): String {
        return try {
            sdf.format(Date(value.toLong()))
        } catch (e: Exception) {
            Log.e("DateAxisValueFormatter", "Error formatting date value: $value", e)
            "" // Leerer String bei Fehlern
        }
    }
}

// Eigener Formatter für die Y-Achse (zeigt Gewicht mit "kg" an)
private class WeightAxisValueFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        // Formatiere mit einer Nachkommastelle und " kg"
        return String.format(Locale.GERMANY, "%.1f kg", value)
    }
}