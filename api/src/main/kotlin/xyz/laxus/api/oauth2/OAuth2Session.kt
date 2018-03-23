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

import com.jagrosh.jdautilities.oauth2.Scope
import com.jagrosh.jdautilities.oauth2.session.Session
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
interface OAuth2Session {
    val accessToken: String
    val refreshToken: String
    val scopes: Array<Scope>
    val tokenType: String
    val expiration: OffsetDateTime

    fun toImpl(): Impl = Impl(accessToken, refreshToken, scopes, tokenType, expiration)

    class Impl(
        private val _accessToken: String,
        private val _refreshToken: String,
        private val _scopes: Array<Scope>,
        private val _tokenType: String,
        private val _expiration: OffsetDateTime
    ): Session {
        override fun getScopes(): Array<Scope> = _scopes
        override fun getExpiration(): OffsetDateTime = _expiration
        override fun getAccessToken(): String = _accessToken
        override fun getTokenType(): String = _tokenType
        override fun getRefreshToken(): String = _refreshToken
    }
}