package ru.mathtasks.multiplicationtable

import android.app.Activity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.transition.*
import android.view.animation.AccelerateInterpolator


const val END_OF_SET_ACTIVITY_Q_ERRORS = "ru.mathtasks.multiplicationtable.end_of_set_activity.q_errors"

class EndOfSetActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_of_set)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if(!hasFocus)
            return;

        val scene2 = Scene.getSceneForLayout(findViewById<ConstraintLayout>(R.id.constraintLayout),R.layout.activity_end_of_set2,this)

        TransitionManager.go(scene2, ChangeBounds() . apply {
            interpolator = AccelerateInterpolator(1.0f)
            duration = 1200
            addListener(object : TransitionListenerAdapter() {
                override fun onTransitionEnd(p0: Transition) {
                    val scene3 = Scene.getSceneForLayout(findViewById<ConstraintLayout>(R.id.constraintLayout),R.layout.activity_end_of_set3,this@EndOfSetActivity)
                    TransitionManager.go(scene3, ChangeBounds() . apply {
                        interpolator = AccelerateInterpolator(1.0f)
                        duration = 1200
                    })
                }
            })
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}