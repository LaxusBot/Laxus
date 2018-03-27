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
@file:Suppress("Unused")
package xyz.laxus.api

import kotlinx.coroutines.experimental.channels.Channel
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject
import spark.QueryParamsMap
import spark.RouteImpl
import spark.Session
import spark.route.HttpMethod
import xyz.laxus.api.util.concurrent.task

@DslMarker
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ContextDsl

class AsyncRoute(private val method: HttpMethod, path: String, private val handle: suspend RouteContext.() -> Any?): RouteImpl(path) {
    override fun handle(request: spark.Request, response: spark.Response): Any? {
        val task = task {
            val context = RouteContext(request, response)
            context.handle()
            context.finish() // This will automatically complete the request
            return@task context.receive()
        }

        return task.get()
    }

    override fun toString(): String {
        return "$method - $path"
    }
}

@ContextDsl
class RouteContext(request: spark.Request, response: spark.Response) {
    val request: Request = Request(this, request)
    val response: Response = Response(this, response)

    private val channel = Channel<Any>(1)

    @PublishedApi
    internal suspend inline fun sendJson(block: JSObject.() -> Unit) = send(jsonObject(block))

    @PublishedApi
    internal suspend fun send(any: Any) {
        if(channel.isClosedForSend || channel.isFull)
            return
        return channel.send(any)
    }

    @PublishedApi
    internal suspend fun receive(): Any {
        if(channel.isClosedForReceive || channel.isEmpty)
            return ""
        return channel.receive()
    }

    suspend fun finish() = send("")
}

@ContextDsl
class Request(val context: RouteContext, private val base: spark.Request) {
    val headers: Set<String> get() = base.headers()
    val queryMap: QueryParamsMap get() = base.queryMap()
    val queryParams: Set<String> get() = base.queryParams()
    val attributes: Set<String> get() = base.attributes()
    val cookies: Map<String, String> get() = base.cookies()
    val contentType: ContentType? by lazy { base.contentType()?.let { ContentType.parse(it) } }
    val contentLength: Int get() = base.contentLength()
    val contextPath: String get() = base.contextPath()
    val ip: String get() = base.ip()
    val uri: String get() = base.uri()
    val url: String get() = base.url()
    val port: Int get() = base.port()
    val body: String get() = base.body()
    val byteBody: ByteArray get() = base.bodyAsBytes()
    val session: Session? get() = base.session(false)
    val protocol: String get() = base.protocol()
    val pathInfo: String get() = base.pathInfo()
    val method: HttpMethod get() = checkNotNull(HttpMethod.valueOf(base.requestMethod().toLowerCase()))

    fun header(header: String): String? {
        return base.headers(header)
    }

    fun queryParam(param: String): String? {
        return base.queryParams(param)
    }

    fun attributeOf(attribute: String): Any? {
        return base.attribute(attribute)
    }

    fun attribute(attribute: String, value: Any?) {
        base.attribute(attribute, value)
    }

    fun cookie(name: String): String? {
        return base.cookie(name)
    }

    fun session(create: Boolean = false): Session {
        return checkNotNull(base.session(create)) {
            "Session returned was null!"
        }
    }
}

@ContextDsl
class Response(val context: RouteContext, private val base: spark.Response) {
    var status: Int
        get() = base.status()
        set(value) = base.status(status)

    fun status(): Int {
        return base.status()
    }

    fun status(status: Int) {
        base.status(status)
    }

    fun contentType(value: ContentType) {
        base.header("Content-Type", "$value")
    }

    fun header(header: String, value: String) {
        base.header(header, value)
    }

    fun redirect(location: String, status: Int = this.status) {
        base.redirect(location, status)
    }
}

@ContextDsl
inline fun <reified T> Request.attribute(attribute: String): T? {
    return attributeOf(attribute) as? T
}

@ContextDsl
suspend inline fun Response.respondJson(status: Int = this.status, block: JSObject.() -> Unit) {
    contentType(appJson)
    this.status = status
    return context.sendJson(block)
}

fun path(path: String, group: () -> Unit) {
    service.path(path, group)
}

@ContextDsl
fun get(path: String, handle: suspend RouteContext.() -> Any?) {
    service.addRoute(HttpMethod.get, AsyncRoute(HttpMethod.get, path, handle))
}

@ContextDsl
fun post(path: String, handle: suspend RouteContext.() -> Any?) {
    service.addRoute(HttpMethod.post, AsyncRoute(HttpMethod.post, path, handle))
}