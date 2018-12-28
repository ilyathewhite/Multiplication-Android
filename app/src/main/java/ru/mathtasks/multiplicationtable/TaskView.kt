package ru.mathtasks.multiplicationtable

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.TransitionSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.AccelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.task_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async


class TaskView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private var nextTvMultipliers = listOf<TextView>()
    private var nextMultiplierIdx = 0

    init {
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
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                typeface = ResourcesCompat.getFont(context, R.font.lato_blackitalic)
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                maxLines = 1
                id = ViewCompat.generateViewId()
                layoutNextMultiplier(constraintSet, this@apply.id, prevTv?.id)
                this@TaskView.clTask.addView(this@apply)
                prevTv = this@apply
            }
        }.toList()
        constraintSet.applyTo(clTask)
    }

    private fun layoutNextMultiplier(constraintSet: ConstraintSet, id: Int, prevId: Int?) {
        constraintSet.clear(id)
        constraintSet.connect(id, ConstraintSet.BOTTOM, this.tvMultiplier.id, ConstraintSet.TOP)
        constraintSet.setMargin(id, ConstraintSet.BOTTOM, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt())
        constraintSet.constrainWidth(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        constraintSet.constrainHeight(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        if (prevId == null) {
            constraintSet.connect(id, ConstraintSet.LEFT, this.tvMultiplier.id, ConstraintSet.LEFT)
            constraintSet.setMargin(id, ConstraintSet.LEFT, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7f, resources.displayMetrics).toInt())
            constraintSet.connect(id, ConstraintSet.RIGHT, this.tvMultiplier.id, ConstraintSet.RIGHT)
        } else {
            constraintSet.connect(id, ConstraintSet.LEFT, prevId, ConstraintSet.RIGHT)
            constraintSet.setMargin(id, ConstraintSet.LEFT, resources.getDimensionPixelSize(R.dimen.taskViewNextMultiplierInterval))
        }
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
            layoutNextMultiplier(this, tvMovingNextMultiplier.id, null)
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
                        if (tvNextNextMultiplier != null)
                            layoutNextMultiplier(this, tvNextNextMultiplier.id, null)
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