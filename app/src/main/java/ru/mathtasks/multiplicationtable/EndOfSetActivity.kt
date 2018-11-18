package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.Transition
import android.support.transition.TransitionListenerAdapter
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.view.animation.AccelerateInterpolator
import android.widget.Button


class EndOfSetActivity : AppCompatActivity() {
    companion object {
        const val INPUT_Q_ERRORS = "ru.mathtasks.multiplicationtable.end_of_set_activity.q_errors"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_of_set)

        findViewById<Button>(R.id.btn_next_drill).setOnClickListener {
            setResult(Activity.RESULT_OK, null)
            finish()
        }

        findViewById<Button>(R.id.btn_end_practice).setOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        val badge = findViewById<ConstraintLayout>(R.id.cl_badge)
        badge.post {
            TransitionManager.beginDelayedTransition(badge, ChangeBounds().apply {
                interpolator = AccelerateInterpolator(1.0f)
                duration = resources.getInteger(R.integer.endOfSetCheckMarkAnimationDuration).toLong()
                addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        animateOuter()
                    }
                })
            })

            ConstraintSet().apply {
                clone(badge)
                setHorizontalBias(R.id.iv_mark_cover, 0.8f)
                constrainPercentWidth(R.id.iv_mark_cover, 0f)
                applyTo(badge)
            }
        }
    }

    private fun animateOuter() {
        val outer = findViewById<ConstraintLayout>(R.id.cl_outer)
        TransitionManager.beginDelayedTransition(outer, ChangeBounds().apply {
            interpolator = AccelerateInterpolator(1.0f)
            duration = resources.getInteger(R.integer.endOfSetSuccessBadgeAnimationDuration).toLong()
        })

        ConstraintSet().apply {
            clone(outer)
            setVerticalBias(R.id.cl_badge, 0.1f)
            setVerticalBias(R.id.ll_buttons, 0.9f)
            applyTo(outer)
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, null)
        finish()
    }
}