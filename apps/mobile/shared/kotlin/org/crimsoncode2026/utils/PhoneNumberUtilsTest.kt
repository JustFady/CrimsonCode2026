package org.crimsoncode2026.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PhoneNumberUtils
 */
class PhoneNumberUtilsTest {

    @Test
    fun `normalizeToE164 handles formatted phone with parentheses`() {
        val result = PhoneNumberUtils.normalizeToE164("(555) 123-4567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles formatted phone with dashes`() {
        val result = PhoneNumberUtils.normalizeToE164("555-123-4567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles formatted phone with spaces`() {
        val result = PhoneNumberUtils.normalizeToE164("555 123 4567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles phone with country code plus`() {
        val result = PhoneNumberUtils.normalizeToE164("+1 555 123 4567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles phone with leading 1`() {
        val result = PhoneNumberUtils.normalizeToE164("15551234567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles already normalized phone`() {
        val result = PhoneNumberUtils.normalizeToE164("+15551234567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 returns null for blank input`() {
        val result = PhoneNumberUtils.normalizeToE164("")
        assertNull(result)
    }

    @Test
    fun `normalizeToE164 returns null for short phone`() {
        val result = PhoneNumberUtils.normalizeToE164("5551234")
        assertNull(result)
    }

    @Test
    fun `normalizeToE164 returns null for long phone`() {
        val result = PhoneNumberUtils.normalizeToE164("155512345678")
        assertNull(result)
    }

    @Test
    fun `normalizeToE164 returns null for 9 digits`() {
        val result = PhoneNumberUtils.normalizeToE164("555123456")
        assertNull(result)
    }

    @Test
    fun `isValidE164 returns true for valid E.164`() {
        assertTrue(PhoneNumberUtils.isValidE164("+15551234567"))
    }

    @Test
    fun `isValidE164 returns false for missing country code`() {
        assertFalse(PhoneNumberUtils.isValidE164("5551234567"))
    }

    @Test
    fun `isValidE164 returns false for wrong country code`() {
        assertFalse(PhoneNumberUtils.isValidE164("+445551234567"))
    }

    @Test
    fun `isValidE164 returns false for short number`() {
        assertFalse(PhoneNumberUtils.isValidE164("+1555123456"))
    }

    @Test
    fun `isValidE164 returns false for long number`() {
        assertFalse(PhoneNumberUtils.isValidE164("+155512345678"))
    }

    @Test
    fun `formatForDisplay formats valid E.164 phone`() {
        val result = PhoneNumberUtils.formatForDisplay("+15551234567")
        assertEquals("(555) 123-4567", result)
    }

    @Test
    fun `formatForDisplay returns original for invalid format`() {
        val result = PhoneNumberUtils.formatForDisplay("555-1234")
        assertEquals("555-1234", result)
    }

    @Test
    fun `formatForDisplay returns original for wrong country code`() {
        val result = PhoneNumberUtils.formatForDisplay("+445551234567")
        assertEquals("+445551234567", result)
    }

    @Test
    fun `maskPhoneNumber masks valid E.164 phone`() {
        val result = PhoneNumberUtils.maskPhoneNumber("+15551234567")
        assertEquals("+1 (***) ***-4567", result)
    }

    @Test
    fun `maskPhoneNumber returns original for invalid format`() {
        val result = PhoneNumberUtils.maskPhoneNumber("555-1234")
        assertEquals("555-1234", result)
    }

    @Test
    fun `getAreaCode extracts area code from valid phone`() {
        val result = PhoneNumberUtils.getAreaCode("+15551234567")
        assertEquals("555", result)
    }

    @Test
    fun `getAreaCode returns null for invalid format`() {
        val result = PhoneNumberUtils.getAreaCode("555-1234")
        assertNull(result)
    }

    @Test
    fun `getAreaCode returns null for wrong country code`() {
        val result = PhoneNumberUtils.getAreaCode("+445551234567")
        assertNull(result)
    }

    @Test
    fun `normalizeToE164 handles phone with mixed formatting`() {
        val result = PhoneNumberUtils.normalizeToE164("+1 (555) 123-4567")
        assertEquals("+15551234567", result)
    }

    @Test
    fun `normalizeToE164 handles phone with all special characters`() {
        val result = PhoneNumberUtils.normalizeToE164("(555).123.4567")
        assertEquals("+15551234567", result)
    }
}
