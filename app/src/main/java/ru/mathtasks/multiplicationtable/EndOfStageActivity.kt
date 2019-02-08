package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds
import android.support.transition.Fade
import android.support.transition.TransitionSet
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_end_of_stage.*
import kotlinx.coroutines.launch


@Parcelize
class EndOfStageActivityActivityParams(
    val qErrors: Int,
    val progress: Float
) : Parcelable

class EndOfStageActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_of_stage)

        val params = intent.getParcelableExtra<EndOfStageActivityActivityParams>(PARAMS)
        pvProgress.progress = params.progress

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
            ivMark.drawable.avdSuspendStartAnimation()

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