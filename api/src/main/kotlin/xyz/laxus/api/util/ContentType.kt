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
package xyz.laxus.api.util

import xyz.laxus.util.hashAll
import java.nio.charset.Charset
import javax.annotation.concurrent.Immutable

/**
 * @author Kaidan Gustave
 */
@Immutable
class ContentType private constructor(
    val mime: String, val extension: String,
    val params: Map<String, String> = mapOf(),
    val charset: Charset? = null
) {
    companion object {
        private const val CharsetMapKey = "charset"

        @JvmStatic
        fun parse(mimeType: String): ContentType {
            val parts = mimeType.split('/', limit = 2)

            require(parts.size == 2) { "MIME type '$mimeType' is an invalid formatted!" }

            val extensionParts = parts[1].split(';').filter { it.isNotEmpty() }

            val mime = parts[0]
            val extension = extensionParts[0]
            var charset: Charset? = null

            val params = extensionParts.takeUnless { it.size == 1 }?.subList(1, extensionParts.size)?.associate by@ {
                val pieces = it.split('=', limit = 2)

                require(pieces.count { it.isNotBlank() } == 2) {
                    "MIME type param '${pieces[0]}' does not have a value specified!"
                }

                val name = pieces[0].trim().toLowerCase()
                val value = pieces[1].trim()

                if(name == CharsetMapKey) {
                    charset = Charset.forName(value)
                }

                return@by name to value
            } ?: mapOf()
            return ContentType(mime, extension, params, charset)
        }

        @JvmStatic
        private fun ContentType.clone(
            mime: String = this.mime,
            extension: String = this.extension,
            params: Map<String, String> = this.params,
            charset: Charset? = this.charset
        ) = ContentType(mime, extension, params, charset)
    }

    fun param(name: String) = params[name.toLowerCase()]

    fun withCharset(charset: Charset?): ContentType {
        return if(charset == null) {
            clone(params = this.params - CharsetMapKey, charset = null)
        } else {
            clone(params = this.params + (CharsetMapKey to charset.name()), charset = charset)
        }
    }

    fun withParam(name: String, value: String): ContentType {
        val newParamMap = mapOf(name.toLowerCase() to value)
        val newCharset = newParamMap[CharsetMapKey]?.let { Charset.forName(it) } ?: this.charset
        return clone(params = newParamMap, charset = newCharset)
    }

    fun withParams(vararg params: Pair<String, String>): ContentType {
        val newParamMap = params.associate { it.copy(first = it.first.toLowerCase()) }
        val newCharset = newParamMap[CharsetMapKey]?.let { Charset.forName(it) } ?: this.charset
        return clone(params = newParamMap, charset = newCharset)
    }

    fun addParam(name: String, value: String): ContentType {
        val newParamMap = this.params + (name.toLowerCase() to value)
        val newCharset = newParamMap[CharsetMapKey]?.let { Charset.forName(it) } ?: this.charset
        return clone(params = newParamMap, charset = newCharset)
    }

    fun addParams(vararg params: Pair<String, String>): ContentType {
        val newParamMap = this.params + params.associate { it.copy(first = it.first.toLowerCase()) }
        val newCharset = newParamMap[CharsetMapKey]?.let { Charset.forName(it) } ?: this.charset
        return clone(params = newParamMap, charset = newCharset)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is ContentType) return false

        // Don't check for charset equivalence because it's covered by the map check
        return mime == other.mime && extension == other.extension &&
               params.entries.all { it.value.equals(other.params[it.key], ignoreCase = true) }
    }

    override fun hashCode(): Int = hashAll(mime, extension, params, charset)

    override fun toString(): String {
        return "$mime/$extension" + params.entries.joinToString(
            separator = ";", prefix = ";") { "${it.key}=${it.value}" }
    }

    object Application {
        val Gzip = ContentType("application", "gzip")
        val Json = ContentType("application", "json")
        val Zip = ContentType("application", "zip")
    }

    object Text {
        val Plain = ContentType("text", "plain")
        val Css = ContentType("text", "css")
        val Html = ContentType("text", "html")
    }
}