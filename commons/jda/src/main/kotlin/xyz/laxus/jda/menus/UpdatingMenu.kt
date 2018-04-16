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
package xyz.laxus.jda.menus

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.Permission.MESSAGE_ADD_REACTION
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.requests.restaction.MessageAction
import xyz.laxus.jda.KEmbedBuilder
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.editMessage
import xyz.laxus.jda.util.embed
import xyz.laxus.util.concurrent.Duration
import xyz.laxus.util.concurrent.duration
import java.awt.Color
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Kaidan Gustave
 */
class UpdatingMenu(builder: UpdatingMenu.Builder): Menu(builder) {
    private companion object {
        private const val cancel = "\u274C"
    }

    private val color = builder.color
    private val text = builder.text
    private val update = builder.update
    private val finalAction = builder.finalAction
    private val interval = builder.interval

    private lateinit var job: Job

    override fun displayAs(message: Message) {
        if(message.channelType == TEXT &&
           !message.guild.selfMember.hasPermission(message.textChannel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialized(message.editMessage(message))
    }

    override fun displayIn(channel: MessageChannel) {
        if(channel is TextChannel &&
           !channel.guild.selfMember.hasPermission(channel, MESSAGE_ADD_REACTION)) {
            throw PermissionException("Must be able to add reactions if not allowing typed input!")
        }
        initialized(channel.sendMessage(text ?: "null"))
    }

    private fun initialized(action: MessageAction) {
        launch(waiter) {
            val m = action.editToMenu().await()
            m.addReaction(cancel).await()
            job = launch(coroutineContext) {
                runUpdating(m)
            }
            waitForCancel(m)
        }
    }

    private suspend fun waitForCancel(message: Message) {
        waiter.receive<MessageReactionAddEvent>(delay = timeout, unit = unit) {
            if(it.messageIdLong != message.idLong)
                return@receive false
            else if(!isValidUser(it.user, it.guild))
                return@receive false
            else return@receive it.reactionEmote.name == cancel
        }.await()
        finalAction?.invoke(message)
        job.cancel()
    }

    private tailrec suspend fun runUpdating(message: Message) {
        message.editMessage { editToMenu() }.await()
        delay(interval.length, interval.unit)
        runUpdating(message)
    }

    private suspend fun MessageAction.editToMenu(): MessageAction {
        override(true)
        reset()
        text?.let { append(text) }
        embed(generateEmbed())
        return this
    }

    private suspend fun generateEmbed() = embed {
        color { this@UpdatingMenu.color }
        update()
    }

    @Menu.Dsl
    class Builder : Menu.Builder<UpdatingMenu.Builder, UpdatingMenu>() {
        lateinit var update: suspend KEmbedBuilder.() -> Unit
        var color: Color? = null
        var text: String? = null
        var finalAction: FinalAction? = null
        var interval = duration(5, SECONDS)

        fun update(update: suspend KEmbedBuilder.() -> Unit): Builder = apply {
            this.update = update
        }

        inline fun color(block: () -> Color?): Builder = apply {
            this.color = block()
        }

        inline fun text(block: () -> String?): Builder = apply {
            this.text = block()
        }

        inline fun interval(block: () -> Duration) = apply {
            this.interval = block()
        }

        fun finalAction(block: FinalAction): Builder = apply {
            this.finalAction = block
        }
    }
}