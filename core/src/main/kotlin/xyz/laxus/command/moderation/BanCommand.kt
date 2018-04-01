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
package xyz.laxus.command.moderation

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.ErrorResponse
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.entities.ModLog
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.banFrom
import xyz.laxus.util.formattedName
import xyz.laxus.util.parseModeratorArgument
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class BanCommand: Command(ModeratorGroup) {
    override val name = "Ban"
    override val arguments = "[@User] <Reason>"
    override val help = "Bans a user from the server."
    override val botPermissions = arrayOf(Permission.BAN_MEMBERS)

    override suspend fun execute(ctx: CommandContext) {
        val modArgs = parseModeratorArgument(ctx.args) ?: return ctx.invalidArgs()

        val targetId = modArgs.first
        val reason = modArgs.second

        val target = try {
            ctx.jda.retrieveUserById(targetId).await()
        } catch(e: ErrorResponseException) {
            if(e.errorResponse == ErrorResponse.UNKNOWN_USER) return ctx.replyError {
                "Could not find a user with ID: $targetId!"
            }
            return ctx.replyError("An unexpected error occurred when finding a user with ID: $targetId!")
        }

        if(target === null) {
            return ctx.replyError("Could not find a user with ID: $targetId")
        }

        val guild = ctx.guild

        // Error Responses
        when {
            ctx.selfUser == target -> return ctx.replyError {
                "I cannot ban myself from the server!"
            }
            ctx.author == target -> return ctx.replyError {
                "You cannot ban yourself from the server!"
            }
            guild.owner.user == target -> return ctx.replyError {
                "You cannot ban ${target.formattedName(true)} because they are the owner of the server!"
            }
            guild.isMember(target) -> {
                val member = guild.getMember(target)!! // Should not be null
                when {
                    !ctx.selfMember.canInteract(member) -> return ctx.replyError {
                        "I cannot ban ${target.formattedName(true)}!"
                    }
                    !ctx.member.canInteract(member) -> return ctx.replyError {
                        "You cannot ban ${target.formattedName(true)}!"
                    }
                }
            }
        }

        try {
            target.banFrom(guild, 1, reason).await()
        } catch(t: Throwable) {
            return ctx.replyError("An error occurred while banning ${target.formattedName(true)}")
        }

        ctx.replySuccess("${target.formattedName(true)} was banned from the server.")

        launch(coroutineContext) {
            ModLog.newBan(ctx.member, target, reason)
        }
    }
}
