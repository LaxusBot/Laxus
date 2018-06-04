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
import xyz.laxus.util.collections.linkedListOf
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
import xyz.laxus.wyvern.internal.reflect.runInvocationSafe
import xyz.laxus.wyvern.plugins.IPlugin
import java.nio.charset.Charset
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility.*
import kotlin.reflect.full.*
import spark.RouteImpl as SparkRoute
import spark.Service as SparkService
import kotlin.jvm.JvmOverloads as Overloads
import kotlin.jvm.JvmStatic as Static

/**
 * @author Kaidan Gustave
 */
class API private constructor(defaultConfig: String?) {
    companion object {
        /** `DefaultConfigName = "api.conf"` */
        private const val DefaultConfigName = "api.conf"

        ///////////////////////
        // Accessor Function //
        ///////////////////////

        /**
         * Starts an [API] instance.
         *
         * Users may optionally specify a [config] name, which
         * should be a resource.
         *
         * If no [config] is specified, the [default][DefaultConfigName]
         * will be used instead.
         *
         * @param config The config name, default `null`
         *
         * @return A newly started [API] instance.
         */
        @Overloads @Static fun start(config: String? = null): API {
            return API(config).also { it.start() }
        }

        /////////////
        // Private //
        /////////////

        /** `true` if this is a constructor that is easily instantiable.  */
        @Static private val KCallable<*>.isEasyConstructor: Boolean get() {
            return visibility == PUBLIC && valueParameters.count { !it.isOptional } == 0
        }

        /**
         * Finds an easily retrieved instance of the [klazz], possibly
         * instantiating one if necessary.
         *
         * To instantiate an instance easily, there must exist at least one
         * [easy constructor][KCallable.isEasyConstructor].
         *
         * @param klazz The type to find an instance for.
         * @param includeCompanion Whether an existing companion object
         * should be considered, default `true`.
         *
         * @return An easily retrieved instance of [klazz].
         */
        @Static private fun findEasyInstance(klazz: KClass<*>, includeCompanion: Boolean = false): Any? {
            klazz.objectInstance?.let { return it }
            klazz.companionObjectInstance?.takeIf { includeCompanion }?.let { return it }
            val constructor = klazz.constructors.find { it.isEasyConstructor }
            return runInvocationSafe { constructor?.call() }
        }

        /**
         * Finds any sub-routes.
         *
         * Sub-handlers are properties marked with [@SubRoute][SubRoute],
         * or nested classes marked with both [@SubRoute][SubRoute] and
         * [@Route][SubRoute].
         *
         * @param klazz The [KClass] to get sub-routes from.
         * @param any The instance of [KClass] to get sub-routes using.
         *
         * @return All sub-routes from the [klazz].
         */
        @Static private fun findSubRoutes(klazz: KClass<*>, any: Any): List<Any> {
            require(klazz.isInstance(any)) { "$any is not an instance of $klazz" }

            val subRoutes = linkedListOf<Any>()
            klazz.memberProperties.mapNotNullTo(subRoutes) subRoutes@ {
                if(it.hasAnnotation<SubRoute>() && it.visibility == PUBLIC) {
                    return@subRoutes runInvocationSafe { it.call(any) }
                }
                return@subRoutes null
            }
            klazz.java.declaredClasses.mapNotNullTo(subRoutes) subRoutes@ {
                val subKlass = it.kotlin
                if(subKlass.isInner) unsupported { "Inner class sub-handlers are not yet supported!" }
                if(!subKlass.hasAnnotation<SubRoute>() || !subKlass.hasAnnotation<Route>()) {
                    return@subRoutes null
                }
                return@subRoutes findEasyInstance(subKlass)
            }
            return subRoutes
        }
    }

    ///////////////////
    // Pseudo Fields //
    ///////////////////

    @Volatile private var _running = false
    private var _defaultContentType = ContentType.Application.Json
    private var _defaultCharset = Charsets.UTF_8
    private var _logName = API::class.qualifiedName ?: "xyz.laxus.wyvern.API"
    private var _port = SparkService.SPARK_DEFAULT_PORT
    private val _configResourceName by systemProperty("wyvern.api.config", default = defaultConfig ?: DefaultConfigName)
    private val _plugins = concurrentHashMap<String, IPlugin>()

    /////////////////////////
    // Internal Properties //
    /////////////////////////

    internal val spark by lazy { checkNotNull(SparkService.ignite()) }

    ///////////////////////
    // Public Properties //
    ///////////////////////

    val log by lazy { createLogger(_logName) }
    val defaultContentType get() = _defaultContentType
    val defaultCharset get() = _defaultCharset
    val config by lazy {
        val resource = _configResourceName.removePrefix("/")
        val config = loadConfig(resource)
        check(config.isResolved) { "Configuration '$resource' was not found!" }
        return@lazy config
    }

    //////////////////////
    // Public Functions //
    //////////////////////

    fun start() {
        // check if we're running already
        require(!_running) { "API is already running!" }

        // configure
        configureStart()

        // start spark
        spark.port(_port)
        spark.init()

        // start resources, plugins, etc.
        startPlugins()

        // flag running
        _running = true
    }

    fun register(any: Any) {
        val klazz = any::class
        val route = checkNotNull(klazz.findAnnotation<Route>()) {
            "$klazz is not annotated with @Route!"
        }
        registerWithRoute(any, klazz, route)
    }

    fun stop() {
        // stop spark
        spark.stop()

        // stop resources, plugins, etc.
        _plugins.values.forEach { it.stop() }

        _running = false
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

        config.int("api.port")?.let { _port = it }

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

        findSubRoutes(klazz, any).forEach {
            val subKlazz = it::class
            val subRoute = checkNotNull(subKlazz.findAnnotation<Route>()) { "$subKlazz is not annotated with @Route!" }
            registerWithRoute(it, subKlazz, subRoute, ancestors + route, responseHeaders)
        }

        val prefix = ancestors.joinToString(separator = "") { it.path }
        klazz.functions.mapNotNull runners@ {
            if(it.visibility != PUBLIC) return@runners null
            val handle = it.findAnnotation<Handle>() ?: return@runners null
            return@runners RouteRunner(
                method = handle.method,
                path = prefix + route.path + handle.extension,
                api = this,
                base = any,
                function = it,
                headers = responseHeaders
            )
        }.forEach(RouteRunner::generate)
    }
}
