package com.example.musicplayer

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArtworkLoader {
  // Intenta cargar artwork desde MediaStore para un audioId; devuelve Bitmap o null
  suspend fun loadArtworkForAudio(context: Context, audioId: Long): Bitmap? {
    return withContext(Dispatchers.IO) {
      try {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId)
        val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
          if (it.moveToFirst()) {
            val albumIdIdx = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            if (albumIdIdx >= 0) {
              val albumId = it.getLong(albumIdIdx)
              val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
              context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                return@withContext BitmapFactory.decodeStream(stream)
              }
            }
          }
        }
      } catch (e: Exception) {
        // ignore
      }
      null
    }
  }
}
