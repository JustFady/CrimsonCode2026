package org.crimsoncode2026.di

import dev.icerock.moko.permissions.PermissionsController
import io.github.jan-tennert.supabase.auth.Auth
import io.github.jan-tennert.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.crimsoncode2026.auth.AuthRepository
import org.crimsoncode2026.auth.AuthRepositoryImpl
import org.crimsoncode2026.auth.DeviceIdProvider
import org.crimsoncode2026.data.EventRecipientRepository
import org.crimsoncode2026.data.EventRecipientRepositoryImpl
import org.crimsoncode2026.data.EventRepository
import org.crimsoncode2026.data.EventRepositoryImpl
import org.crimsoncode2026.data.RealtimeService
import org.crimsoncode2026.data.RealtimeServiceImpl
import org.crimsoncode2026.data.UserContactRepository
import org.crimsoncode2026.data.UserContactRepositoryImpl
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.data.UserRepositoryImpl
import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.domain.usecases.RegisterUserUseCase
import org.crimsoncode2026.domain.usecases.SessionInitUseCase
import org.crimsoncode2026.domain.usecases.UpdateDisplayNameUseCase
import org.crimsoncode2026.domain.usecases.UpdateFcmTokenUseCase
import org.crimsoncode2026.domain.usecases.UpdateLastActiveUseCase
import org.crimsoncode2026.location.LocationRepository
import org.crimsoncode2026.location.permissions.LocationPermissionHandler
import org.crimsoncode2026.location.LocationState
import org.crimsoncode2026.notifications.permissions.NotificationPermissionHandler
import org.crimsoncode2026.location.IpGeolocationService
import org.crimsoncode2026.storage.SecureStorage
import org.crimsoncode2026.di.supabaseClientModule
import org.koin.core.component.getKoin
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val locationModule = module {
    factory { params -> LocationPermissionHandler(get(PermissionsController)) }
    single { IpGeolocationService(get()) }
    single { LocationRepository(get(PermissionsController), get()) }
    factory { params -> LocationState(get(), params.get()) }
}

val notificationsModule = module {
    factory { params -> NotificationPermissionHandler(get(PermissionsController)) }
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

/**
 * Factory function for creating SecureStorage
 * Platform-specific implementation provided by expect/actual
 */
expect fun createSecureStorage(): SecureStorage

/**
 * Koin module for authentication and user session management
 * Provides secure storage, use cases, and session manager
 */
val authModule = module {
    // Secure Storage (platform-specific via expect/actual factory)
    single<SecureStorage> { createSecureStorage() }

    // Auth Repository
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    // User Registration Use Case
    single { RegisterUserUseCase(get(), get(), get(), get(), get()) }

    // Session Init Use Case
    single { SessionInitUseCase(get(), get()) }

    // User Profile Use Cases
    single { UpdateDisplayNameUseCase(get()) }
    single { UpdateFcmTokenUseCase(get()) }
    single { UpdateLastActiveUseCase(get()) }

    // User Session Manager
    single { UserSessionManager(get(), get()) }
}

fun initKoin() {
    startKoin {
        modules(
            supabaseClientModule,
            supabaseDataModule,
            authModule,
            locationModule,
            notificationsModule,
        )
    }

    // Initialize DeviceIdProvider with SecureStorage
    // Must be called after Koin is started
    try {
        val secureStorage = getKoin().get<SecureStorage>()
        DeviceIdProvider.initialize(secureStorage)
    } catch (e: Exception) {
        // Log error but don't crash - DeviceIdProvider can be initialized later
        // In production, use proper logging here
    }
}
