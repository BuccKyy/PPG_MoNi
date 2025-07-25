package com.example.ppg_moni.ui.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.ppg_moni.R

data class ProcessedDataItem(
    val sessionName: String,
    val segmentCount: Int,
    val timestamp: String,
    val totalSize: Long
)

class ProcessedDataAdapter(
    private val onItemClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<ProcessedDataAdapter.ViewHolder>() {

    private var items = listOf<ProcessedDataItem>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val sessionNameText: TextView = view.findViewById(R.id.sessionNameText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val segmentCountText: TextView = view.findViewById(R.id.segmentCountText)
        val fileSizeText: TextView = view.findViewById(R.id.fileSizeText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_processed_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.sessionNameText.text = formatSessionName(item.sessionName)
        holder.timestampText.text = item.timestamp
        holder.segmentCountText.text = "${item.segmentCount} segments"
        holder.fileSizeText.text = formatFileSize(item.totalSize)
        
        holder.cardView.setOnClickListener {
            onItemClick(item.sessionName)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(item.sessionName)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ProcessedDataItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    private fun formatSessionName(sessionName: String): String {
        return try {
            val parts = sessionName.split("_")
            if (parts.size >= 3) {
                "PPG Data - ${parts[1]} ${parts[2]}"
            } else {
                sessionName
            }
        } catch (e: Exception) {
            sessionName
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
} 