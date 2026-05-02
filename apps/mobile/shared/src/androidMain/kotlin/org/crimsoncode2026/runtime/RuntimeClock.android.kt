package org.crimsoncode2026.runtime

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

actual object RuntimeClock {
    actual fun nowMillis(): Long = System.currentTimeMillis()

    actual fun isoNow(): String = Instant.now().toString()

    actual fun isoFromMillis(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    actual fun parseIsoMillis(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    actual fun formatDateTime(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(millis))

    actual fun formatShortDateTime(millis: Long): String =
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(millis))
}
