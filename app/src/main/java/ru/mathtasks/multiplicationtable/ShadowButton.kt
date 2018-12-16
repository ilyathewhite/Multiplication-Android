package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.widget.Button


class ShadowButton(context: Context, attr: AttributeSet) : Button(context, attr) {
    init {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            this.stateListAnimator = null

        val padding = resources.getDimensionPixelSize(R.dimen.shadowButtonPadding)

        val states = StateListDrawable()
        states.addState(intArrayOf(android.R.attr.state_pressed), LayerDrawable(arrayOf(ContextCompat.getDrawable(context, R.drawable.shadow_button_pressed_background))).apply {
            setLayerInset(0, padding, padding, padding, padding)
        })

        val dx = resources.getDimension(R.dimen.shadowButtonShadowDX)
        val dy = resources.getDimension(R.dimen.shadowButtonShadowDY)
        val pd = PaintDrawable(ContextCompat.getColor(context, R.color.shadowButtonColor)).apply {
            setCornerRadius(resources.getDimension(R.dimen.shadowButtonCornerRadius))
            this.paint.setShadowLayer(resources.getDimension(R.dimen.shadowButtonShadowRadius), dx, dy, ContextCompat.getColor(context, R.color.shadowButtonShadowColor))
        }
        states.addState(intArrayOf(), LayerDrawable(arrayOf(pd)).apply {
            setLayerInset(0, padding, padding, padding, padding)
        })

        setBackgroundCompat(states)
    }
}