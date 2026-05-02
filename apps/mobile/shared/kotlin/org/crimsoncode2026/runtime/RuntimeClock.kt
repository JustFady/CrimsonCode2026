package org.crimsoncode2026.runtime

expect object RuntimeClock {
    fun nowMillis(): Long
    fun isoNow(): String
    fun isoFromMillis(millis: Long): String
    fun parseIsoMillis(value: String): Long?
    fun formatDateTime(millis: Long): String
    fun formatShortDateTime(millis: Long): String
}
