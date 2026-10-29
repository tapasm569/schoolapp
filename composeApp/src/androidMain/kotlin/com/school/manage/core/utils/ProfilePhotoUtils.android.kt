package com.school.manage.core.utils

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

@Composable
actual fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPhotoSelected = rememberUpdatedState(onPhotoSelected)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (original != null) {
                    val minEdge = min(original.width, original.height)
                    val startX = (original.width - minEdge) / 2
                    val startY = (original.height - minEdge) / 2
                    val square = Bitmap.createBitmap(original, startX, startY, minEdge, minEdge)
                    val scaled = Bitmap.createScaledBitmap(square, 240, 240, true)

                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 72, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    currentOnPhotoSelected.value(encoded)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return { launcher.launch("image/*") }
}

actual fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap? {
    if (base64Str.isBlank()) return null
    return try {
        val clean = base64Str.substringAfter(",")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@Composable
actual fun rememberCsvFilePickerLauncher(onFileContentLoaded: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentCallback = rememberUpdatedState(onFileContentLoaded)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null && bytes.isNotEmpty()) {
                    val isXlsx = bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
                    if (isXlsx) {
                        val parsedCsv = com.school.manage.core.utils.parseXlsxBytesStandalone(bytes)
                        if (parsedCsv.isNotBlank()) {
                            currentCallback.value(parsedCsv)
                        }
                    } else {
                        val content = bytes.decodeToString()
                        if (content.isNotBlank()) {
                            currentCallback.value(content)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return { launcher.launch("*/*") }
}




private var tempCameraUri: Uri? = null

@Composable
actual fun rememberCameraCaptureLauncher(onPhotoCaptured: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnPhotoCaptured = rememberUpdatedState(onPhotoCaptured)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(tempCameraUri!!)
                val original = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (original != null) {
                    val maxDimension = 1080
                    val ratio = min(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
                    val newW = (original.width * ratio).toInt()
                    val newH = (original.height * ratio).toInt()
                    val scaled = Bitmap.createScaledBitmap(original, newW, newH, true)

                    val output = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, output)
                    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    currentOnPhotoCaptured.value(encoded)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return {
        try {
            val cacheFile = File(context.cacheDir, "camera_attachment_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
actual fun rememberPdfPickerLauncher(onPdfSelected: (base64: String, fileName: String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentCallback = rememberUpdatedState(onPdfSelected)

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "attachment.pdf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "attachment.pdf"
                    }
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null && bytes.isNotEmpty()) {
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    currentCallback.value(encoded, fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return { pdfLauncher.launch("application/pdf") }
}

private var appContextRef: android.content.Context? = null

actual fun openPdfAttachment(base64Pdf: String, fileName: String) {
    val context = appContextRef ?: return
    try {
        val clean = base64Pdf.substringAfter(",")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val safeName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val tempFile = File(context.cacheDir, safeName)
        FileOutputStream(tempFile).use { it.write(bytes) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open PDF with...").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}