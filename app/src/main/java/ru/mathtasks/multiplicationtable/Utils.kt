package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.CompletableSubject

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

fun Resources.getDuration(resourceId: Int): Long {
    return this.getInteger(resourceId).toLong()
}

fun View.alphaAnimator(duration: Long, fromAlpha: Float, toAlpha: Float): Animator {
    return ObjectAnimator.ofFloat(this, View.ALPHA, fromAlpha, toAlpha).setDuration(duration)
}

fun List<Animator>.toCompletable(): Completable {
    if (isEmpty())
        return Completable.fromSingle(Single.just(0))
    return AnimatorSet().apply { playTogether(this@toCompletable) }.toCompletable()
}

fun Animator?.toCompletable(): Completable {
    if (this == null)
        return Completable.fromSingle(Single.just(0))
    val animationSubject = CompletableSubject.create()
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            animationSubject.onComplete()
        }
    })
    return animationSubject
        .doOnSubscribe { start() }
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
}