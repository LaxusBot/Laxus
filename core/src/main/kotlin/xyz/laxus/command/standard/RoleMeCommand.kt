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
import xyz.laxus.Laxus
import xyz.laxus.command.AutoCooldown
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.findRoles
import xyz.laxus.jda.util.giveRole
import xyz.laxus.jda.util.removeRole
import xyz.laxus.util.db.isRoleMe
import xyz.laxus.util.db.roleMeLimit
import xyz.laxus.util.db.roleMeRoles
import xyz.laxus.util.multipleRoles
import xyz.laxus.util.noMatch

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify the name of a role to assign yourself.")
class RoleMeCommand : Command(StandardGroup) {
    override val name = "RoleMe"
    override val arguments = "[Role Name]"
    override val help = "Assigns you a RoleMe role by name."
    override val guildOnly = true
    override val cooldown = 5
    override val cooldownScope = CooldownScope.USER_GUILD
    override val botPermissions = arrayOf(Permission.MANAGE_ROLES)
    override val children = arrayOf(
        RoleMeAddCommand(),
        RoleMeLimitCommand(),
        RoleMeListCommand(),
        RoleMeRemoveCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        // Get all the guild's RoleMe roles
        val roleMeRoles = ctx.guild.roleMeRoles

        // Do we have any RoleMe roles to begin with?
        if(roleMeRoles.isEmpty()) {
            // We have a different error if there is no RoleMe roles at all
            return ctx.replyError("There are no RoleMe roles on this server!")
        }

        // Get the query for roles
        val query = ctx.args
        // Search for roles matching the query
        val found = ctx.guild.findRoles(query)

        // Filter the roles found so that only RoleMe roles remain
        val roleMeRolesFound = found.filter { it in roleMeRoles } // From found, only if it is in roleMeRoles

        // When...
        val roleMeRole = when {
            // The roleMeRolesFound is empty, none match
            roleMeRolesFound.isEmpty() -> return ctx.replyError(noMatch("RoleMe roles", query))
            // The roleMeRolesFound contains multiple roles, too many match
            roleMeRolesFound.size > 1 -> return ctx.replyError(roleMeRolesFound.multipleRoles(query))
            // The roleMeRole we are looking for is the first and only value
            else -> roleMeRolesFound[0]
        }

        // The calling member
        val member = ctx.member

        // If the caller doesn't have the role we have to add it
        if(roleMeRole !in member.roles) {
            // Check for a limit
            /* TODO Limit
            val roleMeLimit = ctx.guild.getCommandLimit(this) ?: 0

            if(roleMeLimit > 0) {
                // The user is at the RoleMe limit
                if(roleMeLimit == roleMeRoles.count { it in member.roles }) {
                    return ctx.replyError("You are at the RoleMe role limit for this server.")
                }
            }
            */

            // Give the caller their requested role, suspend until that is done
            member.giveRole(roleMeRole).await()
            // Respond
            ctx.replySuccess("Successfully gave the **${roleMeRole.name}** role!")
        } else {
            // The caller has the role, we need to remove it
            member.removeRole(roleMeRole).await()
            // Respond
            ctx.replySuccess("Successfully removed the **${roleMeRole.name}** role!")
        }

        // Cooldown
        ctx.invokeCooldown()
    }

