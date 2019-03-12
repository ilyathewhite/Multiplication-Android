package ru.mathtasks.multiplicationtable

import android.animation.Animator
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

data class RowsState(val qCountedRows: Int, val qToBeCountedRows: Int, val qWasCountedRows: Int) {
    fun unitState(row: Row) = when {
        row.multiplier <= qCountedRows -> UnitState.Counted
        row.multiplier <= qCountedRows + qToBeCountedRows -> UnitState.ToBeCounted
        row.multiplier <= qCountedRows + qWasCountedRows -> UnitState.WasCounted
        else -> UnitState.Disabled
    }
}

class FieldView(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {
    companion object {
        const val Q_ROWS = 10
    }

    private var initialized = false
    private var layoutReady = false
    private lateinit var mark2iv: Map<Mark, ImageView>
    private var multiplicand: Int = 0
    private lateinit var rows: Array<Row>

    fun initialize(multiplicand: Int) {
        this.initialized = true
        this.multiplicand = multiplicand

        this.rows = (1..Q_ROWS).map { multiplier ->
            val tvMultiplier = TextView(context).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                text = multiplier.toString()
                setTextColor(ContextCompat.getColor(context, R.color.fieldViewMultiplicand))
                typeface = ResourcesCompat.getFont(context, R.font.lato_regular)
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
                typeface = ResourcesCompat.getFont(context, R.font.lato_regular)
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

        if(layoutReady)
            layout()
    }

    fun layout() {
        layoutReady = true
        if (!initialized)
            return

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
            row.tvText.apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, unitSize.toFloat() * resources.getFloat(R.dimen.fieldViewTextSizeRatio))
                layoutParams = RelativeLayout.LayoutParams(productWidth.toInt(), unitSize).apply { setMargins(productX.toInt(), y, 0, 0) }
            }
        }
    }

    fun clearMark() {
        mark2iv.forEach { (_, iv) -> iv.alpha = 0f }
    }

    fun animateMark(mark: Mark, duration: Long): List<Animator> {
        return mark2iv
            .filter { (ivMark, iv) -> iv.alpha != if (ivMark == mark) 1f else 0f }
            .map { (ivMark, iv) -> iv.alphaAnimator(if (ivMark == mark) 1f else 0f, duration) }
    }

    fun setLastActiveMultiplier(lastActiveMultiplier: Int) {
        rows.forEach { row -> row.setIsMultiplierActive(row.multiplier <= lastActiveMultiplier) }
    }

    fun animateLastActiveMultiplier(lastActiveMultiplier: Int, duration: Long): List<Animator> {
        return rows.mapNotNull { row -> row.animateIsMultiplierActive(row.multiplier <= lastActiveMultiplier, duration) }
    }

    private fun rowText(row: Row, visibleAnswers: Iterable<Int>, questionMultiplier: Int?) = when {
        visibleAnswers.contains(row.multiplier) -> (multiplicand * row.multiplier).toString()
        questionMultiplier == row.multiplier -> "?"
        else -> ""
    }

    fun setRowText(visibleAnswers: Iterable<Int>, questionMultiplier: Int?) {
        rows.forEach { row -> row.setText(rowText(row, visibleAnswers, questionMultiplier)) }
    }

    fun animateRowText(visibleAnswers: Iterable<Int>, questionMultiplier: Int?, duration: Long): List<Animator> {
        return rows.mapNotNull { row -> row.animateText(rowText(row, visibleAnswers, questionMultiplier), duration) }
    }

    fun setRowState(rowsState: RowsState) {
        rows.forEach { row -> row.setUnitState(rowsState.unitState(row)) }
    }

    fun crossFadeRowState(rowsState: RowsState, duration: Long): Animator {
        return rows.map { row -> row.crossFadeUnitState(rowsState.unitState(row), duration) }.playTogether()
    }

    fun animateCountedRows(fromState: RowsState, toState: RowsState, unitAnimation: UnitAnimation, duration: Long): Animator {
        val from = fromState.qCountedRows
        val to = toState.qCountedRows
        return (if (from < to) (from..to - 1) else (from - 1 downTo to)).map { idx ->
            rows[idx].animateUnitState(toState.unitState(rows[idx]), unitAnimation, from > to, duration / abs(to - from))
        }.playSequentially()
    }

    suspend fun pulseRowText(multiplier: Int, scale: Float, duration: Long) {
        rows[multiplier - 1].pulseRowText(scale, duration)
    }
}