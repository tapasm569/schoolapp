package com.school.manage.core.utils

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


