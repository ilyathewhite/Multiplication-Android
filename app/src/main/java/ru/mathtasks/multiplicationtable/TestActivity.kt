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


        val constraintSet = ConstraintSet().apply { clone(clMain) }
        var prevTv: TextView? = null
        this.nextTvMultipliers = tasksLeft.tasks.map { task ->
            TextView(this).apply {
                text = task.multiplier.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.taskViewNextMultiplierFontSize))
                typeface = ResourcesCompat.getFont(context, R.font.lato_blackitalic)
                setTextColor(ContextCompat.getColor(context, R.color.taskViewNextMultiplier))
                maxLines = 1
                id = ViewCompat.generateViewId()
                layoutNextMultiplier(constraintSet, this@apply.id, prevTv?.id)
                this@TestActivity.clMain.addView(this@apply)
                prevTv = this@apply
            }
        }.toList()
        constraintSet.applyTo(clMain)

        applyState()
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(STATE_TOTAL_TASKS, totalTasks)
        outState?.putParcelable(STATE_TASKS_LEFT, tasksLeft)
    }

    private fun applyState() {
        pvProgress.progress = tasksLeft.tasks.size / totalTasks.toFloat()
    }

    override fun onAnswerChanged(answer: Int?) {
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