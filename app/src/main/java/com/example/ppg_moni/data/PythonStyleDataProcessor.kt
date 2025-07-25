package com.example.ppg_moni.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.*

class PythonStyleDataProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "PythonStyleDataProcessor"
        private const val SEGMENT_SIZE = 1024
        private var globalMinPPG: Float? = null
        private var globalMaxPPG: Float? = null
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
            Log.d(TAG, "=== Starting Python-style Processing ===")
            Log.d(TAG, "Processing file: ${inputFile.name}")
            onProgress(ProcessingProgress("Loading", 0.1f, "Đang đọc dữ liệu raw..."))
            val data2D = loadAs2DArray(inputFile)
            if (data2D == null) {
                return@withContext ProcessingResult(false, "File không đúng định dạng 2D array hoặc thiếu cột PPG")
            }
            Log.d(TAG, "Loaded 2D array: ${data2D.size} rows x ${data2D[0].size} columns")
            onProgress(ProcessingProgress("Extracting", 0.2f, "Đang extract cột PPG (cột 0)..."))
            val ppgColumn = extractColumn0(data2D)
            Log.d(TAG, "Extracted PPG column: ${ppgColumn.size} samples")
            onProgress(ProcessingProgress("Analyzing", 0.3f, "Đang tính toán global min/max..."))
            val (minPPG, maxPPG) = calculateGlobalMinMax(ppgColumn)
            Log.d(TAG, "Global PPG range: min=$minPPG, max=$maxPPG")
            onProgress(ProcessingProgress("Segmenting", 0.4f, "Đang chia thành segments 1024 điểm..."))
            val segments = createPythonStyleSegments(ppgColumn)
            Log.d(TAG, "Created ${segments.size} segments of 1024 points each")
            if (segments.isEmpty()) {
                return@withContext ProcessingResult(false, "Không đủ dữ liệu để tạo segments 1024 điểm")
            }
            onProgress(ProcessingProgress("Normalizing", 0.6f, "Đang normalize với global min/max..."))
            val normalizedSegments = segments.mapIndexed { index, segment ->
                onProgress(ProcessingProgress("Normalizing", 0.6f + 0.2f * index / segments.size, 
                    "Đang normalize segment ${index + 1}/${segments.size}..."))
                normalizeWithGlobalMinMax(segment, minPPG, maxPPG)
            }
            onProgress(ProcessingProgress("Saving", 0.8f, "Đang lưu segments..."))
            val outputFiles = savePythonStyleSegments(inputFile.nameWithoutExtension, normalizedSegments)
            onProgress(ProcessingProgress("Complete", 1.0f, "Hoàn thành xử lý Python-style!"))
            Log.d(TAG, "=== Python-style Processing Complete ===")
            Log.d(TAG, "Created ${outputFiles.size} output files")
            ProcessingResult(
                success = true,
                message = "Xử lý thành công theo Python pipeline! Tạo được ${outputFiles.size} segments",
                outputFiles = outputFiles,
                segmentCount = outputFiles.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in Python-style processing: ${e.message}")
            e.printStackTrace()
            ProcessingResult(false, "Lỗi xử lý Python-style: ${e.message}")
        }
    }
    
    private fun loadAs2DArray(file: File): Array<FloatArray>? {
        return try {
            val bytes = file.readBytes()
            val flatData = parseNumpyArray(bytes)
            if (flatData.isEmpty()) {
                Log.e(TAG, "Failed to parse numpy data")
                return null
            }
            Log.d(TAG, "Parsed ${flatData.size} total elements")
            val possibleShapes = listOf(
                Pair(flatData.size / 4, 4),
                Pair(flatData.size / 2, 2),
                Pair(flatData.size / 1, 1)
            )
            for ((rows, cols) in possibleShapes) {
                if (rows * cols == flatData.size && rows > 100 && cols >= 1) {
                    Log.d(TAG, "Detected 2D shape: $rows x $cols")
                    val data2D = Array(rows) { FloatArray(cols) }
                    for (i in 0 until rows) {
                        for (j in 0 until cols) {
                            data2D[i][j] = flatData[i * cols + j]
                        }
                    }
                    if (cols >= 1) {
                        Log.d(TAG, "2D array validation passed: ndim=2, shape[1]=$cols >= 1")
                        return data2D
                    }
                }
            }
            Log.e(TAG, "Could not create valid 2D array from data")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading as 2D array: ${e.message}")
            return null
        }
    }
    
    private fun extractColumn0(data2D: Array<FloatArray>): FloatArray {
        Log.d(TAG, "Extracting column 0 (Python: data[:, 0])")
        val column0 = FloatArray(data2D.size)
        for (i in data2D.indices) {
            column0[i] = data2D[i][0]
        }
        val sampleValues = column0.take(10).joinToString(", ") { "%.2f".format(it) }
        Log.d(TAG, "Column 0 sample values: $sampleValues")
        Log.d(TAG, "Column 0 range: min=${column0.minOrNull()}, max=${column0.maxOrNull()}")
        return column0
    }
    
    private fun calculateGlobalMinMax(ppgData: FloatArray): Pair<Float, Float> {
        val minPPG = ppgData.minOrNull() ?: 0f
        val maxPPG = ppgData.maxOrNull() ?: 1f
        Log.d(TAG, "Python-style global calculation:")
        Log.d(TAG, "  Min PPG: $minPPG")
        Log.d(TAG, "  Max PPG: $maxPPG")
        Log.d(TAG, "  Range: ${maxPPG - minPPG}")
        globalMinPPG = minPPG
        globalMaxPPG = maxPPG
        return Pair(minPPG, maxPPG)
    }
    
    private fun createPythonStyleSegments(ppgData: FloatArray): List<FloatArray> {
        val segments = mutableListOf<FloatArray>()
        val nSegments = ppgData.size / SEGMENT_SIZE
        Log.d(TAG, "Creating Python-style segments:")
        Log.d(TAG, "  Data length: ${ppgData.size}")
        Log.d(TAG, "  Segment size: $SEGMENT_SIZE")
        Log.d(TAG, "  Number of segments: $nSegments")
        for (i in 0 until nSegments) {
            val startIdx = i * SEGMENT_SIZE
            val endIdx = (i + 1) * SEGMENT_SIZE
            val segment = ppgData.sliceArray(startIdx until endIdx)
            segments.add(segment)
            Log.d(TAG, "Segment $i: indices $startIdx-${endIdx-1} (${segment.size} points)")
        }
        return segments
    }
    
    private fun normalizeWithGlobalMinMax(segment: FloatArray, minPPG: Float, maxPPG: Float): FloatArray {
        val range = maxPPG - minPPG
        if (range <= 0) {
            Log.w(TAG, "Invalid range for normalization: $range")
            return segment.map { 0.5f }.toFloatArray()
        }
        val normalized = segment.map { value ->
            ((value - minPPG) / range).coerceIn(0f, 1f)
        }.toFloatArray()
        val originalRange = "min=${segment.minOrNull()}, max=${segment.maxOrNull()}"
        val normalizedRange = "min=${normalized.minOrNull()}, max=${normalized.maxOrNull()}"
        Log.d(TAG, "Normalization: $originalRange -> $normalizedRange")
        return normalized
    }
    
    private fun savePythonStyleSegments(baseName: String, segments: List<FloatArray>): List<String> {
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
                Log.d(TAG, "Saved Python-style: $fileName (${segment.size} points)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Python-style segments: ${e.message}")
        }
        return outputFiles
    }
    
    private fun parseNumpyArray(bytes: ByteArray): FloatArray {
        try {
            if (bytes.size < 10) {
                Log.e(TAG, "File too small: ${bytes.size} bytes")
                return FloatArray(0)
            }
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
                else -> FloatArray(0)
            } ?: FloatArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing numpy: ${e.message}")
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
} 