package com.example.musicplayer

data class AudioItem(
  val id: Long,
  val title: String,
  val artist: String?,
  val uriString: String
)
