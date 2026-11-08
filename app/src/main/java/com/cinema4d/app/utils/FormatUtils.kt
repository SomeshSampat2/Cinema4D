package com.cinema4d.app.utils

import java.util.Locale

object FormatUtils {
    fun inr(amount: Int): String = "₹${"%,d".format(Locale.US, amount)}"

    fun duration(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun ratingCount(count: Int): String = when {
        count >= 100_000 -> "${count / 100_000}L"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
