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
@file:Suppress("Unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.jda.menus

import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.GenericMessageEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.requests.RestAction
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.message
import xyz.laxus.util.functional.AddRemoveBlock
import xyz.laxus.util.ignored
import java.awt.Color
import kotlin.coroutines.experimental.coroutineContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Modeled after jagrosh's Paginator in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
class Paginator private constructor(builder: Paginator.Builder): Menu(builder) {
    internal companion object {
        const val BIG_LEFT = "\u23EA"
        const val LEFT = "\u25C0"
        const val STOP = "\u23F9"
        const val RIGHT = "\u25B6"
        const val BIG_RIGHT = "\u23E9"
    }

    private val color: PageFunction<Color?> = builder.colorFun
    private val text: PageFunction<String?> = builder.textFun
    private val items: List<String> = builder.items
    private val columns: Int = builder.columns
    private val itemsPerPage: Int = builder.itemsPerPage
    private val numberItems: Boolean = builder.numberItems
    private val showPageNumbers: Boolean = builder.showPageNumbers
    private val waitOnSinglePage: Boolean = builder.waitOnSinglePage
    private val bulkSkipNumber: Int = builder.bulkSkipNumber
    private val wrapPageEnds: Boolean = builder.wrapPageEnds
    private val leftText: String? = builder.textToLeft
    private val rightText: String? = builder.textToRight
    private val allowTextInput: Boolean = builder.allowTextInput
    private val finalAction: FinalAction? = builder.finalAction
    private val pages: Int = ceil(items.size.toDouble() / itemsPerPage).roundToInt()

    constructor(builder: Paginator.Builder = Paginator.Builder(),
                build: Paginator.Builder.() -> Unit): this(builder.apply(build))

    init {
        require(items.isNotEmpty()) { "Must include at least one item to paginate." }
    }

    override suspend fun displayIn(channel: MessageChannel) = paginate(channel, 1)

    override suspend fun displayAs(message: Message) = paginate(message, 1)

    suspend fun paginate(message: Message, pageNum: Int) {
        val num = min(max(pageNum, 1), pages)
        initialize(message.editMessage(renderPage(num)), num)
    }

    suspend fun paginate(channel: MessageChannel, pageNum: Int) {
        val num = min(max(pageNum, 1), pages)
        initialize(channel.sendMessage(renderPage(num)), num)
    }

    private suspend fun initialize(action: RestAction<Message>, pageNum: Int) {
        val m = action.await()
        launch(coroutineContext) {
            when {
                pages > 1 -> {
                    ignored {
                        if(bulkSkipNumber > 1) m.addReaction(BIG_LEFT).await()
                        m.addReaction(LEFT).await()
                        m.addReaction(STOP).await()
                        m.addReaction(RIGHT).await()
                        if(bulkSkipNumber > 1) m.addReaction(BIG_RIGHT).await()
                    }
                    pagination(m, pageNum)
                }

                waitOnSinglePage -> {
                    ignored { m.addReaction(STOP).await() }
                    pagination(m, pageNum)
                }

                else -> finalAction?.invoke(m)
            }
        }
    }

    private suspend fun pagination(message: Message, pageNum: Int) {
        if(allowTextInput || leftText !== null && rightText !== null)
            paginationWithTextInput(message, pageNum)
        else
            paginationWithoutTextInput(message, pageNum)
    }

    private suspend fun paginationWithTextInput(message: Message, pageNum: Int) {
        val deferred = waiter.receive<GenericMessageEvent>(delay = timeout, unit = unit) { event ->
            if(event is MessageReactionAddEvent) {
                return@receive checkReaction(event, message)
            } else if(event is MessageReceivedEvent) {
                if(event.channel != message.channel)
                    return@receive false

                val content = event.message.contentRaw
                if(leftText !== null && rightText !== null) {
                    if(content.equals(leftText, true) || content.equals(rightText, true)) {
                        return@receive isValidUser(event.author, event.guild)
                    }
                }

                if(allowTextInput) {
                    ignored {
                        val i = content.toInt()
                        if(i in 1..pages && i != pageNum) {
                            return@receive isValidUser(event.author, event.guild)
                        }
                    }
                }
            }

            // Default return false
            false
        }

        val event = deferred.await()

        when(event) {
            null -> {
                finalAction?.invoke(message)
                return
            }

            is MessageReactionAddEvent -> {
                handleMessageReactionAddAction(event, message, pageNum)
            }

            is MessageReceivedEvent -> {
                val received = event.message
                val content = received.contentRaw

                val targetPage: Int = when {
                    leftText !== null && content.equals(leftText, true) && (1 < pageNum || wrapPageEnds) -> {
                        if(pageNum - 1 < 1 && wrapPageEnds) pages else pageNum - 1
                    }

                    rightText !== null && content.equals(rightText, true) && (pageNum < pages || wrapPageEnds) -> {
                        if(pageNum + 1 > pages && wrapPageEnds) 1 else pageNum + 1
                    }

                    else -> content.toInt()
                }

                val m = ignored(message) { message.editMessage(renderPage(targetPage)).await() }
                pagination(m, targetPage)
            }
        }
    }

    private suspend fun paginationWithoutTextInput(message: Message, pageNum: Int) {
        val deferred = waiter.receive<MessageReactionAddEvent>(delay = timeout, unit = unit) {
            checkReaction(it, message)
        }

        val event = deferred.await()

        if(event === null) {
            finalAction?.invoke(message)
            return
        }

        handleMessageReactionAddAction(event, message, pageNum)
    }

    private suspend fun handleMessageReactionAddAction(event: MessageReactionAddEvent, message: Message, pageNum: Int) {
        var newPageNum = pageNum
        when(event.reactionEmote.name) {
            LEFT -> {
                if(newPageNum > 1)
                    newPageNum--
            }
            RIGHT -> {
                if(newPageNum < pages)
                    newPageNum++
            }
            BIG_LEFT -> {
                if(newPageNum > 1 || wrapPageEnds) {
                    var i = 1
                    while((newPageNum > 1 || wrapPageEnds) && i < bulkSkipNumber) {
                        if(newPageNum == 1 && wrapPageEnds)
                            newPageNum = pages + 1
                        newPageNum--
                        i++
                    }
                }
            }
            BIG_RIGHT -> {
                if(newPageNum < pages || wrapPageEnds) {
                    var i = 1
                    while((newPageNum < pages || wrapPageEnds) && i < bulkSkipNumber) {
                        if(newPageNum == pages && wrapPageEnds)
                            newPageNum = 0
                        newPageNum++
                        i++
                    }
                }
            }
            STOP -> {
                finalAction?.invoke(message)
                return // Stop and return
            }
        }

        ignored { event.reaction.removeReaction(event.user).queue() }
        val m = ignored(message) { message.editMessage(renderPage(newPageNum)).await() }
        pagination(m, newPageNum)
    }

    private fun checkReaction(event: MessageReactionAddEvent, message: Message): Boolean {
        return if(event.messageIdLong != message.idLong) false else when(event.reactionEmote.name) {
            LEFT, RIGHT, STOP -> isValidUser(event.user, event.guild)
            BIG_LEFT, BIG_RIGHT -> bulkSkipNumber > 1 && isValidUser(event.user, event.guild)

            else -> false
        }
    }

    private fun renderPage(pageNum: Int): Message {
        val start = (pageNum - 1) * itemsPerPage
        val end = min(items.size, pageNum * itemsPerPage)

        return message {
            text(pageNum, pages)?.let { this@message.append(it) }
            embed {
                if(columns == 1) {
                    for(i in start until end) {
                        appendln()
                        append(if(numberItems) "`${i + 1}.`" else "")
                        append(items[i])
                    }
                } else {
                    val per = ceil((end - start).toDouble() / columns).roundToInt()
                    for(k in 0 until columns) {
                        field {
                            name = ""
                            var i = start + k * per
                            while((i < end) && (i < start + (k + 1) * per)) {
                                appendln()
                                append(if(numberItems) "${i + 1}. " else "")
                                append(items[i])
                                i++
                            }

                            inline = true
                        }
                    }
                }

                color { color(pageNum, pages) }

                if(showPageNumbers) {
                    footer {
                        value = "Page $pageNum/$pages"
                        url = null
                    }
                }
            }
        }
    }

    class Builder : Menu.Builder<Paginator.Builder, Paginator>() {
        var colorFun: PageFunction<Color?> = { _, _ -> null }
        var textFun: PageFunction<String?> = { _, _ -> null }
        val items: MutableList<String> = ArrayList()
        var columns: Int = 1
            set(value) {
                require(value in 1..3) { "Number of columns must be at least 1 and at most 3" }
                field = value
            }
        var itemsPerPage: Int = 10
            set(value) {
                require(value >= 1) { "Number of items must be at least 1" }
                field = value
            }
        var numberItems: Boolean = false
        var showPageNumbers: Boolean = true
        var waitOnSinglePage: Boolean = false
        var bulkSkipNumber: Int = 1
            set(value) { field = max(value, 1) }
        var wrapPageEnds: Boolean = false
        var textToLeft: String? = null
        var textToRight: String? = null
        var allowTextInput: Boolean = false
        var finalAction: FinalAction? = null

        @PublishedApi
        internal val block by lazy { ItemControllerBlock(items) }

        operator fun String.unaryPlus() {
            items.add(this)
        }

        operator fun plusAssign(item: String) {
            items.add(item)
        }

        operator fun set(index: Int, item: String) {
            if(index > items.size) {
                for(i in (items.size) until (index - 1)) {
                    items[i] = ""
                }
            }
            items[index] = item
        }

        fun clearItems(): Paginator.Builder {
            items.clear()
            return this
        }

        inline fun add(lazy: () -> String): Paginator.Builder {
            items.add(lazy())
            return this
        }

        inline fun items(lazy: AddRemoveBlock<String>.() -> Unit): Paginator.Builder {
            block.lazy()
            return this
        }

        inline fun columns(lazy: () -> Int): Paginator.Builder {
            columns = lazy()
            return this
        }

        inline fun itemsPerPage(lazy: () -> Int): Paginator.Builder {
            itemsPerPage = lazy()
            return this
        }

        inline fun numberItems(lazy: () -> Boolean): Paginator.Builder {
            numberItems = lazy()
            return this
        }

        inline fun showPageNumbers(lazy: () -> Boolean): Paginator.Builder {
            showPageNumbers = lazy()
            return this
        }

        inline fun waitOnSinglePage(lazy: () -> Boolean): Paginator.Builder {
            waitOnSinglePage = lazy()
            return this
        }

        inline fun text(crossinline lazy: PageFunction<String?>): Paginator.Builder {
            textFun = { p, t -> lazy(p, t) }
            return this
        }

        inline fun color(crossinline lazy: PageFunction<Color?>): Paginator.Builder {
            colorFun = { p, t -> lazy(p, t) }
            return this
        }

        fun finalAction(block: FinalAction): Paginator.Builder {
            finalAction = block
            return this
        }
    }
}
