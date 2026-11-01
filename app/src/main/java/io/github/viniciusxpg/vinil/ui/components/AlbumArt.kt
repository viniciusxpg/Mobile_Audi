package io.github.viniciusxpg.vinil.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.viniciusxpg.vinil.util.ArtworkCache
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
    // Se a capa já está no cache, aparece no primeiro quadro, sem "piscar".
    val cached = remember(contentUri, sizePx) {
        contentUri?.let { ArtworkCache.peek(it, sizePx)?.asImageBitmap() }
    }
    val artwork by produceState(initialValue = cached, contentUri, sizePx) {
        if (cached != null || contentUri == null || ArtworkCache.isMissing(contentUri, sizePx)) return@produceState
        value = withContext(Dispatchers.IO) {
            ArtworkCache.load(context, contentUri, sizePx)?.asImageBitmap()
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
