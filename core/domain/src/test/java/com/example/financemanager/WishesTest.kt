package com.example.financemanager

import com.example.financemanager.domain.wishes.readyToDecide
import com.example.financemanager.domain.wishes.wishList
import com.example.financemanager.model.Wish
import com.example.financemanager.model.WishStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WishesTest {

    private val today = LocalDate.of(2026, 11, 20)

    private fun wish(id: Long, price: Double, addedDaysAgo: Long, wait: Int, status: WishStatus = WishStatus.WAITING, decidedDaysAgo: Long? = null) =
        Wish(id, "W$id", price, "", today.minusDays(addedDaysAgo), wait, status, decidedDaysAgo?.let { today.minusDays(it) })

    @Test
    fun aWishIsReadyOnceItsTimeIsUp() {
        assertFalse(wish(1, 10.0, addedDaysAgo = 1, wait = 2).isReady(today))
        assertTrue(wish(1, 10.0, addedDaysAgo = 2, wait = 2).isReady(today))
    }

    @Test
    fun theListIsSplitAndSummedUp() {
        val list = wishList(
            listOf(
                wish(1, 50.0, addedDaysAgo = 0, wait = 3),
                wish(2, 80.0, addedDaysAgo = 2, wait = 1),
                wish(3, 120.0, addedDaysAgo = 10, wait = 2, status = WishStatus.SKIPPED, decidedDaysAgo = 7),
                wish(4, 30.0, addedDaysAgo = 9, wait = 1, status = WishStatus.BOUGHT, decidedDaysAgo = 2),
                wish(5, 15.0, addedDaysAgo = 9, wait = 1, status = WishStatus.SKIPPED, decidedDaysAgo = 5),
            ),
        )
        // The decision due soonest first, then the latest decision first.
        assertEquals(listOf(2L, 1L), list.waiting.map { it.id })
        assertEquals(listOf(4L, 5L, 3L), list.decided.map { it.id })
        assertEquals(135.0, list.savedBySkipping, 0.0)
        assertEquals(2, list.skipped)
        assertEquals(1, list.bought)
    }

    @Test
    fun onlyWaitingWishesWhoseTimeIsUpAreReady() {
        val wishes = listOf(
            wish(1, 50.0, addedDaysAgo = 0, wait = 3),
            wish(2, 80.0, addedDaysAgo = 3, wait = 1),
            wish(3, 20.0, addedDaysAgo = 2, wait = 2),
            wish(4, 30.0, addedDaysAgo = 9, wait = 1, status = WishStatus.BOUGHT, decidedDaysAgo = 2),
        )
        assertEquals(listOf(2L, 3L), readyToDecide(wishes, today).map { it.id })
    }
}
