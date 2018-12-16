package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.AttributeSet
import android.widget.Button
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.custom_button_view.view.*

class CustomButton(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {
    var text: String
        get() = btn.text.toString()
        set(value) {
            btn.text = value
        }

    val innerButton: Button
        get() = btn

    init {
        inflate(context, R.layout.custom_button_view, this)
        context.obtainStyledAttributes(attrs, R.styleable.CustomButton).apply {
            btn.text = getString(R.styleable.CustomButton_text)
            recycle()
        }

        val shapeDrawable = ShapeDrawable().apply {
            paint.color = resources.getColor(R.color.white)
            paint.setShadowLayer(6f, 1.5f, 1.5f, 0x000000)
            shape = RoundRectShape((1..8).map { 6f }.toFloatArray(), null, null)
        }
        setLayerType(LAYER_TYPE_SOFTWARE, shapeDrawable.paint)

        backgroundCompat = LayerDrawable(arrayOf<Drawable>(shapeDrawable)).apply {
            setLayerInset(0, 10, 10, 10, 10)
        }
    }

    var typeface: Typeface
        get() = btn.typeface
        set(value) {
            btn.typeface = value
        }
}