package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout


class ShadowPanel(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    private lateinit var bitmap: Bitmap

    init {
        setWillNotDraw(false)
        setLayerType(Button.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val stroke = resources.getDimension(R.dimen.shadowPanelStrokeWidth)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.WHITE // no matter
            strokeWidth = stroke
            setShadowLayer(
                resources.getDimension(R.dimen.shadowPanelShadowRadius),
                resources.getDimension(R.dimen.shadowPanelShadowDX),
                resources.getDimension(R.dimen.shadowPanelShadowDY),
                ContextCompat.getColor(context, R.color.shadowPanelShadowColor)
            )
        }

        val gradColors = intArrayOf(ContextCompat.getColor(context, R.color.shadowPanelTopColor), ContextCompat.getColor(context, R.color.shadowPanelBottomColor))
        val grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradColors).apply { setBounds(0, 0, w, h) }
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        Canvas(bitmap).apply {
            grad.draw(this)
            drawRect(-stroke / 2, -stroke / 2, w + stroke / 2, h + stroke / 2, paint)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawBitmap(bitmap, 0f, 0f, null)
    }
}