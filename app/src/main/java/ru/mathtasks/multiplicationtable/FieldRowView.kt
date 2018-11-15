package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

enum class ProductShowType { None, Question, Number }

class FieldRowView : LinearLayout {
    private val multiplier: Int
    val multiplicand: Int
    private val tvMultiplicand: TextView
    private val cells: CellsView
    private val tvProduct: TextView
    var state: CellState = CellState.Empty
        private set

    constructor(context: Context, multiplier: Int, multiplicand: Int, dimension: Int) : super(context) {
        this.multiplier = multiplier
        this.multiplicand = multiplicand
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.HORIZONTAL

        this.tvMultiplicand = TextView(context).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            text = multiplicand.toString()
            textSize = Math.min(dimension / 5f, 20f)    // TODO
            layoutParams = ViewGroup.LayoutParams(dimension, dimension)
            setPadding(0, 0, 10, 0)
        }
        this.addView(tvMultiplicand)

        this.cells = CellsView(context, multiplier, dimension)
        this.addView(cells)

        this.tvProduct = TextView(context).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            textSize = Math.min(dimension / 5f, 20f)    // TODO
            layoutParams = ViewGroup.LayoutParams(dimension, dimension)
            setTextColor(ContextCompat.getColor(context, R.color.fieldRowViewProduct))
            setPadding(10, 0, 0, 0)
        }
        this.addView(tvProduct)
    }

    var isMultiplicandActive: Boolean = false
        set(value) {
            val color = if (value) R.color.fieldRowViewMultiplicandActive else R.color.fieldRowViewMultiplicandInactive
            tvMultiplicand.setTextColor(ContextCompat.getColor(context, color))
        }

    var productShowType: ProductShowType = ProductShowType.None
        set(value) {
            tvProduct.text = when (value) {
                ProductShowType.None -> ""
                ProductShowType.Question -> "?"
                ProductShowType.Number -> (multiplicand * multiplier).toString()
            }
        }

    fun animate(toState: CellState, type: AnimationType): Animator {
        this.state = toState
        return cells.animate(toState, type)
    }
}