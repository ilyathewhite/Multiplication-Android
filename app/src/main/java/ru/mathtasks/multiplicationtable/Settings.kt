package ru.mathtasks.multiplicationtable

class Settings {
    companion object {
        private const val DurationCoeff = 1
        const val ShowCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PauseAfterCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PrepareNextTaskDuration = 100L * DurationCoeff
        const val MoveNextTaskDuration = 400L * DurationCoeff

        const val ShowIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val PauseAfterIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val HideIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val ShowVisibleAnswersDuration = 100L * DurationCoeff

        const val ShowHintRowDuration = 400L * DurationCoeff
        const val ShowHintUnitRowDuration = 1000L * DurationCoeff

        const val EndOfStageCheckMarkAnimationDuration = 1000L
        const val EndOfStageSuccessBadgeAnimationDuration = 1000L

        const val RowMultiplicandInactiveAlpha = 0.6f
        const val RowUnitSwitchDuration = 10L
        const val ChooseMultiplicandDelay = 1000L
    }
}