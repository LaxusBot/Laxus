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
package xyz.laxus.command.administrator

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.findRoles
import xyz.laxus.jda.util.giveRole
import xyz.laxus.jda.util.removeRole
import xyz.laxus.util.*
import xyz.laxus.util.db.modRole
import java.util.concurrent.TimeUnit.*
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
class ModeratorCommand: EmptyCommand(AdministratorGroup) {
    override val name = "Moderator"
    override val aliases = arrayOf("Mod")
    override val help = "Manage the server's moderators."
    override val children = arrayOf(
        ModeratorAddCommand(),
        ModeratorRemoveCommand(),
        ModeratorRoleCommand()
    )

    @MustHaveArguments("Specify a user to add as a moderator!")
    private inner class ModeratorAddCommand: Command(this@ModeratorCommand) {
        override val name = "Add"
        override val arguments = "[@User]"
        override val help = "Adds a moderator to the server."
        override val botPermissions = arrayOf(
            Permission.MANAGE_ROLES
        )

        override suspend fun execute(ctx: CommandContext) {
            val guild = ctx.guild
            val modRole = guild.modRole ?: return ctx.replyError {
                "This server has no moderator role!"
            }

            if(!ctx.selfMember.canInteract(modRole)) return ctx.replyError {
                "The `$fullname` command cannot be used because I cannot " +
                "interact with this server's moderator role!"
            }

            val matcher = userMention.matchEntire(ctx.args) ?: return ctx.invalidArgs()
            val targetId = matcher.groupValues[1].toLong()

            val target = guild.getMemberById(targetId) ?: return ctx.replyError {
                "Could not find a user with ID: $targetId"
            }

            // Already a moderator
            if(modRole in target.roles) return ctx.replyError {
                "${target.user.formattedName(true)} is already a moderator!"
            }

            // Cannot interact
            if(!ctx.selfMember.canInteract(target)) return ctx.replyError {
                "I cannot add ${target.user.formattedName(true)} because I cannot interact with them!"
            }

            // Have called confirm addition
            val code = randomInt(max = 99999).toString().padStart(5, '0')
            val confirmation = "${ctx.bot.prefix}Confirm $code"

            val prompt = ctx.send {
                "Found a user: **${target.user.name}** (ID: ${target.user.idLong})\n" +
                "To complete this command, please use `$confirmation`!"
            }

            launch(coroutineContext) {
                val succeeded = Laxus.Waiter.delayUntil<GuildMessageReceivedEvent>(1, MINUTES) succeeded@ {
                    if(it.guild == guild && it.author == ctx.author) {
                        val content = it.message.contentRaw.modifyIf({
                            it.startsWith('`') && it.endsWith('`')
                        }, { it.substring(1, it.length - 1) })
                        if(content.equals(confirmation, ignoreCase = true)) {
                            return@succeeded true
                        }
                    }
                    return@succeeded false
                }

                ignored { prompt.delete().queue() }
                if(!succeeded) return@launch ctx.replyWarning {
                    "Confirmation timed out! Try using `${ctx.bot.prefix}$fullname` again!"
                }

                try {
                    target.giveRole(modRole).await()
                } catch(e: ErrorResponseException) {
                    return@launch ctx.replyError(UnexpectedError)
                }

                ctx.replySuccess("Successfully added ${target.user.formattedName(true)} as a moderator!")
            }
        }
    }

    @MustHaveArguments("Specify a moderator to remove!")
    private inner class ModeratorRemoveCommand: Command(this@ModeratorCommand) {
        override val name = "Remove"
        override val arguments = "[@User]"
        override val help = "Removes a moderator from the server."
        override val botPermissions = arrayOf(
            Permission.MANAGE_ROLES
        )

        override suspend fun execute(ctx: CommandContext) {
            val guild = ctx.guild
            val modRole = guild.modRole ?: return ctx.replyError {
                "This server has no moderator role!"
            }

            if(!ctx.selfMember.canInteract(modRole)) return ctx.replyError {
                "The `$fullname` command cannot be used because I cannot " +
                "interact with this server's moderator role!"
            }

            val matcher = userMention.matchEntire(ctx.args) ?: return ctx.invalidArgs()
            val targetId = matcher.groupValues[1].toLong()

            val target = guild.getMemberById(targetId) ?: return ctx.replyError {
                "Could not find a user with ID: $targetId"
            }

            // Already a moderator
            if(modRole !in target.roles) return ctx.replyError {
                "${target.user.formattedName(true)} is not a moderator!"
            }

            // Cannot interact
            if(!ctx.selfMember.canInteract(target)) return ctx.replyError {
                "I cannot add ${target.user.formattedName(true)} because I cannot interact with them!"
            }

            target.removeRole(modRole).await()
            ctx.replySuccess("Successfully removed ${target.user.formattedName(true)} as a moderator!")
        }
    }

    @MustHaveArguments("Specify the role to use as this server's moderator role.")
    private inner class ModeratorRoleCommand: Command(this@ModeratorCommand) {
        override val name = "Role"
        override val arguments = "[Role]"
        override val help = "Sets the server's moderator role."

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val guild = ctx.guild
            val found = guild.findRoles(query)
            val target = when {
                found.isEmpty() -> return ctx.replyError(noMatch("roles", query))
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                else -> found[0]
            }

            val currentModRole = guild.modRole

            if(target == currentModRole) return ctx.replyError {
                "**${target.name}** is already this server's moderator role!"
            }

            // Setting a server's moderator role should be confirmed because
            //of a potential mismatch when looking up the role.
            // Because of this, we ask for the calling user to verify a 5
            //digit code just to be sure.

            val code = randomInt(max = 99999).toString().padStart(5, '0')
            val confirmation = "${ctx.bot.prefix}Confirm $code"

            val prompt = ctx.send {
                "Found a role: **${target.name}** (ID: ${target.idLong})\n" +
                "To complete this command, please use `$confirmation`!"
            }

            launch(coroutineContext) {
                val succeeded = Laxus.Waiter.delayUntil<GuildMessageReceivedEvent>(1, MINUTES) succeeded@ {
                    if(it.guild == guild && it.author == ctx.author) {
                        val content = it.message.contentRaw.modifyIf({
                            it.startsWith('`') && it.endsWith('`')
                        }, { it.substring(1, it.length - 1) })
                        if(content.equals(confirmation, ignoreCase = true)) {
                            return@succeeded true
                        }
                    }
                    return@succeeded false
                }

                ignored { prompt.delete().await() }
                if(!succeeded) return@launch ctx.replyWarning {
                    "Confirmation timed out! Try using `${ctx.bot.prefix}$fullname` again!"
                }

                guild.modRole = target

                // Make sure to provide a different success response if we can't
                //interact with the target role.
                if(!ctx.selfMember.canInteract(target)) return@launch ctx.replyWarning {
                    "Successfully made **${target.name}** this server's moderator role!\n" +
                    "Note that due to my position in the role hierarchy, I will not be able to " +
                    "give this role to members of the server."
                }

                ctx.replySuccess("Successfully made **${target.name}** this server's moderator role!")
            }
        }
    }
}