    @MustHaveArguments
    private inner class RoleMeAddCommand : Command(this@RoleMeCommand) {
        override val name = "Add"
        override val arguments = "[Role Name]"
        override val help = "Adds a RoleMe role to the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val found = ctx.guild.findRoles(query)

            val role = when {
                found.isEmpty() -> return ctx.replyError(noMatch("roles", query))
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                else -> found[0]
            }

            if(role.isRoleMe) {
                return ctx.replyError("**${role.name}** is already a RoleMe role!")
            }

            role.isRoleMe = true
            ctx.replySuccess("Successfully added **${role.name}** as a RoleMe role!")
        }
    }

    @MustHaveArguments
    private inner class RoleMeRemoveCommand : Command(this@RoleMeCommand) {
        override val name = "Remove"
        override val arguments = "[Role Name]"
        override val help = "Removes a RoleMe role from the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            // Get all the guild's RoleMe roles
            val roleMeRoles = ctx.guild.roleMeRoles

            // Do we have any RoleMe roles to begin with?
            if(roleMeRoles.isEmpty()) {
                return ctx.replyError("There are no RoleMe roles on this server!")
            }

            // Get the query for roles
            val query = ctx.args

            // Search for roles matching the query, then filter
            //so that only RoleMe roles remain.
            val found = ctx.guild.findRoles(query).filter { it in roleMeRoles }

            // When...
            val role = when {
                // The found roles is empty, none match
                found.isEmpty() -> return ctx.replyError(noMatch("RoleMe roles", query))
                // The found roles contains multiple roles, too many match
                found.size > 1 -> return ctx.replyError(found.multipleRoles(query))
                // The found role we are looking for is the first and only value
                else -> found[0]
            }

            // We don't need to check if it's not a RoleMe role because we already
            //filtered out all the non-RoleMe roles

            // Set isRoleMe to false
            role.isRoleMe = false

            // Respond
            ctx.replySuccess("Successfully removed **${role.name}** from RoleMe roles!")
        }
    }

    private inner class RoleMeLimitCommand : Command(this@RoleMeCommand) {
        override val name = "Limit"
        override val arguments = "<Number>"
        override val help = "Sets the limit for RoleMe roles on the server."
        override val guildOnly = true
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val currentLimit = ctx.guild.roleMeLimit

            if(args.isEmpty()) {
                return if(currentLimit === null) {
                    ctx.replySuccess("This server does not have a RoleMe limit.")
                } else {
                    ctx.replySuccess("The RoleMe limit for this server is $currentLimit RoleMe roles.")
                }
            }

            val limit = try {
                args.toShort()
            } catch(e: NumberFormatException) {
                return ctx.replyError("\"$args\" is not a valid number!")
            }

            ctx.guild.roleMeLimit = limit.takeIf { it > 0 }
            if(limit > 0) {
                ctx.replySuccess("Removed the RoleMe limit for this server.")
            } else {
                ctx.replySuccess("Successfully set the RoleMe limit for this server to $limit RoleMe roles.")
            }
        }
    }

    @AutoCooldown
    private inner class RoleMeListCommand : Command(this@RoleMeCommand) {
        override val name = "List"
        override val help = "Lists all the RoleMe roles on the server."
        override val guildOnly = true
        override val cooldown = 5
        override val cooldownScope = CooldownScope.USER_GUILD
        override val botPermissions = arrayOf(
            Permission.MANAGE_ROLES,
            Permission.MESSAGE_MANAGE,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ADD_REACTION
        )

        private val builder = paginatorBuilder {
            waiter           { Laxus.Waiter }
            timeout          { delay { 20 } }
            showPageNumbers  { true }
            numberItems      { true }
            waitOnSinglePage { false }
        }

        override suspend fun execute(ctx: CommandContext) {
            // Get all the guild's RoleMe roles
            val roleMeRoles = ctx.guild.roleMeRoles

            // Do we have any RoleMe roles to begin with?
            if(roleMeRoles.isEmpty()) {
                return ctx.replyError("There are no RoleMe roles on this server!")
            }

            // Clear the builder
            builder.clearItems()

            // Generate paginator
            val paginator = paginator(builder) {
                text { _,_ -> "RoleMe Roles On ${ctx.guild.name}" }
                items {
                    roleMeRoles.forEach { + it.name }
                }
                finalAction { ctx.linkMessage(it) }
                user { ctx.author }
            }

            // Display
            paginator.displayIn(ctx.channel)

            // Cooldown
            ctx.invokeCooldown()
        }
    }
}
