package com.school.manage.core.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
expect fun rememberPhotoPickerLauncher(onPhotoSelected: (String) -> Unit): () -> Unit

@Composable
expect fun rememberCsvFilePickerLauncher(onFileContentLoaded: (String) -> Unit): () -> Unit


@Composable
expect fun rememberCameraCaptureLauncher(onPhotoCaptured: (String) -> Unit): () -> Unit

@Composable
expect fun rememberPdfPickerLauncher(onPdfSelected: (base64: String, fileName: String) -> Unit): () -> Unit

expect fun openPdfAttachment(base64Pdf: String, fileName: String)

expect fun decodeBase64ToImageBitmap(base64Str: String): ImageBitmap?

@Composable
fun ProfileAvatar(
    photoBase64: String,
    name: String = "",
    fallbackEmoji: String = "👤",
    size: Dp = 48.dp,
    cornerRadius: Dp? = null,
    backgroundColor: Color = Color(0xFF2563EB).copy(alpha = 0.14f),
    textColor: Color = Color(0xFF2563EB),
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val imageBitmap = remember(photoBase64) {
        if (photoBase64.isNotBlank()) decodeBase64ToImageBitmap(photoBase64) else null
    }
    val shape = if (cornerRadius != null) RoundedCornerShape(cornerRadius) else CircleShape

    Box(
        modifier = Modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = name.ifEmpty { "Profile Photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .border(1.5.dp, textColor.copy(alpha = 0.35f), shape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor)
                    .border(1.dp, textColor.copy(alpha = 0.25f), shape),
                contentAlignment = Alignment.Center
            ) {
                val initials = name.trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }

                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        fontSize = (size.value * 0.36f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                } else {
                    Text(
                        text = fallbackEmoji.ifEmpty { "👤" },
                        fontSize = (size.value * 0.44f).sp
                    )
                }
            }
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ContactQuickActions(
    phone: String,
    whatsapp: String = "",
    messageText: String = "",
    profileAction: (() -> Unit)? = null,
    removeAction: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val cleanPhone = phone.filter { it.isDigit() }
    val cleanWa = whatsapp.ifBlank { phone }.filter { it.isDigit() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xFF2563EB).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
            modifier = Modifier.weight(1f).height(32.dp)
                .clickable(enabled = cleanPhone.isNotEmpty()) {
                    try {
                        uriHandler.openUri("tel:$cleanPhone")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📞 Call",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB),
                    maxLines = 1
                )
            }
        }

        // WhatsApp
        Surface(
            shape = RoundedCornerShape(7.dp),
            color = Color(0xFF16A34A).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f)),
            modifier = Modifier.weight(1.35f).height(32.dp)
                .clickable(enabled = cleanWa.isNotEmpty()) {
                    try {
                        val formattedWa =
                            if (cleanWa.length == 10) "91$cleanWa" else cleanWa
                        val url = if (messageText.isNotBlank()) {
                            "https://wa.me/$formattedWa?text=" +
                                encodeWhatsAppMessage(messageText)
                        } else {
                            "https://wa.me/$formattedWa"
                        }
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "💬 WhatsApp",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    maxLines = 1
                )
            }
        }

        // Profile
        if (profileAction != null) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFF2563EB).copy(alpha = 0.12f),
                modifier = Modifier.weight(1.15f).height(32.dp)
                    .clickable { profileAction() }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✏️ Profile",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        maxLines = 1
                    )
                }
            }
        }

        // Remove
        if (removeAction != null) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.12f),
                modifier = Modifier.weight(1f).height(32.dp)
                    .clickable { removeAction() }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Remove",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun encodeWhatsAppMessage(message: String): String {
    val bytes = message.encodeToByteArray()
    val hex = "0123456789ABCDEF"
    return buildString {
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            val unreserved =
                value in 65..90 ||
                value in 97..122 ||
                value in 48..57 ||
                value == 45 || value == 46 ||
                value == 95 || value == 126

            if (unreserved) {
                append(value.toChar())
            } else {
                append('%')
                append(hex[value shr 4])
                append(hex[value and 15])
            }
        }
    }
}
