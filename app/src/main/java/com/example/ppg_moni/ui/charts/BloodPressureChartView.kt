package com.example.ppg_moni.ui.charts

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.ppg_moni.R
import com.example.ppg_moni.data.models.BloodPressurePrediction
import com.example.ppg_moni.data.models.BPCategory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.SimpleDateFormat
import java.util.*

class BloodPressureChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LineChart(context, attrs, defStyleAttr) {

    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    
    init {
        setupChart()
    }
    
    private fun setupChart() {
        // Chart appearance
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        setBackgroundColor(ContextCompat.getColor(context, R.color.chart_background_color))
        
        // Grid
        setDrawGridBackground(false)
        
        // X-axis (dates)
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(true)
            gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
            textColor = ContextCompat.getColor(context, R.color.text_secondary)
            textSize = 10f
            labelRotationAngle = -45f
            granularity = 1f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateFormat.format(Date(value.toLong()))
                }
            }
        }
        
        // Left Y-axis (Blood Pressure values)
        axisLeft.apply {
            setDrawGridLines(true)
            gridColor = ContextCompat.getColor(context, R.color.chart_grid_color)
            textColor = ContextCompat.getColor(context, R.color.text_secondary)
            textSize = 10f
            axisMinimum = 40f
            axisMaximum = 200f
            
            // Add reference lines for BP categories
            removeAllLimitLines()
            
            // Normal BP upper limit (120/80)
            addLimitLine(com.github.mikephil.charting.components.LimitLine(120f, "").apply {
                lineColor = ContextCompat.getColor(context, R.color.bp_normal_color)
                lineWidth = 1f
                enableDashedLine(10f, 10f, 0f)
                labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(context, R.color.bp_normal_color)
                textSize = 9f
            })
            
            // Stage 1 Hypertension (130)
            addLimitLine(com.github.mikephil.charting.components.LimitLine(130f, "").apply {
                lineColor = ContextCompat.getColor(context, R.color.bp_stage1_color)
                lineWidth = 1f
                enableDashedLine(10f, 10f, 0f)
                labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(context, R.color.bp_stage1_color)
                textSize = 9f
            })
            
            // Stage 2 Hypertension (140)
            addLimitLine(com.github.mikephil.charting.components.LimitLine(140f, "").apply {
                lineColor = ContextCompat.getColor(context, R.color.bp_stage2_color)
                lineWidth = 1f
                enableDashedLine(10f, 10f, 0f)
                labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(context, R.color.bp_stage2_color)
                textSize = 9f
            })
            
            // Crisis level (180)
            addLimitLine(com.github.mikephil.charting.components.LimitLine(180f, "").apply {
                lineColor = ContextCompat.getColor(context, R.color.bp_crisis_color)
                lineWidth = 2f
                enableDashedLine(5f, 5f, 0f)
                labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                textColor = ContextCompat.getColor(context, R.color.bp_crisis_color)
                textSize = 9f
            })
        }
        
        // Right Y-axis disabled
        axisRight.isEnabled = false
        
        // Legend
        legend.apply {
            isEnabled = true
            textColor = ContextCompat.getColor(context, R.color.text_primary)
            textSize = 12f
            verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
        }
        
        // No data text
        setNoDataText("Chưa có dữ liệu huyết áp")
        setNoDataTextColor(ContextCompat.getColor(context, R.color.text_secondary))
    }
    
    fun updateChart(predictions: List<BloodPressurePrediction>) {
        if (predictions.isEmpty()) {
            clear()
            invalidate()
            return
        }
        
        // Sort predictions by date
        val sortedPredictions = predictions.sortedBy { it.predictedAt }
        
        // Create entries for systolic and diastolic
        val systolicEntries = mutableListOf<Entry>()
        val diastolicEntries = mutableListOf<Entry>()
        
        sortedPredictions.forEach { prediction ->
            val timestamp = prediction.predictedAt.toFloat()
            systolicEntries.add(Entry(timestamp, prediction.systolicBP))
            diastolicEntries.add(Entry(timestamp, prediction.diastolicBP))
        }
        
        // Create datasets
        val systolicDataSet = createSystolicDataSet(systolicEntries)
        val diastolicDataSet = createDiastolicDataSet(diastolicEntries)
        
        // Set data
        val dataSets = mutableListOf<ILineDataSet>()
        dataSets.add(systolicDataSet)
        dataSets.add(diastolicDataSet)
        
        data = LineData(dataSets)
        
        // Refresh chart
        notifyDataSetChanged()
        invalidate()
        
        // Auto-fit to show all data
        fitScreen()
    }
    
    private fun createSystolicDataSet(entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, context.getString(R.string.chart_systolic)).apply {
            color = ContextCompat.getColor(context, R.color.chart_systolic_color)
            setCircleColor(ContextCompat.getColor(context, R.color.chart_systolic_color))
            lineWidth = 3f
            circleRadius = 5f
            circleHoleRadius = 2f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(context, R.color.chart_systolic_color)
            fillAlpha = 30
            setDrawValues(true)
            valueTextColor = ContextCompat.getColor(context, R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}"
                }
            }
            
            // Highlight high values
            highLightColor = ContextCompat.getColor(context, R.color.error_color)
            highlightLineWidth = 2f
        }
    }
    
    private fun createDiastolicDataSet(entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, context.getString(R.string.chart_diastolic)).apply {
            color = ContextCompat.getColor(context, R.color.chart_diastolic_color)
            setCircleColor(ContextCompat.getColor(context, R.color.chart_diastolic_color))
            lineWidth = 3f
            circleRadius = 5f
            circleHoleRadius = 2f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(context, R.color.chart_diastolic_color)
            fillAlpha = 30
            setDrawValues(true)
            valueTextColor = ContextCompat.getColor(context, R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}"
                }
            }
            
            // Highlight high values
            highLightColor = ContextCompat.getColor(context, R.color.error_color)
            highlightLineWidth = 2f
        }
    }
    
    fun highlightAbnormalReadings(predictions: List<BloodPressurePrediction>) {
        // Find abnormal readings and highlight them
        val abnormalIndices = mutableListOf<Int>()
        
        predictions.forEachIndexed { index, prediction ->
            if (prediction.category != BPCategory.NORMAL) {
                abnormalIndices.add(index)
            }
        }
        
        // Create highlight objects for abnormal readings
        val highlights = abnormalIndices.map { index ->
            com.github.mikephil.charting.highlight.Highlight(index.toFloat(), 0, 0)
        }.toTypedArray()
        
        // Apply highlights
        highlightValues(highlights)
    }
    
    fun showLastNDays(days: Int, predictions: List<BloodPressurePrediction>) {
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        val recentPredictions = predictions.filter { it.predictedAt >= cutoffTime }
        updateChart(recentPredictions)
    }
    
    fun exportChartImage(): android.graphics.Bitmap? {
        return chartBitmap
    }
} 