package com.example.ppg_moni.data

import android.content.Context
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

class PPGDataProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "PPGDataProcessor"
    }
    
    // Simulate enhanced processing với normalized data thực
    fun processNormalizedData(): List<PatientData> {
        val patientDataList = mutableListOf<PatientData>()
        
        try {
            val assetManager = context.assets
            val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
            
            Log.d(TAG, "Found ${normalizedFiles.size} normalized data files")
            
            // Group files by session (same timestamp prefix)
            val sessionGroups = normalizedFiles
                .filter { it.endsWith(".npy") }
                .groupBy { filename ->
                    // Extract timestamp part: maxim_241204_232323_seg0.npy -> maxim_241204_232323
                    val parts = filename.split("_")
                    if (parts.size >= 3) "${parts[0]}_${parts[1]}_${parts[2]}" else filename
                }
            
            Log.d(TAG, "Grouped into ${sessionGroups.size} sessions")
            
            sessionGroups.entries.take(10).forEach { (sessionKey, files) ->
                try {
                    Log.d(TAG, "Processing session: $sessionKey with ${files.size} segments")
                    
                    // Load and combine all segments for this session
                    val combinedData = loadAndCombineSegments(files)
                    if (combinedData.isNotEmpty()) {
                        
                        // Enhanced analysis với realistic variations
                        val patientData = analyzePatientData(sessionKey, combinedData)
                        patientDataList.add(patientData)
                        
                        Log.d(TAG, "Created patient data: ${patientData.readableTimestamp}, BP: ${patientData.systolic}/${patientData.diastolic}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing session $sessionKey: ${e.message}")
                }
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Error accessing normalized_data: ${e.message}")
        }
        
        return patientDataList.sortedByDescending { it.filename }
    }
    
    // Process specific session data được user chọn
    fun processSpecificData(sessionKey: String): List<PatientData> {
        val patientDataList = mutableListOf<PatientData>()
        
        try {
            val assetManager = context.assets
            val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
            
            // Find files for specific session
            val sessionFiles = normalizedFiles
                .filter { it.endsWith(".npy") && it.startsWith(sessionKey) }
            
            Log.d(TAG, "Processing specific session: $sessionKey with ${sessionFiles.size} segments")
            
            if (sessionFiles.isNotEmpty()) {
                // Load and combine all segments for this session
                val combinedData = loadAndCombineSegments(sessionFiles)
                if (combinedData.isNotEmpty()) {
                    
                    // Enhanced analysis với realistic variations
                    val patientData = analyzePatientData(sessionKey, combinedData)
                    patientDataList.add(patientData)
                    
                    Log.d(TAG, "Created patient data for $sessionKey: BP: ${patientData.systolic}/${patientData.diastolic}")
                }
            } else {
                Log.w(TAG, "No files found for session: $sessionKey")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing specific data $sessionKey: ${e.message}")
        }
        
        return patientDataList
    }
    
    private fun loadAndCombineSegments(files: List<String>): FloatArray {
        val allData = mutableListOf<Float>()
        
        files.sortedBy { 
            // Sort by segment number: seg0, seg1, seg2...
            val segPart = it.substringAfterLast("_").substringBefore(".")
            segPart.removePrefix("seg").toIntOrNull() ?: 0
        }.forEach { filename ->
            try {
                val inputStream = context.assets.open("normalized_data/$filename")
                val data = loadNpyData(inputStream)
                allData.addAll(data)
                inputStream.close()
                
                Log.d(TAG, "Loaded segment $filename: ${data.size} samples")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading $filename: ${e.message}")
            }
        }
        
        return allData.toFloatArray()
    }
    
    private fun loadNpyData(inputStream: java.io.InputStream): List<Float> {
        try {
            val bytes = inputStream.readBytes()
            
            // Simple NPY parser (assumes float32 data)
            // Skip NPY header (usually around 80-128 bytes)
            val headerEnd = findHeaderEnd(bytes)
            val dataBytes = bytes.sliceArray(headerEnd until bytes.size)
            
            // Convert bytes to floats
            val buffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
            val floats = mutableListOf<Float>()
            
            while (buffer.remaining() >= 4) {
                floats.add(buffer.float)
            }
            
            return floats
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NPY data: ${e.message}")
            return emptyList()
        }
    }
    
    private fun findHeaderEnd(bytes: ByteArray): Int {
        // Look for end of NPY header (find newline after header info)
        for (i in 10 until minOf(200, bytes.size - 1)) {
            if (bytes[i] == 0x0A.toByte()) { // newline
                return i + 1
            }
        }
        return 128 // fallback header size
    }
    
    private fun analyzePatientData(sessionKey: String, ppgData: FloatArray): PatientData {
        // Enhanced analysis algorithms
        val timestamp = System.currentTimeMillis().toString()
        
        // Simulate Red and IR data (realistic separation)
        val mid = ppgData.size / 2
        val redData = ppgData.sliceArray(0 until minOf(mid, ppgData.size))
        val irData = if (mid < ppgData.size) {
            ppgData.sliceArray(mid until ppgData.size)
        } else {
            // If not enough data, simulate IR from red with some variation
            redData.map { it * 0.8f + Random.nextFloat() * 0.2f }.toFloatArray()
        }
        
        // Advanced PPG analysis
        val heartRate = calculateHeartRate(redData)
        val waveRatio = calculateWaveRatio(redData, irData)
        val oxygenSaturation = calculateSpO2(redData, irData)
        val pulseWaveVelocity = calculatePWV(redData)
        
        // Enhanced blood pressure prediction với realistic values
        val (systolic, diastolic) = predictBloodPressure(
            heartRate, waveRatio, oxygenSaturation, pulseWaveVelocity, ppgData
        )
        
        // Confidence based on data quality
        val confidence = calculateConfidence(redData, irData, waveRatio)
        
        return PatientData(
            filename = sessionKey,
            timestamp = timestamp,
            redData = redData,
            irData = irData,
            systolic = systolic,
            diastolic = diastolic,
            heartRate = heartRate,
            oxygenSaturation = oxygenSaturation,
            confidence = confidence,
            waveRatio = waveRatio,
            pulseWaveVelocity = pulseWaveVelocity
        )
    }
    
    private fun calculateHeartRate(redData: FloatArray): Float {
        if (redData.size < 100) return 75f
        
        // Peak detection algorithm
        val peaks = mutableListOf<Int>()
        val threshold = redData.average() + redData.standardDeviation() * 0.5
        
        for (i in 1 until redData.size - 1) {
            if (redData[i] > threshold && 
                redData[i] > redData[i-1] && 
                redData[i] > redData[i+1]) {
                peaks.add(i)
            }
        }
        
        if (peaks.size < 2) return 75f
        
        // Calculate average interval between peaks
        val intervals = peaks.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average()
        
        // Convert to BPM (assuming 100Hz sampling rate)
        val bpm = 60.0 * 100.0 / avgInterval
        return bpm.toFloat().coerceIn(50f, 150f)
    }
    
    private fun calculateWaveRatio(redData: FloatArray, irData: FloatArray): Float {
        if (redData.isEmpty() || irData.isEmpty()) return 0.6f
        
        val redAC = redData.standardDeviation()
        val redDC = redData.average().toFloat()
        val irAC = irData.standardDeviation()
        val irDC = irData.average().toFloat()
        
        val redRatio = if (redDC != 0f) redAC / redDC else 0f
        val irRatio = if (irDC != 0f) irAC / irDC else 0f
        
        val ratio = if (irRatio != 0f) redRatio / irRatio else 0.6f
        return ratio.coerceIn(0.3f, 1.2f)
    }
    
    private fun calculateSpO2(redData: FloatArray, irData: FloatArray): Float {
        val ratio = calculateWaveRatio(redData, irData)
        // SpO2 calculation based on ratio (simplified algorithm)
        val spo2 = 100f - (ratio * 25f)
        return spo2.coerceIn(85f, 100f)
    }
    
    private fun calculatePWV(data: FloatArray): Float {
        // Simplified pulse wave velocity calculation
        val variance = data.standardDeviation()
        val mean = data.average().toFloat()
        val pwv = (variance / mean) * 10f + Random.nextFloat() * 2f
        return pwv.coerceIn(5f, 15f)
    }
    
    private fun predictBloodPressure(
        heartRate: Float, 
        waveRatio: Float, 
        spo2: Float, 
        pwv: Float,
        rawData: FloatArray
    ): Pair<Float, Float> {
        
        // Enhanced prediction algorithm với multiple factors
        var systolic = 120f
        var diastolic = 80f
        
        // Heart rate influence
        when {
            heartRate < 60 -> {
                systolic -= 5f
                diastolic -= 3f
            }
            heartRate > 100 -> {
                systolic += 10f
                diastolic += 5f
            }
        }
        
        // Wave ratio influence (arterial stiffness indicator)
        when {
            waveRatio < 0.5 -> {
                systolic -= 8f
                diastolic -= 4f
            }
            waveRatio > 0.8 -> {
                systolic += 12f
                diastolic += 7f
            }
        }
        
        // Pulse wave velocity influence
        when {
            pwv > 10f -> {
                systolic += 15f
                diastolic += 8f
            }
            pwv < 7f -> {
                systolic -= 5f
                diastolic -= 2f
            }
        }
        
        // Data quality adjustment
        val dataVariance = rawData.standardDeviation()
        val qualityFactor = (dataVariance / rawData.average().toFloat()).coerceIn(0.1f, 0.5f)
        systolic += qualityFactor * 10f
        diastolic += qualityFactor * 5f
        
        // Add some realistic random variation
        systolic += Random.nextFloat() * 10f - 5f
        diastolic += Random.nextFloat() * 8f - 4f
        
        return Pair(
            systolic.coerceIn(90f, 180f),
            diastolic.coerceIn(60f, 120f)
        )
    }
    
    private fun calculateConfidence(redData: FloatArray, irData: FloatArray, waveRatio: Float): Float {
        if (redData.isEmpty()) return 0.5f
        
        var confidence = 0.8f
        
        // Data quality factors
        val signalToNoise = redData.average() / (redData.standardDeviation() + 0.001f)
        if (signalToNoise < 5f) confidence -= 0.2f
        if (signalToNoise > 15f) confidence += 0.1f
        
        // Wave ratio quality
        if (waveRatio in 0.4f..0.9f) confidence += 0.1f
        
        // Data length
        if (redData.size > 500) confidence += 0.05f
        
        return confidence.coerceIn(0.3f, 0.95f)
    }
    
    private fun FloatArray.standardDeviation(): Float {
        if (isEmpty()) return 0f
        val mean = average()
        val variance = map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance).toFloat()
    }
} 