/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "DEPRECATION")
package xyz.laxus.coroutines

import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Continuation interface that allows secure locking of resumption
 * with a key.
 *
 * @author Kaidan Gustave
 */
@Deprecated("no longer supported")
interface KeyLockedContinuation<in K, in T> : Continuation<T> {
    /**
     * Whether or not this continuation is currently locked.
     *
     * Resuming a [KeyLockedContinuation] by very nature of the
     * type is restricted until this returns `false`, signaling
     * it is now unlocked and available to resume.
     */
    val locked: Boolean

    /**
     * Unlocks this continuation matching the provided [key]
     * to the one that locked it initially.
     *
     * If the key is incorrect, this throws an [IllegalArgumentException].
     *
     * @param key The key to unlock this with.
     */
    fun unlock(key: K)

    /**
     * Tries to unlock this continuation matching the provided [key]
     * to the one that locked it initially.
     *
     * If the key is incorrect, then [locked] will remain `true`.
     *
     * @param key The key to unlock this with.
     */
    fun tryUnlock(key: K)

    /**
     * Unlocks and resumes this continuation with the provided [value], and
     * by matching the provided [key] to the one that locked it initially.
     *
     * If the key is incorrect, this throws an [IllegalArgumentException].
     *
     * @param key The key to unlock this with.
     * @param value The value to resume with.
     */
    fun unlockAndResume(key: K, value: T)

    /**
     * Unlocks and resumes this continuation exceptionally with the provided
     * [exception], and by matching the provided [key] to the one that locked
     * it initially.
     *
     * If the key is incorrect, this throws an [IllegalArgumentException].
     *
     * @param key The key to unlock this with.
     * @param exception The exception to resume with.
     */
    fun unlockAndResumeWithException(key: K, exception: Throwable)

    /**
     * Tries to unlock this continuation matching the provided [key]
     * to the one that locked it initially.
     *
     * If this succeeds then the continuation will be resumed successfully
     * with the provided [value].
     *
     * If the key is incorrect, then [locked] will remain `true`
     * and the continuation will remain un-resumed.
     *
     * @param key The key to unlock this with.
     * @param value The value to resume with.
     */
    fun tryUnlockAndResume(key: K, value: T)

    /**
     * Tries to unlock this continuation matching the provided [key]
     * to the one that locked it initially.
     *
     * If this succeeds then the continuation will be resumed exceptionally
     * with the provided [exception].
     *
     * If the key is incorrect, then [locked] will remain `true`
     * and the continuation will remain un-resumed.
     *
     * @param key The key to unlock this with.
     * @param exception The exception to resume with.
     */
    fun tryUnlockAndResumeWithException(key: K, exception: Throwable)

    /**
     * Forcibly resumes the continuation with an [exception].
     *
     * This is the only way to unlock a [KeyLockedContinuation]
     * without knowledge of the key, and certain implementations
     * may choose to not support this, throwing an
     * [UnsupportedOperationException] instead of providing the
     * functionality.
     *
     * @param exception The exception to unlock with.
     */
    fun forceResume(exception: Throwable)
}

@PublishedApi
internal class KeyLockedContinuationImpl<in K, in T>(
    private val key: K,
    private val continuation: Continuation<T>
): KeyLockedContinuation<K, T> {
    @Volatile override var locked = true
        private set

    override val context get() = continuation.context

//    override fun resume(value: T) {
//        resumeCorrectly(value, true)
//    }
//
//    override fun resumeWithException(exception: Throwable) {
//        resumeCorrectlyWithException(exception, true, false)
//    }

    override fun resumeWith(result: Result<T>) {
        val value = result.getOrElse {
            return resumeCorrectlyWithException(it, true, false)
        }
        resumeCorrectly(value, true)
    }

    override fun unlock(key: K) {
        unlockCorrectly(key, true)
    }

    override fun unlockAndResume(key: K, value: T) {
        unlock(key)
        resumeCorrectly(value, true)
    }

    override fun unlockAndResumeWithException(key: K, exception: Throwable) {
        unlock(key)
        resumeCorrectlyWithException(exception, true, false)
    }

    override fun tryUnlock(key: K) {
        unlockCorrectly(key, false)
    }

    override fun tryUnlockAndResume(key: K, value: T) {
        tryUnlock(key)
        resumeCorrectly(value, false)
    }

    override fun tryUnlockAndResumeWithException(key: K, exception: Throwable) {
        tryUnlock(key)
        resumeCorrectlyWithException(exception, false, false)
    }

    override fun forceResume(exception: Throwable) {
        resumeCorrectlyWithException(exception, true, true)
    }

    // Private implements

    private fun unlockCorrectly(key: K, shouldFail: Boolean) {
        // If the key is correct or we shouldn't fail, pass
        require(this.key == key || !shouldFail) { "Key '$key' does not match continuation's key!" }

        if(this.key == key) {
            // unlock
            locked = false
        }
    }

    private fun resumeCorrectly(value: T, shouldFail: Boolean) {
        // If it's not locked or it shouldn't fail, pass
        check(!locked || !shouldFail) { "Continuation has not been unlocked and cannot be resumed!" }

        // If we are not locked, proceed
        if(!locked) {
            // resume
            continuation.resume(value)
        }
    }

    private fun resumeCorrectlyWithException(exception: Throwable, shouldFail: Boolean, force: Boolean) {
        // If it's not locked, we shouldn't fail, or we are forcing, pass
        check(!locked || !shouldFail || force) { "Continuation has not been unlocked and cannot be resumed!" }

        // If we are not locked or we are forcing, continue
        if(!locked || force) {
            // resume
            continuation.resumeWithException(exception)
        }
    }
}

/**
 * Suspends the calling coroutine, waiting for the continuation
 * provided in the [block] to resume.
 *
 * In order for this to resume, the same key provided here must
 * be provided when [unlocking][KeyLockedContinuation.unlock]
 * the continuation.
 *
 * The following example shows how this can be used:
 * ```kotlin
 * lateinit var loginLock: KeyLockedContinuation<String, String>
 *
 * fun authorize(password: String) = runBlocking<Unit> {
 *     val accountInfo = suspendAndLockCoroutine<String, AccountInfo>(PasswordManager.encryptedPassword(password)) {
 *         loginLock = it
 *         logIn(password)
 *     }
 *     println("Logged into: $accountInfo")
 * }
 *
 * fun logIn(password: String) {
 *     loginLock.unlock(PasswordManager.encryptedPassword(password))
 *     loginLock.resume(PasswordManager.retrieveAccountInfo(password))
 * }
 * ```
 *
 * @param K The type of key to lock with.
 * @param T The type of value to resume with.
 * @param key The key to lock with.
 * @param block The block to run.
 *
 * @return The value after the continuation is [resumed][KeyLockedContinuation.resume].
 */
@Deprecated("no longer supported, use mutex's and suspensions together instead!")
suspend inline fun <K, T> suspendAndLockCoroutine(
    key: K, crossinline block: (KeyLockedContinuation<K, T>) -> Unit
): T = suspendCoroutine { cont ->
    block(KeyLockedContinuationImpl(key, cont))
}

suspend inline fun <T> suspendCoroutineWithMutex(
    mutex: Mutex, crossinline block: (Mutex, Continuation<T>) -> Unit
): T = suspendCoroutine { cont -> block(mutex, cont) }
