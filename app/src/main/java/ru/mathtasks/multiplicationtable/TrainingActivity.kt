package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.abs


@Parcelize
class TrainingActivityParams(
    val type: TaskType,
    val multiplicand: Int
) : Parcelable

@Parcelize
class Stage(
    val showPrevAnswers: Boolean,
    val multipliers: Array<Int>,
    val extraHints: Array<Int>
) : Parcelable

@Parcelize
class TrainingActivityState(
    // per game
    val type: TaskType,
    val multiplicand: Int,
    val stages: List<Stage>,
    // per stage
    var stageIdx: Int = 0,
    var qErrors: Int = 0,
    // per task
    var multiplierIdx: Int = 0,
    var attempt: Int = 0,
    // per attempt
    var answer: Int? = null
) : Parcelable

enum class TrainingActivityUpdate {
    InvisibleAnswer,
    VisibleAnswer,
    Incorrect,
    Correct,
    EndOfStage,
    EndOfGame,
    NextStage,
}

class TrainingActivityViewModel : MyViewModel<TrainingActivityParams, TrainingActivityState, TrainingActivityUpdate>() {
    companion object {
        private const val HINT_LAST_MULTIPLIERS = 3
    }

    private var answerFrozen = false

    override fun create(params: TrainingActivityParams) = TrainingActivityState(
        params.type,
        params.multiplicand,
        when (params.type) {
            TaskType.Learn -> listOf(
//              Stage(true, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf()),
//              Stage(false, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf()),
                Stage(true, arrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf()),
                Stage(false, arrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf()),
                Stage(true, arrayOf(1, 3, 5, 7, 9), arrayOf()),
                Stage(true, arrayOf(9, 7, 5, 3, 1), arrayOf()),
                Stage(true, arrayOf(2, 4, 6, 8, 10), arrayOf()),
                Stage(true, arrayOf(10, 8, 6, 4, 2), arrayOf())
            )
            TaskType.Practice -> listOf(Stage(false, (1..10).shuffled().toTypedArray(), arrayOf(1, 5, 10)))
            else -> throw RuntimeException("Invalid params.type ${params.type}")
        }
    )

    val type get() = s.type
    val multiplicand get() = s.multiplicand
    val prevMultiplier get() = stage.multipliers[s.multiplierIdx - 1]
    val multiplier get() = stage.multipliers[s.multiplierIdx]
    val nextMultipliers get() = stage.multipliers.drop(s.multiplierIdx + 1).toTypedArray()
    val multipliersWithAnswers get() = if (stage.showPrevAnswers) stage.multipliers.take(s.multiplierIdx) else listOf()
    val taskProgress get() = if (stage.multipliers.size == s.multiplierIdx) 1f else s.multiplierIdx.toFloat() / stage.multipliers.size
    val stageProgress get() = if (s.stages.size == s.stageIdx) 1f else (s.stageIdx + if (stage.multipliers.size == s.multiplierIdx) 1 else 0).toFloat() / s.stages.size
    val qErrors get() = s.qErrors
    val answer get() = s.answer
    val hintMultiplier  // may be 0
        get() = ((arrayOf(0) + stage.multipliers.take(s.multiplierIdx)).takeLast(HINT_LAST_MULTIPLIERS) + stage.extraHints)
            .filter { it != multiplier }
            .minBy { Math.abs(it - multiplier) }!!

    val startCorrectRowsState get() = RowsState(prevMultiplier, 0, 0)
    val endCorrectRowState get() = RowsState(multiplier, 0, 0)
    val startIncorrectRowsState get() = hintMultiplier.let { hint -> RowsState(hint, Math.max(multiplier - hint, 0), 0) }
    val endIncorrectRowsState get() = RowsState(multiplier, 0, Math.max(hintMultiplier - multiplier, 0))

    val unitAnimation
        get() = when (s.attempt) {
            0 -> null
            1 -> UnitAnimation.ByRow
            else -> UnitAnimation.ByUnit
        }

    private val stage get() = s.stages[s.stageIdx]

    fun onAnswerChanged(answer: Int?) {
        s.answer = answer
        if (!answerFrozen)
            update(TrainingActivityUpdate.VisibleAnswer)
    }

    fun unfreezeAnswer() {
        answerFrozen = false
        update(TrainingActivityUpdate.VisibleAnswer)
    }

    fun onOkPressed(answer: Int) {
        if (answerFrozen)
            return
        s.answer = null
        update(TrainingActivityUpdate.InvisibleAnswer)
        answerFrozen = true
        if (s.multiplicand * multiplier != answer) {
            s.attempt++
            s.qErrors++
            update(TrainingActivityUpdate.Incorrect)
        } else {
            s.attempt = 0
            s.multiplierIdx++
            update(
                when {
                    s.multiplierIdx < s.stages[s.stageIdx].multipliers.size -> TrainingActivityUpdate.Correct
                    s.stageIdx + 1 < s.stages.size -> TrainingActivityUpdate.EndOfStage
                    else -> TrainingActivityUpdate.EndOfGame
                }
            )
        }
    }

    fun onNextStage() {
        s.stageIdx++
        s.qErrors = 0
        s.multiplierIdx = 0
        update(TrainingActivityUpdate.NextStage)
    }
}

class TrainingActivity : ScopedAppActivity() {
    companion object {
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
        private const val END_OF_GAME_ACTIVITY_REQUEST_CODE = 2
    }

    private lateinit var m: TrainingActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        fullScreen()

        m = getViewModel(this, savedInstanceState)

