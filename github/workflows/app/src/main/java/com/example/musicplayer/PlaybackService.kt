package com.example.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerNotificationManager.MediaDescriptionAdapter
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {

  companion object {
    const val CHANNEL_ID = "playback_channel"
    const val CHANNEL_NAME = "Reproducción"
    const val NOTIF_ID = 1
    const val ACTION_PLAY_FROM_URI = "ACTION_PLAY_FROM_URI"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_ARTIST = "extra_artist"
    const val EXTRA_AUDIO_ID = "extra_audio_id"
  }

  private lateinit var player: ExoPlayer
  private lateinit var mediaSession: MediaSession
  private lateinit var notificationManager: NotificationManager
  private var playerNotificationManager: PlayerNotificationManager? = null
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  override fun onCreate() {
    super.onCreate()
    notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    createChannel()

    player = ExoPlayer.Builder(this).build()

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(
        PendingIntent.getActivity(
          this,
          0,
          Intent(this, MainActivity::class.java),
          PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
      )
      .build()

    playerNotificationManager = PlayerNotificationManager.Builder(
      this,
      NOTIF_ID,
      CHANNEL_ID
    )
      .setMediaDescriptionAdapter(object : MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
          val meta = player.currentMediaItem?.mediaMetadata
          return meta?.title ?: "Reproduciendo"
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
          val intent = Intent(this@PlaybackService, MainActivity::class.java)
          return PendingIntent.getActivity(
            this@PlaybackService,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
          val meta = player.currentMediaItem?.mediaMetadata
          return meta?.artist
        }

        override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap? {
          val meta = player.currentMediaItem?.mediaMetadata
          val extras = meta?.extras
          val audioId = extras?.getLong(EXTRA_AUDIO_ID, -1L) ?: -1L
          if (audioId > 0) {
            scope.launch {
              val bmp = ArtworkLoader.loadArtworkForAudio(this@PlaybackService, audioId)
              if (bmp != null) callback.onBitmap(bmp)
            }
          }
          return null
        }
      })
      .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {
          if (ongoing) {
            startForeground(notificationId, notification)
          } else {
            stopForeground(false)
            notificationManager.notify(notificationId, notification)
          }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
          stopSelf()
        }
      })
      .build()

    playerNotificationManager?.setPlayer(player)
    playerNotificationManager?.setMediaSessionToken(mediaSession.sessionCompatToken)

    player.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
          stopForeground(true)
          stopSelf()
        }
      }
    })
  }

  override fun onGetSession(controllerInfo: ControllerInfo): MediaSession = mediaSession

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    intent?.let {
      if (it.action == ACTION_PLAY_FROM_URI && it.data != null) {
        val uri = it.data!!
        val title = it.getStringExtra(EXTRA_TITLE)
        val artist = it.getStringExtra(EXTRA_ARTIST)
        val audioId = it.getLongExtra(EXTRA_AUDIO_ID, -1L)
        playUriWithMetadata(uri, title, artist, audioId)
      } else {
        MediaButtonReceiver.handleIntent(mediaSession.sessionCompatToken, it)
      }
    }
    return START_STICKY
  }

  private fun playUriWithMetadata(uri: Uri, title: String?, artist: String?, audioId: Long) {
    val metadataBuilder = MediaMetadata.Builder()
    if (!title.isNullOrEmpty()) metadataBuilder.setTitle(title)
    if (!artist.isNullOrEmpty()) metadataBuilder.setArtist(artist)
    val extras = Bundle().apply { putLong(EXTRA_AUDIO_ID, audioId) }
    metadataBuilder.setExtras(extras)

    val mediaItem = MediaItem.Builder()
      .setUri(uri)
      .setMediaMetadata(metadataBuilder.build())
      .build()

    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
  }

  private fun createChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
      notificationManager.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    playerNotificationManager?.setPlayer(null)
    scope.cancel()
    player.release()
    mediaSession.release()
    super.onDestroy()
  }
}
