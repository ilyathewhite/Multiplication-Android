package ru.mathtasks.multiplicationtable

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.input_view.*


class InputFragment : Fragment() {
    companion object {
        private const val STATE_ANSWER = "answer"
    }

    interface OnEventListener {
        fun onAnswerChanged(answer: Int?)
        fun onOkPressed(answer: Int)
    }

    var answer: Int? = null
        private set
    private lateinit var listener: OnEventListener

    fun resetAnswer() {
        answer = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.input_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        class B(val button: Button, val value: Int)

        val buttons = arrayOf(
            B(btn1, 1),
            B(btn2, 2),
            B(btn3, 3),
            B(btn4, 4),
            B(btn5, 5),
            B(btn6, 6),
            B(btn7, 7),
            B(btn8, 8),
            B(btn9, 9),
            B(btn0, 0)
        )
        buttons.forEach { b -> b.button.setOnClickListener { onNumKey(b.value) } }
        btnBs.setOnClickListener { onBsKey() }
        btnOk.setOnClickListener { onOkKey() }

        view.viewTreeObserver.addOnGlobalLayoutListener {
            (buttons.map { it.button } + listOf(btnBs, btnOk)).autoSizeText(ResourcesCompat.getFont(activity!!, R.font.lato_bold)!!, 0.4f)
        }

        answer = if (savedInstanceState?.containsKey(STATE_ANSWER) == true) savedInstanceState.getInt(STATE_ANSWER) else null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (answer != null)
            outState.putInt(STATE_ANSWER, answer!!)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnEventListener) {
            listener = context
        } else {
            throw ClassCastException(context.toString() + " must implement InputFragment.OnEventListener")
        }
    }

    private fun onBsKey() {
        answer = if (answer == null || answer!! < 10) null else answer!! / 10
        listener.onAnswerChanged(answer)
    }

    private fun onNumKey(value: Int) {
        answer = if (answer == null || answer == 0) value else if (answer!! >= 100) answer!! else 10 * answer!! + value
        listener.onAnswerChanged(answer)
    }

    private fun onOkKey() {
        if (answer != null)
            listener.onOkPressed(answer!!)
    }
}