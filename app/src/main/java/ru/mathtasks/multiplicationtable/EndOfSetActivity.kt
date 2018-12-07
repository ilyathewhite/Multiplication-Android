package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.Transition
import android.support.transition.TransitionListenerAdapter
import android.support.transition.TransitionManager
import android.support.v7.app.AppCompatActivity
import android.view.animation.AccelerateInterpolator
import kotlinx.android.synthetic.main.activity_end_of_set.*


class EndOfSetActivity : AppCompatActivity() {
    companion object {
        const val INPUT_Q_ERRORS = "ru.mathtasks.multiplicationtable.end_of_set_activity.q_errors"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_of_set)

        btnNextDrill.setOnClickListener {
            setResult(Activity.RESULT_OK, null)
            finish()
        }

        btnEndPractice.setOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        clBadge.post {
            TransitionManager.beginDelayedTransition(clBadge, ChangeBounds().apply {
                interpolator = AccelerateInterpolator(1.0f)
                duration = Settings.EndOfSetCheckMarkAnimationDuration
                addListener(object : TransitionListenerAdapter() {
                    override fun onTransitionEnd(transition: Transition) {
                        animateOuter()
                    }
                })
            })

            ConstraintSet().apply {
                clone(clBadge)
                setHorizontalBias(R.id.ivMarkCover, 0.8f)
                constrainPercentWidth(R.id.ivMarkCover, 0f)
                applyTo(clBadge)
            }
        }
    }

    private fun animateOuter() {
        TransitionManager.beginDelayedTransition(clOuter, ChangeBounds().apply {
            interpolator = AccelerateInterpolator(1.0f)
            duration = Settings.EndOfSetSuccessBadgeAnimationDuration
        })

        ConstraintSet().apply {
            clone(clOuter)
            setVerticalBias(R.id.clBadge, 0.1f)
            setVerticalBias(R.id.llButtons, 0.9f)
            applyTo(clOuter)
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, null)
        finish()
    }
}