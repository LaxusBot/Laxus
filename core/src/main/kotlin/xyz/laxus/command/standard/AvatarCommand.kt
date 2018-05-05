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
package xyz.laxus.command.standard

import net.dv8tion.jda.core.Permission
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.findMembers
import xyz.laxus.jda.util.findUsers
import xyz.laxus.util.formattedName
import xyz.laxus.util.multipleMembers
import xyz.laxus.util.multipleUsers
import xyz.laxus.util.noMatch

/**
 * @author Kaidan Gustave
 */
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
            if(query.isEmpty())
                return@let ctx.member
            val members = ctx.guild.findMembers(query)
            return@let when {
                members.isEmpty() -> null
                members.size > 1 -> return ctx.replyError(members.multipleMembers(query))
                else -> members[0]
            }
        }

        val user = temp?.user ?: ctx.author.takeIf { query.isEmpty() } ?: ctx.jda.findUsers(query).let { users ->
            return@let when {
                users.isEmpty() -> return ctx.replyError(noMatch("users", query))
                users.size > 1 -> return ctx.replyError(users.multipleUsers(query))
                else -> users[0]
            }
        }

        ctx.reply(embed {
            title { "Avatar For ${user.formattedName(true)}" }
            val imageUrl = "${user.effectiveAvatarUrl}?size=1024"
            url { imageUrl }
            image { imageUrl }
            if(ctx.isGuild) {
                val member = ctx.guild.getMember(user)
                color { member?.color ?: ctx.selfMember.color }
            }
        })
    }
}