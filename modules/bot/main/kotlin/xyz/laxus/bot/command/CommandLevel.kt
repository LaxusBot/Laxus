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
package xyz.laxus.bot.command

import xyz.laxus.bot.utils.db.isMod
import xyz.laxus.bot.utils.jda.isAdmin

enum class CommandLevel(val guildOnly: Boolean = false, val test: (CommandContext) -> Boolean = { true }) {
    PUBLIC,
    MODERATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin || ctx.member.isMod }),
    ADMINISTRATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin }),
    SERVER_OWNER(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isOwner }),
    SHENGAERO(test = { ctx -> ctx.isDev }),
    DEFAULT;
}
