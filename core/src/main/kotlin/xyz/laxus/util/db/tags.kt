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
package xyz.laxus.util.db

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import xyz.laxus.db.DBGlobalTags
import xyz.laxus.db.DBLocalTags
import xyz.laxus.db.entities.Tag

// Global

val JDA.tags get() = DBGlobalTags.getTags()
val User.tags get() = DBGlobalTags.getTags(idLong)

fun JDA.getTagByName(name: String): Tag? = DBGlobalTags.getTagByName(name)
fun JDA.createTag(name: String, content: String, owner: User) = DBGlobalTags.createTag(name, content, owner.idLong)
fun JDA.isTag(name: String): Boolean = DBGlobalTags.isTag(name)
fun JDA.findTags(query: String): List<Tag> = DBGlobalTags.findTags(query)

// Local

val Guild.tags get() = DBLocalTags.getTags(idLong)
val Member.tags get() = DBLocalTags.getTags(user.idLong, guild.idLong)

fun Guild.getTagByName(name: String): Tag? = DBLocalTags.getTagByName(name, idLong)
fun Guild.createTag(name: String, content: String, owner: Member) = DBLocalTags.createTag(name, content, owner.user.idLong, idLong)
fun Guild.isTag(name: String): Boolean = DBLocalTags.isTag(name, idLong)
fun Guild.findTags(query: String): List<Tag> = DBLocalTags.findTags(query, idLong)
