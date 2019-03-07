package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.TextView

enum class UnitAnimation { ByRow, ByUnit }

class Row(val multiplier: Int, val tvMultiplier: TextView, val units: Array<UnitView>, val tvText: TextView) {
    private val resources: Resources = tvMultiplier.resources

    fun setIsMultiplierActive(value: Boolean) {
        tvMultiplier.alpha = if (value) 1f else Settings.RowMultiplicandInactiveAlpha
    }

    fun animateIsMultiplierActive(value: Boolean, duration: Long): Animator? {
        val alpha = if (value) 1f else Settings.RowMultiplicandInactiveAlpha
        return if (tvMultiplier.alpha == alpha) null else tvMultiplier.alphaAnimator(alpha, duration)
    }

    fun setText(text: String) {
        tvText.text = text
    }

    fun animateText(text: String, duration: Long): Animator? {
        if (tvText.text == text)
            return null
        tvText.alpha = 0f
        tvText.text = text
        return tvText.alphaAnimator(1f, duration)
    }

    suspend fun pulseRowText(scale: Float, duration: Long) {
        val pivotXValue = tvText.paint.measureText(tvText.text.toString()) / 2 / tvText.width

        ScaleAnimation(tvText.scaleX, scale, tvText.scaleY, scale, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            fillAfter = true
            this.duration = duration / 2
            run(tvText)
        }

        ScaleAnimation(scale, 1f, scale, 1f, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            fillAfter = true
            this.duration = duration / 2
            run(tvText)
        }
    }

    fun setUnitState(state: UnitState) {
        units.forEach { unit -> unit.setState(state) }
    }

    fun animateUnitState(state: UnitState, unitAnimation: UnitAnimation, reverse: Boolean, duration: Long): Animator {
        val switchDuration = Settings.RowUnitSwitchDuration
        return when (unitAnimation) {
            UnitAnimation.ByRow ->
                units.map { unit -> unit.animateState(state, switchDuration, duration - switchDuration) }.playTogether()
            UnitAnimation.ByUnit -> {
                val units = if (reverse) this@Row.units.reversed().toTypedArray() else this@Row.units
                units.map { unit -> unit.animateState(state, switchDuration, (duration / units.size) - switchDuration) }.playSequentially()
            }
        }
    }

    fun crossFadeUnitState(state: UnitState, duration: Long): Animator {
        return units.map { unit -> unit.animateState(state, duration, 0) }.playTogether()
    }
}