package ru.mathtasks.multiplicationtable

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import kotlinx.android.synthetic.main.progress_view.view.*
import android.support.constraint.ConstraintSet
import android.support.transition.ChangeBounds

class ProgressView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    init {
        inflate(context, R.layout.progress_view, this)
        context.obtainStyledAttributes(attrs, R.styleable.ProgressView).apply {
            progress = getFloat(R.styleable.ProgressView_progress, 0f)
            recycle()
        }
    }

    var progress: Float
        set(value) {
            field = value
            ConstraintSet().apply {
                clone(this@ProgressView.clOuter)
                constrainPercentWidth(R.id.vProgress, value)
                applyTo(this@ProgressView.clOuter)
            }
        }

    suspend fun animateProgress(toProgress: Float, duration: Long) {
        clOuter.transition(duration, ChangeBounds()) {
            progress = toProgress
        }
    }
}