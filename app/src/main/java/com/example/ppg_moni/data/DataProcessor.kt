package com.example.ppg_moni.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.*

class DataProcessor(private val context: Context) {
    companion object {
        private const val TAG = "DataProcessor"
        private const val ORIGINAL_SAMPLING_RATE = 25f
        private const val TARGET_SAMPLING_RATE = 50f
        private const val SEGMENT_DURATION = 10f
        private const val SEGMENT_OVERLAP = 0.5f
        private const val FILTER_LOW = 0.5f
        private const val FILTER_HIGH = 8.0f
        private const val MIN_VALUE_THRESHOLD = -300000f
        private const val MAX_VALUE_THRESHOLD = -100000f
        private const val MIN_SIGNAL_LENGTH = 1000
    }
    data class ProcessingResult(
        val success: Boolean,
        val message: String,
        val outputFiles: List<String> = emptyList(),
        val segmentCount: Int = 0
    )
    data class ProcessingProgress(
        val stage: String,
        val progress: Float,
        val message: String
    )
    suspend fun processRawPPGData(
        inputFile: File,
        onProgress: (ProcessingProgress) -> Unit = {}
    ): ProcessingResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting processing for: ${inputFile.name}")
            onProgress(ProcessingProgress("Analyzing", 0.05f, "Đang phân tích cấu trúc dữ liệu..."))
            val analyzer = DataAnalyzer(context)
            val analysis = analyzer.analyzeNumpyFile(inputFile)
            if (analysis == null) {
                return@withContext ProcessingResult(false, "Không thể phân tích cấu trúc file")
            }
            Log.d(TAG, "File analysis: ${analysis.fileName}")
            Log.d(TAG, "  Total samples: ${analysis.totalSamples}")
            Log.d(TAG, "  Possible channels: ${analysis.possibleChannels}")
            Log.d(TAG, "  Value range: ${analysis.valueRange}")
            Log.d(TAG, "  Data type: ${analysis.dataType}")
            Log.d(TAG, "  Recommended extraction: ${analysis.recommendedExtraction}")
            onProgress(ProcessingProgress("Loading", 0.1f, "Đang đọc dữ liệu raw..."))
            val rawData = loadRawPPGData(inputFile)
            if (rawData.isEmpty()) {
                return@withContext ProcessingResult(false, "Không thể đọc dữ liệu từ file")
            }
            Log.d(TAG, "Loaded ${rawData.size} raw samples")
            onProgress(ProcessingProgress("Preprocessing", 0.2f, "Đang tiền xử lý dữ liệu..."))
            val preprocessedData = preprocessPPGDataWithAnalysis(rawData, analysis)
            if (preprocessedData.isEmpty()) {
                return@withContext ProcessingResult(false, "Dữ liệu không hợp lệ sau tiền xử lý")
            }
            Log.d(TAG, "Preprocessed to ${preprocessedData.size} samples")
            onProgress(ProcessingProgress("Segmenting", 0.4f, "Đang chia thành segments..."))
            val segments = createSegments(preprocessedData)
            if (segments.isEmpty()) {
                return@withContext ProcessingResult(false, "Không thể tạo segments từ dữ liệu")
            }
            Log.d(TAG, "Created ${segments.size} segments")
            onProgress(ProcessingProgress("Normalizing", 0.6f, "Đang chuẩn hóa segments..."))
            val normalizedSegments = segments.mapIndexed { index, segment ->
                onProgress(ProcessingProgress("Normalizing", 0.6f + 0.2f * index / segments.size, 
                    "Đang chuẩn hóa segment ${index + 1}/${segments.size}..."))
                normalizeSegmentToMatch(segment)
            }
            onProgress(ProcessingProgress("Saving", 0.8f, "Đang lưu segments..."))
            val outputFiles = saveNormalizedSegments(inputFile.nameWithoutExtension, normalizedSegments)
            onProgress(ProcessingProgress("Complete", 1.0f, "Hoàn thành xử lý!"))
            Log.d(TAG, "Processing complete. Created ${outputFiles.size} output files")
            ProcessingResult(
                success = true,
                message = "Xử lý thành công! Tạo được ${outputFiles.size} segments",
                outputFiles = outputFiles,
                segmentCount = outputFiles.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data: ${e.message}")
            e.printStackTrace()
            ProcessingResult(false, "Lỗi xử lý: ${e.message}")
        }
    }
    private fun loadRawPPGData(file: File): FloatArray {
        return try {
            val bytes = file.readBytes()
            parseNumpyArray(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading raw data: ${e.message}")
            FloatArray(0)
        }
    }
    private fun parseNumpyArray(bytes: ByteArray): FloatArray {
        try {
            if (bytes.size < 10) {
                Log.e(TAG, "File too small to be valid numpy array: ${bytes.size} bytes")
                return FloatArray(0)
            }
            if (bytes[0] != 0x93.toByte() || bytes[1] != 'N'.code.toByte() || 
                bytes[2] != 'U'.code.toByte() || bytes[3] != 'M'.code.toByte() ||
                bytes[4] != 'P'.code.toByte() || bytes[5] != 'Y'.code.toByte()) {
                Log.w(TAG, "File doesn't have numpy magic bytes, trying to parse anyway...")
            }
            var headerEnd = 10
            for (i in 10 until minOf(1000, bytes.size - 1)) {
                if (bytes[i] == 0x0A.toByte()) {
                    headerEnd = i + 1
                    break
                }
            }
            Log.d(TAG, "Numpy header ends at byte: $headerEnd")
            if (headerEnd >= bytes.size) {
                Log.e(TAG, "Header end beyond file size")
                return FloatArray(0)
            }
            val dataBytes = bytes.sliceArray(headerEnd until bytes.size)
            Log.d(TAG, "Data section size: ${dataBytes.size} bytes")
            val result = when {
                dataBytes.size % 8 == 0 -> parseAsDouble(dataBytes)?.map { it.toFloat() }?.toFloatArray()
                dataBytes.size % 4 == 0 -> parseAsFloat(dataBytes)
                dataBytes.size % 2 == 0 -> parseAsShort(dataBytes)?.map { it.toFloat() }?.toFloatArray()
                else -> parseAsByte(dataBytes)?.map { it.toFloat() }?.toFloatArray()
            }
            if (result != null && result.isNotEmpty()) {
                Log.d(TAG, "Successfully parsed ${result.size} data points")
                val sampleSize = minOf(5, result.size)
                val sampleValues = result.take(sampleSize).joinToString(", ") { "%.2f".format(it) }
                Log.d(TAG, "Sample values: $sampleValues")
                return result
            } else {
                Log.e(TAG, "Failed to parse data as any known type")
                return FloatArray(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing numpy array: ${e.message}")
            e.printStackTrace()
            return FloatArray(0)
        }
    }
    private fun parseAsFloat(dataBytes: ByteArray): FloatArray? {
        try {
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
            if (result.any { it.isNaN() || it.isInfinite() }) {
                Log.w(TAG, "Float parsing produced NaN/Infinite values, trying big-endian...")
                return parseAsFloatBigEndian(dataBytes)
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing as float: ${e.message}")
            return null
        }
    }
    private fun parseAsFloatBigEndian(dataBytes: ByteArray): FloatArray? {
        try {
            val floatCount = dataBytes.size / 4
            val result = FloatArray(floatCount)
            for (i in 0 until floatCount) {
                val byteIndex = i * 4
                if (byteIndex + 3 < dataBytes.size) {
                    val bits = ((dataBytes[byteIndex].toInt() and 0xFF) shl 24) or
                              ((dataBytes[byteIndex + 1].toInt() and 0xFF) shl 16) or
                              ((dataBytes[byteIndex + 2].toInt() and 0xFF) shl 8) or
                              (dataBytes[byteIndex + 3].toInt() and 0xFF)
                    result[i] = Float.fromBits(bits)
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing as big-endian float: ${e.message}")
            return null
        }
    }
    private fun parseAsDouble(dataBytes: ByteArray): DoubleArray? {
        try {
            val doubleCount = dataBytes.size / 8
            val result = DoubleArray(doubleCount)
            for (i in 0 until doubleCount) {
                val byteIndex = i * 8
                if (byteIndex + 7 < dataBytes.size) {
                    var bits = 0L
                    for (j in 0..7) {
                        bits = bits or ((dataBytes[byteIndex + j].toLong() and 0xFF) shl (j * 8))
                    }
                    result[i] = Double.fromBits(bits)
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing as double: ${e.message}")
            return null
        }
    }
    private fun parseAsShort(dataBytes: ByteArray): IntArray? {
        try {
            val shortCount = dataBytes.size / 2
            val result = IntArray(shortCount)
            for (i in 0 until shortCount) {
                val byteIndex = i * 2
                if (byteIndex + 1 < dataBytes.size) {
                    val value = (dataBytes[byteIndex].toInt() and 0xFF) or
                               ((dataBytes[byteIndex + 1].toInt() and 0xFF) shl 8)
                    result[i] = value.toShort().toInt()
                }
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing as short: ${e.message}")
            return null
        }
    }
    private fun parseAsByte(dataBytes: ByteArray): IntArray? {
        try {
            return dataBytes.map { it.toInt() }.toIntArray()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing as byte: ${e.message}")
            return null
        }
    }
    private fun preprocessPPGDataWithAnalysis(rawData: FloatArray, analysis: DataAnalyzer.DataAnalysis): FloatArray {
        try {
            Log.d(TAG, "=== Starting PPG Preprocessing with Analysis ===")
            Log.d(TAG, "Input raw data size: ${rawData.size}")
            if (rawData.isEmpty()) {
                Log.e(TAG, "Raw data is empty")
                return FloatArray(0)
            }
            val inputSample = rawData.take(10).joinToString(", ") { "%.2f".format(it) }
            Log.d(TAG, "Raw data sample: $inputSample")
            Log.d(TAG, "Raw data range: min=${rawData.minOrNull()}, max=${rawData.maxOrNull()}")
            Log.d(TAG, "Step 1: Extracting channel using analysis...")
            val channel1Data = extractChannelBasedOnAnalysis(rawData, analysis)
            Log.d(TAG, "Channel extraction: ${rawData.size} -> ${channel1Data.size} samples")
            if (channel1Data.isEmpty()) {
                Log.e(TAG, "Channel extraction resulted in empty data")
                return FloatArray(0)
            }
            val ch1Sample = channel1Data.take(10).joinToString(", ") { "%.2f".format(it) }
            Log.d(TAG, "Extracted channel sample: $ch1Sample")
            Log.d(TAG, "Extracted channel range: min=${channel1Data.minOrNull()}, max=${channel1Data.maxOrNull()}")
            Log.d(TAG, "Step 2: Smart data validation...")
            val validData = smartDataValidation(channel1Data, analysis)
            Log.d(TAG, "Data validation: ${channel1Data.size} -> ${validData.size} samples")
            if (validData.size < MIN_SIGNAL_LENGTH) {
                Log.w(TAG, "Insufficient valid data after validation: ${validData.size} < $MIN_SIGNAL_LENGTH")
                return FloatArray(0)
            }
            return processValidDataEnhanced(validData, analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error in preprocessing with analysis: ${e.message}")
            e.printStackTrace()
            return FloatArray(0)
        }
    }
    private fun extractChannelBasedOnAnalysis(data: FloatArray, analysis: DataAnalyzer.DataAnalysis): FloatArray {
        return when (analysis.possibleChannels) {
            4 -> {
                Log.d(TAG, "Using 4-channel interleaved extraction")
                val channel1Data = mutableListOf<Float>()
                for (i in 1 until data.size step 4) {
                    channel1Data.add(data[i])
                }
                channel1Data.toFloatArray()
            }
            2 -> {
                Log.d(TAG, "Using 2-channel interleaved extraction")
                val channel1Data = mutableListOf<Float>()
                for (i in 1 until data.size step 2) {
                    channel1Data.add(data[i])
                }
                channel1Data.toFloatArray()
            }
            else -> {
                Log.d(TAG, "Using single channel (no extraction needed)")
                data
            }
        }
    }
    private fun smartDataValidation(data: FloatArray, analysis: DataAnalyzer.DataAnalysis): FloatArray {
        val (min, max) = analysis.valueRange
        return when {
            analysis.dataType.contains("Raw PPG") -> {
                Log.d(TAG, "Applying adaptive filtering for raw PPG data")
                filterValidRangeFlexible(data)
            }
            analysis.dataType.contains("Normalized") -> {
                Log.d(TAG, "Data already normalized, minimal filtering")
                data.filter { it.isFinite() && it >= 0f && it <= 1f }.toFloatArray()
            }
            analysis.dataType.contains("ADC") -> {
                Log.d(TAG, "Processing ADC values")
                data.filter { it.isFinite() && it >= 0f }.toFloatArray()
            }
            else -> {
                Log.d(TAG, "Unknown format, using flexible filtering")
                filterValidRangeFlexible(data)
            }
        }
    }
    private fun processValidDataEnhanced(validData: FloatArray, analysis: DataAnalyzer.DataAnalysis): FloatArray {
        try {
            val validSample = validData.take(10).joinToString(", ") { "%.2f".format(it) }
            Log.d(TAG, "Valid data sample: $validSample")
            Log.d(TAG, "Valid data range: min=${validData.minOrNull()}, max=${validData.maxOrNull()}")
            val (originalRate, targetRate) = determineSamplingRates(validData, analysis)
            Log.d(TAG, "Determined sampling rates: ${originalRate}Hz -> ${targetRate}Hz")
            val resampledData = resampleSignal(validData, originalRate, targetRate)
            Log.d(TAG, "Resampling: ${validData.size} -> ${resampledData.size} samples")
            if (resampledData.isEmpty()) {
                Log.e(TAG, "Resampling resulted in empty data")
                return FloatArray(0)
            }
            Log.d(TAG, "Step 4: Applying enhanced filtering...")
            val filteredData = applyEnhancedFiltering(resampledData, analysis, targetRate)
            Log.d(TAG, "Enhanced filtering: ${resampledData.size} -> ${filteredData.size} samples")
            val finalSample = filteredData.take(10).joinToString(", ") { "%.2f".format(it) }
            Log.d(TAG, "Final processed sample: $finalSample")
            Log.d(TAG, "Final range: min=${filteredData.minOrNull()}, max=${filteredData.maxOrNull()}")
            Log.d(TAG, "=== Enhanced Preprocessing Complete ===")
            return filteredData
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced processing: ${e.message}")
            e.printStackTrace()
            return FloatArray(0)
        }
    }
    private fun determineSamplingRates(data: FloatArray, analysis: DataAnalyzer.DataAnalysis): Pair<Float, Float> {
        val estimatedDuration = when {
            data.size > 50000 -> data.size / 100f
            data.size > 25000 -> data.size / 50f
            data.size > 12500 -> data.size / 25f
            else -> data.size / 10f
        }
        val reasonableDuration = estimatedDuration.coerceIn(10f, 600f)
        val originalRate = data.size / reasonableDuration
        Log.d(TAG, "Estimated duration: ${reasonableDuration}s, original rate: ${originalRate}Hz")
        return Pair(originalRate, TARGET_SAMPLING_RATE)
    }
    private fun applyEnhancedFiltering(data: FloatArray, analysis: DataAnalyzer.DataAnalysis, samplingRate: Float): FloatArray {
        return when {
            analysis.dataType.contains("Raw PPG") -> {
                Log.d(TAG, "Applying full PPG processing pipeline")
                val detrended = removeTrend(data)
                val filtered = applyBandpassFilterImproved(detrended, FILTER_LOW, FILTER_HIGH, samplingRate)
                filtered
            }
            analysis.dataType.contains("Normalized") -> {
                Log.d(TAG, "Minimal filtering for normalized data")
                applyLowPassFilter(data, FILTER_HIGH, samplingRate)
            }
            else -> {
                Log.d(TAG, "Conservative filtering for unknown data type")
                applyBandpassFilterImproved(data, FILTER_LOW, FILTER_HIGH, samplingRate)
            }
        }
    }
    private fun removeTrend(data: FloatArray): FloatArray {
        if (data.size < 2) return data
        val x = (0 until data.size).map { it.toFloat() }
        val y = data.toList()
        val n = data.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it * it }.sum()
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        Log.d(TAG, "Linear trend: slope=$slope, intercept=$intercept")
        return data.mapIndexed { index, value ->
            value - (slope * index + intercept)
        }.toFloatArray()
    }
    private fun normalizeSegmentToMatch(segment: FloatArray): FloatArray {
        if (segment.isEmpty()) return FloatArray(0)
        val min = segment.minOrNull() ?: 0f
        val max = segment.maxOrNull() ?: 1f
        val range = max - min
        return if (range > 0) {
            segment.map { (it - min) / range }.toFloatArray()
        } else {
            segment.map { 0.5f }.toFloatArray()
        }
    }
    private fun filterValidRangeFlexible(data: FloatArray): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 0f
        val mean = data.average().toFloat()
        val range = max - min
        Log.d(TAG, "Data stats: min=$min, max=$max, mean=$mean, range=$range")
        val lowerThreshold = when {
            min < -1000000f -> -1000000f
            min < -100000f -> min * 1.2f
            else -> min - range * 0.1f
        }
        val upperThreshold = when {
            max > 1000000f -> 1000000f
            max > 100000f -> max * 1.2f
            else -> max + range * 0.1f
        }
        Log.d(TAG, "Flexible thresholds: lower=$lowerThreshold, upper=$upperThreshold")
        val filtered = data.filter { value ->
            value.isFinite() && value >= lowerThreshold && value <= upperThreshold
        }.toFloatArray()
        Log.d(TAG, "Flexible filtering: ${data.size} -> ${filtered.size} samples")
        return filtered
    }
    private fun filterValidRangeRelaxed(data: FloatArray): FloatArray {
        val filtered = data.filter { value ->
            value.isFinite() && !value.isNaN() && !value.isInfinite()
        }.toFloatArray()
        Log.d(TAG, "Relaxed filtering (finite values only): ${data.size} -> ${filtered.size} samples")
        return filtered
    }
    private fun applyBandpassFilterImproved(data: FloatArray, lowCut: Float, highCut: Float, samplingRate: Float): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        try {
            Log.d(TAG, "Applying improved bandpass filter: lowCut=$lowCut Hz, highCut=$highCut Hz, fs=$samplingRate Hz")
            val highPassFiltered = applyHighPassFilter(data, lowCut, samplingRate)
            val bandPassFiltered = applyLowPassFilter(highPassFiltered, highCut, samplingRate)
            Log.d(TAG, "Bandpass filter complete: ${data.size} -> ${bandPassFiltered.size} samples")
            return bandPassFiltered
        } catch (e: Exception) {
            Log.e(TAG, "Error in bandpass filtering: ${e.message}")
            return data
        }
    }
    private fun applyHighPassFilter(data: FloatArray, cutoffHz: Float, samplingRate: Float): FloatArray {
        val alpha = 1.0f / (1.0f + (2.0f * kotlin.math.PI.toFloat() * cutoffHz / samplingRate))
        val filtered = FloatArray(data.size)
        if (data.isNotEmpty()) {
            filtered[0] = data[0]
            for (i in 1 until data.size) {
                filtered[i] = alpha * (filtered[i-1] + data[i] - data[i-1])
            }
        }
        return filtered
    }
    private fun applyLowPassFilter(data: FloatArray, cutoffHz: Float, samplingRate: Float): FloatArray {
        val alpha = (2.0f * kotlin.math.PI.toFloat() * cutoffHz / samplingRate) / (1.0f + (2.0f * kotlin.math.PI.toFloat() * cutoffHz / samplingRate))
        val filtered = FloatArray(data.size)
        if (data.isNotEmpty()) {
            filtered[0] = data[0]
            for (i in 1 until data.size) {
                filtered[i] = alpha * data[i] + (1.0f - alpha) * filtered[i-1]
            }
        }
        return filtered
    }
    private fun extractChannel1(data: FloatArray): FloatArray {
        try {
            Log.d(TAG, "Extracting channel 1 from ${data.size} samples")
            val result = when {
                data.size % 4 == 0 && data.size > 1000 -> {
                    Log.d(TAG, "Trying 4-channel interleaved extraction")
                    extract4ChannelInterleaved(data)
                }
                data.size % 2 == 0 && data.size > 500 -> {
                    Log.d(TAG, "Trying 2-channel interleaved extraction")
                    extract2ChannelInterleaved(data)
                }
                data.size > 1000 -> {
                    Log.d(TAG, "Trying matrix format extraction")
                    extractMatrixFormat(data)
                }
                else -> {
                    Log.d(TAG, "Using data as single channel")
                    data
                }
            }
            Log.d(TAG, "Channel 1 extraction: ${data.size} -> ${result.size} samples")
            if (result.isNotEmpty()) {
                val sampleValues = result.take(5).joinToString(", ") { "%.2f".format(it) }
                Log.d(TAG, "Channel 1 sample values: $sampleValues")
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting channel 1: ${e.message}")
            return data
        }
    }
    private fun extract4ChannelInterleaved(data: FloatArray): FloatArray {
        val channel1Data = mutableListOf<Float>()
        for (i in 1 until data.size step 4) {
            channel1Data.add(data[i])
        }
        return channel1Data.toFloatArray()
    }
    private fun extract2ChannelInterleaved(data: FloatArray): FloatArray {
        val channel1Data = mutableListOf<Float>()
        for (i in 1 until data.size step 2) {
            channel1Data.add(data[i])
        }
        return channel1Data.toFloatArray()
    }
    private fun extractMatrixFormat(data: FloatArray): FloatArray {
        val halfSize = data.size / 2
        return data.sliceArray(halfSize until data.size)
    }
    private fun resampleSignal(data: FloatArray, originalRate: Float, targetRate: Float): FloatArray {
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
    private fun createSegments(data: FloatArray): List<FloatArray> {
        val segmentSamples = (SEGMENT_DURATION * TARGET_SAMPLING_RATE).toInt()
        val stepSize = (segmentSamples * (1 - SEGMENT_OVERLAP)).toInt()
        val segments = mutableListOf<FloatArray>()
        var startIndex = 0
        while (startIndex + segmentSamples <= data.size) {
            val segment = data.sliceArray(startIndex until startIndex + segmentSamples)
            segments.add(segment)
            startIndex += stepSize
        }
        Log.d(TAG, "Created ${segments.size} segments of ${segmentSamples} samples each")
        return segments
    }
    private fun saveNormalizedSegments(baseName: String, segments: List<FloatArray>): List<String> {
        val outputFiles = mutableListOf<String>()
        try {
            val normalizedDir = File(context.filesDir, "normalized_data")
            if (!normalizedDir.exists()) {
                normalizedDir.mkdirs()
            }
            segments.forEachIndexed { index, segment ->
                val fileName = "${baseName}_seg${index}.npy"
                val outputFile = File(normalizedDir, fileName)
                val bytes = saveAsNumpyBytes(segment)
                outputFile.writeBytes(bytes)
                outputFiles.add(fileName)
                Log.d(TAG, "Saved segment $index to $fileName (${segment.size} samples)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving segments: ${e.message}")
        }
        return outputFiles
    }
    private fun saveAsNumpyBytes(data: FloatArray): ByteArray {
        val headerBytes = createSimpleNumpyHeader(data.size)
        val dataBytes = ByteArray(data.size * 4)
        for (i in data.indices) {
            val bits = data[i].toBits()
            val baseIndex = i * 4
            dataBytes[baseIndex] = (bits and 0xFF).toByte()
            dataBytes[baseIndex + 1] = ((bits shr 8) and 0xFF).toByte()
            dataBytes[baseIndex + 2] = ((bits shr 16) and 0xFF).toByte()
            dataBytes[baseIndex + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return headerBytes + dataBytes
    }
    private fun createSimpleNumpyHeader(dataSize: Int): ByteArray {
        val header = "\u0093NUMPY\u0001\u0000{'descr': '<f4', 'fortran_order': False, 'shape': ($dataSize,), }\n"
        return header.toByteArray()
    }
    fun getProcessedDataFiles(): List<String> {
        return try {
            val normalizedDir = File(context.filesDir, "normalized_data")
            if (normalizedDir.exists()) {
                normalizedDir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing processed files: ${e.message}")
            emptyList()
        }
    }
    fun deleteProcessedData(fileName: String): Boolean {
        return try {
            val file = File(File(context.filesDir, "normalized_data"), fileName)
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file $fileName: ${e.message}")
            false
        }
    }
} 