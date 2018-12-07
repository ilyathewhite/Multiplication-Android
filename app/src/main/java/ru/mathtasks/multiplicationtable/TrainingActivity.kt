package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.min

const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : ScopedAppActivity() {
    companion object {
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
        private const val END_OF_GAME_ACTIVITY_REQUEST_CODE = 2
    }

    private lateinit var taskProvider: TaskProvider
    private var answer: Int? = null
    private var autoUpdateAnswer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        this.taskProvider = TaskProvider(intent.getIntExtra(TRAINING_ACTIVITY_MULTIPLICAND, 0))

        class B(val button: Button, val value: Int)

        val buttons = arrayOf(
            B(btn1, 1),
            B(btn2, 2),
            B(btn3, 3),
            B(btn4, 4),
            B(btn5, 5),
            B(btn6, 6),
            B(btn7, 7),
            B(btn8, 8),
            B(btn9, 9),
            B(btn0, 0)
        )
        buttons.forEach { b -> b.button.setOnClickListener { onNumKey(b.value) } }
        btnBs.setOnClickListener { onBsKey() }
        btnOk.setOnClickListener { onOkKey() }

        fieldView.post {
            fieldView.initialize(taskProvider.multiplicand, fieldView.width, fieldView.height)
            startSet()
            (buttons.map { it.button } + listOf(btnBs, btnOk)).autoSizeText(Typeface.DEFAULT)
        }
    }

    private fun startSet() {
        answer = null
        taskView.apply {
            createNextMultipliers(taskProvider.nextMultipliers)
            setMultiplier(taskProvider.multiplier)
            setMultiplicand(taskProvider.multiplicand)
            setAnswer(answer)
        }
        fieldView.setFieldState(taskProvider.fieldState)
    }

    private fun onBsKey() {
        answer = if (answer == null || answer!! < 10) null else answer!! / 10
        if (autoUpdateAnswer)
            taskView.setAnswer(answer)
    }

    private fun onNumKey(value: Int) {
        answer = if (answer == null || answer == 0) value else if (answer!! >= 100) answer!! else 10 * answer!! + value
        if (autoUpdateAnswer)
            taskView.setAnswer(answer)
    }

    private fun onOkKey() {
        if (!autoUpdateAnswer || answer == null)
            return
        val correct = taskProvider.answer2correct(answer!!)
        if (!correct) {
            answer = null
            launch {
                listOf(
                    fieldView.animateFieldState(fieldView.state.copy(questionMultiplier = null), null, Settings.ShowIncorrectCheckMarkDuration),
                    fieldView.animateMark(Mark.Incorrect, Settings.ShowIncorrectCheckMarkDuration)
                ).flatten().run()

                delay(Settings.PauseAfterIncorrectCheckMarkDuration)

                fieldView.setFieldState(taskProvider.hintFromFieldState)
                autoUpdateAnswer = true
                taskView.setAnswer(answer)

                listOf(
                    fieldView.animateFieldState(taskProvider.fieldState, taskProvider.unitAnimation, Settings.MoveNextTaskDuration),
                    fieldView.animateMark(Mark.None, Settings.HideIncorrectCheckMarkDuration)
                ).flatten().run()
            }
        } else {
            taskProvider.nextTask()
            when {
                taskProvider.endOfSet -> {
                    startActivityForResult(Intent(this, EndOfSetActivity::class.java).apply {
                        putExtra(EndOfSetActivity.INPUT_Q_ERRORS, taskProvider.qErrors)
                    }, END_OF_SET_ACTIVITY_REQUEST_CODE)
                }
                taskProvider.endOfGame -> {
                    startActivityForResult(Intent(this, EndOfSetActivity::class.java).apply {
                        putExtra(EndOfSetActivity.INPUT_Q_ERRORS, taskProvider.qErrors)
                    }, END_OF_GAME_ACTIVITY_REQUEST_CODE)
                }
                else -> {
                    answer = null
                    launch {
                        listOf(
                            fieldView.animateFieldState(fieldView.state.copy(questionMultiplier = null), null, Settings.ShowCorrectCheckMarkDuration),
                            fieldView.animateMark(Mark.Correct, Settings.ShowCorrectCheckMarkDuration)
                        ).flatten().run()

                        delay(Settings.PauseAfterCorrectCheckMarkDuration)

                        val nextTask = taskView.animateNextTask(Settings.PrepareNextTaskDuration, Settings.MoveNextTaskDuration)
                        nextTask.prepare()

                        fieldView.setFieldState(taskProvider.fieldState)
                        autoUpdateAnswer = true
                        taskView.setAnswer(answer)
                        taskView.setMultiplier(taskProvider.multiplier)

                        listOf(
                            async { nextTask.move() },
                            async {
                                listOf(
                                    fieldView.animateFieldState(taskProvider.fieldState, taskProvider.unitAnimation, Settings.MoveNextTaskDuration),
                                    fieldView.animateMark(Mark.None, Settings.MoveNextTaskDuration)
                                ).flatten().run()
                            }
                        ).map { it.await() }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            END_OF_SET_ACTIVITY_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK)
                    finish()
                else {
                    taskProvider.nextTask()
                    startSet()
                }
            }
            END_OF_GAME_ACTIVITY_REQUEST_CODE -> finish()
        }
    }
}