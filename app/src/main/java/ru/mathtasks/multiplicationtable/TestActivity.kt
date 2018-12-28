package ru.mathtasks.multiplicationtable

import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
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

        applyState()
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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