package com.example.snacktrakt.ui.dogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.snacktrakt.data.model.Dog

/**
 * Wiederverwendbare Karte zur Darstellung eines Hundes
 * 
 * Diese Komponente zeigt die wichtigsten Informationen zu einem Hund an, einschließlich:
 * - Profilbild (falls vorhanden)
 * - Name und Rasse
 * - Alter oder Geburtsdatum
 * - Gewicht (falls angegeben)
 * - Kalorienbedarf und verbrauchte Kalorien
 * - Aktionsschaltflächen für Mahlzeiten, Gewichtsverlauf und Bearbeitung
 * 
 * Die Karte kann als ausgewählt markiert werden, was durch eine dickere Umrandung angezeigt wird.
 * 
 * @param dog Das Hundeobjekt mit allen anzuzeigenden Daten
 * @param imageUrl URL zum Profilbild des Hundes (optional)
 * @param isSelected Gibt an, ob diese Karte aktuell ausgewählt ist
 * @param onClick Callback, der ausgeführt wird, wenn die Karte angeklickt wird
 * @param onMahlzeitenClick Callback, der ausgeführt wird, wenn die Mahlzeiten-Schaltfläche angeklickt wird
 * @param onWeightTrackingClick Callback, der ausgeführt wird, wenn die Gewichtsverlauf-Schaltfläche angeklickt wird
 * @param onEditClick Callback, der ausgeführt wird, wenn die Bearbeiten-Schaltfläche angeklickt wird
 */
@Composable
fun DogCard(
    dog: Dog,
    imageUrl: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onMahlzeitenClick: () -> Unit = {},
    onWeightTrackingClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    // Karte mit Umrandung, die je nach Auswahlstatus unterschiedlich ist
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hundebild (falls vorhanden, sonst Platzhalter)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wenn ein Bild-URL vorhanden ist, wird es angezeigt
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Hund ${dog.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // Ansonsten wird ein Platzhalter mit dem Anfangsbuchstaben des Namens angezeigt
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dog.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }
                
                // Informationen zum Hund
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Name des Hundes
                    Text(
                        text = dog.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Rasse des Hundes
                    Text(
                        text = "Rasse: ${dog.rasse}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Alter des Hundes (falls angegeben)
                    dog.alter?.let {
                        Text(
                            text = "Alter: $it Jahre",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Schaltfläche zum Bearbeiten des Hundes
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Hund bearbeiten",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Abschnitt für Kalorieninformationen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Kalorienbedarf des Hundes (falls angegeben)
                    Text(
                        text = "Kalorienbedarf: ${dog.kalorienbedarf ?: "Nicht festgelegt"} kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Schaltfläche zur Anzeige der Mahlzeiten
                    Row {
                        TextButton(
                            onClick = onMahlzeitenClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Fastfood,
                                contentDescription = "Mahlzeiten anzeigen",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Mahlzeiten")
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Schaltfläche zur Anzeige des Gewichtsverlaufs
                        TextButton(
                            onClick = onWeightTrackingClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = "Gewichtsverlauf anzeigen",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Gewicht")
                        }
                    }
                }
                
                // Verbrauchte Kalorien des Hundes
                Text(
                    text = "Verbrauchte Kalorien: ${dog.consumedCalories} kcal",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Fortschrittsbalken für den Kalorienverbrauch
                dog.kalorienbedarf?.let { kalorienbedarf ->
                    val progress = (dog.consumedCalories.toFloat() / kalorienbedarf).coerceIn(0f, 1f)
                    val progressColor = when {
                        progress < 0.5 -> MaterialTheme.colorScheme.primary
                        progress < 0.8 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
