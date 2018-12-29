package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout


open class ShadowBadge(context: Context, attr: AttributeSet) : FrameLayout(context, attr) {
    private lateinit var bitmap: Bitmap

    init {
        setWillNotDraw(false)
        setLayerType(Button.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.shadowBadgeColor) // no matter
            strokeWidth = resources.getDimension(R.dimen.shadowBadgeStrokeWidth)
            setShadowLayer(
                resources.getDimension(R.dimen.shadowBadgeShadowRadius),
                resources.getDimension(R.dimen.shadowBadgeShadowDX),
                resources.getDimension(R.dimen.shadowBadgeShadowDY),
                ContextCompat.getColor(context, R.color.shadowBadgeShadowColor)
            )
        }

        val path = string2GraphicsPath(resources.getString(R.string.shadowBadgePath))

        val padding = resources.getDimensionPixelSize(R.dimen.shadowBadgeShadowPadding)

        val matrix = Matrix()
        val rc = RectF()
        path.computeBounds(rc, true)
        matrix.setTranslate(-rc.left, -rc.top)
        path.transform(matrix)
        path.computeBounds(rc, true)

        matrix.reset()
        matrix.setScale((w - 2 * padding) / rc.width(), (h - 2 * padding) / rc.height(), 0f, 0f)
        path.transform(matrix)
        path.computeBounds(rc, true)

        matrix.reset()
        matrix.setTranslate(padding.toFloat(), padding.toFloat())
        path.transform(matrix)

        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        Canvas(bitmap).drawPath(path, paint)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!.drawBitmap(bitmap, 0f, 0f, null)
    }
}