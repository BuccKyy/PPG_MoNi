package com.example.ppg_moni.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.ppg_moni.data.models.BloodPressurePrediction
import com.example.ppg_moni.ml.PPGDataProcessor
import kotlinx.coroutines.launch

class ChartViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _predictions = MutableLiveData<List<BloodPressurePrediction>>()
    val predictions: LiveData<List<BloodPressurePrediction>> = _predictions
    
    private val _filteredPredictions = MutableLiveData<List<BloodPressurePrediction>>()
    val filteredPredictions: LiveData<List<BloodPressurePrediction>> = _filteredPredictions
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun loadPredictions() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load real patient data with AI predictions
                val ppgProcessor = PPGDataProcessor(getApplication())
                val patientDataList = ppgProcessor.loadPatientData()
                
                // Convert to BloodPressurePrediction
                val predictions = patientDataList.map { dataEntry ->
                    BloodPressurePrediction(
                        id = dataEntry.id,
                        userId = "patient_user",
                        ppgDataId = dataEntry.patientId,
                        systolicBP = dataEntry.systolicBP,
                        diastolicBP = dataEntry.diastolicBP,
                        confidence = dataEntry.confidence.toFloat(),
                        category = classifyBloodPressure(dataEntry.systolicBP.toInt(), dataEntry.diastolicBP.toInt()),
                        riskLevel = assessRiskLevel(dataEntry.systolicBP.toInt(), dataEntry.diastolicBP.toInt()),
                        heartRate = dataEntry.heartRate.toFloat(),
                        predictedAt = dataEntry.timestamp
                    )
                }
                
                _predictions.value = predictions
                _isLoading.value = false
                
                Log.d("ChartViewModel", "Loaded ${predictions.size} predictions with AI analysis")
                
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Error loading predictions: ${e.message}")
                _isLoading.value = false
                _predictions.value = emptyList()
            }
        }
    }
    
    fun filterByDays(days: Int) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val filtered = _predictions.value?.filter { it.predictedAt >= cutoffTime } ?: emptyList()
        _filteredPredictions.value = filtered
    }
    
    fun showAllPredictions() {
        _filteredPredictions.value = _predictions.value ?: emptyList()
    }
    
    fun exportChart(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                // TODO: Implement chart export
                // saveImageToGallery(bitmap)
            } catch (e: Exception) {
                _errorMessage.value = "Export failed: ${e.message}"
            }
        }
    }
    
    fun shareChart(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                // TODO: Implement chart sharing
                // shareImage(bitmap)
            } catch (e: Exception) {
                _errorMessage.value = "Share failed: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }

    private fun classifyBloodPressure(systolic: Int, diastolic: Int): com.example.ppg_moni.data.models.BPCategory {
        return when {
            systolic < 90 || diastolic < 60 -> com.example.ppg_moni.data.models.BPCategory.NORMAL
            systolic < 120 && diastolic < 80 -> com.example.ppg_moni.data.models.BPCategory.NORMAL
            systolic < 130 && diastolic < 80 -> com.example.ppg_moni.data.models.BPCategory.ELEVATED
            systolic < 140 || diastolic < 90 -> com.example.ppg_moni.data.models.BPCategory.STAGE_1_HYPERTENSION
            systolic < 180 || diastolic < 120 -> com.example.ppg_moni.data.models.BPCategory.STAGE_2_HYPERTENSION
            else -> com.example.ppg_moni.data.models.BPCategory.HYPERTENSIVE_CRISIS
        }
    }
    
    private fun assessRiskLevel(systolic: Int, diastolic: Int): com.example.ppg_moni.data.models.RiskLevel {
        return when {
            systolic < 90 || diastolic < 60 -> com.example.ppg_moni.data.models.RiskLevel.MODERATE
            systolic < 120 && diastolic < 80 -> com.example.ppg_moni.data.models.RiskLevel.LOW
            systolic < 130 && diastolic < 80 -> com.example.ppg_moni.data.models.RiskLevel.LOW
            systolic < 140 || diastolic < 90 -> com.example.ppg_moni.data.models.RiskLevel.MODERATE
            systolic < 180 || diastolic < 120 -> com.example.ppg_moni.data.models.RiskLevel.HIGH
            else -> com.example.ppg_moni.data.models.RiskLevel.CRITICAL  // Use CRITICAL instead of VERY_HIGH
        }
    }
} 