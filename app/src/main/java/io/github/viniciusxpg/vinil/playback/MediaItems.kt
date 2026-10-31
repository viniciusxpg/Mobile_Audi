package io.github.viniciusxpg.vinil.playback

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.viniciusxpg.vinil.data.Album
import io.github.viniciusxpg.vinil.data.Song

private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")

fun songContentUri(songId: Long): Uri =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

fun albumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId)

/** Faixa tocável. */
fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(MediaIds.song(id))
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri(albumId))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()

/** Álbum navegável (pasta com as faixas). */
fun Album.toMediaItem(songCountLabel: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(MediaIds.album(id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle("$artist • $songCountLabel")
                .setArtist(artist)
                .setAlbumTitle(title)
                .setArtworkUri(albumArtUri(id))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
                .build(),
        )
        .build()

/** Pasta da raiz da biblioteca ("Todas as músicas", "Álbuns"...). */
fun folderItem(mediaId: String, title: String, mediaType: Int): MediaItem =
    MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .build(),
        )
        .build()
