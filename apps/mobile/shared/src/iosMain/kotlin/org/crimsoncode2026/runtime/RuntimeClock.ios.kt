package org.crimsoncode2026.runtime

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

actual object RuntimeClock {
    private const val CF_ABSOLUTE_TIME_UNIX_EPOCH_OFFSET_SECONDS = 978_307_200.0

    private val isoFormatter: NSISO8601DateFormatter
        get() = NSISO8601DateFormatter().apply {
            formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
        }

    actual fun nowMillis(): Long =
        ((CFAbsoluteTimeGetCurrent() + CF_ABSOLUTE_TIME_UNIX_EPOCH_OFFSET_SECONDS) * 1000.0).toLong()

    actual fun isoNow(): String = isoFromMillis(nowMillis())

    actual fun isoFromMillis(millis: Long): String =
        isoFormatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis.toDouble() / 1000.0))

    actual fun parseIsoMillis(value: String): Long? = null

    actual fun formatDateTime(millis: Long): String =
        NSDateFormatter().apply { dateFormat = "MMM d, yyyy h:mm a" }
            .stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis.toDouble() / 1000.0))

    actual fun formatShortDateTime(millis: Long): String =
        NSDateFormatter().apply { dateFormat = "MMM d, h:mm a" }
            .stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis.toDouble() / 1000.0))
}
