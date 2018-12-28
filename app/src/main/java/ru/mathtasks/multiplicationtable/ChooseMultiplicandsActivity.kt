package ru.mathtasks.multiplicationtable

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
import android.view.MenuItem
import android.widget.Button
import kotlinx.android.synthetic.main.activity_choose_multiplicands.*

class ChooseMultiplicandsActivity : ScopedAppActivity() {
    companion object {
        private const val STATE_SELECTED_MULTIPLICANDS = "selectedMultiplicands"
    }

    private lateinit var selectedMultiplicands: MutableSet<Int>
    private lateinit var multiplicand2button: Map<Int, Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_multiplicands)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        multiplicand2button = mapOf(
            1 to btn1,
            2 to btn2,
            3 to btn3,
            4 to btn4,
            5 to btn5,
            6 to btn6,
            7 to btn7,
            8 to btn8,
            9 to btn9,
            10 to btn10
        )

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        multiplicand2button.forEach { (multiplicand, button) ->
            button.setOnClickListener {
                btnClick(multiplicand)
            }
        }

        llFrame.viewTreeObserver.addOnGlobalLayoutListener {
            multiplicand2button.map { (_, button) -> button }.autoSizeText(ResourcesCompat.getFont(this, R.font.lato_bold)!!, 0.4f)
        }

        btnStart.setOnClickListener {
            startActivity(Intent(this, TrainingActivity::class.java).apply {
                putExtra(TestActivity.PARAM_MULTIPLICANDS, selectedMultiplicands.toTypedArray())
            })
        }

        btnStart.typeface = ResourcesCompat.getFont(this, R.font.lato_regular)

        selectedMultiplicands = savedInstanceState?.getIntArray(STATE_SELECTED_MULTIPLICANDS)?.toMutableSet() ?: HashSet()
        btnStart.isEnabled = !selectedMultiplicands.isEmpty()
        for (m in selectedMultiplicands)
            multiplicand2button[m]!!.isPressed = true
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putIntArray(STATE_SELECTED_MULTIPLICANDS, selectedMultiplicands.toIntArray())
    }

    private fun btnClick(multiplicand: Int) {
        if (selectedMultiplicands.contains(multiplicand))
            selectedMultiplicands.remove(multiplicand)
        else {
            selectedMultiplicands.add(multiplicand)
            multiplicand2button[multiplicand]!!.post {
                multiplicand2button[multiplicand]!!.isPressed = selectedMultiplicands.contains(multiplicand)
            }
        }
        btnStart.isEnabled = !selectedMultiplicands.isEmpty()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId == android.R.id.home) {
            this.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}