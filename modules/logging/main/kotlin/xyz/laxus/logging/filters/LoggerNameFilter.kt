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
package xyz.laxus.logging.filters

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import ch.qos.logback.core.spi.FilterReply.NEUTRAL
import xyz.laxus.commons.collections.FixedSizeCache
import xyz.laxus.utils.regexpStringFromGlob
import java.util.*
import java.util.function.Function
import java.util.regex.PatternSyntaxException

/**
 * @author Kaidan Gustave
 */
class LoggerNameFilter: Filter<ILoggingEvent>() {
    private lateinit var cache: FixedSizeCache<String, FilterReply>
    private val matchers = LinkedList<(String) -> Boolean>()

    // Reuse the same function, even if it has very
    //little to no affect on overhead
    private val computeIfAbsent = Function<String, FilterReply> reply@ {
        for(matcher in matchers) {
            if(matcher(it)) {
                return@reply onMatch
            }
        }
        return@reply onMismatch
    }

    private var _level: Level? = null


    var level: String?
        get() = _level?.levelStr
        set(value) { _level = Level.toLevel(value) }
    var cacheSize = 50
    var onMatch = NEUTRAL
    var onMismatch = NEUTRAL

    fun addPattern(glob: String) {
        addInfo("Parsing Glob to RegExp: $glob")
        val regexp = regexpStringFromGlob(glob)
        addInfo("Parsed: $regexp")

        // Nothing changed, our function
        //will compare for equality
        if(regexp == glob) {
            matchers += { it == glob }
            return
        }

        val regex = try { Regex(regexp, RegexOption.DOT_MATCHES_ALL) } catch(e: PatternSyntaxException) {
            addError("Glob pattern not valid: $glob (index: ${e.index})")
            return
        }

        matchers += { it matches regex }
    }

    override fun start() {
        super.start()
        addInfo("Creating result cache (size: $cacheSize)")
        this.cache = FixedSizeCache(cacheSize)
    }

    override fun decide(event: ILoggingEvent?): FilterReply {
        _level?.let { filterLevel ->
            event?.level?.let { eventLevel ->
                if(filterLevel != eventLevel) {
                    return NEUTRAL
                }
            }
        }
        val loggerName = event?.loggerName
        if(loggerName === null) return NEUTRAL
        return cache.computeIfAbsent(loggerName, computeIfAbsent)
    }

    interface Matcher {
        fun match()
    }
}
