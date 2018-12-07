package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.TextView
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

suspend fun Animator.run(): Unit = suspendCoroutine { cont ->
    this@run.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            cont.resume(Unit)
        }
    })
    this@run.start()
}

suspend fun List<Animator>.run(): Unit = this.merge().run()

fun List<Animator>.merge(): Animator = AnimatorSet().apply { playTogether(this@merge) }

abstract class ScopedAppActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

fun Iterable<Button>.autoSizeText(typeface: Typeface) {
    val p = Paint().apply { textSize = 100f; this.typeface = typeface }
    val needHeight = this.map { button -> val bounds = Rect(); p.getTextBounds(button.text.toString(), 0, button.text.length, bounds); bounds.height() }.max()
    val needWidth = this.map { button -> p.measureText(button.text.toString()) }.max()
    val haveWidth = this.map { button -> button.width - button.paddingLeft - button.paddingRight }.min()
    val haveHeight = this.map { button -> button.height - button.paddingTop - button.paddingBottom }.min()
    val textSize = Math.min(100f * haveHeight!! / needHeight!!, 100f * haveWidth!! / needWidth!!) * 1 / 2
    this.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize); it.typeface = typeface }
}