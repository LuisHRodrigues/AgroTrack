package com.example.agrotrack.ui.editar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrack.R

class RebanhoAdapter(
    private val rebanhos: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<RebanhoAdapter.RebanhoViewHolder>() {

    inner class RebanhoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNomeRebanho: TextView = itemView.findViewById(R.id.tvNomeRebanho)

        fun bind(nomeRebanho: String) {
            tvNomeRebanho.text = nomeRebanho

            itemView.setOnClickListener {
                onItemClick(nomeRebanho)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RebanhoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rebanho, parent, false)
        return RebanhoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RebanhoViewHolder, position: Int) {
        holder.bind(rebanhos[position])
    }

    override fun getItemCount(): Int = rebanhos.size
}