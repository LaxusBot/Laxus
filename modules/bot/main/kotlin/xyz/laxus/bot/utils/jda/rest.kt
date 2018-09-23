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
@file:JvmName("RestUtil")
@file:Suppress("unused")
package xyz.laxus.bot.utils.jda

import kotlinx.coroutines.delay
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

fun passContextToRestAction(enable: Boolean = true) = RestAction.setPassContext(enable)

suspend fun <T> RestAction<T>.await(): T = suspendCoroutine { cont ->
    queue({ cont.resume(it) }, { cont.resumeWithException(it) })
}

suspend fun <T> RestAction<T>.awaitAfter(time: Long, unit: TimeUnit): T {
    delay(time, unit)
    return await()
}
