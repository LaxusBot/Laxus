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
package xyz.laxus.api.routing

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.withCharset
import io.ktor.locations.Locations
import io.ktor.response.respond
import me.kgustave.json.ktor.client.JSKtorSerializer
import me.kgustave.json.ktor.server.JSContentConverter
import me.kgustave.json.reflect.JSDeserializer
import me.kgustave.json.reflect.JSSerializer
import me.kgustave.ktor.client.okhttp.OkHttp
import org.slf4j.event.Level
import xyz.laxus.api.Module
import xyz.laxus.api.error.HttpError
import xyz.laxus.api.error.InternalServerError
import xyz.laxus.api.error.badRequest
import xyz.laxus.api.error.unauthorized
import xyz.laxus.api.handlers.routeHandlers
import java.nio.charset.Charset

object Routes: Module {
    private val deserializer = JSDeserializer()
    private val serializer = JSSerializer()
    private val httpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JSKtorSerializer(Routes.serializer, Routes.deserializer, Charsets.UTF_8)
        }
    }

    override fun Application.install() {
        install(CallLogging) {
            level = Level.DEBUG
        }

        install(Locations)

        install(ContentNegotiation) {
            Charset.availableCharsets().values.forEach {
                register(ContentType.Application.Json.withCharset(it),
                    JSContentConverter(deserializer, serializer, it))
            }
        }

        install(Compression) {
            gzip()
            deflate()
            identity()
        }

        install(CORS) {
            anyHost()
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Patch)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
        }

        install(StatusPages) {
            exception<HttpError> { exception ->
                call.respond(exception.status, exception.toJson())
            }

            exception<Throwable> { exception ->
                val http = exception as? HttpError ?: InternalServerError(exception)
                call.respond(http.status, http.toJson())
            }
        }

        routeHandlers {
            register(Dashboard(httpClient))
            handleMissing {
                body { throw badRequest("Request body is missing") }
                param { throw badRequest("Missing route param!") }
                queryParam { throw badRequest("Missing query param!") }
                header { header ->
                    throw when(header) {
                        HttpHeaders.Authorization -> unauthorized()
                        else -> badRequest("Missing header value '$header'")
                    }
                }
            }
        }
    }
}
