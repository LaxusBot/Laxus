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
package xyz.laxus.db

import xyz.laxus.db.entities.ChannelLink
import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.sql.*

@TableName("channel_links")
@Columns(
    Column("guild_id", "BIGINT", primary = true),
    Column("voice_channel_id", "BIGINT", primary = true),
    Column("text_channel_id", "BIGINT")
)
object DBChannelLinks: Table() {
    fun isLinked(guildId: Long, voiceChannelId: Long, textChannelId: Long): Boolean {
        return connection.prepare(
            "SELECT * FROM channel_links WHERE guild_id = ? AND voice_channel_id = ? AND text_channel_id = ?"
        ) { statement ->
            statement[1] = guildId
            statement[2] = voiceChannelId
            statement[3] = textChannelId
            statement.executeQuery { it.next() }
        }
    }

    fun setChannelLink(link: ChannelLink) {
        connection.update("SELECT * FROM channel_links WHERE guild_id = ? AND voice_channel_id = ?") { statement ->
            statement[1] = link.guildId
            statement[2] = link.voiceChannelId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["text_channel_id"] = link.textChannelId
                } else it.insert {
                    it["guild_id"] = link.guildId
                    it["voice_channel_id"] = link.voiceChannelId
                    it["text_channel_id"] = link.textChannelId
                }
            }
        }
    }

    fun removeChannelLink(link: ChannelLink) {
        connection.update(
            "SELECT * FROM channel_links WHERE guild_id = ? AND voice_channel_id = ? AND text_channel_id = ?"
        ) { statement ->
            statement[1] = link.guildId
            statement[2] = link.voiceChannelId
            statement[3] = link.textChannelId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun getVoiceChannelLink(guildId: Long, voiceChannelId: Long): ChannelLink? {
        connection.prepare("SELECT * FROM channel_links WHERE guild_id = ? AND voice_channel_id = ?") { statement ->
            statement[1] = guildId
            statement[2] = voiceChannelId
            statement.executeQuery {
                if(it.next()) {
                    return ChannelLink(it)
                }
            }
        }
        return null
    }

    fun getTextChannelLinks(guildId: Long, textChannelId: Long): List<ChannelLink> {
        val links = arrayListOf<ChannelLink>()
        connection.prepare("SELECT * FROM channel_links WHERE guild_id = ? AND text_channel_id = ?") { statement ->
            statement[1] = guildId
            statement[2] = textChannelId
            statement.executeQuery {
                while(it.next()) {
                    links += ChannelLink(it)
                }
            }
        }
        return links
    }
}