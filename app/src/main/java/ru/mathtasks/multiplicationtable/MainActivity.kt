package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        class B(val button: Button, val multiplicand: Int);
        val allButtons = arrayOf(
            B(btn1, 1),
            B(btn2, 2),
            B(btn3, 3),
            B(btn4, 4),
            B(btn5, 5),
            B(btn6, 6),
            B(btn7, 7),
            B(btn8, 8),
            B(btn9, 9),
            B(btn10, 10)
        )
        allButtons.forEach { b ->
            b.button.setOnClickListener {
                startActivity(Intent(this, TrainingActivity::class.java).apply {
                    putExtra(TRAINING_ACTIVITY_TASK_PROVIDER, TaskProvider.FromPractice(b.multiplicand))
                })
            }
        }
        btn1.post { allButtons.map { it.button }.autoSizeText(Typeface.DEFAULT) }
    }
}
