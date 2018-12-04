package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.view.View
import android.widget.TextView
import io.reactivex.Completable

enum class UnitAnimation { ByRow, ByUnit }

class Row(val multiplier: Int, private val tvMultiplier: TextView, private val units: Array<UnitView>, private val tv: TextView) {
    private val resources: Resources = tvMultiplier.resources

    fun setIsMultiplierActive(value: Boolean) {
        tvMultiplier.alpha = if (value) 1f else resources.getFloat(R.dimen.rowMultiplicandInactiveAlpha)
    }

    fun animateIsMultiplierActive(value: Boolean, duration: Long): Completable? {
        val toAlpha = if (value) 1f else resources.getFloat(R.dimen.rowMultiplicandInactiveAlpha)
        return if (tvMultiplier.alpha == toAlpha) null else tvMultiplier.animate(duration) { it.alpha(toAlpha) }
    }

    fun setText(text: String) {
        tv.text = text
    }

    fun animateText(text: String, duration: Long): Completable? {
        if (text == tv.text)
            return null
        tv.alpha = 0f
        tv.text = text
        return tv.animate(duration) { it.alpha(1f) }
    }

    fun setUnitState(state: UnitState) {
        units.map { unit -> unit.setState(state) }
    }

    fun animateUnitState(state: UnitState, unitAnimation: UnitAnimation, reverse: Boolean, totalDuration: Long): Completable {
        val switchDuration = resources.getInteger(R.integer.rowUnitSwitchDuration).toLong()
        return when (unitAnimation) {
            UnitAnimation.ByRow ->
                Completable.merge(units.map { unit -> unit.animateState(state, totalDuration - switchDuration, switchDuration) })
            UnitAnimation.ByUnit -> {
                val units = if (reverse) this@Row.units else this@Row.units.reversed().toTypedArray()
                Completable.concat(units.map { unit -> unit.animateState(state, (totalDuration / units.size) - switchDuration, switchDuration) })
            }
        }
    }
}