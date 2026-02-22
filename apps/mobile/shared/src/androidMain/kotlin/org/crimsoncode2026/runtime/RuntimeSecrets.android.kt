package org.crimsoncode2026.runtime

import org.crimsoncode2026.BuildConfig

actual object RuntimeSecrets {
    actual fun supabaseUrl(): String = BuildConfig.SUPABASE_URL
    actual fun supabaseAnonKey(): String = BuildConfig.SUPABASE_ANON_KEY
}
