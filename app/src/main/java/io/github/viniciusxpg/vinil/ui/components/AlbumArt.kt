package io.github.viniciusxpg.vinil.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.util.loadArtworkBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Capa do álbum lida do próprio arquivo de áudio, com um ícone quando não houver capa. */
@Composable
fun AlbumArt(
    contentUri: Uri?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    sizePx: Int = 512,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, contentUri, sizePx) {
        value = contentUri?.let { uri ->
            withContext(Dispatchers.IO) { loadArtwork(context, uri, sizePx) }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = artwork
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f),
            )
        }
    }
}

private fun loadArtwork(context: Context, uri: Uri, sizePx: Int): ImageBitmap? =
    loadArtworkBitmap(context, uri, sizePx)?.asImageBitmap()
