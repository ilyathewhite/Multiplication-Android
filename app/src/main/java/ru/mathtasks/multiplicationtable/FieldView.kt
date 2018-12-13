package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.lang.Math.abs


enum class Mark { Correct, Incorrect, None }

data class FieldState(
    private val qActiveMultipliers: Int,
    val qCountedRows: Int,
    val qToBeCountedRows: Int,
    val qWasCountedRows: Int,
    private val visibleAnswers: Array<Int>,
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
        visibleAnswers.contains(row.multiplier) -> (multiplicand * row.multiplier).toString()
        questionMultiplier == row.multiplier -> "?"
        else -> ""
    }
}

class FieldView(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    companion object {
        const val Q_ROWS = 10
    }

    private lateinit var mark2iv: Map<Mark, ImageView>
    var state = FieldState(0, 0, 0, 0, arrayOf(), null)
        private set
    private var multiplicand: Int = 0
    private lateinit var rows: Array<Row>

    fun initialize(multiplicand: Int) {
        this.multiplicand = multiplicand

        this.rows = (1..Q_ROWS).map { multiplier ->
            val tvMultiplier = TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                text = multiplier.toString()
                setTextColor(ContextCompat.getColor(context, R.color.fieldViewMultiplicand))
                typeface = ResourcesCompat.getFont(context, R.font.lato_italic)
                this@FieldView.addView(this@apply)
            }

            val units = (0 until multiplicand).map {
                UnitView(context).apply {
                    this@FieldView.addView(this@apply)
                }
            }.toTypedArray()

            val tvProduct = TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
                setTextColor(ContextCompat.getColor(context, R.color.fieldViewProduct))
                typeface = ResourcesCompat.getFont(context, R.font.lato_italic)
                this@FieldView.addView(this@apply)
            }

            return@map Row(multiplier, tvMultiplier, units, tvProduct)
        }.toTypedArray()

        this.mark2iv = arrayOf(Mark.Correct, Mark.Incorrect).map { mark ->
            mark to ImageView(context).apply {
                alpha = 0f
                layoutParams = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { addRule(RelativeLayout.CENTER_IN_PARENT) }
                setBackgroundCompat(ContextCompat.getDrawable(context, if (mark == Mark.Correct) R.drawable.checkmark else R.drawable.xmark))
                this@FieldView.addView(this@apply)
            }
        }.toMap()
    }

    fun layout(width: Int, height: Int) {
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

        for (row in rows) {
            val y = topOffset + (unitSize + spacing) * (row.multiplier - 1)

            row.tvMultiplier.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize.toFloat() * resources.getFloat(R.dimen.fieldViewTextSizeRatio))
                layoutParams = RelativeLayout.LayoutParams((multiplierSpaceRatio * unitSize).toInt(), unitSize).apply { setMargins(originX + leftOffset, y, 0, 0) }
            }

            row.units.forEachIndexed { m, unit ->
                val x = Math.round(originX + unitSize * multiplierSpaceRatio + (extraSpaces / 2) * spacing + (unitSize + spacing) * m)
                unit.apply {
                    layoutParams = RelativeLayout.LayoutParams(unitSize, unitSize).apply { setMargins(x, y, 0, 0) }
                    onResize(unitSize)
                }

            }

            val productWidth = productSpaceRatio * unitSize
            val productX = originX + totalContentWidth - productWidth
            row.tvProduct.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize.toFloat() * resources.getFloat(R.dimen.fieldViewTextSizeRatio))
                layoutParams = RelativeLayout.LayoutParams(productWidth.toInt(), unitSize).apply { setMargins(productX.toInt(), y, 0, 0) }
            }
        }
    }

    fun animateMark(mark: Mark, duration: Long): List<Animator> {
        return mark2iv
            .filter { (ivMark, iv) -> iv.alpha != if (ivMark == mark) 1f else 0f }
            .map { (ivMark, iv) -> iv.alphaAnimator(if (ivMark == mark) 1f else 0f, duration) }
    }

    fun setFieldState(state: FieldState) {
        this.state = state
        rows.forEach { row ->
            row.setIsMultiplierActive(state.isMultiplierActive(row))
            row.setUnitState(state.unitState(row))
            row.setText(state.text(row, multiplicand))
        }
    }

    fun animateFieldState(state: FieldState, unitAnimation: UnitAnimation?, duration: Long): List<Animator> {
        val animators = arrayListOf<Animator?>()
        rows.forEach { row ->
            animators.add(row.animateIsMultiplierActive(state.isMultiplierActive(row), duration))
            animators.add(row.animateText(state.text(row, multiplicand), duration))
        }
        if (unitAnimation != null && this.state.qCountedRows != state.qCountedRows) {
            val from = this.state.qCountedRows
            val to = state.qCountedRows
            animators.add(AnimatorSet().apply {
                playSequentially(
                    IntProgression.fromClosedRange(from, to, if (from < to) 1 else -1).map { idx ->
                        rows[idx].animateUnitState(state.unitState(rows[idx]), unitAnimation, to < from, duration / abs(to - from))
                    }
                )
            })
        }
        this.state = state
        return animators.filterNotNull()
    }
}