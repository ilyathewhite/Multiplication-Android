package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.arch.lifecycle.*
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_training.*
import kotlinx.coroutines.async
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

class TrainingActivityViewModel : ViewModel() {
    companion object {
        private const val HINT_LAST_MULTIPLIERS = 3
    }

    private var initialized = false
    private lateinit var s: TrainingActivityState
    private var answerFrozen = false

    private val meInvisibleAnswer = SingleLiveEvent<Int?>()
    val eInvisibleAnswer: LiveData<Int?> get() = meInvisibleAnswer

    private val mdVisibleAnswer = MutableLiveData<Int?>()
    val dVisibleAnswer: LiveData<Int?> get() = mdVisibleAnswer

    private val meIncorrect = SingleLiveEvent<Unit>()
    val eIncorrect: LiveData<Unit> get() = meIncorrect

    private val meCorrect = SingleLiveEvent<Unit>()
    val eCorrect: LiveData<Unit> get() = meCorrect

    class EndOfStage(val endOfGame: Boolean)

    private val meEndOfStage = SingleLiveEvent<EndOfStage>()
    val eEndOfStage: LiveData<EndOfStage> get() = meEndOfStage

    private val meNextStage = SingleLiveEvent<Unit>()
    val eNextStage: LiveData<Unit> get() = meNextStage

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
    val state: Parcelable get() = s
    val hintMultiplier  // may be 0
        get() = ((arrayOf(0) + stage.multipliers.take(s.multiplierIdx)).takeLast(HINT_LAST_MULTIPLIERS) + stage.extraHints)
            .filter { it != multiplier }
            .minBy { Math.abs(it - multiplier) }!!

    val startCorrectRowsState: RowsState
        get() = RowsState(prevMultiplier, 0, 0)

    val endCorrectRowState: RowsState
        get() = RowsState(multiplier, 0, 0)

    val startIncorrectRowsState: RowsState
        get() {
            val hint = hintMultiplier
            return RowsState(hint, Math.max(multiplier - hint, 0), 0)
        }

    val endIncorrectRowsState: RowsState
        get() {
            val hint = hintMultiplier
            return RowsState(multiplier, 0, Math.max(hint - multiplier, 0))
        }

    val unitAnimation
        get() = when (s.attempt) {
            0 -> null
            1 -> UnitAnimation.ByRow
            else -> UnitAnimation.ByUnit
        }

    private val stage get() = s.stages[s.stageIdx]

