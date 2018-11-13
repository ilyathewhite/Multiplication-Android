package ru.mathtasks.multiplicationtable

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.ViewGroup
import android.widget.ImageView

enum class AnimationType { Fast, HintFwd1, HintFwd2, HintBack1, HintBack2 }

class CellsView : ImageView
{
    private val multiplier: Int
    private val dimension: Int
    private var cells: Array<CellView>

    constructor(context: Context, multiplier: Int, dimension: Int) : super(context)
    {
        this.multiplier = multiplier
        this.dimension = dimension
        this.cells = (1..multiplier).map { CellView(context) }.toTypedArray()
        val cellMargin = context.resources.getDimensionPixelSize(R.dimen.cellMargin)
        background = LayerDrawable(cells).apply {
            for (i in 0 until cells.size)
                setLayerInset(i, i * dimension + cellMargin, cellMargin, (multiplier - 1 - i) * dimension + cellMargin, cellMargin)
        }
        layoutParams = ViewGroup.LayoutParams(dimension * multiplier, dimension)
    }

    fun animate(toState: CellState, type: AnimationType) : AnimatorSet {
        val duration = context.resources.getInteger(when(type) {
            AnimationType.Fast -> R.integer.cellsAnimationFastDuration
            AnimationType.HintFwd1, AnimationType.HintBack1 -> R.integer.cellsAnimationHint1Duration
            AnimationType.HintFwd2, AnimationType.HintBack2 -> R.integer.cellsAnimationHint2Duration
        }).toLong()
        var animators = cells.map { cell -> cell.animate(toState, duration / multiplier) }
        return when(type) {
            AnimationType.Fast -> AnimatorSet().apply { playTogether(animators); }
            AnimationType.HintFwd1, AnimationType.HintFwd2 -> AnimatorSet().apply { playSequentially(animators); }
            AnimationType.HintBack1, AnimationType.HintBack2 -> AnimatorSet().apply { playSequentially(animators.reversed()); }
        }
    }
}