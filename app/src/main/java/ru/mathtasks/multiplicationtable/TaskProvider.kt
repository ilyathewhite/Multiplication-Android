package ru.mathtasks.multiplicationtable

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

@Parcelize
class Stage(val showPrevAnswers: Boolean, val multipliers: Array<Int>, val hintFrom: Array<Int>) : Parcelable

@Parcelize
class TaskProvider(
    val multiplicand: Int,
    private var stageIdx: Int,
    private var multiplierIdx: Int,
    private var attempt: Int,
    private val prevMultipliers: MutableList<Int>,
    var qErrors: Int,
    private val stages: List<Stage>
) : Parcelable {

    companion object {
        private const val MAX_LAST_MULTIPLIERS = 3
    }

    constructor(type: TaskType, multiplicand: Int) : this(
        multiplicand, 0, 0, 0, mutableListOf(), 0,
        when (type) {
            TaskType.Learn -> listOf(
                //Stage(true, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
                //Stage(false, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
                //Stage(true, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
                //Stage(false, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
                Stage(true, arrayOf(1, 3, 5, 7, 9), arrayOf(0)),
                Stage(true, arrayOf(9, 7, 5, 3, 1), arrayOf(10)),
                Stage(true, arrayOf(2, 4, 6, 8, 10), arrayOf(0)),
                Stage(true, arrayOf(10, 8, 6, 4, 2), arrayOf(0))
            )
            TaskType.Practice -> listOf(Stage(false, (1..30).map { Random().nextInt(9) + 1 }.toTypedArray(), arrayOf(0, 5, 10)))
            else -> listOf()
        }
    )

    val endOfGame get() = stages.size == stageIdx

    val endOfSet get() = stages[stageIdx].multipliers.size == multiplierIdx

    val multiplier get() = stages[stageIdx].multipliers[multiplierIdx]

    val nextMultipliers get() = stages[stageIdx].multipliers.drop(multiplierIdx + 1).toTypedArray()

    val unitAnimation
        get() = when (attempt) {
            0 -> null; 1 -> UnitAnimation.ByRow; else -> UnitAnimation.ByUnit
        }

    val fieldState: FieldState
        get() {
            val prevAnswers = when {
                stages[0].showPrevAnswers -> prevMultipliers.toTypedArray()
                attempt != 0 -> arrayOf(hintFrom())
                else -> arrayOf()
            }
            return FieldState(multiplier, multiplier, 0, 0, prevAnswers, multiplier)
        }

    val hintFromFieldState: FieldState
        get() {
            val hintFrom = hintFrom()
            return FieldState(multiplier, min(hintFrom, multiplier), max(multiplier - hintFrom, 0), max(hintFrom - multiplier, 0), arrayOf(hintFrom), multiplier)
        }

    fun hintFrom(): Int {
        var min = 10
        var result = -1
        for (m in if (prevMultipliers.size == 0) stages[0].hintFrom.toMutableList() else prevMultipliers) {
            if (Math.abs(m - multiplier) in 1..(min - 1)) {
                min = Math.abs(m - multiplier)
                result = m
            }
        }
        return result
    }

    fun answer2correct(answer: Int): Boolean {
        val result = answer == multiplier * multiplicand
        if (!result) {
            attempt++
            qErrors++
        }
        return result
    }

    fun nextTask() {
        if (endOfGame)
            throw IllegalStateException()
        else if (endOfSet)
            stageIdx++
        else {
            attempt = 0
            prevMultipliers.add(multiplier)
            if (prevMultipliers.size > MAX_LAST_MULTIPLIERS)
                prevMultipliers.removeAt(0)
            multiplierIdx++
            if (endOfSet) {
                prevMultipliers.clear()
                if (stageIdx == stages.size - 1)   // promote end of last set to end of game
                    stageIdx++
            }
        }
    }
}