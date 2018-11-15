package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.lang.Math.min
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.view.View
import java.lang.Math.max


const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : Activity() {
    companion object {
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
    }

    private lateinit var taskProvider: TaskProvider
    private lateinit var fieldRows: List<FieldRowView>
    private var nextMultipliers = mutableListOf<TextView>()
    private var answer: Int? = null
    private var qErrors = 0
    private var attemptIdx = 1
    private var taskAnimator: Animator? = null
    private var keyboardEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        this.taskProvider = TaskProvider(intent.getIntExtra(TRAINING_ACTIVITY_MULTIPLICAND, 0))

        class B(val button_id: Int, val value: Int);
        arrayOf(
            B(R.id.btn_1, 1),
            B(R.id.btn_2, 2),
            B(R.id.btn_3, 3),
            B(R.id.btn_4, 4),
            B(R.id.btn_5, 5),
            B(R.id.btn_6, 6),
            B(R.id.btn_7, 7),
            B(R.id.btn_8, 8),
            B(R.id.btn_9, 9),
            B(R.id.btn_0, 0)
        ).map { b -> findViewById<Button>(b.button_id).setOnClickListener { onNumKey(b.value) } }

        findViewById<Button>(R.id.btn_bs).setOnClickListener { onBsKey() }
        findViewById<Button>(R.id.btn_ok).setOnClickListener { onOkKey() }
        updateTask()
        createNextMultipliers()

        val outField = findViewById<FrameLayout>(R.id.fl_outField)
        outField.post {
            val dimension = min(outField.width / (taskProvider.multiplicand + 2), outField.height / 10)
            this.fieldRows = (1..10).map { m -> FieldRowView(this, taskProvider.multiplicand, m, dimension) }.toList()
            val field = findViewById<LinearLayout>(R.id.ll_field)
            for (row in fieldRows)
                field.addView(row)
            animateTask(false)
        }
    }

    private fun createNextMultipliers() {
        nextMultipliers.clear()
        val llNextMultipliers = findViewById<LinearLayout>(R.id.ll_nextMultipliers)
        for (nextMultiplier in taskProvider.nextMultipliers) {
            val tv = TextView(this).apply {
                text = nextMultiplier.toString()
                textSize = resources.getDimension(R.dimen.trainingActivityNextMultiplierFontSize)
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, resources.getDimensionPixelSize(R.dimen.trainingActivityNextMultiplierInterval), 0)
                }
            }
            llNextMultipliers.addView(tv)
            nextMultipliers.add(tv)
        }
        llNextMultipliers.post { setupNextMultipliers() }
    }

    private fun setupNextMultipliers() {
        val llNextMultipliers = findViewById<LinearLayout>(R.id.ll_nextMultipliers)
        val tvMultiplier = findViewById<TextView>(R.id.tv_multiplier)
        val leftMargin = tvMultiplier.left + tvMultiplier.width / 2 - nextMultipliers[0].width / 2
        llNextMultipliers.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(leftMargin, 0, 0, 0)
        }
        for (tv in nextMultipliers)
            tv.alpha = if (tv.right + leftMargin > this.window.decorView.width) 0f else 1f
    }

    private fun updateTask() {
        findViewById<TextView>(R.id.tv_multiplier).text = taskProvider.multiplier.toString()
        findViewById<TextView>(R.id.tv_answer).text = answer?.toString() ?: ""
    }

    private fun animateTask(animateNextMultiplier: Boolean) {
        val textAnimators = mutableListOf<Animator>()
        val fastAnimators = mutableListOf<Animator>()
        val longAnimators = mutableListOf<Animator>()
        for (r in fieldRows) {
            textAnimators.add(r.animateIsMultiplierActive(r.multiplier <= taskProvider.multiplier))
            r.animateProductShowType(
                when {
                    r.multiplier == taskProvider.multiplier -> ProductShowType.Question
                    taskProvider.isPrevMultiplier(r.multiplier) ||
                            (r.multiplier == taskProvider.hintFrom && attemptIdx > 1) -> ProductShowType.Number
                    else -> ProductShowType.None
                }
            )
            if (r.multiplier > max(taskProvider.hintFrom, taskProvider.multiplier))
                fastAnimators.add(r.animateCells(CellState.Empty, AnimationType.Fast))
            else if (r.multiplier <= min(taskProvider.hintFrom, taskProvider.multiplier))
                fastAnimators.add(r.animateCells(CellState.Filled, AnimationType.Fast))
            else if (attemptIdx == 1 && taskProvider.hintFrom < taskProvider.multiplier)
                fastAnimators.add(r.animateCells(CellState.Filled, AnimationType.Fast))
            else if (attemptIdx == 1 && taskProvider.hintFrom > taskProvider.multiplier)
                fastAnimators.add(r.animateCells(CellState.Empty, AnimationType.Fast))
            else if (taskProvider.hintFrom < taskProvider.multiplier) {
                fastAnimators.add(r.animateCells(CellState.ToBeFilled, AnimationType.Fast))
                longAnimators.add(r.animateCells(CellState.Filled, if (attemptIdx == 2) AnimationType.HintFwd1 else AnimationType.HintFwd2))
            } else if (taskProvider.hintFrom > taskProvider.multiplier) {
                fastAnimators.add(r.animateCells(CellState.Filled, AnimationType.Fast))
                longAnimators.add(0, r.animateCells(CellState.WasEmptied, if (attemptIdx == 2) AnimationType.HintBack1 else AnimationType.HintBack2))
            }
        }
        taskAnimator?.end()
        taskAnimator = AnimatorSet().apply {
            playTogether(
                AnimatorSet().apply { playTogether(textAnimators) },
                AnimatorSet().apply {
                    playSequentially(
                        AnimatorSet().apply { playTogether(fastAnimators) },
                        AnimatorSet().apply { playSequentially(longAnimators) })
                })
            start()
        }
    }

    private fun onBsKey() {
        if (!keyboardEnabled)
            return;
        answer = if (answer == null || answer!! < 10) null else answer!! / 10
        updateTask()
    }

    private fun onNumKey(value: Int) {
        if (!keyboardEnabled)
            return;
        answer = if (answer == null || answer == 0) value else 10 * answer!! + value
        updateTask()
    }

    private fun onOkKey() {
        if (!keyboardEnabled)
            return;
        keyboardEnabled = false
        val correct = answer == taskProvider.multiplicand * taskProvider.multiplier
        if (!correct)
            qErrors++
        animateAnswer(correct) {
            answer = null
            attemptIdx = if (correct) 1 else attemptIdx + 1
            if (correct) {
                taskProvider.nextTask()
                when {
                    taskProvider.endOfSet -> {
                        startActivityForResult(Intent(this, EndOfSetActivity::class.java).apply {
                            putExtra(EndOfSetActivity.INPUT_Q_ERRORS, qErrors)
                        }, END_OF_SET_ACTIVITY_REQUEST_CODE)
                        qErrors = 0
                    }
                    taskProvider.endOfGame -> {
                    }
                    else -> {
                        animateTask(true)
                        updateTask()
                    }
                }
            } else {
                updateTask()
                animateTask(false)
            }
            keyboardEnabled = true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == END_OF_SET_ACTIVITY_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK)
                finish()
            else {
                taskProvider.nextTask()
                createNextMultipliers()
                animateTask(false)
                updateTask()
            }
        }
    }

    private fun animateAnswer(correct: Boolean, onEnd: () -> Unit) {
        findViewById<ImageView>(R.id.iv_mark).apply {
            background = ContextCompat.getDrawable(this@TrainingActivity, if (correct) R.drawable.checkmark else R.drawable.xmark)
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setStartDelay(0).setDuration(100).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(p0: Animator?) {
                    findViewById<ImageView>(R.id.iv_mark)
                        .animate()
                        .alpha(0f)
                        .setStartDelay(500)
                        .setDuration(500)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(p0: Animator?) {
                                visibility = View.GONE
                                onEnd()
                            }
                        })
                }
            })
        }
    }
}