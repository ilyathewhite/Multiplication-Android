package ru.mathtasks.multiplicationtable

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.lang.Math.min

const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : Activity() {

    private var answer : Int? = null
    private lateinit var taskProvider: TaskProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        this.taskProvider = TaskProvider(intent.getIntExtra(TRAINING_ACTIVITY_MULTIPLICAND, 0))

        class B(val button_id:Int, val value: Int);
        val buttons = arrayOf(
            B(R.id.btn_1, 1),
            B(R.id.btn_2, 2),
            B(R.id.btn_3, 3),
            B(R.id.btn_4, 4),
            B(R.id.btn_5, 5),
            B(R.id.btn_6, 6),
            B(R.id.btn_7, 7),
            B(R.id.btn_8, 8),
            B(R.id.btn_9, 9),
            B(R.id.btn_0, 0));
        for(b in buttons)
            findViewById<Button>(b.button_id).setOnClickListener { _ -> onNumKey(b.value)}

        findViewById<Button>(R.id.btn_bs).setOnClickListener { _ -> onBsKey()}
        updateTask()

        val field = findViewById<LinearLayout>(R.id.ll_field)
		field.removeAllViews()
        field.post {
            val dim = min(field.width/(taskProvider.multiplier+2), field.height/10)	// 1 extra before, 2 extra after

            var animatedCells = mutableListOf<CellView>()
            for(m in 1..10) {
                val hl = LinearLayout(this).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }
                field.addView(hl)
                val number = TextView(this).apply {
                    text = m.toString()
                    layoutParams = ViewGroup.LayoutParams(dim, dim)
                    setTextColor(resources.getColor(if (m <= taskProvider.multiplier) R.color.fieldNumberActive else R.color.fieldNumberInactive))
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_END
                    setPadding(0, 0, 10, 0)
                }
                hl.addView(number)

                for(n in 1..taskProvider.multiplier) {
                    val v1 = (when {
                        m <= taskProvider.hintFrom -> CellView(this, CellState.Filled)
                        m <= taskProvider.multiplier -> CellView(this, CellState.ToBeFilled, CellState.Filled, CellAnimationDirection.TopToBottom)
                        else -> CellView(this, CellState.Empty)
                    }).apply {
                        setPadding(3,3,3,3)
                        layoutParams = ViewGroup.LayoutParams(dim, dim)
                    }
                    if(m in taskProvider.hintFrom..taskProvider.multiplier)
                        animatedCells.add(v1)
                    hl.addView(v1)
                }
            }

            val va = ValueAnimator.ofInt(0, 10000).apply {
                addUpdateListener {
                    val value = animatedValue as Int
                    for(c in animatedCells)
                        c.level = value
                }
                duration = 1000
                start()
            }
        }
    }

    private fun onBsKey() {
        answer = if(answer == null || answer!! < 10)
            null
        else
            answer!!/10
        updateTask()
    }

    private fun onNumKey(value: Int) {
        answer = if (answer == null || answer == 0)
            value
        else
            10 * answer!! + value
        updateTask()
    }

    private fun updateTask() {
        findViewById<TextView>(R.id.tv_task).text = "${taskProvider.multiplier}  \u00D7  ${taskProvider.multiplicand}  =  "
        findViewById<TextView>(R.id.tv_answer).text = answer?.toString()?:""
    }
}
