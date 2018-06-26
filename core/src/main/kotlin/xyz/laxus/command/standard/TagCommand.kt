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

import net.dv8tion.jda.core.Permission.*
import xyz.laxus.Laxus
import xyz.laxus.command.AutoCooldown
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.entities.Tag
import xyz.laxus.entities.TagErrorException
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.findMembers
import xyz.laxus.jda.util.findUsers
import xyz.laxus.util.*
import xyz.laxus.util.db.*

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a tag name, and optionally tag arguments.")
class TagCommand: Command(StandardGroup) {
    override val name = "Tag"
    override val aliases = arrayOf("T")
    override val arguments = "[Tag Name] <Tag Arguments>"
    override val help = "Calls a tag."
    override val guildOnly = false
    override val children = arrayOf(
        TagCreateCommand(),
        TagCreateGlobalCommand(),
        TagDeleteCommand(),
        TagEditCommand(),
        TagListCommand(),
        TagOverrideCommand(),
        TagRemoveCommand(),
        TagOwnerCommand(),
        TagSearchCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val parts = ctx.args.split(commandArgs, 2)

        val name = parts[0]
        if(ctx.bot.searchCommand(name) !== null) {
            return ctx.reply("You remember Shengaero's words: *\"Not everything is a tag!\"*")
        }

        val args = if(parts.size > 1) parts[1].trim() else ""
        val parser = ctx.bot.parser

        // Clear parser
        parser.clear()

        // Load default parser data
        parser.put("args", args)
        parser.put("user", ctx.author)

        val tag = if(ctx.isGuild) {
            // Load guild specific parser data
            parser.put("guild", ctx.guild)
            parser.put("channel", ctx.channel)

            // Get guild scope tag
            ctx.guild.getTagByName(name) ?: ctx.jda.getTagByName(name)
        } else ctx.jda.getTagByName(name) // Get global scope tag

        if(tag === null) return ctx.replyError("The tag \"$name\" does not exist or could not be found.")
        if(tag.isOverride()) return ctx.replyWarning ("This tag has been overriden!")

        val output = try {
            parser.parse(tag.content)
        } catch(e: TagErrorException) {
            return ctx.replyError(e.message ?: "Tag \"$name\" could not be processed for an unknown reason!")
        }

        ctx.reply(output)
    }

