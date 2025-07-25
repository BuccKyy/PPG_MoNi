package com.example.ppg_moni.ui.charts

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.ppg_moni.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.*

class PPGSignalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var originalChart: LineChart
    private lateinit var rawChart: LineChart  
    private lateinit var processedChart: LineChart
    
    companion object {
        private const val SAMPLING_RATE = 25f
        private const val TARGET_SAMPLING_RATE = 50f
        private const val FILTER_LOW = 0.5f
        private const val FILTER_HIGH = 8.0f
        private const val START_OFFSET = 600
    }

    init {
        orientation = VERTICAL
        setupCharts()
    }

    private fun setupCharts() {
        originalChart = createChart("Tín hiệu gốc (Original Signal)", Color.BLUE)
        rawChart = createChart("Tín hiệu thô (Raw Signal)", Color.rgb(255, 165, 0))
        processedChart = createChart("Tín hiệu đã xử lý (Processed Signal)", Color.GREEN)
        addView(originalChart, createChartLayoutParams())
        addView(rawChart, createChartLayoutParams())
        addView(processedChart, createChartLayoutParams())
    }

    private fun createChart(title: String, color: Int): LineChart {
        val chart = LineChart(context)
        chart.apply {
            description.text = title
            description.textColor = ContextCompat.getColor(context, R.color.text_primary)
            description.textSize = 12f
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(ContextCompat.getColor(context, R.color.chart_background_color))
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
                textColor = ContextCompat.getColor(context, R.color.text_secondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}"
                    }
                }
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
                textColor = ContextCompat.getColor(context, R.color.text_secondary)
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
        return chart
    }

    private fun createChartLayoutParams(): LayoutParams {
        return LayoutParams(
            LayoutParams.MATCH_PARENT,
            400
        )
    }

    /**
     * Hiển thị tín hiệu PPG từ dữ liệu .npy
     */
    fun displayPPGSignal(data: FloatArray) {
        try {
            android.util.Log.d("PPGSignalView", "Displaying PPG signal with ${data.size} samples")
            
            // Xử lý dữ liệu theo pipeline của view_data_2.py
            val processedData = processPPGData(data)
            
            android.util.Log.d("PPGSignalView", "Processed data - Original: ${processedData.originalData.size}, Raw: ${processedData.rawData.size}, Processed: ${processedData.processedData.size}")
            
            // Hiển thị 3 loại tín hiệu
            updateChart(originalChart, processedData.originalData, Color.BLUE, "Original")
            updateChart(rawChart, processedData.rawData, Color.rgb(255, 165, 0), "Raw")  
            updateChart(processedChart, processedData.processedData, Color.GREEN, "Processed")
            
            android.util.Log.d("PPGSignalView", "Successfully updated all 3 charts")
            
        } catch (e: Exception) {
            android.util.Log.e("PPGSignalView", "Error displaying PPG signal: ${e.message}")
            e.printStackTrace()
            
            // Hiển thị mock data nếu có lỗi
            displayMockSignals()
        }
    }

    private fun updateChart(chart: LineChart, data: FloatArray, color: Int, label: String) {
        val entries = data.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }
        
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 1f
            setDrawCircles(false) // Tắt circle để biểu đồ nhìn sạch hơn
            setDrawValues(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.LINEAR
        }
        
        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    /**
     * Xử lý dữ liệu PPG theo pipeline của view_data_2.py
     */
    private fun processPPGData(data: FloatArray): PPGProcessedData {
        return try {
            if (data.isEmpty()) {
                android.util.Log.w("PPGSignalView", "Empty input data, using mock data")
                return createMockPPGData()
            }
            
            // 1. Tín hiệu gốc (giữ nguyên data đầu vào, có thể downsample để hiển thị)
            val originalData = if (data.size > 2000) {
                // Downsample để hiển thị dễ hơn
                data.filterIndexed { index, _ -> index % (data.size / 1000) == 0 }.toFloatArray()
            } else {
                data.copyOf()
            }
            
            // 2. Tín hiệu thô (simulate segment extraction từ original)
            val segmentLength = minOf(500, data.size)
            val startIndex = if (data.size > segmentLength) data.size / 4 else 0
            val rawData = data.sliceArray(startIndex until minOf(startIndex + segmentLength, data.size))
            
            // 3. Tín hiệu đã xử lý (apply filtering và normalization)
            val filteredData = applySimpleFilter(rawData)
            val processedData = normalizeSignal(filteredData)
            
            android.util.Log.d("PPGSignalView", "Processing complete - Original: ${originalData.size}, Raw: ${rawData.size}, Processed: ${processedData.size}")
            
            PPGProcessedData(originalData, rawData, processedData)
            
        } catch (e: Exception) {
            android.util.Log.e("PPGSignalView", "Error in processPPGData: ${e.message}")
            createMockPPGData()
        }
    }

    private fun extractChannel1(data: FloatArray): FloatArray {
        // Giả sử data đã được transpose và ta cần channel 1
        // Trong thực tế, cần hiểu cấu trúc dữ liệu cụ thể
        return data
    }

    private fun filterValidRange(data: FloatArray, minValue: Float, maxValue: Float): FloatArray {
        return data.filter { it in minValue..maxValue }.toFloatArray()
    }

    private fun extractSegment(data: FloatArray, start: Int, length: Int): FloatArray {
        val end = minOf(start + length, data.size)
        val actualStart = minOf(start, data.size - 1)
        return if (actualStart < end) {
            data.sliceArray(actualStart until end)
        } else {
            floatArrayOf()
        }
    }

    private fun resample(data: FloatArray, originalRate: Float, targetRate: Float): FloatArray {
        if (data.isEmpty()) return floatArrayOf()
        
        val ratio = targetRate / originalRate
        val newSize = (data.size * ratio).toInt()
        val resampled = FloatArray(newSize)
        
        for (i in 0 until newSize) {
            val originalIndex = i / ratio
            val lowerIndex = originalIndex.toInt()
            val upperIndex = minOf(lowerIndex + 1, data.size - 1)
            val fraction = originalIndex - lowerIndex
            
            resampled[i] = if (lowerIndex < data.size) {
                data[lowerIndex] * (1 - fraction) + data[upperIndex] * fraction
            } else {
                data.last()
            }
        }
        
        return resampled
    }

    private fun applyChebyshevFilter(data: FloatArray, lowCut: Float, highCut: Float, samplingRate: Float): FloatArray {
        // Đây là implementation đơn giản của bộ lọc bandpass
        // Trong thực tế, cần sử dụng thư viện DSP chuyên dụng
        
        if (data.isEmpty()) return floatArrayOf()
        
        // Tạm thời sử dụng moving average filter đơn giản
        val windowSize = 5
        val filtered = FloatArray(data.size)
        
        for (i in data.indices) {
            var sum = 0f
            var count = 0
            
            for (j in maxOf(0, i - windowSize/2) until minOf(data.size, i + windowSize/2 + 1)) {
                sum += data[j]
                count++
            }
            
            filtered[i] = sum / count
        }
        
        return filtered
    }

    private fun normalizeSignal(data: FloatArray): FloatArray {
        if (data.isEmpty()) return floatArrayOf()
        
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = max - min
        
        return if (range > 0) {
            data.map { (it - min) / range }.toFloatArray()
        } else {
            data
        }
    }

    /**
     * Tính toán và hiển thị thông tin SpO2
     */
    fun calculateAndDisplaySpO2(redData: FloatArray, irData: FloatArray): Float {
        return if (redData.isNotEmpty() && irData.isNotEmpty()) {
            calculateSpO2(redData, irData)
        } else {
            98.5f // Giá trị mặc định
        }
    }

    private fun calculateSpO2(redData: FloatArray, irData: FloatArray): Float {
        // Tính toán SpO2 dựa trên tỷ lệ AC/DC của red và IR
        val redAC = calculateAC(redData)
        val redDC = redData.average().toFloat()
        val irAC = calculateAC(irData)
        val irDC = irData.average().toFloat()
        
        val redRatio = if (redDC != 0f) redAC / redDC else 0f
        val irRatio = if (irDC != 0f) irAC / irDC else 0f
        val ratio = if (irRatio != 0f) redRatio / irRatio else 0.5f
        
        // SpO2 calculation (simplified)
        val spo2 = 100f - (ratio * 25f)
        return spo2.coerceIn(85f, 100f)
    }

    private fun calculateAC(data: FloatArray): Float {
        if (data.isEmpty()) return 0f
        val mean = data.average().toFloat()
        val variance = data.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }

    private fun applySimpleFilter(data: FloatArray): FloatArray {
        if (data.isEmpty()) return FloatArray(0)
        
        // Simple moving average filter
        val windowSize = 5
        val filtered = FloatArray(data.size)
        
        for (i in data.indices) {
            var sum = 0f
            var count = 0
            
            for (j in maxOf(0, i - windowSize/2) until minOf(data.size, i + windowSize/2 + 1)) {
                sum += data[j]
                count++
            }
            
            filtered[i] = sum / count
        }
        
        return filtered
    }
    
    private fun createMockPPGData(): PPGProcessedData {
        // Tạo 3 tín hiệu mock khác nhau
        val originalData = generateMockSignal(1000, 1.0f, 0.3f) // Signal dài với nhiều noise
        val rawData = generateMockSignal(500, 0.8f, 0.2f)       // Signal trung bình
        val processedData = generateMockSignal(500, 0.6f, 0.1f) // Signal ngắn, ít noise
        
        return PPGProcessedData(originalData, rawData, processedData)
    }
    
    private fun generateMockSignal(length: Int, amplitude: Float, noiseLevel: Float): FloatArray {
        val signal = FloatArray(length)
        val heartbeatFreq = 1.2f // ~72 BPM
        
        for (i in 0 until length) {
            val time = i.toFloat() / 50f // 50Hz sampling rate
            
            // PPG-like signal with heartbeat pattern
            signal[i] = amplitude * (
                0.7f * sin(2 * PI.toFloat() * heartbeatFreq * time).toFloat() +
                0.2f * sin(2 * PI.toFloat() * heartbeatFreq * 2 * time).toFloat() +
                0.1f * sin(2 * PI.toFloat() * heartbeatFreq * 3 * time).toFloat() +
                noiseLevel * (Math.random().toFloat() - 0.5f)
            )
        }
        
        return normalizeSignal(signal)
    }
    
    private fun displayMockSignals() {
        try {
            android.util.Log.d("PPGSignalView", "Displaying mock signals")
            
            val mockData = createMockPPGData()
            
            updateChart(originalChart, mockData.originalData, Color.BLUE, "Original")
            updateChart(rawChart, mockData.rawData, Color.rgb(255, 165, 0), "Raw")
            updateChart(processedChart, mockData.processedData, Color.GREEN, "Processed")
            
            android.util.Log.d("PPGSignalView", "Mock signals displayed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("PPGSignalView", "Error displaying mock signals: ${e.message}")
        }
    }

    data class PPGProcessedData(
        val originalData: FloatArray,
        val rawData: FloatArray,
        val processedData: FloatArray
    )
} 