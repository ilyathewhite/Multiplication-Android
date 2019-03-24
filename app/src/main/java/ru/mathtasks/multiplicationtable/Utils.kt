package ru.mathtasks.multiplicationtable


import android.animation.*
import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.transition.Transition
import android.support.transition.TransitionListenerAdapter
import android.support.transition.TransitionManager
import android.support.transition.TransitionValues
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.*
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val PARAMS = "params"
const val STATE = "state"

fun <T> SDK(sdk: Int, value: T, default: T): T = if (Build.VERSION.SDK_INT >= sdk) value else default

fun View.setBackgroundCompat(value: Drawable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        this.background = value
    else
        this.setBackgroundDrawable(value)
}

fun Resources.getFloat(resourceId: Int): Float {
    val typedValue = TypedValue()
    getValue(resourceId, typedValue, true)
    return typedValue.float
}

fun View.alphaAnimator(alpha: Float, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofFloat(this, View.ALPHA, this.alpha, alpha).setDuration(duration).apply { setStartDelay(startDelay) }
}

fun TextView.textColorAnimator(color: Int, duration: Long, startDelay: Long = 0): Animator {
    return ObjectAnimator.ofObject(this, "textColor", ArgbEvaluator(), this.currentTextColor, color).setDuration(duration).apply { setStartDelay(startDelay) }
}

suspend fun Animation.run(v: View): Unit = suspendCoroutine { cont ->
    this@run.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationEnd(animation: Animation?) {
            cont.resume(Unit)
        }

        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationStart(animation: Animation?) {}
    })
    v.startAnimation(this@run)
}

suspend fun Animator.run(): Unit = suspendCoroutine { cont ->
    this@run.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            cont.resume(Unit)
        }
    })
    this@run.start()
}

suspend fun Drawable.avdSuspendStartAnimation(): Unit = suspendCoroutine { cont ->
    when {
        this is AnimatedVectorDrawableCompat -> {
            this.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    cont.resume(Unit)
                }
            })
            this.start()
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && this is AnimatedVectorDrawable -> {
            this.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    cont.resume(Unit)
                }
            })
            this.start()
        }
        else -> Log.w("MultiplicationTable", "Drawable of type '${this.javaClass}' cannot be animated on device with SDK ${Build.VERSION.SDK_INT}")
    }
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
    private var supervisorJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() {
            if (supervisorJob == null)
                supervisorJob = SupervisorJob()
            return supervisorJob!! + Dispatchers.Main
        }

    override fun onPause() {
        super.onPause()
        supervisorJob?.cancel()
        supervisorJob = null
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

fun Activity.fullScreen() {
    window.decorView.systemUiVisibility = (SDK(android.os.Build.VERSION_CODES.JELLY_BEAN, android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE, 0)
            or SDK(android.os.Build.VERSION_CODES.JELLY_BEAN, android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION, 0)
            or SDK(android.os.Build.VERSION_CODES.JELLY_BEAN, android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, 0)
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or SDK(android.os.Build.VERSION_CODES.JELLY_BEAN, android.view.View.SYSTEM_UI_FLAG_FULLSCREEN, 0)
            or SDK(android.os.Build.VERSION_CODES.KITKAT, android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY, 0))
}

fun string2GraphicsPath(s: String): Path {
    val path = Path()
    val m = Pattern.compile("(M|L) (\\d+\\.?\\d*) (\\d+\\.?\\d*)").matcher(s)
    while (m.find()) {
        try {
            val x = m.group(2).toFloat();
            val y = m.group(3).toFloat()
            when (m.group(1)) {
                "M" -> path.moveTo(x, y)
                "L" -> path.lineTo(x, y)
            }
        } catch (e: NumberFormatException) {
            Log.e("string2GraphicsPath", "Parsing path '$s': '${m.group(2)}' or '${m.group(3)}' not a float")
        }
    }
    return path
}

abstract class MyViewModel<Params : Parcelable, State : Parcelable, Update> : ViewModel() {
    private var initialized = false
    protected lateinit var s: State
        private set
    val state get() = s

    private val eventUpdate = SingleLiveEvent<Update>()

    fun observe(owner: LifecycleOwner, observer: (Update) -> Unit) {
        eventUpdate.observe(owner, Observer { upd ->
            if (upd != null)
                observer(upd)
        })
    }

    protected fun update(upd: Update) {
        this.eventUpdate.value = upd
    }

    abstract fun create(params: Params): State
    open fun onAttached() {}
    fun init(savedInstanceState: Bundle?, intent: Intent) {
        if (initialized) {
            onAttached()
            return
        }
        initialized = true
        s = when {
            savedInstanceState != null -> savedInstanceState.getParcelable(STATE) as State
            else -> create(intent.getParcelableExtra(PARAMS) as Params)
        }
        onAttached()
    }
}

inline fun <reified VM : MyViewModel<*, *, *>> getViewModel(activity: FragmentActivity, savedInstanceState: Bundle?) =
    ViewModelProviders.of(activity).get(VM::class.java).apply { init(savedInstanceState, activity.intent) }

suspend fun CoroutineScope.parallel(vararg functions: suspend CoroutineScope.() -> Unit) = coroutineScope {
    functions.map { func -> async { func() } }.map { it.await() }
}
