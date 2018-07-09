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
package xyz.laxus.api.handlers.internal.routing.params

import io.ktor.application.ApplicationCall
import io.ktor.http.Headers
import io.ktor.http.Parameters
import io.ktor.request.ApplicationRequest
import io.ktor.response.ApplicationResponse

// These are a set of generic ParamResolvers for common Ktor API types.
// All of these are made to be resolved simply as to allow the user ease
//of access for handlers like the following:
//
// @RoutePath("/hello")
// class MyHandler {
//     private val id = AtomicLong(0)
//
//     @Get fun `Get Hello`(request: ApplicationRequest): JSObject {
//         return JSObject {
//             "id" to id.incrementAndGet()
//             "message" to "Hello, ${request.queryParameters["name"] ?: "world"}!"
//         }
//     }
// }

internal object RouteCallResolver: RouteParamResolver<ApplicationCall> {
    override suspend fun resolve(value: ApplicationCall) = value
}

internal object RouteHeadersResolver: RouteParamResolver<Headers> {
    override suspend fun resolve(value: ApplicationCall) = value.request.headers
}

internal object RouteParametersResolver: RouteParamResolver<Parameters> {
    override suspend fun resolve(value: ApplicationCall) = value.parameters
}

internal object RouteQueryParametersResolver: RouteParamResolver<Parameters> {
    override suspend fun resolve(value: ApplicationCall) = value.request.queryParameters
}

internal object RouteRequestResolver: RouteParamResolver<ApplicationRequest> {
    override suspend fun resolve(value: ApplicationCall) = value.request
}

internal object RouteResponseResolver: RouteParamResolver<ApplicationResponse> {
    override suspend fun resolve(value: ApplicationCall) = value.response
}