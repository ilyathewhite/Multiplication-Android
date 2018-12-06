package ru.mathtasks.multiplicationtable

import java.lang.Math.max
import java.lang.Math.min

class TaskProvider(val multiplicand: Int) {
    companion object {
        private const val MAX_LAST_MULTIPLIERS = 3
    }

    class Stage(val showPrevAnswers: Boolean, val multipliers: MutableList<Int>, var hintFrom: Array<Int>);
    private val stages = mutableListOf(
        Stage(true, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
        Stage(false, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
        Stage(true, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
        Stage(false, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
        Stage(true, mutableListOf(1, 3, 5, 7, 9), arrayOf(0)),
        Stage(true, mutableListOf(9, 7, 5, 3, 1), arrayOf(10)),
        Stage(true, mutableListOf(2, 4, 6, 8, 10), arrayOf(0)),
        Stage(true, mutableListOf(10, 8, 6, 4, 2), arrayOf(0)),
        Stage(false, (1..30).map { java.util.Random().nextInt(9) + 1 }.toMutableList(), arrayOf(0, 5, 10))
    )

    private var prevMultipliers = mutableSetOf<Int>()
    private val lastMultipliers = mutableListOf<Int>()

    var qErrors = 0
        private set
    var attempt = 0
        private set

    val endOfGame get() = stages.size == 0
    val endOfSet get() = stages[0].multipliers.size == 0
    val multiplier get() = stages[0].multipliers[0]
    val nextMultipliers get() = stages[0].multipliers.drop(1).toTypedArray()
    val unitAnimation get() = when(attempt) { 0 -> null; 1 -> UnitAnimation.ByRow; else -> UnitAnimation.ByUnit }

    val fieldState: FieldState
        get() {
            val prevAnswers = when {
                stages[0].showPrevAnswers -> prevMultipliers.toTypedArray()
                attempt != 0 -> arrayOf(hintFrom())
                else -> arrayOf()
            }
            return FieldState(Mark.None, multiplier, multiplier, 0, 0, prevAnswers, multiplier)
        }

    val hintFromFieldState : FieldState
        get() {
            val hintFrom = hintFrom()
            return FieldState(Mark.None, multiplier, min(hintFrom, multiplier), max(multiplier - hintFrom, 0), max(hintFrom - multiplier, 0), arrayOf(hintFrom), multiplier)
        }

    private fun hintFrom(): Int {
        var min = 10
        var result = -1
        for (m in if (lastMultipliers.size == 0) stages[0].hintFrom.toMutableList() else lastMultipliers) {
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
            throw IllegalStateException();
        else if (endOfSet)
            stages.removeAt(0)
        else {
            attempt = 0
            prevMultipliers.add(multiplier)
            lastMultipliers.add(multiplier)
            if (lastMultipliers.size > MAX_LAST_MULTIPLIERS)
                lastMultipliers.removeAt(0)
            stages[0].multipliers.removeAt(0)
            if (endOfSet) {
                prevMultipliers.clear()
                lastMultipliers.clear()
                if (stages.size == 1)
                    stages.removeAt(0)
            }
        }
    }
}