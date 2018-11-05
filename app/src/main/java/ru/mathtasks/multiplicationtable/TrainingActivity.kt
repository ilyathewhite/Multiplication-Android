package ru.mathtasks.multiplicationtable

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.lang.Math.min
import android.animation.AnimatorSet
import java.lang.Math.max
import java.util.*


const val TRAINING_ACTIVITY_MULTIPLICAND = "ru.mathtasks.multiplicationtable.trainingactivity.multiplicand"

enum class CellState { Empty, Filled, ToBeFilled, WasEmptied }
enum class AnimationDirection { LeftToRight, RightToLeft, TopToBottom }
enum class DrawFieldAnimation { None, PerRow, PerCell }

class TrainingActivity : Activity() {
    private lateinit var taskProvider: TaskProvider
	private lateinit var field: LinearLayout
	private var answer : Int? = null
    private var animation = DrawFieldAnimation.None

	override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_training)

		this.taskProvider = TaskProvider(intent.getIntExtra(TRAINING_ACTIVITY_MULTIPLICAND, 0))
		this.field = findViewById(R.id.ll_field)

		class B(val button_id:Int, val value: Int);
        val buttons = arrayOf(
            B(R.id.btn_1, 1),
            B(R.id.btn_2, 2),
            B(R.id.btn_3, 3),
            B(R.id.btn_4, 4),
            B(R.id.btn_5, 5),
            B(R.id.btn_6, 6),
            B(R.id.btn_7, 7),
            B(R.id.btn_8, 8),
            B(R.id.btn_9, 9),
            B(R.id.btn_0, 0));
        for(b in buttons)
            findViewById<Button>(b.button_id).setOnClickListener { onNumKey(b.value) }

        findViewById<Button>(R.id.btn_bs).setOnClickListener { onBsKey() }
        findViewById<Button>(R.id.btn_ok).setOnClickListener { onOkKey() }
        updateTask()

        field.post { drawField() }
    }

	private fun drawField()
	{
		field.removeAllViews()
		val dim = min(field.width/(taskProvider.multiplier+2), field.height/10)		// 1 extra before, 1 extra after

		val animatedDrawables = LinkedList<ClipDrawable>()
		for(m in 1..10) {
			val hl = LinearLayout(this).apply {
				layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				orientation = android.widget.LinearLayout.HORIZONTAL
			}
			field.addView(hl)

			val number = TextView(this).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
				text = m.toString()
                textSize = min(dim/5f,20f)
				layoutParams = ViewGroup.LayoutParams(dim, dim)
				setTextColor(resources.getColor(if (m <= taskProvider.multiplicand) R.color.fieldNumberActive else R.color.fieldNumberInactive))
				setPadding(0, 0, 10, 0)
            }
			hl.addView(number)

            var r = when {
                m > max(taskProvider.hintFrom, taskProvider.multiplicand) -> row(CellState.Empty, dim)
                m <= min(taskProvider.hintFrom, taskProvider.multiplicand) ->  row(CellState.Filled, dim)
                animation == DrawFieldAnimation.None -> row(if(taskProvider.hintFrom < taskProvider.multiplicand) CellState.Filled else CellState.Empty, dim)
                taskProvider.hintFrom < taskProvider.multiplicand -> {
                    val ar = animatedRow(CellState.ToBeFilled, CellState.Filled, dim, AnimationDirection.LeftToRight);
                    animatedDrawables.addLast(ar.clipDrawable);
                    ar.drawable;
                }
                else -> {
                    val ar = animatedRow(CellState.Filled, CellState.WasEmptied, dim, AnimationDirection.RightToLeft);
                    animatedDrawables.addFirst(ar.clipDrawable);
                    ar.drawable;
                }
            }

			hl.addView(ImageView(this).apply {
				layoutParams = ViewGroup.LayoutParams(dim * taskProvider.multiplier, dim)
				background = r
            })
		}

        val set = AnimatorSet()
        var prevAnimation : Animator? = null
        for(cd in animatedDrawables) {
            val animation = ObjectAnimator.ofInt(cd, "level", 10000).apply { duration = if(animation == DrawFieldAnimation.PerCell) 200L * taskProvider.multiplier else 800L  }
            val builder = set.play(animation)
            if(prevAnimation!=null)
                builder.after(prevAnimation)
            prevAnimation = animation
        }
        set.start()
	}

    private fun onBsKey() {
        answer = if(answer == null || answer!! < 10)
            null
        else
            answer!!/10
        updateTask()
    }

    private fun onNumKey(value: Int) {
        answer = if (answer == null || answer == 0)
            value
        else
            10 * answer!! + value
        updateTask()
    }

    private fun onOkKey() {
        if (answer != taskProvider.multiplier * taskProvider.multiplicand) {
            AlertDialog.Builder(this).apply {
                setMessage("Ответ не точен")
                setCancelable(false)
                setNegativeButton("OK") { _,_ ->
                    answer = null
                    animation = if (animation == DrawFieldAnimation.None) DrawFieldAnimation.PerRow else DrawFieldAnimation.PerCell
                    updateTask()
                    drawField()
                }
                show()
            }
        }
        else
        {
            answer = null
            animation = DrawFieldAnimation.None
            taskProvider.nextTask()
            updateTask()
            if(!taskProvider.endOfGame && !taskProvider.endOfSet)
                drawField()
        }
    }

    private fun updateTask() {
        findViewById<TextView>(R.id.tv_task).text = "${taskProvider.multiplier}  \u00D7  ${taskProvider.multiplicand}  =  "
        findViewById<TextView>(R.id.tv_answer).text = answer?.toString()?:""
    }

	class AnimatedRow(val drawable: LayerDrawable, val clipDrawable: ClipDrawable);
	private fun animatedRow(stateFrom: CellState, stateTo: CellState, size: Int, dir: AnimationDirection) : AnimatedRow {
		val orientation = if(dir == AnimationDirection.LeftToRight || dir == AnimationDirection.RightToLeft) ClipDrawable.HORIZONTAL else ClipDrawable.VERTICAL
		val clipDrawable2 = ClipDrawable(row(stateTo, size), if(dir == AnimationDirection.LeftToRight) Gravity.LEFT else if(dir == AnimationDirection.RightToLeft) Gravity.RIGHT else Gravity.TOP, orientation) . apply { level = 0 }
		return AnimatedRow(LayerDrawable(arrayOf(row(stateFrom, size), clipDrawable2)),  clipDrawable2)
	}

	private fun row(state: CellState, size: Int) : Drawable {
        val cells = (1..taskProvider.multiplier).map { cell(state) }.toTypedArray()
        val layer = LayerDrawable(cells) . apply {  }
        for (i in 0 until cells.size) {
            layer.setLayerInset(i, i * size + 3, 3, (taskProvider.multiplier - 1 - i) * size + 3, 3)
        }
        return layer
    }

	private fun cell(s: CellState) = resources.getDrawable(when (s) {
		CellState.Empty -> R.drawable.cell_empty
		CellState.Filled -> R.drawable.cell_filled
		CellState.ToBeFilled -> R.drawable.cell_to_be_filled
		CellState.WasEmptied -> R.drawable.cell_was_emptied
	})
}