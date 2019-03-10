package ru.mathtasks.multiplicationtable

import android.arch.lifecycle.*
import android.content.Intent
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Parcelize
class TestActivityParams(val multiplicands: Array<Int>) : Parcelable

@Parcelize
class Task(val multiplicand: Int, val multiplier: Int) : Parcelable

@Parcelize
class TestActivityState(
    val multiplicands: Array<Int>,
    val tasks: List<Task>,
    val taskIdx2Correct: ArrayList<Boolean>,
    var answer: Int? = null
) : Parcelable

class TestActivityViewModel : ViewModel() {
    private var initialized = false
    private lateinit var s: TestActivityState
    private var answerFrozen = false

    private val meInvisibleAnswer = SingleLiveEvent<Int?>()
    val eInvisibleAnswer: LiveData<Int?> get() = meInvisibleAnswer

    private val mdVisibleAnswer = MutableLiveData<Int?>()
    val dVisibleAnswer: LiveData<Int?> get() = mdVisibleAnswer

    private val meAttempt = SingleLiveEvent<Boolean>()
    val eAttempt: LiveData<Boolean> get() = meAttempt

    private val meEndOfTest = SingleLiveEvent<Boolean>()
    val eEndOfTest: LiveData<Boolean> get() = meEndOfTest

    val state: Parcelable get() = s
    val multiplicands get() = s.multiplicands
    val tasks get() = s.tasks
    val taskIdx2Correct get() = s.taskIdx2Correct
    val task get() = s.tasks[s.taskIdx2Correct.size]
    val progress get() = taskIdx2Correct.size.toFloat() / tasks.size
    val answer get() = s.answer

    fun init(savedInstanceState: Bundle?, intent: Intent) {
        if (initialized)
            return
        initialized = true
        s = if (savedInstanceState != null)
            savedInstanceState.getParcelable(STATE) as TestActivityState
        else {
            val params = intent.getParcelableExtra(PARAMS) as TestActivityParams
            TestActivityState(
                params.multiplicands,
                params.multiplicands.flatMap { multiplicand -> (1..10).map { multiplier -> Task(multiplicand, multiplier) } }.shuffled().toList(),
                arrayListOf()
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
        mdVisibleAnswer.value = s.answer
    }

    fun onOkPressed(answer: Int) {
        if (answerFrozen)
            return
        s.answer = null
        meInvisibleAnswer.value = s.answer
        answerFrozen = true
        val correct = (task.multiplicand * task.multiplier == answer)
        s.taskIdx2Correct.add(correct)
        if (s.taskIdx2Correct.size < s.tasks.size)
            meAttempt.value = correct
        else
            meEndOfTest.value = correct
    }
}

class TestActivity : ScopedAppActivity() {
    private lateinit var m: TestActivityViewModel

    private var nextTvMultipliers = listOf<TextView>()
    private var nextTvMultiplicands = listOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        fullScreen()

        m = ViewModelProviders.of(this).get(TestActivityViewModel::class.java)
        m.init(savedInstanceState, intent)

        tbToolbarTitle.text = "Test" + " \u25A1 \u00D7 " + (if (m.multiplicands.count() == 1) m.multiplicands.first().toString() else "\u25A1")

        if (m.tasks.size - m.taskIdx2Correct.size > 1) {
            val constraintSet = ConstraintSet().apply { clone(clMain) }
            this.nextTvMultipliers = nextTvFactors(constraintSet, m.tasks.drop(m.taskIdx2Correct.size + 1).map { it.multiplier }, tvMultiplier.id)
            if (m.multiplicands.size > 1)
                this.nextTvMultiplicands = nextTvFactors(constraintSet, m.tasks.drop(m.taskIdx2Correct.size + 1).map { it.multiplicand }, tvMultiplicand.id)
            constraintSet.applyTo(clMain)
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
            tvAnswer.text = answer?.toString() ?: ""
        })

        m.eAttempt.observe(this, Observer { correct ->
            if(correct == null)
                return@Observer
            val ivMark = if(correct) ivCheckmark else ivXmark
            launch {
                listOf(
                    async { pvProgress.animateProgress(m.progress, Settings.TestActivityShowMarkDuration) },
                    async { ivMark.alphaAnimator(1f, Settings.TestActivityShowMarkDuration).run() }
                ).map { it.await() }

                delay(Settings.TestActivityPauseAfterMarkDuration)

                ivMark.alphaAnimator(0f, Settings.TestActivityHideMarkDuration).run()
            }

            tvMultiplier.text = m.task.multiplier.toString()
            tvMultiplicand.text = m.task.multiplicand.toString()
            pvProgress.progress = m.progress
        })
    }

    private fun nextTvFactors(constraintSet: ConstraintSet, factors: List<Int>, onTopOfId: Int): List<TextView> {
        var prevTv: TextView? = null
        return factors.map { factor ->
            TextView(this).apply tv@{
                text = factor.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.testActivityNextFactorFontSize))
                typeface = ResourcesCompat.getFont(context, R.font.lato_blackitalic)
                setTextColor(ContextCompat.getColor(context, R.color.testActivityNextFactor))
                maxLines = 1
                id = ViewCompat.generateViewId()
                layoutNextFactor(constraintSet, this@tv.id, prevTv?.id, onTopOfId)
                this@TestActivity.clMain.addView(this@tv)
                prevTv = this@tv
            }
        }.toList()
    }

    private fun layoutNextFactor(constraintSet: ConstraintSet, id: Int, prevId: Int?, onTopOfId: Int) {
        constraintSet.apply {
            clear(id)
            connect(id, ConstraintSet.LEFT, onTopOfId, ConstraintSet.LEFT)
            connect(id, ConstraintSet.RIGHT, onTopOfId, ConstraintSet.RIGHT)
            constrainWidth(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            constrainHeight(id, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            if (prevId == null) {
                connect(id, ConstraintSet.BOTTOM, onTopOfId, ConstraintSet.TOP)
                setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityFirstNextMultiplierBottomMargin))
            } else {
                connect(id, ConstraintSet.BOTTOM, prevId, ConstraintSet.TOP)
                setMargin(id, ConstraintSet.BOTTOM, resources.getDimensionPixelSize(R.dimen.testActivityNextMultiplierBottomMargin))
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

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(STATE, m.state)
    }
}