    @MustHaveArguments
    private inner class TagCreateCommand: Command(this@TagCommand) {
        override val name = "Create"
        override val arguments = "[Tag Name] [Tag Content]"
        override val help = "Creates a new local tag."
        override val guildOnly = true
        override val cooldown = 120
        override val cooldownScope = CooldownScope.USER_GUILD

        override suspend fun execute(ctx: CommandContext) {
            val args = (ctx.args + ctx.message.attachments.joinToString("\n", "\n", "") { it.url }).trim()
            val parts = args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify a new tag name and it's content in the format `$arguments`."
            }

            if(name.length > Tag.MaxNameLength) return ctx.replyError {
                "Tag names must be no greater than ${Tag.MaxNameLength} characters long."
            }

            if(content.length > Tag.MaxContentLength) return ctx.replyError {
                "Tag content must be no greater than ${Tag.MaxContentLength} characters long."
            }

            if(ctx.jda.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A global tag named $name already exists!")
            }

            if(ctx.guild.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A local tag named $name already exists on this guild!")
            }

            ctx.guild.createTag(name, content, ctx.member)

            ctx.replySuccess("Successfully created local tag \"$name\"!")

            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments
    private inner class TagCreateGlobalCommand: Command(this@TagCommand) {
        override val name = "CreateGlobal"
        override val arguments = "[Tag Name] [Tag Content]"
        override val help = "Creates a new global tag."
        override val guildOnly = false
        override val cooldown = 120
        override val cooldownScope = CooldownScope.USER

        override suspend fun execute(ctx: CommandContext) {
            val args = (ctx.args + ctx.message.attachments.joinToString("\n", "\n", "") { it.url }).trim()
            val parts = args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify a new tag name and it's content in the format `$arguments`."
            }

            if(name.length > Tag.MaxNameLength) return ctx.replyError {
                "Tag names must be no greater than ${Tag.MaxNameLength} characters long."
            }

            if(content.length > Tag.MaxContentLength) return ctx.replyError {
                "Tag content must be no greater than ${Tag.MaxContentLength} characters long."
            }

            if(ctx.jda.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A global tag named $name already exists!")
            }

            if(ctx.isGuild && ctx.guild.isTag(name)) {
                // The tag already exists
                return ctx.replyError("A local tag named $name already exists on this guild!")
            }

            ctx.jda.createTag(name, content, ctx.author)

            ctx.replySuccess("Successfully created global tag \"$name\"!")

            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments
    private inner class TagDeleteCommand: Command(this@TagCommand) {
        override val name = "Delete"
        override val arguments = "[Tag Name]"
        override val help = "Deletes a tag you own."
        override val guildOnly = false

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            val tag = if(ctx.isGuild) {
                ctx.guild.getTagByName(name) ?: ctx.jda.getTagByName(name)
            } else ctx.jda.getTagByName(name)

            if(tag === null) {
                return ctx.replyError("Unable to find tag named \"$name\".")
            }

            // Future changes might require that this name be directly
            // taken from the database. As such, we save an instance before
            // deleting it to make sure this isn't affected later on.
            val tagName = tag.name

            if(tag.ownerId != ctx.author.idLong) return ctx.replyError {
                "Cannot delete tag \"$tagName\" because you are not the owner of the tag!"
            }

            tag.delete()
            ctx.replySuccess("Successfully deleted tag \"$tagName\"")
        }
    }

    @MustHaveArguments
    private inner class TagEditCommand: Command(this@TagCommand) {
        override val name = "Edit"
        override val arguments = "[Tag Name] [Tag Content]"
        override val help = "Creates a new local tag."
        override val guildOnly = true
        override val cooldown = 30
        override val cooldownScope = CooldownScope.USER

        override suspend fun execute(ctx: CommandContext) {
            val args = (ctx.args + ctx.message.attachments.joinToString("\n", "\n", "") { it.url }).trim()
            val parts = args.split(commandArgs, 2)
            val name = parts[0]
            val content = if(parts.size > 1) parts[1] else return ctx.missingArgs {
                "You must specify an existing tag name and it's new content in the format `$arguments`."
            }

            val tag = if(ctx.isGuild) {
                ctx.guild.getTagByName(name) ?: ctx.jda.getTagByName(name)
            } else ctx.jda.getTagByName(name)

            if(tag === null) {
                return ctx.replyError("Unable to find tag named \"$name\".")
            }

            if(content.length > Tag.MaxContentLength) return ctx.replyError {
                "Tag content must be no greater than ${Tag.MaxContentLength} characters long."
            }

            val tagName = tag.name

            if(tag.ownerId != ctx.author.idLong) return ctx.replyError {
                "Cannot edit tag \"$tagName\" because you are not the owner of the tag!"
            }

            tag.edit(content)
            ctx.replySuccess("Successfully edited tag \"$tagName\"")
            ctx.invokeCooldown()
        }
    }

    @AutoCooldown
    private inner class TagListCommand: Command(this@TagCommand) {
        override val name = "List"
        override val arguments = "<User>"
        override val help = "Gets a list of tags owned by a user."
        override val guildOnly = false
        override val cooldown = 10
        override val botPermissions = arrayOf(
            MESSAGE_EMBED_LINKS,
            MESSAGE_MANAGE,
            MESSAGE_ADD_REACTION
        )

        private val builder = paginatorBuilder {
            waiter { Laxus.Waiter }
            timeout { delay { 20 } }
            showPageNumbers { true }
            numberItems { true }
            waitOnSinglePage { true }
        }

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val temp = if(ctx.isGuild) {
                if(query.isEmpty()) {
                    ctx.member
                } else {
                    val found = ctx.guild.findMembers(query)
                    when {
                        found.isEmpty() -> null
                        found.size > 1 -> return ctx.replyError(found.multipleMembers(query))
                        else -> found[0]
                    }
                }
            } else null

            val user = when {
                temp !== null -> temp.user
                query.isEmpty() -> ctx.author
                else -> {
                    val found = ctx.jda.findUsers(query)
                    when {
                        found.isEmpty() -> return ctx.replyError(noMatch("users", query))
                        found.size > 1 -> return ctx.replyError(found.multipleUsers(query))
                        else -> found[0]
                    }
                }
            }

            val member = if(temp === null && ctx.isGuild) ctx.guild.getMember(user) else temp

            val localTags = member?.let { member.tags }?.map { "${it.name} (Local)" }
            val globalTags = user.tags.map { "${it.name} (Global)" }

            if((localTags === null || localTags.isEmpty()) && globalTags.isEmpty()) {
                return ctx.replyError {
                    "${if(ctx.author == user) "You do" else "${user.formattedName(false)} does"} not have any tags!"
                }
            }

            builder.clearItems()

            val paginator = paginator(builder) {
                text { _, _ -> "Tags owned by ${user.formattedName(true)}" }
                items {
                    + (localTags ?: emptyList())
                    + globalTags
                }
                finalAction { message ->
                    ctx.linkMessage(message)
                    message.guild?.let {
                        if(it.selfMember.hasPermission(message.textChannel, MESSAGE_MANAGE)) {
                            message.clearReactions().queue()
                        }
                    }
                }
                user { ctx.author }
            }

            paginator.displayIn(ctx.channel)
        }
    }

