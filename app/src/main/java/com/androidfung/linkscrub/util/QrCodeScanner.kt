package com.androidfung.linkscrub.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Helper object wrapping Google Code Scanner (ML Kit Barcode Scanning via Google Play Services).
 *
 * Scanning is performed within a secure Google Play Services dialog, requiring
 * NO CAMERA PERMISSIONS in the host application.
 */
object QrCodeScanner {

    private val options: GmsBarcodeScannerOptions by lazy {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
    }

    /**
     * Starts the Google Code Scanner flow.
     *
     * @param context Context (Activity context preferred)
     * @param onSuccess Callback receiving the scanned URL / raw value
     * @param onFailure Callback receiving any scanning exception
     * @param onCanceled Callback when the user cancels or dismisses the scanner
     */
    fun startScan(
        context: Context,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit = {},
        onCanceled: () -> Unit = {},
    ) {
        try {
            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    // Extract barcode.url?.url or barcode.rawValue
                    val scannedValue = barcode.url?.url ?: barcode.rawValue
                    if (!scannedValue.isNullOrBlank()) {
                        onSuccess(scannedValue)
                    }
                }
                .addOnCanceledListener {
                    onCanceled()
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
