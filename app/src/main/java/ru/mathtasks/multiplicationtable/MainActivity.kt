package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

enum class TaskType { Learn, Practice, Test }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ttvLearn.setOnClickListener {
            startActivity(Intent(this, ChooseMultiplicandActivity::class.java).apply {
                putExtra(CHOOSE_MULTIPLICAND_ACTIVITY_TASK_TYPE, TaskType.Learn)
            })
        }
        ttvPractice.setOnClickListener {
            startActivity(Intent(this, ChooseMultiplicandActivity::class.java).apply {
                putExtra(CHOOSE_MULTIPLICAND_ACTIVITY_TASK_TYPE, TaskType.Practice)
            })
        }
        ttvTest.setOnClickListener { }
    }
}
