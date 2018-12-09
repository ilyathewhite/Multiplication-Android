package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.transition.*
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.android.synthetic.main.task_view.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun View.setBackgroundCompat(value: Drawable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        this.background = value
    else
        this.setBackgroundDrawable(value)
}

fun Resources.getFloat(resourceId: Int): Float {
    val typedValue = TypedValue();
    getValue(resourceId, typedValue, true);
    return typedValue.float;
}

class Location(val X: Int, val Y: Int)

fun View.getLocationOnScreen(): Location {
    val loc: IntArray = intArrayOf(0, 0)
    this.getLocationOnScreen(loc)
    return Location(loc[0], loc[1])
}

fun View.alphaAnimator(alpha: Float, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofFloat(this, View.ALPHA, this.alpha, alpha).setDuration(duration).apply { setStartDelay(startDelay) }
}

fun TextView.textColorAnimator(color: Int, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofObject(this, "textColor", ArgbEvaluator(), this.currentTextColor, color).setDuration(duration).apply { setStartDelay(startDelay) }
}

fun TextView.textSizeAnimator(pxSize: Float, duration: Long, startDelay: Long = 0) = ValueAnimator.ofFloat(0f, 1f).apply {
    this.duration = duration
    this.startDelay = startDelay
    val startPxSize = textSize
    addUpdateListener { this@textSizeAnimator.setTextSize(TypedValue.COMPLEX_UNIT_PX, startPxSize + (pxSize - startPxSize) * (it.animatedValue as Float)) }
}

suspend fun Animator.run(): Unit = suspendCoroutine { cont ->
    this@run.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            cont.resume(Unit)
        }
    })
    this@run.start()
}

suspend fun ConstraintLayout.transition(duration: Long, transition: Transition, layoutUpdate: () -> Unit): Unit = suspendCoroutine { cont ->
    TransitionManager.beginDelayedTransition(this, transition.apply {
        this@apply.duration = duration
        addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                cont.resume(Unit)
            }
        })
    })
    layoutUpdate()
}

class ScaleTransition : Transition() {
    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    private fun captureValues(values: TransitionValues) {
        values.values[PROPNAME_SCALE_X] = values.view.scaleX
        values.values[PROPNAME_SCALE_Y] = values.view.scaleY
    }

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        if (endValues == null || startValues == null)
            return null    // no values

        val startX = startValues.values[PROPNAME_SCALE_X] as Float
        val startY = startValues.values[PROPNAME_SCALE_Y] as Float
        val endX = endValues.values[PROPNAME_SCALE_X] as Float
        val endY = endValues.values[PROPNAME_SCALE_Y] as Float

        if (startX == endX && startY == endY)
            return null    // no scale to run

        val view = startValues.view
        val propX = PropertyValuesHolder.ofFloat(PROPNAME_SCALE_X, startX, endX)
        val propY = PropertyValuesHolder.ofFloat(PROPNAME_SCALE_Y, startY, endY)
        val valAnim = ValueAnimator.ofPropertyValuesHolder(propX, propY)
        valAnim.addUpdateListener { valueAnimator ->
            view.apply {
                pivotY = height / 2f
                pivotX = width / 2f
                scaleX = valueAnimator.getAnimatedValue(PROPNAME_SCALE_X) as Float
                scaleY = valueAnimator.getAnimatedValue(PROPNAME_SCALE_Y) as Float
            }
        }
        return valAnim
    }

    companion object {
        private val PROPNAME_SCALE_X = "PROPNAME_SCALE_X"
        private val PROPNAME_SCALE_Y = "PROPNAME_SCALE_Y"
    }
}

suspend fun List<Animator>.run(): Unit = this.merge().run()

fun List<Animator>.merge(): Animator = AnimatorSet().apply { playTogether(this@merge) }

abstract class ScopedAppActivity : AppCompatActivity(), CoroutineScope {
    private var job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

fun Iterable<Button>.autoSizeText(typeface: Typeface) {
    val p = Paint().apply {
        textSize = 100f
        this.typeface = typeface
    }
    val needHeight = this.map { button -> val bounds = Rect(); p.getTextBounds(button.text.toString(), 0, button.text.length, bounds); bounds.height() }.max()
    val needWidth = this.map { button -> p.measureText(button.text.toString()) }.max()
    val haveWidth = this.map { button -> button.width - button.paddingLeft - button.paddingRight }.min()
    val haveHeight = this.map { button -> button.height - button.paddingTop - button.paddingBottom }.min()
    val textSize = Math.min(100f * haveHeight!! / needHeight!!, 100f * haveWidth!! / needWidth!!) * 1 / 2
    this.forEach {
        it.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        it.typeface = typeface
    }
}