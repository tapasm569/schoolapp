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
    messageText: String = ""
) {
    val uriHandler = LocalUriHandler.current
    val cleanPhone = phone.filter { it.isDigit() }
    val cleanWa = whatsapp.ifBlank { phone }.filter { it.isDigit() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Direct Call Button
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2563EB).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
            modifier = Modifier
                .height(30.dp)
                .clickable(enabled = cleanPhone.isNotEmpty()) {
                    try {
                        uriHandler.openUri("tel:$cleanPhone")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("📞", fontSize = 12.sp)
                Text(
                    "Call",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )
            }
        }

        // Direct WhatsApp Button
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF16A34A).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.35f)),
            modifier = Modifier
                .height(30.dp)
                .clickable(enabled = cleanWa.isNotEmpty()) {
                    try {
                        val formattedWa = if (cleanWa.length == 10) "91$cleanWa" else cleanWa
                        val url = if (messageText.isNotBlank()) {
                            "https://wa.me/$formattedWa?text=${messageText.replace(" ", "%20")}"
                        } else {
                            "https://wa.me/$formattedWa"
                        }
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("💬", fontSize = 12.sp)
                Text(
                    "WhatsApp",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )
            }
        }
    }
}
