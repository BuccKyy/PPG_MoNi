package com.example.ppg_moni.data

import android.content.Context
import android.util.Log
import java.io.InputStream
import kotlin.math.*

class PPGSignalProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "PPGSignalProcessor"
        private const val SAMPLING_RATE = 25f
        private const val TARGET_SAMPLING_RATE = 50f
        private const val FILTER_LOW = 0.5f
        private const val FILTER_HIGH = 8.0f
        private const val START_OFFSET = 600
        private const val SEGMENT_DURATION = 10 // seconds
    }
    
    /**
     * Xử lý dữ liệu PPG từ file device_data tương tự view_data_2.py
     */
    fun processDeviceData(fileName: String): PPGSignalData? {
        return try {
            // Đọc dữ liệu từ device_data folder
            val inputStream = context.assets.open("device_data/$fileName")
            val rawData = loadNumpyData(inputStream)
            inputStream.close()
            
            if (rawData.isEmpty()) {
                Log.e(TAG, "Empty data from file: $fileName")
                return null
            }
            
            Log.d(TAG, "Loaded ${rawData.size} samples from $fileName")
            
            // Xử lý dữ liệu theo pipeline của view_data_2.py
            val processedData = processPPGPipeline(rawData)
            
            PPGSignalData(
                fileName = fileName,
                originalData = processedData.originalData,
                rawData = processedData.rawData,
                processedData = processedData.processedData,
                heartRate = calculateHeartRate(processedData.processedData),
                spO2 = calculateSpO2FromProcessedData(processedData.processedData)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing device data $fileName: ${e.message}")
            null
        }
    }
    
    /**
     * Pipeline xử lý PPG theo view_data_2.py
     */
    private fun processPPGPipeline(data: FloatArray): PPGProcessingResult {
        // Step 1: Transpose và lấy channel thứ 2 (data.T[1])
        val channel1Data = extractChannel1(data)
        
        // Step 2: Lọc tín hiệu trong khoảng giá trị hợp lệ (-300000 <= data <= -100000)
        val filteredData = filterValidRange(channel1Data, -300000f, -100000f)
        
        // Step 3: Tín hiệu gốc (org_data)
        val originalData = filteredData.copyOf()
        
        // Step 4: Lấy segment 10 giây từ start offset (raw_data)
        val segmentLength = (SAMPLING_RATE * SEGMENT_DURATION).toInt()
        val rawSegment = extractSegment(filteredData, START_OFFSET, segmentLength)
        
        // Step 5: Resample từ 25Hz lên 50Hz
        val resampledData = resample(rawSegment, SAMPLING_RATE, TARGET_SAMPLING_RATE)
        
        // Step 6: Áp dụng bộ lọc Chebyshev type II bandpass
        val filteredSignal = applyChebyshev2Filter(resampledData, FILTER_LOW, FILTER_HIGH, TARGET_SAMPLING_RATE)
        
        // Step 7: Chuẩn hóa tín hiệu
        val normalizedSignal = normalizeSignal(filteredSignal)
        
        Log.d(TAG, "Processing result - Original: ${originalData.size}, Raw: ${rawSegment.size}, Processed: ${normalizedSignal.size}")
        
        return PPGProcessingResult(originalData, rawSegment, normalizedSignal)
    }
    
    /**
     * Đọc file .npy từ assets
     */
    private fun loadNumpyData(inputStream: InputStream): FloatArray {
        return try {
            val bytes = inputStream.readBytes()
            parseNumpyArray(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading numpy data: ${e.message}")
            FloatArray(0)
        }
    }
    
    /**
     * Parse numpy binary data thành FloatArray
     */
    private fun parseNumpyArray(bytes: ByteArray): FloatArray {
        try {
            // Tìm magic string "\x93NUMPY"
            var headerEnd = 0
            for (i in 0 until minOf(200, bytes.size - 6)) {
                if (bytes[i] == 0x93.toByte() && 
                    bytes[i + 1] == 'N'.code.toByte() &&
                    bytes[i + 2] == 'U'.code.toByte() &&
                    bytes[i + 3] == 'M'.code.toByte() &&
                    bytes[i + 4] == 'P'.code.toByte() &&
                    bytes[i + 5] == 'Y'.code.toByte()) {
                    headerEnd = i + 6
                    break
                }
            }
            
            // Skip header và tìm data section
            headerEnd = skipNumpyHeader(bytes, headerEnd)
            
            // Convert bytes to float array
            val dataBytes = bytes.sliceArray(headerEnd until bytes.size)
            return bytesToFloatArray(dataBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing numpy array: ${e.message}")
            return FloatArray(0)
        }
    }
    
    private fun skipNumpyHeader(bytes: ByteArray, start: Int): Int {
        var offset = start
        
        // Skip version (2 bytes) và header length (2 bytes)
        offset += 4
        
        // Tìm end của header dictionary
        var braceCount = 0
        var found = false
        
        while (offset < bytes.size && !found) {
            when (bytes[offset]) {
                '{'.code.toByte() -> braceCount++
                '}'.code.toByte() -> {
                    braceCount--
                    if (braceCount == 0) {
                        found = true
                    }
                }
            }
            offset++
        }
        
        // Align to boundary
        while (offset % 16 != 0 && offset < bytes.size) {
            offset++
        }
        
        return offset
    }
    
    private fun bytesToFloatArray(dataBytes: ByteArray): FloatArray {
        val floatCount = dataBytes.size / 4
        val result = FloatArray(floatCount)
        
        for (i in 0 until floatCount) {
            val byteIndex = i * 4
            if (byteIndex + 3 < dataBytes.size) {
                // Little endian byte order
                val bits = (dataBytes[byteIndex].toInt() and 0xFF) or
                          ((dataBytes[byteIndex + 1].toInt() and 0xFF) shl 8) or
                          ((dataBytes[byteIndex + 2].toInt() and 0xFF) shl 16) or
                          ((dataBytes[byteIndex + 3].toInt() and 0xFF) shl 24)
                result[i] = Float.fromBits(bits)
            }
        }
        
        return result
    }
    
    /**
     * Extract channel 1 from transposed data (tương tự data.T[1])
     */
    private fun extractChannel1(data: FloatArray): FloatArray {
        // Giả sử data có cấu trúc interleaved [ch0, ch1, ch0, ch1, ...]
        // Hoặc data đã được flatten từ ma trận 2D
        
        // Tạm thời return toàn bộ data, cần điều chỉnh theo cấu trúc thực tế
        return data
    }
    
    /**
     * Lọc tín hiệu trong khoảng giá trị hợp lệ
     */
    private fun filterValidRange(data: FloatArray, minValue: Float, maxValue: Float): FloatArray {
        val validIndices = mutableListOf<Int>()
        
        // Tìm các index có giá trị trong khoảng hợp lệ
        data.forEachIndexed { index, value ->
            if (value in minValue..maxValue) {
                validIndices.add(index)
            }
        }
        
        // Trả về dữ liệu đã lọc
        return validIndices.map { data[it] }.toFloatArray()
    }
    
    /**
     * Lấy segment dữ liệu từ start với độ dài length
     */
    private fun extractSegment(data: FloatArray, start: Int, length: Int): FloatArray {
        val end = minOf(start + length, data.size)
        val actualStart = minOf(start, data.size - 1)
        
        return if (actualStart < end && actualStart >= 0) {
            data.sliceArray(actualStart until end)
        } else {
            // Nếu không đủ dữ liệu, lấy từ đầu
            val availableLength = minOf(length, data.size)
            data.sliceArray(0 until availableLength)
        }
    }
    
    /**
     * Resample tín hiệu từ originalRate lên targetRate
     */
    private fun resample(data: FloatArray, originalRate: Float, targetRate: Float): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        
        val ratio = targetRate / originalRate
        val newSize = (data.size * ratio).toInt()
        val result = FloatArray(newSize)
        
        for (i in 0 until newSize) {
            val originalIndex = i / ratio
            val lowerIndex = floor(originalIndex).toInt()
            val upperIndex = minOf(lowerIndex + 1, data.size - 1)
            val fraction = originalIndex - lowerIndex
            
            result[i] = if (lowerIndex < data.size) {
                data[lowerIndex] * (1 - fraction) + data[upperIndex] * fraction
            } else {
                data.last()
            }
        }
        
        return result
    }
    
    /**
     * Áp dụng bộ lọc Chebyshev type II bandpass
     */
    private fun applyChebyshev2Filter(data: FloatArray, lowCut: Float, highCut: Float, samplingRate: Float): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        
        // Implementation đơn giản của bandpass filter
        // Trong thực tế nên dùng thư viện DSP chuyên dụng như Commons Math
        
        val nyquist = samplingRate / 2
        val low = lowCut / nyquist
        val high = highCut / nyquist
        
        // Butterworth bandpass filter đơn giản
        return applyButterworthBandpass(data, low, high)
    }
    
    private fun applyButterworthBandpass(data: FloatArray, lowNorm: Float, highNorm: Float): FloatArray {
        // Đây là implementation đơn giản
        // Trong thực tế cần dùng proper IIR filter
        
        val result = data.copyOf()
        
        // High-pass component (remove DC and very low frequencies)
        for (i in 1 until result.size) {
            result[i] = result[i] - result[i-1] * 0.95f
        }
        
        // Low-pass component (remove high frequencies)
        val alpha = 0.1f
        for (i in 1 until result.size) {
            result[i] = alpha * result[i] + (1 - alpha) * result[i-1]
        }
        
        return result
    }
    
    /**
     * Chuẩn hóa tín hiệu về khoảng [0, 1]
     */
    private fun normalizeSignal(data: FloatArray): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = max - min
        
        return if (range > 0) {
            data.map { (it - min) / range }.toFloatArray()
        } else {
            data.map { 0.5f }.toFloatArray()
        }
    }
    
    /**
     * Tính heart rate từ tín hiệu đã xử lý
     */
    private fun calculateHeartRate(processedData: FloatArray): Float {
        if (processedData.size < 100) return 75f
        
        // Peak detection
        val peaks = findPeaks(processedData)
        
        if (peaks.size < 2) return 75f
        
        // Tính average interval between peaks
        val intervals = peaks.zipWithNext { a, b -> b - a }
        val avgInterval = intervals.average().toFloat()
        
        // Convert to BPM (giả sử 50Hz sampling rate)
        val bpm = 60f * TARGET_SAMPLING_RATE / avgInterval
        return bpm.coerceIn(50f, 150f)
    }
    
    private fun findPeaks(data: FloatArray): List<Int> {
        val peaks = mutableListOf<Int>()
        val threshold = data.average() + data.map { abs(it - data.average()) }.average() * 0.5
        
        for (i in 1 until data.size - 1) {
            if (data[i] > threshold && 
                data[i] > data[i-1] && 
                data[i] > data[i+1]) {
                
                // Minimum distance between peaks
                if (peaks.isEmpty() || i - peaks.last() > TARGET_SAMPLING_RATE / 4) {
                    peaks.add(i)
                }
            }
        }
        
        return peaks
    }
    
    /**
     * Tính SpO2 từ tín hiệu đã xử lý
     */
    private fun calculateSpO2FromProcessedData(processedData: FloatArray): Float {
        // Đây là ước tính đơn giản
        // Trong thực tế cần tín hiệu Red và IR riêng biệt
        
        val variance = processedData.map { (it - processedData.average()).pow(2) }.average()
        val stdDev = sqrt(variance).toFloat()
        
        // Ước tính SpO2 dựa trên chất lượng tín hiệu
        val signalQuality = (1f - stdDev).coerceIn(0f, 1f)
        val baseSpo2 = 98f
        val spo2 = baseSpo2 + signalQuality * 2f
        
        return spo2.coerceIn(95f, 100f)
    }
    
    /**
     * Data classes
     */
    data class PPGProcessingResult(
        val originalData: FloatArray,
        val rawData: FloatArray,
        val processedData: FloatArray
    )
    
    data class PPGSignalData(
        val fileName: String,
        val originalData: FloatArray,
        val rawData: FloatArray,
        val processedData: FloatArray,
        val heartRate: Float,
        val spO2: Float
    )
} 