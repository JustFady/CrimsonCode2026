package org.crimsoncode2026.di

/**
 * App configuration class for Android
 * Loads configuration from BuildConfig (generated from local.properties)
 *
 * Required Configuration (in apps/mobile/androidApp/local.properties):
 * - supabase.url: Supabase project URL
 * - supabase.anon.key: Supabase anon/public key for client-side access
 *
 * Optional Configuration:
 * - SUPABASE_SERVICE_ROLE_KEY: Service role key (NOT used in mobile app, only for Edge Functions)
 * - FCM_SERVER_KEY: Firebase Cloud Messaging server key
 * - NETWORK_CACHE_DURATION: Cache duration for network requests in seconds (default: 10)
 * - DATA_CACHE_TTL: Time-to-live for cached data in seconds (default: 300)
 */
object AppConfig {
    /**
     * Supabase project URL from BuildConfig
     */
    val supabaseUrl: String
        get() = try {
            org.crimsoncode2026.BuildConfig.SUPABASE_URL
        } catch (e: Exception) {
            System.getenv("SUPABASE_URL") ?: "https://your-project-ref.supabase.co"
        }

    /**
     * Supabase Anon/Public key from BuildConfig
     * Required for client-side Supabase access
     */
    val supabaseAnonKey: String
        get() = try {
            org.crimsoncode2026.BuildConfig.SUPABASE_ANON_KEY
        } catch (e: Exception) {
            System.getenv("SUPABASE_ANON_KEY") ?: "your-anon-key-here"
        }

    /**
     * Supabase Service Role key
     * Only for admin operations (Edge Functions) - not used in mobile app
     * WARNING: NEVER use this key in the mobile app client
     */
    val supabaseServiceRoleKey: String
        get() = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
            ?: "your-service-role-key-here"

    /**
     * Firebase Cloud Messaging Server Key
     * Used for push notification delivery
     */
    val fcmServerKey: String
        get() = System.getenv("FCM_SERVER_KEY") ?: ""

    /**
     * Cache duration for network requests (in seconds)
     */
    val networkCacheDuration: Long
        get() = System.getenv("NETWORK_CACHE_DURATION")?.toLongOrNull() ?: 10

    /**
     * Time-to-live for cached data (in seconds)
     */
    val dataCacheTtl: Long
        get() = System.getenv("DATA_CACHE_TTL")?.toLongOrNull() ?: 300

    /**
     * Validates configuration values
     * @return true if all required values are present and not default placeholders
     */
    fun validate(): Boolean {
        return supabaseUrl.isNotBlank() &&
                supabaseUrl != "https://your-project-ref.supabase.co" &&
                supabaseAnonKey.isNotBlank() &&
                supabaseAnonKey != "your-anon-key-here"
    }
}
