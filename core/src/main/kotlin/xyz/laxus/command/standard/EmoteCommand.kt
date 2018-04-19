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
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.embed
import xyz.laxus.util.*

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify an custom emoji to get info on.")
class EmoteCommand : Command(StandardGroup) {
    private companion object {
        private const val bullet = "\uD83D\uDD39"
    }

    override val name = "Emote"
    override val aliases = arrayOf("Emoji")
    override val arguments = "[Emote|Emoji]"
    override val help = "Gets info on an emote or unicode character."
    override val guildOnly = false
    override val botPermissions = arrayOf(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val args = ctx.args

        if(args matches emoteRegex) {
            val emotes = ctx.message.emotes
            if(emotes.size < 1) return ctx.replyError {
                "The specified emote was fake, or could not be retrieved!"
            }

            val emote = emotes[0]
            ctx.reply(embed {
                title { "Info on ${if(ctx.jda.getEmoteById(emote.idLong) === null) ":${emote.name}:" else emote.asMention}" }

                if(ctx.isGuild) {
                    color { ctx.member.color }
                }

                + "$bullet **Name:** ${emote.name}\n"
                emote.guild?.let { + "$bullet **Guild:** ${it.name} (ID: ${it.id})\n" }
                + "$bullet **Creation Date:** ${emote.creationTime.readableFormat}\n"
                + "$bullet **ID:** ${emote.id}\n"
                + "$bullet **Managed:** ${if(emote.isManaged) Emojis.GreenTick else Emojis.RedTick}"

                image { emote.imageUrl }
                footer { value = "Created at" }
                time { emote.creationTime }
            })
        } else {
            val unicode = args.replace(" ", "")
            if(unicode.length > 10) {
                return ctx.replyError("Cannot process more than 10 characters!")
            }

            ctx.reply(embed {
                if(ctx.isGuild) {
                    color { ctx.selfMember.color }
                }
                title { "Unicode Information:" }
                unicode.codePoints().use {
                    it.forEach {
                        val chars = it.toChars()
                        var hex = it.toHexString().toUpperCase()

                        while(hex.length < 4) {
                            hex = "0$hex"
                        }

                        append("\n`\\u")
                        append(hex)
                        append("`   ")

                        if(chars.size > 1) {
                            var hex0 = chars[0].toInt().toHexString().toUpperCase()
                            var hex1 = chars[1].toInt().toHexString().toUpperCase()
                            while(hex0.length < 4) {
                                hex0 = "0$hex0"
                            }
                            while(hex1.length < 4) {
                                hex1 = "0$hex1"
                            }
                            append("[`\\u")
                            append(hex0)
                            append("\\u")
                            append(hex1)
                            append("`]   ")
                        }

                        append(String(chars))
                        append("   _")
                        append(it.name)
                        append("_")
                    }
                }
            })
        }
    }
}
