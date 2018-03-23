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
package xyz.laxus.jda.util

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.requests.restaction.MessageAction
import xyz.laxus.jda.KEmbedBuilder

inline fun message(builder: MessageBuilder = MessageBuilder(), init: MessageBuilder.() -> Unit): Message {
    builder.init()
    return builder.build()
}

inline fun MessageBuilder.embed(crossinline init: KEmbedBuilder.() -> Unit): MessageBuilder {
    val builder = KEmbedBuilder()
    builder.init()
    return setEmbed(builder.build())
}

inline fun <reified M: Message> M.editMessage(block: MessageBuilder.() -> Unit): MessageAction {
    return editMessage(MessageBuilder().apply(block).build())
}

inline fun embed(init: KEmbedBuilder.() -> Unit): MessageEmbed = with (KEmbedBuilder()) {
    init()
    build()
}

fun filterMassMentions(string: String): String {
    return string.replace("@everyone", "@\u0435veryone").replace("@here", "@h\u0435re").trim()
}
