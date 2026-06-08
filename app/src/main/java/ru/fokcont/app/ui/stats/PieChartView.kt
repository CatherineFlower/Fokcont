package ru.fokcont.app.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<Pair<String, Long>> = emptyList()

    private val colors = listOf(
        0xFF4E7E6B.toInt(),
        0xFFE05252.toInt(),
        0xFF598EBA.toInt(),
        0xFFDAAA4B.toInt(),
        0xFF8E6BB5.toInt()
    )

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }

    private val rectF = RectF()

    fun setData(newData: List<Pair<String, Long>>) {
        data = newData.filter { it.second > 0L }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val size = min(viewWidth, viewHeight) * 0.76f

        rectF.set(
            (viewWidth - size) / 2f,
            (viewHeight - size) / 2f,
            (viewWidth + size) / 2f,
            (viewHeight + size) / 2f
        )

        if (data.isEmpty()) {
            slicePaint.color = 0xFF4E7E6B.toInt()
            canvas.drawArc(rectF, 0f, 360f, true, slicePaint)

            canvas.drawText("Фокус", viewWidth / 2f, viewHeight / 2f - 6f, smallTextPaint)
            canvas.drawText("100%", viewWidth / 2f, viewHeight / 2f + 28f, textPaint)
            return
        }

        val total = data.sumOf { it.second }.toFloat()
        var startAngle = -90f

        data.forEachIndexed { index, pair ->
            val sweepAngle = pair.second / total * 360f

            slicePaint.color = colors[index % colors.size]
            canvas.drawArc(rectF, startAngle, sweepAngle, true, slicePaint)

            if (sweepAngle >= 22f) {
                drawSliceLabel(canvas, pair.first, pair.second, total, startAngle, sweepAngle, size)
            }

            startAngle += sweepAngle
        }
    }

    private fun drawSliceLabel(
        canvas: Canvas,
        label: String,
        value: Long,
        total: Float,
        startAngle: Float,
        sweepAngle: Float,
        size: Float
    ) {
        val percent = value / total * 100f
        val middleAngle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())

        val radius = size * 0.28f
        val centerX = width / 2f
        val centerY = height / 2f

        val x = centerX + cos(middleAngle).toFloat() * radius
        val y = centerY + sin(middleAngle).toFloat() * radius

        val shortLabel = if (label.length > 9) label.take(9) + "…" else label

        canvas.drawText(shortLabel, x, y - 8f, smallTextPaint)
        canvas.drawText("${percent.toInt()}%", x, y + 26f, textPaint)
    }
}