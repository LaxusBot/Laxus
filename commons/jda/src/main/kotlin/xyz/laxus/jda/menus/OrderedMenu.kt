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
@file:Suppress("unused", "LiftReturnOrAssignment")
package xyz.laxus.jda.menus

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.RestAction
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.message
import xyz.laxus.util.collections.unmodifiableList
import xyz.laxus.util.modifyIf
import java.awt.Color
import java.util.*
import kotlin.coroutines.experimental.coroutineContext

/**
 * Modeled after jagrosh's OrderedMenu in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
class OrderedMenu private constructor(builder: OrderedMenu.Builder): Menu(builder) {
    private companion object {
        val numbers = arrayOf(
            "1\u20E3", "2\u20E3",
            "3\u20E3", "4\u20E3",
            "5\u20E3", "6\u20E3",
            "7\u20E3", "8\u20E3",
            "9\u20E3", "\uD83D\uDD1F"
        )

        val letters = arrayOf(
            "\uD83C\uDDE6", "\uD83C\uDDE7",
            "\uD83C\uDDE8", "\uD83C\uDDE9",
            "\uD83C\uDDEA", "\uD83C\uDDEB",
            "\uD83C\uDDEC", "\uD83C\uDDED",
            "\uD83C\uDDEE", "\uD83C\uDDEF"
        )

        const val cancel = "\u274C"
    }

    private val color: Color? = builder.color
    private val text: String? = builder.text
    private val description: String? = builder.description
    private val choices: List<OrderedMenu.Choice> = builder.choices
    private val useLetters: Boolean = builder.useLetters
    private val allowTypedInput: Boolean = builder.allowTypedInput
    private val useCancel: Boolean = builder.useCancel
    private val finalAction: FinalAction? = builder.finalAction

    constructor(builder: OrderedMenu.Builder = OrderedMenu.Builder(),
                build: OrderedMenu.Builder.() -> Unit): this(builder.apply(build))

    override suspend fun displayIn(channel: MessageChannel) {
        if(channel is TextChannel && !allowTypedInput &&
           !channel.guild.selfMember.hasPermission(channel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialize(channel.sendMessage(message))
    }

    override suspend fun displayAs(message: Message) {
        // This check is basically for whether or not the menu can even display.
        // Is from text channel
        // Does not allow typed input
        // Does not have permission to add reactions
        if(message.channelType == ChannelType.TEXT && !allowTypedInput &&
           !message.guild.selfMember.hasPermission(message.textChannel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialize(message.editMessage(this.message))
    }

    private suspend fun initialize(action: RestAction<Message>) {
        val m = action.await()
        launch(coroutineContext) {
            try {
                // From 0 until the number of choices.
                // The last run of this loop will be used to queue
                // the last reaction and possibly a cancel emoji
                // if useCancel was set true before this OrderedMenu
                // was built.
                for(i in choices.indices) {
                    // If this is not the last run of this loop
                    if(i < choices.size - 1) {
                        m.addReaction(i.emoji).await()
                    } else {
                        var re = m.addReaction(i.emoji)
                        // If we're using the cancel function we want
                        // to add a "step" so we queue the last emoji being
                        // added and then make the RestAction to start waiting
                        // on the cancel reaction being added.
                        if(useCancel) {
                            re.await() // queue the last emoji
                            re = m.addReaction(cancel)
                        }
                        // queue the last emoji or the cancel button
                        re.await()
                        if(allowTypedInput) waitGeneric(m) else waitReactionOnly(m)
                    } // If this is the last run of this loop
                }
            } catch (ex: PermissionException) {
                // If there is a permission exception mid process, we'll still
                // attempt to make due with what we have.
                if(allowTypedInput) waitGeneric(m) else waitReactionOnly(m)
            }
        }
    }

    private suspend fun waitGeneric(message: Message) {
        val deferred = waiter.receive<GenericMessageEvent>(delay = timeout, unit = unit) {
            when(it) {
                is MessageReactionAddEvent -> it.isValid(message)
                is MessageReceivedEvent -> it.isValid(message)
                else -> false
            }
        }

        val event = deferred.await()

        if(event === null) {
            finalAction?.invoke(message)
            return
        }

        if(event is MessageReactionAddEvent) {
            if(event.reactionEmote.name == cancel && useCancel) {
                finalAction?.invoke(message)
            } else {
                val choice = choices[event.reactionEmote.name.number]
                choice(message)
            }
        } else if(event is MessageReceivedEvent) {
            val num = event.message.contentRaw.messageNumber
            if(num < 0 || num > choices.size) {
                finalAction?.invoke(message)
            } else {
                val choice = choices[num]
                choice(message)
            }
        }
    }

    private suspend fun waitReactionOnly(message: Message) {
        val deferred = waiter.receive<MessageReactionAddEvent>(delay = timeout, unit = unit) {
            it.isValid(message)
        }

        val event = deferred.await()

        if(event === null) {
            finalAction?.invoke(message)
            return
        }

        if(event.reactionEmote.name == cancel && useCancel) {
            finalAction?.invoke(message)
        } else {
            val choice = choices[event.reactionEmote.name.number]
            choice(message)
        }
    }

    private fun MessageReactionAddEvent.isValid(message: Message): Boolean {
        if(messageIdLong != message.idLong)
            return false
        if(!isValidUser(user, guild))
            return false
        if(reactionEmote.name == cancel && useCancel)
            return true

        val num = reactionEmote.name.number

        return !(num < 0 || num > choices.size)
    }

    private fun MessageReceivedEvent.isValid(message: Message): Boolean {
        if(channel.idLong != message.channel.idLong)
            return false

        return isValidUser(author, guild)
    }

    private val message get() = message {
        text?.let { append { text } }
        embed {
            description?.let { append(it) }

            this.color = this@OrderedMenu.color

            for((i, c) in choices.withIndex()) {
                append("\n${i.emoji} $c")
            }
        }
    }

    private val Int.emoji: String get() {
        return numbers.modifyIf(useLetters) { letters }[this]
    }

    private val String.number: Int get() {
        return numbers.modifyIf(useLetters) { letters }.withIndex().firstOrNull {
            it.value == this
        }?.index ?: -1
    }

    private val String.messageNumber: Int get() {
        if(useLetters) {
            // This doesn't look good, but bear with me for a second:
            // So the maximum number of letters you can have as reactions
            // is 10 (the maximum number of choices in general even).
            // If you look carefully, you'll see that a corresponds to the
            // index 1, b to the index 2, and so on.
            return if(length == 1) " abcdefghij".indexOf(toLowerCase()) else -1
        } else {
            // The same as above applies here, albeit in a different way.
            return when {
                length == 1 -> " 123456789".indexOf(this)
                this == "10" -> 10
                else -> -1
            }
        }
    }

    class Builder : Menu.Builder<OrderedMenu.Builder, OrderedMenu>() {
        var color: Color? = null
        var text: String? = null
        var description: String? = null
        var useLetters = false
        var allowTypedInput = true
        var useCancel = false
        var finalAction: FinalAction? = null

        private val _choices: MutableList<OrderedMenu.Choice> = LinkedList()
        internal val choices: List<OrderedMenu.Choice> get() = unmodifiableList(_choices)

        operator fun set(name: String, action: FinalAction) {
            _choices += OrderedMenu.Choice(name, action)
        }

        fun clearChoices() = _choices.clear()
        fun finalAction(block: FinalAction): OrderedMenu.Builder = apply { finalAction = block }
        fun choice(name: String, action: FinalAction): OrderedMenu.Builder = apply { set(name, action) }
        inline fun description(lazy: () -> String?): OrderedMenu.Builder = apply { description = lazy() }
        inline fun text(lazy: () -> String?): OrderedMenu.Builder = apply { text = lazy() }
        inline fun useCancelButton(lazy: () -> Boolean): OrderedMenu.Builder = apply { useCancel = lazy() }
        inline fun color(lazy: () -> Color?): OrderedMenu.Builder = apply { color = lazy() }
        inline fun useLetters(lazy: () -> Boolean): OrderedMenu.Builder = apply { useLetters = lazy() }
        inline fun useNumbers(lazy: () -> Boolean): OrderedMenu.Builder = apply { useLetters = !lazy() }
        inline fun allowTextInput(lazy: () -> Boolean): OrderedMenu.Builder = apply { allowTypedInput = lazy() }
    }

    class Choice(val name: String = "null", private val action: FinalAction? = null) {
        suspend operator fun invoke(message: Message) {
            action?.invoke(message)
        }

        override fun toString(): String = name
    }
}