    @MustHaveArguments
    private inner class TagOverrideCommand: Command(this@TagCommand) {
        override val name = "Override"
        override val arguments = "[Tag Name] <Override Content>"
        override val help = "Overrides a tag for the server by name."
        override val guildOnly = true
        override val defaultLevel = Level.MODERATOR

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)
            val name = parts[0]
            val content = parts.getOrNull(1) ?: "${Laxus.Warning} This tag has been overriden!"
            val tag = ctx.guild.getTagByName(name) ?: ctx.jda.getTagByName(name)
            if(tag === null) return ctx.replyError {
                "Unable to find tag named \"$name\"."
            }
            if(content.length > Tag.MaxContentLength) return ctx.replyError {
                "Tag content must be no greater than ${Tag.MaxContentLength} characters long."
            }
            tag.override(ctx.guild.idLong)
            ctx.replySuccess("Successfully override tag \"${tag.name}\"")
        }
    }

    @MustHaveArguments
    private inner class TagOwnerCommand: Command(this@TagCommand) {
        override val name = "Owner"
        override val arguments = "[Tag Name]"
        override val help = "Gets the owner of a tag by name."
        override val cooldown = 10

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val tag = if(ctx.isGuild) {
                ctx.guild.getTagByName(query) ?: ctx.jda.getTagByName(query)
            } else ctx.jda.getTagByName(query)

            if(tag === null) {
                return ctx.replyError("Unable to find tag matching \"$query\".")
            }

            val ownerId = tag.ownerId

            // This is due to an override either by the server or by me
            if(ownerId === null) {
                val message = "The ${if(tag.isGlobal()) "global" else "local"} tag \"${tag.name}\" has no owner."
                return ctx.replySuccess(message)
            }

            val owner = ignored(null) { ctx.jda.retrieveUserById(ownerId).await() }

            if(owner === null) {
                return ctx.replyError("Unable to retrieve the owner of tag \"${tag.name}\"!")
            }

            val message = "The ${if(tag.isGlobal()) "global" else "local"} tag \"${tag.name}\" " +
                          "is owned by ${owner.formattedName(true)}."

            ctx.replySuccess(message)
            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments
    private inner class TagRemoveCommand: Command(this@TagCommand) {
        override val name = "Remove"
        override val arguments = "[Tag Name] <Override Content>"
        override val help = "Removes a tag globally by name."
        override val guildOnly = false
        override val defaultLevel = Level.SHENGAERO

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)
            val name = parts[0]
            val content = parts.getOrNull(1) ?: "${Laxus.Warning} This tag has been overriden!"
            val tag = ctx.jda.getTagByName(name)
            if(tag === null) return ctx.replyError {
                "Unable to find tag named \"$name\"."
            }
            if(content.length > Tag.MaxContentLength) return ctx.replyError {
                "Tag content must be no greater than ${Tag.MaxContentLength} characters long."
            }
            tag.delete()
            ctx.replySuccess("Successfully override tag \"${tag.name}\"")
        }
    }

    @MustHaveArguments
    private inner class TagSearchCommand: Command(this@TagCommand) {
        override val name = "Search"
        override val arguments = "[Query]"
        override val help = "Searches for tags by name."
        override val guildOnly = false
        override val cooldown = 30
        override val cooldownScope = CooldownScope.USER
        override val botPermissions = arrayOf(
            MESSAGE_EMBED_LINKS,
            MESSAGE_MANAGE,
            MESSAGE_ADD_REACTION
        )

        private val builder = paginatorBuilder {
            waiter { Laxus.Waiter }
            timeout { delay { 20 } }
            showPageNumbers { true }
            numberItems { true }
            waitOnSinglePage { true }
            itemsPerPage { 10 }
            allowTextInput { true }
        }

        override suspend fun execute(ctx: CommandContext) {
            val query = ctx.args
            val tags = if(ctx.isGuild) ctx.guild.findTags(query) + ctx.jda.findTags(query) else ctx.jda.findTags(query)

            if(tags.isEmpty()) return ctx.replyWarning {
                "No tags found matching \"$query\"!"
            }

            val tagLabels = tags.map {
                val owner = run {
                    val ownerId = it.ownerId
                    if(ownerId === null) return@run "Override"
                    val user = ctx.takeIf { it.isGuild }?.guild?.getMemberById(ownerId)?.user ?:
                               ctx.jda.getUserById(ownerId) ?:
                               ignored(null) { ctx.jda.retrieveUserById(ownerId).await() }
                    return@run user?.toString() ?: "ID: $ownerId"
                }
                return@map "${it.name} ($owner)"
            }

            val paginator = paginator(builder) {
                text { _, _ -> "Tags matching **$query**" }
                items { + tagLabels }
                finalAction { message ->
                    ctx.linkMessage(message)
                    message.guild?.let {
                        if(it.selfMember.hasPermission(message.textChannel, MESSAGE_MANAGE)) {
                            message.clearReactions().queue()
                        }
                    }
                }
                user { ctx.author }
            }

            paginator.displayIn(ctx.channel)
            ctx.invokeCooldown()
        }
    }
}
