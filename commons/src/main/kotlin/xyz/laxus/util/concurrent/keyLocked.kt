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
package xyz.laxus.util.concurrent

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
interface KeyLockedContinuation<in K, in T> : Continuation<T> {
    val locked: Boolean

    fun unlock(key: K)
    fun tryUnlock(key: K)

    fun unlockAndResume(key: K, value: T)

    fun unlockAndResumeWithException(key: K, exception: Throwable)

    fun tryUnlockAndResume(key: K, value: T)

    fun tryUnlockAndResumeWithException(key: K, exception: Throwable)

    fun forceResume(exception: Throwable)
}

@PublishedApi
internal class KeyLockedContinuationImpl<in K, in T>(
    private val key: K,
    private val continuation: Continuation<T>
): KeyLockedContinuation<K, T> {
    @Volatile override var locked = true
    override val context get() = continuation.context

    override fun resume(value: T) {
        resumeCorrectly(value, true)
    }

    override fun resumeWithException(exception: Throwable) {
        resumeCorrectlyWithException(exception, true, false)
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

suspend inline fun <reified K, reified T> suspendAndLockCoroutine(
    key: K,
    crossinline block: (KeyLockedContinuation<K, T>) -> Unit
): T {
    return suspendCoroutine { cont ->
        val keyCont = KeyLockedContinuationImpl(key, cont)
        block(keyCont)
    }
}