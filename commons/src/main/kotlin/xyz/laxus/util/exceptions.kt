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
@file:Suppress("Unused")
package xyz.laxus.util

inline fun unsupported(msg: () -> String): Nothing = throw UnsupportedOperationException(msg())

inline fun doNotSupport(condition: Boolean, msg: () -> String) {
    if(condition) {
        unsupported(msg)
    }
}

inline fun onlySupport(condition: Boolean, msg: () -> String) {
    if(!condition) {
        unsupported(msg)
    }
}

inline fun ignored(block: () -> Unit) {
    try { block() } catch(t: Throwable) {}
}

inline fun <reified T> ignored(default: T, block: () -> T): T {
    return try { block() } catch(t: Throwable) { default }
}

inline fun <reified T: Throwable> ignore(block: () -> Unit) {
    try {
        block()
    } catch(t: Throwable) {
        if(t is T) return
        throw t
    }
}
