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
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.locations.Locations
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import me.kgustave.json.JSObject
import me.kgustave.json.ktor.client.JSKtorSerializer
import me.kgustave.json.ktor.server.JSContentConverter
import me.kgustave.json.reflect.JSDeserializer
import me.kgustave.json.reflect.JSSerializer
import me.kgustave.json.toJSArray
import me.kgustave.ktor.client.okhttp.OkHttp
import org.slf4j.event.Level
import xyz.laxus.api.Module
import xyz.laxus.api.error.HttpError
import xyz.laxus.api.error.InternalServerError
import xyz.laxus.api.error.badRequest
import xyz.laxus.api.oauth2.DiscordOAuth2
import xyz.laxus.api.oauth2.OAuth2
import xyz.laxus.api.oauth2.OAuth2Session

object Routes: Module {
    private val deserializer = JSDeserializer()
    private val serializer = JSSerializer()
    private val httpClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = JSKtorSerializer(Routes.serializer, Routes.deserializer, Charsets.UTF_8)
        }
    }
    private val discordOAuth = DiscordOAuth2(httpClient)

    override fun Application.install() {
        install(CallLogging) {
            level = Level.DEBUG
        }

        install(Locations)

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JSContentConverter(deserializer, serializer, Charsets.UTF_8))
        }

        install(Compression, Compression.Configuration::default)

        install(CORS) {
            anyHost()
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Patch)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
        }

        install(StatusPages) {
            exception<Throwable> {
                val http = it as? HttpError ?: InternalServerError(it)
                val json = http.toJson()
                call.respond(json)
            }
        }

        val authKey = environment.config.property("ktor.auth.discord.name").getString()
        install(Routing) {
            route("/dashboard") {
                authenticate(authKey) {
                    get("/auth") {
                        val session = call.oAuth2Session ?: throw badRequest("Could not complete OAuth2 flow!")
                        OAuth2.cookies(session, call.request.origin.host).forEach { call.response.cookies.append(it) }
                    }
                    get("/user") {
                        val session = call.oAuth2Session ?: return@get call.redirectToAuth()
                        val user = discordOAuth.user(session)

                    }
                    get("/guilds") {
                        val session = call.oAuth2Session ?: return@get call.redirectToAuth()
                        val guilds = discordOAuth.guilds(session).asSequence().map {
                            JSObject {
                                "name" to it.name
                                "id" to it.idLong
                            }
                        }.toJSArray()
                    }
                }
            }
        }
    }

    private val ApplicationCall.oAuth2Session get() = authentication.principal<OAuth2Session>()
    private suspend fun ApplicationCall.redirectToAuth() = respondRedirect(url = "/dashboard/auth")
}
