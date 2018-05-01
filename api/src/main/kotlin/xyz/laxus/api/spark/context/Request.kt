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
@file:Suppress("unused")
package xyz.laxus.api.spark.context

import spark.QueryParamsMap
import spark.Session
import spark.route.HttpMethod
import xyz.laxus.api.util.ContentType

/**
 * @author Kaidan Gustave
 */
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

    inline fun <reified T> attribute(attribute: String): T? {
        return attributeOf(attribute) as? T
    }
}