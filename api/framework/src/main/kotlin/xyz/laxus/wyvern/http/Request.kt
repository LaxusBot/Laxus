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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.wyvern.http

import spark.QueryParamsMap
import spark.Session
import spark.route.HttpMethod
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.http.header.ContentType
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

/**
 * @author Kaidan Gustave
 */
class Request internal constructor(val context: CallContext, private val base: spark.Request) {
    val api: API get() = context.api
    val attributes: Set<String> get() = base.attributes()
    val body: String get() = base.body()
    val byteBody: ByteArray get() = base.bodyAsBytes()
    val contentLength: Int get() = base.contentLength()
    val contentType: ContentType by lazy { base.contentType()?.let { ContentType.parse(it) } ?: api.defaultContentType }
    val contextPath: String get() = base.contextPath()
    val cookies: Map<String, String> get() = base.cookies()
    val headers: Set<String> get() = base.headers()
    val ip: String get() = base.ip()
    val method: HttpMethod get() = checkNotNull(HttpMethod.valueOf(base.requestMethod().toLowerCase()))
    val params: Map<String, String> get() = base.params()
    val pathInfo: String get() = base.pathInfo()
    val port: Int get() = base.port()
    val protocol: String get() = base.protocol()
    val queryMap: QueryParamsMap get() = base.queryMap()
    val queryParams: Set<String> get() = base.queryParams()
    val session: Session? get() = base.session(false)
    val uri: String get() = base.uri()
    val url: String get() = base.url()
    val userAgent: String? get() = base.userAgent()

    fun param(name: String): String {
        return requireNotNull(base.params(name)) {
            "Route param with name '$name' was null or not found!"
        }
    }

    fun header(header: String): String? {
        return base.headers(header)
    }

    fun queryParam(param: String): String? {
        return base.queryParams(param)
    }

    fun queryParams(param: String): Array<out String> {
        return base.queryParamsValues(param)
    }

    fun attributeOf(attribute: String): Any? {
        return base.attribute(attribute)
    }

    fun attribute(attribute: String, value: Any?) {
        base.attribute(attribute, value)
    }

    fun <T: Any> attributeOf(attribute: String, type: KClass<T>): T? {
        return type.safeCast(attributeOf(attribute))
    }

    fun cookie(name: String): String? {
        return base.cookie(name)
    }

    fun session(): Session {
        return base.session(false)
    }

    fun session(create: Boolean): Session = checkNotNull(base.session(create)) { "Session returned was null!" }
}