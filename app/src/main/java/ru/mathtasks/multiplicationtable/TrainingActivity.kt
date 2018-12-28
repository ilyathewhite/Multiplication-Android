package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.abs

class TrainingActivity : ScopedAppActivity(), InputFragment.OnEventListener {
    companion object {
        const val PARAM_MULTIPLICAND = "multiplicand"
        const val PARAM_TASK_TYPE = "taskType"
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
        private const val END_OF_GAME_ACTIVITY_REQUEST_CODE = 2
        private const val STATE_TASK_PROVIDER = "taskProvider"
    }

    private lateinit var taskProvider: TaskProvider
    private var autoUpdateAnswer = true
    private val input
        get() = fInput as InputFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        fullScreen()

        val taskType = intent.getSerializableExtra(PARAM_TASK_TYPE) as TaskType
        val multiplicand = intent.getIntExtra(PARAM_MULTIPLICAND, 1)
        tbToolbarTitle.text = (if (taskType == TaskType.Learn) "Learn" else "Practice") + " \u25A1 \u00D7 $multiplicand"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null)
            taskProvider = TaskProvider(taskType, multiplicand)
        else {
            taskProvider = savedInstanceState.getParcelable(STATE_TASK_PROVIDER)!!
            if (taskProvider.endOfGame) {
                finish()
                return
            }
        }
        pvProgress.progress = taskProvider.taskProgress
        fieldView.initialize(taskProvider.multiplicand)
        applyState()

        llOuter.viewTreeObserver.addOnGlobalLayoutListener {
            fieldView.layout(fieldView.width, fieldView.height)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE_TASK_PROVIDER, taskProvider)
    }

    private fun applyState() {
        pvProgress.progress = taskProvider.taskProgress
        taskView.apply {
            createNextMultipliers(taskProvider.nextMultipliers)
            setMultiplier(taskProvider.multiplier)
            setMultiplicand(taskProvider.multiplicand)
            setAnswer(input.answer)
        }
        fieldView.setRowText(taskProvider.prevAnswers, taskProvider.multiplier)
        fieldView.setLastActiveMultiplier(taskProvider.multiplier)
        fieldView.setRowState(taskProvider.rowsState)
    }

    override fun onAnswerChanged(answer: Int?) {
        if (autoUpdateAnswer)
            taskView.setAnswer(answer)
    }

    override fun onOkPressed(answer: Int) {
        if (!autoUpdateAnswer)
            return
        val correct = taskProvider.answer2correct(answer)
        if (!correct) {
            input.resetAnswer()
            launch {
                listOf(
                    fieldView.animateRowText(listOf(), null, Settings.ShowIncorrectCheckMarkDuration),
                    fieldView.animateMark(Mark.Incorrect, Settings.ShowIncorrectCheckMarkDuration)
                ).flatten().run()

                delay(Settings.PauseAfterIncorrectCheckMarkDuration)

                fieldView.setRowText(listOf(taskProvider.hintFrom()), taskProvider.multiplier)
                fieldView.setRowState(taskProvider.hintRowsState)
                autoUpdateAnswer = true
                taskView.setAnswer(input.answer)

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
                    input.resetAnswer()
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
                        taskView.setAnswer(input.answer)
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
                    if (!taskProvider.nextTask())
                        finish()
                    else {
                        input.resetAnswer()
                        applyState()
                        fullScreen()
                    }
                }
            }
            END_OF_GAME_ACTIVITY_REQUEST_CODE -> finish()
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