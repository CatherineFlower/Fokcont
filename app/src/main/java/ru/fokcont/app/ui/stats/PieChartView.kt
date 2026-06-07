package ru.fokcont.app.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import ru.fokcont.app.R

class PieChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<Pair<String, Long>> = emptyList()
    private val colors = listOf(
        0xFF4E7E6B.toInt(), // Primary
        0xFF8FA998.toInt(), // Accent
        0xFFA7C4B5.toInt(), // Primary Light
        0xFFBDBDBD.toInt(), // Grey
        0xFF636E72.toInt()  // Text Secondary
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rectF = RectF()

    fun setData(newData: List<Pair<String, Long>>) {
        this.data = newData
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val size = Math.min(width, height) * 0.8f
        
        rectF.set(
            (width - size) / 2,
            (height - size) / 2,
            (width + size) / 2,
            (height + size) / 2
        )

        // Если данных вообще нет (нет сессий)
        if (data.isEmpty()) {
            paint.color = 0xCCFFFFFF.toInt() // Полупрозрачный белый фон под текст
            canvas.drawArc(rectF, 0f, 360f, true, paint)
            
            paint.color = 0xFF636E72.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 40f
            canvas.drawText("Нет данных", width / 2, height / 2 + 15f, paint)
            return
        }

        val total = data.sumOf { it.second }.toFloat()
        
        // Если сессии есть, но отвлечений 0 - рисуем сплошной зеленый круг
        if (total == 0f) {
            paint.color = 0xFF4E7E6B.toInt() // Primary Green
            canvas.drawArc(rectF, 0f, 360f, true, paint)
            
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 36f
            canvas.drawText("Фокус 100%", width / 2, height / 2 + 15f, paint)
            return
        }

        var startAngle = 0f
        data.forEachIndexed { index, pair ->
            val sweepAngle = (pair.second / total) * 360f
            paint.color = colors[index % colors.size]
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }
    }
}
