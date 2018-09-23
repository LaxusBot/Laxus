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
@file:JvmName("TimeUtil")
package xyz.laxus.bot.utils

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*

// Used to expand time arguments without any space inbetween
//the numeric length and the unit string so that they do have
//a space. IE: 1sec -> 1 sec
private val noSpaceExpansionRegex = Regex("(\\d+)\\s*([a-zA-Z]+)")

fun parseTimeArgument(str: String): Duration? {
    val expanded = str.replace(noSpaceExpansionRegex) {
        val (_, n, u) = it.groupValues
        return@replace "$n $u"
    }

    // left is quantity, right is unit
    var unit = false
    val (a, t) = expanded.split(' ').asSequence().filter { it.isNotBlank() }
        .partition {
            unit = !unit
            return@partition unit
        }

    if(a.size != t.size) return null

    val milliseconds = (a.map(String::toLongOrNull) zip t.map(::chronoUnitFrom))
        .fold(0L) { sum, (amount, type) ->
            if(amount === null || type === null) return@fold sum
            return@fold sum + (type.duration.toMillis() * amount)
        }
    return Duration.ofMillis(milliseconds)
}

internal fun chronoUnitFrom(str: String): ChronoUnit? {
    return when((if(str.length > 1 && str.endsWith('s'))
        str.substring(0, str.lastIndex) else str).toLowerCase()) {
        "year", "y" -> YEARS
        "month", "mon", "mo" -> MONTHS
        "week", "w" -> WEEKS
        "day", "d" -> DAYS
        "hour", "hr", "h" -> HOURS
        "minute", "min", "m" -> MINUTES
        "second", "sec", "s" -> SECONDS
        else -> null
    }
}
