package com.example.ppg_moni.ui.data

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ppg_moni.R
import com.example.ppg_moni.data.DataProcessor
import com.example.ppg_moni.data.PythonStyleDataProcessor
import com.example.ppg_moni.data.DataAnalyzer
import com.example.ppg_moni.databinding.FragmentDataManagementBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class DataManagementFragment : Fragment() {
    
    companion object {
        private const val TAG = "DataManagementFragment"
    }
    
    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var dataProcessor: DataProcessor
    private lateinit var pythonStyleProcessor: PythonStyleDataProcessor
    private lateinit var processedDataAdapter: ProcessedDataAdapter
    
    private var selectedFile: File? = null
    
    // Permissions launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.forEach { (permission, granted) ->
            if (!granted) {
                allGranted = false
                Log.w(TAG, "Permission denied: $permission")
            }
        }
        
        if (allGranted) {
            Log.d(TAG, "All permissions granted, opening file picker")
            launchFilePicker()
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền truy cập file để upload dữ liệu", Toast.LENGTH_LONG).show()
        }
    }
    
    // Storage access launcher for Android 11+
    private val storageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d(TAG, "Storage access granted")
                launchFilePicker()
            } else {
                Toast.makeText(requireContext(), "Cần cấp quyền quản lý file để upload dữ liệu", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        } else {
            Log.d(TAG, "File picker cancelled or failed")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataManagementBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupComponents()
        setupClickListeners()
        loadProcessedData()
    }
    
    private fun setupComponents() {
        dataProcessor = DataProcessor(requireContext())
        
        // Setup Python-style processor for better device_data compatibility
        pythonStyleProcessor = PythonStyleDataProcessor(requireContext())
        
        // Setup RecyclerView
        processedDataAdapter = ProcessedDataAdapter(
            onItemClick = { fileName ->
                viewProcessedData(fileName)
            },
            onDeleteClick = { fileName ->
                deleteProcessedData(fileName)
            }
        )
        
        binding.processedDataRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = processedDataAdapter
        }
    }
    
    private fun setupClickListeners() {
        // Select file button
        binding.selectFileButton.setOnClickListener {
            openFilePicker()
        }
        
        // Remove selected file button
        binding.removeFileButton.setOnClickListener {
            removeSelectedFile()
        }
        
        // Process button
        binding.processButton.setOnClickListener {
            selectedFile?.let { file ->
                processSelectedFile(file)
            }
        }
        
        // View results button
        binding.viewResultsButton.setOnClickListener {
            navigateToCharts()
        }
        
        // Process another button
        binding.processAnotherButton.setOnClickListener {
            resetProcessingState()
        }
        
        // Refresh data button
        binding.refreshDataButton.setOnClickListener {
            loadProcessedData()
        }
        
        // Test sample data button
        binding.testSampleDataButton.setOnClickListener {
            testWithSampleData()
        }
        
        // Long click for advanced test menu
        binding.testSampleDataButton.setOnLongClickListener {
            showAdvancedTestMenu()
            true
        }
    }
    
    private fun openFilePicker() {
        Log.d(TAG, "Opening file picker...")
        
        // Check and request permissions first
        if (hasRequiredPermissions()) {
            launchFilePicker()
        } else {
            requestPermissions()
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - need MANAGE_EXTERNAL_STORAGE or scoped storage
                Environment.isExternalStorageManager() || 
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-10 - need READ_EXTERNAL_STORAGE
                checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> true // Below Android 6, no runtime permissions needed
        }
    }
    
    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+ - request MANAGE_EXTERNAL_STORAGE
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                    storageAccessLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting storage access: ${e.message}")
                    // Fallback to regular permissions
                    requestRegularPermissions()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                requestRegularPermissions()
            }
            else -> {
                // No permissions needed
                launchFilePicker()
            }
        }
    }
    
    private fun requestRegularPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            permissions.addAll(listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            ))
        } else {
            // Android 6-12
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    private fun launchFilePicker() {
        Log.d(TAG, "Launching file picker...")
        
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",
                "application/x-numpy-data",
                "*/*"
            ))
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Chọn file .npy từ device_data"))
        } catch (e: Exception) {
            Log.e(TAG, "Error launching file picker: ${e.message}")
            Toast.makeText(requireContext(), "Không thể mở file picker: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleSelectedFile(uri: Uri) {
        Log.d(TAG, "Handling selected file: $uri")
        
        try {
            val contentResolver = requireContext().contentResolver
            
            // Get file info
            val fileName = getFileNameFromUri(uri) ?: "unknown_file.npy"
            val fileSize = getFileSizeFromUri(uri)
            
            Log.d(TAG, "File info: name=$fileName, size=$fileSize bytes")
            
            // Validate file type
            if (!fileName.endsWith(".npy", ignoreCase = true)) {
                Toast.makeText(requireContext(), "Vui lòng chọn file .npy", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Check file size
            if (fileSize > 50 * 1024 * 1024) { // 50MB limit
                Toast.makeText(requireContext(), "File quá lớn (>50MB). Vui lòng chọn file nhỏ hơn.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val inputStream = contentResolver.openInputStream(uri)
            
            if (inputStream != null) {
                // Copy file to internal storage
                val internalFile = File(requireContext().cacheDir, fileName)
                val outputStream = FileOutputStream(internalFile)
                
                val bytesRead = inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                
                Log.d(TAG, "File copied successfully: $bytesRead bytes written to ${internalFile.absolutePath}")
                
                // Validate copied file
                if (internalFile.exists() && internalFile.length() > 0) {
                    // Update UI
                    selectedFile = internalFile
                    updateSelectedFileUI(internalFile)
                    
                    Log.d(TAG, "File selected successfully: ${internalFile.name}, size: ${internalFile.length()} bytes")
                    Toast.makeText(requireContext(), "Đã chọn file: $fileName", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "File copy failed or resulted in empty file")
                    Toast.makeText(requireContext(), "Lỗi: File copy không thành công", Toast.LENGTH_SHORT).show()
                }
                
            } else {
                Log.e(TAG, "Cannot open input stream for URI: $uri")
                Toast.makeText(requireContext(), "Không thể đọc file. Vui lòng thử file khác.", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception accessing file: ${e.message}")
            Toast.makeText(requireContext(), "Không có quyền truy cập file. Vui lòng cấp quyền.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file: ${e.message}")
            e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi khi xử lý file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                it.getString(nameIndex)
            } else {
                null
            }
        }
    }
    
    private fun getFileSizeFromUri(uri: Uri): Long {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex >= 0 && it.moveToFirst()) {
                it.getLong(sizeIndex)
            } else {
                0L
            }
        } ?: 0L
    }
    
    private fun updateSelectedFileUI(file: File) {
        binding.selectedFileLayout.visibility = View.VISIBLE
        binding.selectedFileNameText.text = file.name
        binding.selectedFileSizeText.text = "Size: ${formatFileSize(file.length())}"
        
        // Enable process button
        binding.processButton.isEnabled = true
        binding.processButton.alpha = 1.0f
    }
    
    private fun removeSelectedFile() {
        selectedFile?.delete()
        selectedFile = null
        
        binding.selectedFileLayout.visibility = View.GONE
        binding.processButton.isEnabled = false
        binding.processButton.alpha = 0.6f
    }
    
    private fun processSelectedFile(file: File) {
        // Show progress UI
        binding.progressCard.visibility = View.VISIBLE
        binding.resultsCard.visibility = View.GONE
        
        // Disable buttons during processing
        binding.selectFileButton.isEnabled = false
        binding.processButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Processing file with Python-style processor: ${file.name}")
                
                // Use Python-style processor for better device_data compatibility
                val result = pythonStyleProcessor.processRawPPGData(file) { progress ->
                    // Update progress UI on main thread
                    requireActivity().runOnUiThread {
                        updateProgressUI(adaptProgress(progress))
                    }
                }
                
                // Show results
                showProcessingResults(adaptResult(result))
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing file: ${e.message}")
                hideProgressUI()
                Toast.makeText(requireContext(), "Lỗi xử lý: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Re-enable buttons
                binding.selectFileButton.isEnabled = true
                binding.processButton.isEnabled = true
            }
        }
    }
    
    // Adapt PythonStyleDataProcessor results to DataProcessor format
    private fun adaptProgress(progress: PythonStyleDataProcessor.ProcessingProgress): DataProcessor.ProcessingProgress {
        return DataProcessor.ProcessingProgress(progress.stage, progress.progress, progress.message)
    }
    
    private fun adaptResult(result: PythonStyleDataProcessor.ProcessingResult): DataProcessor.ProcessingResult {
        return DataProcessor.ProcessingResult(result.success, result.message, result.outputFiles, result.segmentCount)
    }
    
    private fun updateProgressUI(progress: DataProcessor.ProcessingProgress) {
        binding.progressStageText.text = progress.stage
        binding.progressPercentText.text = "${(progress.progress * 100).toInt()}%"
        binding.processingProgressBar.progress = (progress.progress * 100).toInt()
        binding.progressMessageText.text = progress.message
    }
    
    private fun showProcessingResults(result: DataProcessor.ProcessingResult) {
        binding.progressCard.visibility = View.GONE
        binding.resultsCard.visibility = View.VISIBLE
        
        if (result.success) {
            binding.resultsTitle.text = "✅ Xử lý hoàn thành!"
            binding.resultsTitle.setTextColor(resources.getColor(R.color.success_color, null))
            binding.resultsMessage.text = result.message
            binding.segmentCountText.text = result.segmentCount.toString()
            
            // Calculate total file size
            val totalSize = result.outputFiles.sumOf { fileName ->
                val file = File(File(requireContext().filesDir, "normalized_data"), fileName)
                if (file.exists()) file.length() else 0L
            }
            binding.fileSizeText.text = formatFileSize(totalSize)
            
            // Refresh processed data list
            loadProcessedData()
            
            Log.d(TAG, "Processing successful: ${result.segmentCount} segments created")
            
        } else {
            binding.resultsTitle.text = "❌ Xử lý thất bại"
            binding.resultsTitle.setTextColor(resources.getColor(R.color.error_color, null))
            
            // Show detailed error message
            val detailedMessage = when {
                result.message.contains("Không thể đọc dữ liệu") -> 
                    "File không đúng định dạng .npy hoặc bị hỏng.\n\nGợi ý:\n• Kiểm tra file có đúng từ device_data không\n• Thử file khác\n• Sử dụng Test sample data để kiểm tra"
                
                result.message.contains("Dữ liệu không hợp lệ sau tiền xử lý") -> 
                    "Dữ liệu PPG không phù hợp để xử lý.\n\nNguyên nhân có thể:\n• File không chứa tín hiệu PPG\n• Dữ liệu quá ít hoặc nhiễu quá lớn\n• Định dạng multi-channel không đúng\n\nGợi ý: Thử Test sample data để xác nhận app hoạt động"
                
                result.message.contains("Không thể tạo segments") -> 
                    "Tín hiệu PPG quá ngắn để tạo segments.\n\nYêu cầu:\n• Tối thiểu 30 giây dữ liệu\n• Tần số lấy mẫu 25Hz\n• Tín hiệu chất lượng tốt"
                
                else -> result.message
            }
            
            binding.resultsMessage.text = detailedMessage
            binding.segmentCountText.text = "0"
            binding.fileSizeText.text = "0 KB"
            
            Log.e(TAG, "Processing failed: ${result.message}")
        }
    }
    
    private fun hideProgressUI() {
        binding.progressCard.visibility = View.GONE
    }
    
    private fun resetProcessingState() {
        binding.resultsCard.visibility = View.GONE
        removeSelectedFile()
    }
    
    private fun loadProcessedData() {
        val processedFiles = dataProcessor.getProcessedDataFiles()
        
        if (processedFiles.isEmpty()) {
            binding.processedDataRecyclerView.visibility = View.GONE
            binding.emptyDataLayout.visibility = View.VISIBLE
        } else {
            binding.processedDataRecyclerView.visibility = View.VISIBLE
            binding.emptyDataLayout.visibility = View.GONE
            
            // Group files by session
            val sessionGroups = processedFiles.groupBy { fileName ->
                fileName.substringBeforeLast("_seg")
            }
            
            val sessionList = sessionGroups.map { (sessionName, files) ->
                ProcessedDataItem(
                    sessionName = sessionName,
                    segmentCount = files.size,
                    timestamp = extractTimestamp(sessionName),
                    totalSize = calculateSessionSize(files)
                )
            }.sortedByDescending { it.timestamp }
            
            processedDataAdapter.updateData(sessionList)
        }
    }
    
    private fun extractTimestamp(sessionName: String): String {
        return try {
            val parts = sessionName.split("_")
            if (parts.size >= 3) {
                val date = parts[1] // YYMMDD
                val time = parts[2] // HHMMSS
                
                val day = date.substring(4, 6)
                val month = date.substring(2, 4)
                val year = "20" + date.substring(0, 2)
                
                val hour = time.substring(0, 2)
                val minute = time.substring(2, 4)
                
                "$day/$month/$year $hour:$minute"
            } else {
                sessionName
            }
        } catch (e: Exception) {
            sessionName
        }
    }
    
    private fun calculateSessionSize(files: List<String>): Long {
        return files.sumOf { fileName ->
            val file = File(File(requireContext().filesDir, "normalized_data"), fileName)
            if (file.exists()) file.length() else 0L
        }
    }
    
    private fun viewProcessedData(sessionName: String) {
        // Navigate to charts with this specific session
        try {
            val bundle = Bundle().apply {
                putString("session_name", sessionName)
            }
            findNavController().navigate(R.id.navigation_charts, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            Toast.makeText(requireContext(), "Lỗi navigation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteProcessedData(sessionName: String) {
        // Find and delete all files for this session
        val allFiles = dataProcessor.getProcessedDataFiles()
        val sessionFiles = allFiles.filter { it.startsWith(sessionName) }
        
        var deletedCount = 0
        sessionFiles.forEach { fileName ->
            if (dataProcessor.deleteProcessedData(fileName)) {
                deletedCount++
            }
        }
        
        if (deletedCount > 0) {
            Toast.makeText(requireContext(), "Đã xóa $deletedCount files", Toast.LENGTH_SHORT).show()
            loadProcessedData()
        } else {
            Toast.makeText(requireContext(), "Không thể xóa dữ liệu", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToCharts() {
        try {
            findNavController().navigate(R.id.navigation_charts)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            Toast.makeText(requireContext(), "Lỗi navigation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testWithSampleData() {
        Log.d(TAG, "Testing with sample data...")
        
        // Show progress UI
        binding.progressCard.visibility = View.VISIBLE
        binding.resultsCard.visibility = View.GONE
        
        // Create sample PPG data similar to device_data
        lifecycleScope.launch {
            try {
                // Show loading
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Generating", 0.1f, "Tạo sample data..."))
                }
                
                val sampleFile = createSampleDataFile()
                
                // Show processing
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Processing", 0.3f, "Xử lý sample data..."))
                }
                
                // Process the sample data
                val result = dataProcessor.processRawPPGData(sampleFile) { progress ->
                    requireActivity().runOnUiThread {
                        updateProgressUI(progress)
                    }
                }
                
                // Show results
                showProcessingResults(result)
                
                // Clean up sample file
                sampleFile.delete()
                
                if (result.success) {
                    Toast.makeText(requireContext(), "Test thành công! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Test thất bại: ${result.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing with sample data: ${e.message}")
                hideProgressUI()
                Toast.makeText(requireContext(), "Lỗi test: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun createSampleDataFile(): File {
        // Create realistic PPG sample data
        val sampleData = generateSamplePPGData()
        
        // Create sample file
        val sampleFile = File(requireContext().cacheDir, "sample_maxim_250110_120000.npy")
        
        // Write as simplified numpy format
        val headerBytes = createSimpleNumpyHeader(sampleData.size)
        val dataBytes = ByteArray(sampleData.size * 4)
        
        for (i in sampleData.indices) {
            val bits = sampleData[i].toBits()
            val baseIndex = i * 4
            dataBytes[baseIndex] = (bits and 0xFF).toByte()
            dataBytes[baseIndex + 1] = ((bits shr 8) and 0xFF).toByte()
            dataBytes[baseIndex + 2] = ((bits shr 16) and 0xFF).toByte()
            dataBytes[baseIndex + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        
        sampleFile.writeBytes(headerBytes + dataBytes)
        
        Log.d(TAG, "Created sample file: ${sampleFile.absolutePath}, size: ${sampleFile.length()} bytes")
        
        return sampleFile
    }
    
    private fun generateSamplePPGData(): FloatArray {
        // Generate realistic PPG signal similar to device_data
        val samplingRate = 25f // Hz
        val duration = 60f // seconds
        val sampleCount = (samplingRate * duration).toInt()
        
        val data = FloatArray(sampleCount * 4) // 4 channels like real device_data
        
        for (i in 0 until sampleCount) {
            val time = i / samplingRate
            
            // Simulate PPG signal with heartbeat
            val heartRate = 75.0 // BPM
            val heartPhase = 2 * kotlin.math.PI * heartRate * time / 60.0
            
            // Generate synthetic PPG waveform
            val ppgSignal = -200000f + // Baseline offset (similar to real data range)
                           -50000f * kotlin.math.sin(heartPhase).toFloat() + // Primary heart signal
                           -10000f * kotlin.math.sin(2 * heartPhase).toFloat() + // Harmonic
                           -5000f * kotlin.math.sin(3 * heartPhase).toFloat() + // Higher harmonic
                           kotlin.random.Random.nextFloat() * 1000f - 500f // Noise
            
            // Fill all 4 channels (interleaved format)
            data[i * 4] = ppgSignal * 0.8f + kotlin.random.Random.nextFloat() * 1000f // Channel 0
            data[i * 4 + 1] = ppgSignal // Channel 1 (main PPG)
            data[i * 4 + 2] = ppgSignal * 1.2f + kotlin.random.Random.nextFloat() * 1000f // Channel 2
            data[i * 4 + 3] = kotlin.random.Random.nextFloat() * 5000f // Channel 3 (noise)
        }
        
        Log.d(TAG, "Generated ${data.size} sample data points (${sampleCount} samples x 4 channels)")
        
        return data
    }
    
    private fun createSimpleNumpyHeader(dataSize: Int): ByteArray {
        val header = "\u0093NUMPY\u0001\u0000{'descr': '<f4', 'fortran_order': False, 'shape': ($dataSize,), }\n"
        return header.toByteArray()
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
    
    private fun showAdvancedTestMenu() {
        val options = arrayOf(
            "🧪 Test với synthetic data",
            "📋 Test với normalized data mẫu", 
            "🐍 Test với Python-style processor",
            "🔍 Analyze normalized data structure"
        )
        
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Advanced Testing")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> testWithSampleData()
                1 -> testWithNormalizedSample()
                2 -> testWithPythonStyleProcessor()
                3 -> analyzeNormalizedDataStructure()
            }
        }
        builder.show()
    }
    
    private fun testWithNormalizedSample() {
        Log.d(TAG, "Testing with real normalized data sample...")
        
        lifecycleScope.launch {
            try {
                // Show progress
                binding.progressCard.visibility = View.VISIBLE
                binding.resultsCard.visibility = View.GONE
                
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Loading", 0.1f, "Đang load normalized data mẫu..."))
                }
                
                // Load sample from normalized_data in assets
                val assetManager = requireContext().assets
                val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
                
                if (normalizedFiles.isEmpty()) {
                    throw Exception("Không tìm thấy normalized data trong assets")
                }
                
                // Take a sample file
                val sampleFile = normalizedFiles.first()
                val inputStream = assetManager.open("normalized_data/$sampleFile")
                val sampleBytes = inputStream.readBytes()
                inputStream.close()
                
                // Create temp file to simulate upload
                val tempFile = File(requireContext().cacheDir, "test_$sampleFile")
                tempFile.writeBytes(sampleBytes)
                
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Processing", 0.3f, "Đang xử lý normalized data..."))
                }
                
                // Process through our pipeline
                val result = dataProcessor.processRawPPGData(tempFile) { progress ->
                    requireActivity().runOnUiThread {
                        updateProgressUI(progress)
                    }
                }
                
                // Clean up
                tempFile.delete()
                
                // Show results
                showProcessingResults(result)
                
                if (result.success) {
                    Toast.makeText(requireContext(), "Test với normalized data thành công! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Test thất bại: ${result.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing with normalized sample: ${e.message}")
                hideProgressUI()
                Toast.makeText(requireContext(), "Lỗi test: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun analyzeNormalizedDataStructure() {
        Log.d(TAG, "Analyzing normalized data structure...")
        
        lifecycleScope.launch {
            try {
                val analyzer = DataAnalyzer(requireContext())
                val comparison = analyzer.compareWithNormalizedData()
                
                // Show analysis results in dialog
                requireActivity().runOnUiThread {
                    val builder = android.app.AlertDialog.Builder(requireContext())
                    builder.setTitle("📊 Normalized Data Analysis")
                    builder.setMessage(comparison)
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
                
                Log.d(TAG, "Analysis complete:\n$comparison")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing normalized data: ${e.message}")
                Toast.makeText(requireContext(), "Lỗi analysis: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testWithPythonStyleProcessor() {
        Log.d(TAG, "Testing with Python-style processor...")
        
        // Show progress UI
        binding.progressCard.visibility = View.VISIBLE
        binding.resultsCard.visibility = View.GONE
        
        // Create sample device_data-like file
        lifecycleScope.launch {
            try {
                // Show loading
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Generating", 0.1f, "Tạo sample device_data..."))
                }
                
                val sampleFile = createDeviceDataStyleFile()
                
                // Show processing
                requireActivity().runOnUiThread {
                    updateProgressUI(DataProcessor.ProcessingProgress("Processing", 0.3f, "Xử lý với Python-style processor..."))
                }
                
                // Process with Python-style processor
                val result = pythonStyleProcessor.processRawPPGData(sampleFile) { progress ->
                    requireActivity().runOnUiThread {
                        updateProgressUI(adaptProgress(progress))
                    }
                }
                
                // Show results
                showProcessingResults(adaptResult(result))
                
                // Clean up sample file
                sampleFile.delete()
                
                if (result.success) {
                    Toast.makeText(requireContext(), "Python-style test thành công! 🐍✨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Python-style test thất bại: ${result.message}", Toast.LENGTH_LONG).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing with Python-style processor: ${e.message}")
                hideProgressUI()
                Toast.makeText(requireContext(), "Lỗi Python-style test: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun createDeviceDataStyleFile(): File {
        // Create realistic device_data format: 2D array with multiple channels
        val duration = 60f // seconds
        val samplingRate = 25f // Hz
        val channels = 4 // 4 channels like real device_data
        val sampleCount = (duration * samplingRate).toInt()
        
        // Generate 2D data [samples x channels] like device_data
        val data2D = Array(sampleCount) { FloatArray(channels) }
        
        for (i in 0 until sampleCount) {
            val time = i / samplingRate
            
            // Simulate PPG signal for channel 0 (Python extracts column 0)
            val heartRate = 75.0 // BPM
            val heartPhase = 2 * kotlin.math.PI * heartRate * time / 60.0
            
            val ppgSignal = -200000f + // Baseline similar to real device_data
                           -50000f * kotlin.math.sin(heartPhase).toFloat() +
                           -10000f * kotlin.math.sin(2 * heartPhase).toFloat() +
                           kotlin.random.Random.nextFloat() * 1000f - 500f
            
            // Fill channels like real device_data
            data2D[i][0] = ppgSignal // Main PPG channel (what Python extracts)
            data2D[i][1] = ppgSignal * 0.9f + kotlin.random.Random.nextFloat() * 1000f
            data2D[i][2] = ppgSignal * 1.1f + kotlin.random.Random.nextFloat() * 1000f
            data2D[i][3] = kotlin.random.Random.nextFloat() * 5000f // Noise channel
        }
        
        // Flatten to 1D array for numpy format (row-major order)
        val flatData = FloatArray(sampleCount * channels)
        for (i in 0 until sampleCount) {
            for (j in 0 until channels) {
                flatData[i * channels + j] = data2D[i][j]
            }
        }
        
        // Create file
        val sampleFile = File(requireContext().cacheDir, "python_style_test_device_data.npy")
        
        // Write as numpy format
        val headerBytes = createSimpleNumpyHeader(flatData.size)
        val dataBytes = ByteArray(flatData.size * 4)
        
        for (i in flatData.indices) {
            val bits = flatData[i].toBits()
            val baseIndex = i * 4
            dataBytes[baseIndex] = (bits and 0xFF).toByte()
            dataBytes[baseIndex + 1] = ((bits shr 8) and 0xFF).toByte()
            dataBytes[baseIndex + 2] = ((bits shr 16) and 0xFF).toByte()
            dataBytes[baseIndex + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        
        sampleFile.writeBytes(headerBytes + dataBytes)
        
        Log.d(TAG, "Created device_data-style file: ${sampleFile.absolutePath}")
        Log.d(TAG, "  Shape: $sampleCount x $channels")
        Log.d(TAG, "  Total elements: ${flatData.size}")
        Log.d(TAG, "  File size: ${sampleFile.length()} bytes")
        
        return sampleFile
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        selectedFile?.delete() // Clean up temp file
        _binding = null
    }
} 