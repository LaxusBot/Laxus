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
@file:Suppress("LiftReturnOrAssignment")

package xyz.laxus.api.modules

import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.contentType
import io.ktor.request.receiveText
import io.ktor.routing.Routing
import io.ktor.routing.route
import io.ktor.server.engine.ShutDownUrl
import me.kgustave.json.JSObject
import me.kgustave.json.exceptions.JSException
import me.kgustave.json.exceptions.JSSyntaxException
import me.kgustave.json.parseJsonObject
import org.slf4j.event.Level
import xyz.laxus.api.routes.RouteController
import xyz.laxus.api.util.HtmlContentType
import xyz.laxus.api.util.JsonContentType
import xyz.laxus.api.util.configure
import xyz.laxus.util.config
import xyz.laxus.util.enum
import xyz.laxus.util.list
import xyz.laxus.util.reflect.loadClass
import xyz.laxus.util.string
import kotlin.reflect.full.isSubclassOf

/**
 * @author Kaidan Gustave
 */
object Routes : KtorModule {
    override fun Application.load(config: Config) {
        // Install Call Logging
        install(CallLogging) {
            this.level = config.enum<Level>("ktor.application.logger.level") ?: Level.TRACE
        }

        if(config.string("ktor.deployment.environment") == "development") {
            install(ShutDownUrl.ApplicationCallFeature) {
                this.shutDownUrl = "/api/shutdown"
                this.exitCodeSupplier = { 0 }
            }
        }

        // Install Exception Handling
        installExceptionHandling()

        // Install Call Negotiation
        installContentNegotiation()

        // Install CORS
        install(CORS, CORS.Configuration::anyHost)

        // Install Auto Head Response
        install(AutoHeadResponse)

        // Install Routing
        installRouting(config)
    }

    private fun Application.installContentNegotiation() = install(ContentNegotiation) {
        configure(JsonContentType) {
            receive r@ {
                val bodyText = call.receiveText()
                if(call.request.contentType().match(JsonContentType)) {
                    return@r try { parseJsonObject(bodyText) } catch(e: JSSyntaxException) { null }
                }
                return@r null
            }

            send s@ { contentType, value ->
                if(contentType.match(JsonContentType) && value is JSObject) {
                    return@s value.toString()
                }

                if(contentType.match(HtmlContentType)) {
                    return@s value
                }

                return@s null
            }
        }
    }

    private fun Application.installExceptionHandling() = install(StatusPages) {
        exception<JSException> {
            call.response.status(HttpStatusCode.BadRequest)
            finish()
        }
        exception<Throwable> {
            if(it::class.java !in exceptions) {
                log.error("Internal Server Error", it)
                call.response.status(HttpStatusCode.InternalServerError)
            }
            finish()
        }
    }

    private fun Application.installRouting(config: Config) = install(Routing) {
        val routing = checkNotNull(config.config("ktor.application.routing"))
        route(routing.string("base") ?: "/api") {
            val controllers = routing.list("controllers") ?: emptyList<ConfigValue>()
            controllers.forEach {
                val fqName = it.string ?: return@forEach
                val objClass = checkNotNull(loadClass(fqName)) { "Cannot load class '$fqName'" }
                val isControllerObject = objClass.isSubclassOf(RouteController::class) &&
                                         objClass.objectInstance !== null

                check(isControllerObject) {
                    "'$objClass' is not a subclass of ${RouteController::class} or is not an object!"
                }

                val controller = objClass.objectInstance as RouteController
                controller.create(this)
            }
        }
    }
}
