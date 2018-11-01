package ru.mathtasks.multiplicationtable

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.widget.Button

class MainActivity : Activity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        class B(val button_id:Int, val multiplicand: Int);
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
            B(R.id.btn_10, 10));
        for(b in buttons) {
            findViewById<Button>(b.button_id).setOnClickListener { _ ->
                startActivity(Intent(this, TrainingActivity::class.java).apply {
                    putExtra(TRAINING_ACTIVITY_MULTIPLICAND, b.multiplicand)
                })
            }
        }
    }
}
