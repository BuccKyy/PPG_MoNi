package com.example.ppg_moni.data

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.*

class DataAnalyzer(private val context: Context) {
    companion object {
        private const val TAG = "DataAnalyzer"
    }
    data class DataAnalysis(
        val fileName: String,
        val totalSamples: Int,
        val shape: List<Int>,
        val dataType: String,
        val valueRange: Pair<Float, Float>,
        val samplesPreview: List<Float>,
        val possibleChannels: Int,
        val recommendedExtraction: String
    )
    fun analyzeNumpyFile(file: File): DataAnalysis? {
        return try {
            Log.d(TAG, "=== Analyzing ${file.name} ===")
            val bytes = file.readBytes()
            Log.d(TAG, "File size: ${bytes.size} bytes")
            val headerInfo = parseNumpyHeader(bytes)
            Log.d(TAG, "Header info: $headerInfo")
            val data = parseNumpyData(bytes)
            Log.d(TAG, "Parsed ${data.size} data points")
            if (data.isEmpty()) {
                Log.e(TAG, "No data found in file")
                return null
            }
            val analysis = analyzeDataStructure(file.name, data)
            Log.d(TAG, "Analysis complete for ${file.name}")
            Log.d(TAG, "Total samples: ${analysis.totalSamples}")
            Log.d(TAG, "Value range: ${analysis.valueRange}")
            Log.d(TAG, "Possible channels: ${analysis.possibleChannels}")
            Log.d(TAG, "Recommended extraction: ${analysis.recommendedExtraction}")
            return analysis
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing file ${file.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    private fun parseNumpyHeader(bytes: ByteArray): Map<String, String> {
        val headerInfo = mutableMapOf<String, String>()
        try {
            if (bytes.size >= 6) {
                val magic = bytes.sliceArray(0..5).map { it.toInt().toChar() }.joinToString("")
                headerInfo["magic"] = magic
            }
            var headerEnd = 10
            for (i in 10 until minOf(1000, bytes.size - 1)) {
                if (bytes[i] == 0x0A.toByte()) {
                    headerEnd = i + 1
                    break
                }
            }
            headerInfo["header_end"] = headerEnd.toString()
            headerInfo["data_size"] = (bytes.size - headerEnd).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing header: ${e.message}")
        }
        return headerInfo
    }
    private fun parseNumpyData(bytes: ByteArray): FloatArray {
        try {
            var headerEnd = 10
            for (i in 10 until minOf(1000, bytes.size - 1)) {
                if (bytes[i] == 0x0A.toByte()) {
                    headerEnd = i + 1
                    break
                }
            }
            val dataBytes = bytes.sliceArray(headerEnd until bytes.size)
            return when {
                dataBytes.size % 8 == 0 -> parseAsDouble(dataBytes)?.map { it.toFloat() }?.toFloatArray()
                dataBytes.size % 4 == 0 -> parseAsFloat(dataBytes)
                dataBytes.size % 2 == 0 -> parseAsShort(dataBytes)?.map { it.toFloat() }?.toFloatArray()
                else -> parseAsInt8(dataBytes)?.map { it.toFloat() }?.toFloatArray()
            } ?: FloatArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data: ${e.message}")
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
            return result
        } catch (e: Exception) {
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
            return null
        }
    }
    private fun parseAsInt8(dataBytes: ByteArray): IntArray? {
        return try {
            dataBytes.map { it.toInt() }.toIntArray()
        } catch (e: Exception) {
            null
        }
    }
    private fun analyzeDataStructure(fileName: String, data: FloatArray): DataAnalysis {
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 0f
        val mean = data.average().toFloat()
        val possibleChannels = when {
            data.size % 4 == 0 && data.size > 4000 -> 4
            data.size % 2 == 0 && data.size > 2000 -> 2
            else -> 1
        }
        val recommendedExtraction = when {
            possibleChannels == 4 -> "4-channel interleaved (extract every 4th starting from index 1)"
            possibleChannels == 2 -> "2-channel interleaved (extract every 2nd starting from index 1)"
            else -> "Single channel (use as-is)"
        }
        val estimatedSamplingRate = when {
            data.size > 100000 -> 100f
            data.size > 50000 -> 50f
            data.size > 25000 -> 25f
            else -> 25f
        }
        val estimatedDuration = data.size / (estimatedSamplingRate * possibleChannels)
        val shape = if (possibleChannels > 1) {
            listOf(data.size / possibleChannels, possibleChannels)
        } else {
            listOf(data.size)
        }
        Log.d(TAG, "Structure analysis:")
        Log.d(TAG, "  Possible channels: $possibleChannels")
        Log.d(TAG, "  Estimated sampling rate: $estimatedSamplingRate Hz")
        Log.d(TAG, "  Estimated duration: $estimatedDuration seconds")
        Log.d(TAG, "  Data range: $min to $max (mean: $mean)")
        return DataAnalysis(
            fileName = fileName,
            totalSamples = data.size,
            shape = shape,
            dataType = detectDataType(data),
            valueRange = Pair(min, max),
            samplesPreview = data.take(10),
            possibleChannels = possibleChannels,
            recommendedExtraction = recommendedExtraction
        )
    }
    private fun detectDataType(data: FloatArray): String {
        val hasNegativeValues = data.any { it < 0 }
        val hasLargeValues = data.any { abs(it) > 10000 }
        val hasSmallValues = data.any { abs(it) < 1 }
        return when {
            hasNegativeValues && hasLargeValues -> "Raw PPG signal (with DC offset)"
            hasSmallValues && !hasLargeValues -> "Normalized PPG signal [0,1]"
            hasLargeValues && !hasNegativeValues -> "ADC values (unsigned)"
            else -> "Unknown format"
        }
    }
    fun compareWithNormalizedData(): String {
        val comparison = StringBuilder()
        try {
            val assetManager = context.assets
            val normalizedFiles = assetManager.list("normalized_data") ?: emptyArray()
            if (normalizedFiles.isNotEmpty()) {
                val sampleFile = normalizedFiles.first()
                val inputStream = assetManager.open("normalized_data/$sampleFile")
                val normalizedBytes = inputStream.readBytes()
                inputStream.close()
                val normalizedData = parseNumpyData(normalizedBytes)
                comparison.append("=== NORMALIZED DATA STRUCTURE ===\n")
                comparison.append("Sample file: $sampleFile\n")
                comparison.append("Size: ${normalizedData.size} samples\n")
                comparison.append("Range: ${normalizedData.minOrNull()} to ${normalizedData.maxOrNull()}\n")
                comparison.append("Sample values: ${normalizedData.take(5).joinToString(", ")}\n")
                comparison.append("Typical segment size: ${normalizedData.size} (should be ~500 for 10sec at 50Hz)\n\n")
                comparison.append("=== TARGET FORMAT ===\n")
                comparison.append("• Each segment: 500 samples (10 seconds at 50Hz)\n")
                comparison.append("• Value range: [0, 1] (normalized)\n")
                comparison.append("• Single channel PPG signal\n")
                comparison.append("• No DC offset, filtered and cleaned\n")
            }
        } catch (e: Exception) {
            comparison.append("Error analyzing normalized data: ${e.message}\n")
        }
        return comparison.toString()
    }
} 