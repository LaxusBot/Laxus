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
package xyz.laxus.jda

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.EmbedBuilder.ZERO_WIDTH_SPACE
import net.dv8tion.jda.core.entities.IMentionable
import net.dv8tion.jda.core.entities.MessageEmbed
import xyz.laxus.jda.util.MessageDsl
import xyz.laxus.util.lineSeparator
import xyz.laxus.util.modifyIf
import java.awt.Color
import java.time.temporal.TemporalAccessor

// Inspired by club.minnced.kjda.builders.KJDAEmbedBuilder

/**
 * @author Kaidan Gustave
 */
@MessageDsl
class KEmbedBuilder @PublishedApi internal constructor(): Appendable {
    @PublishedApi
    internal val fields = mutableListOf<MessageEmbed.Field>()

    val description = StringBuilder()
    val length: Int get() {
        var length = description.length
        synchronized(fields) {
            length = fields.stream().map { f -> f.name.length + f.value.length }.reduce(length) { a, b -> a + b }
        }
        title?.let { length += it.length }
        author?.let { length += it.value.length }
        footer?.let { length += it.value.length }
        return length
    }

    @MessageDsl
    var title: String? = null
    @MessageDsl
    var url: String? = null
    @MessageDsl
    var thumbnail: String? = null
    @MessageDsl
    var image: String? = null
    @MessageDsl
    var author: Entity? = null
    @MessageDsl
    var footer: Entity? = null
    @MessageDsl
    var time: TemporalAccessor? = null
    @MessageDsl
    var color: Color? = null

    @PublishedApi
    internal fun build(): MessageEmbed = EmbedBuilder().apply {
        val(description, fields, title, url, time, color, author, thumbnail, footer, image) = this@KEmbedBuilder

        fields.forEach { addField(it) }

        if(!description.isBlank()) setDescription(description.toString())
        if(!title.isNullOrBlank()) setTitle(title, url)
        image?.let(::setImage)
        time?.let(::setTimestamp)
        thumbnail?.let(::setThumbnail)
        color?.let(::setColor)
        footer?.let { setFooter(it.value, it.icon) }
        author?.let { setAuthor(it.value, it.url, it.icon) }

    }.build()

    private operator fun component1()  = description
    private operator fun component2()  = fields
    private operator fun component3()  = title
    private operator fun component4()  = url
    private operator fun component5()  = time
    private operator fun component6()  = color
    private operator fun component7()  = author
    private operator fun component8()  = thumbnail
    private operator fun component9()  = footer
    private operator fun component10() = image

    @MessageDsl
    override fun append(csq: CharSequence?): KEmbedBuilder {
        description.append(csq)
        return this
    }

    @MessageDsl
    override fun append(csq: CharSequence?, start: Int, end: Int): KEmbedBuilder {
        description.append(csq, start, end)
        return this
    }

    @MessageDsl
    override fun append(c: Char): KEmbedBuilder {
        description.append(c)
        return this
    }

    @MessageDsl
    fun append(any: Any?) = append(((any as? IMentionable)?.asMention) ?: any.toString())

    @MessageDsl
    fun appendln(any: Any?) = append(any).appendln()

    @MessageDsl
    fun appendln() = append("\n")

    @MessageDsl
    operator fun plusAssign(any: Any?) { append(any) }

    @MessageDsl
    operator fun String.unaryPlus(): KEmbedBuilder {
        description.append(this)
        return this@KEmbedBuilder
    }

    @MessageDsl
    inline fun image(lazy: () -> String): KEmbedBuilder {
        image = lazy()
        return this
    }

    @MessageDsl
    inline fun url(lazy: () -> String): KEmbedBuilder {
        url = lazy()
        return this
    }

    @MessageDsl
    inline fun title(lazy: () -> String): KEmbedBuilder {
        title = lazy()
        return this
    }

    @MessageDsl
    inline fun thumbnail(lazy: () -> String): KEmbedBuilder {
        thumbnail = lazy()
        return this
    }

    @MessageDsl
    inline fun time(lazy: () -> TemporalAccessor): KEmbedBuilder {
        time = lazy()
        return this
    }

    @MessageDsl
    inline fun color(lazy: () -> Color?): KEmbedBuilder {
        color = lazy()
        return this
    }

    @MessageDsl
    inline fun author(lazy: Entity.() -> Unit): KEmbedBuilder {
        val data = Entity()
        data.lazy()
        author = data
        return this
    }

    @MessageDsl
    inline fun footer(lazy: Entity.() -> Unit): KEmbedBuilder {
        val data = Entity()
        data.lazy()
        footer = data
        return this
    }

    @MessageDsl
    inline fun field(name: String = ZERO_WIDTH_SPACE,
                     inline: Boolean = true,
                     lazy: Field.() -> Unit): KEmbedBuilder {
        val builder = Field(name = name, inline = inline)
        builder.lazy()
        fields.add(MessageEmbed.Field(builder.name, builder.value.toString(), builder.inline))
        return this
    }

    @MessageDsl
    inline fun code(lang: String, block: () -> Unit) {
        + "```$lang\n"
        block()
        + "```"
    }

    @MessageDsl
    data class Entity @PublishedApi internal constructor(
        @MessageDsl
        var value: String = ZERO_WIDTH_SPACE,
        @MessageDsl
        var url: String? = null,
        @MessageDsl
        var icon: String? = null
    ) {
        @MessageDsl
        inline fun value(lazy: () -> String): Entity {
            val value = lazy()
            this.value = value.modifyIf(value.isBlank()) { ZERO_WIDTH_SPACE }
            return this
        }

        @MessageDsl
        inline fun url(lazy: () -> String?): Entity {
            url = lazy()
            return this
        }

        @MessageDsl
        inline fun icon(lazy: () -> String?): Entity {
            icon = lazy()
            return this
        }
    }

    @MessageDsl
    data class Field @PublishedApi internal constructor(
        @MessageDsl
        var name: String = ZERO_WIDTH_SPACE,
        @MessageDsl
        var inline: Boolean = true
    ): Appendable {
        @PublishedApi
        internal val value = StringBuilder()

        @MessageDsl
        operator fun String.unaryPlus(): Field {
            append(this)
            return this@Field
        }

        @MessageDsl
        override fun append(csq: CharSequence?): Field {
            value.append(csq)
            return this
        }

        @MessageDsl
        override fun append(csq: CharSequence?, start: Int, end: Int) = append(csq?.subSequence(start..end))
        @MessageDsl

        override fun append(c: Char) = append(c.toString())

        @MessageDsl
        fun appendln(any: Any?) = append(any).appendln()

        @MessageDsl
        fun appendln() = append(lineSeparator)

        @MessageDsl
        fun append(any: Any?) = append(((any as? IMentionable)?.asMention) ?: any.toString())
    }
}
