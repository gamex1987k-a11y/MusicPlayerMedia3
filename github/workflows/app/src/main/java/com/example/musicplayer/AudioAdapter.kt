package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AudioAdapter(
  private val items: List<AudioItem>,
  private val onClick: (AudioItem) -> Unit
) : RecyclerView.Adapter<AudioAdapter.VH>() {

  inner class VH(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.title)
    val subtitle: TextView? = view.findViewById(R.id.subtitle)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val v = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
    return VH(v)
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    val item = items[position]
    holder.title.text = item.title
    holder.subtitle?.text = item.artist ?: ""
    holder.itemView.setOnClickListener { onClick(item) }
  }

  override fun getItemCount() = items.size
}
