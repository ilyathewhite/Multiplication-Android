package ru.mathtasks.multiplicationtable

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View

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
fun View.getLocationOnScreen() : Location {
    val loc: IntArray = intArrayOf(0, 0)
    this.getLocationOnScreen(loc)
    return Location(loc[0], loc[1])
}

fun Resources.getDuration(resourceId: Int) : Long {
    return this.getInteger(resourceId).toLong()
}