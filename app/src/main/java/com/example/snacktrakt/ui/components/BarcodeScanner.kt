package com.example.snacktrakt.ui.components

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "BarcodeScanner"

/**
 * Eine Composable-Funktion, die einen Barcode-Scanner mit Kamera-Vorschau darstellt.
 * Der Scanner erkennt EAN-Codes und ruft einen Callback bei erkanntem Code auf.
 * 
 * @param onBarcodeDetected Callback der aufgerufen wird, wenn ein Barcode erkannt wurde
 * @param modifier Modifier für das Anpassen des Layouts
 */
@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Speichert, ob die Kamera gestartet wurde
    var hasCameraStarted by remember { mutableStateOf(false) }
    
    // Erstellt einen Barcode-Scanner, der auf EAN-Codes fokussiert ist
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    
    // Kameraansicht erstellen
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                // Ein PreviewView für die Kameraansicht erstellen
                val previewView = PreviewView(context)
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                
                // Wenn die Kamera noch nicht gestartet wurde, starte sie
                if (!hasCameraStarted) {
                    hasCameraStarted = true
                    // Starte die Kamera in einem LaunchedEffect
                    startCamera(
                        context = context,
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner,
                        barcodeScanner = barcodeScanner,
                        onBarcodeDetected = onBarcodeDetected
                    )
                }
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Startet die Kamera und richtet den Barcode-Analysator ein
 */
private fun startCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    barcodeScanner: BarcodeScanner,
    onBarcodeDetected: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = Executors.newSingleThreadExecutor()
    
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        
        // Kamera-Vorschau einrichten
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        // Bildanalyse für Barcodes einrichten
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                setAnalyzer(executor, BarcodeAnalyzer(barcodeScanner, onBarcodeDetected))
            }
        
        try {
            // Vorherige Bindungen aufheben
            cameraProvider.unbindAll()
            
            // Binde Anwendungsfälle an die Kamera
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Kamera-Bindung fehlgeschlagen", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Analyzer-Klasse für die Barcode-Erkennung in Bildern
 */
private class BarcodeAnalyzer(
    private val barcodeScanner: BarcodeScanner,
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private var isScanning = true
    
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Verarbeite gefundene Barcodes
                    for (barcode in barcodes) {
                        // Wir sind nur an EAN/UPC-Codes interessiert
                        if (barcode.valueType == Barcode.TYPE_PRODUCT) {
                            barcode.rawValue?.let { rawValue ->
                                Log.d(TAG, "Barcode erkannt: $rawValue")
                                // Pausiere das Scannen kurzzeitig, um Mehrfacherkennung zu vermeiden
                                isScanning = false
                                // Rufe den Callback mit dem gefundenen Barcode-Wert auf
                                onBarcodeDetected(rawValue)
                                
                                // Starte das Scannen nach einer kurzen Verzögerung wieder
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    isScanning = true
                                }, 2000)
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Barcode-Erkennung fehlgeschlagen", exception)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

/**
 * Extension-Funktion, um einen Camera Provider zu erhalten und in eine suspendierbare
 * Funktion zu konvertieren
 */
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, ContextCompat.getMainExecutor(this))
    }
}
