package org.crimsoncode2026.di

import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.crimsoncode2026.data.EventRecipientRepository
import org.crimsoncode2026.data.EventRecipientRepositoryImpl
import org.crimsoncode2026.data.EventRepository
import org.crimsoncode2026.data.EventRepositoryImpl
import org.crimsoncode2026.data.RealtimeService
import org.crimsoncode2026.data.RealtimeServiceImpl
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.data.UserContactRepositoryImpl
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.data.UserRepositoryImpl
import org.crimsoncode2026.location.LocationRepository
import org.crimsoncode2026.location.permissions.LocationPermissionHandler
import org.crimsoncode2026.location.LocationState
import org.crimsoncode2026.location.IpGeolocationService
import org.crimsoncode2026.di.supabaseClientModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val locationModule = module {
    factory { params -> LocationPermissionHandler(get(PermissionsController)) }
    single { IpGeolocationService(get()) }
    single { LocationRepository(get(PermissionsController), get()) }
    factory { params -> LocationState(get(), params.get()) }
}

/**
 * Koin module for Supabase data repositories
 * Provides singleton instances of repositories and realtime service
 */
val supabaseDataModule = module {
    // User Repository
    single<UserRepository> { UserRepositoryImpl(get()) }

    // User Contact Repository
    single<UserContactRepository> { UserContactRepositoryImpl(get()) }

    // Event Repository
    single<EventRepository> { EventRepositoryImpl(get()) }

    // Event Recipient Repository
    single<EventRecipientRepository> { EventRecipientRepositoryImpl(get()) }

    // Realtime Service
    single<RealtimeService> {
        RealtimeServiceImpl(
            realtime = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
}

fun initKoin() {
    startKoin {
        modules(
            supabaseClientModule,
            supabaseDataModule,
            locationModule,
        )
    }
}
