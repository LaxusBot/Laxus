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
package xyz.laxus.command.owner

import xyz.laxus.command.*
import xyz.laxus.db.entities.ExperimentAccess
import xyz.laxus.jda.util.await
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.experimentAccessLevel
import xyz.laxus.util.ignored
import xyz.laxus.util.titleName

class ExperimentsCommand: EmptyCommand(OwnerGroup) {
    override val name = "Experiments"
    override val help = "Manages experiments."
    override val devOnly = true
    override val guildOnly = false
    override val hasAdjustableLevel = false
    override val children = arrayOf(ExperimentsAddCommand(), ExperimentsRemoveCommand())

    @MustHaveArguments("Specify a Type, ID, and an experimental level!")
    private inner class ExperimentsAddCommand: Command(this@ExperimentsCommand) {
        override val name = "Add"
        override val arguments = "[Type] [ID] [Level]"
        override val devOnly = true
        override val guildOnly = false
        override val hasAdjustableLevel = false

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 3)

            if(parts.size < 3) return ctx.replyError {
                "Invalid number of arguments, specify a type, ID, and an experimental level!"
            }

            val type = ignored(null) { ExperimentAccess.Type.valueOf(parts[0].toUpperCase()) } ?:
                       return ctx.replyError { "${parts[0]} is not a valid type!" }

            val id = parts[1].toLongOrNull() ?:
                     return ctx.replyError { "${parts[1]} is not a valid long!" }

            val level = ignored(null) {
                val levelString = parts[2].split(commandArgs).joinToString("_") { it.toUpperCase() }
                ExperimentAccess.Level.valueOf(levelString)
            } ?: return ctx.replyError {
                "${parts[2]} is not a valid experimental level!"
            }

            when(type) {
                ExperimentAccess.Type.USER -> {
                    val user = ignored(null) { ctx.jda.retrieveUserById(id).await() } ?:
                               return ctx.replyError { "Could not find user with ID $id" }
                    user.experimentAccessLevel = level
                }
                ExperimentAccess.Type.GUILD -> {
                    val guild = ctx.jda.getGuildById(id) ?:
                                return ctx.replyError { "Could not find guild with ID $id" }
                    guild.experimentAccessLevel = level
                }
            }

            ctx.replySuccess("Successfully added ${type.name.toLowerCase()} with ID $id as **${level.titleName}**")
        }
    }

    @MustHaveArguments("Specify a Type, and an ID!")
    private inner class ExperimentsRemoveCommand: Command(this@ExperimentsCommand) {
        override val name = "Remove"
        override val arguments = "[Type] [ID]"
        override val devOnly = true
        override val guildOnly = false
        override val hasAdjustableLevel = false

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)

            if(parts.size < 2) return ctx.replyError {
                "Invalid number of arguments, specify a type and an ID!"
            }

            val type = ignored(null) { ExperimentAccess.Type.valueOf(parts[0].toUpperCase()) } ?:
                       return ctx.replyError { "${parts[0]} is not a valid type!" }
            val id = parts[1].toLongOrNull() ?:
                     return ctx.replyError { "${parts[1]} is not a valid long!" }

            when(type) {
                ExperimentAccess.Type.USER -> {
                    val user = ignored(null) { ctx.jda.retrieveUserById(id).await() } ?:
                               return ctx.replyError { "Could not find user with ID $id" }
                    user.experimentAccessLevel = null
                }
                ExperimentAccess.Type.GUILD -> {
                    val guild = ctx.jda.getGuildById(id) ?:
                                return ctx.replyError { "Could not find guild with ID $id" }
                    guild.experimentAccessLevel = null
                }
            }

            ctx.replySuccess("Successfully removed ${type.name.toLowerCase()} with ID $id!")
        }
    }
}