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
import com.example.ppg_moni.data.PatientData

class PatientDataAdapter(
    private var patientDataList: List<PatientData>,
    private val onItemClick: (PatientData) -> Unit
) : RecyclerView.Adapter<PatientDataAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val dateTimeText: TextView = view.findViewById(R.id.dateTimeText)
        val bloodPressureText: TextView = view.findViewById(R.id.bloodPressureText)
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val heartRateText: TextView = view.findViewById(R.id.heartRateText)
        val oxygenText: TextView = view.findViewById(R.id.oxygenText)
        val confidenceText: TextView = view.findViewById(R.id.confidenceText)
        val statusIndicator: View = view.findViewById(R.id.statusIndicator)
        val explanationText: TextView = view.findViewById(R.id.explanationText)
        val waveRatioText: TextView = view.findViewById(R.id.waveRatioText)
        val waveRatioExplanation: TextView = view.findViewById(R.id.waveRatioExplanation)
        val recommendationsText: TextView = view.findViewById(R.id.recommendationsText)
        val statusIcon: ImageView = view.findViewById(R.id.statusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_data, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val patientData = patientDataList[position]
        
        // Basic information
        holder.dateTimeText.text = patientData.readableTimestamp
        holder.bloodPressureText.text = "${patientData.systolic.toInt()}/${patientData.diastolic.toInt()} mmHg"
        holder.categoryText.text = patientData.bloodPressureCategory
        holder.heartRateText.text = "${patientData.heartRate.toInt()} BPM"
        holder.oxygenText.text = "SpO: ${String.format("%.1f", patientData.oxygenSaturation)}%"
        holder.confidenceText.text = String.format("%.0f%%", patientData.confidence * 100)
        
        // Wave ratio information
        holder.waveRatioText.text = String.format("%.2f", patientData.waveRatio)
        holder.waveRatioExplanation.text = patientData.waveRatioExplanation
        
        // Patient explanation
        holder.explanationText.text = patientData.explanation
        
        // Health recommendations
        val recommendations = patientData.healthRecommendations.joinToString("\n")
        holder.recommendationsText.text = recommendations
        
        // Status color and icon
        val statusColor = Color.parseColor(patientData.statusColor)
        holder.statusIndicator.setBackgroundColor(statusColor)
        holder.categoryText.setTextColor(statusColor)
        
        // Set status icon based on category
        when (patientData.bloodPressureCategory) {
            "Bình thường" -> {
                holder.statusIcon.setImageResource(R.drawable.ic_heart_healthy)
                holder.statusIcon.setColorFilter(Color.parseColor("#4CAF50"))
            }
            "Hơi cao" -> {
                holder.statusIcon.setImageResource(R.drawable.ic_heart_warning)
                holder.statusIcon.setColorFilter(Color.parseColor("#FF9800"))
            }
            "Cao độ 1" -> {
                holder.statusIcon.setImageResource(R.drawable.ic_heart_danger)
                holder.statusIcon.setColorFilter(Color.parseColor("#FF5722"))
            }
            "Cao độ 2" -> {
                holder.statusIcon.setImageResource(R.drawable.ic_heart_critical)
                holder.statusIcon.setColorFilter(Color.parseColor("#F44336"))
            }
            else -> {
                holder.statusIcon.setImageResource(R.drawable.ic_heart_unknown)
                holder.statusIcon.setColorFilter(Color.parseColor("#9E9E9E"))
            }
        }
        
        // Card background gradient based on category
        setCardBackground(holder.cardView, patientData.bloodPressureCategory)
        
        // Click listener
        holder.cardView.setOnClickListener {
            onItemClick(patientData)
        }
        
        // Add subtle animation
        holder.cardView.alpha = 0f
        holder.cardView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(position * 50L)
            .start()
    }
    
    private fun setCardBackground(cardView: CardView, category: String) {
        when (category) {
            "Bình thường" -> {
                cardView.setCardBackgroundColor(Color.parseColor("#F1F8E9"))
            }
            "Hơi cao" -> {
                cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
            }
            "Cao độ 1" -> {
                cardView.setCardBackgroundColor(Color.parseColor("#FBE9E7"))
            }
            "Cao độ 2" -> {
                cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            else -> {
                cardView.setCardBackgroundColor(Color.parseColor("#FAFAFA"))
            }
        }
    }

    override fun getItemCount() = patientDataList.size

    fun updateData(newPatientDataList: List<PatientData>) {
        patientDataList = newPatientDataList
        notifyDataSetChanged()
    }
} 