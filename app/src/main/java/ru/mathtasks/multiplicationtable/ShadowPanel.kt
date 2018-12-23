package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PaintDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.RelativeLayout

class ShadowPanel(context: Context, attr: AttributeSet) : RelativeLayout(context, attr) {
    init {
        val dy = resources.getDimension(R.dimen.shadowPanelShadowDY)
        val cd = ColorDrawable(ContextCompat.getColor(context, R.color.shadowPanelBackgroundShadowColor))
        val pd = PaintDrawable(ContextCompat.getColor(context, R.color.shadowPanelColor)).apply {
            this.paint.setShadowLayer(resources.getDimension(R.dimen.shadowPanelShadowRadius), 0f, dy, ContextCompat.getColor(context, R.color.shadowPanelShadowColor))
        }
        val ld = LayerDrawable(arrayOf(cd, pd)).apply {
            setLayerInset(0, 0, 0, 0, 0)
            setLayerInset(1, 0, 0, 0, resources.getDimensionPixelSize(R.dimen.shadowPanelPadding))
        }
        setBackgroundCompat(ld)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
}