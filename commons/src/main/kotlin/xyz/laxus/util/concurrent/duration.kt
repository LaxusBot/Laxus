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
package xyz.laxus.util.concurrent

import xyz.laxus.util.hashAll
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

fun duration(length: Long, unit: TimeUnit) = Duration(length, unit)

data class Duration
internal constructor(val length: Long, val unit: TimeUnit): Comparable<Duration> {
    init {
        require(length >= 0) { "Duration cannot have a negative length!" }
    }

    // Conversion Functions

    fun inSeconds()      = copy(length = lengthIn(SECONDS),      unit = SECONDS     )
    fun inMinutes()      = copy(length = lengthIn(MINUTES),      unit = MINUTES     )
    fun inHours()        = copy(length = lengthIn(HOURS),        unit = HOURS       )
    fun inMilliseconds() = copy(length = lengthIn(MILLISECONDS), unit = MILLISECONDS)
    fun inDays()         = copy(length = lengthIn(DAYS),         unit = DAYS        )
    fun inMicroseconds() = copy(length = lengthIn(MICROSECONDS), unit = MICROSECONDS)
    fun inNanoseconds()  = copy(length = lengthIn(NANOSECONDS),  unit = NANOSECONDS )

    private fun lengthIn(unit: TimeUnit): Long {
        if(this.unit == unit) return this.length
        return this.unit.convert(this.length, unit)
    }

    override fun compareTo(other: Duration): Int {
        if(this == other) return 0

        // Same unit, compare length
        if(this.unit == other.unit) {
            return this.length.compareTo(other.length)
        }

        // Different units, convert length to
        //other unit and compare
        return lengthIn(other.unit).compareTo(other.length)
    }

    override fun toString() = "$length ${unit.name.toLowerCase()}"
    override fun hashCode() = hashAll(length, unit)
    override fun equals(other: Any?): Boolean {
        if(other !is Duration) return false
        if(this === other) return true

        // Same unit, check length for equality
        if(this.unit == other.unit) {
            return this.length == other.length
        }

        // Different units, convert length to
        //other unit and check for equality
        return lengthIn(other.unit) == other.length
    }
}