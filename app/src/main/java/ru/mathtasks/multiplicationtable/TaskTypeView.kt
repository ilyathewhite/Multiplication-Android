package ru.mathtasks.multiplicationtable

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import kotlinx.android.synthetic.main.task_type_view.view.*

class TaskTypeView(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    init {
        inflate(context, R.layout.task_type_view, this)
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.TaskTypeView)
        tvCaption.text = attributes.getString(R.styleable.TaskTypeView_caption)
        tvDescription.text = attributes.getString(R.styleable.TaskTypeView_description)
        attributes.recycle()
        isClickable = true
    }
}