package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.util.TypedValue
import android.view.View
import io.reactivex.Completable
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

fun View.animate(duration: Long, animator: (ViewPropertyAnimatorCompat) -> ViewPropertyAnimatorCompat): Completable {
    val animationSubject = CompletableSubject.create()
    return animationSubject.doOnSubscribe {
        animator(ViewCompat.animate(this).setDuration(duration)).withEndAction { animationSubject.onComplete() }
    }
}

fun Animator.toCompletable(): Completable {
    val animationSubject = CompletableSubject.create()
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            animationSubject.onComplete()
        }
    })
    return animationSubject.doOnSubscribe { this.start() }
}