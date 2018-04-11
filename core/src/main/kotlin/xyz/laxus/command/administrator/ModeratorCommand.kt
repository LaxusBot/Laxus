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

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.jda.util.findRoles
import xyz.laxus.util.db.modRole
import xyz.laxus.util.multipleRoles
import xyz.laxus.util.noMatch

/**
 * @author Kaidan Gustave
 */
class ModeratorCommand : EmptyCommand(AdministratorGroup) {
    override val name = "Moderator"
    override val help = "Manage the server's moderators"
    override val children = arrayOf(
        ModeratorAddCommand(),
        ModeratorRoleCommand()
    )

    private inner class ModeratorAddCommand : Command(this@ModeratorCommand) {
        override val name = "Add"

        override suspend fun execute(ctx: CommandContext) {

            TODO("not implemented")
        }
    }

    private inner class ModeratorRoleCommand : Command(this@ModeratorCommand) {
        override val name = "Role"
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

            if(!ctx.selfMember.canInteract(target)) return ctx.replyError {
                "**${target.name}** cannot"
            }

            guild.modRole = target
        }
    }
}