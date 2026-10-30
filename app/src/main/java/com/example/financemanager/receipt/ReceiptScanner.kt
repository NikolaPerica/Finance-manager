package com.example.financemanager.receipt

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.financemanager.domain.receipt.OcrLine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads the text on a photo with ML Kit. Recognition runs on the phone, so the photo never
 * leaves it. The model comes through Google Play services (downloaded once, after install)
 * instead of inside the app, which would make the app about 40 MB bigger.
 */
class ReceiptScanner(private val context: Context) {

    /**
     * All lines of text on the image at [uri]. Throws if the image can't be read, or
     * [MlKitException] with [MlKitException.UNAVAILABLE] while the model is still downloading.
     */
    suspend fun read(uri: Uri): List<OcrLine> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = recognizer.process(InputImage.fromFilePath(context, uri)).await()
            return text.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    OcrLine(line.text, box.top, box.bottom, box.left)
                }
            }
        } finally {
            recognizer.close()
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
