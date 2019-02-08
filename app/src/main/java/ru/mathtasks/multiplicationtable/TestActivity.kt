package ru.mathtasks.multiplicationtable

import android.os.Bundle
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.MenuItem
import android.widget.TextView
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : ScopedAppActivity(), InputFragment.EventListener {
    @Parcelize
    class Params(val multiplicands: Array<Int>) : Parcelable

    @Parcelize
    private class Task(val multiplicand: Int, val multiplier: Int) : Parcelable

    @Parcelize
    private class State(val tasks: List<Task>, val taskIdx2Correct: List<Int>) : Parcelable

    private var nextTvMultipliers = listOf<TextView>()
    private var nextTvMultiplicands = listOf<TextView>()
    private var okAllowed = true

    private lateinit var state: State
    private val input
        get() = fInput as InputFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        fullScreen()

        val params = intent.getParcelableExtra<Params>(PARAMS)
        tbToolbarTitle.text = "Test" + " \u25A1 \u00D7 " + (if (params.multiplicands.count() == 1) params.multiplicands.first().toString() else "\u25A1")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            state = State(params.multiplicands.flatMap { multiplicand -> (1..10).map { multiplier -> Task(multiplicand, multiplier) } }.shuffled().toList(), listOf())
        } else {
            state = savedInstanceState.getParcelable(STATE)!!
            if (state.taskIdx2Correct.size == state.tasks.size) {
                finish()
                return
            }
        }

        if (state.tasks.size - state.taskIdx2Correct.size > 1) {
            val constraintSet = ConstraintSet().apply { clone(clMain) }
            this.nextTvMultipliers = nextTvFactors(constraintSet, state.tasks.drop(state.taskIdx2Correct.size + 1).map { it.multiplier }, tvMultiplier.id)
            if (params.multiplicands.size > 1)
                this.nextTvMultiplicands = nextTvFactors(constraintSet, state.tasks.drop(state.taskIdx2Correct.size + 1).map { it.multiplicand }, tvMultiplicand.id)
            constraintSet.applyTo(clMain)
        }

        tvMultiplier.text = state.tasks[state.taskIdx2Correct.size].multiplier.toString()
        tvMultiplicand.text = state.tasks[state.taskIdx2Correct.size].multiplicand.toString()

        applyState()
    }

    private fun nextTvFactors(constraintSet: ConstraintSet, factors: List<Int>, onTopOfId: Int): List<TextView> {
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
                this@TestActivity.clMain.addView(this@tv)
                prevTv = this@tv
            }
        }.toList()
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE, state)
    }

    private fun applyState() {
        //pvProgress.progress = state.solvedTasks.toFloat() / state.tasks.size
    }

    override fun onAnswerChanged(answer: Int?) {
        tvAnswer.text = answer?.toString() ?: ""
    }

    override fun onOkPressed(answer: Int) {
        if (!okAllowed)
            return
//        okAllowed = false
//        input.resetAnswer()
//        if (state.tasks[state.solvedTasks].let { it.multiplier * it.multiplicand } == answer) {
//            launch {
//                ivCheckmark.alphaAnimator(1f, Settings.TestActivityShowMarkDuration).run()
//                ivCheckmark.alphaAnimator(0f, Settings.TestActivityHideMarkDuration).run()
//            }
//        } else {
//            launch {
//                ivXmark.alphaAnimator(1f, Settings.TestActivityShowMarkDuration).run()
//                ivXmark.alphaAnimator(0f, Settings.TestActivityHideMarkDuration).run()
//            }
//        }
//        state.solvedTasks++
//        if (state.solvedTasks == state.tasks.size) {
//            finish()
//            return
//        }
//
//        okAllowed = true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            this.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}