package ru.mathtasks.multiplicationtable

import android.animation.*
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.util.TypedValue
import android.view.View
import io.reactivex.Completable
import io.reactivex.Single
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

fun View.alphaAnimation(duration: Long, toAlpha: Float): Animator? {
    if (this.alpha == toAlpha)
        return null
    return ObjectAnimator.ofFloat(this, View.ALPHA, this.alpha, toAlpha).setDuration(duration)
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
    return animationSubject.doOnSubscribe { this.start() }
}

//fun List<Animator>.merge(): Animator? = when (this.size) {
//    0 -> null
//    1 -> this[0]
//    else -> AnimatorSet().apply { playTogether(this) }
//}
//
//@JvmName("merge_nullables")
//fun List<Animator?>.merge(): Animator? = (this.filter { it != null } as ArrayList<Animator>).merge()
