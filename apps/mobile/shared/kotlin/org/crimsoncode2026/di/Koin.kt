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
import org.crimsoncode2026.domain.usecases.RegisterFcmTokenUseCase
import org.crimsoncode2026.domain.usecases.GetReceivedEventsUseCase
import org.crimsoncode2026.domain.usecases.MarkEventOpenedUseCase
import org.crimsoncode2026.domain.usecases.SubscribeToPrivateEventsUseCase
import org.crimsoncode2026.domain.usecases.CreatePrivateEventWithRecipientsUseCase
import org.crimsoncode2026.domain.usecases.CreatePublicEventUseCase
import org.crimsoncode2026.domain.usecases.CreateEventUseCase
import org.crimsoncode2026.domain.usecases.QueryPublicEventsUseCase
import org.crimsoncode2026.location.LocationRepository
import org.crimsoncode2026.location.permissions.LocationPermissionHandler
import org.crimsoncode2026.location.LocationState
import org.crimsoncode2026.notifications.permissions.NotificationPermissionHandler
import org.crimsoncode2026.notifications.NotificationPresenter
import org.crimsoncode2026.contacts.permissions.ContactsPermissionHandler
import org.crimsoncode2026.contacts.DeviceContactsService
import org.crimsoncode2026.domain.usecases.AppUserDetectionUseCase
import org.crimsoncode2026.domain.usecases.ImportContactsUseCase
import org.crimsoncode2026.screens.contacts.ContactsViewModel
import org.crimsoncode2026.screens.eventcreation.EventCreationViewModel
import org.crimsoncode2026.screens.main.MainMapViewModel
import org.crimsoncode2026.screens.privateevents.PrivateEventsViewModel
import org.crimsoncode2026.screens.settings.SettingsViewModel
import org.crimsoncode2026.domain.usecases.FcmTokenInitializationUseCase
import org.crimsoncode2026.notifications.FcmTokenManager
import org.crimsoncode2026.notifications.FcmTokenRepository
import io.github.mirzemehdi.kmpnotifier.KmpNotifier
import org.crimsoncode2026.location.IpGeolocationService
import org.crimsoncode2026.storage.SecureStorage
import org.crimsoncode2026.storage.PreferencesStorage
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
    // KMPNotifier - main instance for FCM token management
    single { KmpNotifier() }

    // Notification Permission Handler
    factory { params -> NotificationPermissionHandler(get(PermissionsController)) }

    // FCM Token Manager - handles token retrieval and caching
    single { FcmTokenManager(get(), get()) }

    // FCM Token Repository - coordinates FCM token lifecycle
    single { FcmTokenRepository(get(), get()) }

    // FCM Token Initialization Use Case - handles token refresh on app launch
    single { FcmTokenInitializationUseCase(get(), get()) }

    // Notification Presenter - displays notifications with severity-based styling
    single { NotificationPresenter(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}

val contactsModule = module {
    // Contacts Permission Handler
    factory { params -> ContactsPermissionHandler(get(PermissionsController)) }

    // Device Contacts Service - imports device contacts using Kontacts library
    single { DeviceContactsService() }

    // App User Detection Use Case - matches contacts against Users table
    single { AppUserDetectionUseCase(get(), get()) }

    // Import Contacts Use Case - orchestrates contact import and sync
    single { ImportContactsUseCase(get(), get(), get()) }

    // Contacts ViewModel - state management for contact selection screen
    factory { params ->
        ContactsViewModel(
            userContactRepository = get(),
            importContactsUseCase = get(),
            contactsPermissionHandler = get(),
            userSessionManager = get(),
            scope = params.get()
        )
    }
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

    // Preferences Storage (platform-specific via expect/actual factory)
    single<PreferencesStorage> { createPreferencesStorage() }

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

    // FCM Token Registration Use Case
    single { RegisterFcmTokenUseCase(get(), get()) }

    // User Session Manager
    single { UserSessionManager(get(), get()) }

    // Events Use Cases
    single { GetReceivedEventsUseCase(get(), get(), get(), get()) }
    single { MarkEventOpenedUseCase(get(), get()) }
    single { SubscribeToPrivateEventsUseCase(get(), get()) }
    single { CreatePrivateEventWithRecipientsUseCase(get(), get(), get(), get(), get()) }
    single { CreatePublicEventUseCase(get(), get()) }
    single { CreateEventUseCase(get(), get()) }
}

/**
 * Koin module for user settings
 * Provides SettingsViewModel for preference management
 * Note: PreferencesStorage is provided by authModule
 */
val settingsModule = module {
    // Settings ViewModel - state management for user preferences
    factory { params ->
        SettingsViewModel(
            preferencesStorage = get(),
            scope = params.get()
        )
    }
}

/**
 * Koin module for event management screens and ViewModels
 * Provides ViewModels for received events display and interaction
 */
val eventsModule = module {
    // Private Events ViewModel - state management for received events list
    factory { params ->
        PrivateEventsViewModel(
            getReceivedEventsUseCase = get(),
            markEventOpenedUseCase = get(),
            subscribeToPrivateEventsUseCase = get(),
            scope = params.get()
        )
    }

    // Event Creation ViewModel - state management for event creation wizard
    factory { params ->
        EventCreationViewModel(
            createEventUseCase = get(),
            scope = params.get()
        )
    }

    // Main Map ViewModel - state management for main map screen (singleton for shared state)
    single {
        MainMapViewModel(
            queryPublicEventsUseCase = get(),
            getReceivedEventsUseCase = get(),
            subscribeToPrivateEventsUseCase = get(),
            locationState = get { parametersOf(CoroutineScope(SupervisorJob() + Dispatchers.Default)) },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }

    // Query Public Events Use Case - fetches public events within map bounds
    single { QueryPublicEventsUseCase(get(), get()) }
}

fun initKoin() {
    startKoin {
        modules(
            supabaseClientModule,
            supabaseDataModule,
            authModule,
            locationModule,
            notificationsModule,
            contactsModule,
            eventsModule,
            settingsModule,
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
