package ru.mathtasks.multiplicationtable

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.task_view.view.*

class TaskView : LinearLayout {
    private val tvMovingNextMultiplier: TextView
    private var nextTvMultipliers = listOf<TextView>()
    private var nextMultiplierIdx = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.task_view, this)

        this.tvMovingNextMultiplier = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
            setTextColor(ContextCompat.getColor(context, R.color.taskViewTask))
            alpha = 0f
            setTypeface(null, Typeface.ITALIC)
            this@TaskView.rlOuter.addView(this)
        }
    }

    fun createNextMultipliers(nextMultipliers: Array<Int>) {
        nextMultiplierIdx = 0
        llNextMultipliers.removeAllViews()
        this.nextTvMultipliers = nextMultipliers.map { nextMultiplier ->
            TextView(context).apply {
                text = nextMultiplier.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, resources.getDimensionPixelSize(R.dimen.taskViewNextMultiplierInterval), 0)
                }
                this@TaskView.llNextMultipliers.addView(this@apply)
            }
        }.toList()
        llNextMultipliers.post {
            llNextMultipliers.layoutParams = RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(tvMultiplier.left + tvMultiplier.width / 2 - nextTvMultipliers[0].width / 2, 0, 0, 0)
            }
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

    class Animations(val prepare: suspend () -> Unit, val move: suspend () -> Unit)
    suspend fun animateNextTask(prepareDuration: Long, moveDuration: Long): Animations {
        val tvNextMultiplier = nextTvMultipliers[nextMultiplierIdx]
        nextMultiplierIdx++
        val tvNextMultiplierLoc = tvNextMultiplier.getLocationOnScreen()
        val rlOuterLoc = rlOuter.getLocationOnScreen()
        val tvMovingNextMultiplierStartY = tvNextMultiplierLoc.Y - rlOuterLoc.Y
        val tvMovingNextMultiplierStartRight = rlOuter.width - (tvNextMultiplierLoc.X - rlOuterLoc.X) - tvNextMultiplier.width

        val tvMovingNextMultiplierStartTextSize = tvNextMultiplier.textSize
        val tvMovingNextMultiplierEndTextSize = tvMultiplier.textSize
        val tvMultiplierLoc = tvMultiplier.getLocationOnScreen()
        val tvMovingNextMultiplierEndY = tvMultiplierLoc.Y - rlOuterLoc.Y
        val tvMovingNextMultiplierEndRight = rlOuter.width - (tvMultiplierLoc.X - rlOuterLoc.X) - tvMultiplier.width

        val tvNextNextMultiplier = if (nextMultiplierIdx < nextTvMultipliers.size) nextTvMultipliers[nextMultiplierIdx] else null
        val tvNextMultiplier0StartLeftMargin = (nextTvMultipliers[0].layoutParams as LinearLayout.LayoutParams).leftMargin
        var tvNextMultiplier0LeftMarginShift = 0

        return Animations({
            tvMovingNextMultiplier.apply {
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                text = tvNextMultiplier.text
                layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    setMargins(0, tvMovingNextMultiplierStartY, tvMovingNextMultiplierStartRight, 0)
                }
            }

            listOf(
                tvMultiplier.alphaAnimator(0f, prepareDuration),
                tvAnswer.alphaAnimator(0f, prepareDuration),
                tvNextMultiplier.alphaAnimator(0f, prepareDuration),
                tvMovingNextMultiplier.alphaAnimator(1f, prepareDuration)
            ).merge().run()

            tvMultiplier.text = tvNextMultiplier.text
            if (tvNextNextMultiplier != null) {
                tvMultiplier.post {
                    tvNextMultiplier0LeftMarginShift = (tvNextNextMultiplier.getLocationOnScreen().X + tvNextNextMultiplier.width / 2) -
                            (tvNextMultiplier.getLocationOnScreen().X + tvNextMultiplier.width / 2)
                }
            }
        }, {
            listOf(
                tvMovingNextMultiplier.textColorAnimator(ContextCompat.getColor(context, R.color.taskViewTask), moveDuration),
                ValueAnimator.ofFloat(0f, 1f).apply {
                    this.duration = moveDuration

                    addUpdateListener { animator ->
                        val progress = animator.animatedValue as Float
                        tvMovingNextMultiplier.setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            tvMovingNextMultiplierStartTextSize + (tvMovingNextMultiplierEndTextSize - tvMovingNextMultiplierStartTextSize) * progress
                        )
                        tvMovingNextMultiplier.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                            setMargins(
                                0,
                                (tvMovingNextMultiplierStartY + (tvMovingNextMultiplierEndY - tvMovingNextMultiplierStartY) * progress).toInt(),
                                (tvMovingNextMultiplierStartRight + (tvMovingNextMultiplierEndRight - tvMovingNextMultiplierStartRight) * progress).toInt(),
                                0
                            )
                        }
                        if (tvNextNextMultiplier != null) {
                            nextTvMultipliers[0].layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(
                                    (tvNextMultiplier0StartLeftMargin - tvNextMultiplier0LeftMarginShift * progress).toInt(), 0,
                                    (nextTvMultipliers[0].layoutParams as LinearLayout.LayoutParams).rightMargin, 0
                                )
                            }
                        }
                    }
                }
            ).run()

            tvMovingNextMultiplier.alpha = 0f
            tvMultiplier.alpha = 1f
            tvAnswer.alpha = 1f
        })
    }
}