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
import java.util.*

class TestActivity : ScopedAppActivity(), InputFragment.OnEventListener {
    companion object {
        const val PARAM_MULTIPLICANDS = "multiplicands"
        private const val STATE_TOTAL_TASKS = "totalTasks"
        private const val STATE_TASKS_LEFT = "tasksLeft"
        private const val Q_TASKS_PER_MULTIPLICAND = 10
    }

    private var nextTvMultipliers = listOf<TextView>()
    private var nextTvMultiplicands = listOf<TextView>()
    private var nextIdx = 0

    @Parcelize
    class Task(val multiplicand: Int, val multiplier: Int) : Parcelable

    @Parcelize
    class Tasks(val tasks: List<Task>) : Parcelable

    private var totalTasks = 0
    private lateinit var tasksLeft: Tasks
    private val input
        get() = fInput as InputFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        fullScreen()

        val multiplicands = intent.getIntArrayExtra(PARAM_MULTIPLICANDS)
        tbToolbarTitle.text = "Test" + " \u25A1 \u00D7 " + (if (multiplicands.count() == 1) multiplicands.first().toString() else "\u25A1")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val r = Random()
            totalTasks = Q_TASKS_PER_MULTIPLICAND * multiplicands.size
            tasksLeft = Tasks((1..totalTasks).map { Task(multiplicands[r.nextInt(multiplicands.size)], r.nextInt(10) + 1) }.toList())
        } else {
            totalTasks = savedInstanceState.getInt(STATE_TOTAL_TASKS)
            tasksLeft = savedInstanceState.getParcelable(STATE_TASKS_LEFT)!!
            if (tasksLeft.tasks.isEmpty()) {
                finish()
                return
            }
        }

        if (tasksLeft.tasks.size > 1) {
            val constraintSet = ConstraintSet().apply { clone(clMain) }
            this.nextTvMultipliers = nextTvFactors(constraintSet, tasksLeft.tasks.drop(1).map { it.multiplier }, tvMultiplier.id)
            if (multiplicands.size > 1)
                this.nextTvMultiplicands = nextTvFactors(constraintSet, tasksLeft.tasks.drop(1).map { it.multiplicand }, tvMultiplicand.id)
            constraintSet.applyTo(clMain)
        }

        tvMultiplier.text = tasksLeft.tasks[0].multiplier.toString()
        tvMultiplicand.text = tasksLeft.tasks[0].multiplicand.toString()

        applyState()
    }

    private fun nextTvFactors(constraintSet: ConstraintSet, factors: List<Int>, onTopOfId: Int): List<TextView> {
        var prevTv: TextView? = null
        return factors.map { factor ->
            TextView(this).apply {
                text = factor.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.testActivityNextFactorFontSize))
                typeface = ResourcesCompat.getFont(context, R.font.lato_blackitalic)
                setTextColor(ContextCompat.getColor(context, R.color.testActivityNextFactor))
                maxLines = 1
                id = ViewCompat.generateViewId()
                layoutNextFactor(constraintSet, this@apply.id, prevTv?.id, onTopOfId)
                this@TestActivity.clMain.addView(this@apply)
                prevTv = this@apply
            }
        }.toList()
    }

    private fun layoutNextFactor(constraintSet: ConstraintSet, id: Int, prevId: Int?, onTopOfId: Int) {
        constraintSet.clear(id)
        constraintSet.connect(id, ConstraintSet.LEFT, onTopOfId, ConstraintSet.LEFT)
        constraintSet.connect(id, ConstraintSet.RIGHT, onTopOfId, ConstraintSet.RIGHT)
        constraintSet.constrainWidth(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        constraintSet.constrainHeight(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        if (prevId == null) {
            constraintSet.connect(id, ConstraintSet.BOTTOM, onTopOfId, ConstraintSet.TOP)
            constraintSet.setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityFirstNextMultiplierBottomMargin))
        } else {
            constraintSet.connect(id, ConstraintSet.BOTTOM, prevId, ConstraintSet.TOP)
            constraintSet.setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityNextMultiplierBottomMargin))
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(STATE_TOTAL_TASKS, totalTasks)
        outState?.putParcelable(STATE_TASKS_LEFT, tasksLeft)
    }

    private fun applyState() {
        pvProgress.progress = tasksLeft.tasks.size / totalTasks.toFloat()
    }

    override fun onAnswerChanged(answer: Int?) {
        tvAnswer.text = answer?.toString() ?: ""
    }

    override fun onOkPressed(answer: Int) {
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            this.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}