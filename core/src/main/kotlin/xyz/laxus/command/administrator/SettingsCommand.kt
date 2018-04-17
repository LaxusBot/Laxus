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
import xyz.laxus.util.db.isRolePersist

/**
 * @author Kaidan Gustave
 */
class SettingsCommand: EmptyCommand(AdministratorGroup) {
    override val name = "Settings"
    override val aliases = arrayOf("Configurations", "Config")
    override val help = "Manage various settings for the server."
    override val children = arrayOf<Command>(
        SettingsRolePersistCommand()
    )

    private inner class SettingsRolePersistCommand: Command(this@SettingsCommand) {
        override val name = "RolePersist"
        override val aliases = arrayOf("SaveRoles", "KeepRoles")
        override val arguments = "<ON|OFF>"
        override val help = "Configures the server's role persist."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args

            if(args.isEmpty()) {
                val state = if(ctx.guild.isRolePersist) "ON" else "OFF"

                return ctx.reply { "Currently this server has role persist toggled `$state`!" }
            }

            val requestedState = when(args.toUpperCase()) {
                "ON", "TRUE", "ACTIVE", "ENABLED" -> true
                "OFF", "FALSE", "DISABLED" -> false
                else -> return ctx.invalidArgs {
                    "\"$args\" is not a valid mode to set role persist to!"
                }
            }

            ctx.guild.isRolePersist = requestedState
            ctx.replySuccess("Successfully set role persist to `${args.toUpperCase()}`!")
        }
    }
}