package com.hzk.back2u.data

import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = Firebase.auth
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override suspend fun register(email: String, password: String): Result<Unit> =
        authCall { auth.createUserWithEmailAndPassword(email, password).await() }

    override suspend fun login(email: String, password: String): Result<Unit> =
        authCall { auth.signInWithEmailAndPassword(email, password).await() }

    override fun logout() {
        auth.signOut()
    }

    private suspend fun authCall(block: suspend () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAuthException())
        }

    // Weak password is checked first because it is a subtype of invalid credentials.
    private fun Exception.toAuthException(): AuthException = when (this) {
        is FirebaseAuthUserCollisionException -> AuthException(AuthFailure.EMAIL_IN_USE)
        is FirebaseAuthWeakPasswordException -> AuthException(AuthFailure.WEAK_PASSWORD)
        is FirebaseAuthInvalidCredentialsException -> AuthException(AuthFailure.INVALID_CREDENTIALS)
        is FirebaseAuthInvalidUserException -> AuthException(AuthFailure.USER_NOT_FOUND)
        is FirebaseNetworkException -> AuthException(AuthFailure.NETWORK)
        else -> AuthException(AuthFailure.UNKNOWN)
    }
}