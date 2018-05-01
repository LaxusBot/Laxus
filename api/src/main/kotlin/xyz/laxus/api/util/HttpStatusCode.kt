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
package xyz.laxus.api.util

import org.eclipse.jetty.http.HttpStatus

/**
 * @author Kaidan Gustave
 */
enum class HttpStatusCode(val code: Int) {
    OK(HttpStatus.OK_200),
    BAD_REQUEST(HttpStatus.BAD_REQUEST_400),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED_401);

    companion object {
        fun codeOf(code: Int) = requireNotNull(values().find { it.code == code }) {
            "Unable to find HttpStatusCode value with code: $code!"
        }
    }
}
