package ru.mathtasks.multiplicationtable

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.*
import android.support.v4.content.ContextCompat
import android.animation.ValueAnimator

enum class CellState { Empty, Filled, ToBeFilled, WasEmptied }

class CellView : GradientDrawable {
    private val context: Context
    private var fillColor: Int
    private val strokeWidthPixel: Int
    private var strokeColor: Int

    constructor(context: Context) {
        this.context = context
        shape = RECTANGLE
        cornerRadius = context.resources.getDimension(R.dimen.cellCornerRadius)
        this.fillColor = state2FillColor(CellState.Empty)
        setColor(fillColor)
        this.strokeColor = state2StrokeColor(CellState.Empty)
        this.strokeWidthPixel = context.resources.getDimensionPixelSize(R.dimen.cellStrokeWidth)
        setStroke(strokeWidthPixel, strokeColor)
    }

    private fun state2FillColor(state: CellState) = ContextCompat.getColor(context, when(state) {
        CellState.Empty -> R.color.cellEmpty
        CellState.Filled -> R.color.cellFilled
        CellState.ToBeFilled -> R.color.cellToBeFilled
        CellState.WasEmptied -> R.color.cellWasEmptied
    })

    private fun state2StrokeColor(state: CellState) = ContextCompat.getColor(context, when(state) {
        CellState.Empty -> R.color.cellEmptyStroke
        CellState.Filled -> R.color.cellFilledStroke
        CellState.ToBeFilled -> R.color.cellToBeFilledStroke
        CellState.WasEmptied -> R.color.cellWasEmptiedStroke
    })

    fun animate(toState: CellState, duration: Long) : AnimatorSet {
        val fillAnimator = ValueAnimator.ofArgb(fillColor, state2FillColor(toState))
        fillAnimator.duration = duration
        fillAnimator.addUpdateListener { animator ->
            this.fillColor = animator.animatedValue as Int
            setColor(fillColor)
        }
        val strokeAnimator = ValueAnimator.ofArgb(strokeColor, state2StrokeColor(toState))
        strokeAnimator.duration = duration
        strokeAnimator.addUpdateListener { animator ->
            this.strokeColor = animator.animatedValue as Int
            setStroke(strokeWidthPixel, strokeColor)
        }
        return AnimatorSet().apply { playTogether(fillAnimator, strokeAnimator) }
    }
}