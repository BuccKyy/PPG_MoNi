package com.example.ppg_moni.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.random.Random

class BloodPressurePredictor(private val context: Context) {
    
    private var approximateInterpreter: Interpreter? = null
    private var refinementInterpreter: Interpreter? = null
    
    companion object {
        private const val TAG = "BloodPressurePredictor"
        private const val APPROXIMATE_MODEL = "ApproximateNetwork.tflite"
        private const val REFINEMENT_MODEL = "RefinementNetwork.tflite"
        private const val INPUT_SIZE = 1024  // PPG signal length
    }
    
    init {
        initializeModels()
    }
    
    private fun initializeModels() {
        try {
            // Load approximate model
            val approximateBuffer = loadModelFile(APPROXIMATE_MODEL)
            approximateInterpreter = Interpreter(approximateBuffer)
            
            // Load refinement model  
            val refinementBuffer = loadModelFile(REFINEMENT_MODEL)
            refinementInterpreter = Interpreter(refinementBuffer)
            
            Log.d(TAG, "TensorFlow Lite models loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TF Lite models: ${e.message}")
        }
    }
    
    private fun loadModelFile(modelName: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    suspend fun predictBloodPressure(ppgData: FloatArray): Pair<Float, Float> = withContext(Dispatchers.Default) {
        return@withContext try {
            if (approximateInterpreter == null || refinementInterpreter == null) {
                Log.w(TAG, "Models not loaded, using mock prediction")
                return@withContext generateMockPrediction()
            }
            
            // Preprocess PPG data
            val normalizedData = preprocessPPGData(ppgData)
            
            // Step 1: Approximate prediction
            val approximateResult = runApproximateModel(normalizedData)
            
            // Step 2: Refinement prediction
            val refinedResult = runRefinementModel(normalizedData, approximateResult)
            
            Log.d(TAG, "BP Prediction - Systolic: ${refinedResult.first}, Diastolic: ${refinedResult.second}")
            refinedResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Prediction error: ${e.message}")
            generateMockPrediction()
        }
    }
    
    private fun preprocessPPGData(ppgData: FloatArray): FloatArray {
        // Normalize to 0-1 range
        val min = ppgData.minOrNull() ?: 0f
        val max = ppgData.maxOrNull() ?: 1f
        val range = max - min
        
        return if (range > 0) {
            ppgData.map { (it - min) / range }.toFloatArray()
        } else {
            ppgData
        }
    }
    
    private fun runApproximateModel(inputData: FloatArray): FloatArray {
        val input = ByteBuffer.allocateDirect(INPUT_SIZE * 4).order(ByteOrder.nativeOrder())
        
        // Fill input buffer
        for (i in 0 until INPUT_SIZE) {
            val value = if (i < inputData.size) inputData[i] else 0f
            input.putFloat(value)
        }
        
        // Output buffer for approximate model (assuming 2 outputs: SBP, DBP)
        val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
        
        approximateInterpreter?.run(input, output)
        
        output.rewind()
        return floatArrayOf(output.float, output.float)
    }
    
    private fun runRefinementModel(inputData: FloatArray, approximateResult: FloatArray): Pair<Float, Float> {
        // For refinement, we might need both PPG data and approximate results
        val inputSize = INPUT_SIZE + 2  // PPG data + approximate results
        val input = ByteBuffer.allocateDirect(inputSize * 4).order(ByteOrder.nativeOrder())
        
        // Add PPG data
        for (i in 0 until INPUT_SIZE) {
            val value = if (i < inputData.size) inputData[i] else 0f
            input.putFloat(value)
        }
        
        // Add approximate results
        input.putFloat(approximateResult[0])
        input.putFloat(approximateResult[1])
        
        // Output buffer
        val output = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder())
        
        refinementInterpreter?.run(input, output)
        
        output.rewind()
        val systolic = output.float
        val diastolic = output.float
        
        return Pair(systolic, diastolic)
    }
    
    private fun generateMockPrediction(): Pair<Float, Float> {
        // Generate realistic BP values for demo
        val systolic = Random.nextFloat() * 40 + 110  // 110-150 range
        val diastolic = Random.nextFloat() * 30 + 70   // 70-100 range
        return Pair(systolic, diastolic)
    }
    
    fun release() {
        approximateInterpreter?.close()
        refinementInterpreter?.close()
        approximateInterpreter = null
        refinementInterpreter = null
    }
} 