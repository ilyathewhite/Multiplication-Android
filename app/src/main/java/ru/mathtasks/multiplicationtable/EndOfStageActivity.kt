package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import kotlinx.android.synthetic.main.activity_end_of_stage.*
import kotlinx.coroutines.launch


class EndOfStageActivity : ScopedAppActivity() {
    companion object {
        const val PARAM_Q_ERRORS = "qErrors"
        const val PARAM_PROGRESS = "progress"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_of_stage)

        pvProgress.progress = intent.getFloatExtra(PARAM_PROGRESS, 0f)

        btnNextDrill.setOnClickListener {
            setResult(Activity.RESULT_OK, null)
            finish()
        }

        btnEndPractice.setOnClickListener {
            setResult(Activity.RESULT_CANCELED, null)
            finish()
        }

        clOuter.post {
            launch {
                clBadge.transition(Settings.EndOfStageCheckMarkAnimationDuration, ChangeBounds()) {
                    ConstraintSet().apply {
                        clone(clBadge)
                        setHorizontalBias(R.id.ivMarkCover, 0.8f)
                        constrainPercentWidth(R.id.ivMarkCover, 0f)
                        applyTo(clBadge)
                    }
                }

                clOuter.transition(Settings.EndOfStageSuccessBadgeAnimationDuration, ChangeBounds()) {
                    ConstraintSet().apply {
                        clone(clOuter)
                        setVerticalBias(R.id.clBadge, 0.1f)
                        setVerticalBias(R.id.llButtons, 0.9f)
                        applyTo(clOuter)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, null)
        finish()
    }
}