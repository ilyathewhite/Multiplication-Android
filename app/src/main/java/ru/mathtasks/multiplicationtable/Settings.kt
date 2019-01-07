package ru.mathtasks.multiplicationtable

class Settings {
    companion object {
        private const val DurationCoeff = 1
        const val ShowCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PauseAfterCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PrepareNextTaskDuration = 100L * DurationCoeff
        const val MoveNextTaskDuration = 400L * DurationCoeff

        const val TrainingActivityShowIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityPauseAfterIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityHideIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityShowVisibleAnswersDuration = 100L * DurationCoeff

        const val ShowHintRowDuration = 400L * DurationCoeff
        const val ShowHintUnitRowDuration = 1000L * DurationCoeff

        // for EndOfStageCheckMarkAnimationDuration see success_badge_mark.xml animation duration
        const val EndOfStageSuccessBadgeAnimationDuration = 1000L

        const val RowMultiplicandInactiveAlpha = 0.6f
        const val RowUnitSwitchDuration = 10L
        const val ChooseMultiplicandDelay = 1000L

        const val TestActivityShowMarkDuration = 250L
        const val TestActivityHideMarkDuration = 250L
    }
}