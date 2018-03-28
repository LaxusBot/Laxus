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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package xyz.laxus.util

import java.util.TimeZone

/**
 * @author Kaidan Gustave
 */
object Emojis {
    // Discord Custom Emotes
    const val RedTick   = "<:xmark:314349398824058880>"
    const val GreenTick = "<:check:314349398811475968>"
    const val GitHub     = "<:GitHub:377567548075671552>"
    const val Twitch     = "<:twitch:314349922755411970>"

    // Unicode Emojis
    const val GlobeWithMeridians = "\uD83C\uDF10"
    const val Cake               = "\uD83C\uDF70"

    // Holder for flag emojis.
    // The first is the actual emoji.
    // The second is for the country name that is the first part
    // of a ZoneInfo#id.
    // So for "Chicago, Illinois" the ZoneInfo would be:
    // ZoneInfo.getTimeZone("America/Chicago")
    // Of which the country is "America" and the Flag would
    // be Flag.US
    enum class Flag(val emoji: String, country: String? = null) {
        USA("\uD83C\uDDFA\uD83C\uDDF8", "America"),
        CANADA("\uD83C\uDDE8\uD83C\uDDE6"),
        AFRICA("\uD83C\uDDE6\uD83C\uDDEB"),
        GERMANY("\uD83C\uDDE9\uD83C\uDDEA"),
        ENGLAND("\uD83C\uDDEC\uD83C\uDDE7"),
        RUSSIA("\uD83C\uDDF7\uD83C\uDDFA"),
        FRANCE("\uD83C\uDDEB\uD83C\uDDF7")
        // TODO Add more countries
        ;

        val country = country ?: niceName

        override fun toString(): String = emoji

        companion object {
            fun of(zone: TimeZone?, ignoreCase: Boolean = true): Flag? {
                val countryId = (zone ?: return null).id.run {
                    substring(0, this.indexOf('/').takeIf { it != -1 } ?: return null)
                }

                return values().find { countryId.equals(it.country, ignoreCase) }
            }
        }
    }

    enum class Star(val emoji: String, val threshold: Int) {
        MEDIUM_WHITE_STAR("\u2B50", 1),
        GLOWING_STAR("\uD83C\uDF1F", 6),
        DIZZY_SYMBOL("\uD83D\uDCAB", 11),
        SPARKLES("\u2728", 16);

        companion object {
            fun forCount(count: Int): Star = values().firstOrNull { count <= it.threshold } ?: SPARKLES
        }
    }
}
