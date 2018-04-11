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

import xyz.laxus.db.entities.Tag
import xyz.laxus.db.entities.impl.GlobalTagImpl
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("GLOBAL_TAGS")
@Columns(
    Column("NAME", "$VARCHAR(50)", unique = true),
    Column("CONTENT", "$VARCHAR(1900)"),
    Column("OWNER_ID", BIGINT, nullable = true)
)
object DBGlobalTags : Table() {
    fun getTags(): List<Tag> {
        val list = ArrayList<Tag>()
        connection.prepare("SELECT * FROM GLOBAL_TAGS") { statement ->
            statement.executeQuery {
                it.whileNext {
                    list += GlobalTagImpl(it)
                }
            }
        }
        return list
    }

    fun getTags(userId: Long): List<Tag> {
        val list = ArrayList<Tag>()
        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE OWNER_ID = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                it.whileNext {
                    list += GlobalTagImpl(it)
                }
            }
        }
        return list
    }

    fun getTagByName(name: String): Tag? {
        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)") { statement ->
            statement[1] = name
            statement.executeQuery {
                if(it.next()) {
                    return GlobalTagImpl(it)
                }
            }
        }
        return null
    }

    fun createTag(name: String, content: String, ownerId: Long) {
        require(name.length <= 50) { "Tag name length exceeds maximum of 50 characters!" }
        require(content.length <= 1900) { "Tag content length exceeds maximum of 50 characters!" }

        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["NAME"] = name
                    it["CONTENT"] = content
                    it["OWNER_ID"] = ownerId
                }
            }
        }
    }

    fun updateTag(tag: Tag) {
        require(tag.name.length <= 50) { "Tag name length exceeds maximum of 50 characters!" }
        require(tag.content.length <= 1900) { "Tag content length exceeds maximum of 50 characters!" }

        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = tag.name
            statement.executeQuery {
                if(it.next()) it.update {
                    it["CONTENT"] = tag.content
                }
            }
        }
    }

    fun deleteTag(name: String) {
        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun overrideTag(tag: Tag) {
        require(tag.ownerId === null) { "Cannot override a local tag with non-null ownerId!" }

        connection.prepare("SELECT * FROM GLOBAL_TAGS WHERE LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = tag.name
            statement.executeQuery {
                if(it.next()) it.update {
                    it["OWNER_ID"] = tag.ownerId
                }
            }
        }
    }
}