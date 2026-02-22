package org.crimsoncode2026.runtime

expect object RuntimeSecrets {
    fun supabaseUrl(): String
    fun supabaseAnonKey(): String
}
