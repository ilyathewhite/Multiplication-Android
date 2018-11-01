package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.View
import android.view.ViewGroup
import android.widget.*

import kotlinx.android.synthetic.main.activity_training.*

const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

class TrainingActivity : Activity() {

    private var multiplier = 0;
    private var multiplicand = 0;
    private var answer : Int? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training)

        this.multiplier = 1
        this.multiplicand = intent.getIntExtra(TRAINING_ACTIVITY_MULTIPLICAND, 0)

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

        findViewById<Button>(R.id.btn_bs).setOnClickListener { _ -> onBsKey() }
        updateTask()

        val field = findViewById<LinearLayout>(R.id.ll_field)

        for(m in 1..10) {
            val hl = LinearLayout(this) .apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                orientation = android.widget.LinearLayout.HORIZONTAL
            }
            field.addView(hl)
            for(n in 1..multiplicand) {
                val v1 = CellView(this) .apply {
                    State = when(n % 4) { 0 -> CellState.Empty 1 -> CellState.Filled 2 -> CellState.WasEmptied else -> CellState.ToBeFilled }
                    layoutParams = ViewGroup.LayoutParams(60, 60)
                }
                hl.addView(v1)
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
        findViewById<TextView>(R.id.tv_task).text = "$multiplier  \u00D7  $multiplicand  =  "
        findViewById<TextView>(R.id.tv_answer).text = answer?.toString()?:""
    }
}
