package com.example.onlinelearning.model.entity

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Statistics for the "Clocking in!" card. */
data class LearningStats(
    val todayMinutes: Int = 0,
    val totalHours: Double = 0.0,
    /** Days with at least one finished lesson. */
    val activeDays: Int = 0,
    /** Monday..Sunday of the current week: true when the user studied that day. */
    val week: List<Boolean> = List(7) { false },
    /** Index of today in [week] (0 = Monday). */
    val todayIndex: Int = 0,
)

/** Builds [LearningStats] from the activity stored in the profile. */
fun UserProfile?.learningStats(today: LocalDate = LocalDate.now()): LearningStats {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return LearningStats(
        todayMinutes = this?.minutesOn(today) ?: 0,
        totalHours = (this?.totalMinutes ?: 0L) / 60.0,
        activeDays = this?.activity?.count { it.value > 0 } ?: 0,
        week = (0..6).map { (this?.minutesOn(monday.plusDays(it.toLong())) ?: 0) > 0 },
        todayIndex = today.dayOfWeek.value - 1,
    )
}
