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
package xyz.laxus.wyvern.http.error

import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.http.HttpStatus.Code.*

// 400 - Bad Request
fun badRequest(message: String) = BadRequestError(message)
fun missingValue(expected: String, what: String = "value") = badRequest("Expected '$expected' but found no such $what!")

// 401 - Unauthorized
fun unauthorized() = UnauthorizedError()

// 403 - Forbidden
fun forbidden(message: String) = createHttpErrorWithMessage(FORBIDDEN, message)

// 404 - Not Found
fun notFound(message: String) = NotFoundError(message)

// 500 - Internal Server Error
fun internalServerError(cause: Throwable) = InternalServerError(cause)

private fun createHttpErrorWithMessage(code: HttpStatus.Code, message: String?): HttpError {
    return message?.let { HttpError(code, message) } ?: HttpError(code)
}