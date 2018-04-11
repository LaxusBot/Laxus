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
@file:Suppress("unused")
package xyz.laxus.entities

import com.jagrosh.jagtag.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import xyz.laxus.util.*
import xyz.laxus.jda.util.findMembers
import xyz.laxus.jda.util.findTextChannels
import xyz.laxus.jda.util.findUsers
import java.awt.Color
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlin.streams.toList

internal val tagMethods: Collection<Method> by lazy {
    arrayListOf(
        method("user", { get<User>("user").name }, { input ->
            if(input[0].isEmpty()) {
                throw ParseException("Invalid 'user' statement")
            }
            return@method userSearch(this, input).name
        }),

        method("nick", function = {
            val user = get<User>("user")
            if("guild" !in this) {
                user.name
            } else {
                return@method get<Guild>("guild").getMember(user)?.nickname ?: user.name
            }
        }, biFunction = { input ->
            if(input[0].isEmpty())
                throw ParseException("Invalid 'nick' statement")
            val user = userSearch(this, input)
            if("guild" !in this) {
                user.name
            } else {
                get<Guild>("guild").getMember(user)?.nickname ?: user.name
            }
        }),

        Method("discrim", ParseFunction { env ->
            env.get<User>("user").discriminator
        }, ParseBiFunction { env, input ->
            if(input[0].isEmpty())
                throw ParseException("Invalid 'user' statement")
            userSearch(env, input).discriminator
        }),

        Method("@user", ParseFunction { env ->
            if(env.contains("guild"))
                env.get<Guild>("guild").getMember(env.get<User>("user"))!!.asMention
            else
                env.get<User>("user").asMention
        }, ParseBiFunction { env, input ->
            if(input[0].isEmpty())
                throw ParseException("Invalid '@user' statement")
            if(env.contains("guild"))
                env.get<Guild>("guild").getMember(userSearch(env, input))!!.asMention
            else
                userSearch(env, input).asMention
        }),

        Method("userid", ParseFunction { env ->
            env.get<User>("user").id
        }, ParseBiFunction { env, input ->
            if(input[0].isEmpty())
                throw ParseException("Invalid 'userid' statement")
            userSearch(env, input).id
        }),

        Method("avatar", ParseFunction { env ->
            env.get<User>("user").avatarUrl
        }, ParseBiFunction { env, input ->
            if(input[0].isEmpty())
                throw ParseException("Invalid 'avatar' statement")
            userSearch(env, input).avatarUrl
        }),

        Method("server", { env ->
            if(!env.contains("guild"))
                throw TagErrorException("Tag is only available in a guild!")
            env.get<Guild>("guild").name
        }),

        Method("serverid", { env ->
            if(!env.contains("guild"))
                throw TagErrorException("Tag is only available in a guild!")
            env.get<Guild>("guild").id
        }),

        Method("servercount", { env ->
            if(!env.contains("guild")) "1" else env.get<Guild>("guild").members.size.toString()
        }),

        Method("channel", ParseFunction { env ->
            if(!env.contains("guild")) "DM"
            else env.get<TextChannel>("channel").name
        }, ParseBiFunction { env, input ->
            channelSearch(env, input)?.name?:"DM"
        }),

        Method("channelid", ParseFunction { env ->
            if(!env.contains("guild")) "0"
            else env.get<TextChannel>("channel").id
        }, ParseBiFunction { env, input ->
            channelSearch(env, input)?.id?:"0"
        }),

        Method("#channel", ParseFunction { env ->
            if(!env.contains("guild")) "DM"
            else env.get<TextChannel>("channel").asMention
        }, ParseBiFunction { env, input ->
            channelSearch(env, input)?.asMention?:"DM"
        }),

        Method("randuser", ParseFunction { env ->
            if(!env.contains("guild"))
                env.get<User>("user").name
            val guild = env.get<Guild>("guild")
            guild.members[(guild.members.size*Math.random()).toInt()].user.name
        }),

        Method("randonline", ParseFunction { env ->
            if(!env.contains("guild"))
                env.get<User>("user").name
            val guild = env.get<Guild>("guild")
            val online = guild.members.stream().filter { it.onlineStatus == OnlineStatus.ONLINE }.toList()
            online[(online.size*Math.random()).toInt()].user.name
        }),

        Method("randchannel", ParseFunction { env ->
            if(!env.contains("guild")) "DM"
            else {
                val guild = env.get<Guild>("guild")
                guild.textChannels[(guild.textChannels.size * Math.random()).toInt()].name
            }
        })
    )
}

