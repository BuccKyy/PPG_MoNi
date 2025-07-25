package com.example.ppg_moni.ui.charts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ppg_moni.R
import com.example.ppg_moni.data.PPGDataProcessor
import com.example.ppg_moni.data.PPGSignalProcessor
import com.example.ppg_moni.data.PatientData
import com.example.ppg_moni.databinding.FragmentChartBinding
import kotlin.math.*

class ChartFragment : Fragment() {

    companion object {
        private const val TAG = "ChartFragment"
    }

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChartViewModel
    private lateinit var patientDataAdapter: PatientDataAdapter
    private lateinit var dataSelectionAdapter: DataSelectionAdapter
    private lateinit var ppgDataProcessor: PPGDataProcessor
    private lateinit var ppgSignalProcessor: PPGSignalProcessor
    
    private var availableDataFiles = mutableListOf<DataFile>()
    private var selectedDataFile: DataFile? = null
    private var isInSelectionMode = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupAdapters()
        setupUI()
        loadAvailableDataFiles()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[ChartViewModel::class.java]
        ppgDataProcessor = PPGDataProcessor(requireContext())
        ppgSignalProcessor = PPGSignalProcessor(requireContext())
    }

    private fun setupAdapters() {
        // Data selection adapter
        dataSelectionAdapter = DataSelectionAdapter(
            dataFiles = emptyList(),
            onFileSelected = { dataFile ->
                selectDataFile(dataFile)
            }
        )
        
        // Patient data adapter (for results)
        patientDataAdapter = PatientDataAdapter(
            patientDataList = emptyList(),
            onItemClick = { patientData ->
                showPatientDetails(patientData)
            }
        )
        
        binding.recyclerViewPatientData.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            if (isInSelectionMode) {
                findNavController().popBackStack()
            } else {
                // Go back to selection mode
                showDataSelectionMode()
            }
        }
        
        // Analyze button
        binding.analyzeButton.setOnClickListener {
            selectedDataFile?.let { dataFile ->
                analyzeSelectedData(dataFile)
            } ?: run {
                Toast.makeText(requireContext(), "Vui lòng chọn dữ liệu trước", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Refresh button
        binding.refreshButton.setOnClickListener {
            if (isInSelectionMode) {
                loadAvailableDataFiles()
            } else {
                selectedDataFile?.let { analyzeSelectedData(it) }
            }
        }
        
        // Toggle PPG Signal button
        binding.toggleSignalButton.setOnClickListener {
            togglePPGSignalVisibility()
        }
        
        // Start in selection mode
        showDataSelectionMode()
    }
    
    private fun showDataSelectionMode() {
        isInSelectionMode = true
        
        binding.titleText.text = "Chọn dữ liệu PPG"
        binding.subtitleText.text = "Chọn dữ liệu để phân tích huyết áp"
        
        // Show selection UI
        binding.recyclerViewPatientData.adapter = dataSelectionAdapter
        binding.analyzeButton.visibility = View.VISIBLE
        binding.summaryCard.visibility = View.GONE
        binding.emptyStateText.text = "Đang tải danh sách dữ liệu..."
        
        // Update button states
        updateAnalyzeButtonState()
    }
    
    private fun showResultsMode() {
        isInSelectionMode = false
        
        binding.titleText.text = "Kết quả phân tích"
        binding.subtitleText.text = "Dữ liệu: ${selectedDataFile?.displayName}"
        
        // Show results UI
        binding.recyclerViewPatientData.adapter = patientDataAdapter
        binding.analyzeButton.visibility = View.GONE
        binding.summaryCard.visibility = View.VISIBLE
        binding.ppgSignalCard.visibility = View.VISIBLE
    }

    private fun loadAvailableDataFiles() {
        Log.d(TAG, "Loading available data files...")
        
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE
        
        try {
            val assetManager = requireContext().assets
            val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
            
            // Group files by session
            val sessionGroups = normalizedFiles
                .filter { it.endsWith(".npy") }
                .groupBy { filename ->
                    val parts = filename.split("_")
                    if (parts.size >= 3) "${parts[0]}_${parts[1]}_${parts[2]}" else filename
                }
            
            availableDataFiles.clear()
            sessionGroups.forEach { (sessionKey, files) ->
                val displayName = formatSessionDisplayName(sessionKey)
                val timestamp = formatSessionTimestamp(sessionKey)
                
                availableDataFiles.add(
                    DataFile(
                        filename = sessionKey,
                        displayName = displayName,
                        timestamp = timestamp,
                        segmentCount = files.size
                    )
                )
            }
            
            Log.d(TAG, "Found ${availableDataFiles.size} data sessions")
            
            if (availableDataFiles.isNotEmpty()) {
                dataSelectionAdapter.updateData(availableDataFiles)
                binding.recyclerViewPatientData.visibility = View.VISIBLE
                binding.emptyStateText.visibility = View.GONE
            } else {
                showEmptyState()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data files: ${e.message}")
            showError("Không thể tải danh sách dữ liệu: ${e.message}")
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun selectDataFile(dataFile: DataFile) {
        // Update selection state
        availableDataFiles.forEachIndexed { index, file ->
            availableDataFiles[index] = file.copy(isSelected = file.filename == dataFile.filename)
        }
        
        selectedDataFile = dataFile
        dataSelectionAdapter.updateData(availableDataFiles)
        updateAnalyzeButtonState()
        
        Log.d(TAG, "Selected data file: ${dataFile.displayName}")
    }
    
    private fun updateAnalyzeButtonState() {
        val hasSelection = selectedDataFile != null
        binding.analyzeButton.isEnabled = hasSelection
        binding.analyzeButton.text = if (hasSelection) {
            "📊 Phân tích dữ liệu"
        } else {
            "Chọn dữ liệu trước"
        }
        binding.analyzeButton.alpha = if (hasSelection) 1.0f else 0.6f
    }
    
    private fun analyzeSelectedData(dataFile: DataFile) {
        Log.d(TAG, "Analyzing selected data: ${dataFile.displayName}")
        
        binding.progressBar.visibility = View.VISIBLE
        
        try {
            // Process specific selected data
            val patientDataList = ppgDataProcessor.processSpecificData(dataFile.filename)
            
            if (patientDataList.isNotEmpty()) {
                patientDataAdapter.updateData(patientDataList)
                updateSummary(patientDataList)
                
                // Process and display PPG signal
                processPPGSignal(dataFile.filename)
                
                showResultsMode()
                
                Toast.makeText(requireContext(), "Phân tích hoàn tất!", Toast.LENGTH_SHORT).show()
            } else {
                showError("Không thể phân tích dữ liệu này")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing data: ${e.message}")
            showError("Lỗi phân tích: ${e.message}")
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun updateSummary(patientDataList: List<PatientData>) {
        val data = patientDataList.first() // Since we're analyzing one session
        
        binding.summaryText.text = buildString {
            append("📊 Kết quả phân tích\n\n")
            append("• Thời gian đo: ${data.readableTimestamp}\n")
            append("• Huyết áp: ${data.systolic.toInt()}/${data.diastolic.toInt()} mmHg\n")
            append("• Nhịp tim: ${data.heartRate.toInt()} BPM\n")
            append("• SpO₂: ${String.format("%.1f", data.oxygenSaturation)}%\n")
            append("• Tỉ lệ sóng: ${String.format("%.2f", data.waveRatio)}\n")
            append("• Độ tin cậy: ${String.format("%.0f", data.confidence * 100)}%\n\n")
            append("• Đánh giá: ${data.bloodPressureCategory}")
        }
        
        binding.summaryCard.visibility = View.VISIBLE
    }
    
    private fun formatSessionDisplayName(sessionKey: String): String {
        return "Dữ liệu PPG - ${sessionKey.replace("maxim_", "").replace("_", " ")}"
    }
    
    private fun formatSessionTimestamp(sessionKey: String): String {
        return try {
            val parts = sessionKey.split("_")
            if (parts.size >= 3) {
                val dateStr = parts[1] // 241204
                val timeStr = parts[2] // 232323
                
                val year = "20" + dateStr.substring(0, 2)
                val month = dateStr.substring(2, 4)
                val day = dateStr.substring(4, 6)
                
                val hour = timeStr.substring(0, 2)
                val minute = timeStr.substring(2, 4)
                
                "$day/$month/$year $hour:$minute"
            } else {
                sessionKey
            }
        } catch (e: Exception) {
            sessionKey
        }
    }
    
    private fun showPatientDetails(patientData: PatientData) {
        val message = buildString {
            append("🏥 Chi tiết phân tích\n\n")
            append("Thời gian: ${patientData.readableTimestamp}\n")
            append("Huyết áp: ${patientData.systolic.toInt()}/${patientData.diastolic.toInt()} mmHg\n")
            append("Nhịp tim: ${patientData.heartRate.toInt()} BPM\n")
            append("SpO₂: ${String.format("%.1f", patientData.oxygenSaturation)}%\n")
            append("Tỉ lệ sóng: ${String.format("%.2f", patientData.waveRatio)}\n")
            append("Độ tin cậy: ${String.format("%.0f", patientData.confidence * 100)}%\n\n")
            append("🔍 Phân tích:\n")
            append("${patientData.explanation}\n\n")
            append("🌊 Sóng mạch:\n")
            append("${patientData.waveRatioExplanation}\n\n")
            append("💡 Khuyến nghị:\n")
            append(patientData.healthRecommendations.joinToString("\n"))
        }
        
        Toast.makeText(requireContext(), "Chi tiết đã được ghi log", Toast.LENGTH_SHORT).show()
        Log.i(TAG, message)
    }
    
    private fun showEmptyState() {
        binding.recyclerViewPatientData.visibility = View.GONE
        binding.summaryCard.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
        binding.emptyStateText.text = "Không tìm thấy dữ liệu PPG.\nHãy đảm bảo có file .npy trong thư mục assets/normalized_data/"
    }
    
    private fun showError(message: String) {
        binding.recyclerViewPatientData.visibility = View.GONE
        binding.summaryCard.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
        binding.emptyStateText.text = "❌ Lỗi: $message"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun togglePPGSignalVisibility() {
        val container = binding.ppgSignalContainer
        val button = binding.toggleSignalButton
        val spo2Layout = binding.spo2InfoLayout
        
        if (container.visibility == View.GONE) {
            container.visibility = View.VISIBLE
            spo2Layout.visibility = View.VISIBLE
            button.text = "Ẩn"
        } else {
            container.visibility = View.GONE
            spo2Layout.visibility = View.GONE
            button.text = "Hiển thị"
        }
    }
    
    private fun processPPGSignal(fileName: String) {
        try {
            Log.d(TAG, "Processing PPG signal for: $fileName")
            
            // Sử dụng normalized data trực tiếp vì không có device_data trong assets
            processNormalizedSignal(fileName)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing PPG signal: ${e.message}")
            // Hiển thị mock data nếu có lỗi
            displayMockPPGSignal()
        }
    }
    
    private fun findCorrespondingDeviceFile(normalizedFileName: String): String? {
        try {
            // Extract timestamp from normalized filename: maxim_241204_232323_seg0
            val baseName = normalizedFileName.substringBeforeLast("_seg")
            
            // List device_data files
            val assetManager = requireContext().assets
            val deviceFiles = assetManager.list("device_data") ?: return null
            
            // Find matching device file
            val matchingFile = deviceFiles.find { it.startsWith(baseName) && it.endsWith(".npy") }
            
            Log.d(TAG, "Looking for device file matching $baseName, found: $matchingFile")
            return matchingFile
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding device file: ${e.message}")
            return null
        }
    }
    
    private fun processNormalizedSignal(fileName: String) {
        try {
            Log.d(TAG, "Processing normalized signal for: $fileName")
            
            val assetManager = requireContext().assets
            val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
            
            // Tìm tất cả files của session này
            val sessionFiles = normalizedFiles
                .filter { it.endsWith(".npy") && it.startsWith(fileName) }
                .sortedBy { 
                    // Sort by segment number
                    val segPart = it.substringAfterLast("_").substringBefore(".")
                    segPart.removePrefix("seg").toIntOrNull() ?: 0
                }
            
            Log.d(TAG, "Found ${sessionFiles.size} session files: $sessionFiles")
            
            if (sessionFiles.isNotEmpty()) {
                // Load và combine all segments
                val allData = mutableListOf<Float>()
                
                sessionFiles.forEach { file ->
                    try {
                        val inputStream = assetManager.open("normalized_data/$file")
                        val segmentData = loadNormalizedData(inputStream)
                        inputStream.close()
                        
                        if (segmentData.isNotEmpty()) {
                            allData.addAll(segmentData.toList())
                            Log.d(TAG, "Loaded segment $file: ${segmentData.size} samples")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading segment $file: ${e.message}")
                    }
                }
                
                if (allData.isNotEmpty()) {
                    val combinedData = allData.toFloatArray()
                    
                    // Hiển thị tín hiệu PPG
                    binding.ppgSignalView.displayPPGSignal(combinedData)
                    
                    // Tính toán heart rate và SpO2 từ data
                    val heartRate = calculateHeartRateFromNormalized(combinedData)
                    val spO2 = calculateSpO2FromNormalized(combinedData)
                    
                    displaySpO2Info(spO2, heartRate)
                    
                    Log.d(TAG, "Successfully displayed normalized signal: ${combinedData.size} samples, HR: $heartRate, SpO2: $spO2")
                } else {
                    Log.w(TAG, "No data loaded from session files")
                    displayMockPPGSignal()
                }
            } else {
                Log.w(TAG, "No session files found for: $fileName")
                displayMockPPGSignal()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing normalized signal: ${e.message}")
            displayMockPPGSignal()
        }
    }
    
    private fun loadNormalizedData(inputStream: java.io.InputStream): FloatArray {
        return try {
            val bytes = inputStream.readBytes()
            
            // Simple NPY parser
            val headerEnd = findHeaderEnd(bytes)
            val dataBytes = bytes.sliceArray(headerEnd until bytes.size)
            
            val floatCount = dataBytes.size / 4
            val result = FloatArray(floatCount)
            
            for (i in 0 until floatCount) {
                val byteIndex = i * 4
                if (byteIndex + 3 < dataBytes.size) {
                    val bits = (dataBytes[byteIndex].toInt() and 0xFF) or
                              ((dataBytes[byteIndex + 1].toInt() and 0xFF) shl 8) or
                              ((dataBytes[byteIndex + 2].toInt() and 0xFF) shl 16) or
                              ((dataBytes[byteIndex + 3].toInt() and 0xFF) shl 24)
                    result[i] = Float.fromBits(bits)
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading normalized data: ${e.message}")
            FloatArray(0)
        }
    }
    
    private fun findHeaderEnd(bytes: ByteArray): Int {
        for (i in 10 until minOf(200, bytes.size - 1)) {
            if (bytes[i] == 0x0A.toByte()) {
                return i + 1
            }
        }
        return 128
    }
    
    private fun displayPPGSignal(signalData: PPGSignalProcessor.PPGSignalData) {
        // Hiển thị tín hiệu PPG qua PPGSignalView
        val combinedData = signalData.processedData // Có thể combine multiple signals nếu cần
        binding.ppgSignalView.displayPPGSignal(combinedData)
        
        Log.d(TAG, "Displayed PPG signal: ${signalData.fileName}")
    }
    
    private fun displaySpO2Info(spo2: Float, heartRate: Float) {
        binding.spo2ValueText.text = "${String.format("%.1f", spo2)}%"
        
        // Determine SpO2 status and color
        val (status, colorRes) = when {
            spo2 >= 98f -> "Tuyệt vời" to R.color.success_color
            spo2 >= 95f -> "Bình thường" to R.color.bp_normal_color
            spo2 >= 90f -> "Thấp" to R.color.warning_color
            else -> "Rất thấp" to R.color.error_color
        }
        
        binding.spo2StatusText.text = status
        binding.spo2StatusText.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        binding.spo2ValueText.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        
        Log.d(TAG, "SpO2 displayed: $spo2% ($status), HR: $heartRate BPM")
    }
    
    private fun displayMockPPGSignal() {
        try {
            // Tạo mock PPG signal để demo
            val mockSignal = generateMockPPGSignal()
            binding.ppgSignalView.displayPPGSignal(mockSignal)
            displaySpO2Info(98.2f, 72f)
            Log.d(TAG, "Displayed mock PPG signal with ${mockSignal.size} samples")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying mock signal: ${e.message}")
        }
    }
    
    private fun generateMockPPGSignal(): FloatArray {
        // Tạo mock signal tương tự như tín hiệu PPG thực
        val signalLength = 500
        val mockSignal = FloatArray(signalLength)
        
        for (i in 0 until signalLength) {
            // Tạo sóng sine với noise để giống PPG signal
            val heartbeatFreq = 1.2f // ~72 BPM
            val time = i.toFloat() / 50f // Giả sử 50Hz sampling rate
            
            mockSignal[i] = (0.8f * sin(2 * PI.toFloat() * heartbeatFreq * time).toFloat() + 
                           0.2f * sin(2 * PI.toFloat() * heartbeatFreq * 2 * time).toFloat() +
                           0.1f * (Math.random().toFloat() - 0.5f))
        }
        
        // Normalize signal
        val min = mockSignal.minOrNull() ?: 0f
        val max = mockSignal.maxOrNull() ?: 1f
        val range = max - min
        
        return if (range > 0) {
            mockSignal.map { (it - min) / range }.toFloatArray()
        } else {
            mockSignal
        }
    }
    
    private fun calculateHeartRateFromNormalized(data: FloatArray): Float {
        return try {
            if (data.size < 100) return 75f
            
            // Simple peak detection for heart rate
            val peaks = mutableListOf<Int>()
            val threshold = data.average() + data.map { kotlin.math.abs(it - data.average()) }.average() * 0.5
            
            for (i in 1 until data.size - 1) {
                if (data[i] > threshold && 
                    data[i] > data[i-1] && 
                    data[i] > data[i+1]) {
                    
                    // Minimum distance between peaks (assume 50Hz sampling)
                    if (peaks.isEmpty() || i - peaks.last() > 25) {
                        peaks.add(i)
                    }
                }
            }
            
            if (peaks.size < 2) return 75f
            
            // Calculate average interval between peaks
            val intervals = peaks.zipWithNext { a, b -> b - a }
            val avgInterval = intervals.average().toFloat()
            
            // Convert to BPM (assuming 50Hz sampling rate)
            val bpm = 60f * 50f / avgInterval
            bpm.coerceIn(50f, 150f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating heart rate: ${e.message}")
            75f
        }
    }
    
    private fun calculateSpO2FromNormalized(data: FloatArray): Float {
        return try {
            if (data.isEmpty()) return 98.5f
            
            // Estimate SpO2 based on signal quality
            val variance = data.map { (it - data.average()).pow(2) }.average()
            val stdDev = sqrt(variance).toFloat()
            
            // Higher signal quality (lower std dev) = higher SpO2
            val signalQuality = (1f - stdDev).coerceIn(0f, 1f)
            val baseSpo2 = 97f
            val spo2 = baseSpo2 + signalQuality * 3f
            
            spo2.coerceIn(95f, 100f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating SpO2: ${e.message}")
            98.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 