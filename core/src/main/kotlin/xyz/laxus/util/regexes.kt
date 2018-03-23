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
@file:Suppress("Unused")
package xyz.laxus.util

val commandArgs = Regex("\\s+")
val discordID = Regex("\\d{17,20}")
val emoteRegex = Regex("<:\\S{2,32}:($discordID>)")
val userMention = Regex("<@!?($discordID)>")
val reasonPattern = Regex("(^.+)\\s(?:for\\s+)([\\s\\S]+)$", RegexOption.DOT_MATCHES_ALL)
val targetIDWithReason = Regex("($discordID)(?:\\s+(?:for\\s+)?([\\s\\S]+))?")
val targetMentionWithReason = Regex("$userMention(?:\\s+(?:for\\s+)?([\\s\\S]+))?")

fun parseModeratorArgument(args: String): Pair<Long, String?>? {
    val groups = (targetIDWithReason.matchEntire(args) ?:
                  targetMentionWithReason.matchEntire(args) ?: return null).groupValues

    return groups[1].trim().toLong() to groups[2].trim().takeIf { it.isNotEmpty() }
}

infix fun String.doesNotMatch(regex: Regex): Boolean = !(this matches regex)
