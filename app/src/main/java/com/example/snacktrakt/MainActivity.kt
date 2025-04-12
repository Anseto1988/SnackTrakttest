package com.example.snacktrakt

// --- IMPORTS ---
// Standard-Imports...
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.* // Material 3 Komponenten
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.viewModel // Für viewModel() Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel // Für viewModel() Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Daten-Imports...
import com.example.snacktrakt.data.AppwriteConfig
import com.example.snacktrakt.data.AuthRepository
import com.example.snacktrakt.data.DogRepository
import com.example.snacktrakt.data.FutterDBRepository
import com.example.snacktrakt.data.model.Dog
// UI-Screen Imports...
import com.example.snacktrakt.ui.auth.AuthScreen
import com.example.snacktrakt.ui.auth.AuthViewModel
import com.example.snacktrakt.ui.calories.CaloriesScreen
import com.example.snacktrakt.ui.dashboard.DashboardScreen
import com.example.snacktrakt.ui.dogs.CreateDogScreen
import com.example.snacktrakt.ui.dogs.DogViewModel
import com.example.snacktrakt.ui.dogs.EditDogScreen
import com.example.snacktrakt.ui.futter.FutterDBViewModel
import com.example.snacktrakt.ui.futter.FutterScreen
import com.example.snacktrakt.ui.theme.SnacktraktTheme
import com.example.snacktrakt.ui.weight.WeightTrackingScreen
import com.example.snacktrakt.ui.weight.WeightViewModel // ++ WeightViewModel importieren ++
// Coroutine Imports...
import kotlinx.coroutines.launch
// --- ENDE IMPORTS ---

class MainActivity : ComponentActivity() {

    // Repositories initialisieren (besser als Singleton oder mit DI)
    private val authRepository: AuthRepository by lazy {
        AuthRepository(AppwriteConfig.createClient(applicationContext))
    }
    
    private val dogRepository: DogRepository by lazy {
        DogRepository(
            AppwriteConfig.createClient(applicationContext),
            applicationContext,
            authRepository // Wiederverwendung des AuthRepository
        )
    }
    
    private val futterDBRepository: FutterDBRepository by lazy {
        FutterDBRepository(AppwriteConfig.createClient(applicationContext))
    }
    
    // ViewModels über Factory initialisieren
    private val authViewModel: AuthViewModel by lazy {
        ViewModelProvider(this, AuthViewModelFactory(authRepository))[AuthViewModel::class.java]
    }

    private val dogViewModel: DogViewModel by lazy {
        ViewModelProvider(this, DogViewModelFactory(
            dogRepository,
            authRepository // Wiederverwendung des AuthRepository
        ))[DogViewModel::class.java]
    }

    private val futterDBViewModel: FutterDBViewModel by lazy {
        ViewModelProvider(this, FutterDBViewModelFactory(
            futterDBRepository
        ))[FutterDBViewModel::class.java]
    }

    // ++ WeightViewModel hinzufügen ++
    private val weightViewModel: WeightViewModel by lazy {
        ViewModelProvider(this, WeightViewModelFactory(dogRepository))[WeightViewModel::class.java]
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnacktraktTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation( // ViewModels übergeben
                        authViewModel = authViewModel,
                        dogViewModel = dogViewModel,
                        futterDBViewModel = futterDBViewModel,
                        weightViewModel = weightViewModel // ++ WeightViewModel übergeben ++
                    )
                }
            }
        }
    }
}

// --- ViewModel Factories (vereinfacht die Initialisierung) ---
class AuthViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(authRepository) as T
}
class DogViewModelFactory(private val dogRepository: DogRepository, private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DogViewModel(dogRepository, authRepository) as T
}
class FutterDBViewModelFactory(private val futterDBRepository: FutterDBRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FutterDBViewModel(futterDBRepository) as T
}
// ++ Factory für WeightViewModel ++
class WeightViewModelFactory(private val dogRepository: DogRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WeightViewModel(dogRepository) as T
}


