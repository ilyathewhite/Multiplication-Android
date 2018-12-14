package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.res.Resources
import android.widget.TextView

enum class UnitAnimation { ByRow, ByUnit }

class Row(val multiplier: Int, val tvMultiplier: TextView, val units: Array<UnitView>, val tvProduct: TextView) {
    private val resources: Resources = tvMultiplier.resources

    fun setIsMultiplierActive(value: Boolean) {
        tvMultiplier.alpha = if (value) 1f else Settings.RowMultiplicandInactiveAlpha
    }

    fun animateIsMultiplierActive(value: Boolean, duration: Long): Animator? {
        val alpha = if (value) 1f else Settings.RowMultiplicandInactiveAlpha
        return if (tvMultiplier.alpha == alpha) null else tvMultiplier.alphaAnimator(alpha, duration)
    }

    fun setText(text: String) {
        tvProduct.text = text
    }

    fun animateText(text: String, duration: Long): Animator? {
        if (tvProduct.text == text)
            return null
        tvProduct.alpha = 0f
        tvProduct.text = text
        return tvProduct.alphaAnimator(1f, duration)
    }

    fun setUnitState(state: UnitState) {
        units.forEach { unit -> unit.setState(state) }
    }

    fun animateUnitState(state: UnitState, unitAnimation: UnitAnimation, reverse: Boolean, duration: Long): Animator {
        val switchDuration = Settings.RowUnitSwitchDuration
        return AnimatorSet().apply {
            when (unitAnimation) {
                UnitAnimation.ByRow ->
                    playTogether(units.map { unit -> unit.animateState(state, switchDuration, duration - switchDuration) })
                UnitAnimation.ByUnit -> {
                    val units = if (reverse) this@Row.units.reversed().toTypedArray() else this@Row.units
                    playSequentially(units.map { unit -> unit.animateState(state, switchDuration, (duration / units.size) - switchDuration) })
                }
            }
        }
    }
}