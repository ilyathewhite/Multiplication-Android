package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.TransitionSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.MenuItem
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Parcelize
class TestActivityParams(val multiplicands: Array<Int>) : Parcelable

@Parcelize
class Task(val multiplicand: Int, val multiplier: Int) : Parcelable

@Parcelize
class TestActivityState(
    val multiplicands: Array<Int>,
    val tasks: List<Task>,
    val taskIdx2Correct: ArrayList<Boolean>,
    var answer: Int? = null
) : Parcelable

sealed class TestActivityUpdate {
    object VisibleAnswer : TestActivityUpdate()
    object InvisibleAnswer : TestActivityUpdate()
    class Attempt(val correct: Boolean) : TestActivityUpdate()
    class EndOfTest(val correct: Boolean) : TestActivityUpdate()
}

class TestActivityViewModel : MyViewModel<TestActivityParams, TestActivityState, TestActivityUpdate>() {
    private var answerFrozen = false

    val multiplicands get() = s.multiplicands
    val tasks get() = s.tasks
    val taskIdx2Correct get() = s.taskIdx2Correct
    val task get() = s.tasks[s.taskIdx2Correct.size]
    val progress get() = taskIdx2Correct.size.toFloat() / tasks.size
    val answer get() = s.answer

    override fun create(params: TestActivityParams) = TestActivityState(
        params.multiplicands,
        params.multiplicands.flatMap { multiplicand -> (1..10).map { multiplier -> Task(multiplicand, multiplier) } }.shuffled().toList(),
        arrayListOf()
    )

    fun onAnswerChanged(answer: Int?) {
        s.answer = answer
        if (!answerFrozen)
            update(TestActivityUpdate.VisibleAnswer)
    }

    fun onPreAnimationEnded() {
        answerFrozen = false
        update(TestActivityUpdate.VisibleAnswer)
    }

    fun onOkPressed(answer: Int) {
        if (answerFrozen)
            return
        s.answer = null
        update(TestActivityUpdate.InvisibleAnswer)
        answerFrozen = true
        val correct = (task.multiplicand * task.multiplier == answer)
        s.taskIdx2Correct.add(correct)
        if (s.taskIdx2Correct.size < s.tasks.size)
            update(TestActivityUpdate.Attempt(correct))
        else
            update(TestActivityUpdate.EndOfTest(correct))
    }
}

class TestActivity : ScopedAppActivity() {
    private lateinit var m: TestActivityViewModel

    private var nextTvMultipliers = mutableListOf<TextView>()
    private var nextTvMultiplicands = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        fullScreen()

        m = getViewModel(this, savedInstanceState)

        tbToolbarTitle.text = "Test" + " \u25A1 \u00D7 " + (if (m.multiplicands.count() == 1) m.multiplicands.first().toString() else "\u25A1")
        setProgressText()

        if (m.taskIdx2Correct.size + 1 < m.tasks.size) {
            val constraintSet = ConstraintSet().apply { clone(clMain) }
            this.nextTvMultipliers = nextTvFactors(constraintSet, m.tasks.drop(m.taskIdx2Correct.size + 1).map { it.multiplier }, tvMultiplier.id)
            if (isManyMultiplicands)
                this.nextTvMultiplicands = nextTvFactors(constraintSet, m.tasks.drop(m.taskIdx2Correct.size + 1).map { it.multiplicand }, tvMultiplicand.id)
            constraintSet.applyTo(clMain)
        }

        val input = fInput as InputFragment
        input.setListener(object : InputFragment.EventListener {
            override fun onAnswerChanged(answer: Int?) {
                m.onAnswerChanged(answer)
            }

            override fun onOkPressed(answer: Int) {
                m.onOkPressed(answer)
            }
        })
        input.answer = m.answer
        tvAnswer.text = m.answer?.toString() ?: ""

