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
package xyz.laxus.api

import me.kgustave.json.parseJsonObject
import xyz.laxus.util.createLogger

/**
 * @author Kaidan Gustave
 */
object API {
    private val Log = createLogger(API::class)

    fun start() {
        port(8080)
        path("/api") {
            get("/hello") {
                response.respondJson(200) {
                    "message" to "Hello, World!"
                }
            }

            post("/prefixes/:guild.id") {
                val json = parseJsonObject(request.body)
                Log.info("\n${json.toJsonString(2)}")
                response.status(200)
            }
        }
    }
}