    fun init(savedInstanceState: Bundle?, intent: Intent) {
        if (initialized)
            return
        initialized = true
        s = if (savedInstanceState != null)
            savedInstanceState.getParcelable(STATE) as TrainingActivityState
        else {
            val params = intent.getParcelableExtra(PARAMS) as TrainingActivityParams
            TrainingActivityState(
                params.type,
                params.multiplicand,
                when (params.type) {
                    TaskType.Learn -> listOf(
//                        Stage(true, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf()),
//                        Stage(false, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf()),
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
        }
        mdVisibleAnswer.value = s.answer
    }

    fun onAnswerChanged(answer: Int?) {
        s.answer = answer
        if (!answerFrozen)
            mdVisibleAnswer.value = answer
    }

    fun onPreAnimationEnded() {
        answerFrozen = false
        mdVisibleAnswer.value = answer
    }

    fun onOkPressed(answer: Int) {
        if (answerFrozen)
            return
        s.answer = null
        meInvisibleAnswer.value = null
        answerFrozen = true
        if (s.multiplicand * multiplier != answer) {
            s.attempt++
            s.qErrors++
            meIncorrect.value = Unit
        } else {
            s.attempt = 0
            s.multiplierIdx++
            when {
                s.multiplierIdx < s.stages[s.stageIdx].multipliers.size -> meCorrect.value = Unit
                s.stageIdx + 1 < s.stages.size -> meEndOfStage.value = EndOfStage(false)
                else -> meEndOfStage.value = EndOfStage(true)
            }
        }
    }

    fun onNextStage() {
        s.stageIdx++
        s.qErrors = 0
        s.multiplierIdx = 0
        meNextStage.value = Unit
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

        m = ViewModelProviders.of(this).get(TrainingActivityViewModel::class.java)
        m.init(savedInstanceState, intent)

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

        m.eInvisibleAnswer.observe(this, Observer { answer ->
            input.answer = answer
        })

        m.dVisibleAnswer.observe(this, Observer { answer ->
            taskView.setAnswer(answer)
        })

        m.eNextStage.observe(this, Observer {
            taskViewInitStage()
        })

        m.eIncorrect.observe(this, Observer {
            launch {
                val hintMultiplier = m.hintMultiplier

                listOf(
                    fieldView.animateRowText(if (hintMultiplier == 0) listOf() else listOf(hintMultiplier), m.multiplier, Settings.TrainingActivityShowIncorrectCheckMarkDuration),
                    fieldView.animateMark(Mark.Incorrect, Settings.TrainingActivityShowIncorrectCheckMarkDuration)
                ).flatten().run()

                delay(Settings.TrainingActivityPauseAfterIncorrectCheckMarkDuration)

                fieldView.animateMark(Mark.None, Settings.TrainingActivityHideIncorrectCheckMarkDuration).run()

                fieldView.crossFadeRowState(m.startIncorrectRowsState, Settings.TrainingActivityCrossFadeHintViewDuration).run()

                m.onPreAnimationEnded()

                if (hintMultiplier != 0)
                    fieldView.pulseRowText(hintMultiplier, Settings.TrainingActivityPulseScale, Settings.TrainingActivityPulseHintFromDuration)

                val duration = abs(m.multiplier - hintMultiplier) *
                        if (m.unitAnimation == UnitAnimation.ByUnit) Settings.ShowHintRowDuration else Settings.ShowHintUnitRowDuration
                fieldView.animateCountedRows(m.startIncorrectRowsState, m.endIncorrectRowsState, m.unitAnimation!!, duration).run()
            }
        })

        m.eCorrect.observe(this, Observer {
            launch {
                listOf(
                    async { pvProgress.animateProgress(m.taskProgress, Settings.ShowCorrectCheckMarkDuration) },
                    async { fieldView.crossFadeRowState(m.startCorrectRowsState, Settings.ShowCorrectCheckMarkDuration).run() },
                    async {
                        listOf(
                            fieldView.animateRowText(m.multipliersWithAnswers, null, Settings.ShowCorrectCheckMarkDuration),
                            fieldView.animateMark(Mark.Correct, Settings.ShowCorrectCheckMarkDuration)
                        ).flatten().run()
                    }
                ).map { it.await() }

                delay(Settings.PauseAfterCorrectCheckMarkDuration)

                taskView.prepareNextTask(Settings.PrepareNextTaskDuration)

                fieldView.setRowText(m.multipliersWithAnswers, m.multiplier)
                fieldView.setLastActiveMultiplier(m.multiplier)
                m.onPreAnimationEnded()
                taskView.setAnswer(m.answer)
                taskView.setMultiplier(m.multiplier)

                fieldView.animateCountedRows(
                    m.startCorrectRowsState,
                    m.endCorrectRowState,
                    UnitAnimation.ByRow,
                    abs(m.multiplier - m.prevMultiplier) * Settings.ShowHintRowDuration
                ).run()

                listOf(
                    async { taskView.moveNextTask(this, Settings.MoveNextTaskDuration) },
                    async { fieldView.animateMark(Mark.None, Settings.MoveNextTaskDuration).run() }
                ).map { it.await() }
            }
        })

        m.eEndOfStage.observe(this, Observer { p ->
            if (p == null)
                return@Observer
            launch {
                listOf(
                    async { pvProgress.animateProgress(m.taskProgress, Settings.ShowCorrectCheckMarkDuration) },
                    async {
                        listOf(
                            fieldView.animateRowText(m.multipliersWithAnswers, null, Settings.ShowCorrectCheckMarkDuration),
                            fieldView.animateMark(Mark.Correct, Settings.ShowCorrectCheckMarkDuration)
                        ).flatten().run()
                    }
                ).map { it.await() }

                delay(Settings.PauseAfterCorrectCheckMarkDuration)

                fieldView.animateMark(Mark.None, Settings.MoveNextTaskDuration).run()

                startActivityForResult(Intent(this@TrainingActivity, EndOfStageActivity::class.java).apply {
                    putExtra(PARAMS, EndOfStageActivityActivityParams(m.qErrors, m.stageProgress))
                }, if (p.endOfGame) END_OF_GAME_ACTIVITY_REQUEST_CODE else END_OF_SET_ACTIVITY_REQUEST_CODE)
            }
        })
    }

    private fun taskViewInitStage() {
        pvProgress.progress = m.taskProgress
        fieldView.clearMark()
        fieldView.setRowText(m.multipliersWithAnswers, questionMultiplier = m.multiplier)
        fieldView.setLastActiveMultiplier(m.multiplier)
        fieldView.setRowState(m.endCorrectRowState)
        m.onPreAnimationEnded()
        fullScreen()
        taskView.apply {
            createNextMultipliers(m.nextMultipliers)
            setMultiplier(m.multiplier)
            setMultiplicand(m.multiplicand)
        }
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