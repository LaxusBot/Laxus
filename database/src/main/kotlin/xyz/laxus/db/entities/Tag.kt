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
package xyz.laxus.db.entities

import xyz.laxus.db.DBGlobalTags
import xyz.laxus.db.DBLocalTags
import xyz.laxus.db.sql.getNullLong
import java.sql.ResultSet

data class Tag(val name: String, val content: String, val ownerId: Long?, val guildId: Long? = null) {
    fun isGlobal() = guildId === null

    fun isOverride() = ownerId === null

    fun edit(newContent: String) {
        DBLocalTags.updateTag(this.copy(content = newContent))
    }

    fun delete() {
        DBLocalTags.deleteTag(this)
    }

    fun override(guildId: Long? = null) {
        val override = this.copy(ownerId = null, guildId = guildId)
        if(override.guildId === null) {
            DBGlobalTags.overrideTag(override)
        } else {
            DBLocalTags.overrideTag(override)
        }
    }

    companion object {
        const val MaxNameLength = 50
        const val MaxContentLength = 1900

        internal fun local(results: ResultSet) = Tag(
            name = results.getString("name"),
            content = results.getString("content"),
            ownerId = results.getNullLong("owner_id"),
            guildId = results.getLong("guild_id")
        )

        internal fun global(results: ResultSet) = Tag(
            name = results.getString("name"),
            content = results.getString("content"),
            ownerId = results.getNullLong("owner_id")
        )
    }
}