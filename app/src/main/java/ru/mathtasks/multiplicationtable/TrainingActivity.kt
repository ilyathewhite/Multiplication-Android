package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import io.reactivex.Completable
import java.util.concurrent.TimeUnit

const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : AppCompatActivity() {
    companion object {
        private const val END_OF_SET_ACTIVITY_REQUEST_CODE = 1
    }

    private lateinit var taskProvider: TaskProvider
    private lateinit var fieldView: FieldView
    private lateinit var taskView: TaskView
    private var answer: Int? = null
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
            fieldView.setFieldState(taskProvider.fieldState)
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
        if (!autoUpdateAnswer || answer == null)
            return
        val correct = taskProvider.answer2correct(answer!!)
        if (correct) {
            taskProvider.nextTask()
            if (!taskProvider.endOfGame && !taskProvider.endOfSet) {
                answer = null
                val showMarkAnimation =
                    fieldView.animateFieldState(fieldView.state.copy(mark = Mark.Correct, questionMultiplier = null), null, Settings.ShowCorrectCheckMarkDuration)
                val movingAnimations = taskView.animateNextTask(Settings.PrepareMultiplierMovingDuration, Settings.MultiplierMovingDuration)

                showMarkAnimation
                    .andThen(Completable.timer(Settings.PauseAfterCorrectCheckMarkDuration, TimeUnit.MILLISECONDS))
                    .andThen(movingAnimations.prepareAnimation)
                    .andThen(Completable.fromRunnable { fieldView.setFieldState(taskProvider.hintFromFieldState) })
                    .andThen(Completable.fromRunnable { autoUpdateAnswer = true; taskView.setAnswer(answer) })
                    .subscribe()
            }
        }
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
}