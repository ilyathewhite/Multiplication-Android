package ru.mathtasks.multiplicationtable

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.ViewGroup
import android.widget.ImageView

enum class AnimationType { Fast, HintFwd1, HintFwd2, HintBack1, HintBack2 }

class CellsView : ImageView
{
    private val multiplicand: Int
    private val dimension: Int
    private var cells: Array<CellView>

    constructor(context: Context, multiplicand: Int, dimension: Int) : super(context)
    {
        this.multiplicand = multiplicand
        this.dimension = dimension
        this.cells = (1..multiplicand).map { CellView(context) }.toTypedArray()
        val cellMargin = resources.getDimensionPixelSize(R.dimen.cellMargin)
        background = LayerDrawable(cells).apply {
            for (i in 0 until cells.size)
                setLayerInset(i, i * dimension + cellMargin, cellMargin, (multiplicand - 1 - i) * dimension + cellMargin, cellMargin)
        }
        layoutParams = ViewGroup.LayoutParams(dimension * multiplicand, dimension)
    }

    fun animate(toState: CellState, type: AnimationType) : AnimatorSet {
        val duration = context.resources.getInteger(when(type) {
            AnimationType.Fast -> R.integer.cellsAnimationFastDuration
            AnimationType.HintFwd1, AnimationType.HintBack1 -> R.integer.cellsAnimationHint1Duration
            AnimationType.HintFwd2, AnimationType.HintBack2 -> R.integer.cellsAnimationHint2Duration
        }).toLong()
        var animators = cells.map { cell -> cell.animate(toState, duration / multiplicand) }
        return AnimatorSet().apply {
            when(type) {
                AnimationType.Fast -> playTogether(animators)
                AnimationType.HintFwd1, AnimationType.HintFwd2 -> playSequentially(animators)
                AnimationType.HintBack1, AnimationType.HintBack2 -> playSequentially(animators.reversed())
            }
        }
    }
}