package com.hzk.back2u.data

import kotlinx.coroutines.flow.Flow

// App-level reasons why login or registration can fail.
// The UI only knows these, never the Firebase exception classes.
enum class AuthFailure {
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    NETWORK,
    UNKNOWN
}

class AuthException(val failure: AuthFailure) : Exception(failure.name)

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentUserId: String?
    val currentUserEmail: String?

    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    fun logout()
}