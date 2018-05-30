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

import com.typesafe.config.Config
import com.typesafe.config.ConfigObject
import xyz.laxus.util.*
import xyz.laxus.util.collections.concurrentHashMap
import xyz.laxus.util.delegation.systemProperty
import xyz.laxus.util.reflect.hasAnnotation
import xyz.laxus.util.reflect.isType
import xyz.laxus.wyvern.annotation.Handle
import xyz.laxus.wyvern.annotation.Route
import xyz.laxus.wyvern.annotation.SubRoute
import xyz.laxus.wyvern.http.HttpMethod
import xyz.laxus.wyvern.http.header.ContentType
import xyz.laxus.wyvern.internal.APIRoute
import xyz.laxus.wyvern.internal.RouteRunner
import xyz.laxus.wyvern.internal.annotation.responseHeaders
import xyz.laxus.wyvern.plugins.IPlugin
import java.nio.charset.Charset
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility.*
import kotlin.reflect.full.*
import spark.RouteImpl as SparkRoute
import spark.Service as SparkService

/**
 * @author Kaidan Gustave
 */
class API private constructor() {
    companion object {
        // accessor
        @JvmStatic fun start(): API {
            val api = API()
            api.configureStart()
            return api
        }

        ///////////////////////
        // Private Functions //
        ///////////////////////

        @JvmStatic private val KCallable<*>.isEasyConstructor: Boolean get() {
            return visibility == PUBLIC && valueParameters.count { !it.isOptional } > 0
        }

        @JvmStatic private fun findEasyInstance(klazz: KClass<*>, includeCompanion: Boolean = false): Any? {
            klazz.objectInstance?.let { return it }
            klazz.companionObjectInstance?.takeIf { includeCompanion }?.let { return it }
            val constructor = klazz.constructors.find { it.isEasyConstructor }
            return constructor?.call()
        }

        @JvmStatic private fun findSubHandlers(klazz: KClass<*>, any: Any): List<Any> {
            return klazz.memberProperties.mapNotNull handlers@ {
                if(it.hasAnnotation<SubRoute>() && it.visibility == PUBLIC) {
                    return@handlers it.call(any)
                }
                return@handlers null
            } + klazz.java.declaredClasses.mapNotNull { findEasyInstance(it.kotlin) }
        }
    }

    private var _defaultContentType = ContentType.Application.Json
    private var _defaultCharset = Charsets.UTF_8
    private var _logName = API::class.qualifiedName ?: "xyz.laxus.wyvern.API"
    private val _configResourceName by systemProperty("wyvern.api.config", default = "api.conf")
    private val _plugins = concurrentHashMap<String, IPlugin>()

    internal val spark by lazy { checkNotNull(SparkService.ignite()) }

    val log by lazy { createLogger(_logName) }
    val defaultContentType get() = _defaultContentType
    val defaultCharset get() = _defaultCharset
    val config by lazy {
        val resource = _configResourceName.removePrefix("/")
        val config = loadConfig(resource)
        check(config.isResolved) { "Configuration '$resource' was not found!" }
        return@lazy config
    }

    fun register(any: Any) {
        val klazz = any::class
        val route = checkNotNull(klazz.findAnnotation<Route>()) {
            "$klazz is not annotated with @Route!"
        }
        registerWithRoute(any, klazz, route)
    }

    fun stop() {
        spark.stop()
        _plugins.values.forEach { it.stop() }
    }

    ////////////////////////
    // Internal Functions //
    ////////////////////////

    internal fun newRoute(method: HttpMethod, route: APIRoute<*>) {
        spark.addRoute(method.toSparkHttpMethod(), route)
    }

    ///////////////////////
    // Private Functions //
    ///////////////////////

    private fun configureStart() {

        // load and process configurations

        config.int("api.port")?.let(spark::port)

        config.config("api.default")?.let {
            it.string("charset")?.let {
                _defaultCharset = Charset.forName(it)
            }

            it.string("mime")?.let {
                _defaultContentType = ContentType.parse(it).modifyIf({ it.charset != defaultCharset }) {
                    it.withCharset(defaultCharset)
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

                    if(!pluginKlass.isSubclassOf(IPlugin::class)) return@forEach log.warn {
                        "$pluginKlass is not a subclass of ${IPlugin::class}"
                    }

                    val validConstructor = pluginKlass.constructors.firstOrNull valid@ {
                        if(it.visibility != PUBLIC) return@valid false
                        val params = it.valueParameters
                        if(params.isEmpty() || params.size > 1)
                            return@valid false
                        return@valid params[0].isType<Config>()
                    }

                    val instance = (validConstructor?.call(pluginConfig) ?: findEasyInstance(pluginKlass)) as? IPlugin

                    if(instance === null) return@forEach log.warn { "No valid constructor for class $pluginKlass" }

                    _plugins[pluginRegistryName] = instance
                }

                it.klass?.let { pluginKlass ->
                    if(!pluginKlass.isSubclassOf(IPlugin::class)) return@forEach log.warn {
                        "$pluginKlass is not a subclass of ${IPlugin::class}"
                    }

                    val instance = findEasyInstance(pluginKlass) as? IPlugin ?: return@forEach log.warn {
                        "No valid constructor for class $pluginKlass"
                    }

                    _plugins[pluginKlass.simpleName!!] = instance
                }

                log.warn("Could not register plugin with value: ${it.unwrapped()}")
            }
        }

        // start resources, plugins, etc.

        startPlugins()
    }

    private fun startPlugins() {
        _plugins.values.forEach { it.start() }
    }

    private fun registerWithRoute(
        any: Any,
        klazz: KClass<*>,
        route: Route,
        ancestors: List<Route> = emptyList(),
        headers: Map<String, String> = emptyMap()
    ) {
        val responseHeaders = headers + klazz.responseHeaders
        findSubHandlers(klazz, any).forEach {
            val subKlazz = it::class
            val subRoute = checkNotNull(subKlazz.findAnnotation<Route>()) { "$subKlazz is not annotated with @Route!" }
            registerWithRoute(it, subKlazz, subRoute, ancestors + route, responseHeaders)
        }
        val prefix = ancestors.joinToString(separator = "") { it.path }
        klazz.functions.filter { it.hasAnnotation<Handle>() && it.visibility == PUBLIC }.map {
            val handle = checkNotNull(it.findAnnotation<Handle>())
            return@map RouteRunner(
                method = handle.method,
                path = prefix + route.path + handle.extension,
                api = this,
                base = any,
                function = it,
                headers = responseHeaders
            )
        }.forEach { it.generate() }
    }
}
