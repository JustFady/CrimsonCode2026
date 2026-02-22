package org.crimsoncode2026.utils

/**
 * Utility class for phone number normalization and validation.
 * Handles USA phone numbers in E.164 format.
 *
 * E.164 format for USA: +1XXXXXXXXXX (11 digits total)
 */
object PhoneNumberUtils {

    /**
     * USA country code
     */
    private const val USA_COUNTRY_CODE = "1"

    /**
     * Expected length for USA phone numbers (excluding country code)
     */
    private const val USA_PHONE_LENGTH = 10

    /**
     * Normalizes a phone number to E.164 format for USA.
     *
     * Handles various input formats:
     * - With/without country code (+1, 1, or none)
     * - With/without formatting (spaces, parentheses, dashes)
     * - With leading 1 or not
     *
     * Examples:
     * - (555) 123-4567 -> +15551234567
     * - 555-123-4567 -> +15551234567
     * - +1 555 123 4567 -> +15551234567
     * - 15551234567 -> +15551234567
     *
     * @param input Raw phone number string
     * @return Normalized E.164 phone number (+1XXXXXXXXXX) or null if invalid
     */
    fun normalizeToE164(input: String): String? {
        if (input.isBlank()) return null

        // Extract only digits from input
        val digits = input.replace(Regex("[^\\d]"), "")

        // Must have 10 or 11 digits (with optional leading 1)
        if (digits.length !in listOf(USA_PHONE_LENGTH, USA_PHONE_LENGTH + 1)) {
            return null
        }

        // Get 10-digit number, removing leading 1 if present
        val tenDigitNumber = if (digits.length == USA_PHONE_LENGTH + 1 && digits.startsWith("1")) {
            digits.substring(1)
        } else {
            digits
        }

        // Validate it's exactly 10 digits now
        if (tenDigitNumber.length != USA_PHONE_LENGTH) {
            return null
        }

        // Return E.164 format
        return "+$USA_COUNTRY_CODE$tenDigitNumber"
    }

    /**
     * Validates if a string is a valid E.164 USA phone number.
     *
     * @param phone Phone number to validate
     * @return true if valid E.164 USA format, false otherwise
     */
    fun isValidE164(phone: String): Boolean {
        return phone.startsWith("+$USA_COUNTRY_CODE") &&
                phone.replace(Regex("[^\\d]"), "").length == USA_PHONE_LENGTH + 1
    }

    /**
     * Formats a normalized E.164 phone number for display.
     *
     * Input: +15551234567
     * Output: (555) 123-4567
     *
     * @param phone E.164 phone number
     * @return Formatted display string or original if invalid format
     */
    fun formatForDisplay(phone: String): String {
        val digits = phone.replace(Regex("[^\\d]"), "")

        return when {
            digits.length != USA_PHONE_LENGTH + 1 || !digits.startsWith(USA_COUNTRY_CODE) -> phone
            else -> {
                val tenDigits = digits.substring(1)
                "(${tenDigits.take(3)}) ${tenDigits.substring(3, 6)}-${tenDigits.takeLast(4)}"
            }
        }
    }

    /**
     * Masks a phone number for privacy (showing only last 4 digits).
     *
     * Input: +15551234567
     * Output: +1 (***) ***-4567
     *
     * @param phone E.164 phone number
     * @return Masked phone number or original if invalid format
     */
    fun maskPhoneNumber(phone: String): String {
        if (!isValidE164(phone)) return phone

        val digits = phone.replace(Regex("[^\\d]"), "")
        val lastFour = digits.takeLast(4)
        return "+$USA_COUNTRY_CODE (***) ***-$lastFour"
    }

    /**
     * Extracts area code from a normalized E.164 phone number.
     *
     * Input: +15551234567
     * Output: 555
     *
     * @param phone E.164 phone number
     * @return Area code or null if invalid format
     */
    fun getAreaCode(phone: String): String? {
        val digits = phone.replace(Regex("[^\\d]"), "")

        return when {
            digits.length != USA_PHONE_LENGTH + 1 || !digits.startsWith(USA_COUNTRY_CODE) -> null
            else -> digits.substring(1, 4)
        }
    }
}