        m.observe(this) { u ->
            val o = when (u) {
                TestActivityUpdate.VisibleAnswer -> tvAnswer.text = m.answer?.toString() ?: ""
                TestActivityUpdate.InvisibleAnswer -> input.answer = m.answer
                is TestActivityUpdate.Attempt -> attempt(u.correct)
                is TestActivityUpdate.EndOfTest -> endOfGame(u.correct)
            }
        }
    }

    // region init
    private val isManyMultiplicands get() = m.multiplicands.size > 1

    private fun nextTvFactors(constraintSet: ConstraintSet, factors: List<Int>, onTopOfId: Int): MutableList<TextView> {
        var prevTv: TextView? = null
        return factors.map { factor ->
            TextView(this).apply tv@{
                text = factor.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.testActivityNextFactorFontSize))
                typeface = ResourcesCompat.getFont(context, R.font.lato_blackitalic)
                setTextColor(ContextCompat.getColor(context, R.color.testActivityNextFactor))
                maxLines = 1
                id = ViewCompat.generateViewId()
                layoutNextFactor(constraintSet, this@tv.id, prevTv?.id, onTopOfId)
                this@TestActivity.clMain.addView(this@tv, 0)
                prevTv = this@tv
            }
        }.toMutableList()
    }

    private fun layoutNextFactor(constraintSet: ConstraintSet, id: Int, prevId: Int?, onTopOfId: Int) {
        constraintSet.apply {
            clear(id)
            connect(id, ConstraintSet.LEFT, onTopOfId, ConstraintSet.LEFT)
            connect(id, ConstraintSet.RIGHT, onTopOfId, ConstraintSet.RIGHT)
            constrainWidth(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            constrainHeight(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            if (prevId == null) {
                connect(id, ConstraintSet.BOTTOM, onTopOfId, ConstraintSet.TOP)
                setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityFirstNextMultiplierBottomMargin))
            } else {
                connect(id, ConstraintSet.BOTTOM, prevId, ConstraintSet.TOP)
                setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityNextMultiplierBottomMargin))
            }
        }
    }
    // endregion

    private fun ivMark(correct: Boolean) = if (correct) ivCheckmark else ivXmark

    // region attempt
    private fun attempt(correct: Boolean) {
        launch {
            showMarkAndProgress(correct)

            listOf(
                ivMark(correct).alphaAnimator(0f, Settings.TestActivityShowMarkDuration),
                tvMultiplier.alphaAnimator(0f, Settings.TestActivityShowMarkDuration),
                tvMultiplicand.alphaAnimator(0f, Settings.TestActivityShowMarkDuration),
                tvAnswer.alphaAnimator(0f, Settings.TestActivityShowMarkDuration)
            ).playTogether().run()

            m.onPreAnimationEnded()
            setProgressText()

            prepareNextTask(Settings.TestActivityHideMarkDuration)
            moveNextTask(Settings.TestActivityMoveTaskDuration)
        }
    }

    private suspend fun prepareNextTask(duration: Long) {
        class Data(val tvNext: TextView, val tvMoving: TextView, val onTopOfTv: TextView)

        val ds = mutableListOf(Data(nextTvMultipliers[0], tvMovingNextMultiplier, tvMultiplier))
        if (isManyMultiplicands)
            ds.add(Data(nextTvMultiplicands[0], tvMovingNextMultiplicand, tvMultiplicand))

        val cl = ConstraintSet().apply { clone(clMain) }
        val animators = mutableListOf(tvAnswer.alphaAnimator(0f, duration))
        for (d in ds) {
            d.tvMoving.apply {
                setTextColor(d.tvNext.currentTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, d.tvNext.textSize)
                text = d.tvNext.text
            }
            layoutNextFactor(cl, d.tvMoving.id, null, d.onTopOfTv.id)
            animators.addAll(
                listOf(
                    d.onTopOfTv.alphaAnimator(0f, duration),
                    d.tvNext.alphaAnimator(0f, duration),
                    d.tvMoving.alphaAnimator(1f, duration)
                )
            )
        }
        cl.applyTo(clMain)

        animators.playTogether().run()

        for (d in ds)
            d.onTopOfTv.text = d.tvNext.text
    }

    private suspend fun moveNextTask(duration: Long) {
        class Data(val nextTvs: MutableList<TextView>, val tvMoving: TextView, val onTopOfTv: TextView)

        val ds = mutableListOf(Data(nextTvMultipliers, tvMovingNextMultiplier, tvMultiplier))
        if (isManyMultiplicands)
            ds.add(Data(nextTvMultiplicands, tvMovingNextMultiplicand, tvMultiplicand))
        val animators = mutableListOf<Animator>()
        val cl = ConstraintSet().apply { clone(clMain) }
        for (d in ds) {
            animators.add(d.tvMoving.textColorAnimator(ContextCompat.getColor(this@TestActivity, R.color.taskViewTask), duration))
            cl.apply {
                clear(d.tvMoving.id)
                constrainWidth(d.tvMoving.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                constrainHeight(d.tvMoving.id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                connect(d.tvMoving.id, ConstraintSet.TOP, d.onTopOfTv.id, ConstraintSet.TOP)
                connect(d.tvMoving.id, ConstraintSet.BOTTOM, d.onTopOfTv.id, ConstraintSet.BOTTOM)
                connect(d.tvMoving.id, ConstraintSet.LEFT, d.onTopOfTv.id, ConstraintSet.LEFT)
                connect(d.tvMoving.id, ConstraintSet.RIGHT, d.onTopOfTv.id, ConstraintSet.RIGHT)
                if (d.nextTvs.size > 0)
                    layoutNextFactor(this, d.nextTvs[0].id, null, d.onTopOfTv.id)
            }
        }

        parallel(
            { animators.playTogether().run() },
            {
                clMain.transition(duration, TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(ChangeBounds()).addTransition(ScaleTransition())
                    interpolator = AccelerateInterpolator(1.0f)
                }) {
                    cl.applyTo(clMain)
                    for (d in ds) {
                        d.tvMoving.scaleX = d.onTopOfTv.textSize / d.tvMoving.textSize
                        d.tvMoving.scaleY = d.onTopOfTv.textSize / d.tvMoving.textSize
                    }
                }
            }
        )

        for (d in ds) {
            d.tvMoving.alpha = 0f
            d.tvMoving.scaleX = 1f
            d.tvMoving.scaleY = 1f
            d.onTopOfTv.alpha = 1f
        }
        tvAnswer.alpha = 1f
        nextTvMultipliers.removeAt(0)
        if (isManyMultiplicands)
            nextTvMultiplicands.removeAt(0)
    }
    // endregion

    // region endOfGame
    private fun endOfGame(correct: Boolean) {
        launch {
            showMarkAndProgress(correct)

            ivMark(correct).alphaAnimator(0f, Settings.TestActivityShowMarkDuration).run()
            m.onPreAnimationEnded()

            startActivityForResult(Intent(this@TestActivity, EndOfStageActivity::class.java).apply {
                putExtra(PARAMS, EndOfStageActivityActivityParams(0, 0f))
            }, 0)
        }
    }
    // endregion

    private fun setProgressText() {
        tvProgress.text = "" + (m.taskIdx2Correct.size + 1) + "nd out of " + m.tasks.size
    }

    private suspend fun showMarkAndProgress(correct: Boolean) = coroutineScope {
        ivCheckmark.alpha = 0f
        ivXmark.alpha = 0f

        parallel(
            { pvProgress.animateProgress(m.progress, Settings.TestActivityShowMarkDuration) },
            { ivMark(correct).alphaAnimator(1f, Settings.TestActivityShowMarkDuration).run() }
        )

        delay(Settings.TestActivityPauseAfterMarkDuration)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            this.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE, m.state)
    }
}