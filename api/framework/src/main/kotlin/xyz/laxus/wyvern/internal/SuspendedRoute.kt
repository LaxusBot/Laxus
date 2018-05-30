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

import xyz.laxus.util.concurrent.task
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.http.CallContext

/**
 * @author Kaidan Gustave
 */
internal open class SuspendedRoute(
    api: API, path: String,
    private val handle: suspend CallContext.() -> Any?
): APIRoute<Any?>(api, path) {
    override fun handle(context: CallContext): Any? {
        val task = task { // TODO Add specific context support here
            return@task context.handle()
        }

        return task.get()
    }
}