package com.example.detectorproj

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var moduleboxPaint = Paint()
    private var antennaboxPaint = Paint()
    private var defaultPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        moduleboxPaint.reset()
        antennaboxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textPaint.color = ContextCompat.getColor(context!!, R.color.textColor)
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        moduleboxPaint.color = ContextCompat.getColor(context!!, R.color.module_detected)
        moduleboxPaint.strokeWidth = 8F
        moduleboxPaint.style = Paint.Style.STROKE

        antennaboxPaint.color = ContextCompat.getColor(context!!, R.color.antenna_detected)
        antennaboxPaint.strokeWidth = 8F
        antennaboxPaint.style = Paint.Style.STROKE

        defaultPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        defaultPaint.strokeWidth = 8F
        defaultPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach { result ->
            val left = result.x1 * width
            val top = result.y1 * height
            val right = result.x2 * width
            val bottom = result.y2 * height

            val paint = when (result.clsName) {
                "antenna" -> antennaboxPaint
                "module" -> moduleboxPaint
                else -> defaultPaint
            }

            canvas.drawRect(left, top, right, bottom, paint)

            val drawableText = (result.clsName + " " + (maxConfToSend * 100F).roundToInt() + "%")

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length , bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawText(drawableText, left, top - BOUNDING_RECT_TEXT_PADDING, textPaint)

        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}