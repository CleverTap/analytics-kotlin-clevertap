package com.segment.analytics.kotlin.destinations

import java.util.Date

object CleverTapUtils {
    const val DATE_PREFIX = "\$D_"

    fun getClevertapDate(date : Date): String {
        return DATE_PREFIX + date.time / 1000
    }
}