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
@file:JvmName("Routing")

package xyz.laxus.api.modules

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.features.*
import io.ktor.http.ContentType
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpMethod
import io.ktor.locations.Locations
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import me.kgustave.json.ktor.server.JSContentConverter
import org.slf4j.event.Level
import xyz.laxus.api.error.HttpError
import xyz.laxus.api.error.InternalServerError
import xyz.laxus.api.oauth2.OAuth2Session

object Routing: Module {
    override fun Application.install() {
        install(CallLogging) {
            level = Level.DEBUG
        }

        install(Locations)

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JSContentConverter(charset = Charsets.UTF_8))
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
                    get("/auth") { handleDashboardAuth() }
                }
            }
        }
    }

    private fun PipelineContext<Unit, ApplicationCall>.handleDashboardAuth() {
        val session = call.authentication.principal<OAuth2Session>() ?: TODO("Handle null auth")
        call.request.cookies["access_token", CookieEncoding.BASE64_ENCODING]?.let {}
        OAuth2.cookies(session, call.request.origin.host).forEach {
            val cookies = call.response.cookies
            cookies.append(it)
        }
    }
}
