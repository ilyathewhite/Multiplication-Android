package ru.mathtasks.multiplicationtable

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View

fun View.setBackgroundCompat(value: Drawable?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        this.background = value
    else
        this.setBackgroundDrawable(value)
}