package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.view.MenuItem
import android.widget.Button
import kotlinx.android.synthetic.main.activity_choose_multiplicand.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChooseMultiplicandActivity : ScopedAppActivity() {
    companion object {
        const val PARAM_TASK_TYPE = "taskType"
        private const val STATE_SELECTED_MULTIPLICAND = "selectedMultiplicand"
    }

    private lateinit var taskType: TaskType
    private var selectedMultiplicand: Int = 2
    private lateinit var multiplicand2button: Map<Int, Button>
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_multiplicand)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false);

        multiplicand2button = mapOf(
            1 to btn1,
            3 to btn3,
            2 to btn2,
            4 to btn4,
            5 to btn5,
            6 to btn6,
            7 to btn7,
            8 to btn8,
            9 to btn9,
            10 to btn10
        )

        taskType = intent!!.extras!![PARAM_TASK_TYPE] as TaskType
        tbToolbarTitle.text = if (taskType == TaskType.Learn) "Learn" else "Practice"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true);

        multiplicand2button.forEach { (multiplicand, button) ->
            button.setOnClickListener {
                btnClick(multiplicand)
            }
        }

        llFrame.viewTreeObserver.addOnGlobalLayoutListener {
            multiplicand2button.map { (_, button) -> button }.autoSizeText(ResourcesCompat.getFont(this, R.font.lato_bold)!!, 0.4f)
        }

        btnStart.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java).apply {
                putExtra(TrainingActivity.PARAM_TASK_TYPE, taskType)
                putExtra(TrainingActivity.PARAM_MULTIPLICAND, selectedMultiplicand)
            })
        }

        if (savedInstanceState != null)
            selectedMultiplicand = savedInstanceState.getInt(STATE_SELECTED_MULTIPLICAND)

        btnClick(selectedMultiplicand)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt(STATE_SELECTED_MULTIPLICAND, selectedMultiplicand)
    }

    private fun btnClick(multiplicand: Int) {
        multiplicand2button[selectedMultiplicand]!!.isPressed = false
        selectedMultiplicand = multiplicand
        tvMultiplicand.text = selectedMultiplicand.toString()

        job?.cancel()
        job = launch {
            multiplicand2button[selectedMultiplicand]!!.isPressed = true
            while (true) {
                for (m in 1..10) {
                    tvMultiplier.text = m.toString()
                    tvProduct.text = (selectedMultiplicand * m).toString()
                    delay(Settings.ChooseMultiplicandDelay)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            this.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}