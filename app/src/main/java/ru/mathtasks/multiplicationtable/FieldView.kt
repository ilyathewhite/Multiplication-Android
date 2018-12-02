package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.lang.Math.*

enum class Mark { Correct, Incorrect, None }

data class FieldState(
    val mark: Mark,
    private val qActiveMultipliers: Int,
    val qCountedRows: Int,
    val qToBeCountedRows: Int,
    val qWasCountedRows: Int,
    private val hintMultipliers: Array<Int>,
    private val questionMultiplier: Int?
) {

    fun isMultiplierActive(row: Row) = row.multiplier <= qActiveMultipliers

    fun unitState(row: Row) = when {
        row.multiplier <= qCountedRows -> UnitState.Counted
        row.multiplier <= qCountedRows + qToBeCountedRows -> UnitState.ToBeCounted
        row.multiplier <= qCountedRows + qWasCountedRows -> UnitState.WasCounted
        else -> UnitState.Disabled
    }

    fun text(row: Row, multiplicand: Int) = when {
        hintMultipliers.contains(row.multiplier) -> (multiplicand * row.multiplier).toString()
        questionMultiplier == row.multiplier -> "?"
        else -> ""
    }
}

class FieldView(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    companion object {
        const val Q_ROWS = 10
    }

    private lateinit var mark2iv: Map<Mark, ImageView>
    var state = FieldState(Mark.None, 0, 0, 0, 0, arrayOf(), null)
        private set
    private var multiplicand: Int = 0
    private lateinit var rows: Array<Row>

    fun initialize(multiplicand: Int, width: Int, height: Int) {
        this.multiplicand = multiplicand

        val leftOffset = resources.getDimensionPixelSize(R.dimen.fieldViewLeftOffset)
        val rightOffset = resources.getDimensionPixelSize(R.dimen.fieldViewRightOffset)
        val topOffset = resources.getDimensionPixelSize(R.dimen.fieldViewTopOffset)
        val bottomOffset = resources.getDimensionPixelSize(R.dimen.fieldViewBottomOffset)
        val multiplierSpaceRatio = resources.getFloat(R.dimen.fieldViewMultiplierSpaceRatio)
        val productSpaceRatio = resources.getFloat(R.dimen.fieldViewProductSpaceRatio)
        val totalColumnsSpaceRatio = multiplicand + multiplierSpaceRatio + productSpaceRatio
        val extraSpaces = 4         // before and after multiplier, before and after product
        val spacingRatio = resources.getFloat(R.dimen.fieldViewSpacingRatio)

        val unitSizeFromHeight = (height.toFloat() - topOffset - bottomOffset) / ((Q_ROWS - 1) * spacingRatio + Q_ROWS)
        val unitSizeFromWidth = (width.toFloat() - leftOffset - rightOffset) / ((multiplicand - 1 + extraSpaces) * spacingRatio + totalColumnsSpaceRatio)
        val unitSize = Math.floor(Math.min(unitSizeFromHeight, unitSizeFromWidth).toDouble()).toInt()
        val spacing = Math.round(spacingRatio * unitSize)

        val totalContentWidth = totalColumnsSpaceRatio * unitSize + (multiplicand - 1 + extraSpaces) * spacing
        val originX = Math.round((width - totalContentWidth) / 2.0).toInt()

        this.rows = (1..Q_ROWS).map { multiplier ->
            val y = topOffset + (unitSize + spacing) * (multiplier - 1)

            val tvMultiplier = TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                text = multiplier.toString()
                setTextColor(ContextCompat.getColor(context, R.color.fieldViewMultiplicand))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize.toFloat() * resources.getFloat(R.dimen.fieldViewTextSizeRatio))
                layoutParams = RelativeLayout.LayoutParams((multiplierSpaceRatio * unitSize).toInt(), unitSize).apply { setMargins(originX + leftOffset, y, 0, 0) }
                this@FieldView.addView(this@apply)
            }

            val units = (0 until multiplicand).map { m ->
                val x = Math.round(originX + unitSize * multiplierSpaceRatio + (extraSpaces / 2) * spacing + (unitSize + spacing) * m)
                UnitView(context, unitSize).apply {
                    layoutParams = RelativeLayout.LayoutParams(unitSize, unitSize).apply { setMargins(x, y, 0, 0) }
                    this@FieldView.addView(this@apply)
                }
            }.toTypedArray()

            val productWidth = productSpaceRatio * unitSize
            val productX = originX + totalContentWidth - productWidth
            val tvProduct = TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                layoutParams = RelativeLayout.LayoutParams(productWidth.toInt(), unitSize).apply { setMargins(productX.toInt(), y, 0, 0) }
                setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize.toFloat() * resources.getFloat(R.dimen.fieldViewTextSizeRatio))
                setTextColor(ContextCompat.getColor(context, R.color.rowViewProduct))
                this@FieldView.addView(this@apply)
            }

            return@map Row(multiplier, tvMultiplier, units, tvProduct)
        }.toTypedArray()

        this.mark2iv = arrayOf(Mark.Correct, Mark.Incorrect).map { mark ->
            mark to ImageView(context).apply {
                gravity = Gravity.CENTER
                //visibility = View.GONE
                alpha = 0.2f
                layoutParams = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                setBackgroundCompat(ContextCompat.getDrawable(context, if (mark == Mark.Correct) R.drawable.checkmark else R.drawable.xmark))
                this@FieldView.addView(this@apply)
            }
        }.toMap()
    }

    private fun setMark(mark: Mark) {
        mark2iv.map { (mark, iv) -> iv.alpha = if (mark == state.mark) 1f else 0.2f }
    }

    private fun animateMark(mark: Mark, duration: Long): Animator? {
        val animators = mark2iv.map { (ivMark, iv) ->
            val aim = if (ivMark == mark) 1f else 0f
            if (iv.alpha == aim) null else ObjectAnimator.ofFloat(iv, View.ALPHA, iv.alpha, aim).apply { setDuration(duration) }
        }.filter { a -> a != null }
        return when {
            animators.isEmpty() -> null
            animators.size == 1 -> animators[0]
            else -> AnimatorSet().apply { playTogether(animators) }
        }
    }

    fun setFieldState(state: FieldState) {
        this.state = state
        setMark(state.mark)
        rows.map { row ->
            row.setIsMultiplierActive(state.isMultiplierActive(row))
            row.setUnitState(state.unitState(row))
            row.setText(state.text(row, multiplicand))
        }
    }

    fun animateFieldState(state: FieldState, unitAnimation: UnitAnimation?, duration: Long): Animator {
        val animators = arrayListOf<Animator?>()
        animators.add(animateMark(state.mark, duration))
        rows.map { row ->
            animators.add(row.animateIsMultiplierActive(state.isMultiplierActive(row), duration))
            animators.add(row.animateText(state.text(row, multiplicand), duration))
        }
        val ranges = arrayOf(
            Pair(this.state.qCountedRows, state.qCountedRows),
            Pair(this.state.qToBeCountedRows, state.qToBeCountedRows),
            Pair(this.state.qWasCountedRows, state.qWasCountedRows)
        )
        if(unitAnimation!=null) {
            for (range in ranges) {
                for (idx in min(range.first, range.second) until max(range.first, range.second))
                    animators.add(rows[idx].animateUnitState(state.unitState(rows[idx]), unitAnimation, range.first > range.second, duration / abs(range.second - range.first)))
            }
        }
        this.state = state
        return AnimatorSet().apply { playTogether(animators.filter { a -> a != null }) }
    }
}