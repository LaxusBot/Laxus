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
package xyz.laxus.util.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

private typealias AuthorizationResolver = HttpRequestBuilder.() -> String?

class AuthorizationHeaders private constructor(private val resolvers: List<AuthorizationResolver>) {
    class Configuration internal constructor() {
        internal val resolvers = mutableListOf<AuthorizationResolver>()
        internal fun build() = AuthorizationHeaders(resolvers)
    }

    companion object Feature: HttpClientFeature<Configuration, AuthorizationHeaders> {
        override val key = AttributeKey<AuthorizationHeaders>("AuthorizationHeaders")
        override suspend fun prepare(block: Configuration.() -> Unit) = Configuration().apply(block).build()
        override fun install(feature: AuthorizationHeaders, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                with(feature) { resolvers.asSequence().mapNotNull { context.it() }.firstOrNull() }?.let {
                    context.headers[HttpHeaders.Authorization] = it
                }
            }
        }
    }
}

@DslMarker
private annotation class AuthorizationHeadersDsl

@AuthorizationHeadersDsl
suspend inline fun HttpClientConfig.authorizationHeaders(
    crossinline block: AuthorizationHeaders.Configuration.() -> Unit
) = install(AuthorizationHeaders) { block() }
@AuthorizationHeadersDsl
fun AuthorizationHeaders.Configuration.resolver(block: AuthorizationResolver) { this.resolvers += block }
@AuthorizationHeadersDsl
fun AuthorizationHeaders.Configuration.resolver(host: String, authorization: String?) = resolver {
    if(url.host != host) null else authorization
}