package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.support.v4.content.ContextCompat
import android.view.View

enum class UnitState { Disabled, Counted, ToBeCounted, WasCounted }

class UnitView : View {
    private var fillColor: Int
    private val strokeWidthPixel: Int
    private var strokeColor: Int
    private val drawable: GradientDrawable

    constructor(context: Context) : super(context) {
        this.fillColor = state2FillColor(UnitState.Disabled)
        this.strokeColor = state2StrokeColor(UnitState.Disabled)
        this.strokeWidthPixel = context.resources.getDimensionPixelSize(R.dimen.unitViewStrokeWidth)
        this.drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(strokeWidthPixel, strokeColor)
        }
        backgroundCompat = drawable
    }

    fun onResize(unitSize: Int) {
        this.drawable.cornerRadius = unitSize * context.resources.getFloat(R.dimen.unitViewCornerRadiusRatio)
    }

    private fun state2FillColor(state: UnitState) = ContextCompat.getColor(
        context, when (state) {
            UnitState.Disabled -> R.color.unitViewDisabled
            UnitState.Counted -> R.color.unitViewCounted
            UnitState.ToBeCounted -> R.color.unitViewToBeCounted
            UnitState.WasCounted -> R.color.unitViewWasCounted
        }
    )

    private fun state2StrokeColor(state: UnitState) = ContextCompat.getColor(
        context, when (state) {
            UnitState.Disabled -> R.color.unitViewDisabledStroke
            UnitState.Counted -> R.color.unitViewCountedStroke
            UnitState.ToBeCounted -> R.color.unitViewToBoCountedStroke
            UnitState.WasCounted -> R.color.unitViewWasCountedStroke
        }
    )

    fun setState(toState: UnitState) {
        this.fillColor = state2FillColor(toState)
        drawable.setColor(fillColor)
        this.strokeColor = state2StrokeColor(toState)
        drawable.setStroke(strokeWidthPixel, strokeColor)
    }

    fun animateState(state: UnitState, duration: Long, startDelay: Long): Animator {
        val fillAnimator = ValueAnimator.ofObject(ArgbEvaluator(), fillColor, state2FillColor(state))
        fillAnimator.startDelay = startDelay
        fillAnimator.duration = duration
        fillAnimator.addUpdateListener { animator ->
            this.fillColor = animator.animatedValue as Int
            drawable.setColor(fillColor)
        }
        val strokeAnimator = ValueAnimator.ofObject(ArgbEvaluator(), strokeColor, state2StrokeColor(state))
        strokeAnimator.startDelay = startDelay
        strokeAnimator.duration = duration
        strokeAnimator.addUpdateListener { animator ->
            this.strokeColor = animator.animatedValue as Int
            drawable.setStroke(strokeWidthPixel, strokeColor)
        }
        return listOf(fillAnimator, strokeAnimator).playTogether()
    }
}