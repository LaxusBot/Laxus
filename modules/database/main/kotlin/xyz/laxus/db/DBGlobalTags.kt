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

import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.entities.Tag
import xyz.laxus.db.sql.*

/**
 * @author Kaidan Gustave
 */
@TableName("global_tags")
@Columns(
    Column("name", "VARCHAR(50)", primary = true),
    Column("content", "VARCHAR(1900)"),
    Column("owner_id", "BIGINT", nullable = true)
)
object DBGlobalTags: Table() {
    fun isTag(name: String): Boolean {
        return connection.prepare("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = name
            statement.executeQuery { it.next() }
        }
    }

    fun getTags(): List<Tag> {
        val list = arrayListOf<Tag>()
        connection.prepare("SELECT * FROM global_tags") { statement ->
            statement.executeQuery {
                while(it.next()) {
                    list += Tag.global(it)
                }
            }
        }
        return list
    }

    fun getTags(userId: Long): List<Tag> {
        val list = arrayListOf<Tag>()
        connection.prepare("SELECT * FROM global_tags WHERE owner_id = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                while(it.next()) {
                    list += Tag.global(it)
                }
            }
        }
        return list
    }

    fun findTags(query: String): List<Tag> {
        val list = arrayListOf<Tag>()
        connection.prepare("SELECT * FROM global_tags WHERE LOWER(name) ILIKE LOWER(?)") { statement ->
            statement[1] = "$query%"
            statement.executeQuery {
                while(it.next()) {
                    list += Tag.global(it)
                }
            }
        }
        return list
    }

    fun getTagByName(name: String): Tag? {
        connection.prepare("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = name
            statement.executeQuery {
                if(it.next()) {
                    return Tag.global(it)
                }
            }
        }
        return null
    }

    fun createTag(name: String, content: String, ownerId: Long?) {
        require(name.length <= Tag.MaxNameLength) {
            "Tag name length exceeds maximum of ${Tag.MaxNameLength} characters!"
        }
        require(content.length <= Tag.MaxContentLength) {
            "Tag content length exceeds maximum of ${Tag.MaxContentLength} characters!"
        }

        connection.update("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["name"] = name
                    it["content"] = content
                    it["owner_id"] = ownerId
                }
            }
        }
    }

    fun updateTag(tag: Tag) {
        require(tag.name.length <= Tag.MaxNameLength) {
            "Tag name length exceeds maximum of ${Tag.MaxNameLength} characters!"
        }
        require(tag.content.length <= Tag.MaxContentLength) {
            "Tag content length exceeds maximum of ${Tag.MaxContentLength} characters!"
        }

        connection.update("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = tag.name
            statement.executeQuery {
                if(it.next()) it.update {
                    it["content"] = tag.content
                }
            }
        }
    }

    fun deleteTag(tag: Tag) {
        connection.update("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = tag.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun overrideTag(tag: Tag) {
        require(tag.ownerId === null) { "Cannot override a local tag with non-null ownerId!" }

        connection.update("SELECT * FROM global_tags WHERE LOWER(name) = LOWER(?)") { statement ->
            statement[1] = tag.name
            statement.executeQuery {
                if(it.next()) it.update {
                    it["owner_id"] = tag.ownerId
                } else it.insert {
                    it["name"] = tag.name
                    it["content"] = tag.content
                    it["owner_id"] = tag.ownerId
                }
            }
        }
    }
}