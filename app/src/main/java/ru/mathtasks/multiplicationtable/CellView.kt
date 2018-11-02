package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.ImageView

enum class CellState { Empty, Filled, ToBeFilled, WasEmptied }
enum class CellAnimationDirection { LeftToRight, TopToBottom }

class CellView : ImageView {
    private var drawable2: ClipDrawable? = null
    private var drawable1: ClipDrawable? = null

    constructor(context: Context, state: CellState) : super(context) {
        setImageDrawable(cell(state))
    }

    constructor(context: Context, state1: CellState, state2: CellState, dir: CellAnimationDirection) : super(context) {
        val orientation = if(dir == CellAnimationDirection.LeftToRight) ClipDrawable.HORIZONTAL else ClipDrawable.VERTICAL
        drawable1 = ClipDrawable(cell(state1), if(dir == CellAnimationDirection.LeftToRight) Gravity.RIGHT else Gravity.BOTTOM, orientation) . apply { level = 10000 }
        drawable2 = ClipDrawable(cell(state2), if(dir == CellAnimationDirection.LeftToRight) Gravity.LEFT else Gravity.TOP, orientation) .apply { level = 0 }
        setImageDrawable(LayerDrawable(arrayOf(drawable1, drawable2)))
    }

    var level = 0
        set(value) {
            if (drawable1 != null)
                drawable1!!.level = 10000 - value
            if (drawable2 != null)
                drawable2!!.level = value
        }

    private fun cell(s: CellState) = resources.getDrawable(when (s) {
        CellState.Empty -> R.drawable.cell_empty
        CellState.Filled -> R.drawable.cell_filled
        CellState.ToBeFilled -> R.drawable.cell_to_be_filled
        CellState.WasEmptied -> R.drawable.cell_was_emptied
    })
}