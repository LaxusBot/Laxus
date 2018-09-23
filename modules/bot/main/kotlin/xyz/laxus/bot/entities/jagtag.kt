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
package xyz.laxus.bot.entities

import com.jagrosh.jagtag.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus.ONLINE
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.utils.jda.findMembers
import xyz.laxus.bot.utils.jda.findTextChannels
import xyz.laxus.bot.utils.jda.findUsers
import xyz.laxus.bot.utils.multipleMembers
import xyz.laxus.bot.utils.multipleTextChannels
import xyz.laxus.bot.utils.multipleUsers
import xyz.laxus.bot.utils.noMatch
import java.awt.Color
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlin.random.Random

internal val tagMethods: Collection<Method> by lazy {
    listOf(
        method("user", function = { get<User>("user").name }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid 'user' statement")
            userSearch(this, input).name
        }),

        method("nick", function = {
            val user = get<User>("user")
            get<Guild>("guild")?.getMember(user)?.nickname ?: user.name
        }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid 'nick' statement")
            val user = userSearch(this, input)
            get<Guild>("guild")?.getMember(user)?.nickname ?: user.name
        }),

        method("discrim", function = { get<User>("user").discriminator }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid 'user' statement")
            userSearch(this, input).discriminator
        }),

        method("@user", function = {
            val user = get<User>("user")
            get<Guild>("guild")?.getMember(user)?.asMention ?: user.asMention
        }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid '@user' statement")
            val user = userSearch(this, input)
            get<Guild>("guild")?.getMember(user)?.asMention ?: user.asMention
        }),

        method("userid", function = { get<User>("user").id }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid 'userid' statement")
            userSearch(this, input).id
        }),

        method("avatar", function = { get<User>("user").avatarUrl }, biFunction = { input ->
            if(input[0].isEmpty()) throw ParseException("Invalid 'avatar' statement")
            userSearch(this, input).avatarUrl
        }),

        method("server", function = {
            if("guild" !in this) throw TagErrorException("Tag is only available in a guild!")
            get<Guild>("guild").name
        }),

        method("serverid", function = {
            if("guild" !in this) throw TagErrorException("Tag is only available in a guild!")
            get<Guild>("guild").id
        }),

        method("servercount", function = {
            if("guild" !in this) "1" else get<Guild>("guild").memberCache.size().toString()
        }),

        method("channel", function = {
            if("guild" !in this) "DM" else get<TextChannel>("channel").name
        }, biFunction = { input ->
            channelSearch(this, input)?.name ?: "DM"
        }),

        method("channelid", function = {
            if("guild" !in this) "0" else get<TextChannel>("channel").id
        }, biFunction = { input ->
            channelSearch(this, input)?.id ?: "0"
        }),

        method("#channel", function = {
            if("guild" !in this) "DM" else get<TextChannel>("channel").asMention
        }, biFunction = { input ->
            channelSearch(this, input)?.asMention ?: "DM"
        }),

        method("randuser", function = {
            if("guild" !in this) return@method get<User>("user").name
            val members = get<Guild>("guild").members
            members[(members.size * Random.nextDouble()).toInt()].user.name
        }),

        method("randonline", function = {
            if("guild" !in this) return@method get<User>("user").name
            val online = get<Guild>("guild").memberCache.filter { it.onlineStatus == ONLINE }
            online[(online.size * Random.nextDouble()).toInt()].user.name
        }),

        method("randchannel", function = {
            if("guild" !in this) "DM" else {
                val textChannels = get<Guild>("guild").textChannels
                textChannels[(textChannels.size * Random.nextDouble()).toInt()].name
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
