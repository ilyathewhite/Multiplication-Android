package ru.mathtasks.multiplicationtable

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.TransitionSet
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.task_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async


class TaskView : LinearLayout {
    private var nextTvMultipliers = listOf<TextView>()
    private var nextMultiplierIdx = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        inflate(context, R.layout.task_view, this)
    }

    fun createNextMultipliers(nextMultipliers: Array<Int>) {
        nextMultiplierIdx = 0
        nextTvMultipliers.forEach { clTask.removeView(it) }
        val constraintSet = ConstraintSet().apply { clone(clTask) }
        var prevTv: TextView? = null
        this.nextTvMultipliers = nextMultipliers.map { nextMultiplier ->
            TextView(context).apply {
                text = nextMultiplier.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                typeface = this@TaskView.tvAnswer.typeface
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                maxLines = 1
                id = ViewCompat.generateViewId()
                constraintSet.connect(this@apply.id, ConstraintSet.BOTTOM, this@TaskView.tvMultiplier.id, ConstraintSet.TOP)
                constraintSet.constrainWidth(this@apply.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                constraintSet.constrainHeight(this@apply.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                if (prevTv == null) {
                    constraintSet.connect(this@apply.id, ConstraintSet.LEFT, this@TaskView.tvMultiplier.id, ConstraintSet.LEFT)
                    constraintSet.connect(this@apply.id, ConstraintSet.RIGHT, this@TaskView.tvMultiplier.id, ConstraintSet.RIGHT)
                } else {
                    constraintSet.connect(this@apply.id, ConstraintSet.LEFT, prevTv!!.id, ConstraintSet.RIGHT)
                    constraintSet.setMargin(this@apply.id, ConstraintSet.LEFT, resources.getDimensionPixelSize(R.dimen.taskViewNextMultiplierInterval))
                }
                this@TaskView.clTask.addView(this@apply)
                prevTv = this@apply
            }
        }.toList()
        constraintSet.applyTo(clTask)
    }

    fun setMultiplier(multiplier: Int) {
        tvMultiplier.text = multiplier.toString()
    }

    fun setMultiplicand(multiplicand: Int) {
        tvMultiplicand.text = multiplicand.toString()
    }

    fun setAnswer(answer: Int?) {
        tvAnswer.text = answer?.toString() ?: ""
    }

    suspend fun prepareNextTask(duration: Long) {
        val tvNextMultiplier = nextTvMultipliers[nextMultiplierIdx]
        nextMultiplierIdx++

        tvMovingNextMultiplier.apply {
            setTextColor(tvNextMultiplier.currentTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, tvNextMultiplier.textSize)
            text = tvNextMultiplier.text
        }
        ConstraintSet().apply {
            clone(clTask)
            clear(tvMovingNextMultiplier.id)
            constrainWidth(tvMovingNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            constrainHeight(tvMovingNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            connect(tvMovingNextMultiplier.id, ConstraintSet.BOTTOM, tvMultiplier.id, ConstraintSet.TOP)
            connect(tvMovingNextMultiplier.id, ConstraintSet.LEFT, tvMultiplier.id, ConstraintSet.LEFT)
            connect(tvMovingNextMultiplier.id, ConstraintSet.RIGHT, tvMultiplier.id, ConstraintSet.RIGHT)
            applyTo(clTask)
        }

        listOf(
            tvMultiplier.alphaAnimator(0f, duration),
            tvAnswer.alphaAnimator(0f, duration),
            tvNextMultiplier.alphaAnimator(0f, duration),
            tvMovingNextMultiplier.alphaAnimator(1f, duration)
        ).playTogether().run()

        tvMultiplier.text = tvNextMultiplier.text
    }

    suspend fun moveNextTask(scope: CoroutineScope, duration: Long) {
        val tvNextNextMultiplier = if (nextMultiplierIdx < nextTvMultipliers.size) nextTvMultipliers[nextMultiplierIdx] else null

        listOf(
            scope.async {
                tvMovingNextMultiplier.textColorAnimator(ContextCompat.getColor(context, R.color.taskViewTask), duration).run()
            },
            scope.async {
                clTask.transition(duration, TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(ChangeBounds()).addTransition(ScaleTransition())
                    interpolator = AccelerateInterpolator(1.0f)
                }) {
                    ConstraintSet().apply {
                        clone(clTask)
                        clear(tvMovingNextMultiplier.id)
                        constrainWidth(tvMovingNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                        constrainHeight(tvMovingNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                        connect(tvMovingNextMultiplier.id, ConstraintSet.TOP, tvMultiplier.id, ConstraintSet.TOP)
                        connect(tvMovingNextMultiplier.id, ConstraintSet.BOTTOM, tvMultiplier.id, ConstraintSet.BOTTOM)
                        connect(tvMovingNextMultiplier.id, ConstraintSet.LEFT, tvMultiplier.id, ConstraintSet.LEFT)
                        connect(tvMovingNextMultiplier.id, ConstraintSet.RIGHT, tvMultiplier.id, ConstraintSet.RIGHT)
                        if (tvNextNextMultiplier != null) {
                            clear(tvNextNextMultiplier.id)
                            constrainWidth(tvNextNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                            constrainHeight(tvNextNextMultiplier.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                            connect(tvNextNextMultiplier.id, ConstraintSet.BOTTOM, tvMultiplier.id, ConstraintSet.TOP)
                            connect(tvNextNextMultiplier.id, ConstraintSet.LEFT, tvMultiplier.id, ConstraintSet.LEFT)
                            connect(tvNextNextMultiplier.id, ConstraintSet.RIGHT, tvMultiplier.id, ConstraintSet.RIGHT)
                        }
                        applyTo(clTask)
                    }
                    tvMovingNextMultiplier.scaleX = tvMultiplier.textSize / tvMovingNextMultiplier.textSize
                    tvMovingNextMultiplier.scaleY = tvMultiplier.textSize / tvMovingNextMultiplier.textSize
                }
            }
        ).map { it.await() }

        tvMovingNextMultiplier.alpha = 0f
        tvMovingNextMultiplier.scaleX = 1f
        tvMovingNextMultiplier.scaleY = 1f
        tvMultiplier.alpha = 1f
        tvAnswer.alpha = 1f
    }
}