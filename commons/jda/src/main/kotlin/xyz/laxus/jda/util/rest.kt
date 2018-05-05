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
@file:Suppress("unused")
package xyz.laxus.jda.util

import kotlinx.coroutines.experimental.delay
import net.dv8tion.jda.core.requests.RestAction
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.suspendCoroutine

fun passContextToRestAction(enable: Boolean = true) {
    RestAction.setPassContext(enable)
}

suspend inline fun <reified T> RestAction<T>.await() = suspendCoroutine<T> { cont ->
    queue({ cont.resume(it) }, { cont.resumeWithException(it) })
}

suspend inline fun <reified T> RestAction<T>.awaitAfter(time: Long, unit: TimeUnit): T {
    delay(time, unit)
    return await()
}
