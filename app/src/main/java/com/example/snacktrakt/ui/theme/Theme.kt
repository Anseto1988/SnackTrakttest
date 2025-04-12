package com.example.snacktrakt.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Dunkles Farbschema für die Snacktrakt-App
 * 
 * Definiert die Hauptfarben für den Dark Mode der Anwendung.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * Helles Farbschema für die Snacktrakt-App
 * 
 * Definiert die Hauptfarben für den Light Mode der Anwendung.
 * Weitere Farben könnten bei Bedarf angepasst werden.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Haupttheme der Snacktrakt-App
 * 
 * Diese Composable-Funktion definiert das Design-Theme für die gesamte Anwendung.
 * Sie berücksichtigt:
 * - System-Einstellung für Dark/Light Mode
 * - Dynamic Color (auf Android 12+)
 * - Typography und Shape-Einstellungen
 * 
 * Alle UI-Komponenten innerhalb dieses Themes erben automatisch die definierten
 * Designattribute wie Farben, Typografie und Formen.
 * 
 * @param darkTheme Ob das dunkle Theme verwendet werden soll, standardmäßig abhängig von System-Einstellungen
 * @param dynamicColor Ob dynamische Farben (Android 12+) verwendet werden sollen
 * @param content Der Inhalt, der mit diesem Theme gerendert werden soll
 */
@Composable
fun SnacktraktTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Auswahl des Farbschemas basierend auf System-Einstellungen und Gerätefunktionen
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}