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
@file:Suppress("ObjectPropertyName", "MemberVisibilityCanBePrivate")
package xyz.laxus.wyvern

import com.typesafe.config.ConfigObject
import xyz.laxus.util.*
import xyz.laxus.util.collections.concurrentHashMap
import xyz.laxus.util.delegation.systemProperty
import xyz.laxus.util.reflect.hasAnnotation
import xyz.laxus.wyvern.annotation.Handle
import xyz.laxus.wyvern.annotation.Route
import xyz.laxus.wyvern.annotation.SubRoute
import xyz.laxus.wyvern.annotation.responseHeaderAnnotations
import xyz.laxus.wyvern.http.header.ContentType
import xyz.laxus.wyvern.internal.RouteRunner
import xyz.laxus.wyvern.plugins.WyvernPlugin
import java.nio.charset.Charset
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import spark.Service as SparkService

/**
 * @author Kaidan Gustave
 */
object API {
    private var _defaultContentType = ContentType.Application.Json
    private var _defaultCharset = Charsets.UTF_8
    private var _logName = API::class.qualifiedName ?: "xyz.laxus.wyvern.API"
    private val _configResourceName by systemProperty("wyvern.api.config", default = "api.conf")
    private val _plugins = concurrentHashMap<String, WyvernPlugin>()

    internal val Spark by lazy { checkNotNull(SparkService.ignite()) }
    internal val Log by lazy { createLogger(_logName) }

    val DefaultContentType get() = _defaultContentType
    val DefaultCharset get() = _defaultCharset
    val Config by lazy {
        val resource = _configResourceName.removePrefix("/")
        val config = loadConfig(resource)
        check(config.isResolved) { "Configuration '$resource' was not found!" }
        return@lazy config
    }

    fun start() {
        configureStart()
        _plugins.values.forEach { it.start() }
    }

    fun stop() {
        Spark.stop()
        _plugins.values.forEach { it.stop() }
    }

    fun register(any: Any) {
        val klazz = any::class
        val route = checkNotNull(klazz.findAnnotation<Route>()) {
            "$klazz is not annotated with @Route!"
        }
        registerWithRoute(any, klazz, route)
    }



    ///////////////////////
    // Private Functions //
    ///////////////////////

    private fun configureStart() {
        val config = Config
        config.int("api.port")?.let(Spark::port)

        config.config("api.default")?.let {
            it.string("charset")?.let {
                _defaultCharset = Charset.forName(it)
            }

            it.string("mime")?.let {
                _defaultContentType = ContentType.parse(it).modifyIf({ it.charset != DefaultCharset }) {
                    it.withCharset(DefaultCharset)
                }
            }
        }

        config.string("api.log")?.let { _logName = it }

        config.list("api.routes")?.let {
            it.forEach {
                val klazz = checkNotNull(it.klass) { "Could not locate class: ${it.unwrapped()}" }
                val maybeInstance = findEasyInstance(klazz)
                val instance = checkNotNull(maybeInstance) { "Could not retrieve instance of class: $klazz" }
                register(instance)
            }
        }

        config.list("api.plugins")?.let {
            it.forEach {
                if(it is ConfigObject) {
                    val pluginConfig = it.config("config") ?: emptyConfig()
                    val pluginKlass = checkNotNull(it.klass("class")) { "Could not find class value!" }
                    val pluginRegistryName = it.string("name") ?: pluginKlass.simpleName!!

                    if(!pluginKlass.isSubclassOf(WyvernPlugin::class)) return@forEach Log.warn {
                        "$pluginKlass is not a subclass of ${WyvernPlugin::class}"
                    }

                    val instance = findEasyInstance(pluginKlass) as? WyvernPlugin ?: return@forEach Log.warn {
                        "No constructor for class: $pluginKlass had 0 parameters!"
                    }

                    _plugins[pluginRegistryName] = instance

                    return@forEach try { instance.apply(pluginConfig) } catch(t: Throwable) {
                        Log.error("Encountered an exception when applying plugin $pluginKlass", t)
                    }
                }

                it.klass?.let { pluginKlass ->
                    if(!pluginKlass.isSubclassOf(WyvernPlugin::class)) return@forEach Log.warn {
                        "$pluginKlass is not a subclass of ${WyvernPlugin::class}"
                    }

                    val instance = findEasyInstance(pluginKlass) as? WyvernPlugin ?: return@forEach Log.warn {
                        "No constructor for class: $pluginKlass had 0 parameters!"
                    }

                    _plugins[pluginKlass.simpleName!!] = instance

                    return@forEach try { instance.apply(emptyConfig()) } catch(t: Throwable) {
                        Log.error("Encountered an exception when applying plugin $pluginKlass", t)
                    }
                }

                Log.warn("Could not register plugin with value: ${it.unwrapped()}")
            }
        }
    }

    private fun findEasyInstance(
        klazz: KClass<*>,
        includeCompanion: Boolean = false
    ): Any? {
        klazz.objectInstance?.let { return it }
        klazz.companionObjectInstance?.takeIf { includeCompanion }?.let { return it }
        val constructor = klazz.constructors.find { it.isEasyConstructor }
        return constructor?.call()
    }

    private val KCallable<*>.isEasyConstructor get() = parameters.count { !it.isOptional } > 0

    private fun registerWithRoute(
        any: Any,
        klazz: KClass<*>,
        route: Route,
        ancestors: List<Route> = emptyList(),
        headers: Map<String, String> = emptyMap()
    ) {
        val responseHeaders = headers + klazz.responseHeaderAnnotations.associate { it.header to it.value }

        val subHandlers = findSubHandlers(klazz, any)

        subHandlers.forEach {
            val subKlazz = it::class
            val subRoute = checkNotNull(subKlazz.findAnnotation<Route>()) { "$subKlazz is not annotated with @Route!" }
            registerWithRoute(it, subKlazz, subRoute, ancestors + route, responseHeaders)
        }

        val prefix = ancestors.joinToString(separator = "") { it.path }
        klazz.functions.filter { it.hasAnnotation<Handle>() && it.visibility == KVisibility.PUBLIC }.map {
            val handle = checkNotNull(it.findAnnotation<Handle>())
            RouteRunner(prefix + route.path + handle.extension, handle.method, any, it, responseHeaders)
        }.forEach { it.generate() }
    }

    @JvmStatic private fun findSubHandlers(klazz: KClass<*>, any: Any): List<Any> {
        return klazz.memberProperties.mapNotNull handlers@ {
            if(it.hasAnnotation<SubRoute>() && it.visibility == KVisibility.PUBLIC) it.call(any) else null
        } + klazz.java.declaredClasses.mapNotNull {
            it.kotlin.takeIf { it.hasAnnotation<Route>() }?.objectInstance
        }
    }
}