internal val embedMethods: Collection<Method> by lazy {
    listOf(
        Method("title", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid title statement!")
            val parts = input[0].split(Regex("\\|"),limit = 2)
            env.get<EmbedBuilder>("builder").setTitle(parts[0], if(parts.size>1) parts[1] else null); ""
        }, false),

        Method("author", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid author statement!")
            val parts = input[0].split(Regex("\\|"),limit = 3)
            env.get<EmbedBuilder>("builder").setAuthor(
                parts[0],
                if(parts.size>1) parts[1] else null,
                if(parts.size>2) parts[2] else null
            ); ""
        }, false),

        Method("thumbnail", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid thumbnail statement!")
            env.get<EmbedBuilder>("builder").setThumbnail(input[0]); ""
        }, false),

        Method("description", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid description statement!")
            env.get<EmbedBuilder>("builder").setDescription(input[0]); ""
        }, false),

        Method("field", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid field statement!")
            val parts = input[0].split(Regex("\\|"),limit = 3)
            if(parts.size<2)
                throw TagErrorException("Invalid field statement!")
            env.get<EmbedBuilder>("builder").addField(
                parts[0],
                parts[1],
                if(parts.size>2) parts[2].equals("true",true) else true
            ); ""
        }, false),

        Method("image", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid image statement!")
            env.get<EmbedBuilder>("builder").setImage(input[0]); ""
        }, false),

        Method("color", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid color statement!")
            val b = env.get<EmbedBuilder>("builder")
            when(input[0].toLowerCase()){
                "red"        -> b.setColor(Color.RED)
                "orange"     -> b.setColor(Color.ORANGE)
                "yellow"     -> b.setColor(Color.YELLOW)
                "green"      -> b.setColor(Color.GREEN)
                "cyan"       -> b.setColor(Color.CYAN)
                "blue"       -> b.setColor(Color.BLUE)
                "magenta"    -> b.setColor(Color.MAGENTA)
                "pink"       -> b.setColor(Color.PINK)
                "black"      -> b.setColor(Color.decode("#000001"))
                "dark_gray",
                "dark_grey"  -> b.setColor(Color.DARK_GRAY)
                "light_gray",
                "light_grey" -> b.setColor(Color.LIGHT_GRAY)
                "white"      -> b.setColor(Color.WHITE)

                "blurple"    -> b.setColor(Color.decode("#7289DA"))
                "greyple"    -> b.setColor(Color.decode("#99AAB5"))
                "darktheme"  -> b.setColor(Color.decode("#2C2F33"))
                else -> {
                    try {
                        b.setColor(Color.decode(input[0]))
                    } catch (e: NumberFormatException) {
                        throw TagErrorException("Invalid color statement!")
                    }
                }
            }; ""
        }, false),

        Method("footer", {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid footer statement!")
            val parts = input[0].split(Regex("\\|"),limit = 2)
            env.get<EmbedBuilder>("builder").setFooter(parts[0], if(parts.size>1) parts[1] else null); ""
        }, false),

        Method("timestamp", {env ->
            env.get<EmbedBuilder>("builder").setTimestamp(OffsetDateTime.now()); ""
        }, {env, input ->
            if(input[0].isEmpty())
                throw TagErrorException("Invalid timestamp statement!")
            OffsetDateTime.parse(input[0])
            try {
                env.get<EmbedBuilder>("builder").setTimestamp(OffsetDateTime.parse(input[0]))
            } catch (e: DateTimeParseException) {
                throw TagErrorException("Invalid timestamp statement!")
            }; ""
        }, false)
    )
}

private fun userSearch(env: Environment, input: Array<out String>): User {
    if(env.contains("guild")) { // is from guild
        with(env.get<Guild>("guild").findMembers(input[0])) {
            if(this.isEmpty())
                throw TagErrorException(noMatch("members", input[0]))
            if(this.size>1)
                throw TagErrorException(this.multipleMembers(input[0]))
            return this[0].user
        }
    } else {
        with(env.get<User>("user").jda.findUsers(input[0])) {
            if(this.isEmpty())
                throw TagErrorException(noMatch("users", input[0]))
            if(this.size>1)
                throw TagErrorException(this.multipleUsers(input[0]))
            return this[0]
        }
    }
}

private fun channelSearch(env: Environment, input: Array<out String>): TextChannel? {
    if(!env.contains("guild"))
        return null
    if(input[0].isEmpty())
        throw ParseException("Invalid 'channel' statement")
    with(env.get<Guild>("guild").findTextChannels(input[0]))
    {
        if(this.isEmpty())
            throw TagErrorException(noMatch("channels", input[0]))
        if(this.size>1)
            throw TagErrorException(this.multipleTextChannels(input[0]))
        return this[0]
    }
}

inline fun method(
    name: String,
    crossinline function: Environment.() -> String
) = Method(name, { env -> env.function() })

inline fun method(
    name: String,
    crossinline biFunction: Environment.(Array<out String>) -> String,
    split: Boolean = true
) = Method(name, { env, args -> env.biFunction(args) }, split)

inline fun method(
    name: String,
    crossinline function: Environment.() -> String,
    crossinline biFunction: Environment.(Array<out String>) -> String,
    split: Boolean = true
) = Method(name,
    ParseFunction { env -> env.function() },
    ParseBiFunction { env, args -> env.biFunction(args) },
    split
)

class TagErrorException(message: String): RuntimeException(message)
