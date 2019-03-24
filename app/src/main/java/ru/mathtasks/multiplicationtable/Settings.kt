package ru.mathtasks.multiplicationtable

class Settings {
    companion object {
        private const val DurationCoeff = 10
        const val TrainingActivityShowCorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityPauseAfterCorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityPrepareNextTaskDuration = 100L * DurationCoeff
        const val TrainingActivityMoveNextTaskDuration = 400L * DurationCoeff

        const val TrainingActivityShowIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityPauseAfterIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityHideIncorrectCheckMarkDuration = 250L * DurationCoeff
        const val TrainingActivityPulseScale = 1.5f
        const val TrainingActivityPulseHintFromDuration = 1000L * DurationCoeff
        const val TrainingActivityCrossFadeHintViewDuration = 100L * DurationCoeff

        const val ShowHintRowDuration = 400L * DurationCoeff
        const val ShowHintUnitRowDuration = 1000L * DurationCoeff

        // for EndOfStageCheckMarkAnimationDuration see success_badge_mark.xml animation duration
        const val EndOfStageSuccessBadgeAnimationDuration = 1000L

        const val RowMultiplicandInactiveAlpha = 0.6f
        const val RowUnitSwitchDuration = 10L
        const val ChooseMultiplicandDelay = 1000L

        const val TestActivityShowMarkDuration = 250L * DurationCoeff
        const val TestActivityPauseAfterMarkDuration = 250L * DurationCoeff
        const val TestActivityHideMarkDuration = 250L * DurationCoeff
        const val TestActivityMoveTaskDuration = 250L * DurationCoeff
    }
}