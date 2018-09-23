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
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.command.*
import xyz.laxus.bot.menus.paginator
import xyz.laxus.bot.menus.paginatorBuilder
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.bot.utils.jda.findRoles
import xyz.laxus.bot.utils.db.colorMeRoles
import xyz.laxus.bot.utils.db.isColorMe
import xyz.laxus.bot.utils.multipleRoles
import xyz.laxus.bot.utils.noMatch
import java.awt.Color

@AutoCooldown(AutoCooldown.Mode.AFTER)
class ColorMeCommand: Command(StandardGroup) {
    private companion object {
        private val HexRegex = Regex("#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]")
    }

    override val name = "ColorMe"
    override val arguments = "[Color|HexCode]"
    override val help = "Changes the color of your highest ColorMe role."
    override val guildOnly = true
    override val cooldown = 5
    override val cooldownScope = CooldownScope.USER_GUILD
    override val botPermissions = arrayOf(Permission.MANAGE_ROLES)
    override val children = arrayOf(
        ColorMeAddCommand(),
        ColorMeListCommand(),
        ColorMeRemoveCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val colorMeRoles = ctx.guild.colorMeRoles

        // We have a different error if there is no ColorMe roles at all
        commandErrorIf(colorMeRoles.isEmpty()) { "There are no ColorMe roles on this server!" }

        val target = colorMeRoles.firstOrNull { it in ctx.member.roles }

        commandErrorIf(target == null) { "You do not have any ColorMe roles!" }

        val value = ctx.args
        val color = when {
            value matches HexRegex -> {
                val result = runCatching { Color.decode(value) }
                commandErrorIfNull(result.getOrNull()) { "$value is not a valid hex!" }
            }
            else -> when(value.toLowerCase()) {
                // Regular Colors
                "red"                      -> Color.RED
                "orange"                   -> Color.ORANGE
                "yellow"                   -> Color.YELLOW
                "green"                    -> Color.GREEN
                "cyan"                     -> Color.CYAN
                "blue"                     -> Color.BLUE
                "magenta"                  -> Color.MAGENTA
                "pink"                     -> Color.PINK
                "black"                    -> Color.decode("#000001")
                "purple"                   -> Color.decode("#800080")
                "dark gray", "dark grey"   -> Color.DARK_GRAY
                "gray", "grey"             -> Color.GRAY
                "light gray", "light grey" -> Color.LIGHT_GRAY
                "white"                    -> Color.WHITE

                // Discord Colors
                "blurple"                  -> Color.decode("#7289DA")
                "greyple"                  -> Color.decode("#99AAB5")
                "darktheme"                -> Color.decode("#2C2F33")
                else                       -> commandError("$value is not a valid color!")
            }
        }


        commandErrorIf(!ctx.selfMember.canInteract(target)) { "Cannot interact with your highest ColorMe role!" }

        target.manager.setColor(color).await()
        ctx.replySuccess("Successfully changed your color to $value")
    }

    @MustHaveArguments
    private inner class ColorMeAddCommand: Command(this@ColorMeCommand) {
        override val name = "Add"
        override val arguments = "[Role Name]"
        override val help = "Adds a ColorMe role to the server."
        override val guildOnly = true
        override val defaultLevel = CommandLevel.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val found = ctx.guild.findRoles(query)

            commandErrorIf(found.isEmpty()) { noMatch("roles", query) }
            commandErrorIf(found.size > 1) { found.multipleRoles(query) }

            val role = found[0]

            commandErrorIf(role.isColorMe) { "**${role.name}** is already a ColorMe role!" }

            role.isColorMe = true
            ctx.replySuccess("Successfully added **${role.name}** as a ColorMe role!")
        }
    }

    @MustHaveArguments
    private inner class ColorMeRemoveCommand: Command(this@ColorMeCommand) {
        override val name = "Remove"
        override val arguments = "[Role Name]"
        override val help = "Removes a ColorMe role from the server."
        override val guildOnly = true
        override val defaultLevel = CommandLevel.ADMINISTRATOR
        override val botPermissions = arrayOf(Permission.MANAGE_ROLES)

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val colorMeRoles = ctx.guild.colorMeRoles

            commandErrorIf(colorMeRoles.isEmpty()) { "There are no ColorMe roles on this server!" }

            val found = ctx.guild.findRoles(query).filter { it in colorMeRoles }

            commandErrorIf(found.isEmpty()) { noMatch("ColorMe roles", query) }
            commandErrorIf(found.size > 1) { found.multipleRoles(query) }

            val role = found[0]

            // We don't need to check if it's not a ColorMe role because we already
            // filtered out all the non-ColorMe roles

            role.isColorMe = false
            ctx.replySuccess("Successfully removed **${role.name}** from ColorMe roles!")
        }
    }

    @AutoCooldown(AutoCooldown.Mode.AFTER)
    private inner class ColorMeListCommand: Command(this@ColorMeCommand) {
        override val name = "List"
        override val help = "Lists all the ColorMe roles on the server."
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
            val colorMeRoles = ctx.guild.colorMeRoles
            commandErrorIf(colorMeRoles.isEmpty()) { "There are no ColorMe roles on this server!" }
            builder.clearItems()

            val paginator = paginator(builder) {
                text { _,_ -> "ColorMe Roles On ${ctx.guild.name}" }
                items { colorMeRoles.forEach { + it.name } }
                finalAction { ctx.linkMessage(it) }
                user { ctx.author }
            }

            paginator.displayIn(ctx.channel)
        }
    }
}
