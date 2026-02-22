package org.crimsoncode2026.data

import io.github.jan-tennert.supabase.postgrest.Postgrest
import io.github.jan-tennert.supabase.postgrest.filter.FilterBuilder
import io.github.jan-tennert.supabase.postgrest.result.PostgrestResult

/**
 * Repository interface for User data operations
 */
interface UserRepository {

    /**
     * Create a new user
     * @param user User to create
     * @return Result with created user or error
     */
    suspend fun createUser(user: User): Result<User>

    /**
     * Get user by ID
     * @param userId User ID
     * @return Result with user or null if not found
     */
    suspend fun getUserById(userId: String): Result<User?>

    /**
     * Get user by phone number
     * @param phoneNumber Phone number (E.164 format)
     * @return Result with user or null if not found
     */
    suspend fun getUserByPhoneNumber(phoneNumber: String): Result<User?>

    /**
     * Get user by device ID
     * @param deviceId Device ID
     * @return Result with user or null if not found
     */
    suspend fun getUserByDeviceId(deviceId: String): Result<User?>

    /**
     * Update user display name
     * @param userId User ID
     * @param displayName New display name
     * @return Result with updated user or error
     */
    suspend fun updateDisplayName(userId: String, displayName: String): Result<User>

    /**
     * Update user FCM token
     * @param userId User ID
     * @param fcmToken New FCM token
     * @return Result with updated user or error
     */
    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<User>

    /**
     * Update user last active timestamp
     * @param userId User ID
     * @return Result with updated user or error
     */
    suspend fun updateLastActive(userId: String): Result<User>

    /**
     * Update user device ID
     * Used for one-device-per-account enforcement when user logs in from new device
     * @param userId User ID
     * @param deviceId New device ID
     * @return Result with updated user or error
     */
    suspend fun updateDeviceId(userId: String, deviceId: String): Result<User>

    /**
     * Deactivate user account
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun deactivateUser(userId: String): Result<Unit>

    /**
     * Delete user (soft delete by setting deleted_at)
     * @param userId User ID
     * @return Result indicating success or error
     */
    suspend fun deleteUser(userId: String): Result<Unit>
}

/**
 * Supabase implementation of UserRepository
 */
class UserRepositoryImpl(
    private val postgrest: Postgrest
) : UserRepository {

    companion object {
        private const val TABLE = User.TABLE_NAME
    }

    override suspend fun createUser(user: User): Result<User> = try {
        val result = postgrest.from(TABLE)
            .insert(user)
            .decodeSingle<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserById(userId: String): Result<User?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserByPhoneNumber(phoneNumber: String): Result<User?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("phone_number", phoneNumber)
                }
            }
            .decodeSingleOrNull<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserByDeviceId(deviceId: String): Result<User?> = try {
        val result = postgrest.from(TABLE)
            .select {
                filter {
                    eq("device_id", deviceId)
                }
            }
            .decodeSingleOrNull<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateDisplayName(userId: String, displayName: String): Result<User> = try {
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "display_name" to displayName,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateFcmToken(userId: String, fcmToken: String): Result<User> = try {
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "fcm_token" to fcmToken,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateLastActive(userId: String): Result<User> = try {
        val now = System.currentTimeMillis()
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "last_active_at" to now,
                    "updated_at" to now
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateDeviceId(userId: String, deviceId: String): Result<User> = try {
        val now = System.currentTimeMillis()
        val result = postgrest.from(TABLE)
            .update(
                mapOf(
                    "device_id" to deviceId,
                    "updated_at" to now
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<User>()

        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deactivateUser(userId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .update(
                mapOf(
                    "is_active" to false,
                    "updated_at" to System.currentTimeMillis()
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = try {
        postgrest.from(TABLE)
            .delete {
                filter {
                    eq("id", userId)
                }
            }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
