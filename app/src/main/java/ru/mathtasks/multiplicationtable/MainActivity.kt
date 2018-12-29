package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

enum class TaskType { Learn, Practice, Test }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        for ((ttv, type) in mapOf(ttvLearn to TaskType.Learn, ttvPractice to TaskType.Practice)) {
            ttv.setOnClickListener {
                startActivity(Intent(this, ChooseMultiplicandActivity::class.java).apply {
                    putExtra(ChooseMultiplicandActivity.PARAM_TASK_TYPE, type)
                })
            }
        }
        ttvTest.setOnClickListener {
            startActivity(Intent(this, ChooseMultiplicandsActivity::class.java))

//            startActivityForResult(Intent(this, EndOfStageActivity::class.java).apply {
//                putExtra(EndOfStageActivity.PARAM_Q_ERRORS, 0)
//                putExtra(EndOfStageActivity.PARAM_PROGRESS, 0.5f)
//            }, 0)

        }
    }
}