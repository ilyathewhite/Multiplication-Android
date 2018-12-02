package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.lang.Math.max
import java.lang.Math.min

const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : AppCompatActivity() {
    companion object {
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
    }

    private lateinit var taskProvider: TaskProvider
    private lateinit var fieldView: FieldView
    private lateinit var taskView: TaskView
    private var answer: Int? = null
    private var qErrors = 0
    private var attemptIdx = 1
    private var lastAnimator: Animator? = null
    private var autoUpdateAnswer = true

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

        this.taskView = findViewById<TaskView>(R.id.task_view).apply {
            createNextMultipliers(taskProvider.nextMultipliers)
            setMultiplier(taskProvider.multiplier)
            setMultiplicand(taskProvider.multiplicand)
            setAnswer(null)
        }

        this.fieldView = findViewById(R.id.field_view)
        fieldView.post {
            fieldView.initialize(taskProvider.multiplicand, fieldView.width, fieldView.height)
            fieldView.setFieldState(FieldState(Mark.None, taskProvider.multiplier, taskProvider.multiplier, 0, 0, arrayOf(), taskProvider.multiplier))
        }
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
        if (!autoUpdateAnswer)
            return
        val correct = answer == taskProvider.multiplicand * taskProvider.multiplier
        if (!correct)
            qErrors++

//        if (correct) {
//            answer = null
//            attemptIdx = 0
//            fieldView.animateFieldState(
//                fieldView.state.copy(mark = Mark.Correct, questionMultiplier = null),
//                null,
//                resources.getDuration(R.integer.trainingActivityShowCorrectCheckMarkDuration)
//            ).apply {
//                addListener(object : {_, -> {
//                    override fun onAnimationEnd(animation: Animator?) {
//                    }
//                })
//                start()
//            }
//        }
    }

//    animateAnswer(correct)
//    {
//        answer = null
//        attemptIdx = if (correct) 1 else attemptIdx + 1
//        if (correct) {
//            fieldView[taskProvider.multiplier - 1].animateProductShowType(ProductShowType.None)
//            taskProvider.nextTask()
//            when {
//                taskProvider.endOfSet -> {
//                    startActivityForResult(Intent(this, EndOfSetActivity::class.java).apply {
//                        putExtra(EndOfSetActivity.INPUT_Q_ERRORS, qErrors)
//                    }, END_OF_SET_ACTIVITY_REQUEST_CODE)
//                    qErrors = 0
//                }
//                taskProvider.endOfGame -> {
//                }
//                else -> {
//                    animateTask(true)
//                    updateTask()
//                }
//            }
//        } else {
//            updateTask()
//            animateTask(false)
//        }
//        keyboardEnabled = true
//    }
//}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == END_OF_SET_ACTIVITY_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK)
                finish()
            else {
                taskProvider.nextTask()
                taskView.createNextMultipliers(taskProvider.nextMultipliers)
                taskView.setMultiplier(taskProvider.multiplier)

//                animateTask(false)
//                updateTask()
            }
        }
    }

    private fun animateAnswer(correct: Boolean, onEnd: () -> Unit) {
        findViewById<ImageView>(R.id.iv_mark).apply {
            setBackgroundCompat(ContextCompat.getDrawable(this@TrainingActivity, if (correct) R.drawable.checkmark else R.drawable.xmark))
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