package ru.mathtasks.multiplicationtable

class Settings {
    companion object {
        private const val DurationCoeff = 10
        const val ShowCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PauseAfterCorrectCheckMarkDuration = 250L * DurationCoeff
        const val PrepareMultiplierMovingDuration = 100L * DurationCoeff
        const val MultiplierMovingDuration = 400L * DurationCoeff
    }
}