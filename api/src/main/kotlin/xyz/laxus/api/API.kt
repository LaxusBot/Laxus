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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "ObjectPropertyName")
package xyz.laxus.api

import com.typesafe.config.ConfigValue
import io.ktor.server.engine.*
import io.ktor.server.engine.ConnectorType.Companion.HTTPS
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.Logger
import xyz.laxus.api.modules.KtorModule
import xyz.laxus.api.util.connector
import xyz.laxus.util.*
import xyz.laxus.util.reflect.loadClass
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.coroutineContext
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf

/**
 * @author Kaidan Gustave
 */
object API {
    private lateinit var embeddedServer: NettyApplicationEngine
    private lateinit var _log: Logger
    @Volatile private var online = false


    val Server get() = embeddedServer
    val log get() = _log
    val isOnline get() = online

    fun init() {
        check(!::embeddedServer.isInitialized) { "Server is already initialized!" }
        val config = loadConfig("api.conf")
        val deployment = checkNotNull(config.obj("ktor.deployment")) {
            "api.conf does not have 'ktor.deployment' configuration"
        }
        val application = config.obj("ktor.application")

        _log = createLogger(application?.string("logger.name") ?: "Laxus API")

        embeddedServer = embeddedServer(Netty, applicationEngineEnvironment {
            connector(HTTPS) {
                this.host = deployment.string("host") ?: "0.0.0.0"
                this.port = deployment.int("port") ?: 8080
            }

            this.log = _log
            this.watchPaths = deployment.list("watch")?.mapNotNull(ConfigValue::string) ?: emptyList()

            application?.list("modules")?.forEach {
                val fqName = it?.string ?: return@forEach

                val objClass = checkNotNull(loadClass(fqName)) { "Cannot load class '$fqName'" }

                val isModuleObject = objClass.isSubclassOf(KtorModule::class)
                val isProviderCompanion = objClass.companionObject?.isSubclassOf(KtorModule.Provider::class) == true

                check(isModuleObject || isProviderCompanion) {
                    "Module '${objClass.simpleName}' does not implement ${KtorModule::class} and " +
                    "companion does not exist or doesn't implement ${KtorModule.Provider::class}!"
                }

                val moduleInstance = if(isModuleObject) {
                    checkNotNull(objClass.objectInstance as? KtorModule) {
                        "Module '${objClass.simpleName}' is not an object or does not implement ${KtorModule::class}"
                    }
                } else {
                    val companion = checkNotNull(objClass.companionObjectInstance as KtorModule.Provider<*>) {
                        "Module '${objClass.simpleName}' does not have a companion or companion " +
                        "does not implement ${KtorModule.Provider::class}"
                    }

                    companion.provide()
                }

                with(moduleInstance) {
                    module { load(config) }
                }
            }
        })
    }

    fun start() {
        check(::embeddedServer.isInitialized) { "Server has not been initialized!" }
        check(!online) { "Server is already online!" }

        embeddedServer.start()
        online = true
    }

    fun stop() {
        if(::embeddedServer.isInitialized) {
            embeddedServer.stop(5, 5, TimeUnit.SECONDS)
            online = false
        }
    }
}


suspend fun foo() = produce(coroutineContext) {
    println("World!")
    send(Unit)
}

fun main(args: Array<String>) {
    newSingleThreadContext("context").use {
        runBlocking(it) {
            val gen = foo()
            print("Hello ")
            gen.receive()
        }
    }
}