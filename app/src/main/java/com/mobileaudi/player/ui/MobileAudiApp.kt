package com.mobileaudi.player.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobileaudi.player.ui.screens.LibraryScreen
import com.mobileaudi.player.ui.screens.NowPlayingScreen
import com.mobileaudi.player.ui.screens.PermissionScreen
import com.mobileaudi.player.util.PermissionUtils

@Composable
fun MobileAudiApp(viewModel: PlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasPermission = PermissionUtils.hasAudioPermission(context)
    }

    // O usuário pode conceder a permissão pelas configurações e voltar ao app.
    LifecycleResumeEffect(Unit) {
        hasPermission = PermissionUtils.hasAudioPermission(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadLibrary()
    }

    if (!hasPermission) {
        PermissionScreen(
            onRequestPermission = { permissionLauncher.launch(PermissionUtils.runtimePermissions()) },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        )
        return
    }

    if (showNowPlaying && state.current != null) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(
            state = state,
            onClose = { showNowPlaying = false },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onSeek = viewModel::seekTo,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
        )
    } else {
        LibraryScreen(
            state = state,
            onSongClick = viewModel::playSong,
            onRefresh = { viewModel.loadLibrary(forceRefresh = true) },
            onPlayPause = viewModel::playPause,
            onNext = viewModel::next,
            onOpenNowPlaying = { showNowPlaying = true },
        )
    }
}
