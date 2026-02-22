package org.crimsoncode2026.di

import io.github.jan-tennert.supabase.SupabaseClient
import io.github.jan-tennert.supabase.auth.Auth
import io.github.jan-tennert.supabase.postgrest.Postgrest
import io.github.jan-tennert.supabase.realtime.Realtime
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Supabase configuration for Emergency Response App
 * Uses supabase-kt v3.3.0 with Kotlin Multiplatform support
 */
object SupabaseConfig {

    /**
     * JSON serializer for Supabase client
     */
    val jsonSerializer: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Creates the main Supabase client with Auth, PostGrest, and Realtime modules
     * Uses configuration loaded from environment variables or defaults
     */
    fun createSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl,
        supabaseAnonKey = AppConfig.supabaseAnonKey,
        serviceRoleKey = null // Never pass service key in mobile app
    )

    /**
     * Creates the Supabase client with service role key for admin operations
     * Should ONLY be used in Edge Functions, NOT mobile app
     */
    fun createSupabaseClientWithServiceRole(): SupabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl,
        supabaseAnonKey = AppConfig.supabaseAnonKey,
        serviceRoleKey = AppConfig.supabaseServiceRoleKey
    )

    /**
     * Core Supabase client creation logic
     * @param supabaseUrl Supabase project URL
     * @param supabaseAnonKey Anon/public key for client-side access
     * @param serviceRoleKey Service role key for admin operations (should be null in app)
     */
    private fun createSupabaseClient(
        supabaseUrl: String,
        supabaseAnonKey: String,
        serviceRoleKey: String?
    ): SupabaseClient {
        val httpClient = HttpClient {
            install(ContentNegotiation) {
                json(SupabaseConfig.jsonSerializer, contentType = ContentType.Application.Json)
            }
        }

        return SupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseAnonKey = supabaseAnonKey,
            httpClient = httpClient,
            modules = listOf(
                Auth(),
                Postgrest(),
                Realtime()
            )
        )
    }
}

/**
 * Koin module for Supabase client
 * Provides singleton instances of Supabase client and its modules
 */
val supabaseClientModule: Module = module {
    single { SupabaseConfig.createSupabaseClient() }
    single { get<SupabaseClient>().auth }
    single { get<SupabaseClient>().postgrest }
    single { get<SupabaseClient>().realtime }
}
