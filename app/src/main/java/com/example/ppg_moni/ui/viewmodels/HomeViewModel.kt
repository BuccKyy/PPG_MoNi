package com.example.ppg_moni.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ppg_moni.data.models.BloodPressurePrediction
import com.example.ppg_moni.data.models.UserProfile
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    
    private val _latestPrediction = MutableLiveData<BloodPressurePrediction?>()
    val latestPrediction: LiveData<BloodPressurePrediction?> = _latestPrediction
    
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Load from database when ready
                // loadLatestPrediction()
                // loadUserProfile()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshData() {
        loadData()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
} 