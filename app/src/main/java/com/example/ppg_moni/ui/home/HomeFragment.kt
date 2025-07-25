package com.example.ppg_moni.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.ppg_moni.R
import com.example.ppg_moni.databinding.FragmentHomeBinding
import com.example.ppg_moni.ui.viewmodels.HomeViewModel

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Basic UI setup - will be enhanced later
        binding.welcomeText.text = "Chào mừng đến với PPG MoNi"
    }
    
    private fun observeViewModel() {
        // Observe view model data - will be enhanced later
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Handle loading state
        }
    }
    
    private fun setupClickListeners() {
        Log.d("HomeFragment", "Setting up click listeners")
        
        binding.uploadDataButton.setOnClickListener {
            Log.d("HomeFragment", "Upload Data button clicked")
            try {
                findNavController().navigate(R.id.navigation_data_management)
                Log.d("HomeFragment", "Navigation to data management successful")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation failed: ${e.message}")
                Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        
        binding.viewChartsButton.setOnClickListener {
            Log.d("HomeFragment", "View Charts button clicked") 
            try {
                findNavController().navigate(R.id.navigation_charts)
                Log.d("HomeFragment", "Navigation to charts successful")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Navigation failed: ${e.message}")
                Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 