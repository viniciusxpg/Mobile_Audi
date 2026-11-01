package io.github.viniciusxpg.vinil.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaButtonReceiver
import io.github.viniciusxpg.vinil.MainActivity
import io.github.viniciusxpg.vinil.R

/**
 * Widget da tela inicial com a capa, o título e os botões anterior / tocar-pausar / próxima.
 *
 * Quem atualiza o conteúdo é o serviço de reprodução ([PlayerWidget.update]). Os botões enviam
 * comandos de mídia ao [MediaButtonReceiver] do Media3 — o mesmo caminho dos fones Bluetooth —,
 * então "tocar" funciona até com o app fechado (retoma a última música).
 */
class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Sem o serviço rodando, mostra a última música conhecida (sem capa) e o botão de play.
        val saved = PlayerWidget.lastKnown(context)
        manager.updateAppWidget(
            appWidgetIds,
            PlayerWidget.buildViews(context, saved.first, saved.second, isPlaying = false, art = null),
        )
    }
}

object PlayerWidget {
    private const val PREFS_NAME = "widget"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"

    /** Verdadeiro se há pelo menos um widget na tela inicial (evita trabalho à toa). */
    fun hasWidgets(context: Context): Boolean =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            .isNotEmpty()

    fun update(context: Context, title: String?, artist: String?, isPlaying: Boolean, art: Bitmap?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TITLE, title)
            putString(KEY_ARTIST, artist)
        }
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, buildViews(context, title, artist, isPlaying, art))
    }

    internal fun lastKnown(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TITLE, null) to prefs.getString(KEY_ARTIST, null)
    }

    internal fun buildViews(
        context: Context,
        title: String?,
        artist: String?,
        isPlaying: Boolean,
        art: Bitmap?,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_player).apply {
        setTextViewText(R.id.widget_title, title ?: context.getString(R.string.widget_nothing_playing))
        setTextViewText(R.id.widget_artist, artist ?: context.getString(R.string.app_name))
        if (art != null) {
            setImageViewBitmap(R.id.widget_art, art)
        } else {
            setImageViewResource(R.id.widget_art, R.drawable.widget_art_placeholder)
        }
        setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )
        setContentDescription(
            R.id.widget_play_pause,
            context.getString(if (isPlaying) R.string.action_pause else R.string.action_play),
        )

        setOnClickPendingIntent(R.id.widget_play_pause, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        setOnClickPendingIntent(R.id.widget_next, mediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT))
        setOnClickPendingIntent(R.id.widget_previous, mediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
        setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    }

    @OptIn(UnstableApi::class)
    private fun mediaButton(context: Context, keyCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .setComponent(ComponentName(context, MediaButtonReceiver::class.java))
            .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        return PendingIntent.getBroadcast(
            context,
            keyCode, // um requestCode por botão, para não sobrescrever os outros
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
