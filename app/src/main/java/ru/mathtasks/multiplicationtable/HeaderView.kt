package ru.mathtasks.multiplicationtable

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.header_view.view.*

class HeaderView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    init {
        inflate(context, R.layout.header_view, this)
        context.obtainStyledAttributes(attrs, R.styleable.Common).apply {
            tvCaption.text = getString(R.styleable.Common_caption)
            recycle()
        }
    }

    var caption: String
        set(value) {
            tvCaption.text = value
        }
        get() = tvCaption.text.toString()
}