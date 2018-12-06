package ru.mathtasks.multiplicationtable

import android.animation.*
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


class TaskView : LinearLayout {
    private lateinit var rlOuter: RelativeLayout
    private lateinit var tvMultiplier: TextView
    private lateinit var tvMultiplicand: TextView
    private lateinit var tvAnswer: TextView
    private lateinit var llNextMultipliers: LinearLayout
    private lateinit var tvMovingNextMultiplier: TextView
    private var nextTvMultipliers = mutableListOf<TextView>()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.task_view, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        this.rlOuter = findViewById(R.id.rl_outer)
        this.tvMultiplier = findViewById(R.id.tv_multiplier)
        this.tvMultiplicand = findViewById(R.id.tv_multiplicand)
        this.tvAnswer = findViewById(R.id.tv_answer)
        this.llNextMultipliers = findViewById(R.id.ll_nextMultipliers)
        this.tvMovingNextMultiplier = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
            alpha = 0f
            setTypeface(null, Typeface.ITALIC)
            rlOuter.addView(this)
        }
    }

    fun createNextMultipliers(nextMultipliers: Array<Int>) {
        llNextMultipliers.removeAllViews()
        this.nextTvMultipliers = nextMultipliers.map { nextMultiplier ->
            TextView(context).apply {
                text = nextMultiplier.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                alpha = resources.getFloat(R.dimen.taskViewNextMultiplierAlpha)
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, resources.getDimensionPixelSize(R.dimen.taskViewNextMultiplierInterval), 0)
                }
                llNextMultipliers.addView(this)
            }
        }.toMutableList()
        llNextMultipliers.post {
            val leftMargin = tvMultiplier.left + tvMultiplier.width / 2 - nextTvMultipliers[0].width / 2
            llNextMultipliers.layoutParams = RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(leftMargin, 0, 0, 0)
            }
            for (tv in nextTvMultipliers)
                tv.alpha = if (tv.right + leftMargin > llNextMultipliers.width) 0f else 1f
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

    class NextTaskAnimations(val prepareAnimation: Animator?, val movingAnimation: Animator?)

    fun animateNextTask(prepareDuration: Long, movingDuration: Long): NextTaskAnimations {
        val tvNextMultiplier = nextTvMultipliers[0]
        val tvNextMultiplierLoc = tvNextMultiplier.getLocationOnScreen()
        val rlOuterLoc = rlOuter.getLocationOnScreen()
        val tvMovingNextMultiplierStartY = tvNextMultiplierLoc.Y - rlOuterLoc.Y
        val tvMovingNextMultiplierStartRight = rlOuter.width - (tvNextMultiplierLoc.X - rlOuterLoc.X) - tvNextMultiplier.width

        tvMovingNextMultiplier.apply {
            text = tvNextMultiplier.text
            layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                setMargins(0, tvMovingNextMultiplierStartY, tvMovingNextMultiplierStartRight, 0)
            }
        }

        val prepareAnimation = arrayListOf(
            tvNextMultiplier.alphaAnimation(prepareDuration, 0f),
            tvMovingNextMultiplier.alphaAnimation(prepareDuration, resources.getFloat(R.dimen.taskViewNextMultiplierAlpha)),
            tvMultiplier.alphaAnimation(prepareDuration, 0f),
            tvAnswer.alphaAnimation(prepareDuration, 0f)
        ).merge()

        val tvMovingNextMultiplierStartTextSize = tvNextMultiplier.textSize
        val tvMovingNextMultiplierEndTextSize = tvMultiplier.textSize
        val tvMultiplierLoc = tvMultiplier.getLocationOnScreen()
        val tvMovingNextMultiplierEndY = tvMultiplierLoc.Y - rlOuterLoc.Y
        val tvMovingNextMultiplierEndRight = rlOuter.width - (tvMultiplierLoc.X - rlOuterLoc.X) - tvMultiplier.width

        val tvNextNextMultiplier = if (nextTvMultipliers.size > 1) nextTvMultipliers[1] else null
        val tvNextNextMultiplierStartX = (tvNextMultiplier.layoutParams as LinearLayout.LayoutParams).leftMargin
        val tvNextNextMultiplierEndX = if (tvNextNextMultiplier != null) tvNextNextMultiplierStartX + tvNextMultiplierLoc.X - tvNextNextMultiplier.getLocationOnScreen().X else null

        val movingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = movingDuration

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
                    tvNextMultiplier.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(
                            (tvNextNextMultiplierStartX + (tvNextNextMultiplierEndX!! - tvNextNextMultiplierStartX) * progress).toInt(), 0,
                            (tvNextMultiplier.layoutParams as LinearLayout.LayoutParams).rightMargin, 0
                        )
                    }
                }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    tvMovingNextMultiplier.alpha = 0f
                    if (nextTvMultipliers.size > 1)  // don't remove last as it will shut ll_nextMultipliers
                        findViewById<LinearLayout>(R.id.ll_nextMultipliers).removeView(tvNextMultiplier)
                    else
                        findViewById<LinearLayout>(R.id.ll_nextMultipliers).alpha = 0f
                    nextTvMultipliers.removeAt(0)
                    tvMultiplier.alpha = 1f
                    tvAnswer.alpha = 1f
                }
            })
        }

        return NextTaskAnimations(prepareAnimation, movingAnimator)
    }
}