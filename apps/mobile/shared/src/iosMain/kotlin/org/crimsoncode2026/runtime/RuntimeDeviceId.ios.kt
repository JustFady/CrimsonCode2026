package org.crimsoncode2026.runtime

import platform.Foundation.NSUUID

actual object RuntimeDeviceId {
    private val fallback = "ios-" + NSUUID().UUIDString
    actual fun value(): String = fallback
}
