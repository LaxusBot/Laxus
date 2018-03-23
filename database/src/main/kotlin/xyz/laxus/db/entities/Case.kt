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
package xyz.laxus.db.entities

import xyz.laxus.db.DBCases
import java.sql.ResultSet

/**
 * @author Kaidan Gustave
 */
data class Case(
    val number: Int,
    val guildId: Long,
    val modId: Long,
    val targetId: Long,
    val isOnUser: Boolean,
    val action: Action,
    var reason: String? = null
) {
    internal constructor(results: ResultSet): this(
        number = results.getInt("CASE_NUMBER"),
        guildId = results.getLong("GUILD_ID"),
        modId = results.getLong("MOD_ID"),
        targetId = results.getLong("TARGET_ID"),
        isOnUser = results.getBoolean("IS_ON_USER"),
        action = Action.valueOf(results.getString("ACTION")),
        reason = results.getString("REASON")
    )

    fun updateReason(reason: String) {
        this.reason = reason
        DBCases.updateCase(this)
    }

    enum class Action {
        BAN,
        UNBAN,
        KICK,
        MUTE,
        UNMUTE,
        CLEAN,
        OTHER;
    }
}