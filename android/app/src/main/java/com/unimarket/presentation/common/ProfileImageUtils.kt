package com.unimarket.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

suspend fun uriToProfileImageData(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val original = BitmapFactory.decodeStream(input) ?: return@withContext null
            bitmapToProfileImageData(original)
        }
    }.getOrNull()
}

suspend fun bitmapToProfileImageData(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
    runCatching {
        val largestSide = max(bitmap.width, bitmap.height)
        val scale = if (largestSide > 420) 420f / largestSide else 1f
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        val out = ByteArrayOutputStream()
        var quality = 82
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 80_000 && quality > 35) {
            out.reset()
            quality -= 8
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        if (scaled !== bitmap) {
            scaled.recycle()
        }

        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        "data:image/jpeg;base64,$encoded"
    }.getOrNull()
}

@Composable
fun ProfileImage(
    imageData: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    val bitmap = remember(imageData) {
        val raw = imageData?.trim().orEmpty()
        if (!raw.startsWith("data:image/", ignoreCase = true)) {
            null
        } else {
            val payload = raw.substringAfter("base64,", missingDelimiterValue = "")
            runCatching {
                val bytes = Base64.decode(payload, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        fallback()
    }
}