        tbToolbarTitle.text = (if (m.type == TaskType.Learn) "Learn" else "Practice") + " \u25A1 \u00D7 ${m.multiplicand}"
        fieldView.initialize(m.multiplicand)
        taskViewInitStage()

        llOuter.viewTreeObserver.addOnGlobalLayoutListener {
            fieldView.layout(fieldView.width, fieldView.height)
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
        taskView.setAnswer(m.answer)

        m.observe(this) { u ->
            val o = when (u) {
                TrainingActivityUpdate.InvisibleAnswer -> input.answer = m.answer
                TrainingActivityUpdate.VisibleAnswer -> taskView.setAnswer(m.answer)
                TrainingActivityUpdate.Incorrect -> onIncorrect()
                TrainingActivityUpdate.Correct -> onCorrect()
                TrainingActivityUpdate.EndOfStage -> onEndOfStage(false)
                TrainingActivityUpdate.EndOfGame -> onEndOfStage(true)
                TrainingActivityUpdate.NextStage -> taskViewInitStage()
            }
        }
    }

    private fun onIncorrect() {
        launch {
            val hintMultiplier = m.hintMultiplier

            listOf(
                fieldView.animateRowText(if (hintMultiplier == 0) listOf() else listOf(hintMultiplier), m.multiplier, Settings.TrainingActivityShowIncorrectCheckMarkDuration),
                fieldView.animateMark(Mark.Incorrect, Settings.TrainingActivityShowIncorrectCheckMarkDuration)
            ).flatten().run()

            delay(Settings.TrainingActivityPauseAfterIncorrectCheckMarkDuration)

            fieldView.animateMark(Mark.None, Settings.TrainingActivityHideIncorrectCheckMarkDuration).run()

            fieldView.crossFadeRowState(m.startIncorrectRowsState, Settings.TrainingActivityCrossFadeHintViewDuration).run()

            m.unfreezeAnswer()

            if (hintMultiplier != 0)
                fieldView.pulseRowText(hintMultiplier, Settings.TrainingActivityPulseScale, Settings.TrainingActivityPulseHintFromDuration)

            val duration = abs(m.multiplier - hintMultiplier) *
                    if (m.unitAnimation == UnitAnimation.ByUnit) Settings.ShowHintRowDuration else Settings.ShowHintUnitRowDuration
            fieldView.animateCountedRows(m.startIncorrectRowsState, m.endIncorrectRowsState, m.unitAnimation!!, duration).run()
        }
    }

    private fun onCorrect() {
        launch {
            parallel(
                { pvProgress.animateProgress(m.taskProgress, Settings.TrainingActivityShowCorrectCheckMarkDuration) },
                { fieldView.crossFadeRowState(m.startCorrectRowsState, Settings.TrainingActivityShowCorrectCheckMarkDuration).run() },
                { fieldView.animateRowText(m.multipliersWithAnswers, null, Settings.TrainingActivityShowCorrectCheckMarkDuration).run() },
                { fieldView.animateMark(Mark.Correct, Settings.TrainingActivityShowCorrectCheckMarkDuration).run() }
            )

            delay(Settings.TrainingActivityPauseAfterCorrectCheckMarkDuration)

            taskView.prepareNextTask(Settings.TrainingActivityPrepareNextTaskDuration)

            fieldView.setRowText(m.multipliersWithAnswers, m.multiplier)
            fieldView.setLastActiveMultiplier(m.multiplier)
            m.unfreezeAnswer()
            taskView.setAnswer(m.answer)
            taskView.setMultiplier(m.multiplier)

            fieldView.animateCountedRows(
                m.startCorrectRowsState,
                m.endCorrectRowState,
                UnitAnimation.ByRow,
                abs(m.multiplier - m.prevMultiplier) * Settings.ShowHintRowDuration
            ).run()

            parallel(
                { taskView.moveNextTask(this, Settings.TrainingActivityMoveNextTaskDuration) },
                { fieldView.animateMark(Mark.None, Settings.TrainingActivityMoveNextTaskDuration).run() }
            )
        }
    }

    private fun onEndOfStage(endOfGame: Boolean) {
        launch {
            parallel(
                { pvProgress.animateProgress(m.taskProgress, Settings.TrainingActivityShowCorrectCheckMarkDuration) },
                { fieldView.animateRowText(m.multipliersWithAnswers, null, Settings.TrainingActivityShowCorrectCheckMarkDuration).run() },
                { fieldView.animateMark(Mark.Correct, Settings.TrainingActivityShowCorrectCheckMarkDuration).run() }
            )

            delay(Settings.TrainingActivityPauseAfterCorrectCheckMarkDuration)

            fieldView.animateMark(Mark.None, Settings.TrainingActivityMoveNextTaskDuration).run()

            startActivityForResult(Intent(this@TrainingActivity, EndOfStageActivity::class.java).apply {
                putExtra(PARAMS, EndOfStageActivityActivityParams(m.qErrors, m.stageProgress))
            }, if (endOfGame) END_OF_GAME_ACTIVITY_REQUEST_CODE else END_OF_SET_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun taskViewInitStage() {
        pvProgress.progress = m.taskProgress
        fieldView.clearMark()
        fieldView.setRowText(m.multipliersWithAnswers, questionMultiplier = m.multiplier)
        fieldView.setLastActiveMultiplier(m.multiplier)
        fieldView.setRowState(m.endCorrectRowState)
        fullScreen()
        taskView.apply {
            createNextMultipliers(m.nextMultipliers)
            setMultiplier(m.multiplier)
            setMultiplicand(m.multiplicand)
        }
        m.unfreezeAnswer()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == END_OF_SET_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK -> m.onNextStage()
            else -> finish()
        }
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