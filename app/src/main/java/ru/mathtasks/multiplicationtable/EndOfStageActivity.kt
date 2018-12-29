package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.Fade
import android.support.transition.TransitionSet
import android.view.View
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
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        launch {
            clBadge.transition(Settings.EndOfStageCheckMarkAnimationDuration, ChangeBounds()) {
                ConstraintSet().apply {
                    clone(clBadge)
                    setHorizontalBias(R.id.ivMarkCover, 0.8f)
                    constrainPercentWidth(R.id.ivMarkCover, 0f)
                    applyTo(clBadge)
                }
            }

            clOuter.transition(Settings.EndOfStageSuccessBadgeAnimationDuration, TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds()).addTransition(Fade())
            }) {
                ConstraintSet().apply {
                    clone(clOuter)
                    setVerticalBias(R.id.clBadge, 0.2f)
                    clear(R.id.llButtons)
                    connect(R.id.llButtons, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
                    connect(R.id.llButtons, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
                    connect(R.id.llButtons, ConstraintSet.TOP, R.id.clBadge, ConstraintSet.BOTTOM)
                    setMargin(R.id.llButtons, ConstraintSet.TOP, resources.getDimensionPixelSize(R.dimen.endOfStageSpace))
                    constrainWidth(R.id.llButtons, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    constrainHeight(R.id.llButtons, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    applyTo(clOuter)
                }
                llButtons.visibility = View.VISIBLE
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, null)
        finish()
    }
}