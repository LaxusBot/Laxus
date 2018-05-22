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
package xyz.laxus.wyvern.internal

import spark.Request
import spark.Response
import spark.RouteImpl
import xyz.laxus.wyvern.context.RouteContext
import xyz.laxus.wyvern.http.header.ContentType

/**
 * @author Kaidan Gustave
 */
class NormalRoute(
    path: String,
    private val handle: RouteContext.() -> Any?
): RouteImpl(path) {
    override fun handle(request: Request, response: Response): Any? {
        val context = RouteContext(request, response, false)
        return context.handle()?.takeUnless { it === Unit }
    }
}