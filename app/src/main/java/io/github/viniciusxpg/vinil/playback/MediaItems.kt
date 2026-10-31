package io.github.viniciusxpg.vinil.playback

import android.content.ContentUris
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.viniciusxpg.vinil.data.Album
import io.github.viniciusxpg.vinil.data.Artist
import io.github.viniciusxpg.vinil.data.Folder
import io.github.viniciusxpg.vinil.data.Playlist
import io.github.viniciusxpg.vinil.data.Song
import io.github.viniciusxpg.vinil.data.uri

private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")

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
    browsableItem(
        mediaId = MediaIds.album(id),
        title = title,
        subtitle = "$artist • $songCountLabel",
        mediaType = MediaMetadata.MEDIA_TYPE_ALBUM,
        artworkUri = albumArtUri(id),
        artist = artist,
    )

fun Artist.toMediaItem(summary: String): MediaItem =
    browsableItem(
        mediaId = MediaIds.artist(name),
        title = name,
        subtitle = summary,
        mediaType = MediaMetadata.MEDIA_TYPE_ARTIST,
        artworkUri = songs.firstOrNull()?.let { albumArtUri(it.albumId) },
        artist = name,
    )

fun Folder.toMediaItem(songCountLabel: String): MediaItem =
    browsableItem(
        mediaId = MediaIds.folder(path),
        title = name,
        subtitle = songCountLabel,
        mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
    )

fun Playlist.toMediaItem(songCountLabel: String): MediaItem =
    browsableItem(
        mediaId = MediaIds.playlist(id),
        title = name,
        subtitle = songCountLabel,
        mediaType = MediaMetadata.MEDIA_TYPE_PLAYLIST,
    )

/** Pasta da raiz da biblioteca ("Todas as músicas", "Álbuns"...). */
fun folderItem(mediaId: String, title: String, mediaType: Int): MediaItem =
    browsableItem(mediaId = mediaId, title = title, mediaType = mediaType)

private fun browsableItem(
    mediaId: String,
    title: String,
    mediaType: Int,
    subtitle: String? = null,
    artworkUri: Uri? = null,
    artist: String? = null,
): MediaItem =
    MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setArtist(artist)
                .setArtworkUri(artworkUri)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .build(),
        )
        .build()
