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
package xyz.laxus.api.oauth2

import io.ktor.config.ApplicationConfig
import io.ktor.http.HttpMethod

/**
 * @author Kaidan Gustave
 */
data class ClientInfo(
    val name: String,
    val id: String,
    val secret: String,
    val method: HttpMethod,
    val defaultScopes: List<String>
) {
    companion object {
        fun from(config: ApplicationConfig) = ClientInfo(
            name = config.property("name").getString(),
            id = config.property("client.id").getString(),
            secret = config.property("client.secret").getString(),
            method = config.propertyOrNull("http.method")?.getString()?.let { HttpMethod.parse(it) } ?: HttpMethod.Put,
            defaultScopes = config.property("default.scopes").getList()
        )
    }
}