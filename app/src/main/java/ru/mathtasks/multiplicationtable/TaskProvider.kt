package ru.mathtasks.multiplicationtable

class TaskProvider(val multiplier: Int) {
    class Stage(val showPrevAnswers: Boolean, val multiplicands: MutableList<Int>, var hintFrom: Array<Int>);
    private val stages = mutableListOf(
          Stage(true, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
          Stage(false, mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), arrayOf(0)),
          Stage(true, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
          Stage(false, mutableListOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), arrayOf(0)),
          Stage(true, mutableListOf(1, 3, 5, 7, 9), arrayOf(0)),
          Stage(true, mutableListOf(9, 7, 5, 3, 1), arrayOf(10)),
          Stage(true, mutableListOf(2, 4, 6, 8, 10), arrayOf(0)),
          Stage(true, mutableListOf(10, 8, 6, 4, 2), arrayOf(0)),
          Stage(false, (1..30).map { java.util.Random().nextInt(9) + 1 }.toMutableList(), arrayOf(0, 5, 10)))

    private var prevMultiplicands = mutableSetOf<Int>()
    private val MAX_LAST_MULTIPLICANTS = 3
    private val lastMultiplicands = mutableListOf<Int>()

    val endOfGame get() = stages.size == 0

    val endOfSet get() = stages[0].multiplicands.size == 0

    val multiplicand get() = stages[0].multiplicands[0]

    val nextMultiplicands get() = stages[0].multiplicands.toTypedArray()

    fun isPrevMultiplicand(multiplicand: Int) = stages[0].showPrevAnswers && prevMultiplicands.contains(multiplicand)

    val hintFrom : Int get() {
        val multiplicand = multiplicand
        var min = 10
        var result = -1
        for (m in if (lastMultiplicands.size == 0) stages[0].hintFrom.toMutableList() else lastMultiplicands) {
            if (Math.abs(m - multiplicand) in 1..(min - 1)) {
                min = Math.abs(m - multiplicand)
                result = m
            }
        }
        return result
    }

    fun nextTask() {
        if (endOfGame)
            throw IllegalStateException();
        else if (endOfSet)
            stages.removeAt(0)
        else {
            prevMultiplicands.add(multiplicand)
            lastMultiplicands.add(multiplicand)
            if(lastMultiplicands.size > MAX_LAST_MULTIPLICANTS)
                lastMultiplicands.removeAt(0)
            stages[0].multiplicands.removeAt(0)
            if (endOfSet) {
                prevMultiplicands.clear()
                lastMultiplicands.clear()
                if (stages.size == 1)
                    stages.removeAt(0)
            }
        }
    }
}