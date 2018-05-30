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
package xyz.laxus.wyvern.internal

import spark.Request
import spark.Response
import spark.RouteImpl
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.http.CallContext

/**
 * @author Kaidan Gustave
 */
internal abstract class APIRoute<R>(
    private val api: API,
    path: String
): RouteImpl(path) {
    override fun handle(request: Request?, response: Response?): R {
        requireNotNull(request) { "Internal spark request was null! Please report this to the maintainers!" }
        requireNotNull(response) { "Internal spark response was null! Please report this to the maintainers!" }
        val context = CallContext(api, request!!, response!!)
        return handle(context)
    }

    protected abstract fun handle(context: CallContext): R
}
