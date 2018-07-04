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
package xyz.laxus.util

import xyz.laxus.util.collections.sumByLong
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.*

// Used to expand time arguments without any space inbetween
//the numeric length and the unit string so that they do have
//a space. IE: 1sec -> 1 sec
private val noSpaceExpansionRegex = Regex("(\\d+)([a-zA-Z]+)")

fun parseTimeArgument(str: String, relativeTo: LocalDateTime = LocalDateTime.now()): Duration? {
    val expanded = str.replace(noSpaceExpansionRegex) {
        val (_, n, u) = it.groupValues
        return@replace "$n $u"
    }

    var unit = false
    // left is quantity, right is unit
    val (a, t) = expanded.split(' ').asSequence().filter { it.isNotBlank() }.partition {
        unit = !unit
        return@partition unit
    }

    if(a.size != t.size) return null

    val milliseconds = (a.map(String::toLongOrNull) zip t.map(::chronoUnitFrom)).sumByLong sum@ {
        val (amount, type) = it
        if(amount === null || type === null) return null
        return@sum relativeTo.until(relativeTo.plus(amount, type), MILLIS)
    }

    return Duration.ofMillis(milliseconds)
}

internal fun chronoUnitFrom(str: String): ChronoUnit? {
    val checking = str.modifyIf({ it.length > 1 && it.endsWith('s') }) { it.substring(0, it.lastIndex) }.toLowerCase()
    return when(checking) {
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
