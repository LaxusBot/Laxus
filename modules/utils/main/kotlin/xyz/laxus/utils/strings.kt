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
@file:JvmName("StringsUtil")
@file:Suppress("unused")
package xyz.laxus.utils

/**
 * Inversion of [String.matches].
 *
 * @receiver The [String] to check.
 *
 * @param regex The [Regex] to check.
 *
 * @return `false` if the receiver does not match the [regex].
 */
infix fun String.doesNotMatch(regex: Regex): Boolean = !(this matches regex)

/**
 * Converts a [Text Glob](https://metacpan.org/pod/Text::Glob)
 * pattern to a [RegExp][Regex] equivalent.
 *
 * @param glob The Text Glob to convert.
 *
 * @return A RegExp equivalent.
 */
fun regexpStringFromGlob(glob: String): String {
    var g = glob
    if(g.startsWith('*')) g = g.substring(1)
    if(g.endsWith('*')) g = g.substring(0, g.length - 1)
    return buildString(glob.length) {
        var escaping = false
        var level = 0
        for(c in g) when(c) {
            '*'  -> {
                append(if(escaping) "\\*" else ".*")
                escaping = false
            }

            '?'  -> {
                append(if(escaping) "\\?" else ".")
                escaping = false
            }

            '.', '(', ')',
            '+', '|', '^',
            '$', '@', '%' -> {
                append('\\')
                append(c)
                escaping = false
            }

            '\\' -> {
                if(escaping) {
                    append("\\\\")
                    escaping = false
                } else {
                    escaping = true
                }
            }

            '{' -> {
                if(escaping) {
                    append("\\{")
                } else {
                    append('(')
                    level++
                }
                escaping = false
            }

            '}' -> {
                if(level > 0 && !escaping) {
                    append(')')
                    level--
                } else if(escaping) {
                    append("\\}")
                } else {
                    append("}")
                }
                escaping = false
            }

            ',' -> {
                if(level > 0 && !escaping) {
                    append('|')
                } else if(escaping) {
                    append("\\,")
                } else {
                    append(',')
                }
                escaping = false
            }

            else -> {
                append(c)
                escaping = false
            }
        }
    }
}

/**
 * Creates a [Regex] from the specified
 * [Text Glob](https://metacpan.org/pod/Text::Glob) pattern.
 *
 * @param glob The Text Glob to convert.
 *
 * @return A Regex
 */
fun regexpFromGlob(glob: String): Regex = Regex(regexpStringFromGlob(glob))
