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
package xyz.laxus.bot.utils

import net.dv8tion.jda.core.entities.*
import xyz.laxus.bot.utils.jda.boldFormattedName
import xyz.laxus.utils.titleName
import java.time.LocalDateTime
import java.time.OffsetDateTime

fun noMatch(lookedFor: String, query: String): String = "Could not find any $lookedFor matching \"$query\"!"

fun List<User>.multipleUsers(argument: String): String = listOut("user", argument) { it.boldFormattedName }
fun List<Member>.multipleMembers(argument: String): String = listOut("member", argument) { it.user.boldFormattedName }
fun List<TextChannel>.multipleTextChannels(argument: String): String = listOut("text channel", argument) { it.asMention }
fun List<VoiceChannel>.multipleVoiceChannels(argument: String): String = listOut("voice channel", argument) { it.name }
fun List<Category>.multipleCategories(argument: String): String = listOut("categories", argument) { it.name }
fun List<Role>.multipleRoles(argument: String): String = listOut("role", argument) { it.name }

inline fun <reified T> List<T>.listOut(kind: String, argument: String, conversion: (T) -> String): String = buildString {
    append("Multiple ${kind}s found matching \"$argument\":\n")
    val s = this@listOut.size
    for(i in 0 until 4) {
        append("${this@listOut[i].let(conversion)}\n")
        if(i == 3 && s > 4) {
            append("And ${s - 4} other $kind")
            append(if(s - 4 > 1) "s..." else "...")
        }
        if(i + 1 == s)
            break
    }
}

val OffsetDateTime.readableFormat get() = "${dayOfWeek.titleName}, ${month.titleName} $dayOfMonth, $year"
val LocalDateTime.readableFormat get() = "${dayOfWeek.titleName}, ${month.titleName} $dayOfMonth, $year"
