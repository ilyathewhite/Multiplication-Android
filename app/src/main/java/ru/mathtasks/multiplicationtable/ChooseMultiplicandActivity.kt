package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_choose_multiplicand.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val CHOOSE_MULTIPLICAND_ACTIVITY_TASK_TYPE = "ru.mathTasks.multiplicationTable.chooseMultiplicandActivity.taskType"

class ChooseMultiplicandActivity : ScopedAppActivity() {
    companion object {
        private const val STATE_SELECTED_MULTIPLICAND = "selectedMultiplicand"
    }

    private lateinit var taskType: TaskType
    private var selectedMultiplicand: Int = 2
    private lateinit var multiplicand2button: Map<Int, Button>
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_multiplicand)

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

        taskType = intent!!.extras!![CHOOSE_MULTIPLICAND_ACTIVITY_TASK_TYPE] as TaskType
        tvTaskType.text = if (taskType == TaskType.Learn) "Learn" else "Practice"

        multiplicand2button.forEach { (multiplicand, button) ->
            button.setOnClickListener {
                btnClick(multiplicand)
            }
        }

        llFrame.viewTreeObserver.addOnGlobalLayoutListener {
            multiplicand2button.map { (_, button) -> button }.autoSizeText(Typeface.DEFAULT)
        }

        btnStart.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java).apply {
                putExtra(TRAINING_ACTIVITY_TASK_PROVIDER, TaskProvider(taskType, selectedMultiplicand))
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
}