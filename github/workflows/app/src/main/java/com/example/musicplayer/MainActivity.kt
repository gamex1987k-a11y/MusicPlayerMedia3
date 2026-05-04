package com.example.musicplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
  private val audioList = mutableListOf<AudioItem>()
  private lateinit var adapter: AudioAdapter
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) loadAudio()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val recycler = findViewById<RecyclerView>(R.id.recycler)
    adapter = AudioAdapter(audioList) { item ->
      // Enviamos metadata y URI al servicio
      val intent = Intent(this, PlaybackService::class.java).apply {
        action = PlaybackService.ACTION_PLAY_FROM_URI
        data = Uri.parse(item.uriString)
        putExtra(PlaybackService.EXTRA_TITLE, item.title)
        putExtra(PlaybackService.EXTRA_ARTIST, item.artist)
        putExtra(PlaybackService.EXTRA_AUDIO_ID, item.id)
      }
      ContextCompat.startForegroundService(this, intent)
    }
    recycler.layoutManager = LinearLayoutManager(this)
    recycler.adapter = adapter

    checkPermissionAndLoad()
  }

  private fun checkPermissionAndLoad() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
      != PackageManager.PERMISSION_GRANTED) {
      requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
      loadAudio()
    }
  }

  private fun loadAudio() {
    scope.launch {
      val list = withContext(Dispatchers.IO) { queryAudio() }
      audioList.clear()
      audioList.addAll(list)
      adapter.notifyDataSetChanged()
    }
  }

  private fun queryAudio(): List<AudioItem> {
    val list = mutableListOf<AudioItem>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1"
    val cursor = contentResolver.query(uri, projection, selection, null, null)
    cursor?.use {
      val idIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val titleIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
      val artistIdx = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      while (it.moveToNext()) {
        val id = it.getLong(idIdx)
        val title = it.getString(titleIdx) ?: "Unknown"
        val artist = it.getString(artistIdx)
        val contentUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
        list.add(AudioItem(id, title, artist, contentUri.toString()))
      }
    }
    return list
  }

  override fun onDestroy() {
    super.onDestroy()
    scope.cancel()
  }
}
