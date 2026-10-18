package com.school.manage.core.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    size: Dp = 46.dp,
    cornerRadius: Dp? = null,
    backgroundColor: Color = Color(0xFF2563EB).copy(alpha = 0.15f),
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
                    .border(1.dp, backgroundColor, shape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                val initials = name.trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }

                if (fallbackEmoji.isNotEmpty()) {
                    Text(
                        text = fallbackEmoji,
                        fontSize = (size.value * 0.46f).sp
                    )
                } else {
                    Text(
                        text = initials.ifEmpty { "👤" },
                        fontSize = (size.value * 0.38f).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
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
