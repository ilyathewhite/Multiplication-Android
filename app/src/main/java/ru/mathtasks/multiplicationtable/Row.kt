package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.res.Resources
import android.widget.TextView

enum class UnitAnimation { ByRow, ByUnit }

class Row(val multiplier: Int, private val tvMultiplier: TextView, private val units: Array<UnitView>, private val tv: TextView) {
    private val resources: Resources = tvMultiplier.resources

    fun setIsMultiplierActive(value: Boolean) {
        tvMultiplier.alpha = if (value) 1f else Settings.RowMultiplicandInactiveAlpha
    }

    fun animateIsMultiplierActive(value: Boolean, duration: Long): Animator {
        return tvMultiplier.alphaAnimator(if (value) 1f else Settings.RowMultiplicandInactiveAlpha, duration)
    }

    fun setText(text: String) {
         tv.text = text
    }

    fun animateText(text: String, duration: Long): Animator? {
        if (tv.text == text)
            return null
        tv.alpha = 0f
        tv.text = text
        return tv.alphaAnimator(1f, duration)
    }

    fun setUnitState(state: UnitState) {
        units.forEach { unit -> unit.setState(state) }
    }

    fun animateUnitState(state: UnitState, unitAnimation: UnitAnimation, reverse: Boolean, totalDuration: Long): Animator {
        val switchDuration = Settings.RowUnitSwitchDuration
        return AnimatorSet().apply {
            when (unitAnimation) {
                UnitAnimation.ByRow ->
                    playTogether(units.map { unit -> unit.animateState(state, switchDuration, totalDuration - switchDuration) })
                UnitAnimation.ByUnit -> {
                    val units = if (reverse) this@Row.units.reversed().toTypedArray() else this@Row.units
                    playSequentially(units.map { unit -> unit.animateState(state, switchDuration, (totalDuration / units.size) - switchDuration) })
                }
            }
        }
    }
}