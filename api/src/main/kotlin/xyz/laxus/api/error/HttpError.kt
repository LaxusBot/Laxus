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
@file:Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
package xyz.laxus.api.error

import io.ktor.http.HttpStatusCode
import me.kgustave.json.JSObject

open class HttpError(status: HttpStatusCode, override val message: String = status.description): RuntimeException() {
    val code = status.value

    init {
        require(code >= 400) {
            "Status code doesn't represent a client or server error response type!"
        }
    }

    internal open fun toJson() = JSObject {
        "status" to code
        "message" to message
    }
}