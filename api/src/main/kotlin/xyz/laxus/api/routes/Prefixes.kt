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

package xyz.laxus.api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import me.kgustave.json.JSObject
import me.kgustave.json.toJSArray
import xyz.laxus.api.util.respondJson
import xyz.laxus.db.DBPrefixes
import xyz.laxus.util.ignored

/**
 * @author Kaidan Gustave
 */
@Path("/prefixes")
object Prefixes : RouteController() {
    private const val GUILD_ID = "{guild.id}"

    @Route(Method.GET, "/$GUILD_ID")
    suspend fun CallContext.get() {
        val guildId = ignored(null) { call.parameters["guild.id"]?.toLong() }
        if(guildId === null) {
            return call.respondJson(status = HttpStatusCode.BadRequest) {
                "message" to "Invalid parameter for 'guild.id'"
            }
        }

        val prefixes = DBPrefixes.getPrefixes(guildId)

        call.respondJson(status = HttpStatusCode.OK) {
            "guild_id" to "$guildId"
            "prefixes" to prefixes.toJSArray()
        }
    }

    @Route(Method.POST, "/$GUILD_ID")
    suspend fun CallContext.post() {
        val guildId = ignored(null) { call.parameters["guild.id"]?.toLong() }
        if(guildId === null) {
            return call.respondJson(status = HttpStatusCode.BadRequest) {
                "message" to "Invalid parameter for 'guild.id'"
            }
        }

        val body = call.receive<JSObject>()

        body.array("prefixes").forEach {
            it ?: return@forEach
            DBPrefixes.addPrefix(guildId, "$it")
        }

        call.response.status(HttpStatusCode.Created)
        call.respond(HttpStatusCode.Created)
    }

    @Route(Method.DELETE, "/$GUILD_ID")
    suspend fun CallContext.delete() {
        val guildId = ignored(null) { call.parameters["guild.id"]?.toLong() }
        if(guildId === null) {
            return call.respondJson(status = HttpStatusCode.BadRequest) {
                "message" to "Invalid parameter for 'guild.id'"
            }
        }

        val body = call.receive<JSObject>()

        body.array("prefixes").forEach {
            it ?: return@forEach
            DBPrefixes.removePrefix(guildId, "$it")
        }

        call.response.status(HttpStatusCode.OK)
        call.respond(HttpStatusCode.OK)
    }
}