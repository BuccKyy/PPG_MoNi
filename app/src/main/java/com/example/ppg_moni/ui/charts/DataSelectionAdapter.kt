package com.example.ppg_moni.ui.charts

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.ppg_moni.R

data class DataFile(
    val filename: String,
    val displayName: String,
    val timestamp: String,
    val segmentCount: Int,
    val isSelected: Boolean = false
)

class DataSelectionAdapter(
    private var dataFiles: List<DataFile>,
    private val onFileSelected: (DataFile) -> Unit
) : RecyclerView.Adapter<DataSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val fileNameText: TextView = view.findViewById(R.id.fileNameText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val segmentCountText: TextView = view.findViewById(R.id.segmentCountText)
        val selectionIcon: ImageView = view.findViewById(R.id.selectionIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataFile = dataFiles[position]
        
        holder.fileNameText.text = dataFile.displayName
        holder.timestampText.text = dataFile.timestamp
        holder.segmentCountText.text = "Segments: ${dataFile.segmentCount}"
        
        // Set selection state
        if (dataFile.isSelected) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            holder.selectionIcon.setImageResource(R.drawable.ic_check_circle)
            holder.selectionIcon.setColorFilter(Color.parseColor("#2196F3"))
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            holder.selectionIcon.setImageResource(R.drawable.ic_radio_unchecked)
            holder.selectionIcon.setColorFilter(Color.parseColor("#9E9E9E"))
        }
        
        holder.cardView.setOnClickListener {
            onFileSelected(dataFile)
        }
        
        // Add animation
        holder.cardView.alpha = 0f
        holder.cardView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .start()
    }

    override fun getItemCount() = dataFiles.size

    fun updateData(newDataFiles: List<DataFile>) {
        dataFiles = newDataFiles
        notifyDataSetChanged()
    }
} 