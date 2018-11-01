package ru.mathtasks.multiplicationtable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView

enum class CellState
{
    Empty, Filled, ToBeFilled, WasEmptied
}
class CellView : ImageView {
    var State: CellState = CellState.Empty;

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        background = getResources().getDrawable(R.drawable.cell_view)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (State == CellState.Empty)
            View.mergeDrawableStates(drawableState, intArrayOf(R.attr.state_empty))
        if (State == CellState.Filled)
            View.mergeDrawableStates(drawableState, intArrayOf(R.attr.state_filled))
        if (State == CellState.ToBeFilled)
            View.mergeDrawableStates(drawableState, intArrayOf(R.attr.state_to_be_filled))
        if (State == CellState.WasEmptied)
            View.mergeDrawableStates(drawableState, intArrayOf(R.attr.state_was_emptied))
        return drawableState
    }
}