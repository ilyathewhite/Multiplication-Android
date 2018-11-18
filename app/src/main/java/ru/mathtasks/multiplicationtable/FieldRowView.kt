package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView


enum class ProductShowType { None, Question, Number }

class FieldRowView : LinearLayout {
    private val multiplicand: Int
    val multiplier: Int
    private val tvMultiplier: TextView
    private val cells: CellsView
    private var product: String = ""
    private val tsProduct: TextSwitcher
    var state: CellState = CellState.Empty
        private set

    constructor(context: Context, multiplicand: Int, multiplier: Int, dimension: Int) : super(context) {
        this.multiplicand = multiplicand
        this.multiplier = multiplier
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        orientation = LinearLayout.HORIZONTAL

        this.tvMultiplier = TextView(context).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            text = multiplier.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension.toFloat() / 2)
            layoutParams = ViewGroup.LayoutParams(dimension, dimension)
            setPadding(0, 0, 10, 0)
        }
        this.addView(tvMultiplier)

        this.cells = CellsView(context, multiplicand, dimension)
        this.addView(cells)

        this.tsProduct = TextSwitcher(context).apply {
            setFactory {
                TextView(context).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                    layoutParams = FrameLayout.LayoutParams(dimension, dimension)
                    setPadding(10, 0, 0, 0)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, dimension.toFloat() / 2)
                    setTextColor(ContextCompat.getColor(context, R.color.fieldRowViewProduct))
                }
            }
            inAnimation = AnimationUtils.loadAnimation(context, R.anim.field_row_view_text_fade_in)
            outAnimation = AnimationUtils.loadAnimation(context, R.anim.field_row_view_text_fade_out)
        }
        this.addView(tsProduct)
    }

    fun animateIsMultiplierActive(value: Boolean): Animator {
        val toColor = ContextCompat.getColor(context, if (value) R.color.fieldRowViewMultiplicandActive else R.color.fieldRowViewMultiplicandInactive)
        return ValueAnimator.ofObject(ArgbEvaluator(),  tvMultiplier.currentTextColor, toColor).apply {
            this.duration = resources.getInteger(R.integer.fieldRowViewIsMultiplierActiveAnimationDuration).toLong()
            addUpdateListener { animator ->
                tvMultiplier.setTextColor(animator.animatedValue as Int)
            }
        }
    }

    fun animateProductShowType(type: ProductShowType) {
        val newText = when (type) {
            ProductShowType.None -> ""
            ProductShowType.Question -> "?"
            ProductShowType.Number -> (multiplier * multiplicand).toString()
        }
        if (newText == product)
            return
        product = newText
        tsProduct.setText(product)
    }

    fun animateCells(toState: CellState, type: AnimationType): Animator {
        this.state = toState
        return cells.animate(toState, type)
    }
}