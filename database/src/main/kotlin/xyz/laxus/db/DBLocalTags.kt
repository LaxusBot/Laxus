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
package xyz.laxus.db

import xyz.laxus.db.entities.LocalTag
import xyz.laxus.db.entities.impl.LocalTagImpl
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("local_tags")
@Columns(
    Column("name", "$VARCHAR(50)", primary = true),
    Column("content", "$VARCHAR(1900)"),
    Column("owner_id", BIGINT, nullable = true),
    Column("guild_id", BIGINT, primary = true)
)
object DBLocalTags : Table() {
    fun getTags(guildId: Long): List<LocalTag> {
        val list = ArrayList<LocalTag>()
        connection.prepare("SELECT * FROM local_tags WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext {
                    list += LocalTagImpl(it)
                }
            }
        }
        return list
    }

    fun getTags(userId: Long, guildId: Long): List<LocalTag> {
        val list = ArrayList<LocalTag>()
        connection.prepare("SELECT * FROM local_tags WHERE owner_id = ? AND guild_id = ?") { statement ->
            statement[1] = userId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext {
                    list += LocalTagImpl(it)
                }
            }
        }
        return list
    }

    fun getTagByName(name: String, guildId: Long): LocalTag? {
        connection.prepare("SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?") { statement ->
            statement[1] = name
            statement[2] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return LocalTagImpl(it)
                }
            }
        }
        return null
    }

    fun createTag(name: String, content: String, ownerId: Long?, guildId: Long) {
        require(name.length <= 50) { "Tag name length exceeds maximum of 50 characters!" }
        require(content.length <= 1900) { "Tag content length exceeds maximum of 50 characters!" }

        connection.prepare("SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = name
            statement[2] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["name"] = name
                    it["content"] = content
                    it["owner_id"] = ownerId
                    it["guild_id"] = guildId
                }
            }
        }
    }

    fun updateTag(tag: LocalTag) {
        require(tag.name.length <= 50) { "Tag name length exceeds maximum of 50 characters!" }
        require(tag.content.length <= 1900) { "Tag content length exceeds maximum of 50 characters!" }

        connection.prepare("SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = tag.name
            statement[2] = tag.guildId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["content"] = tag.content
                }
            }
        }
    }

    fun deleteTag(tag: LocalTag) {
        connection.prepare("SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = tag.name
            statement[2] = tag.guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun overrideTag(tag: LocalTag) {
        require(tag.ownerId === null) { "Cannot override a local tag with non-null ownerId!" }

        connection.prepare("SELECT * FROM local_tags WHERE LOWER(name) = LOWER(?) AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = tag.name
            statement[2] = tag.guildId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["owner_id"] = tag.ownerId
                }
            }
        }
    }
}