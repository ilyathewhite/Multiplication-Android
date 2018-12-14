package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.transition.Transition
import android.support.transition.TransitionListenerAdapter
import android.support.transition.TransitionManager
import android.support.transition.TransitionValues
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun <T> SDK(sdk: Int, value: T, default: T): T = if (Build.VERSION.SDK_INT >= sdk) value else default

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

fun View.alphaAnimator(alpha: Float, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofFloat(this, View.ALPHA, this.alpha, alpha).setDuration(duration).apply { setStartDelay(startDelay) }
}

fun TextView.textColorAnimator(color: Int, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofObject(this, "textColor", ArgbEvaluator(), this.currentTextColor, color).setDuration(duration).apply { setStartDelay(startDelay) }
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
        values.values[PROP_SCALE_X] = values.view.scaleX
        values.values[PROP_SCALE_Y] = values.view.scaleY
    }

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        if (endValues == null || startValues == null)
            return null    // no values

        val startX = startValues.values[PROP_SCALE_X] as Float
        val startY = startValues.values[PROP_SCALE_Y] as Float
        val endX = endValues.values[PROP_SCALE_X] as Float
        val endY = endValues.values[PROP_SCALE_Y] as Float

        if (startX == endX && startY == endY)
            return null    // no scale to run

        val view = startValues.view
        val propX = PropertyValuesHolder.ofFloat(PROP_SCALE_X, startX, endX)
        val propY = PropertyValuesHolder.ofFloat(PROP_SCALE_Y, startY, endY)
        val valueAnimator = ValueAnimator.ofPropertyValuesHolder(propX, propY)
        valueAnimator.addUpdateListener { a ->
            view.apply {
                pivotY = height / 2f
                pivotX = width / 2f
                scaleX = a.getAnimatedValue(PROP_SCALE_X) as Float
                scaleY = a.getAnimatedValue(PROP_SCALE_Y) as Float
            }
        }
        return valueAnimator
    }

    companion object {
        private const val PROP_SCALE_X = "PROP_SCALE_X"
        private const val PROP_SCALE_Y = "PROP_SCALE_Y"
    }
}

suspend fun List<Animator>.run(): Unit = this.playTogether().run()

fun List<Animator>.playTogether(): Animator = AnimatorSet().apply { playTogether(this@playTogether) }
fun List<Animator>.playSequentially(): Animator = AnimatorSet().apply { playSequentially(this@playSequentially) }

abstract class ScopedAppActivity : AppCompatActivity(), CoroutineScope {
    private var superviserJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() {
            if (superviserJob == null)
                superviserJob = SupervisorJob()
            return superviserJob!! + Dispatchers.Main
        }

    override fun onPause() {
        super.onPause()
        superviserJob?.cancel()
        superviserJob = null
    }
}

fun Iterable<Button>.autoSizeText(typeface: Typeface, part: Float) {
    val p = Paint().apply {
        textSize = 100f
        this.typeface = typeface
    }
    val needHeight = this.map { button -> val bounds = Rect(); p.getTextBounds(button.text.toString(), 0, button.text.length, bounds); bounds.height() }.max()
    val needWidth = this.map { button -> p.measureText(button.text.toString()) }.max()
    val haveWidth = this.map { button -> button.width - button.paddingLeft - button.paddingRight }.min()
    val haveHeight = this.map { button -> button.height - button.paddingTop - button.paddingBottom }.min()
    val textSize = Math.min(100f * haveHeight!! / needHeight!!, 100f * haveWidth!! / needWidth!!) * part
    for (button in this) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        button.typeface = typeface
    }
}

fun View.onLayoutOnce(action: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            else
                viewTreeObserver.removeGlobalOnLayoutListener(this)
            action()
        }
    })
}