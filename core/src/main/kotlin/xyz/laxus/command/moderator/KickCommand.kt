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
package xyz.laxus.command.moderator

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.entities.ModLog
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.kick
import xyz.laxus.util.formattedName
import xyz.laxus.util.parseModeratorArgument
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class KickCommand: Command(ModeratorGroup) {
    override val name = "Kick"
    override val arguments = "[@User] <Reason>"
    override val help = "Kicks a user from the server."
    override val botPermissions = arrayOf(Permission.KICK_MEMBERS)

    override suspend fun execute(ctx: CommandContext) {
        val modArgs = parseModeratorArgument(ctx.args) ?: return ctx.invalidArgs()

        val targetId = modArgs.first
        val reason = modArgs.second

        val member = ctx.guild.getMemberById(targetId)

        if(member === null) {
            // Theoretically this should only happen if they use direct ID
            // as a reference. In the case they don't, well they know what
            // the fuck they are doing anyways.
            // Can't cover everything I assume.
            return ctx.replyError("Could not find a user with ID: $targetId")
        }

        val target = member.user
        val guild = ctx.guild

        // Error Responses
        when {
            ctx.selfUser == target -> return ctx.replyError {
                "I cannot kick myself from the server!"
            }
            ctx.author == target -> return ctx.replyError {
                "You cannot kick yourself from the server!"
            }
            guild.owner.user == target -> return ctx.replyError {
                "You cannot kick ${target.formattedName(true)} because they are the owner of the server!"
            }
            !ctx.selfMember.canInteract(member) -> return ctx.replyError {
                "I cannot kick ${target.formattedName(true)}!"
            }
            !ctx.member.canInteract(member) -> return ctx.replyError {
                "You cannot kick ${target.formattedName(true)}!"
            }
        }

        try {
            member.kick(reason).await()
        } catch(t: Throwable) {
            return ctx.replyError("An error occurred while kicking ${target.formattedName(true)}")
        }

        ctx.replySuccess("${target.formattedName(true)} was kicked from the server.")

        launch(coroutineContext) {
            ModLog.newKick(ctx.member, target, reason)
        }
    }
}
