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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.wyvern.context

import org.eclipse.jetty.http.HttpStatus
import xyz.laxus.wyvern.http.header.ContentType

/**
 * @author Kaidan Gustave
 */
@ContextDsl
class Response(val context: RouteContext, private val base: spark.Response) {
    var status: HttpStatus.Code
        get() = status()
        set(value) = status(value)

    fun status() = checkNotNull(HttpStatus.getCode(base.status()))

    fun status(status: Int) {
        base.status(status)
    }

    fun status(status: HttpStatus.Code) {
        status(status.code)
    }

    fun header(header: String, value: String?) {
        base.raw().setHeader(header, value)
    }

    fun contentType(value: ContentType?) {
        header("Content-Type", value?.let { "$it" })
    }

    fun redirect(location: String, status: HttpStatus.Code = status()) {
        base.redirect(location, status.code)
    }
}