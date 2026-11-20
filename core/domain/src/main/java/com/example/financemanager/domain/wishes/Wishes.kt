package com.example.financemanager.domain.wishes

import com.example.financemanager.model.Wish
import com.example.financemanager.model.WishStatus
import java.time.LocalDate

/** The wish list split into what is still being thought over and what was decided. */
data class WishList(
    /** The decision due soonest first. */
    val waiting: List<Wish>,
    /** The latest decision first. */
    val decided: List<Wish>,
    /** What giving things up has kept in the pocket. */
    val savedBySkipping: Double,
    val skipped: Int,
    val bought: Int,
)

fun wishList(wishes: List<Wish>): WishList {
    val (waiting, decided) = wishes.partition { it.status == WishStatus.WAITING }
    val skipped = decided.filter { it.status == WishStatus.SKIPPED }
    return WishList(
        waiting = waiting.sortedWith(compareBy<Wish> { it.decideOn }.thenBy { it.id }),
        decided = decided.sortedWith(compareByDescending<Wish> { it.decidedOn }.thenByDescending { it.id }),
        savedBySkipping = skipped.sumOf { it.price },
        skipped = skipped.size,
        bought = decided.count { it.status == WishStatus.BOUGHT },
    )
}

/** Wishes still waiting whose time to think it over has passed, the oldest first. */
fun readyToDecide(wishes: List<Wish>, today: LocalDate): List<Wish> =
    wishes.filter { it.status == WishStatus.WAITING && it.isReady(today) }.sortedBy { it.decideOn }
