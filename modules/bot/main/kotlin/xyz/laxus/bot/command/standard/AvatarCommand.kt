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
package xyz.laxus.bot.command.standard

import net.dv8tion.jda.core.Permission
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.commandError
import xyz.laxus.bot.command.commandErrorIf
import xyz.laxus.bot.utils.jda.boldFormattedName
import xyz.laxus.bot.utils.jda.embed
import xyz.laxus.bot.utils.jda.findMembers
import xyz.laxus.bot.utils.jda.findUsers
import xyz.laxus.bot.utils.multipleMembers
import xyz.laxus.bot.utils.multipleUsers
import xyz.laxus.bot.utils.noMatch

class AvatarCommand: Command(StandardGroup) {
    override val name = "Avatar"
    override val aliases = arrayOf("Avy", "Pfp")
    override val arguments = "<User>"
    override val help = "Gets a user's avatar."
    override val guildOnly = false
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        val temp = ctx.takeIf { ctx.isGuild }?.let {
            if(query.isEmpty()) return@let ctx.member
            val members = ctx.guild.findMembers(query)
            return@let when {
                members.isEmpty() -> null
                members.size > 1 -> commandError(members.multipleMembers(query))
                else -> members[0]
            }
        }
        val user = temp?.user ?: ctx.author.takeIf { query.isEmpty() } ?: ctx.jda.findUsers(query).let { users ->
            commandErrorIf(users.isEmpty()) { noMatch("users", query) }
            commandErrorIf(users.size > 1) { users.multipleUsers(query) }
            return@let users[0]
        }

        ctx.reply(embed {
            title { "Avatar For ${user.boldFormattedName}" }
            url { "${user.effectiveAvatarUrl}?size=1024" }
            image { "${user.effectiveAvatarUrl}?size=1024" }
            if(ctx.isGuild) {
                val member = ctx.guild.getMember(user)
                color { member?.color ?: ctx.selfMember.color }
            }
        })
    }
}
