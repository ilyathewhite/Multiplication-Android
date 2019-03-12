package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
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

sealed class TrainingActivityViewUpdate {
    class SetToolbarText(
        val text: String
    ) : TrainingActivityViewUpdate()

    class InitField(
        val multiplicand: Int
    ) : TrainingActivityViewUpdate()

    class SetupStage(
        val taskProgress: Float,
        val multiplier: Int,
        val multiplicand: Int,
        val nextMultipliers: List<Int>,
        val multipliersWithAnswers: List<Int>,
        val rowsState: RowsState
    ) : TrainingActivityViewUpdate()

    class SetAnswer(
        val answer: Int?
    ) : TrainingActivityViewUpdate()

    class ShowAnswer(
        val answer: Int?
    ) : TrainingActivityViewUpdate()

    class AnimateRowText(
        val visibleAnswers: List<Int>,
        val multiplier: Int?,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class AnimateMark(
        val mark: Mark,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class Delay(
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class CrossFadeRowState(
        val state: RowsState,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class PulseRowText(
        val multiplier: Int,
        val scale: Float,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class AnimateCountedRows(
        val startState: RowsState,
        val endState: RowsState,
        val animation: UnitAnimation,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class AnimateProgress(
        val taskProgress: Float,
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class PrepareNextTask(
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class SetRowText(
        val multipliersWithAnswers: List<Int>,
        val multiplier: Int
    ) : TrainingActivityViewUpdate()

    class SetLastActiveMultiplier(
        val multiplier: Int
    ) : TrainingActivityViewUpdate()

    class SetMultiplier(
        val multiplier: Int
    ) : TrainingActivityViewUpdate()

    class MoveNextTask(
        val duration: Long
    ) : TrainingActivityViewUpdate()

    class ShowEndOfStage(
        val qErrors: Int,
        val stageProgress: Float,
        val endOfGame: Boolean
    ) : TrainingActivityViewUpdate()

    class ViewModelEvent(
        val event: () -> Unit
    ) : TrainingActivityViewUpdate()
}

class TrainingActivityViewUpdates(vararg list: TrainingActivityViewUpdate?) {
    val updates = mutableListOf<List<TrainingActivityViewUpdate?>>()

    init {
        if (list.isNotEmpty())
            updates.add(list.toList())
    }

    fun add(vararg list: TrainingActivityViewUpdate?) {
        updates.add(list.toList())
    }
}

class TrainingActivityViewModel : ViewModel() {
    companion object {
        private const val HINT_LAST_MULTIPLIERS = 3
    }

    private lateinit var s: TrainingActivityState
    val state: Parcelable get() = s

    private val meUpdate = SingleLiveEvent<TrainingActivityViewUpdates>()
    val eUpdate: LiveData<TrainingActivityViewUpdates> get() = meUpdate

    private var loaded = false
    private var answerFrozen = false

    fun init(savedInstanceState: Bundle?, intent: Intent) {
        if (!loaded) {
            loaded = true
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
        }

        val updates = TrainingActivityViewUpdates(
            TrainingActivityViewUpdate.SetToolbarText((if (s.type == TaskType.Learn) "Learn" else "Practice") + " \u25A1 \u00D7 ${s.multiplicand}"),
            TrainingActivityViewUpdate.InitField(s.multiplicand),
            TrainingActivityViewUpdate.SetAnswer(s.answer),
            TrainingActivityViewUpdate.ShowAnswer(s.answer)
        )
        updates.add(setupStage)
        meUpdate.value = updates
    }

    fun onAnswerChanged(answer: Int?) {
        s.answer = answer
        if (!answerFrozen)
            meUpdate.value = TrainingActivityViewUpdates(TrainingActivityViewUpdate.ShowAnswer(answer))
    }

    fun onPreAnimationEnded() {
        answerFrozen = false
        meUpdate.value = TrainingActivityViewUpdates(TrainingActivityViewUpdate.ShowAnswer(s.answer))
    }

    fun onOkPressed(answer: Int) {
        if (answerFrozen)
            return
        s.answer = null
        answerFrozen = true
        val updates = TrainingActivityViewUpdates(TrainingActivityViewUpdate.SetAnswer(null))
        if (s.multiplicand * multiplier != answer) {
            s.attempt++
            s.qErrors++

            val hintMultiplier = this.hintMultiplier

            updates.add(TrainingActivityViewUpdate.ShowAnswer(null))
            updates.add(
                TrainingActivityViewUpdate.AnimateRowText(
                    if (hintMultiplier == 0) listOf() else listOf(hintMultiplier),
                    multiplier,
                    Settings.TrainingActivityShowIncorrectCheckMarkDuration
                ),
                TrainingActivityViewUpdate.AnimateMark(Mark.Incorrect, Settings.TrainingActivityShowIncorrectCheckMarkDuration)
            )

            updates.add(TrainingActivityViewUpdate.Delay(Settings.TrainingActivityPauseAfterIncorrectCheckMarkDuration))

            updates.add(TrainingActivityViewUpdate.AnimateMark(Mark.None, Settings.TrainingActivityHideIncorrectCheckMarkDuration))

            updates.add(TrainingActivityViewUpdate.CrossFadeRowState(startIncorrectRowsState, Settings.TrainingActivityCrossFadeHintViewDuration))

            updates.add(TrainingActivityViewUpdate.ViewModelEvent { onPreAnimationEnded() })

            if (hintMultiplier != 0)
                updates.add(TrainingActivityViewUpdate.PulseRowText(hintMultiplier, Settings.TrainingActivityPulseScale, Settings.TrainingActivityPulseHintFromDuration))

            val duration = abs(multiplier - hintMultiplier) *
                    if (unitAnimation == UnitAnimation.ByUnit) Settings.ShowHintRowDuration else Settings.ShowHintUnitRowDuration
            updates.add(TrainingActivityViewUpdate.AnimateCountedRows(startIncorrectRowsState, endIncorrectRowsState, unitAnimation!!, duration))

        } else {
            s.attempt = 0
            s.multiplierIdx++

            val endOfStage = s.multiplierIdx == s.stages[s.stageIdx].multipliers.size

            updates.add(
                TrainingActivityViewUpdate.AnimateProgress(taskProgress, Settings.ShowCorrectCheckMarkDuration),
                if (endOfStage) null else TrainingActivityViewUpdate.CrossFadeRowState(startCorrectRowsState, Settings.ShowCorrectCheckMarkDuration),
                if (endOfStage) null else TrainingActivityViewUpdate.AnimateRowText(multipliersWithAnswers, null, Settings.ShowCorrectCheckMarkDuration),
                TrainingActivityViewUpdate.AnimateMark(Mark.Correct, Settings.ShowCorrectCheckMarkDuration)
            )

            updates.add(TrainingActivityViewUpdate.Delay(Settings.PauseAfterCorrectCheckMarkDuration))

            updates.add(
                TrainingActivityViewUpdate.AnimateMark(Mark.None, Settings.TrainingActivityHideCorrectCheckMarkDuration),
                TrainingActivityViewUpdate.ShowAnswer(null),
                TrainingActivityViewUpdate.PrepareNextTask(Settings.PrepareNextTaskDuration)
            )

            if (!endOfStage) {
                updates.add(TrainingActivityViewUpdate.ViewModelEvent { onPreAnimationEnded() })

                updates.add(
                    TrainingActivityViewUpdate.SetRowText(multipliersWithAnswers, multiplier),
                    TrainingActivityViewUpdate.SetLastActiveMultiplier(multiplier),
                    TrainingActivityViewUpdate.SetAnswer(answer),
                    TrainingActivityViewUpdate.SetMultiplier(multiplier),
                    TrainingActivityViewUpdate.AnimateCountedRows(
                        startCorrectRowsState,
                        endCorrectRowState,
                        UnitAnimation.ByRow,
                        abs(multiplier - prevMultiplier) * Settings.ShowHintRowDuration
                    )
                )

                updates.add(TrainingActivityViewUpdate.MoveNextTask(Settings.MoveNextTaskDuration))
            } else {
                updates.add(TrainingActivityViewUpdate.ShowEndOfStage(s.qErrors, stageProgress, s.stageIdx + 1 == s.stages.size))
            }
        }

        meUpdate.value = updates
    }

    fun onNextStage() {
        s.stageIdx++
        s.qErrors = 0
        s.multiplierIdx = 0
        s.answer = null
        meUpdate.value = TrainingActivityViewUpdates(
            TrainingActivityViewUpdate.SetAnswer(null),
            TrainingActivityViewUpdate.ShowAnswer(null),
            setupStage
        )
    }

    private val prevMultiplier get() = stage.multipliers[s.multiplierIdx - 1]
    private val multiplier get() = stage.multipliers[s.multiplierIdx]
    private val nextMultipliers get() = stage.multipliers.drop(s.multiplierIdx + 1).toList()
    private val multipliersWithAnswers get() = if (stage.showPrevAnswers) stage.multipliers.take(s.multiplierIdx) else listOf()
    private val taskProgress get() = if (stage.multipliers.size == s.multiplierIdx) 1f else s.multiplierIdx.toFloat() / stage.multipliers.size
    private val stageProgress get() = if (s.stages.size == s.stageIdx) 1f else (s.stageIdx + if (stage.multipliers.size == s.multiplierIdx) 1 else 0).toFloat() / s.stages.size
    private val hintMultiplier  // may be 0
        get() = ((arrayOf(0) + stage.multipliers.take(s.multiplierIdx)).takeLast(HINT_LAST_MULTIPLIERS) + stage.extraHints)
            .filter { it != multiplier }
            .minBy { Math.abs(it - multiplier) }!!

    private val startCorrectRowsState get() = RowsState(prevMultiplier, 0, 0)
    private val endCorrectRowState get() = RowsState(multiplier, 0, 0)
    private val startIncorrectRowsState get() = hintMultiplier.let { hint -> RowsState(hint, Math.max(multiplier - hint, 0), 0) }
    private val endIncorrectRowsState get() = RowsState(multiplier, 0, Math.max(hintMultiplier - multiplier, 0))

    private val unitAnimation
        get() = when (s.attempt) {
            0 -> null
            1 -> UnitAnimation.ByRow
            else -> UnitAnimation.ByUnit
        }

    private val setupStage
        get() = TrainingActivityViewUpdate.SetupStage(
            taskProgress,
            multiplier,
            s.multiplicand,
            nextMultipliers,
            multipliersWithAnswers,
            endCorrectRowState
        )

    private val stage get() = s.stages[s.stageIdx]
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
        m.eUpdate.observe(this, Observer { if (it != null) update(it) })
        m.init(savedInstanceState, intent)

        llOuter.viewTreeObserver.addOnGlobalLayoutListener {
            fieldView.layout()
        }

        (fInput as InputFragment).setListener(object : InputFragment.EventListener {
            override fun onAnswerChanged(answer: Int?) {
                m.onAnswerChanged(answer)
            }

            override fun onOkPressed(answer: Int) {
                m.onOkPressed(answer)
            }
        })
    }

    private fun update(updates: TrainingActivityViewUpdates) {
        launch {
            for (list in updates.updates) {
                list.filterNotNull().map { async { update(it) } }.map { it.await() }
            }
        }
    }

    private suspend fun update(u: TrainingActivityViewUpdate) = when (u) {
        is TrainingActivityViewUpdate.SetToolbarText -> tbToolbarTitle.text = u.text
        is TrainingActivityViewUpdate.InitField -> fieldView.initialize(u.multiplicand)
        is TrainingActivityViewUpdate.SetupStage -> setupStage(u)
        is TrainingActivityViewUpdate.SetAnswer -> (fInput as InputFragment).answer = u.answer
        is TrainingActivityViewUpdate.ShowAnswer -> taskView.setAnswer(u.answer)
        is TrainingActivityViewUpdate.AnimateRowText -> fieldView.animateRowText(u.visibleAnswers, u.multiplier, u.duration).run()
        is TrainingActivityViewUpdate.AnimateMark -> fieldView.animateMark(u.mark, u.duration).run()
        is TrainingActivityViewUpdate.Delay -> delay(u.duration)
        is TrainingActivityViewUpdate.CrossFadeRowState -> fieldView.crossFadeRowState(u.state, u.duration).run()
        is TrainingActivityViewUpdate.PulseRowText -> fieldView.pulseRowText(u.multiplier, u.scale, u.duration)
        is TrainingActivityViewUpdate.AnimateCountedRows -> fieldView.animateCountedRows(u.startState, u.endState, u.animation, u.duration).run()
        is TrainingActivityViewUpdate.AnimateProgress -> pvProgress.animateProgress(u.taskProgress, u.duration)
        is TrainingActivityViewUpdate.PrepareNextTask -> taskView.prepareNextTask(u.duration)
        is TrainingActivityViewUpdate.SetRowText -> fieldView.setRowText(u.multipliersWithAnswers, u.multiplier)
        is TrainingActivityViewUpdate.SetLastActiveMultiplier -> fieldView.setLastActiveMultiplier(u.multiplier)
        is TrainingActivityViewUpdate.SetMultiplier -> taskView.setMultiplier(u.multiplier)
        is TrainingActivityViewUpdate.MoveNextTask -> taskView.moveNextTask(this, u.duration)
        is TrainingActivityViewUpdate.ShowEndOfStage -> endOfStage(u)
        is TrainingActivityViewUpdate.ViewModelEvent -> u.event()
    }

    private fun setupStage(u: TrainingActivityViewUpdate.SetupStage) {
        pvProgress.progress = u.taskProgress
        fieldView.clearMark()
        fieldView.setRowText(u.multipliersWithAnswers, questionMultiplier = u.multiplier)
        fieldView.setLastActiveMultiplier(u.multiplier)
        fieldView.setRowState(u.rowsState)
        m.onPreAnimationEnded()
        fullScreen()
        taskView.apply {
            createNextMultipliers(u.nextMultipliers)
            setMultiplier(u.multiplier)
            setMultiplicand(u.multiplicand)
        }
    }

    private fun endOfStage(u: TrainingActivityViewUpdate.ShowEndOfStage) {
        startActivityForResult(Intent(this@TrainingActivity, EndOfStageActivity::class.java).apply {
            putExtra(PARAMS, EndOfStageActivityActivityParams(u.qErrors, u.stageProgress))
        }, if (u.endOfGame) END_OF_GAME_ACTIVITY_REQUEST_CODE else END_OF_SET_ACTIVITY_REQUEST_CODE)
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