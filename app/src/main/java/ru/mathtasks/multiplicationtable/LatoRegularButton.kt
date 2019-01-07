package ru.mathtasks.multiplicationtable

import android.content.Context
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.widget.Button


class LatoRegularButton(context: Context, attrs: AttributeSet) : Button(context, attrs) {
    init {
        typeface = ResourcesCompat.getFont(context, R.font.lato_regular)
    }
}