/**
 * Haupt-Navigation der Anwendung
 */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    dogViewModel: DogViewModel,
    futterDBViewModel: FutterDBViewModel,
    weightViewModel: WeightViewModel // ++ Parameter hinzugefügt ++
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Beobachte den Login-Status, um die korrekte Navigation zu gewährleisten
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val startDestination = if (isLoggedIn) "dashboard" else "auth" // Startziel basierend auf Login

    // Diese Composable reagiert auf Änderungen des isLoggedIn-Status
    LaunchedEffect(isLoggedIn) {
        val currentRoute = navController.currentDestination?.route
        
        if (isLoggedIn && (currentRoute == "auth" || currentRoute == null)) {
            // Wenn eingeloggt und auf Auth-Screen oder keiner Route, zum Dashboard navigieren
            navController.navigate("dashboard") {
                // Entferne Auth vom Stack, damit Back-Button nicht dorthin zurückführt
                popUpTo("auth") { inclusive = true } 
            }
        } else if (isLoggedIn.not() && currentRoute != "auth") {
            // Wenn ausgeloggt und nicht auf Auth-Screen, dorthin navigieren und Stack löschen
            navController.navigate("auth") {
                // Entferne alle Routen vom Stack und starte neu mit Auth
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }


    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                authViewModel = authViewModel,
                onNavigateToDashboard = {
                    navController.navigate("dashboard") { popUpTo("auth") { inclusive = true } }
                }
            )
        }
        composable("dashboard") {
            // Sicherstellen, dass wir nur zum Dashboard kommen, wenn eingeloggt
            if (isLoggedIn.not()) {
                // Sofort zur Auth-Seite zurück, falls der Zustand wechselt, während wir hier sind
                LaunchedEffect(Unit) {
                    navController.navigate("auth") { popUpTo("dashboard") { inclusive = true } }
                }
            } else {
                DashboardScreen(
                    dogViewModel = dogViewModel, // DogViewModel übergeben
                    onNavigateToCalories = { navController.navigate("calories") },
                    onNavigateToFutter = { navController.navigate("futter") },
                    onNavigateToEditDog = { dogId -> navController.navigate("edit_dog/$dogId") },
                    onNavigateToCreateDog = { navController.navigate("create_dog") },
                    onNavigateToWeightTracking = { dogId -> navController.navigate("weight_tracking/$dogId") },
                    onLogout = {
                        scope.launch {
                            authViewModel.logout()
                            // Navigation wird durch LaunchedEffect oben gehandhabt
                        }
                    }
                )
            }
        }
        composable("calories") {
            if (isLoggedIn.not()) return@composable // Verhindere Zugriff, wenn nicht eingeloggt
            CaloriesScreen(
                onNavigateBack = { navController.popBackStack() },
                dogViewModel = dogViewModel, // Braucht DogViewModel für selectedDog
                futterDBViewModel = futterDBViewModel
            )
        }
        composable("futter") {
            if (isLoggedIn.not()) return@composable
            FutterScreen(
                onNavigateBack = { navController.popBackStack() },
                dogViewModel = dogViewModel, // Braucht DogViewModel für selectedDog & Futterliste
                futterDBViewModel = futterDBViewModel // Wird hier aktuell nicht direkt verwendet
            )
        }
        composable("edit_dog/{dogId}") { backStackEntry ->
            if (isLoggedIn.not()) return@composable
            val dogId = backStackEntry.arguments?.getString("dogId")
            if (dogId == null) { Text("Fehler: Hunde-ID fehlt."); return@composable }

            // Lade den Hund direkt hier oder im Screen selbst
            // Variante 1: Hier laden (kann initiale Ladeanzeige hier machen)
            var dogToEdit by remember { mutableStateOf<Dog?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(dogId) {
                isLoading = true
                errorMsg = null
                try {
                    dogToEdit = dogViewModel.loadDogById(dogId)
                } catch (e: Exception) {
                    errorMsg = "Fehler: ${e.message}"
                } finally {
                    isLoading = false
                }
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                errorMsg != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(errorMsg!!) }
                dogToEdit != null -> EditDogScreen(
                    dog = dogToEdit!!, // Sicher nicht null hier
                    onNavigateBack = { navController.popBackStack() },
                    dogViewModel = dogViewModel
                )
                else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Hund nicht gefunden.") }
            }
        }
        composable("create_dog") {
            if (isLoggedIn.not()) return@composable
            CreateDogScreen(
                onNavigateBack = { navController.popBackStack() },
                dogViewModel = dogViewModel
            )
        }
        composable("weight_tracking/{dogId}") { backStackEntry ->
            if (isLoggedIn.not()) return@composable
            val dogId = backStackEntry.arguments?.getString("dogId")
            if (dogId == null) { Text("Fehler: Hunde-ID fehlt."); return@composable }

            // ++ Korrekten ViewModel übergeben ++
            WeightTrackingScreen(
                dogId = dogId,
                onNavigateBack = { navController.popBackStack() },
                weightViewModel = weightViewModel // <-- HIER WeightViewModel!
            )
        }
    }
}