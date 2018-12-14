package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.abs

class TrainingActivity : ScopedAppActivity() {
    companion object {
        const val PARAM_MULTIPLICAND = "multiplicand"
        const val PARAM_TASK_TYPE = "taskType"
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
        private const val END_OF_GAME_ACTIVITY_REQUEST_CODE = 2
        private const val STATE_TASK_PROVIDER = "taskProvider"
        private const val STATE_ANSWER = "answer"
    }

    private lateinit var taskProvider: TaskProvider
    private var answer: Int? = null
    private var autoUpdateAnswer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        setSystemUiVisibility()

        val taskType = intent.getSerializableExtra(PARAM_TASK_TYPE) as TaskType
        val multiplicand = intent.getIntExtra(PARAM_MULTIPLICAND, 1)
        hHeader.caption = (if (taskType == TaskType.Learn) "Learn" else "Practice") + " \u25A1 \u00D7 $multiplicand"

        if (savedInstanceState == null)
            taskProvider = TaskProvider(taskType, multiplicand)
        else {
            taskProvider = savedInstanceState.getParcelable(STATE_TASK_PROVIDER)!!
            if (taskProvider.endOfGame) {
                finish()
                return
            }
            answer = if (savedInstanceState.containsKey(STATE_ANSWER)) savedInstanceState.getInt(STATE_ANSWER) else null
        }
        pvProgress.progress = taskProvider.taskProgress
        fieldView.initialize(taskProvider.multiplicand)
        applyState()

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


        llOuter.viewTreeObserver.addOnGlobalLayoutListener {
            fieldView.layout(fieldView.width, fieldView.height)
            val allButtons = buttons.map { it.button } + listOf(btnBs, btnOk)
            allButtons.autoSizeText(ResourcesCompat.getFont(this, R.font.lato_bold)!!, 0.8f)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
                allButtons.forEach { it.elevation = 10f }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE_TASK_PROVIDER, taskProvider)
        if (answer != null)
            outState?.putInt(STATE_ANSWER, answer!!)
    }

    private fun applyState() {
        pvProgress.progress = taskProvider.taskProgress
        taskView.apply {
            createNextMultipliers(taskProvider.nextMultipliers)
            setMultiplier(taskProvider.multiplier)
            setMultiplicand(taskProvider.multiplicand)
            setAnswer(answer)
        }
        fieldView.setRowText(taskProvider.prevAnswers, taskProvider.multiplier)
        fieldView.setLastActiveMultiplier(taskProvider.multiplier)
        fieldView.setRowState(taskProvider.rowsState)
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
                    fieldView.animateRowText(listOf(), null, Settings.ShowIncorrectCheckMarkDuration),
                    fieldView.animateMark(Mark.Incorrect, Settings.ShowIncorrectCheckMarkDuration)
                ).flatten().run()

                delay(Settings.PauseAfterIncorrectCheckMarkDuration)

                fieldView.setRowText(listOf(taskProvider.hintFrom()), taskProvider.multiplier)
                fieldView.setRowState(taskProvider.hintRowsState)
                autoUpdateAnswer = true
                taskView.setAnswer(null)

                fieldView.animateMark(Mark.None, Settings.HideIncorrectCheckMarkDuration).run()

                val duration = abs(taskProvider.hintFrom() - taskProvider.multiplier) *
                        if (taskProvider.unitAnimation == UnitAnimation.ByUnit) Settings.ShowHintRowDuration else Settings.ShowHintUnitRowDuration
                fieldView.animateCountedRows(taskProvider.hintRowsState, taskProvider.rowsState, taskProvider.unitAnimation!!, duration).run()

                fieldView.animateRowText(taskProvider.prevAnswers, taskProvider.multiplier, Settings.ShowVisibleAnswersDuration).run()
            }
        } else {
            taskProvider.nextTask()
            when {
                taskProvider.endOfGame -> {
                    pvProgress.progress = taskProvider.taskProgress
                    startActivityForResult(Intent(this, EndOfStageActivity::class.java).apply {
                        putExtra(EndOfStageActivity.PARAM_Q_ERRORS, taskProvider.qErrors)
                        putExtra(EndOfStageActivity.PARAM_PROGRESS, taskProvider.stageProgress)
                    }, END_OF_GAME_ACTIVITY_REQUEST_CODE)
                }
                taskProvider.endOfStage -> {
                    pvProgress.progress = taskProvider.taskProgress
                    startActivityForResult(Intent(this, EndOfStageActivity::class.java).apply {
                        putExtra(EndOfStageActivity.PARAM_Q_ERRORS, taskProvider.qErrors)
                        putExtra(EndOfStageActivity.PARAM_PROGRESS, taskProvider.stageProgress)
                    }, END_OF_SET_ACTIVITY_REQUEST_CODE)
                }
                else -> {
                    answer = null
                    launch {
                        listOf(
                            async { pvProgress.animateProgress(taskProvider.taskProgress, Settings.ShowCorrectCheckMarkDuration) },
                            async {
                                listOf(
                                    fieldView.animateRowText(taskProvider.prevAnswers, null, Settings.ShowCorrectCheckMarkDuration),
                                    fieldView.animateMark(Mark.Correct, Settings.ShowCorrectCheckMarkDuration)
                                ).flatten().run()
                            }
                        ).map { it.await() }

                        delay(Settings.PauseAfterCorrectCheckMarkDuration)

                        taskView.prepareNextTask(Settings.PrepareNextTaskDuration)

                        fieldView.setRowText(taskProvider.prevAnswers, taskProvider.multiplier)
                        fieldView.setLastActiveMultiplier(taskProvider.multiplier)
                        fieldView.setRowState(taskProvider.rowsState)
                        autoUpdateAnswer = true
                        taskView.setAnswer(answer)
                        taskView.setMultiplier(taskProvider.multiplier)

                        listOf(
                            async { taskView.moveNextTask(this, Settings.MoveNextTaskDuration) },
                            async { fieldView.animateMark(Mark.None, Settings.MoveNextTaskDuration).run() }
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
                    answer = null
                    applyState()
                    setSystemUiVisibility()
                }
            }
            END_OF_GAME_ACTIVITY_REQUEST_CODE -> finish()
        }
    }

    private fun setSystemUiVisibility() {
        window.decorView.systemUiVisibility = (SDK(Build.VERSION_CODES.JELLY_BEAN, View.SYSTEM_UI_FLAG_LAYOUT_STABLE, 0)
                or SDK(Build.VERSION_CODES.JELLY_BEAN, View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION, 0)
                or SDK(Build.VERSION_CODES.JELLY_BEAN, View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, 0)
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SDK(Build.VERSION_CODES.JELLY_BEAN, View.SYSTEM_UI_FLAG_FULLSCREEN, 0)
                or SDK(Build.VERSION_CODES.KITKAT, View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, 0))
    }
}